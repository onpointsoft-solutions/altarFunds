"""
Auto-Reconciliation Service for Payment Processing
Automatically matches payments with bank records and handles reconciliation
"""

import logging
from datetime import datetime, timedelta
from django.utils import timezone
from django.db import transaction
from decimal import Decimal
from .models import PaymentRequest, PaymentReconciliation, PaymentDetail
from common.services import AuditService

logger = logging.getLogger(__name__)


class AutoReconciliationService:
    """Service for automatic payment reconciliation"""
    
    @staticmethod
    def run_daily_reconciliation():
        """Run daily reconciliation for all pending payments"""
        try:
            # Get all completed payment requests without reconciliation
            pending_payments = PaymentRequest.objects.filter(
                status='completed',
                paymentreconciliation__isnull=True
            ).select_related('user', 'paystack_account')
            
            reconciled_count = 0
            for payment_request in pending_payments:
                try:
                    result = AutoReconciliationService.reconcile_payment(payment_request)
                    if result['success']:
                        reconciled_count += 1
                        logger.info(f"Auto-reconciled payment: {payment_request.transaction_reference}")
                except Exception as e:
                    logger.error(f"Error reconciling payment {payment_request.id}: {str(e)}")
            
            logger.info(f"Daily reconciliation completed. Reconciled {reconciled_count} payments.")
            return {
                'success': True,
                'reconciled_count': reconciled_count,
                'total_pending': pending_payments.count()
            }
            
        except Exception as e:
            logger.error(f"Daily reconciliation failed: {str(e)}")
            return {'success': False, 'error': str(e)}
    
    @staticmethod
    @transaction.atomic
    def reconcile_payment(payment_request):
        """Reconcile a single payment request"""
        try:
            # Create reconciliation record
            reconciliation = PaymentReconciliation.objects.create(
                payment_request=payment_request,
                reconciliation_status='pending'
            )
            
            # Try to match with bank records (simulated)
            match_result = AutoReconciliationService._match_with_bank_records(payment_request)
            
            # Update reconciliation with match results
            reconciliation.bank_transaction_id = match_result.get('transaction_id')
            reconciliation.bank_reference = match_result.get('reference')
            reconciliation.matched_amount = match_result.get('matched_amount')
            reconciliation.confidence_score = match_result.get('confidence_score', 0)
            reconciliation.reconciliation_notes = match_result.get('notes', '')
            
            # Set status based on confidence score
            if match_result.get('confidence_score', 0) >= 80:
                reconciliation.reconciliation_status = 'matched'
                reconciliation.matched_date = timezone.now()
            elif match_result.get('confidence_score', 0) >= 50:
                reconciliation.reconciliation_status = 'manual_review'
            else:
                reconciliation.reconciliation_status = 'unmatched'
            
            reconciliation.save()
            
            # Log reconciliation
            AuditService.log_user_action(
                user=payment_request.user,
                action='PAYMENT_RECONCILIATION',
                details={
                    'payment_request_id': payment_request.id,
                    'transaction_reference': payment_request.transaction_reference,
                    'reconciliation_status': reconciliation.reconciliation_status,
                    'confidence_score': reconciliation.confidence_score,
                    'matched_amount': float(reconciliation.matched_amount or 0)
                }
            )
            
            return {
                'success': True,
                'reconciliation_id': reconciliation.id,
                'status': reconciliation.reconciliation_status,
                'confidence_score': reconciliation.confidence_score
            }
            
        except Exception as e:
            logger.error(f"Error reconciling payment {payment_request.id}: {str(e)}")
            return {'success': False, 'error': str(e)}
    
    @staticmethod
    def _match_with_bank_records(payment_request):
        """Simulate matching with bank records"""
        # This would integrate with actual bank APIs in production
        # For now, we'll simulate the matching process
        
        amount = payment_request.amount
        reference = payment_request.transaction_reference
        date_created = payment_request.created_at
        
        # Simulate bank record lookup
        confidence_score = 85  # Default high confidence for completed payments
        matched_amount = amount
        transaction_id = f"BANK_{reference}"
        bank_reference = reference
        
        # Add some randomness for demo
        import random
        if random.random() < 0.1:  # 10% chance of low confidence
            confidence_score = random.randint(30, 60)
            matched_amount = amount * Decimal('0.95')  # Slight amount difference
            bank_reference = f"ADJUSTED_{reference}"
        
        notes = f"Auto-matched with bank records. Confidence: {confidence_score}%"
        
        return {
            'transaction_id': transaction_id,
            'reference': bank_reference,
            'matched_amount': matched_amount,
            'confidence_score': confidence_score,
            'notes': notes
        }
    
    @staticmethod
    def get_reconciliation_summary(church=None):
        """Get reconciliation summary statistics"""
        queryset = PaymentReconciliation.objects.all()
        
        if church:
            queryset = queryset.filter(payment_request__paystack_account__church=church)
        
        total_payments = queryset.count()
        pending_reconciliation = queryset.filter(reconciliation_status='pending').count()
        matched_payments = queryset.filter(reconciliation_status='matched').count()
        unmatched_payments = queryset.filter(reconciliation_status='unmatched').count()
        manual_review_required = queryset.filter(reconciliation_status='manual_review').count()
        
        # Calculate total amount reconciled
        total_amount_reconciled = queryset.filter(
            reconciliation_status='matched'
        ).aggregate(
            total=models.Sum('matched_amount')
        )['total'] or Decimal('0')
        
        # Calculate average confidence score
        avg_confidence = queryset.filter(
            confidence_score__gt=0
        ).aggregate(
            avg=models.Avg('confidence_score')
        )['avg'] or 0
        
        return {
            'total_payments': total_payments,
            'pending_reconciliation': pending_reconciliation,
            'matched_payments': matched_payments,
            'unmatched_payments': unmatched_payments,
            'manual_review_required': manual_review_required,
            'total_amount_reconciled': total_amount_reconciled,
            'average_confidence_score': avg_confidence
        }


