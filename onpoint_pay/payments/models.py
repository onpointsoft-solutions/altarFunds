import uuid
import secrets
from decimal import Decimal
from django.db import models
from django.conf import settings
from django.utils import timezone
from merchants.models import Merchant


class Transaction(models.Model):
    """Main transaction model for all payment types"""
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    merchant = models.ForeignKey(
        Merchant, on_delete=models.CASCADE, related_name='transactions'
    )
    
    # Transaction details
    reference = models.CharField(max_length=100, unique=True)
    external_reference = models.CharField(max_length=100, blank=True)  # Client's reference
    amount = models.DecimalField(max_digits=15, decimal_places=2)
    currency = models.CharField(max_length=3, default='KES')
    
    # Payment method and type
    payment_method = models.CharField(
        max_length=20,
        choices=[
            ('mpesa', 'M-Pesa'),
            ('card', 'Card'),
            ('bank', 'Bank Transfer'),
        ]
    )
    transaction_type = models.CharField(
        max_length=20,
        choices=[
            ('payment', 'Payment'),
            ('refund', 'Refund'),
            ('reversal', 'Reversal'),
        ],
        default='payment'
    )
    
    # Customer information
    customer_name = models.CharField(max_length=255, blank=True)
    customer_email = models.EmailField(blank=True)
    customer_phone = models.CharField(max_length=20, blank=True)
    
    # Status tracking
    status = models.CharField(
        max_length=20,
        choices=[
            ('pending', 'Pending'),
            ('processing', 'Processing'),
            ('completed', 'Completed'),
            ('failed', 'Failed'),
            ('cancelled', 'Cancelled'),
            ('refunded', 'Refunded'),
            ('reversed', 'Reversed'),
        ],
        default='pending'
    )
    
    # Financial details
    fees = models.DecimalField(max_digits=15, decimal_places=2, default=0)
    net_amount = models.DecimalField(max_digits=15, decimal_places=2)
    
    # Callback and webhook
    callback_url = models.URLField(blank=True)
    webhook_delivered = models.BooleanField(default=False)
    webhook_attempts = models.PositiveIntegerField(default=0)
    
    # Metadata
    metadata = models.JSONField(default=dict, blank=True)
    
    # Timestamps
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)
    completed_at = models.DateTimeField(null=True, blank=True)
    expires_at = models.DateTimeField(null=True, blank=True)
    
    class Meta:
        db_table = 'transactions'
        verbose_name = 'Transaction'
        verbose_name_plural = 'Transactions'
        ordering = ['-created_at']
        indexes = [
            models.Index(fields=['merchant', 'status']),
            models.Index(fields=['reference']),
            models.Index(fields=['payment_method']),
            models.Index(fields=['created_at']),
        ]
    
    def __str__(self):
        return f"{self.reference} - {self.merchant.business_name}"
    
    def save(self, *args, **kwargs):
        if not self.reference:
            self.reference = f"TXN{timezone.now().strftime('%Y%m%d')}{secrets.token_urlsafe(8).upper()}"
        if not self.net_amount:
            self.net_amount = self.amount - self.fees
        super().save(*args, **kwargs)


class MpesaRequest(models.Model):
    """M-Pesa specific transaction details"""
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    transaction = models.OneToOneField(
        Transaction, on_delete=models.CASCADE, related_name='mpesa_details'
    )
    
    # M-Pesa specific fields
    phone_number = models.CharField(max_length=20)
    checkout_request_id = models.CharField(max_length=100, unique=True)
    merchant_request_id = models.CharField(max_length=100, blank=True)
    
    # Response from M-Pesa
    response_code = models.CharField(max_length=10, blank=True)
    response_description = models.TextField(blank=True)
    customer_message = models.TextField(blank=True)
    
    # Callback details
    mpesa_receipt = models.CharField(max_length=50, blank=True)
    transaction_date = models.DateTimeField(null=True, blank=True)
    result_code = models.CharField(max_length=10, blank=True)
    result_description = models.TextField(blank=True)
    
    # Timestamps
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)
    callback_received_at = models.DateTimeField(null=True, blank=True)
    
    class Meta:
        db_table = 'mpesa_requests'
        verbose_name = 'M-Pesa Request'
        verbose_name_plural = 'M-Pesa Requests'
        ordering = ['-created_at']
        indexes = [
            models.Index(fields=['checkout_request_id']),
            models.Index(fields=['mpesa_receipt']),
        ]
    
    def __str__(self):
        return f"{self.checkout_request_id} - {self.transaction.reference}"


