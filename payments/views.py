from rest_framework import viewsets, status
from rest_framework.decorators import action, api_view, permission_classes
from rest_framework.response import Response
from rest_framework.permissions import IsAuthenticated, AllowAny
from django.shortcuts import get_object_or_404
from django.views.decorators.csrf import csrf_exempt
from django.utils.decorators import method_decorator
from django.utils import timezone
from .models import PaymentRequest, Payment, Transaction, PaystackAccount
from .serializers import (
    PaymentRequestSerializer, PaymentSerializer, TransactionSerializer,
    PaystackAccountSerializer, ChurchAccountListSerializer
)
from .paystack_service import paystack_service, paystack_transfer_service
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
        """Verify Paystack payment and mark GivingTransaction complete"""
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
                paystack_status = result.get('status', '')

                # ── Update Payment record ─────────────────────────────────
                try:
                    payment = Payment.objects.get(reference=reference)
                    if paystack_status == 'success':
                        payment.status = 'completed'
                        payment.paid_at = timezone.now()
                        payment.transaction_id = result.get('reference')
                        payment.save()
                except Payment.DoesNotExist:
                    pass  # not every flow creates a Payment row

                # ── Update GivingTransaction record ───────────────────────
                from giving.models import GivingTransaction as GivingTx
                giving_tx = GivingTx.objects.filter(
                    payment_reference=reference
                ).first()
                if giving_tx and paystack_status == 'success' and giving_tx.status != 'completed':
                    giving_tx.mark_completed(payment_reference=reference)

                return Response({
                    'success': True,
                    'data': {
                        'status': paystack_status,
                        'amount': result.get('amount'),
                        'reference': reference,
                        'paid_at': result.get('paid_at')
                    }
                }, status=status.HTTP_200_OK)
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

        logger.info(f"Paystack webhook received: {event_type}")

        # ── Route event to the correct handler ──────────────────────────
        if event_type == 'charge.success':
            # 1. Mark the payment as complete (existing logic)
            result = paystack_service.process_webhook(event_type, event_data)

            # 2. Initiate disbursement to church via Paystack Transfer
            reference = event_data.get('reference', '')
            _trigger_disbursement_for_charge(reference, event_data)

        elif event_type in ('transfer.success',):
            result = paystack_transfer_service.handle_transfer_success(event_data)

        elif event_type in ('transfer.failed', 'transfer.reversed'):
            result = paystack_transfer_service.handle_transfer_failed(event_data)

        else:
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


class PaystackAccountViewSet(viewsets.ModelViewSet):
    """ViewSet for Paystack account management"""
    serializer_class = PaystackAccountSerializer
    permission_classes = [IsAuthenticated]
    
    def get_queryset(self):
        """Filter accounts based on user role"""
        user = self.request.user
        
        # System admins see all accounts
        if user.role == 'system_admin':
            return PaystackAccount.objects.all()
        
        # Church admins see their church's accounts
        if user.role in ['pastor', 'treasurer', 'auditor']:
            return PaystackAccount.objects.filter(church=user.church)
        
        # Members see no accounts
        return PaystackAccount.objects.none()
    
    def perform_create(self, serializer):
        """Create Paystack account with church context"""
        user = self.request.user
        
        # Only church admins can create accounts
        if user.role not in ['pastor', 'treasurer', 'auditor', 'system_admin']:
            raise PermissionError("Only church administrators can create Paystack accounts")
        
        # Set church for non-system admins
        if user.role != 'system_admin':
            serializer.save(church=user.church)
        else:
            serializer.save()
    
    @action(detail=True, methods=['post'])
    def set_default(self, request, pk=None):
        """Set account as default for the church"""
        account = self.get_object()
        
        # Check permissions
        user = request.user
        if user.role not in ['pastor', 'treasurer', 'auditor', 'system_admin']:
            return Response(
                {'error': 'Only church administrators can set default accounts'},
                status=status.HTTP_403_FORBIDDEN
            )
        
        # Check if user can manage this church's account
        if user.role != 'system_admin' and account.church != user.church:
            return Response(
                {'error': 'You can only manage your own church accounts'},
                status=status.HTTP_403_FORBIDDEN
            )
        
        # Set as default
        PaystackAccount.objects.filter(church=account.church, is_default=True).update(is_default=False)
        account.is_default = True
        account.save()
        
        return Response({'status': 'success', 'message': 'Account set as default'})