class PaymentDetailsService:
    """Service for managing payment details"""
    
    @staticmethod
    @transaction.atomic
    def create_payment_detail(payment_request_id, detail_data, user):
        """Create payment detail record"""
        try:
            payment_request = PaymentRequest.objects.get(id=payment_request_id)
            
            # Create payment detail
            payment_detail = PaymentDetail.objects.create(
                payment_request=payment_request,
                detail_type=detail_data.get('detail_type'),
                bank_name=detail_data.get('bank_name', ''),
                account_number=detail_data.get('account_number', ''),
                transaction_id=detail_data.get('transaction_id', ''),
                reference_number=detail_data.get('reference_number', ''),
                actual_amount=detail_data.get('actual_amount'),
                transaction_date=detail_data.get('transaction_date'),
                settlement_date=detail_data.get('settlement_date'),
                payer_name=detail_data.get('payer_name', ''),
                payer_account=detail_data.get('payer_account', ''),
                notes=detail_data.get('notes', ''),
                receipt_image=detail_data.get('receipt_image'),
                supporting_document=detail_data.get('supporting_document')
            )
            
            # Log creation
            AuditService.log_user_action(
                user=user,
                action='PAYMENT_DETAIL_CREATED',
                details={
                    'payment_request_id': payment_request_id,
                    'detail_type': payment_detail.detail_type,
                    'actual_amount': float(payment_detail.actual_amount),
                    'transaction_id': payment_detail.transaction_id
                }
            )
            
            return {
                'success': True,
                'payment_detail_id': payment_detail.id,
                'message': 'Payment detail created successfully'
            }
            
        except PaymentRequest.DoesNotExist:
            return {'success': False, 'error': 'Payment request not found'}
        except Exception as e:
            logger.error(f"Error creating payment detail: {str(e)}")
            return {'success': False, 'error': str(e)}
    
    @staticmethod
    @transaction.atomic
    def verify_payment_detail(payment_detail_id, user):
        """Verify a payment detail"""
        try:
            payment_detail = PaymentDetail.objects.get(id=payment_detail_id)
            
            # Update verification
            payment_detail.is_verified = True
            payment_detail.verified_by = user
            payment_detail.verified_at = timezone.now()
            payment_detail.save()
            
            # Log verification
            AuditService.log_user_action(
                user=user,
                action='PAYMENT_DETAIL_VERIFIED',
                details={
                    'payment_detail_id': payment_detail_id,
                    'payment_request_id': payment_detail.payment_request.id,
                    'actual_amount': float(payment_detail.actual_amount)
                }
            )
            
            return {
                'success': True,
                'message': 'Payment detail verified successfully'
            }
            
        except PaymentDetail.DoesNotExist:
            return {'success': False, 'error': 'Payment detail not found'}
        except Exception as e:
            logger.error(f"Error verifying payment detail: {str(e)}")
            return {'success': False, 'error': str(e)}
    
    @staticmethod
    def get_payment_details_for_church(church):
        """Get all payment details for a church"""
        return PaymentDetail.objects.filter(
            payment_request__paystack_account__church=church
        ).select_related('payment_request', 'verified_by').order_by('-created_at')
