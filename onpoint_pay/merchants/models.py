import uuid
import secrets
from django.db import models
from django.contrib.auth.models import AbstractUser
from django.utils import timezone


class Merchant(AbstractUser):
    """Merchant model for payment gateway users"""
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    business_name = models.CharField(max_length=255, unique=True)
    business_email = models.EmailField(unique=True)
    business_phone = models.CharField(max_length=20, blank=True)
    business_description = models.TextField(blank=True)
    business_address = models.TextField(blank=True)
    business_type = models.CharField(max_length=100, blank=True)
    registration_number = models.CharField(max_length=100, blank=True)
    tax_identification = models.CharField(max_length=100, blank=True)
    website = models.URLField(blank=True)
    logo = models.CharField(max_length=255, blank=True, null=True)
    
    # Override AbstractUser fields with related_name to avoid conflicts
    groups = models.ManyToManyField(
        'auth.Group',
        verbose_name='groups',
        blank=True,
        help_text='The groups this user belongs to.',
        related_name="merchant_groups",
        related_query_name="merchant_group",
    )
    user_permissions = models.ManyToManyField(
        'auth.Permission',
        verbose_name='user permissions',
        blank=True,
        help_text='Specific permissions for this user.',
        related_name="merchant_permissions",
        related_query_name="merchant_permission",
    )
    
    # Compliance and verification
    is_verified = models.BooleanField(default=False)
    verification_date = models.DateTimeField(null=True, blank=True)
    kyc_documents = models.JSONField(default=dict, blank=True)  # Store KYC document URLs
    
    # Business settings
    default_currency = models.CharField(max_length=3, default='KES')
    settlement_bank_account = models.CharField(max_length=255, blank=True)
    settlement_frequency = models.CharField(
        max_length=20,
        choices=[
            ('daily', 'Daily'),
            ('weekly', 'Weekly'),
            ('monthly', 'Monthly'),
        ],
        default='daily'
    )
    
    # Status and timestamps
    is_active = models.BooleanField(default=True)
    is_suspended = models.BooleanField(default=False)
    suspension_reason = models.TextField(blank=True)
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)
    last_login_at = models.DateTimeField(null=True, blank=True)
    
    # Rate limiting
    daily_transaction_limit = models.DecimalField(
        max_digits=15, decimal_places=2, default=1000000.00
    )
    monthly_transaction_limit = models.DecimalField(
        max_digits=15, decimal_places=2, default=30000000.00
    )
    
    USERNAME_FIELD = 'email'
    REQUIRED_FIELDS = ['business_name', 'first_name', 'last_name']
    
    class Meta:
        db_table = 'merchants'
        verbose_name = 'Merchant'
        verbose_name_plural = 'Merchants'
        ordering = ['-created_at']
    
    def __str__(self):
        return f"{self.business_name} ({self.email})"
    
    @property
    def full_name(self):
        return f"{self.first_name} {self.last_name}".strip()


class ApiKey(models.Model):
    """API Keys for merchant authentication"""
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    merchant = models.ForeignKey(
        Merchant, on_delete=models.CASCADE, related_name='api_keys'
    )
    name = models.CharField(max_length=100)  # Friendly name for the key
    key_type = models.CharField(
        max_length=20,
        choices=[
            ('public', 'Public Key'),
            ('secret', 'Secret Key'),
            ('webhook', 'Webhook Key'),
        ],
        default='public'
    )
    
    # The actual keys
    public_key = models.CharField(max_length=255, unique=True, editable=False)
    secret_key = models.CharField(max_length=255, unique=True, editable=False)
    
    # Permissions and restrictions
    permissions = models.JSONField(default=dict)  # {"payments": ["create", "read"], "refunds": ["create"]}
    allowed_ips = models.JSONField(default=list)  # List of allowed IP addresses
    allowed_domains = models.JSONField(default=list)  # List of allowed domains for webhooks
    
    # Status and lifecycle
    is_active = models.BooleanField(default=True)
    expires_at = models.DateTimeField(null=True, blank=True)
    last_used_at = models.DateTimeField(null=True, blank=True)
    usage_count = models.PositiveIntegerField(default=0)
    
    # Timestamps
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)
    
    class Meta:
        db_table = 'api_keys'
        verbose_name = 'API Key'
        verbose_name_plural = 'API Keys'
        ordering = ['-created_at']
        unique_together = [['merchant', 'name']]
    
    def __str__(self):
        return f"{self.name} - {self.merchant.business_name}"
    
    def save(self, *args, **kwargs):
        if not self.public_key:
            self.public_key = f"pk_{secrets.token_urlsafe(32)}"
        if not self.secret_key:
            self.secret_key = f"sk_{secrets.token_urlsafe(40)}"
        super().save(*args, **kwargs)
    
    def is_valid(self):
        """Check if API key is valid and not expired"""
        if not self.is_active:
            return False
        if self.expires_at and self.expires_at < timezone.now():
            return False
        return True


class WebhookConfiguration(models.Model):
    """Webhook configuration for merchants"""
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    merchant = models.ForeignKey(
        Merchant, on_delete=models.CASCADE, related_name='webhook_configs'
    )
    name = models.CharField(max_length=100)
    url = models.URLField()
    events = models.JSONField(default=list)  # ["payment.completed", "payment.failed"]
    
    # Security
    secret = models.CharField(max_length=255, default=secrets.token_urlsafe(32))
    is_active = models.BooleanField(default=True)
    retry_attempts = models.PositiveIntegerField(default=3)
    timeout_seconds = models.PositiveIntegerField(default=30)
    
    # Timestamps
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)
    last_triggered_at = models.DateTimeField(null=True, blank=True)
    
    class Meta:
        db_table = 'webhook_configurations'
        verbose_name = 'Webhook Configuration'
        verbose_name_plural = 'Webhook Configurations'
        ordering = ['-created_at']
        unique_together = [['merchant', 'name']]
    
    def __str__(self):
        return f"{self.name} - {self.merchant.business_name}"


class Settlement(models.Model):
    """Settlement records for merchants"""
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    merchant = models.ForeignKey(
        Merchant, on_delete=models.CASCADE, related_name='settlements'
    )
    
    # Settlement details
    settlement_reference = models.CharField(max_length=100, unique=True)
    period_start = models.DateTimeField()
    period_end = models.DateTimeField()
    total_amount = models.DecimalField(max_digits=15, decimal_places=2)
    fees = models.DecimalField(max_digits=15, decimal_places=2, default=0)
    net_amount = models.DecimalField(max_digits=15, decimal_places=2)
    
    # Status
    status = models.CharField(
        max_length=20,
        choices=[
            ('pending', 'Pending'),
            ('processing', 'Processing'),
            ('completed', 'Completed'),
            ('failed', 'Failed'),
        ],
        default='pending'
    )
    
    # Bank transfer details
    bank_reference = models.CharField(max_length=100, blank=True)
    processed_at = models.DateTimeField(null=True, blank=True)
    
    # Timestamps
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)
    
    class Meta:
        db_table = 'settlements'
        verbose_name = 'Settlement'
        verbose_name_plural = 'Settlements'
        ordering = ['-created_at']
    
    def __str__(self):
        return f"{self.settlement_reference} - {self.merchant.business_name}"
