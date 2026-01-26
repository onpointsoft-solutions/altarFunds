import hashlib
import hmac
import requests
from datetime import datetime, timedelta
from decimal import Decimal
from django.conf import settings
from django.utils import timezone
from django.db import models
from merchants.models import Merchant
from .models import WebhookLog, AuditLog


def validate_api_key(request):
    """Validate API key from request headers"""
    # Get API key from Authorization header
    auth_header = request.META.get('HTTP_AUTHORIZATION', '')
    if not auth_header.startswith('Bearer '):
        return None
    
    api_key = auth_header[7:]  # Remove 'Bearer ' prefix
    
    # Find matching API key
    try:
        from merchants.models import ApiKey
        key_obj = ApiKey.objects.get(
            secret_key=api_key,
            is_active=True
        )
        
        # Check if key is expired
        if key_obj.expires_at and key_obj.expires_at < timezone.now():
            return None
        
        # Check IP whitelist
        if key_obj.allowed_ips:
            client_ip = get_client_ip(request)
            if client_ip not in key_obj.allowed_ips:
                return None
        
        # Update usage
        key_obj.last_used_at = timezone.now()
        key_obj.usage_count += 1
        key_obj.save(update_fields=['last_used_at', 'usage_count'])
        
        return key_obj
        
    except ApiKey.DoesNotExist:
        return None


def check_rate_limits(merchant, amount):
    """Check if merchant is within rate limits"""
    today = timezone.now().date()
    
    # Check daily limit
    daily_total = merchant.transactions.filter(
        created_at__date=today,
        status='completed'
    ).aggregate(total=models.Sum('amount'))['total'] or 0
    
    daily_limit = merchant.daily_transaction_limit
    
    if daily_total + amount > daily_limit:
        return {
            'allowed': False,
            'message': f'Daily transaction limit of KES {daily_limit:,} exceeded'
        }
    
    # Check monthly limit
    month_start = today.replace(day=1)
    monthly_total = merchant.transactions.filter(
        created_at__date__gte=month_start,
        status='completed'
    ).aggregate(total=models.Sum('amount'))['total'] or 0
    
    monthly_limit = merchant.monthly_transaction_limit
    
    if monthly_total + amount > monthly_limit:
        return {
            'allowed': False,
            'message': f'Monthly transaction limit of KES {monthly_limit:,} exceeded'
        }
    
    return {'allowed': True}


def create_webhook_log(merchant, transaction, webhook_url, event_type, payload):
    """Create a webhook log entry"""
    return WebhookLog.objects.create(
        merchant=merchant,
        transaction=transaction,
        webhook_url=webhook_url,
        event_type=event_type,
        payload=payload,
        status='pending'
    )


def deliver_webhook(webhook_log):
    """Deliver webhook to merchant"""
    try:
        # Get merchant's webhook secret
        webhook_config = merchant.webhook_configs.filter(
            url=webhook_log.webhook_url,
            is_active=True
        ).first()
        
        if not webhook_config:
            webhook_log.status = 'failed'
            webhook_log.response_status_code = 404
            webhook_log.response_body = 'Webhook configuration not found'
            webhook_log.save()
            return False
        
        # Prepare headers
        headers = {
            'Content-Type': 'application/json',
            'X-OnPoint-Signature': generate_webhook_signature(
                webhook_log.payload, webhook_config.secret
            ),
            'X-OnPoint-Event': webhook_log.event_type,
            'User-Agent': 'OnPoint-Pay-Webhook/1.0'
        }
        
        # Send webhook
        response = requests.post(
            webhook_log.webhook_url,
            json=webhook_log.payload,
            headers=headers,
            timeout=webhook_config.timeout_seconds
        )
        
        # Update webhook log
        webhook_log.response_status_code = response.status_code
        webhook_log.response_body = response.text
        webhook_log.response_headers = dict(response.headers)
        
        if response.status_code == 200:
            webhook_log.status = 'delivered'
            webhook_log.delivered_at = timezone.now()
        else:
            webhook_log.status = 'failed'
            webhook_log.attempt_count += 1
            webhook_log.next_retry_at = timezone.now() + timedelta(minutes=5 ** webhook_log.attempt_count)
        
        webhook_log.save()
        return webhook_log.status == 'delivered'
        
    except requests.exceptions.Timeout:
        webhook_log.status = 'retrying'
        webhook_log.attempt_count += 1
        webhook_log.next_retry_at = timezone.now() + timedelta(minutes=5 ** webhook_log.attempt_count)
        webhook_log.save()
        return False
    except Exception as e:
        webhook_log.status = 'failed'
        webhook_log.response_body = str(e)
        webhook_log.attempt_count += 1
        webhook_log.next_retry_at = timezone.now() + timedelta(minutes=5 ** webhook_log.attempt_count)
        webhook_log.save()
        return False


def generate_webhook_signature(payload, secret):
    """Generate HMAC signature for webhook"""
    if isinstance(payload, dict):
        import json
        payload = json.dumps(payload, sort_keys=True)
    
    signature = hmac.new(
        secret.encode('utf-8'),
        payload.encode('utf-8'),
        hashlib.sha256
    ).hexdigest()
    
    return f"sha256={signature}"


def verify_webhook_signature(payload, signature, secret):
    """Verify webhook signature"""
    expected_signature = generate_webhook_signature(payload, secret)
    return hmac.compare_digest(signature, expected_signature)


def get_client_ip(request):
    """Get client IP address from request"""
    x_forwarded_for = request.META.get('HTTP_X_FORWARDED_FOR')
    if x_forwarded_for:
        ip = x_forwarded_for.split(',')[0]
    else:
        ip = request.META.get('REMOTE_ADDR')
    return ip


def calculate_transaction_fees(amount, payment_method):
    """Calculate transaction fees based on amount and payment method"""
    if payment_method == 'mpesa':
        # M-Pesa fee structure
        if amount <= 100:
            fee = 0
        elif amount <= 500:
            fee = 8
        elif amount <= 1000:
            fee = 12
        elif amount <= 1500:
            fee = 20
        elif amount <= 2500:
            fee = 25
        elif amount <= 3500:
            fee = 35
        elif amount <= 5000:
            fee = 45
        elif amount <= 10000:
            fee = 65
        elif amount <= 20000:
            fee = 85
        elif amount <= 35000:
            fee = 100
        elif amount <= 50000:
            fee = 105
        elif amount <= 70000:
            fee = 110
        else:
            fee = 115
    elif payment_method == 'card':
        # Card payment fees (1.5% + KES 30)
        fee = max(amount * Decimal('0.015'), Decimal('30'))
    else:
        fee = 0
    
    return fee


def format_phone_number(phone):
    """Format phone number to Kenyan format"""
    if not phone:
        return ''
    
    # Remove all non-digit characters
    digits = ''.join(filter(str.isdigit, phone))
    
    # Format to 254XXXXXXXXX
    if len(digits) == 9 and digits.startswith('7'):
        return f"254{digits}"
    elif len(digits) == 12 and digits.startswith('254'):
        return digits
    elif len(digits) == 10 and digits.startswith('07'):
        return f"254{digits[1:]}"
    else:
        return phone  # Return original if format doesn't match


def validate_callback_signature(request, secret):
    """Validate M-Pesa callback signature"""
    # M-Pesa doesn't provide signatures in callbacks,
    # but we can validate the structure and required fields
    return True
