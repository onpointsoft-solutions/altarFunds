from rest_framework.decorators import api_view, permission_classes
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework import status
from django.utils import timezone
from django.conf import settings
import uuid
import logging

from .models import GivingCategory, GivingTransaction
from .paystack_service import PaystackService
from accounts.models import Member

logger = logging.getLogger(__name__)


@api_view(['POST'])
@permission_classes([IsAuthenticated])
def initialize_payment(request):
    """Initialize Paystack payment for giving"""
    try:
        user = request.user
        logger.info(f"Initializing payment for user: {user.email}")
        
        # Get request data
        category_id = request.data.get('category_id')
        amount = request.data.get('amount')
        
        if not category_id or not amount:
            return Response({
                'success': False,
                'message': 'Category ID and amount are required',
                'data': None
            }, status=status.HTTP_400_BAD_REQUEST)
        
        # Validate user has church and member profile
        if not user.church:
            return Response({
                'success': False,
                'message': 'User is not associated with any church',
                'data': None
            }, status=status.HTTP_400_BAD_REQUEST)
        
        try:
            member = user.member_profile
        except Member.DoesNotExist:
            return Response({
                'success': False,
                'message': 'Member profile not found',
                'data': None
            }, status=status.HTTP_400_BAD_REQUEST)
        
        # Validate giving category
        try:
            category = GivingCategory.objects.get(
                id=category_id,
                church=user.church,
                is_active=True
            )
        except GivingCategory.DoesNotExist:
            return Response({
                'success': False,
                'message': 'Invalid giving category',
                'data': None
            }, status=status.HTTP_400_BAD_REQUEST)
        
        # Generate unique transaction reference
        reference = f"GIVE_{user.church.church_code}_{uuid.uuid4().hex[:8]}"
        
        # Initialize Paystack transaction
        metadata = {
            'user_id': user.id,
            'church_id': user.church.id,
            'category_id': category_id,
            'member_id': member.id,
            'transaction_type': 'giving'
        }
        
        result = PaystackService.initialize_transaction(
            email=user.email,
            amount=float(amount),
            reference=reference,
            metadata=metadata
        )
        
        if result['success']:
            # Create pending giving transaction
            transaction = GivingTransaction.objects.create(
                member=member,
                church=user.church,
                category=category,
                transaction_type='one_time',
                amount=amount,
                currency='KES',
                payment_method='card',
                payment_reference=reference,
                status='pending',
                transaction_date=timezone.now(),
                created_by=user
            )
            
            logger.info(f"Initialized payment {reference} for transaction {transaction.transaction_id}")
            
            return Response({
                'success': True,
                'message': 'Payment initialized successfully',
                'data': {
                    'transaction_id': transaction.id,
                    'reference': reference,
                    'payment_url': result['data']['authorization_url'],
                    'access_code': result['data']['access_code']
                }
            }, status=status.HTTP_200_OK)
        else:
            return Response({
                'success': False,
                'message': result['error'],
                'data': None
            }, status=status.HTTP_400_BAD_REQUEST)
            
    except Exception as e:
        logger.error(f"Error initializing payment: {str(e)}")
        return Response({
            'success': False,
            'message': 'Failed to initialize payment',
            'data': None
        }, status=status.HTTP_500_INTERNAL_SERVER_ERROR)


