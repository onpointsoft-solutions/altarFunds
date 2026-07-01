from rest_framework import viewsets, status
from rest_framework.decorators import action, api_view, permission_classes
from rest_framework.response import Response
from rest_framework.permissions import IsAuthenticated
from django.db.models import Sum, Count, Q
from django.utils import timezone
from django.db import transaction
from datetime import datetime, timedelta
from decimal import Decimal

from .models import GivingCategory, GivingTransaction, RecurringGiving, Pledge, GivingCampaign
from .serializers import (
    GivingCategorySerializer, 
    GivingTransactionSerializer, 
    RecurringGivingSerializer, 
    PledgeSerializer, 
    GivingCampaignSerializer
)
from common.permissions import IsMember, IsChurchAdmin, IsSystemAdmin, IsOwnerOrChurchAdmin
from payments.models import Payment
import logging

logger = logging.getLogger(__name__)


@api_view(['GET', 'POST'])
@permission_classes([IsAuthenticated])
def giving_categories(request):
    """
    GET  — return active giving categories for the user's church.
    POST — create a new giving category (treasurer / pastor only).
    """
    user = request.user

    if not user.church and user.role != 'system_admin':
        return Response({
            'success': False, 'message': 'User is not associated with any church', 'data': []
        }, status=status.HTTP_400_BAD_REQUEST)

    # ── GET ──────────────────────────────────────────────────────────────────
    if request.method == 'GET':
        try:
            if user.role == 'system_admin':
                categories = GivingCategory.objects.filter(is_active=True)
            else:
                categories = GivingCategory.objects.filter(church=user.church, is_active=True)
            serializer = GivingCategorySerializer(categories, many=True)
            return Response({
                'success': True,
                'message': f'Found {categories.count()} categories',
                'data': serializer.data
            }, status=status.HTTP_200_OK)
        except Exception as e:
            logger.error(f"Error fetching giving categories: {str(e)}")
            return Response({'success': False, 'message': 'Failed to fetch categories', 'data': []},
                            status=status.HTTP_500_INTERNAL_SERVER_ERROR)

    # ── POST ─────────────────────────────────────────────────────────────────
    if user.role not in ('treasurer', 'pastor', 'denomination_admin', 'system_admin'):
        return Response({'success': False, 'message': 'Only treasurers and pastors can create categories.'},
                        status=status.HTTP_403_FORBIDDEN)

    try:
        name         = request.data.get('name', '').strip()
        description  = request.data.get('description', '').strip()
        has_target   = bool(request.data.get('has_target', False))
        monthly_tgt  = request.data.get('monthly_target', None)
        yearly_tgt   = request.data.get('yearly_target',  None)

        if not name:
            return Response({'success': False, 'message': 'name is required.'},
                            status=status.HTTP_400_BAD_REQUEST)

        church = user.church if user.role != 'system_admin' else None
        if church is None and user.role == 'system_admin':
            from churches.models import Church as _Church
            church_id = request.data.get('church')
            if church_id:
                church = _Church.objects.get(pk=church_id)

        cat = GivingCategory.objects.create(
            name           = name,
            description    = description,
            church         = church,
            has_target     = has_target,
            monthly_target = monthly_tgt,
            yearly_target  = yearly_tgt,
            is_active      = True,
            display_order  = GivingCategory.objects.filter(church=church).count(),
        )
        serializer = GivingCategorySerializer(cat)
        return Response({'success': True, 'message': f'Category "{name}" created.', 'data': serializer.data},
                        status=status.HTTP_201_CREATED)

    except Exception as e:
        logger.error(f"Error creating giving category: {str(e)}")
        return Response({'success': False, 'message': f'Failed to create category: {str(e)}'},
                        status=status.HTTP_500_INTERNAL_SERVER_ERROR)


