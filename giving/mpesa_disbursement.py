import requests
import json
from django.conf import settings
from django.utils import timezone
from decimal import Decimal
import logging
from .models import GivingTransaction

logger = logging.getLogger(__name__)


class MpesaDisbursementService:
    """Service for handling M-Pesa disbursements to churches"""
    
    BASE_URL = "https://api.safaricom.co.ke"
    
    @classmethod
    def get_access_token(cls):
        """Get M-Pesa API access token"""
        try:
            url = f"{cls.BASE_URL}/oauth/v1/generate?grant_type=client_credentials"
            headers = {
                "Authorization": f"Basic {settings.MPESA_BASIC_AUTH}"
            }
            
            response = requests.get(url, headers=headers)
            
            if response.status_code == 200:
                data = response.json()
                return data.get("access_token")
            
            logger.error(f"Failed to get M-Pesa access token: {response.text}")
            return None
            
        except Exception as e:
            logger.error(f"M-Pesa access token error: {str(e)}")
            return None
    
    @classmethod
    def initiate_disbursement(cls, phone_number, amount, remarks, transaction_id):
        """Initiate M-Pesa disbursement to church"""
        try:
            access_token = cls.get_access_token()
            if not access_token:
                return {
                    "success": False,
                    "error": "Failed to get access token"
                }
            
            url = f"{cls.BASE_URL}/mpesa/b2c/v1/paymentrequest"
            headers = {
                "Authorization": f"Bearer {access_token}",
                "Content-Type": "application/json"
            }
            
            payload = {
                "InitiatorName": settings.MPESA_INITIATOR_NAME,
                "SecurityCredential": settings.MPESA_SECURITY_CREDENTIAL,
                "CommandID": "BusinessPayment",  # or SalaryPayment, PromotionPayment
                "Amount": int(amount),
                "PartyA": settings.MPESA_SHORTCODE,
                "PartyB": phone_number,
                "Remarks": remarks,
                "QueueTimeOutURL": f"{settings.BACKEND_URL}/api/mpesa/disbursement/timeout/",
                "ResultURL": f"{settings.BACKEND_URL}/api/mpesa/disbursement/result/",
                "Occasion": f"Church Disbursement - {transaction_id}"
            }
            
            response = requests.post(url, headers=headers, json=payload)
            
            if response.status_code == 200:
                data = response.json()
                if data.get("ResponseCode") == "0":
                    return {
                        "success": True,
                        "conversation_id": data.get("ConversationID"),
                        "originator_conversation_id": data.get("OriginatorConversationID")
                    }
                else:
                    return {
                        "success": False,
                        "error": data.get("errorMessage", "Disbursement failed")
                    }
            
            logger.error(f"M-Pesa disbursement failed: {response.text}")
            return {
                "success": False,
                "error": "Disbursement request failed"
            }
            
        except Exception as e:
            logger.error(f"M-Pesa disbursement error: {str(e)}")
            return {
                "success": False,
                "error": str(e)
            }
    
    @classmethod
    def disburse_to_church(cls, giving_transaction):
        """Disburse funds to church via M-Pesa"""
        try:
            church = giving_transaction.church
            
            # Determine payment method (Paybill or Till)
            if church.mpesa_paybill_number:
                # For Paybill, we need to send to a specific phone number
                # Usually the church treasurer or pastor's number
                phone_number = church.senior_pastor_phone or church.phone_number
                remarks = f"Disbursement to {church.name} - Paybill {church.mpesa_paybill_number}"
            elif church.mpesa_till_number:
                # For Till, similar process
                phone_number = church.senior_pastor_phone or church.phone_number
                remarks = f"Disbursement to {church.name} - Till {church.mpesa_till_number}"
            else:
                return {
                    "success": False,
                    "error": "No M-Pesa payment details configured for church"
                }
            
            # Calculate disbursement amount (minus platform fee)
            platform_fee_percentage = getattr(settings, 'PLATFORM_FEE_PERCENTAGE', 2.5)
            platform_fee = giving_transaction.amount * (Decimal(platform_fee_percentage) / Decimal(100))
            disbursement_amount = giving_transaction.amount - platform_fee
            
            # Initiate disbursement
            result = cls.initiate_disbursement(
                phone_number=phone_number,
                amount=float(disbursement_amount),
                remarks=remarks,
                transaction_id=str(giving_transaction.transaction_id)
            )
            
            if result["success"]:
                # Create disbursement record
                from .models import ChurchDisbursement
                disbursement = ChurchDisbursement.objects.create(
                    giving_transaction=giving_transaction,
                    church=church,
                    amount=disbursement_amount,
                    platform_fee=platform_fee,
                    disbursement_method='mpesa',
                    conversation_id=result.get("conversation_id"),
                    status='processing',
                    created_at=timezone.now()
                )
                
                logger.info(f"Initiated disbursement {disbursement.id} for transaction {giving_transaction.transaction_id}")
                
                return {
                    "success": True,
                    "disbursement_id": disbursement.id,
                    "amount": float(disbursement_amount),
                    "platform_fee": float(platform_fee)
                }
            else:
                # Mark for retry
                from .models import ChurchDisbursement
                disbursement = ChurchDisbursement.objects.create(
                    giving_transaction=giving_transaction,
                    church=church,
                    amount=disbursement_amount,
                    platform_fee=platform_fee,
                    disbursement_method='mpesa',
                    status='failed',
                    error_message=result.get("error"),
                    retry_count=1,
                    next_retry_at=timezone.now() + timezone.timedelta(hours=1),
                    created_at=timezone.now()
                )
                
                return result
            
        except Exception as e:
            logger.error(f"Disbursement error: {str(e)}")
            return {
                "success": False,
                "error": str(e)
            }
    
    @classmethod
    def handle_disbursement_result(cls, result_data):
        """Handle M-Pesa disbursement result callback"""
        try:
            conversation_id = result_data.get("ConversationID")
            result_code = result_data.get("ResultCode")
            result_desc = result_data.get("ResultDesc")
            
            if not conversation_id:
                logger.error("No ConversationID in disbursement result")
                return
            
            # Find the disbursement record
            from .models import ChurchDisbursement
            try:
                disbursement = ChurchDisbursement.objects.get(
                    conversation_id=conversation_id
                )
            except ChurchDisbursement.DoesNotExist:
                logger.error(f"No disbursement found for conversation ID: {conversation_id}")
                return
            
            # Update disbursement status
            if result_code == "0":
                # Successful
                disbursement.status = 'completed'
                disbursement.completed_at = timezone.now()
                disbursement.mpesa_receipt = result_data.get("TransactionDetails", {}).get("ReceiptNo")
                
                # Update giving transaction status
                disbursement.giving_transaction.disbursement_status = 'completed'
                disbursement.giving_transaction.save()
                
                logger.info(f"Disbursement {disbursement.id} completed successfully")
                
                # Send notification to church
                cls.send_disbursement_notification(disbursement, "success")
                
            else:
                # Failed
                disbursement.status = 'failed'
                disbursement.error_message = result_desc
                disbursement.retry_count += 1
                
                # Schedule retry if under max attempts
                max_retries = getattr(settings, 'MPESA_MAX_RETRIES', 3)
                if disbursement.retry_count < max_retries:
                    disbursement.status = 'pending_retry'
                    disbursement.next_retry_at = timezone.now() + timezone.timedelta(hours=2 ** disbursement.retry_count)
                    
                    # Schedule retry task
                    from .tasks import retry_disbursement
                    retry_disbursement.apply_async(
                        args=[disbursement.id],
                        eta=disbursement.next_retry_at
                    )
                else:
                    # Max retries reached, mark as permanently failed
                    disbursement.giving_transaction.disbursement_status = 'failed'
                    disbursement.giving_transaction.save()
                
                logger.error(f"Disbursement {disbursement.id} failed: {result_desc}")
                cls.send_disbursement_notification(disbursement, "failed")
            
            disbursement.save()
            
        except Exception as e:
            logger.error(f"Error handling disbursement result: {str(e)}")
    
    @classmethod
    def send_disbursement_notification(cls, disbursement, status):
        """Send disbursement notification to church"""
        try:
            from common.services import NotificationService
            
            if status == "success":
                message = f"Payment of KES {disbursement.amount} has been sent to your church via M-Pesa. Receipt: {disbursement.mpesa_receipt}"
            else:
                message = f"Payment disbursement failed. Amount: KES {disbursement.amount}. Error: {disbursement.error_message}"
            
            # Send to church pastor/admin
            NotificationService.send_notification(
                user=disbursement.church.senior_pastor or disbursement.giving_transaction.member.user,
                title=f"Payment Disbursement {status.title()}",
                message=message,
                notification_type='payment_disbursement'
            )
            
        except Exception as e:
            logger.error(f"Error sending disbursement notification: {str(e)}")
