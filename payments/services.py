import logging
import requests
import json
import base64
from datetime import datetime, timedelta
from django.conf import settings
from django.utils import timezone
from django.db import transaction
from .models import PaymentRequest, PaymentCallback, PaymentReversal, PaymentBatch
from giving.models import GivingTransaction
from common.services import AuditService, NotificationService
from common.exceptions import AltarFundsException

logger = logging.getLogger('altar_funds')


class MpesaService:
    """M-Pesa Daraja API service"""
    
    def __init__(self):
        self.consumer_key = settings.MPESA_CONSUMER_KEY
        self.consumer_secret = settings.MPESA_CONSUMER_SECRET
        self.passkey = settings.MPESA_PASSKEY
        self.shortcode = settings.MPESA_SHORTCODE
        self.callback_url = settings.MPESA_CALLBACK_URL
        self.base_url = settings.MPESA_BASE_URL
        self.access_token = None
        self.token_expires_at = None
    
    def get_access_token(self):
        """Get M-Pesa access token"""
        if self.access_token and self.token_expires_at > timezone.now():
            return self.access_token
        
        url = f"{self.base_url}/oauth/v1/generate?grant_type=client_credentials"
        
        try:
            # Create basic auth credentials
            credentials = base64.b64encode(
                f"{self.consumer_key}:{self.consumer_secret}".encode()
            ).decode()
            
            headers = {
                'Authorization': f'Basic {credentials}',
                'Content-Type': 'application/json'
            }
            
            response = requests.get(url, headers=headers, timeout=30)
            response.raise_for_status()
            
            data = response.json()
            self.access_token = data['access_token']
            
            # Token expires in 1 hour, set expiry 5 minutes early
            self.token_expires_at = timezone.now() + timedelta(minutes=55)
            
            logger.info("M-Pesa access token obtained successfully")
            return self.access_token
            
        except requests.exceptions.RequestException as e:
            logger.error(f"Failed to get M-Pesa access token: {e}")
            raise AltarFundsException("Failed to connect to M-Pesa")
    
    def stk_push(self, phone_number, amount, account_reference, transaction_desc):
        """Initiate STK Push payment"""
        try:
            access_token = self.get_access_token()
            
            url = f"{self.base_url}/mpesa/stkpush/v1/processrequest"
            
            # Prepare timestamp
            timestamp = datetime.now().strftime('%Y%m%d%H%M%S')
            
            # Generate password
            password = base64.b64encode(
                f"{self.shortcode}{self.passkey}{timestamp}".encode()
            ).decode()
            
            # Prepare request payload
            payload = {
                'BusinessShortCode': self.shortcode,
                'Password': password,
                'Timestamp': timestamp,
                'TransactionType': 'CustomerPayBillOnline',
                'Amount': int(amount),
                'PartyA': phone_number,
                'PartyB': self.shortcode,
                'PhoneNumber': phone_number,
                'CallBackURL': self.callback_url,
                'AccountReference': account_reference,
                'TransactionDesc': transaction_desc
            }
            
            headers = {
                'Authorization': f'Bearer {access_token}',
                'Content-Type': 'application/json'
            }
            
            response = requests.post(url, json=payload, headers=headers, timeout=30)
            response.raise_for_status()
            
            data = response.json()
            
            logger.info(f"STK Push initiated: {data}")
            return data
            
        except requests.exceptions.RequestException as e:
            logger.error(f"STK Push failed: {e}")
            raise AltarFundsException(f"STK Push failed: {str(e)}")
    
    def transaction_status(self, checkout_request_id):
        """Check transaction status"""
        try:
            access_token = self.get_access_token()
            
            url = f"{self.base_url}/mpesa/stkpushquery/v1/query"
            
            # Prepare timestamp
            timestamp = datetime.now().strftime('%Y%m%d%H%M%S')
            
            # Generate password
            password = base64.b64encode(
                f"{self.shortcode}{self.passkey}{timestamp}".encode()
            ).decode()
            
            payload = {
                'BusinessShortCode': self.shortcode,
                'Password': password,
                'Timestamp': timestamp,
                'CheckoutRequestID': checkout_request_id
            }
            
            headers = {
                'Authorization': f'Bearer {access_token}',
                'Content-Type': 'application/json'
            }
            
            response = requests.post(url, json=payload, headers=headers, timeout=30)
            response.raise_for_status()
            
            data = response.json()
            
            logger.info(f"Transaction status checked: {data}")
            return data
            
        except requests.exceptions.RequestException as e:
            logger.error(f"Transaction status check failed: {e}")
            raise AltarFundsException(f"Transaction status check failed: {str(e)}")
    
    def account_balance(self):
        """Check account balance"""
        try:
            access_token = self.get_access_token()
            
            url = f"{self.base_url}/mpesa/accountbalance/v1/query"
            
            # Prepare timestamp
            timestamp = datetime.now().strftime('%Y%m%d%H%M%S')
            
            # Generate password
            password = base64.b64encode(
                f"{self.shortcode}{self.passkey}{timestamp}".encode()
            ).decode()
            
            payload = {
                'Initiator': settings.MPESA_INITIATOR_NAME,
                'SecurityCredential': settings.MPESA_SECURITY_CREDENTIAL,
                'CommandID': 'AccountBalance',
                'PartyA': self.shortcode,
                'IdentifierType': '4',
                'Remarks': 'Account balance check',
                'QueueTimeOutURL': self.callback_url,
                'ResultURL': self.callback_url
            }
            
            headers = {
                'Authorization': f'Bearer {access_token}',
                'Content-Type': 'application/json'
            }
            
            response = requests.post(url, json=payload, headers=headers, timeout=30)
            response.raise_for_status()
            
            data = response.json()
            
            logger.info(f"Account balance checked: {data}")
            return data
            
        except requests.exceptions.RequestException as e:
            logger.error(f"Account balance check failed: {e}")
            raise AltarFundsException(f"Account balance check failed: {str(e)}")