def _normalize_payment_method(raw: str) -> str:
    """Map mobile app payment method strings to valid GivingTransaction choices."""
    mapping = {
        'mpesa':        'mobile_money',
        'm-pesa':       'mobile_money',
        'm_pesa':       'mobile_money',
        'mobile_money': 'mobile_money',
        'card':         'card',
        'paystack':     'paystack',
        'bank':         'bank_transfer',
        'bank_transfer':'bank_transfer',
        'cash':         'cash',
        'check':        'check',
        'cheque':       'check',
    }
    return mapping.get(raw.strip().lower(), 'other')


@api_view(['POST'])
@permission_classes([IsAuthenticated])
def create_giving_transaction(request):
    """Create a giving transaction from mobile app"""
    try:
        user = request.user
        logger.info(f"Creating giving transaction for user: {user.email}")
        
        # Check if user has a church
        if not user.church:
            logger.warning(f"User {user.email} has no church assigned")
            return Response({
                'success': False,
                'message': 'User is not associated with any church',
                'data': None
            }, status=status.HTTP_400_BAD_REQUEST)
        
        # Get the member profile for the user
        try:
            member = user.member_profile
        except:
            logger.warning(f"User {user.email} has no member profile")
            return Response({
                'success': False,
                'message': 'User member profile not found',
                'data': None
            }, status=status.HTTP_400_BAD_REQUEST)
        
        # Get request data
        data = request.data
        
        # Support both 'category' (backend) and 'donationType' (mobile) field names
        category_id = data.get('category') or data.get('donationType')
        amount = data.get('amount')
        
        # Validate required fields
        if not category_id or not amount:
            return Response({
                'success': False,
                'message': 'Category and amount are required',
                'data': None
            }, status=status.HTTP_400_BAD_REQUEST)
        
        # Get category
        try:
            category = GivingCategory.objects.get(
                id=category_id, 
                church=user.church, 
                is_active=True
            )
        except GivingCategory.DoesNotExist:
            return Response({
                'success': False,
                'message': 'Invalid category',
                'data': None
            }, status=status.HTTP_400_BAD_REQUEST)
        
        # Create transaction
        giving_tx = GivingTransaction.objects.create(
            member           = member,
            church           = user.church,
            category         = category,
            amount           = data['amount'],
            payment_method   = _normalize_payment_method(
                                    data.get('payment_method') or data.get('paymentMethod') or 'mobile_money'),
            transaction_type = 'one_time',
            status           = 'pending',
            transaction_date = timezone.now(),          # DateTimeField — pass datetime, not date
            created_by       = user,
            updated_by       = user,
            notes            = data.get('note') or data.get('notes') or data.get('description') or '',
            is_anonymous     = bool(data.get('is_anonymous') or data.get('isAnonymous') or False),
        )

        logger.info(f"Created transaction {giving_tx.transaction_id} for user {user.email}")

        response_data = {
            'success': True,
            'message': 'Transaction created successfully',
            'data': GivingTransactionSerializer(giving_tx).data,
        }

        # ── Paystack initialization — always run for all payment methods ──
        # All mobile payments go through Paystack (card, mobile money via Paystack, etc.)
        try:
            from payments.paystack_service import PaystackService
            import uuid as _uuid
            svc = PaystackService()
            ref = f"AF-{_uuid.uuid4().hex[:12].upper()}"
            giving_tx.payment_reference = ref
            giving_tx.save(update_fields=['payment_reference'])

            callback_url = data.get('callback_url',
                'https://backend.sanctum.co.ke/api/payments/paystack/callback/')

            result = svc.initialize_payment(
                email        = user.email,
                amount       = giving_tx.amount,
                reference    = ref,
                metadata     = {
                    'user_id':        user.id,
                    'church_id':      user.church.id if user.church else None,
                    'transaction_id': str(giving_tx.transaction_id),
                    'category':       category.name,
                },
                callback_url = callback_url,
            )

            if result.get('success'):
                response_data['authorization_url'] = result['authorization_url']
                response_data['access_code']       = result['access_code']
                response_data['payment_reference'] = ref
                # Also embed in the serialized transaction data
                response_data['data']['payment_reference'] = ref
                logger.info(f"Paystack init OK for {giving_tx.transaction_id}: {ref}")
            else:
                logger.error(
                    f"Paystack init failed for {giving_tx.transaction_id}: {result.get('message')}")
                response_data['warning'] = 'Payment gateway could not be initialized — try again.'
        except Exception as paystack_exc:
            logger.error(f"Paystack initialization exception: {paystack_exc}", exc_info=True)
            response_data['warning'] = 'Payment gateway error — please retry.'

        return Response(response_data, status=status.HTTP_201_CREATED)

    except Exception as e:
        logger.error(f"Error creating giving transaction: {str(e)}", exc_info=True)
        return Response({
            'success': False,
            'message': f'Failed to create transaction: {str(e)}',
            'data': None
        }, status=status.HTTP_500_INTERNAL_SERVER_ERROR)