class CardPayment(models.Model):
    """Card payment specific details"""
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    transaction = models.OneToOneField(
        Transaction, on_delete=models.CASCADE, related_name='card_details'
    )
    
    # Card details (encrypted/tokenized)
    card_token = models.CharField(max_length=255, blank=True)
    card_last4 = models.CharField(max_length=4, blank=True)
    card_brand = models.CharField(max_length=50, blank=True)  # Visa, Mastercard, etc.
    card_expiry_month = models.CharField(max_length=2, blank=True)
    card_expiry_year = models.CharField(max_length=4, blank=True)
    
    # 3D Secure
    three_d_secure = models.BooleanField(default=False)
    three_d_secure_url = models.URLField(blank=True)
    three_d_secure_pa_res = models.TextField(blank=True)
    
    # Processor details
    processor_reference = models.CharField(max_length=100, blank=True)
    processor_response = models.JSONField(default=dict, blank=True)
    authorization_code = models.CharField(max_length=50, blank=True)
    
    # Chargeback details
    chargeback_reason = models.TextField(blank=True)
    chargeback_date = models.DateTimeField(null=True, blank=True)
    
    # Timestamps
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)
    
    class Meta:
        db_table = 'card_payments'
        verbose_name = 'Card Payment'
        verbose_name_plural = 'Card Payments'
        ordering = ['-created_at']
    
    def __str__(self):
        return f"****{self.card_last4} - {self.transaction.reference}"


class WebhookLog(models.Model):
    """Log of webhook deliveries"""
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    merchant = models.ForeignKey(
        Merchant, on_delete=models.CASCADE, related_name='webhook_logs'
    )
    transaction = models.ForeignKey(
        Transaction, on_delete=models.CASCADE, related_name='webhook_logs', null=True, blank=True
    )
    
    # Webhook details
    webhook_url = models.URLField()
    event_type = models.CharField(max_length=100)
    payload = models.JSONField()
    
    # Response details
    response_status_code = models.PositiveIntegerField(null=True, blank=True)
    response_body = models.TextField(blank=True)
    response_headers = models.JSONField(default=dict, blank=True)
    
    # Status
    status = models.CharField(
        max_length=20,
        choices=[
            ('pending', 'Pending'),
            ('delivered', 'Delivered'),
            ('failed', 'Failed'),
            ('retrying', 'Retrying'),
        ],
        default='pending'
    )
    
    # Retry information
    attempt_count = models.PositiveIntegerField(default=1)
    next_retry_at = models.DateTimeField(null=True, blank=True)
    
    # Timestamps
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)
    delivered_at = models.DateTimeField(null=True, blank=True)
    
    class Meta:
        db_table = 'webhook_logs'
        verbose_name = 'Webhook Log'
        verbose_name_plural = 'Webhook Logs'
        ordering = ['-created_at']
        indexes = [
            models.Index(fields=['merchant', 'status']),
            models.Index(fields=['event_type']),
            models.Index(fields=['next_retry_at']),
        ]
    
    def __str__(self):
        return f"{self.event_type} - {self.merchant.business_name}"


class AuditLog(models.Model):
    """Audit trail for all payment gateway operations"""
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    merchant = models.ForeignKey(
        Merchant, on_delete=models.CASCADE, related_name='audit_logs', null=True, blank=True
    )
    
    # Action details
    action = models.CharField(max_length=100)  # payment_initiated, refund_processed, etc.
    resource_type = models.CharField(max_length=50)  # transaction, merchant, api_key
    resource_id = models.CharField(max_length=100, blank=True)
    
    # Request details
    ip_address = models.GenericIPAddressField(null=True, blank=True)
    user_agent = models.TextField(blank=True)
    api_key_used = models.CharField(max_length=255, blank=True)
    
    # Changes
    old_values = models.JSONField(default=dict, blank=True)
    new_values = models.JSONField(default=dict, blank=True)
    
    # Result
    success = models.BooleanField()
    error_message = models.TextField(blank=True)
    
    # Timestamps
    created_at = models.DateTimeField(auto_now_add=True)
    
    class Meta:
        db_table = 'audit_logs'
        verbose_name = 'Audit Log'
        verbose_name_plural = 'Audit Logs'
        ordering = ['-created_at']
        indexes = [
            models.Index(fields=['merchant', 'action']),
            models.Index(fields=['resource_type', 'resource_id']),
            models.Index(fields=['created_at']),
        ]
    
    def __str__(self):
        return f"{self.action} - {self.merchant.business_name if self.merchant else 'System'}"
