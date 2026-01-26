"""
Paystack Payment Integration Service
Handles payment initialization, verification, and webhook processing
"""
import requests
import hmac
import hashlib
from django.conf import settings
from django.utils import timezone
from decimal import Decimal
import logging

logger = logging.getLogger(__name__)


class PaystackService:
    """Service class for Paystack payment operations"""
    
    BASE_URL = "https://api.paystack.co"
    
    def __init__(self):
        self.secret_key = settings.PAYSTACK_SECRET_KEY
        self.public_key = settings.PAYSTACK_PUBLIC_KEY
        self.headers = {
            "Authorization": f"Bearer {self.secret_key}",
            "Content-Type": "application/json"
        }
    
    def initialize_payment(self, email, amount, reference, metadata=None, callback_url=None):
        """
        Initialize a payment transaction
        
        Args:
            email (str): Customer email
            amount (Decimal): Amount in Naira (will be converted to kobo)
            reference (str): Unique transaction reference
            metadata (dict): Additional transaction metadata
            callback_url (str): URL to redirect after payment
            
        Returns:
            dict: Response containing authorization_url and access_code
        """
        try:
            # Convert amount to kobo (Paystack uses kobo)
            amount_in_kobo = int(Decimal(amount) * 100)
            
            payload = {
                "email": email,
                "amount": amount_in_kobo,
                "reference": reference,
                "currency": "NGN",
                "metadata": metadata or {},
            }
            
            if callback_url:
                payload["callback_url"] = callback_url
            
            response = requests.post(
                f"{self.BASE_URL}/transaction/initialize",
                json=payload,
                headers=self.headers,
                timeout=30
            )
            
            response.raise_for_status()
            data = response.json()
            
            if data.get("status"):
                logger.info(f"Payment initialized successfully: {reference}")
                return {
                    "success": True,
                    "authorization_url": data["data"]["authorization_url"],
                    "access_code": data["data"]["access_code"],
                    "reference": data["data"]["reference"]
                }
            else:
                logger.error(f"Payment initialization failed: {data.get('message')}")
                return {
                    "success": False,
                    "message": data.get("message", "Payment initialization failed")
                }
                
        except requests.exceptions.RequestException as e:
            logger.error(f"Paystack API error: {str(e)}")
            return {
                "success": False,
                "message": "Unable to connect to payment gateway"
            }
        except Exception as e:
            logger.error(f"Payment initialization error: {str(e)}")
            return {
                "success": False,
                "message": "Payment initialization failed"
            }
    
    def verify_payment(self, reference):
        """
        Verify a payment transaction
        
        Args:
            reference (str): Transaction reference
            
        Returns:
            dict: Payment verification details
        """
        try:
            response = requests.get(
                f"{self.BASE_URL}/transaction/verify/{reference}",
                headers=self.headers,
                timeout=30
            )
            
            response.raise_for_status()
            data = response.json()
            
            if data.get("status"):
                transaction_data = data["data"]
                
                # Convert amount from kobo to naira
                amount = Decimal(transaction_data["amount"]) / 100
                
                logger.info(f"Payment verified successfully: {reference}")
                return {
                    "success": True,
                    "status": transaction_data["status"],
                    "amount": amount,
                    "currency": transaction_data["currency"],
                    "reference": transaction_data["reference"],
                    "paid_at": transaction_data.get("paid_at"),
                    "channel": transaction_data.get("channel"),
                    "customer": transaction_data.get("customer"),
                    "metadata": transaction_data.get("metadata", {})
                }
            else:
                logger.error(f"Payment verification failed: {data.get('message')}")
                return {
                    "success": False,
                    "message": data.get("message", "Payment verification failed")
                }
                
        except requests.exceptions.RequestException as e:
            logger.error(f"Paystack API error: {str(e)}")
            return {
                "success": False,
                "message": "Unable to verify payment"
            }
        except Exception as e:
            logger.error(f"Payment verification error: {str(e)}")
            return {
                "success": False,
                "message": "Payment verification failed"
            }
    
    def verify_webhook_signature(self, payload, signature):
        """
        Verify Paystack webhook signature
        
        Args:
            payload (bytes): Request body
            signature (str): X-Paystack-Signature header value
            
        Returns:
            bool: True if signature is valid
        """
        try:
            computed_signature = hmac.new(
                self.secret_key.encode('utf-8'),
                payload,
                hashlib.sha512
            ).hexdigest()
            
            return hmac.compare_digest(computed_signature, signature)
        except Exception as e:
            logger.error(f"Webhook signature verification error: {str(e)}")
            return False
    
    def process_webhook(self, event_type, data):
        """
        Process Paystack webhook events
        
        Args:
            event_type (str): Type of webhook event
            data (dict): Event data
            
        Returns:
            dict: Processing result
        """
        try:
            if event_type == "charge.success":
                return self._handle_successful_charge(data)
            elif event_type == "charge.failed":
                return self._handle_failed_charge(data)
            elif event_type == "transfer.success":
                return self._handle_successful_transfer(data)
            elif event_type == "transfer.failed":
                return self._handle_failed_transfer(data)
            else:
                logger.info(f"Unhandled webhook event: {event_type}")
                return {"success": True, "message": "Event acknowledged"}
                
        except Exception as e:
            logger.error(f"Webhook processing error: {str(e)}")
            return {"success": False, "message": "Webhook processing failed"}
    
    def _handle_successful_charge(self, data):
        """Handle successful charge webhook"""
        from .models import Payment
        from giving.models import Giving
        
        reference = data.get("reference")
        amount = Decimal(data.get("amount", 0)) / 100  # Convert from kobo
        
        try:
            # Find the payment record
            payment = Payment.objects.get(reference=reference)
            
            # Update payment status
            if payment.status != "completed":
                payment.status = "completed"
                payment.paid_at = timezone.now()
                payment.payment_method = data.get("channel", "paystack")
                payment.transaction_id = data.get("id")
                payment.save()
                
                # Update associated giving record
                if hasattr(payment, 'giving'):
                    giving = payment.giving
                    giving.status = "completed"
                    giving.payment_date = timezone.now()
                    giving.save()
                    
                    logger.info(f"Payment completed: {reference} - Amount: {amount}")
                
            return {"success": True, "message": "Payment processed"}
            
        except Payment.DoesNotExist:
            logger.error(f"Payment not found for reference: {reference}")
            return {"success": False, "message": "Payment not found"}
        except Exception as e:
            logger.error(f"Error processing successful charge: {str(e)}")
            return {"success": False, "message": "Processing failed"}
    
    def _handle_failed_charge(self, data):
        """Handle failed charge webhook"""
        from .models import Payment
        
        reference = data.get("reference")
        
        try:
            payment = Payment.objects.get(reference=reference)
            payment.status = "failed"
            payment.failure_reason = data.get("gateway_response", "Payment failed")
            payment.save()
            
            # Update associated giving record
            if hasattr(payment, 'giving'):
                giving = payment.giving
                giving.status = "failed"
                giving.save()
            
            logger.info(f"Payment failed: {reference}")
            return {"success": True, "message": "Failed payment processed"}
            
        except Payment.DoesNotExist:
            logger.error(f"Payment not found for reference: {reference}")
            return {"success": False, "message": "Payment not found"}
        except Exception as e:
            logger.error(f"Error processing failed charge: {str(e)}")
            return {"success": False, "message": "Processing failed"}
    
    def _handle_successful_transfer(self, data):
        """Handle successful transfer webhook"""
        logger.info(f"Transfer successful: {data.get('reference')}")
        return {"success": True, "message": "Transfer processed"}
    
    def _handle_failed_transfer(self, data):
        """Handle failed transfer webhook"""
        logger.info(f"Transfer failed: {data.get('reference')}")
        return {"success": True, "message": "Failed transfer processed"}
    
    def get_transaction(self, transaction_id):
        """
        Get transaction details by ID
        
        Args:
            transaction_id (int): Paystack transaction ID
            
        Returns:
            dict: Transaction details
        """
        try:
            response = requests.get(
                f"{self.BASE_URL}/transaction/{transaction_id}",
                headers=self.headers,
                timeout=30
            )
            
            response.raise_for_status()
            data = response.json()
            
            if data.get("status"):
                return {
                    "success": True,
                    "data": data["data"]
                }
            else:
                return {
                    "success": False,
                    "message": data.get("message", "Failed to fetch transaction")
                }
                
        except Exception as e:
            logger.error(f"Error fetching transaction: {str(e)}")
            return {
                "success": False,
                "message": "Failed to fetch transaction"
            }
    
    def list_transactions(self, per_page=50, page=1, status=None, customer=None):
        """
        List transactions with filters
        
        Args:
            per_page (int): Number of records per page
            page (int): Page number
            status (str): Filter by status (success, failed, abandoned)
            customer (str): Filter by customer ID
            
        Returns:
            dict: List of transactions
        """
        try:
            params = {
                "perPage": per_page,
                "page": page
            }
            
            if status:
                params["status"] = status
            if customer:
                params["customer"] = customer
            
            response = requests.get(
                f"{self.BASE_URL}/transaction",
                headers=self.headers,
                params=params,
                timeout=30
            )
            
            response.raise_for_status()
            data = response.json()
            
            if data.get("status"):
                return {
                    "success": True,
                    "data": data["data"],
                    "meta": data.get("meta", {})
                }
            else:
                return {
                    "success": False,
                    "message": data.get("message", "Failed to fetch transactions")
                }
                
        except Exception as e:
            logger.error(f"Error listing transactions: {str(e)}")
            return {
                "success": False,
                "message": "Failed to fetch transactions"
            }


# Singleton instance
paystack_service = PaystackService()