@api_view(['POST'])
@permission_classes([IsAuthenticated])
def retry_giving_payment(request, transaction_id):
    """
    POST /api/giving/transactions/<transaction_id>/retry-payment/
    Re-initializes Paystack for a pending transaction so the user can
    complete payment from the GivingFragment "Complete Payment" button.
    """
    try:
        user = request.user
        member = user.member_profile

        giving_tx = GivingTransaction.objects.get(
            transaction_id=transaction_id,
            member=member,
            status='pending'
        )

        from payments.paystack_service import PaystackService
        import uuid as _uuid

        svc = PaystackService()
        ref = f"AF-{_uuid.uuid4().hex[:12].upper()}"
        giving_tx.payment_reference = ref
        giving_tx.save(update_fields=['payment_reference'])

        result = svc.initialize_payment(
            email        = user.email,
            amount       = giving_tx.amount,
            reference    = ref,
            metadata     = {
                'user_id':        user.id,
                'church_id':      user.church.id if user.church else None,
                'transaction_id': str(giving_tx.transaction_id),
                'category':       giving_tx.category.name,
            },
            callback_url = f"https://backend.sanctum.co.ke/api/payments/paystack/callback/",
        )

        if result.get('success'):
            return Response({
                'success':           True,
                'authorization_url': result['authorization_url'],
                'access_code':       result['access_code'],
                'payment_reference': ref,
                'transaction_id':    str(giving_tx.transaction_id),
            }, status=status.HTTP_200_OK)
        else:
            return Response({
                'success': False,
                'message': result.get('message', 'Paystack initialization failed'),
            }, status=status.HTTP_502_BAD_GATEWAY)

    except GivingTransaction.DoesNotExist:
        return Response({'success': False, 'message': 'Pending transaction not found.'},
                        status=status.HTTP_404_NOT_FOUND)
    except Exception as e:
        logger.error(f"retry_giving_payment error: {e}", exc_info=True)
        return Response({'success': False, 'message': str(e)},
                        status=status.HTTP_500_INTERNAL_SERVER_ERROR)


class GivingCategoryViewSet(viewsets.ModelViewSet):
    queryset = GivingCategory.objects.all()
    serializer_class = GivingCategorySerializer
    permission_classes = [IsAuthenticated]
    
    def get_queryset(self):
        """Filter active categories for user's church"""
        user = self.request.user
        if user.role == 'system_admin':
            return GivingCategory.objects.filter(is_active=True)
        else:
            return GivingCategory.objects.filter(church=user.church, is_active=True)


