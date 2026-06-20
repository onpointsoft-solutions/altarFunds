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
    
    def __init__(self, church_account=None):
        # Use church account keys if provided, otherwise use default settings
        if church_account:
            self.secret_key = church_account.paystack_secret_key
            self.public_key = church_account.paystack_public_key
            self.account_name = church_account.account_name
            self.account_code = church_account.account_code
        else:
            self.secret_key = settings.PAYSTACK_SECRET_KEY
            self.public_key = settings.PAYSTACK_PUBLIC_KEY
            self.account_name = "Default"
            self.account_code = "DEFAULT"
        
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
        """Handle successful charge webhook — marks payment + giving transaction complete."""
        from .models import Payment
        from giving.models import GivingTransaction

        reference = data.get("reference")
        amount    = Decimal(data.get("amount", 0)) / 100   # kobo → naira/KES

        # ── Update Payment record ─────────────────────────────────────────
        try:
            payment = Payment.objects.get(reference=reference)
            if payment.status != "completed":
                payment.status         = "completed"
                payment.paid_at        = timezone.now()
                payment.payment_method = data.get("channel", "paystack")
                payment.transaction_id = data.get("id")
                payment.save()
        except Payment.DoesNotExist:
            logger.warning(f"Payment record not found for reference: {reference}")

        # ── Update GivingTransaction record ───────────────────────────────
        giving_tx = (
            GivingTransaction.objects
            .filter(payment_reference=reference)
            .first()
        )

        if giving_tx and giving_tx.status != "completed":
            giving_tx.mark_completed(payment_reference=reference)
            logger.info(f"GivingTransaction completed: {reference} – KES {amount}")

        return {"success": True, "message": "Payment processed"}
    
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


# ════════════════════════════════════════════════════════════════════════════
#  PAYSTACK TRANSFER SERVICE
#
#  Flow:
#   1.  Member pays into the single AltarFunds Paystack account.
#   2.  Webhook fires → charge.success → _handle_successful_charge().
#   3.  We look up the church's ChurchBankAccount.paystack_recipient_code.
#       If none exists we create a Transfer Recipient automatically.
#   4.  We initiate a Paystack Transfer for (amount − platform_fee).
#   5.  We save the transfer details in ChurchDisbursement.
#   6.  transfer.success / transfer.failed webhooks update the disbursement.
#
#  Admin setup (one-time per church bank account):
#   POST /api/payments/create-transfer-recipient/
#   { "bank_account_id": <id> }
#   → calls Paystack POST /transferrecipient, stores recipient_code on the
#     ChurchBankAccount row.
# ════════════════════════════════════════════════════════════════════════════

