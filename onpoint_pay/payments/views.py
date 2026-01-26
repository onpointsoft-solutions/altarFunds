import uuid
import secrets
from decimal import Decimal
from rest_framework import status, generics, permissions
from rest_framework.decorators import api_view, permission_classes
from rest_framework.response import Response
from rest_framework.permissions import IsAuthenticated
from django.utils import timezone
from django.db import transaction
from django.conf import settings
from merchants.models import Merchant, ApiKey
from .models import Transaction, MpesaRequest, CardPayment, AuditLog
from .serializers import (
    PaymentInitiateSerializer,
    TransactionSerializer,
    PaymentStatusSerializer,
    RefundSerializer
)
from .services.mpesa_service import MpesaService
from .utils import validate_api_key, check_rate_limits, create_webhook_log


class PaymentInitiateView(generics.CreateAPIView):
    """Initiate a new payment"""
    permission_classes = [IsAuthenticated]
    serializer_class = PaymentInitiateSerializer
    
    def post(self, request, *args, **kwargs):
        serializer = self.get_serializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        
        # Validate API key and rate limits
        api_key = validate_api_key(request)
        if not api_key:
            return Response(
                {'error': 'Invalid or missing API key'},
                status=status.HTTP_401_UNAUTHORIZED
            )
        
        # Check rate limits
        rate_limit_check = check_rate_limits(request.user, serializer.validated_data['amount'])
        if not rate_limit_check['allowed']:
            return Response(
                {'error': rate_limit_check['message']},
                status=status.HTTP_429_TOO_MANY_REQUESTS
            )
        
        try:
            with transaction.atomic():
                # Create transaction record
                transaction_data = {
                    'merchant': request.user,
                    'amount': serializer.validated_data['amount'],
                    'currency': serializer.validated_data['currency'],
                    'payment_method': serializer.validated_data['payment_method'],
                    'customer_phone': serializer.validated_data.get('phone', ''),
                    'customer_email': serializer.validated_data.get('email', ''),
                    'reference': serializer.validated_data.get('reference', ''),
                    'callback_url': serializer.validated_data.get('callback_url', ''),
                    'metadata': serializer.validated_data.get('metadata', {}),
                    'expires_at': timezone.now() + timezone.timedelta(minutes=30),
                }
                
                new_transaction = Transaction.objects.create(**transaction_data)
                
                # Process payment based on method
                payment_method = serializer.validated_data['payment_method']
                
                if payment_method == 'mpesa':
                    return self._process_mpesa_payment(
                        new_transaction, serializer.validated_data, api_key
                    )
                elif payment_method == 'card':
                    return self._process_card_payment(
                        new_transaction, serializer.validated_data, api_key
                    )
                else:
                    return Response(
                        {'error': 'Unsupported payment method'},
                        status=status.HTTP_400_BAD_REQUEST
                    )
        
        except Exception as e:
            # Log error
            AuditLog.objects.create(
                merchant=request.user,
                action='payment_initiation_failed',
                resource_type='transaction',
                resource_id=str(new_transaction.id),
                ip_address=self.get_client_ip(request),
                user_agent=request.META.get('HTTP_USER_AGENT', ''),
                success=False,
                error_message=str(e)
            )
            
            return Response(
                {'error': 'Payment initiation failed. Please try again.'},
                status=status.HTTP_500_INTERNAL_SERVER_ERROR
            )
    
    def _process_mpesa_payment(self, transaction, data, api_key):
        """Process M-Pesa payment"""
        try:
            mpesa_service = MpesaService()
            
            # Initiate STK Push
            result = mpesa_service.initiate_stk_push(
                phone_number=data['phone'],
                amount=transaction.amount,
                reference=transaction.reference,
                callback_url=f"{settings.MPESA_CALLBACK_URL}?transaction_id={transaction.id}",
                description=data.get('metadata', {}).get('description', '')
            )
            
            if result['success']:
                # Create M-Pesa request record
                MpesaRequest.objects.create(
                    transaction=transaction,
                    phone_number=data['phone'],
                    checkout_request_id=result['checkout_request_id'],
                    merchant_request_id=result.get('merchant_request_id', ''),
                    response_code='0',
                    response_description='Success',
                    customer_message=result.get('customer_message', '')
                )
                
                # Update transaction status
                transaction.status = 'processing'
                transaction.save(update_fields=['status'])
                
                # Log success
                AuditLog.objects.create(
                    merchant=transaction.merchant,
                    action='mpesa_stk_initiated',
                    resource_type='transaction',
                    resource_id=str(transaction.id),
                    ip_address=self.get_client_ip(self.request),
                    user_agent=self.request.META.get('HTTP_USER_AGENT', ''),
                    success=True,
                    new_values={
                        'checkout_request_id': result['checkout_request_id'],
                        'phone_number': data['phone']
                    }
                )
                
                return Response({
                    'success': True,
                    'message': 'Payment initiated successfully',
                    'transaction': TransactionSerializer(transaction).data,
                    'payment_details': {
                        'checkout_request_id': result['checkout_request_id'],
                        'customer_message': result.get('customer_message', ''),
                        'phone_number': data['phone']
                    }
                }, status=status.HTTP_201_CREATED)
            else:
                # Update transaction status to failed
                transaction.status = 'failed'
                transaction.save(update_fields=['status'])
                
                return Response({
                    'success': False,
                    'error': result['error'],
                    'transaction': TransactionSerializer(transaction).data
                }, status=status.HTTP_400_BAD_REQUEST)
        
        except Exception as e:
            transaction.status = 'failed'
            transaction.save(update_fields=['status'])
            raise e
    
    def _process_card_payment(self, transaction, data, api_key):
        """Process card payment"""
        # For now, return a placeholder response
        # In production, integrate with Paystack/Flutterwave
        return Response({
            'success': False,
            'error': 'Card payments not yet implemented',
            'transaction': TransactionSerializer(transaction).data
        }, status=status.HTTP_501_NOT_IMPLEMENTED)
    
    def get_client_ip(self, request):
        x_forwarded_for = request.META.get('HTTP_X_FORWARDED_FOR')
        if x_forwarded_for:
            ip = x_forwarded_for.split(',')[0]
        else:
            ip = request.META.get('REMOTE_ADDR')
        return ip