class GivingTransactionViewSet(viewsets.ModelViewSet):
    serializer_class = GivingTransactionSerializer
    permission_classes = [IsAuthenticated, IsOwnerOrChurchAdmin]
    queryset = GivingTransaction.objects.all()
    
    def get_queryset(self):
        """Filter giving transactions based on user role"""
        user = self.request.user
        
        # System admins see all transactions
        if user.role == 'system_admin':
            return GivingTransaction.objects.all()
        
        # Church admins see their church's transactions
        if user.role in ['pastor', 'treasurer', 'auditor']:
            return GivingTransaction.objects.filter(church=user.church)
        
        # Members see only their own transactions
        return GivingTransaction.objects.filter(member__user=user)
    
    def perform_create(self, serializer):
        """Create giving transaction and link with payment"""
        serializer.save(member=self.request.user.member_profile)
    
    @action(detail=False, methods=['get'], permission_classes=[IsAuthenticated])
    def history(self, request):
        """Get user's giving history"""
        try:
            user = request.user
            
            # Get query parameters
            start_date = request.query_params.get('start_date')
            end_date = request.query_params.get('end_date')
            giving_type = request.query_params.get('giving_type')
            church_id = request.query_params.get('church_id')
            
            # Base query
            givings = GivingTransaction.objects.filter(member__user=user)
            
            # Apply filters
            if start_date:
                givings = givings.filter(transaction_date__gte=start_date)
            if end_date:
                givings = givings.filter(transaction_date__lte=end_date)
            if giving_type:
                givings = givings.filter(category__name=giving_type)
            if church_id:
                givings = givings.filter(church_id=church_id)
            
            # Order by date
            givings = givings.order_by('-transaction_date')
            
            # Calculate totals
            total_given = givings.aggregate(total=Sum('amount'))['total'] or Decimal('0.00')
            
            # Serialize data
            serializer = self.get_serializer(givings, many=True)
            
            return Response({
                'success': True,
                'data': {
                    'total_given': float(total_given),
                    'transaction_count': givings.count(),
                    'givings': serializer.data
                }
            }, status=status.HTTP_200_OK)
            
        except Exception as e:
            logger.error(f"Error fetching giving history: {str(e)}")
            return Response({
                'success': False,
                'message': 'Failed to fetch giving history'
            }, status=status.HTTP_500_INTERNAL_SERVER_ERROR)
    
    @action(detail=False, methods=['get'], permission_classes=[IsAuthenticated])
    def summary(self, request):
        """Get giving summary statistics"""
        try:
            user = request.user
            
            # Get givings for the current year
            current_year = timezone.now().year
            givings = GivingTransaction.objects.filter(
                member__user=user,
                transaction_date__year=current_year,
                status='completed'
            )
            
            # Calculate totals by category
            by_category = givings.values('category__name').annotate(
                total=Sum('amount'),
                count=Count('id')
            )
            
            # Calculate monthly totals
            monthly_totals = []
            for month in range(1, 13):
                month_givings = givings.filter(transaction_date__month=month)
                total = month_givings.aggregate(total=Sum('amount'))['total'] or Decimal('0.00')
                monthly_totals.append({
                    'month': month,
                    'total': float(total),
                    'count': month_givings.count()
                })
            
            # Overall totals
            total_given = givings.aggregate(total=Sum('amount'))['total'] or Decimal('0.00')
            
            return Response({
                'success': True,
                'data': {
                    'year': current_year,
                    'total_given': float(total_given),
                    'transaction_count': givings.count(),
                    'by_category': list(by_category),
                    'monthly_totals': monthly_totals
                }
            }, status=status.HTTP_200_OK)
            
        except Exception as e:
            logger.error(f"Error fetching giving summary: {str(e)}")
            return Response({
                'success': False,
                'message': 'Failed to fetch giving summary'
            }, status=status.HTTP_500_INTERNAL_SERVER_ERROR)


class RecurringGivingViewSet(viewsets.ModelViewSet):
    serializer_class = RecurringGivingSerializer
    permission_classes = [IsAuthenticated]
    queryset = RecurringGiving.objects.all()
    
    def get_queryset(self):
        """Filter recurring givings by user"""
        user = self.request.user
        
        if user.role == 'system_admin':
            return RecurringGiving.objects.all()
        
        return RecurringGiving.objects.filter(member__user=user)
    
    def perform_create(self, serializer):
        """Create recurring giving"""
        serializer.save(member=self.request.user.member_profile)


