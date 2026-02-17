import requests
import json
from django.conf import settings
from django.utils import timezone
from decimal import Decimal
import logging
from .models import GivingTransaction

logger = logging.getLogger(__name__)


class PaystackDisbursementService:
    """Service for handling Paystack disbursements to churches"""
    
    BASE_URL = "https://api.paystack.co"
    
    @classmethod
    def create_transfer_recipient(cls, church):
        """Create a transfer recipient for the church"""
        try:
            headers = {
                "Authorization": f"Bearer {settings.PAYSTACK_SECRET_KEY}",
                "Content-Type": "application/json"
            }
            
            # Use church bank details or mobile money details
            if church.bank_account_number and church.bank_name:
                # Bank transfer recipient
                payload = {
                    "type": "nuban",
                    "name": church.bank_account_name or church.name,
                    "description": f"{church.name} - Disbursement Account",
                    "account_number": church.bank_account_number,
                    "bank_code": cls.get_bank_code(church.bank_name),
                    "currency": "KES"
                }
            elif church.mpesa_paybill_number or church.mpesa_till_number:
                # Mobile money recipient
                phone_number = church.senior_pastor_phone or church.phone_number
                if not phone_number:
                    return {
                        "success": False,
                        "error": "No phone number available for mobile money disbursement"
                    }
                
                payload = {
                    "type": "mobile_money",
                    "name": church.name,
                    "description": f"{church.name} - Mobile Money",
                    "currency": "KES",
                    "email": church.email,
                    "mobile_money": {
                        "provider": "MPESA",
                        "phone": phone_number
                    }
                }
            else:
                return {
                    "success": False,
                    "error": "No valid payment details configured for church"
                }
            
            response = requests.post(
                f"{cls.BASE_URL}/transferrecipient",
                headers=headers,
                json=payload
            )
            
            if response.status_code == 201:
                data = response.json()
                if data.get("status"):
                    return {
                        "success": True,
                        "recipient_code": data["data"]["recipient_code"],
                        "recipient_id": data["data"]["id"]
                    }
            
            logger.error(f"Failed to create transfer recipient: {response.text}")
            return {
                "success": False,
                "error": "Failed to create transfer recipient"
            }
            
        except Exception as e:
            logger.error(f"Error creating transfer recipient: {str(e)}")
            return {
                "success": False,
                "error": str(e)
            }
    
    @classmethod
    def get_bank_code(cls, bank_name):
        """Get bank code from bank name (simplified)"""
        # This is a simplified mapping - in production, you'd have a complete database
        bank_codes = {
            "Safaricom": "63301",
            "Equity Bank": "63001",
            "KCB Bank": "63002",
            "Cooperative Bank": "63003",
            "Standard Chartered": "63004",
            "Barclays Bank": "63005",
            "NCBA Bank": "63006",
            "Diamond Trust": "63007",
            "Family Bank": "63008",
            "I&M Bank": "63009",
        }
        
        # Try to find exact match
        if bank_name in bank_codes:
            return bank_codes[bank_name]
        
        # Try partial match
        for bank, code in bank_codes.items():
            if bank.lower() in bank_name.lower():
                return code
        
        # Default to Equity Bank if not found
        return "63001"
    
    @classmethod
    def initiate_transfer(cls, recipient_code, amount, reference, reason):
        """Initiate Paystack transfer to church"""
        try:
            headers = {
                "Authorization": f"Bearer {settings.PAYSTACK_SECRET_KEY}",
                "Content-Type": "application/json"
            }
            
            payload = {
                "source": "balance",  # Use Paystack balance
                "amount": int(amount * 100),  # Convert to kobo/cents
                "recipient": recipient_code,
                "reference": reference,
                "reason": reason,
                "currency": "KES"
            }
            
            response = requests.post(
                f"{cls.BASE_URL}/transfer",
                headers=headers,
                json=payload
            )
            
            if response.status_code == 200:
                data = response.json()
                if data.get("status"):
                    return {
                        "success": True,
                        "transfer_code": data["data"]["transfer_code"],
                        "transfer_id": data["data"]["id"]
                    }
            
            logger.error(f"Failed to initiate transfer: {response.text}")
            return {
                "success": False,
                "error": "Failed to initiate transfer"
            }
            
        except Exception as e:
            logger.error(f"Error initiating transfer: {str(e)}")
            return {
                "success": False,
                "error": str(e)
            }
    
    @classmethod
    def verify_transfer(cls, transfer_code):
        """Verify a Paystack transfer"""
        try:
            headers = {
                "Authorization": f"Bearer {settings.PAYSTACK_SECRET_KEY}",
                "Content-Type": "application/json"
            }
            
            response = requests.get(
                f"{cls.BASE_URL}/transfer/verify/{transfer_code}",
                headers=headers
            )
            
            if response.status_code == 200:
                data = response.json()
                if data.get("status"):
                    return {
                        "success": True,
                        "data": data["data"]
                    }
            
            logger.error(f"Failed to verify transfer: {response.text}")
            return {
                "success": False,
                "error": "Failed to verify transfer"
            }
            
        except Exception as e:
            logger.error(f"Error verifying transfer: {str(e)}")
            return {
                "success": False,
                "error": str(e)
            }
    
    @classmethod
    def disburse_to_church(cls, giving_transaction):
        """Disburse funds to church via Paystack"""
        try:
            church = giving_transaction.church
            
            # First, create or get transfer recipient
            recipient_result = cls.create_transfer_recipient(church)
            
            if not recipient_result["success"]:
                return {
                    "success": False,
                    "error": recipient_result["error"]
                }
            
            recipient_code = recipient_result["recipient_code"]
            
            # Calculate disbursement amount (minus platform fee)
            platform_fee_percentage = getattr(settings, 'PLATFORM_FEE_PERCENTAGE', 2.5)
            platform_fee = giving_transaction.amount * (Decimal(platform_fee_percentage) / Decimal(100))
            disbursement_amount = giving_transaction.amount - platform_fee
            
            # Generate transfer reference
            reference = f"DISBURSE_{church.church_code}_{giving_transaction.transaction_id}"
            reason = f"Disbursement to {church.name} - {giving_transaction.category.name}"
            
            # Initiate transfer
            transfer_result = cls.initiate_transfer(
                recipient_code=recipient_code,
                amount=float(disbursement_amount),
                reference=reference,
                reason=reason
            )
            
            if transfer_result["success"]:
                # Create disbursement record
                from .models import ChurchDisbursement
                disbursement = ChurchDisbursement.objects.create(
                    giving_transaction=giving_transaction,
                    church=church,
                    amount=disbursement_amount,
                    platform_fee=platform_fee,
                    disbursement_method='paystack',
                    conversation_id=transfer_result.get("transfer_id"),
                    status='processing',
                    created_at=timezone.now()
                )
                
                logger.info(f"Initiated Paystack disbursement {disbursement.id} for transaction {giving_transaction.transaction_id}")
                
                return {
                    "success": True,
                    "disbursement_id": disbursement.id,
                    "amount": float(disbursement_amount),
                    "platform_fee": float(platform_fee),
                    "transfer_code": transfer_result["transfer_code"]
                }
            else:
                # Mark for retry
                from .models import ChurchDisbursement
                disbursement = ChurchDisbursement.objects.create(
                    giving_transaction=giving_transaction,
                    church=church,
                    amount=disbursement_amount,
                    platform_fee=platform_fee,
                    disbursement_method='paystack',
                    status='failed',
                    error_message=transfer_result.get("error"),
                    retry_count=1,
                    next_retry_at=timezone.now() + timezone.timedelta(hours=1),
                    created_at=timezone.now()
                )
                
                return transfer_result
            
        except Exception as e:
            logger.error(f"Disbursement error: {str(e)}")
            return {
                "success": False,
                "error": str(e)
            }
    
    @classmethod
    def handle_transfer_webhook(cls, webhook_data):
        """Handle Paystack transfer webhook"""
        try:
            event = webhook_data.get("event")
            data = webhook_data.get("data", {})
            
            if event == "transfer.success":
                # Transfer successful
                reference = data.get("reference")
                transfer_code = data.get("transfer_code")
                
                # Find the disbursement record
                from .models import ChurchDisbursement
                try:
                    disbursement = ChurchDisbursement.objects.get(
                        giving_transaction__payment_reference__contains=reference.split("_")[-1]
                    )
                except ChurchDisbursement.DoesNotExist:
                    logger.error(f"No disbursement found for transfer reference: {reference}")
                    return
                
                # Mark as completed
                disbursement.status = 'completed'
                disbursement.completed_at = timezone.now()
                disbursement.save()
                
                # Update giving transaction
                disbursement.giving_transaction.disbursement_status = 'completed'
                disbursement.giving_transaction.save()
                
                logger.info(f"Paystack disbursement {disbursement.id} completed successfully")
                
                # Send notification
                cls.send_disbursement_notification(disbursement, "success")
                
            elif event == "transfer.failed":
                # Transfer failed
                reference = data.get("reference")
                transfer_code = data.get("transfer_code")
                
                # Find the disbursement record
                from .models import ChurchDisbursement
                try:
                    disbursement = ChurchDisbursement.objects.get(
                        giving_transaction__payment_reference__contains=reference.split("_")[-1]
                    )
                except ChurchDisbursement.DoesNotExist:
                    logger.error(f"No disbursement found for transfer reference: {reference}")
                    return
                
                # Mark as failed and schedule retry
                disbursement.mark_failed(data.get("message", "Transfer failed"))
                
                logger.error(f"Paystack disbursement {disbursement.id} failed")
                cls.send_disbursement_notification(disbursement, "failed")
                
        except Exception as e:
            logger.error(f"Error handling Paystack transfer webhook: {str(e)}")
    
    @classmethod
    def send_disbursement_notification(cls, disbursement, status):
        """Send disbursement notification to church"""
        try:
            from common.services import NotificationService
            
            if status == "success":
                message = f"Payment of KES {disbursement.amount} has been sent to your church via Paystack. Reference: {disbursement.conversation_id}"
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