@api_view(['POST'])
@permission_classes([IsAuthenticated])
def verify_payment(request):
    """Verify Paystack payment and process giving transaction"""
    try:
        user = request.user
        reference = request.data.get('reference')
        
        if not reference:
            return Response({
                'success': False,
                'message': 'Transaction reference is required',
                'data': None
            }, status=status.HTTP_400_BAD_REQUEST)
        
        logger.info(f"Verifying payment {reference} for user: {user.email}")
        
        # Verify transaction with Paystack
        result = PaystackService.verify_transaction(reference)
        
        if result['success']:
            paystack_data = result['data']
            
            # Check if payment was successful
            if paystack_data['status'] == 'success':
                # Find the pending transaction
                try:
                    transaction = GivingTransaction.objects.get(
                        payment_reference=reference,
                        member=user.member_profile
                    )
                except GivingTransaction.DoesNotExist:
                    return Response({
                        'success': False,
                        'message': 'Transaction not found',
                        'data': None
                    }, status=status.HTTP_404_NOT_FOUND)
                
                # Process the payment
                process_result = PaystackService.process_payment(
                    transaction_id=reference,
                    user=user,
                    church=transaction.church,
                    category=transaction.category,
                    amount=float(paystack_data['amount']) / 100,  # Convert back from kobo
                    metadata=paystack_data.get('metadata')
                )
                
                if process_result['success']:
                    return Response({
                        'success': True,
                        'message': 'Payment verified and processed successfully',
                        'data': {
                            'transaction_id': process_result['transaction_id'],
                            'amount': process_result['amount'],
                            'status': 'completed'
                        }
                    }, status=status.HTTP_200_OK)
                else:
                    return Response({
                        'success': False,
                        'message': process_result['error'],
                        'data': None
                    }, status=status.HTTP_400_BAD_REQUEST)
            else:
                return Response({
                    'success': False,
                    'message': f"Payment failed: {paystack_data.get('gateway_response', 'Unknown error')}",
                    'data': None
                }, status=status.HTTP_400_BAD_REQUEST)
        else:
            return Response({
                'success': False,
                'message': result['error'],
                'data': None
            }, status=status.HTTP_400_BAD_REQUEST)
            
    except Exception as e:
        logger.error(f"Error verifying payment: {str(e)}")
        return Response({
            'success': False,
            'message': 'Failed to verify payment',
            'data': None
        }, status=status.HTTP_500_INTERNAL_SERVER_ERROR)


@api_view(['POST'])
@permission_classes([])
def paystack_webhook(request):
    """Handle Paystack webhook notifications"""
    try:
        # Verify webhook signature (optional but recommended)
        # You should verify the webhook signature using your Paystack secret key
        
        event_data = request.data
        event = event_data.get('event')
        data = event_data.get('data')
        
        logger.info(f"Received Paystack webhook: {event}")
        
        if event == 'charge.success':
            # Payment successful - process the transaction
            reference = data.get('reference')
            
            try:
                transaction = GivingTransaction.objects.get(
                    payment_reference=reference,
                    status='pending'
                )
                
                # Process the successful payment
                result = PaystackService.process_payment(
                    transaction_id=reference,
                    user=transaction.member.user,
                    church=transaction.church,
                    category=transaction.category,
                    amount=float(data.get('amount')) / 100,
                    metadata=data.get('metadata')
                )
                
                if result['success']:
                    logger.info(f"Webhook processed payment {reference} successfully")
                else:
                    logger.error(f"Webhook failed to process payment {reference}: {result['error']}")
                    
            except GivingTransaction.DoesNotExist:
                logger.warning(f"Transaction not found for webhook reference: {reference}")
        
        elif event == 'charge.failed':
            # Payment failed - update transaction status
            reference = data.get('reference')
            
            try:
                transaction = GivingTransaction.objects.get(
                    payment_reference=reference,
                    status='pending'
                )
                
                transaction.status = 'failed'
                transaction.notes = f"Payment failed: {data.get('gateway_response', 'Unknown error')}"
                transaction.save()
                
                logger.info(f"Webhook marked payment {reference} as failed")
                
            except GivingTransaction.DoesNotExist:
                logger.warning(f"Transaction not found for webhook reference: {reference}")
        
        return Response({'status': 'success'}, status=status.HTTP_200_OK)
        
    except Exception as e:
        logger.error(f"Error processing Paystack webhook: {str(e)}")
        return Response(
            {'status': 'error', 'message': str(e)}, 
            status=status.HTTP_500_INTERNAL_SERVER_ERROR
        )