class PledgeViewSet(viewsets.ModelViewSet):
    serializer_class = PledgeSerializer
    permission_classes = [IsAuthenticated]
    queryset = Pledge.objects.all()
    
    def get_queryset(self):
        """Filter pledges by user"""
        user = self.request.user
        
        if user.role == 'system_admin':
            return Pledge.objects.all()
        
        if user.role in ['pastor', 'treasurer', 'auditor']:
            return Pledge.objects.filter(church=user.church)
        
        return Pledge.objects.filter(member__user=user)
    
    def perform_create(self, serializer):
        """Create pledge"""
        serializer.save(member=self.request.user.member_profile)


class GivingCampaignViewSet(viewsets.ModelViewSet):
    serializer_class = GivingCampaignSerializer
    permission_classes = [IsAuthenticated]
    queryset = GivingCampaign.objects.all()
    
    def get_queryset(self):
        """Filter campaigns by church"""
        user = self.request.user
        
        if user.role == 'system_admin':
            return GivingCampaign.objects.all()
        
        if user.role in ['pastor', 'treasurer', 'auditor']:
            return GivingCampaign.objects.filter(church=user.church)
        
        # Members see active campaigns from their church
        return GivingCampaign.objects.filter(
            church=user.church,
            is_active=True
        )


@api_view(['GET'])
@permission_classes([IsChurchAdmin])
def church_givings(request, church_id):
    """Get givings for a specific church (Church Admin only)"""
    try:
        user = request.user
        
        # Verify user has access to this church
        if user.role != 'system_admin' and user.church_id != church_id:
            return Response({
                'success': False,
                'message': 'You can only view givings for your own church'
            }, status=status.HTTP_403_FORBIDDEN)
        
        # Get query parameters
        start_date = request.query_params.get('start_date')
        end_date = request.query_params.get('end_date')
        giving_type = request.query_params.get('giving_type')
        
        # Base query — GivingTransaction links to church via member.user.church
        # Use member__user__church_id to avoid AttributeError if church_id is on User
        givings = GivingTransaction.objects.filter(
            member__user__church_id=church_id,
            status='completed'
        )
        
        # Apply filters
        if start_date:
            givings = givings.filter(transaction_date__gte=start_date)
        if end_date:
            givings = givings.filter(transaction_date__lte=end_date)
        if giving_type:
            givings = givings.filter(category__name=giving_type)
        
        # Calculate totals
        total_received = givings.aggregate(total=Sum('amount'))['total'] or Decimal('0.00')
        
        # Group by category
        by_category = givings.values('category__name').annotate(
            total=Sum('amount'),
            count=Count('id')
        )
        
        # Recent givings
        recent_givings = givings.select_related('member__user', 'category').order_by('-transaction_date')[:10]
        
        giving_data = []
        for giving in recent_givings:
            try:
                member_name = (
                    giving.member.user.get_full_name()
                    if not giving.is_anonymous and giving.member and giving.member.user
                    else 'Anonymous'
                )
            except Exception:
                member_name = 'Unknown'
            giving_data.append({
                'id': giving.id,
                'amount': float(giving.amount),
                'member': member_name,
                'category': giving.category.name if giving.category else 'General',
                'date': giving.transaction_date,
                'payment_method': giving.payment_method,
                'status': giving.status
            })
        
        return Response({
            'success': True,
            'data': {
                'total_received': float(total_received),
                'transaction_count': givings.count(),
                'by_category': list(by_category),
                'recent_givings': giving_data
            }
        }, status=status.HTTP_200_OK)
        
    except Exception as e:
        logger.error(f"Error fetching church givings: {str(e)}")
        return Response({
            'success': False,
            'message': 'Failed to fetch church givings'
        }, status=status.HTTP_500_INTERNAL_SERVER_ERROR)
