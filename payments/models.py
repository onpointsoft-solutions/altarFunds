from django.db import models
from django.conf import settings

class PaystackAccount(models.Model):
    """Paystack account configuration for churches"""
    ACCOUNT_TYPES = [
        ('main', 'Main Account'),
        ('tithe', 'Tithe Account'),
        ('offering', 'Offering Account'),
        ('building', 'Building Fund'),
        ('missions', 'Missions Account'),
        ('welfare', 'Welfare Account'),
        ('other', 'Other Account'),
    ]
    
    church = models.ForeignKey('churches.Church', on_delete=models.CASCADE)
    account_name = models.CharField(max_length=100)
    account_code = models.CharField(max_length=20, unique=True)
    account_type = models.CharField(max_length=20, choices=ACCOUNT_TYPES, default='main')
    paystack_secret_key = models.CharField(max_length=100)
    paystack_public_key = models.CharField(max_length=100)
    business_number = models.CharField(max_length=20, blank=True, null=True)
    is_active = models.BooleanField(default=True)
    is_default = models.BooleanField(default=False)
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)
    
    class Meta:
        unique_together = ['church', 'account_type']
    
    def __str__(self):
        return f"{self.church.name} - {self.account_name}"
    
    def save(self, *args, **kwargs):
        # Ensure only one default account per church
        if self.is_default:
            PaystackAccount.objects.filter(church=self.church, is_default=True).update(is_default=False)
        super().save(*args, **kwargs)

class PaymentRequest(models.Model):
    STATUS_CHOICES = [
        ('pending', 'Pending'),
        ('processing', 'Processing'),
        ('completed', 'Completed'),
        ('failed', 'Failed'),
    ]
    
    PAYMENT_METHODS = [
        ('paystack', 'Paystack'),
        ('card', 'Credit/Debit Card'),
        ('bank', 'Bank Transfer'),
        ('cash', 'Cash'),
    ]
    
    # Core fields
    user = models.ForeignKey('accounts.User', on_delete=models.CASCADE)
    amount = models.DecimalField(max_digits=10, decimal_places=2)
    payment_method = models.CharField(max_length=20, choices=PAYMENT_METHODS, default='paystack')
    status = models.CharField(max_length=20, choices=STATUS_CHOICES, default='pending')
    
    # Paystack routing fields
    paystack_account = models.ForeignKey('PaystackAccount', on_delete=models.SET_NULL, null=True, blank=True)
    phone_number = models.CharField(max_length=20, blank=True, null=True)
    business_number = models.CharField(max_length=20, blank=True, null=True)
    account_reference = models.CharField(max_length=100, blank=True, null=True)
    transaction_desc = models.CharField(max_length=200, blank=True, null=True)
    
    # Response fields
    transaction_reference = models.CharField(max_length=100, blank=True, null=True)
    authorization_url = models.CharField(max_length=500, blank=True, null=True)
    access_code = models.CharField(max_length=100, blank=True, null=True)
    
    # Timestamps
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)
    created_by = models.ForeignKey('accounts.User', on_delete=models.SET_NULL, null=True, related_name='payment_requests_created')

    def __str__(self):
        return f"{self.user.email} - {self.amount} ({self.payment_method})"
    
    def mark_processing(self):
        """Mark as processing"""
        self.status = 'processing'
        self.save()
    
    def mark_completed(self, response_data):
        """Mark as completed with response data"""
        self.status = 'completed'
        self.transaction_reference = response_data.get('reference')
        self.authorization_url = response_data.get('authorization_url')
        self.access_code = response_data.get('access_code')
        self.save()
    
    def mark_failed(self, response_data):
        """Mark as failed with error data"""
        self.status = 'failed'
        self.save()

class Payment(models.Model):
    STATUS_CHOICES = [
        ('pending', 'Pending'),
        ('completed', 'Completed'),
        ('failed', 'Failed'),
        ('refunded', 'Refunded'),
    ]
    
    payment_request = models.OneToOneField(PaymentRequest, on_delete=models.CASCADE)
    amount = models.DecimalField(max_digits=10, decimal_places=2)
    payment_method = models.CharField(max_length=20)
    status = models.CharField(max_length=20, choices=STATUS_CHOICES, default='pending')
    transaction_reference = models.CharField(max_length=100, blank=True, null=True)
    processed_at = models.DateTimeField(blank=True, null=True)
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    def __str__(self):
        return f"{self.payment_request.user.email} - {self.amount}"