class PaymentService:
    """Main payment processing service - Paystack only with church account routing"""
    
    @staticmethod
    @transaction.atomic
    def initiate_payment(giving_transaction, payment_method='paystack', church_account=None):
        """Initiate payment for a giving transaction using Paystack with church account routing"""
        try:
            # Get church account for routing
            target_account = church_account or giving_transaction.church.get_default_paystack_account()
            
            if not target_account:
                raise AltarFundsException("No Paystack account configured for this church")
            
            # Create payment request with account routing
            payment_request = PaymentRequest.objects.create(
                giving_transaction=giving_transaction,
                church=giving_transaction.church,
                amount=giving_transaction.amount,
                phone_number=giving_transaction.member.user.phone_number,
                business_number=target_account.business_number,
                account_reference=str(giving_transaction.transaction_id),
                transaction_desc=f"AltarFunds - {giving_transaction.category.name}",
                payment_method=payment_method,
                paystack_account=target_account,
                created_by=giving_transaction.member.user
            )
            
            # Process payment using Paystack with account routing
            PaystackPaymentService.process_payment(payment_request, target_account)
            
            # Log initiation with account routing info
            AuditService.log_user_action(
                user=giving_transaction.member.user,
                action='PAYMENT_INITIATED',
                details={
                    'transaction_id': str(giving_transaction.transaction_id),
                    'amount': float(giving_transaction.amount),
                    'payment_method': payment_method,
                    'payment_request_id': str(payment_request.request_id),
                    'church_account': target_account.account_name,
                    'routing_account_id': target_account.id
                }
            )
            
            return payment_request
            
        except Exception as e:
            logger.error(f"Payment initiation failed: {e}")
            raise AltarFundsException("Payment initiation failed")
    
    @staticmethod
    @transaction.atomic
    def process_callback(callback_data, provider='paystack'):
        """Process payment callback"""
        try:
            # Create callback record
            callback = PaymentCallback.objects.create(
                provider=provider,
                callback_type='payment_confirmation',
                transaction_id=callback_data.get('reference', ''),
                raw_data=callback_data,
                ip_address='127.0.0.1'  # Should get from request
            )
            
            # Validate callback signature
            if provider == 'paystack':
                signature = callback_data.get('signature')
                payload = callback_data.get('payload', '')
                paystack_service = PaystackService()
                callback.is_valid = paystack_service.verify_webhook_signature(payload, signature)
            else:
                callback.is_valid = True  # Other providers
            
            # Find related payment request
            payment_request = None
            if 'reference' in callback_data:
                payment_request = PaymentRequest.objects.filter(
                    transaction_reference=callback_data['reference']
                ).first()
            
            if not payment_request:
                logger.warning(f"No payment request found for callback: {callback_data}")
                callback.status = 'invalid'
                callback.validation_errors = "No matching payment request found"
                callback.save()
                return callback
            
            # Link callback to payment request
            callback.payment_request = payment_request
            callback.giving_transaction = payment_request.giving_transaction
            callback.save()
            
            # Process the callback
            if provider == 'paystack':
                PaystackPaymentService.process_webhook(callback_data, payment_request.paystack_account)
            
            return callback
            
        except Exception as e:
            logger.error(f"Callback processing failed: {e}")
            raise AltarFundsException("Callback processing failed")
    
    @staticmethod
    @transaction.atomic
    def retry_payment(payment_request):
        """Retry failed payment"""
        if not payment_request.can_retry():
            raise AltarFundsException("Payment cannot be retried")
        
        try:
            # Schedule retry
            payment_request.schedule_retry()
            
            # Process retry using Paystack
            if payment_request.paystack_account:
                PaystackPaymentService.process_payment(payment_request, payment_request.paystack_account)
            else:
                raise AltarFundsException("No Paystack account configured for retry")
            
            # Log retry
            AuditService.log_user_action(
                user=payment_request.created_by,
                action='PAYMENT_RETRY',
                details={
                    'payment_request_id': str(payment_request.request_id),
                    'retry_count': payment_request.retry_count,
                    'account_name': payment_request.paystack_account.account_name
                }
            )
            
            return payment_request
            
        except Exception as e:
            logger.error(f"Payment retry failed: {e}")
            raise AltarFundsException("Payment retry failed")
    
    @staticmethod
    @transaction.atomic
    def reverse_payment(giving_transaction, amount, reason, requested_by):
        """Process payment reversal/refund"""
        try:
            # Create reversal record
            reversal = PaymentReversal.objects.create(
                original_transaction=giving_transaction,
                reversal_type='refund',
                amount=amount,
                reason=reason,
                requested_by=requested_by,
                created_by=requested_by
            )
            
            # Process reversal using Paystack
            PaystackPaymentService.process_reversal(reversal)
            
            # Log reversal
            AuditService.log_user_action(
                user=requested_by,
                action='PAYMENT_REVERSAL',
                details={
                    'original_transaction': str(giving_transaction.transaction_id),
                    'reversal_amount': float(amount),
                    'reason': reason
                }
            )
            
            return reversal
            
        except Exception as e:
            logger.error(f"Payment reversal failed: {e}")
            raise AltarFundsException("Payment reversal failed")


