from rest_framework.decorators import api_view, permission_classes
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework import status
from django.utils import timezone
from django.views.decorators.csrf import csrf_exempt
from django.views.decorators.http import require_http_methods
import json
import logging

from .models import ChurchDisbursement, GivingTransaction
from .paystack_disbursement import PaystackDisbursementService

logger = logging.getLogger(__name__)


@api_view(['POST'])
@permission_classes([IsAuthenticated])
def initiate_disbursement(request):
    """Initiate Paystack disbursement to church (admin only)"""
    try:
        user = request.user
        
        # Check if user has permission (church admin or system admin)
        if user.role not in ['pastor', 'denomination_admin', 'system_admin']:
            return Response({
                'success': False,
                'message': 'Permission denied',
                'data': None
            }, status=status.HTTP_403_FORBIDDEN)
        
        transaction_id = request.data.get('transaction_id')
        
        if not transaction_id:
            return Response({
                'success': False,
                'message': 'Transaction ID is required',
                'data': None
            }, status=status.HTTP_400_BAD_REQUEST)
        
        # Get the giving transaction
        try:
            transaction = GivingTransaction.objects.get(id=transaction_id)
        except GivingTransaction.DoesNotExist:
            return Response({
                'success': False,
                'message': 'Transaction not found',
                'data': None
            }, status=status.HTTP_404_NOT_FOUND)
        
        # Check if user can disburse for this church
        if user.role == 'pastor' and transaction.church != user.church:
            return Response({
                'success': False,
                'message': 'You can only disburse funds for your own church',
                'data': None
            }, status=status.HTTP_403_FORBIDDEN)
        
        # Check if disbursement already exists
        if hasattr(transaction, 'disbursement'):
            return Response({
                'success': False,
                'message': 'Disbursement already initiated for this transaction',
                'data': {
                    'disbursement_id': transaction.disbursement.id,
                    'status': transaction.disbursement.status
                }
            }, status=status.HTTP_400_BAD_REQUEST)
        
        # Initiate disbursement
        result = PaystackDisbursementService.disburse_to_church(transaction)
        
        if result['success']:
            return Response({
                'success': True,
                'message': 'Disbursement initiated successfully',
                'data': {
                    'disbursement_id': result['disbursement_id'],
                    'amount': result['amount'],
                    'platform_fee': result.get('platform_fee', 0),
                    'transfer_code': result.get('transfer_code')
                }
            }, status=status.HTTP_200_OK)
        else:
            return Response({
                'success': False,
                'message': result['error'],
                'data': None
            }, status=status.HTTP_400_BAD_REQUEST)
            
    except Exception as e:
        logger.error(f"Error initiating disbursement: {str(e)}")
        return Response({
            'success': False,
            'message': 'Failed to initiate disbursement',
            'data': None
        }, status=status.HTTP_500_INTERNAL_SERVER_ERROR)


@api_view(['GET'])
@permission_classes([IsAuthenticated])
def disbursement_status(request, disbursement_id):
    """Get disbursement status"""
    try:
        user = request.user
        
        # Get disbursement
        try:
            disbursement = ChurchDisbursement.objects.get(id=disbursement_id)
        except ChurchDisbursement.DoesNotExist:
            return Response({
                'success': False,
                'message': 'Disbursement not found',
                'data': None
            }, status=status.HTTP_404_NOT_FOUND)
        
        # Check if user can view this disbursement
        if user.role == 'pastor' and disbursement.church != user.church:
            return Response({
                'success': False,
                'message': 'Permission denied',
                'data': None
            }, status=status.HTTP_403_FORBIDDEN)
        
        response_data = {
            'id': disbursement.id,
            'church_name': disbursement.church.name,
            'amount': float(disbursement.amount),
            'platform_fee': float(disbursement.platform_fee),
            'net_amount': float(disbursement.net_amount),
            'status': disbursement.status,
            'disbursement_method': disbursement.disbursement_method,
            'created_at': disbursement.created_at,
            'error_message': disbursement.error_message,
            'retry_count': disbursement.retry_count,
            'paystack_receipt': disbursement.paystack_receipt,
            'transfer_code': disbursement.transfer_code
        }
        
        return Response({
            'success': True,
            'message': 'Disbursement status retrieved successfully',
            'data': response_data
        }, status=status.HTTP_200_OK)
        
    except Exception as e:
        logger.error(f"Error getting disbursement status: {str(e)}")
        return Response({
            'success': False,
            'message': 'Failed to get disbursement status',
            'data': None
        }, status=status.HTTP_500_INTERNAL_SERVER_ERROR)


@api_view(['GET'])
@permission_classes([IsAuthenticated])
def church_disbursements(request):
    """Get all disbursements for a church"""
    try:
        user = request.user
        
        # Filter by user's church (if pastor) or all churches (if admin)
        if user.role == 'pastor':
            if not user.church:
                return Response({
                    'success': False,
                    'message': 'User is not associated with any church',
                    'data': []
                }, status=status.HTTP_400_BAD_REQUEST)
            
            disbursements = ChurchDisbursement.objects.filter(church=user.church)
        elif user.role in ['denomination_admin', 'system_admin']:
            disbursements = ChurchDisbursement.objects.all()
        else:
            return Response({
                'success': False,
                'message': 'Permission denied',
                'data': []
            }, status=status.HTTP_403_FORBIDDEN)
        
        # Order by most recent
        disbursements = disbursements.order_by('-created_at')
        
        # Serialize data
        response_data = []
        for disbursement in disbursements:
            response_data.append({
                'id': disbursement.id,
                'church_name': disbursement.church.name,
                'amount': float(disbursement.amount),
                'platform_fee': float(disbursement.platform_fee),
                'net_amount': float(disbursement.net_amount),
                'status': disbursement.status,
                'disbursement_method': disbursement.disbursement_method,
                'created_at': disbursement.created_at,
                'completed_at': disbursement.completed_at,
                'paystack_receipt': disbursement.paystack_receipt,
                'transfer_code': disbursement.transfer_code,
                'giving_transaction_id': disbursement.giving_transaction.id,
                'giver_name': disbursement.giving_transaction.member.user.get_full_name()
            })
        
        return Response({
            'success': True,
            'message': f'Found {len(response_data)} disbursements',
            'data': response_data
        }, status=status.HTTP_200_OK)
        
    except Exception as e:
        logger.error(f"Error getting church disbursements: {str(e)}")
        return Response({
            'success': False,
            'message': 'Failed to get disbursements',
            'data': []
        }, status=status.HTTP_500_INTERNAL_SERVER_ERROR)


@csrf_exempt
@require_http_methods(["POST"])
def paystack_transfer_webhook(request):
    """Handle Paystack transfer webhook notifications"""
    try:
        # Parse the webhook data
        webhook_data = json.loads(request.body)
        
        logger.info(f"Received Paystack transfer webhook: {webhook_data}")
        
        # Process the webhook
        PaystackDisbursementService.handle_transfer_webhook(webhook_data)
        
        return Response({'status': 'success'}, status=status.HTTP_200_OK)
        
    except json.JSONDecodeError:
        logger.error("Invalid JSON in Paystack transfer webhook")
        return Response(
            {'status': 'error', 'message': 'Invalid JSON'}, 
            status=status.HTTP_400_BAD_REQUEST
        )
    except Exception as e:
        logger.error(f"Error processing Paystack transfer webhook: {str(e)}")
        return Response(
            {'status': 'error', 'message': str(e)}, 
            status=status.HTTP_500_INTERNAL_SERVER_ERROR
        )