class Transaction(models.Model):
    payment = models.ForeignKey(Payment, on_delete=models.CASCADE)
    amount = models.DecimalField(max_digits=10, decimal_places=2)
    status = models.CharField(max_length=20)
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    def __str__(self):
        return f"{self.payment} - {self.amount}"


class PaymentReconciliation(models.Model):
    """Auto-reconciliation model for matching payments with bank records"""
    
    RECONCILIATION_STATUS = [
        ('pending', 'Pending'),
        ('matched', 'Matched'),
        ('unmatched', 'Unmatched'),
        ('manual_review', 'Manual Review'),
        ('completed', 'Completed'),
    ]
    
    # Reference fields
    payment_request = models.OneToOneField(PaymentRequest, on_delete=models.CASCADE)
    bank_transaction_id = models.CharField(max_length=100, blank=True, null=True)
    bank_reference = models.CharField(max_length=100, blank=True, null=True)
    
    # Reconciliation details
    reconciliation_status = models.CharField(
        max_length=20, 
        choices=RECONCILIATION_STATUS, 
        default='pending'
    )
    matched_amount = models.DecimalField(max_digits=10, decimal_places=2, null=True, blank=True)
    matched_date = models.DateTimeField(null=True, blank=True)
    
    # Auto-reconciliation results
    confidence_score = models.IntegerField(default=0)  # 0-100 confidence level
    reconciliation_notes = models.TextField(blank=True)
    
    # Manual review fields
    reviewed_by = models.ForeignKey('accounts.User', on_delete=models.SET_NULL, null=True, blank=True)
    reviewed_at = models.DateTimeField(null=True, blank=True)
    review_notes = models.TextField(blank=True)
    
    # Timestamps
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)
    
    class Meta:
        db_table = 'payments_reconciliation'
        verbose_name = 'Payment Reconciliation'
        verbose_name_plural = 'Payment Reconciliations'
        indexes = [
            models.Index(fields=['reconciliation_status']),
            models.Index(fields=['matched_date']),
            models.Index(fields=['confidence_score']),
        ]
    
    def __str__(self):
        return f"Reconciliation for {self.payment_request}"


class PaymentDetail(models.Model):
    """Additional payment details for admin management"""
    
    DETAIL_TYPES = [
        ('bank_deposit', 'Bank Deposit'),
        ('mobile_transfer', 'Mobile Transfer'),
        ('cash_deposit', 'Cash Deposit'),
        ('check_deposit', 'Check Deposit'),
        ('online_transfer', 'Online Transfer'),
        ('other', 'Other'),
    ]
    
    # Core fields
    payment_request = models.OneToOneField(PaymentRequest, on_delete=models.CASCADE)
    detail_type = models.CharField(max_length=20, choices=DETAIL_TYPES)
    
    # Bank/Transfer details
    bank_name = models.CharField(max_length=200, blank=True)
    account_number = models.CharField(max_length=50, blank=True)
    transaction_id = models.CharField(max_length=100, blank=True)
    reference_number = models.CharField(max_length=100, blank=True)
    
    # Amount and date
    actual_amount = models.DecimalField(max_digits=10, decimal_places=2)
    transaction_date = models.DateTimeField()
    settlement_date = models.DateTimeField(null=True, blank=True)
    
    # Additional information
    payer_name = models.CharField(max_length=200, blank=True)
    payer_account = models.CharField(max_length=50, blank=True)
    notes = models.TextField(blank=True)
    
    # Verification
    is_verified = models.BooleanField(default=False)
    verified_by = models.ForeignKey('accounts.User', on_delete=models.SET_NULL, null=True, blank=True)
    verified_at = models.DateTimeField(null=True, blank=True)
    
    # Attachments
    receipt_image = models.ImageField(upload_to='payment_receipts/', blank=True, null=True)
    supporting_document = models.FileField(upload_to='payment_docs/', blank=True, null=True)
    
    # Timestamps
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)
    
    class Meta:
        db_table = 'payment_details'
        verbose_name = 'Payment Detail'
        verbose_name_plural = 'Payment Details'
        indexes = [
            models.Index(fields=['detail_type']),
            models.Index(fields=['transaction_date']),
            models.Index(fields=['is_verified']),
        ]
    
    def __str__(self):
        return f"Payment details for {self.payment_request}"