class PaymentStatusView(generics.GenericAPIView):
    """Check payment status"""
    permission_classes = [IsAuthenticated]
    serializer_class = PaymentStatusSerializer
    
    def post(self, request, *args, **kwargs):
        serializer = self.get_serializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        
        try:
            transaction = Transaction.objects.get(
                reference=serializer.validated_data['reference'],
                merchant=request.user
            )
            
            # If transaction is still pending, check with payment provider
            if transaction.status == 'processing':
                self._check_external_status(transaction)
            
            return Response({
                'success': True,
                'transaction': TransactionSerializer(transaction).data
            })
            
        except Transaction.DoesNotExist:
            return Response(
                {'error': 'Transaction not found'},
                status=status.HTTP_404_NOT_FOUND
            )
    
    def _check_external_status(self, transaction):
        """Check status with external payment provider"""
        if transaction.payment_method == 'mpesa':
            try:
                mpesa_request = transaction.mpesa_details
                if mpesa_request and mpesa_request.checkout_request_id:
                    mpesa_service = MpesaService()
                    status_result = mpesa_service.check_transaction_status(
                        mpesa_request.checkout_request_id
                    )
                    
                    if status_result['success']:
                        # Update transaction and M-Pesa records
                        transaction.status = 'completed'
                        transaction.completed_at = timezone.now()
                        transaction.save(update_fields=['status', 'completed_at'])
                        
                        mpesa_request.mpesa_receipt = status_result.get('mpesa_receipt')
                        mpesa_request.transaction_date = status_result.get('transaction_date')
                        mpesa_request.result_code = status_result.get('result_code')
                        mpesa_request.callback_received_at = timezone.now()
                        mpesa_request.save(update_fields=[
                            'mpesa_receipt', 'transaction_date', 
                            'result_code', 'callback_received_at'
                        ])
                        
                        # Trigger webhook
                        self._trigger_webhook(transaction, 'payment.completed')
            except Exception as e:
                # Log error but don't fail the request
                pass
    
    def _trigger_webhook(self, transaction, event_type):
        """Trigger webhook for merchant"""
        if transaction.callback_url:
            create_webhook_log(
                merchant=transaction.merchant,
                transaction=transaction,
                webhook_url=transaction.callback_url,
                event_type=event_type,
                payload=TransactionSerializer(transaction).data
            )