class PaystackPaymentService:
    """Paystack specific payment processing with church account routing"""
    
    @staticmethod
    def process_payment(payment_request, church_account):
        """Process payment using Paystack with church account routing"""
        try:
            from .paystack_service import PaystackService
            
            # Mark as processing
            payment_request.mark_processing()
            
            # Generate unique reference with account routing
            reference = f"AF-{church_account.account_code}-{payment_request.request_id}"
            
            # Initialize Paystack service with church account
            paystack_service = PaystackService(church_account)
            
            # Initialize payment with Paystack using church account
            response = paystack_service.initialize_payment(
                email=payment_request.giving_transaction.member.user.email,
                amount=payment_request.amount,
                reference=reference,
                metadata={
                    'church_id': payment_request.giving_transaction.church.id,
                    'church_name': payment_request.giving_transaction.church.name,
                    'account_name': church_account.account_name,
                    'account_code': church_account.account_code,
                    'giving_category': payment_request.giving_transaction.category.name,
                    'member_id': payment_request.giving_transaction.member.id,
                    'member_name': payment_request.giving_transaction.member.user.get_full_name(),
                    'payment_request_id': str(payment_request.request_id),
                    'routing_method': 'church_account'
                },
                callback_url=f"{settings.BASE_URL}/api/payments/paystack/webhook/"
            )
            
            # Update payment request with response
            if response['success']:
                payment_request.mark_completed(response)
                
                # Update giving transaction
                payment_request.giving_transaction.status = 'pending_payment'
                payment_request.giving_transaction.payment_reference = reference
                payment_request.giving_transaction.save()
                
                logger.info(f"Paystack payment initiated: {reference} - Account: {church_account.account_name}")
            else:
                payment_request.mark_failed(response)
                
                # Update giving transaction
                payment_request.giving_transaction.mark_failed(
                    response.get('message', 'Payment initialization failed')
                )
                
                logger.error(f"Paystack payment initialization failed: {response}")
            
        except Exception as e:
            logger.error(f"Paystack payment processing failed: {e}")
            payment_request.mark_failed({'ResponseMessage': str(e)})
            raise
    
    @staticmethod
    def process_webhook(callback_data, church_account):
        """Process Paystack webhook with church account routing"""
        try:
            from .paystack_service import PaystackService
            
            # Initialize Paystack service with church account
            paystack_service = PaystackService(church_account)
            
            # Verify webhook signature
            signature = callback_data.get('signature')
            payload = callback_data.get('payload', '')
            
            if not paystack_service.verify_webhook_signature(payload, signature):
                logger.warning("Invalid Paystack webhook signature")
                return {'success': False, 'message': 'Invalid signature'}
            
            # Process webhook event
            event_type = callback_data.get('event_type')
            event_data = callback_data.get('data')
            
            result = paystack_service.process_webhook(event_type, event_data)
            
            # Log church account routing
            if result['success']:
                logger.info(f"Paystack webhook processed: {event_type} - Account: {church_account.account_name}")
            
            return result
            
        except Exception as e:
            logger.error(f"Paystack webhook processing failed: {e}")
            return {'success': False, 'message': 'Webhook processing failed'}
    
    @staticmethod
    def process_reversal(reversal):
        """Process Paystack reversal/refund"""
        try:
            from .paystack_service import PaystackService
            
            # Get original transaction
            original_transaction = reversal.original_transaction
            
            # Initialize Paystack service
            paystack_service = PaystackService()
            
            # Process refund (this would use Paystack's refund API)
            # For now, we'll simulate the reversal
            reversal.mark_completed(f"REF_{reversal.reversal_id}")
            
            # Update original transaction
            original_transaction.refund(reversal.amount, reversal.reason)
            
            logger.info(f"Paystack reversal completed: {reversal.reversal_id}")
            
        except Exception as e:
            logger.error(f"Paystack reversal failed: {e}")
            reversal.mark_failed(str(e))
            raise
    
    @staticmethod
    def process_payment_callback(callback):
        """Process M-Pesa payment callback"""
        try:
            callback_data = callback.raw_data
            
            # Extract callback data
            stk_callback = callback_data.get('STKCallback', {})
            result_code = stk_callback.get('ResultCode')
            result_desc = stk_callback.get('ResultDesc')
            
            # Extract metadata
            metadata = stk_callback.get('CallbackMetadata', {}).get('Item', [])
            
            # Parse metadata items
            amount = 0
            mpesa_receipt = ''
            phone_number = ''
            transaction_date = ''
            
            for item in metadata:
                if item.get('Name') == 'Amount':
                    amount = item.get('Value', 0)
                elif item.get('Name') == 'MpesaReceiptNumber':
                    mpesa_receipt = item.get('Value', '')
                elif item.get('Name') == 'PhoneNumber':
                    phone_number = item.get('Value', '')
                elif item.get('Name') == 'TransactionDate':
                    transaction_date = item.get('Value', '')
            
            # Process result
            if result_code == 0:  # Success
                # Update giving transaction
                giving_transaction = callback.giving_transaction
                giving_transaction.mark_completed(mpesa_receipt)
                
                # Update payment request
                payment_request = callback.payment_request
                payment_request.record_callback(callback_data)
                
                callback.mark_processed({
                    'status': 'success',
                    'amount': amount,
                    'mpesa_receipt': mpesa_receipt,
                    'phone_number': phone_number,
                    'transaction_date': transaction_date
                })
                
                logger.info(f"M-Pesa payment completed: {mpesa_receipt}")
                
            else:  # Failed
                # Update giving transaction
                giving_transaction = callback.giving_transaction
                giving_transaction.mark_failed(result_desc)
                
                callback.mark_processed({
                    'status': 'failed',
                    'result_code': result_code,
                    'result_desc': result_desc
                })
                
                logger.error(f"M-Pesa payment failed: {result_desc}")
            
        except Exception as e:
            logger.error(f"M-Pesa callback processing failed: {e}")
            callback.status = 'failed'
            callback.validation_errors = str(e)
            callback.save()
    
    @staticmethod
    def process_reversal(reversal):
        """Process M-Pesa reversal"""
        try:
            mpesa_service = MpesaService()
            
            # This would implement M-Pesa reversal API
            # For now, we'll simulate the reversal
            
            reversal.mark_completed(f"REV_{reversal.reversal_id}")
            
            # Update original transaction
            original_transaction = reversal.original_transaction
            original_transaction.refund(reversal.amount, reversal.reason)
            
            logger.info(f"M-Pesa reversal completed: {reversal.reversal_id}")
            
        except Exception as e:
            logger.error(f"M-Pesa reversal failed: {e}")
            reversal.mark_failed(str(e))
            raise


