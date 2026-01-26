from rest_framework import viewsets, status
from rest_framework.decorators import action, api_view, permission_classes
from rest_framework.response import Response
from rest_framework.permissions import IsAuthenticated, AllowAny
from django.shortcuts import get_object_or_404
from django.views.decorators.csrf import csrf_exempt
from django.utils.decorators import method_decorator
from django.utils import timezone
from .models import PaymentRequest, Payment, Transaction
from .serializers import PaymentRequestSerializer, PaymentSerializer, TransactionSerializer
from .paystack_service import paystack_service
from common.permissions import CanViewPayments, IsChurchAdmin, IsSystemAdmin
import json
import uuid
import logging

logger = logging.getLogger(__name__)


class PaymentRequestViewSet(viewsets.ModelViewSet):
    queryset = PaymentRequest.objects.all()
    serializer_class = PaymentRequestSerializer
    permission_classes = [IsAuthenticated]

    @action(detail=True, methods=['post'])
    def approve(self, request, pk=None):
        payment_request = self.get_object()
        payment_request.status = 'approved'
        payment_request.save()
        return Response({'status': 'approved'})


class PaymentViewSet(viewsets.ModelViewSet):
    queryset = Payment.objects.all()
    serializer_class = PaymentSerializer
    permission_classes = [IsAuthenticated, CanViewPayments]
    
    def get_queryset(self):
        """Filter payments based on user role"""
        user = self.request.user
        
        # System admins see all payments
        if user.role == 'system_admin':
            return Payment.objects.all()
        
        # Church admins see their church's payments
        if user.role in ['pastor', 'treasurer', 'auditor']:
            return Payment.objects.filter(giving__church=user.church)
        
        # Members see only their own payments
        return Payment.objects.filter(user=user)
    
    @action(detail=False, methods=['post'], permission_classes=[IsAuthenticated])
    def initialize_paystack(self, request):
        """Initialize Paystack payment"""
        try:
            # Extract payment details
            email = request.data.get('email') or request.user.email
            amount = request.data.get('amount')
            giving_type = request.data.get('giving_type')
            church_id = request.data.get('church_id')
            metadata = request.data.get('metadata', {})
            
            # Validate required fields
            if not amount or not giving_type or not church_id:
                return Response({
                    'success': False,
                    'message': 'Missing required fields: amount, giving_type, church_id'
                }, status=status.HTTP_400_BAD_REQUEST)
            
            # Generate unique reference
            reference = f"AF-{uuid.uuid4().hex[:12].upper()}"
            
            # Add user and church info to metadata
            metadata.update({
                'user_id': request.user.id,
                'user_email': request.user.email,
                'church_id': church_id,
                'giving_type': giving_type
            })
            
            # Initialize payment with Paystack
            result = paystack_service.initialize_payment(
                email=email,
                amount=amount,
                reference=reference,
                metadata=metadata,
                callback_url=request.data.get('callback_url')
            )
            
            if result['success']:
                # Create payment record
                payment = Payment.objects.create(
                    user=request.user,
                    amount=amount,
                    reference=reference,
                    payment_method='paystack',
                    status='pending',
                    metadata=metadata
                )
                
                return Response({
                    'success': True,
                    'data': {
                        'authorization_url': result['authorization_url'],
                        'access_code': result['access_code'],
                        'reference': reference
                    }
                }, status=status.HTTP_200_OK)
            else:
                return Response({
                    'success': False,
                    'message': result.get('message', 'Payment initialization failed')
                }, status=status.HTTP_400_BAD_REQUEST)
                
        except Exception as e:
            logger.error(f"Payment initialization error: {str(e)}")
            return Response({
                'success': False,
                'message': 'Payment initialization failed'
            }, status=status.HTTP_500_INTERNAL_SERVER_ERROR)
    
    @action(detail=False, methods=['get'], permission_classes=[IsAuthenticated])
    def verify_payment(self, request):
        """Verify Paystack payment"""
        reference = request.query_params.get('reference')
        
        if not reference:
            return Response({
                'success': False,
                'message': 'Payment reference is required'
            }, status=status.HTTP_400_BAD_REQUEST)
        
        try:
            # Verify with Paystack
            result = paystack_service.verify_payment(reference)
            
            if result['success']:
                # Update payment record
                try:
                    payment = Payment.objects.get(reference=reference)
                    
                    if result['status'] == 'success':
                        payment.status = 'completed'
                        payment.paid_at = timezone.now()
                        payment.transaction_id = result.get('reference')
                        payment.save()
                        
                        # Update associated giving record if exists
                        if hasattr(payment, 'giving'):
                            giving = payment.giving
                            giving.status = 'completed'
                            giving.payment_date = timezone.now()
                            giving.save()
                    
                    return Response({
                        'success': True,
                        'data': {
                            'status': result['status'],
                            'amount': result['amount'],
                            'reference': reference,
                            'paid_at': result.get('paid_at')
                        }
                    }, status=status.HTTP_200_OK)
                    
                except Payment.DoesNotExist:
                    return Response({
                        'success': False,
                        'message': 'Payment record not found'
                    }, status=status.HTTP_404_NOT_FOUND)
            else:
                return Response({
                    'success': False,
                    'message': result.get('message', 'Payment verification failed')
                }, status=status.HTTP_400_BAD_REQUEST)
                
        except Exception as e:
            logger.error(f"Payment verification error: {str(e)}")
            return Response({
                'success': False,
                'message': 'Payment verification failed'
            }, status=status.HTTP_500_INTERNAL_SERVER_ERROR)


class TransactionViewSet(viewsets.ModelViewSet):
    queryset = Transaction.objects.all()
    serializer_class = TransactionSerializer
    permission_classes = [IsAuthenticated]


@csrf_exempt
@api_view(['POST'])
@permission_classes([AllowAny])
def paystack_webhook(request):
    """Handle Paystack webhook events"""
    try:
        # Get webhook signature
        signature = request.headers.get('X-Paystack-Signature')
        
        if not signature:
            return Response({
                'success': False,
                'message': 'Missing signature'
            }, status=status.HTTP_400_BAD_REQUEST)
        
        # Verify signature
        payload = request.body
        if not paystack_service.verify_webhook_signature(payload, signature):
            logger.warning("Invalid webhook signature")
            return Response({
                'success': False,
                'message': 'Invalid signature'
            }, status=status.HTTP_401_UNAUTHORIZED)
        
        # Parse webhook data
        data = json.loads(payload)
        event_type = data.get('event')
        event_data = data.get('data', {})
        
        # Process webhook
        result = paystack_service.process_webhook(event_type, event_data)
        
        return Response(result, status=status.HTTP_200_OK)
        
    except json.JSONDecodeError:
        logger.error("Invalid JSON in webhook payload")
        return Response({
            'success': False,
            'message': 'Invalid JSON'
        }, status=status.HTTP_400_BAD_REQUEST)
    except Exception as e:
        logger.error(f"Webhook processing error: {str(e)}")
        return Response({
            'success': False,
            'message': 'Webhook processing failed'
        }, status=status.HTTP_500_INTERNAL_SERVER_ERROR)