class PaystackTransferService:
    """
    Handles Paystack Transfers (disbursement from AltarFunds → church bank).

    Uses the *platform* Paystack secret key (settings.PAYSTACK_SECRET_KEY) —
    NOT the church's own key — because the money is collected into the single
    AltarFunds account and then pushed out.
    """

    BASE_URL = "https://api.paystack.co"

    def __init__(self):
        self.secret_key = settings.PAYSTACK_SECRET_KEY
        self.headers = {
            "Authorization": f"Bearer {self.secret_key}",
            "Content-Type": "application/json",
        }
        self.platform_fee_pct = getattr(settings, "PLATFORM_FEE_PERCENTAGE", 1.5)

    # ── Recipient management ──────────────────────────────────────────────

    def create_transfer_recipient(self, bank_account):
        """
        Register a ChurchBankAccount as a Paystack Transfer Recipient.

        Paystack needs: type, name, account_number, bank_code.
        Returns the recipient_code (e.g. 'RCP_xxxxx') on success.

        Docs: https://paystack.com/docs/transfers/creating-transfer-recipient/
        """
        try:
            # Look up Paystack bank code from bank name
            bank_code = self._resolve_bank_code(bank_account.bank_name)
            if not bank_code:
                return {
                    "success": False,
                    "message": (
                        f"Could not resolve Paystack bank code for "
                        f"'{bank_account.bank_name}'. "
                        "Use GET /api/payments/list-banks/ to find the correct code."
                    ),
                }

            payload = {
                "type":           "nuban",          # Nigeria/Kenya inter-bank
                "name":           bank_account.account_name,
                "account_number": bank_account.account_number,
                "bank_code":      bank_code,
                "currency":       "KES",            # adjust if multi-currency
                "description":    f"Church account – {bank_account.church.name}",
                "metadata": {
                    "church_id":         bank_account.church.id,
                    "church_name":       bank_account.church.name,
                    "bank_account_id":   bank_account.id,
                },
            }

            response = requests.post(
                f"{self.BASE_URL}/transferrecipient",
                json=payload,
                headers=self.headers,
                timeout=30,
            )
            response.raise_for_status()
            data = response.json()

            if data.get("status"):
                recipient = data["data"]
                recipient_code = recipient["recipient_code"]
                recipient_id   = recipient["id"]

                # Persist on the bank account row
                bank_account.paystack_recipient_code = recipient_code
                bank_account.paystack_recipient_id   = recipient_id
                bank_account.save(update_fields=[
                    "paystack_recipient_code",
                    "paystack_recipient_id",
                ])

                logger.info(
                    f"Transfer recipient created: {recipient_code} "
                    f"for {bank_account.church.name}"
                )
                return {
                    "success":        True,
                    "recipient_code": recipient_code,
                    "recipient_id":   recipient_id,
                    "details":        recipient,
                }
            else:
                msg = data.get("message", "Recipient creation failed")
                logger.error(f"Paystack recipient creation failed: {msg}")
                return {"success": False, "message": msg}

        except requests.exceptions.RequestException as e:
            logger.error(f"Paystack API error (create_recipient): {e}")
            return {"success": False, "message": "Unable to connect to Paystack"}
        except Exception as e:
            logger.error(f"create_transfer_recipient error: {e}")
            return {"success": False, "message": str(e)}

    def list_banks(self, country="nigeria"):
        """
        Fetch Paystack's list of supported banks + their codes.
        Use this to find the bank_code for a church's bank.
        """
        try:
            response = requests.get(
                f"{self.BASE_URL}/bank",
                params={"country": country, "perPage": 100},
                headers=self.headers,
                timeout=30,
            )
            response.raise_for_status()
            data = response.json()
            if data.get("status"):
                return {"success": True, "banks": data["data"]}
            return {"success": False, "message": data.get("message")}
        except Exception as e:
            logger.error(f"list_banks error: {e}")
            return {"success": False, "message": str(e)}

    def verify_bank_account(self, account_number, bank_code):
        """
        Resolve account number → account name via Paystack.
        Useful for confirming a church's bank account before creating a recipient.
        """
        try:
            response = requests.get(
                f"{self.BASE_URL}/bank/resolve",
                params={"account_number": account_number, "bank_code": bank_code},
                headers=self.headers,
                timeout=30,
            )
            response.raise_for_status()
            data = response.json()
            if data.get("status"):
                return {
                    "success":      True,
                    "account_name": data["data"]["account_name"],
                    "account_number": data["data"]["account_number"],
                }
            return {"success": False, "message": data.get("message")}
        except Exception as e:
            logger.error(f"verify_bank_account error: {e}")
            return {"success": False, "message": str(e)}

    # ── Transfer initiation ───────────────────────────────────────────────

    def initiate_transfer(self, giving_transaction):
        """
        Send money from the AltarFunds Paystack balance to the church's
        bank account after a successful donation.

        Creates / updates a ChurchDisbursement record.
        Returns (success: bool, message: str).
        """
        from giving.models import ChurchDisbursement
        from decimal import Decimal

        church = giving_transaction.church

        # ── Find the church's primary bank account with a recipient code ─
        bank_account = (
            church.bank_accounts
            .filter(is_active=True, paystack_recipient_code__isnull=False)
            .exclude(paystack_recipient_code="")
            .order_by("-is_primary")
            .first()
        )

        if not bank_account:
            # Try to auto-create a recipient for the primary account
            primary = church.bank_accounts.filter(is_active=True, is_primary=True).first()
            if primary:
                result = self.create_transfer_recipient(primary)
                if result["success"]:
                    bank_account = primary
                else:
                    msg = (
                        f"No Paystack recipient configured for {church.name}. "
                        "Ask a church admin to set up a bank account via "
                        "POST /api/payments/create-transfer-recipient/"
                    )
                    logger.warning(msg)
                    return False, msg
            else:
                msg = f"No bank account found for {church.name}"
                logger.warning(msg)
                return False, msg

        # ── Calculate amounts ─────────────────────────────────────────────
        gross_amount  = Decimal(str(giving_transaction.amount))
        fee_pct       = Decimal(str(self.platform_fee_pct)) / Decimal("100")
        platform_fee  = (gross_amount * fee_pct).quantize(Decimal("0.01"))
        net_amount    = gross_amount - platform_fee

        # Paystack transfers are in kobo/pesewas (× 100)
        amount_in_kobo = int(net_amount * 100)

        if amount_in_kobo <= 0:
            return False, "Net transfer amount is zero or negative after fees"

        # ── Build transfer reference ──────────────────────────────────────
        reference = f"DISB-{giving_transaction.transaction_id}"

        # ── Create or refresh the disbursement record ─────────────────────
        disbursement, _ = ChurchDisbursement.objects.update_or_create(
            giving_transaction=giving_transaction,
            defaults={
                "church":               church,
                "amount":               gross_amount,
                "platform_fee":         platform_fee,
                "net_amount":           net_amount,
                "disbursement_method":  "paystack",
                "status":               "processing",
            },
        )

        # ── Call Paystack Transfer API ────────────────────────────────────
        try:
            payload = {
                "source":    "balance",
                "amount":    amount_in_kobo,
                "recipient": bank_account.paystack_recipient_code,
                "reason":    (
                    f"{church.name} – "
                    f"{giving_transaction.category.name} – "
                    f"{giving_transaction.transaction_id}"
                ),
                "reference": reference,
            }

            response = requests.post(
                f"{self.BASE_URL}/transfer",
                json=payload,
                headers=self.headers,
                timeout=30,
            )
            response.raise_for_status()
            data = response.json()

            if data.get("status"):
                transfer_data = data["data"]
                disbursement.conversation_id = str(transfer_data.get("id", ""))
                disbursement.transfer_code   = transfer_data.get("transfer_code", "")
                disbursement.status          = (
                    "completed"
                    if transfer_data.get("status") == "success"
                    else "processing"
                )
                disbursement.save()

                logger.info(
                    f"Transfer initiated: {disbursement.transfer_code} "
                    f"KES {net_amount} → {church.name}"
                )
                return True, f"Transfer initiated: {disbursement.transfer_code}"

            else:
                msg = data.get("message", "Transfer initiation failed")
                disbursement.status        = "failed"
                disbursement.error_message = msg
                disbursement.save()
                logger.error(f"Paystack transfer failed: {msg}")
                return False, msg

        except requests.exceptions.RequestException as e:
            disbursement.status        = "failed"
            disbursement.error_message = str(e)
            disbursement.save()
            logger.error(f"Paystack API error (initiate_transfer): {e}")
            return False, "Unable to connect to Paystack"
        except Exception as e:
            disbursement.status        = "failed"
            disbursement.error_message = str(e)
            disbursement.save()
            logger.error(f"initiate_transfer error: {e}")
            return False, str(e)

    # ── Webhook handlers ──────────────────────────────────────────────────

    def handle_transfer_success(self, data):
        """Called by paystack_webhook when event == 'transfer.success'."""
        from giving.models import ChurchDisbursement
        from django.utils import timezone

        transfer_code = data.get("transfer_code") or data.get("code")
        reference     = data.get("reference", "")

        qs = ChurchDisbursement.objects.filter(
            models_Q(transfer_code=transfer_code) | models_Q(conversation_id=str(data.get("id", "")))
        )
        if not qs.exists():
            # Try matching by reference embedded in the reason field
            qs = ChurchDisbursement.objects.filter(
                giving_transaction__transaction_id__icontains=reference.replace("DISB-", "")
            )

        if qs.exists():
            d = qs.first()
            d.status          = "completed"
            d.completed_at    = timezone.now()
            d.paystack_receipt = data.get("reference", transfer_code)
            d.save()

            # Mark the giving transaction's disbursement_status
            d.giving_transaction.disbursement_status = "completed"
            d.giving_transaction.save(update_fields=["disbursement_status"])

            logger.info(f"Disbursement completed: {transfer_code} → {d.church.name}")
        else:
            logger.warning(f"No disbursement found for transfer: {transfer_code}")

        return {"success": True, "message": "Transfer success processed"}

    def handle_transfer_failed(self, data):
        """Called by paystack_webhook when event == 'transfer.failed' or 'transfer.reversed'."""
        from giving.models import ChurchDisbursement

        transfer_code = data.get("transfer_code") or data.get("code")

        qs = ChurchDisbursement.objects.filter(
            models_Q(transfer_code=transfer_code) | models_Q(conversation_id=str(data.get("id", "")))
        )
        if qs.exists():
            d = qs.first()
            d.status        = "failed"
            d.error_message = data.get("gateway_response") or data.get("reason", "Transfer failed")
            d.retry_count   += 1
            d.save()

            # Schedule retry if within limit
            if d.retry_count < d.max_retries:
                from django.utils import timezone
                from datetime import timedelta
                d.next_retry_at = timezone.now() + timedelta(minutes=30 * d.retry_count)
                d.status = "pending_retry"
                d.save()
                logger.info(f"Disbursement {transfer_code} scheduled for retry #{d.retry_count}")
            else:
                logger.error(
                    f"Disbursement {transfer_code} permanently failed after "
                    f"{d.retry_count} attempts"
                )

        return {"success": True, "message": "Transfer failure processed"}

    def retry_failed_disbursements(self):
        """
        Called by a periodic task (e.g. Celery beat) to retry failed disbursements.
        """
        from giving.models import ChurchDisbursement
        from django.utils import timezone

        due = ChurchDisbursement.objects.filter(
            status="pending_retry",
            next_retry_at__lte=timezone.now(),
            retry_count__lt=models_F("max_retries"),
        ).select_related("giving_transaction", "church")

        retried, failed = 0, 0
        for d in due:
            success, msg = self.initiate_transfer(d.giving_transaction)
            if success:
                retried += 1
            else:
                failed += 1
                logger.error(f"Retry failed for disbursement {d.id}: {msg}")

        logger.info(f"Disbursement retries: {retried} succeeded, {failed} failed")
        return retried, failed

    # ── Internal helpers ──────────────────────────────────────────────────

    def _resolve_bank_code(self, bank_name):
        """
        Simple lookup of common Kenyan / Nigerian bank names → Paystack bank codes.
        Extend this dict or use the live list_banks() API for exhaustive coverage.
        """
        # Common Kenyan banks (Paystack Kenya)
        KENYA_BANK_CODES = {
            "equity bank":              "E001",
            "equity":                   "E001",
            "kcb":                      "K001",
            "kcb bank":                 "K001",
            "kenya commercial bank":    "K001",
            "cooperative bank":         "C001",
            "co-operative bank":        "C001",
            "ncba":                     "N001",
            "ncba bank":                "N001",
            "family bank":              "F001",
            "dtb":                      "D001",
            "diamond trust bank":       "D001",
            "stanbic bank":             "S001",
            "standard chartered":       "SC01",
            "standard chartered bank":  "SC01",
            "absa bank":                "A001",
            "absa":                     "A001",
            "i&m bank":                 "I001",
            "i&m":                      "I001",
            "prime bank":               "P001",
            "sidian bank":              "SI01",
            "gt bank":                  "GT01",
            "guaranty trust bank":      "GT01",
            "mpesa":                    "M001",
            "safaricom":                "M001",
        }
        return KENYA_BANK_CODES.get(bank_name.lower().strip())


# Helper imports needed only inside class methods — pulled to module level
# so they don't cause circular imports
from django.db.models import Q  as models_Q
from django.db.models import F  as models_F


# ── Module-level singleton ────────────────────────────────────────────────
paystack_transfer_service = PaystackTransferService()