@api_view(['GET'])
@permission_classes([IsAuthenticated])
def get_church_accounts(request):
    """Get available Paystack accounts for the user's church"""
    user = request.user
    
    if not hasattr(user, 'church') or not user.church:
        return Response(
            {'error': 'No church associated with user'},
            status=status.HTTP_400_BAD_REQUEST
        )
    
    accounts = PaystackAccount.objects.filter(
        church=user.church,
        is_active=True
    ).order_by('-is_default', 'account_name')
    
    account_data = []
    for account in accounts:
        account_data.append({
            'id': account.id,
            'account_name': account.account_name,
            'account_code': account.account_code,
            'account_type': account.account_type,
            'is_default': account.is_default,
            'business_number': account.business_number
        })
    
    return Response({
        'church_name': user.church.name,
        'accounts': account_data
    })


@api_view(['POST'])
@permission_classes([IsAuthenticated])
def create_paystack_account(request):
    """Create a new Paystack account for the church"""
    user = request.user
    
    # Check permissions
    if user.role not in ['pastor', 'treasurer', 'auditor', 'system_admin']:
        return Response(
            {'error': 'Only church administrators can create Paystack accounts'},
            status=status.HTTP_403_FORBIDDEN
        )
    
    try:
        data = request.data
        
        # Validate required fields
        required_fields = ['account_name', 'account_code', 'account_type', 'paystack_secret_key', 'paystack_public_key']
        for field in required_fields:
            if not data.get(field):
                return Response(
                    {'error': f'{field} is required'},
                    status=status.HTTP_400_BAD_REQUEST
                )
        
        # Create account
        account_data = {
            'account_name': data['account_name'],
            'account_code': data['account_code'],
            'account_type': data['account_type'],
            'paystack_secret_key': data['paystack_secret_key'],
            'paystack_public_key': data['paystack_public_key'],
            'business_number': data.get('business_number', ''),
            'is_active': True,
            'is_default': data.get('is_default', False)
        }
        
        # Set church for non-system admins
        if user.role != 'system_admin':
            account_data['church'] = user.church
        
        # Create the account
        account = PaystackAccount.objects.create(**account_data)
        
        return Response({
            'success': True,
            'message': 'Paystack account created successfully',
            'account': {
                'id': account.id,
                'account_name': account.account_name,
                'account_code': account.account_code,
                'account_type': account.account_type,
                'is_default': account.is_default
            }
        }, status=status.HTTP_201_CREATED)
        
    except Exception as e:
        logger.error(f"Error creating Paystack account: {str(e)}")
        return Response(
            {'error': 'Failed to create account', 'details': str(e)},
            status=status.HTTP_500_INTERNAL_SERVER_ERROR
        )