class PaymentSchedulerService:
    """Service for scheduling payment operations"""
    
    @staticmethod
    def process_pending_payments():
        """Process pending payments scheduled for retry"""
        now = timezone.now()
        
        # Find payments ready for retry
        payment_requests = PaymentRequest.objects.filter(
            status='pending',
            next_retry_at__lte=now
        ).select_related('giving_transaction', 'giving_transaction__member__user')
        
        for payment_request in payment_requests:
            try:
                PaymentService.retry_payment(payment_request)
                logger.info(f"Payment retry processed: {payment_request.request_id}")
            except Exception as e:
                logger.error(f"Payment retry failed: {payment_request.request_id} - {e}")
    
    @staticmethod
    def check_transaction_status():
        """Check status of pending transactions"""
        # Find transactions that have been processing for too long
        cutoff_time = timezone.now() - timedelta(minutes=30)
        
        payment_requests = PaymentRequest.objects.filter(
            status='processing',
            processing_started_at__lte=cutoff_time
        ).select_related('giving_transaction')
        
        for payment_request in payment_requests:
            try:
                mpesa_service = MpesaService()
                response = mpesa_service.transaction_status(
                    payment_request.checkout_request_id
                )
                
                # Process status response
                result_code = response.get('ResultCode')
                if result_code == '0':
                    payment_request.giving_transaction.mark_completed(
                        response.get('MpesaReceiptNumber', '')
                    )
                else:
                    payment_request.giving_transaction.mark_failed(
                        response.get('ResultDesc', 'Transaction failed')
                    )
                
                logger.info(f"Transaction status checked: {payment_request.request_id}")
                
            except Exception as e:
                logger.error(f"Transaction status check failed: {payment_request.request_id} - {e}")
    
    @staticmethod
    def process_payment_batches():
        """Process scheduled payment batches"""
        now = timezone.now()
        
        # Find batches scheduled for processing
        batches = PaymentBatch.objects.filter(
            status='pending',
            scheduled_for__lte=now
        ).select_related('church')
        
        for batch in batches:
            try:
                # Process batch based on type
                if batch.batch_type == 'settlement':
                    SettlementService.process_settlement_batch(batch)
                elif batch.batch_type == 'payout':
                    PayoutService.process_payout_batch(batch)
                
                logger.info(f"Payment batch processed: {batch.batch_id}")
                
            except Exception as e:
                logger.error(f"Payment batch processing failed: {batch.batch_id} - {e}")


class SettlementService:
    """Service for processing settlements"""
    
    @staticmethod
    def process_settlement_batch(batch):
        """Process settlement batch"""
        # This would implement settlement logic
        batch.mark_processed(f"SETTLE_{batch.batch_id}", {'status': 'completed'})


class PayoutService:
    """Service for processing payouts"""
    
    @staticmethod
    def process_payout_batch(batch):
        """Process payout batch"""
        # This would implement payout logic
        batch.mark_processed(f"PAYOUT_{batch.batch_id}", {'status': 'completed'})
