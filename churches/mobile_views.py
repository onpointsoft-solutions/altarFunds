from rest_framework.decorators import api_view, permission_classes
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework import status
from django.utils import timezone
import logging

logger = logging.getLogger(__name__)


@api_view(['GET'])
@permission_classes([IsAuthenticated])
def church_payment_details(request):
    """Get payment details for user's church"""
    try:
        user = request.user
        logger.info(f"Fetching payment details for user: {user.email}")
        
        # Check if user has a church
        if not user.church:
            logger.warning(f"User {user.email} has no church assigned")
            return Response({
                'success': False,
                'message': 'User is not associated with any church',
                'data': None
            }, status=status.HTTP_400_BAD_REQUEST)
        
        church = user.church
        
        # Prepare payment details
        payment_details = {
            'church_name': church.name,
            'church_code': church.church_code,
            'mpesa': {
                'paybill_number': church.mpesa_paybill_number,
                'account_number': church.mpesa_account_number,
                'till_number': church.mpesa_till_number,
            },
            'bank': {
                'account_name': church.bank_account_name,
                'account_number': church.bank_account_number,
                'bank_name': church.bank_name,
                'branch': church.bank_branch,
            },
            'paystack': {
                'enabled': church.enable_paystack,
                'public_key': church.paystack_public_key if church.enable_paystack else None,
            },
            'allow_online_giving': church.allow_online_giving,
        }
        
        # Remove empty payment methods
        if not payment_details['mpesa']['paybill_number'] and not payment_details['mpesa']['till_number']:
            payment_details.pop('mpesa', None)
        
        if not any(payment_details['bank'].values()):
            payment_details.pop('bank', None)
        
        logger.info(f"Returning payment details for {church.name}")
        
        return Response({
            'success': True,
            'message': 'Payment details retrieved successfully',
            'data': payment_details
        }, status=status.HTTP_200_OK)
        
    except Exception as e:
        logger.error(f"Error fetching payment details: {str(e)}")
        return Response({
            'success': False,
            'message': 'Failed to fetch payment details',
            'data': None
        }, status=status.HTTP_500_INTERNAL_SERVER_ERROR)


@api_view(['GET'])
@permission_classes([IsAuthenticated])
def church_theme_colors(request):
    """Get theme colors for user's church"""
    try:
        user = request.user
        logger.info(f"Fetching theme colors for user: {user.email}")
        
        # Check if user has a church
        if not user.church:
            logger.warning(f"User {user.email} has no church assigned")
            return Response({
                'success': False,
                'message': 'User is not associated with any church',
                'data': None
            }, status=status.HTTP_400_BAD_REQUEST)
        
        church = user.church
        
        # Prepare theme colors
        theme_colors = {
            'primary_color': church.primary_color,
            'secondary_color': church.secondary_color,
            'accent_color': church.accent_color,
            'church_name': church.name,
            'church_code': church.church_code,
        }
        
        # Add logo URL if available
        if church.logo:
            theme_colors['logo_url'] = request.build_absolute_uri(church.logo.url)
        
        logger.info(f"Returning theme colors for {church.name}")
        
        return Response({
            'success': True,
            'message': 'Theme colors retrieved successfully',
            'data': theme_colors
        }, status=status.HTTP_200_OK)
        
    except Exception as e:
        logger.error(f"Error fetching theme colors: {str(e)}")
        return Response({
            'success': False,
            'message': 'Failed to fetch theme colors',
            'data': None
        }, status=status.HTTP_500_INTERNAL_SERVER_ERROR)