@api_view(['POST'])
@permission_classes([IsAuthenticated])
def initiate_giving_with_account(request):
    """Initiate giving with Paystack account selection"""
    user = request.user
    
    try:
        data = request.data
        
        # Validate required fields
        required_fields = ['amount', 'category_id', 'paystack_account_id']
        for field in required_fields:
            if not data.get(field):
                return Response(
                    {'error': f'{field} is required'},
                    status=status.HTTP_400_BAD_REQUEST
                )
        
        # Get member
        from accounts.models import Member
        try:
            member = Member.objects.get(user=user)
        except Member.DoesNotExist:
            return Response(
                {'error': 'Member profile not found'},
                status=status.HTTP_404_NOT_FOUND
            )
        
        # Get category
        from giving.models import GivingCategory
        try:
            category = GivingCategory.objects.get(id=data['category_id'], church=member.church)
        except GivingCategory.DoesNotExist:
            return Response(
                {'error': 'Invalid category'},
                status=status.HTTP_400_BAD_REQUEST
            )
        
        # Get Paystack account
        try:
            paystack_account = PaystackAccount.objects.get(
                id=data['paystack_account_id'],
                church=member.church,
                is_active=True
            )
        except PaystackAccount.DoesNotExist:
            return Response(
                {'error': 'Invalid or inactive Paystack account'},
                status=status.HTTP_400_BAD_REQUEST
            )
        
        # Create giving transaction
        from giving.models import GivingTransaction
        transaction = GivingTransaction.objects.create(
            member=member,
            church=member.church,
            category=category,
            amount=data['amount'],
            notes=data.get('notes', ''),
            dedication=data.get('dedication', ''),
            is_anonymous=data.get('is_anonymous', False),
            payment_method='paystack',
            paystack_account=paystack_account,
            status='pending_payment'
        )
        
        # Initiate payment using Paystack service
        from .services import PaymentService
        payment_request = PaymentService.initiate_payment(
            transaction, 
            payment_method='paystack', 
            church_account=paystack_account
        )
        
        return Response({
            'success': True,
            'message': 'Payment initiated successfully',
            'transaction': {
                'transaction_id': str(transaction.transaction_id),
                'amount': float(transaction.amount),
                'category': category.name,
                'account_name': paystack_account.account_name,
                'account_type': paystack_account.account_type,
                'status': transaction.status
            },
            'payment_request': {
                'authorization_url': payment_request.authorization_url,
                'access_code': payment_request.access_code,
                'reference': payment_request.transaction_reference
            }
        }, status=status.HTTP_201_CREATED)
        
    except Exception as e:
        logger.error(f"Error initiating giving with account: {str(e)}")
        return Response(
            {'error': 'Failed to initiate giving', 'details': str(e)},
            status=status.HTTP_500_INTERNAL_SERVER_ERROR
        )


# ════════════════════════════════════════════════════════════════════════════
#  INTERNAL HELPER
# ════════════════════════════════════════════════════════════════════════════

def _trigger_disbursement_for_charge(reference, event_data):
    """
    After a successful Paystack charge, find the related GivingTransaction
    and initiate a Transfer to the church's bank account.

    Called inside the webhook view — runs synchronously.
    For high-volume production systems consider offloading to a Celery task.
    """
    try:
        # Find giving transaction by payment reference
        from giving.models import GivingTransaction

        giving_tx = (
            GivingTransaction.objects
            .select_related('church', 'category', 'member__user')
            .filter(payment_reference=reference)
            .first()
        )

        if not giving_tx:
            # Try looking up via the Payment model
            try:
                payment = Payment.objects.get(reference=reference)
                # Check if there's a giving transaction linked via metadata
                metadata = event_data.get('metadata', {})
                church_id = metadata.get('church_id')
                if church_id:
                    giving_tx = (
                        GivingTransaction.objects
                        .select_related('church', 'category', 'member__user')
                        .filter(
                            church_id=church_id,
                            status='completed',
                            disbursement_status='pending'
                        )
                        .order_by('-transaction_date')
                        .first()
                    )
            except Payment.DoesNotExist:
                pass

        if not giving_tx:
            logger.warning(
                f"No GivingTransaction found for charge reference '{reference}' — "
                "disbursement skipped."
            )
            return

        # Skip if already disbursed
        if giving_tx.disbursement_status == 'completed':
            return

        success, msg = paystack_transfer_service.initiate_transfer(giving_tx)
        if success:
            logger.info(f"Disbursement initiated for {reference}: {msg}")
        else:
            logger.error(f"Disbursement failed for {reference}: {msg}")

    except Exception as e:
        logger.error(f"_trigger_disbursement_for_charge error ({reference}): {e}")


# ════════════════════════════════════════════════════════════════════════════
#  ADMIN: TRANSFER RECIPIENT MANAGEMENT
# ════════════════════════════════════════════════════════════════════════════

