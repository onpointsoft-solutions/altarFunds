import requests
import json
from django.conf import settings
from django.utils import timezone
from decimal import Decimal
import logging
from .models import GivingTransaction

logger = logging.getLogger(__name__)


class PaystackService:
    """Service for handling Paystack payments"""
    
    BASE_URL = "https://api.paystack.co"
    
    @classmethod
    def initialize_transaction(cls, email, amount, reference, callback_url=None, metadata=None):
        """Initialize a Paystack transaction"""
        try:
            headers = {
                "Authorization": f"Bearer {settings.PAYSTACK_SECRET_KEY}",
                "Content-Type": "application/json"
            }
            
            payload = {
                "email": email,
                "amount": int(amount * 100),  # Convert to kobo/cents
                "reference": reference,
                "currency": "KES",  # Kenyan Shillings
                "callback_url": callback_url or f"{settings.FRONTEND_URL}/payment/verify",
            }
            
            if metadata:
                payload["metadata"] = metadata
            
            response = requests.post(
                f"{cls.BASE_URL}/transaction/initialize",
                headers=headers,
                json=payload
            )
            
            if response.status_code == 200:
                data = response.json()
                if data.get("status"):
                    return {
                        "success": True,
                        "data": data["data"]
                    }
            
            logger.error(f"Paystack initialization failed: {response.text}")
            return {
                "success": False,
                "error": "Failed to initialize payment"
            }
            
        except Exception as e:
            logger.error(f"Paystack initialization error: {str(e)}")
            return {
                "success": False,
                "error": str(e)
            }
    
    @classmethod
    def verify_transaction(cls, reference):
        """Verify a Paystack transaction"""
        try:
            headers = {
                "Authorization": f"Bearer {settings.PAYSTACK_SECRET_KEY}",
                "Content-Type": "application/json"
            }
            
            response = requests.get(
                f"{cls.BASE_URL}/transaction/verify/{reference}",
                headers=headers
            )
            
            if response.status_code == 200:
                data = response.json()
                if data.get("status"):
                    return {
                        "success": True,
                        "data": data["data"]
                    }
            
            logger.error(f"Paystack verification failed: {response.text}")
            return {
                "success": False,
                "error": "Failed to verify payment"
            }
            
        except Exception as e:
            logger.error(f"Paystack verification error: {str(e)}")
            return {
                "success": False,
                "error": str(e)
            }
    
    @classmethod
    def process_payment(cls, transaction_id, user, church, category, amount, metadata=None):
        """Process a successful Paystack payment"""
        try:
            # Create giving transaction
            giving_transaction = GivingTransaction.objects.create(
                member=user.member_profile,
                church=church,
                category=category,
                transaction_type='one_time',
                amount=Decimal(amount),
                currency='KES',
                payment_method='card',
                payment_reference=transaction_id,
                status='completed',
                transaction_date=timezone.now(),
                completed_date=timezone.now(),
                notes="Paystack payment",
                created_by=user
            )
            
            logger.info(f"Created giving transaction: {giving_transaction.transaction_id}")
            
            # Schedule disbursement to church
            from .tasks import schedule_church_disbursement
            schedule_church_disbursement.delay(giving_transaction.id)
            
            return {
                "success": True,
                "transaction_id": str(giving_transaction.transaction_id),
                "amount": float(giving_transaction.amount)
            }
            
        except Exception as e:
            logger.error(f"Error processing payment: {str(e)}")
            return {
                "success": False,
                "error": str(e)
            }