class TransactionListView(generics.ListAPIView):
    """List merchant's transactions"""
    permission_classes = [IsAuthenticated]
    serializer_class = TransactionSerializer
    
    def get_queryset(self):
        queryset = Transaction.objects.filter(merchant=self.request.user)
        
        # Filter by status
        status_filter = self.request.query_params.get('status')
        if status_filter:
            queryset = queryset.filter(status=status_filter)
        
        # Filter by payment method
        method_filter = self.request.query_params.get('payment_method')
        if method_filter:
            queryset = queryset.filter(payment_method=method_filter)
        
        # Filter by date range
        date_from = self.request.query_params.get('date_from')
        date_to = self.request.query_params.get('date_to')
        if date_from:
            queryset = queryset.filter(created_at__gte=date_from)
        if date_to:
            queryset = queryset.filter(created_at__lte=date_to)
        
        return queryset.order_by('-created_at')


class TransactionDetailView(generics.RetrieveAPIView):
    """Get transaction details"""
    permission_classes = [IsAuthenticated]
    serializer_class = TransactionSerializer
    
    def get_queryset(self):
        return Transaction.objects.filter(merchant=self.request.user)


@api_view(['POST'])
@permission_classes([IsAuthenticated])
def refund_payment(request):
    """Refund a payment"""
    serializer = RefundSerializer(data=request.data)
    serializer.is_valid(raise_exception=True)
    
    try:
        with transaction.atomic():
            # Get original transaction
            original_transaction = Transaction.objects.get(
                reference=serializer.validated_data['reference'],
                merchant=request.user
            )
            
            # Create refund transaction
            refund_transaction = Transaction.objects.create(
                merchant=request.user,
                amount=serializer.validated_data['amount'],
                currency=original_transaction.currency,
                payment_method=original_transaction.payment_method,
                transaction_type='refund',
                customer_name=original_transaction.customer_name,
                customer_email=original_transaction.customer_email,
                customer_phone=original_transaction.customer_phone,
                external_reference=original_transaction.reference,
                metadata={
                    'original_transaction': original_transaction.reference,
                    'refund_reason': serializer.validated_data.get('reason', '')
                }
            )
            
            # Process refund based on payment method
            if original_transaction.payment_method == 'mpesa':
                # For M-Pesa, we would typically need to do a manual reversal
                # or have the customer contact M-Pesa directly
                refund_transaction.status = 'completed'
                refund_transaction.completed_at = timezone.now()
                refund_transaction.save(update_fields=['status', 'completed_at'])
                
                # Update original transaction
                original_transaction.status = 'refunded'
                original_transaction.save(update_fields=['status'])
                
                # Log the refund
                AuditLog.objects.create(
                    merchant=request.user,
                    action='payment_refunded',
                    resource_type='transaction',
                    resource_id=str(refund_transaction.id),
                    ip_address=get_client_ip(request),
                    user_agent=request.META.get('HTTP_USER_AGENT', ''),
                    success=True,
                    new_values={
                        'original_transaction': original_transaction.reference,
                        'refund_amount': serializer.validated_data['amount']
                    }
                )
                
                return Response({
                    'success': True,
                    'message': 'Refund processed successfully',
                    'refund_transaction': TransactionSerializer(refund_transaction).data
                })
            else:
                return Response({
                    'success': False,
                    'error': 'Refunds not supported for this payment method'
                }, status=status.HTTP_400_BAD_REQUEST)
    
    except Transaction.DoesNotExist:
        return Response(
            {'error': 'Original transaction not found'},
            status=status.HTTP_404_NOT_FOUND
        )
    except Exception as e:
        return Response(
            {'error': 'Refund processing failed'},
            status=status.HTTP_500_INTERNAL_SERVER_ERROR
        )


def get_client_ip(request):
    """Helper function to get client IP"""
    x_forwarded_for = request.META.get('HTTP_X_FORWARDED_FOR')
    if x_forwarded_for:
        ip = x_forwarded_for.split(',')[0]
    else:
        ip = request.META.get('REMOTE_ADDR')
    return ip