@api_view(['POST'])
@permission_classes([IsAuthenticated])
def create_transfer_recipient(request):
    """
    Register a church's bank account as a Paystack Transfer Recipient.

    Must be called once per bank account before disbursements can be made.

    Request body:
        { "bank_account_id": <int> }

    The endpoint resolves the bank code from the stored bank name.
    Use GET /api/payments/list-banks/ to find the correct code if auto-
    resolution fails, then update the bank account's bank_name to match.
    """
    user = request.user

    if user.role not in ('pastor', 'treasurer', 'auditor', 'system_admin', 'church_admin', 'denomination_admin'):
        return Response(
            {'error': 'Only church administrators can register transfer recipients'},
            status=status.HTTP_403_FORBIDDEN,
        )

    bank_account_id = request.data.get('bank_account_id')
    if not bank_account_id:
        return Response(
            {'error': 'bank_account_id is required'},
            status=status.HTTP_400_BAD_REQUEST,
        )

    from churches.models import ChurchBankAccount
    try:
        bank_account = ChurchBankAccount.objects.select_related('church').get(
            id=bank_account_id
        )
    except ChurchBankAccount.DoesNotExist:
        return Response({'error': 'Bank account not found'}, status=status.HTTP_404_NOT_FOUND)

    # Non-system admins can only manage their own church
    if user.role != 'system_admin' and bank_account.church != user.church:
        return Response(
            {'error': 'You can only manage your own church accounts'},
            status=status.HTTP_403_FORBIDDEN,
        )

    result = paystack_transfer_service.create_transfer_recipient(bank_account)

    if result['success']:
        return Response({
            'success':        True,
            'message':        'Transfer recipient created successfully',
            'recipient_code': result['recipient_code'],
            'recipient_id':   result['recipient_id'],
        }, status=status.HTTP_201_CREATED)

    return Response(
        {'success': False, 'message': result['message']},
        status=status.HTTP_400_BAD_REQUEST,
    )


@api_view(['POST'])
@permission_classes([IsAuthenticated])
def verify_bank_account(request):
    """
    Verify that a bank account number belongs to the expected account holder
    before registering it as a Paystack recipient.

    Request body:
        { "account_number": "...", "bank_code": "..." }
    """
    account_number = request.data.get('account_number')
    bank_code      = request.data.get('bank_code')

    if not account_number or not bank_code:
        return Response(
            {'error': 'account_number and bank_code are required'},
            status=status.HTTP_400_BAD_REQUEST,
        )

    result = paystack_transfer_service.verify_bank_account(account_number, bank_code)
    if result['success']:
        return Response({'success': True, 'data': result})
    return Response({'success': False, 'message': result['message']}, status=status.HTTP_400_BAD_REQUEST)


@api_view(['GET'])
@permission_classes([IsAuthenticated])
def list_banks(request):
    """
    Return Paystack's list of supported banks and their codes.

    Query param:
        country=nigeria  (default)  or  country=kenya
    """
    country = request.query_params.get('country', 'nigeria')
    result  = paystack_transfer_service.list_banks(country)
    if result['success']:
        return Response({'success': True, 'banks': result['banks']})
    return Response({'success': False, 'message': result['message']}, status=status.HTTP_502_BAD_GATEWAY)


@api_view(['GET'])
@permission_classes([IsAuthenticated])
def get_disbursement_status(request, transaction_id):
    """
    Return the disbursement status for a specific giving transaction.

    URL: /api/payments/disbursement/<transaction_id>/
    """
    from giving.models import GivingTransaction, ChurchDisbursement
    import uuid as uuid_lib

    try:
        tx_uuid = uuid_lib.UUID(str(transaction_id))
        giving_tx = GivingTransaction.objects.get(transaction_id=tx_uuid)
    except (ValueError, GivingTransaction.DoesNotExist):
        return Response({'error': 'Transaction not found'}, status=status.HTTP_404_NOT_FOUND)

    # Permission check
    user = request.user
    if user.role not in ('system_admin', 'denomination_admin') and giving_tx.church != user.church:
        return Response({'error': 'Access denied'}, status=status.HTTP_403_FORBIDDEN)

    try:
        d = giving_tx.disbursement
        return Response({
            'success': True,
            'disbursement': {
                'status':            d.status,
                'gross_amount':      float(d.amount),
                'platform_fee':      float(d.platform_fee),
                'net_amount':        float(d.net_amount),
                'transfer_code':     d.transfer_code,
                'paystack_receipt':  d.paystack_receipt,
                'retry_count':       d.retry_count,
                'processed_at':      d.processed_at,
                'completed_at':      d.completed_at,
                'error_message':     d.error_message or None,
            },
        })
    except Exception:
        return Response({
            'success': True,
            'disbursement': {
                'status':   giving_tx.disbursement_status,
                'message':  'Disbursement record not yet created',
            },
        })
