from rest_framework import serializers
from django.utils import timezone
from .models import Transaction, MpesaRequest, CardPayment


class PaymentInitiateSerializer(serializers.Serializer):
    """Serializer for initiating payments"""
    amount = serializers.DecimalField(max_digits=15, decimal_places=2, min_value=1)
    currency = serializers.CharField(max_length=3, default='KES')
    phone = serializers.CharField(max_length=20, required=False)
    email = serializers.EmailField(required=False)
    reference = serializers.CharField(max_length=100, required=False)
    payment_method = serializers.ChoiceField(
        choices=['mpesa', 'card'],
        default='mpesa'
    )
    callback_url = serializers.URLField(required=False)
    metadata = serializers.JSONField(default=dict, required=False)
    
    def validate_phone(self, value):
        """Validate phone number format"""
        if value:
            # Remove any non-digit characters
            phone = ''.join(filter(str.isdigit, value))
            
            # Check if it's a valid Kenyan number
            if len(phone) == 9 and phone.startswith('7'):
                return f"254{phone}"
            elif len(phone) == 12 and phone.startswith('254'):
                return phone
            elif len(phone) == 10 and phone.startswith('07'):
                return f"254{phone[1:]}"
            else:
                raise serializers.ValidationError(
                    "Invalid Kenyan phone number format. "
                    "Use formats: 07XXXXXXXX, 254XXXXXXXXX, or +254XXXXXXXXX"
                )
        return value
    
    def validate(self, attrs):
        """Validate payment method requirements"""
        payment_method = attrs.get('payment_method')
        phone = attrs.get('phone')
        email = attrs.get('email')
        
        if payment_method == 'mpesa' and not phone:
            raise serializers.ValidationError(
                "Phone number is required for M-Pesa payments"
            )
        
        if payment_method == 'card' and not email:
            raise serializers.ValidationError(
                "Email is required for card payments"
            )
        
        return attrs


class TransactionSerializer(serializers.ModelSerializer):
    """Serializer for transaction details"""
    merchant_name = serializers.CharField(source='merchant.business_name', read_only=True)
    payment_method_display = serializers.CharField(source='get_payment_method_display', read_only=True)
    status_display = serializers.CharField(source='get_status_display', read_only=True)
    transaction_type_display = serializers.CharField(source='get_transaction_type_display', read_only=True)
    
    class Meta:
        model = Transaction
        fields = [
            'id', 'reference', 'external_reference', 'amount', 'currency',
            'payment_method', 'payment_method_display', 'transaction_type',
            'transaction_type_display', 'customer_name', 'customer_email',
            'customer_phone', 'status', 'status_display', 'fees',
            'net_amount', 'callback_url', 'webhook_delivered',
            'webhook_attempts', 'metadata', 'merchant_name',
            'created_at', 'updated_at', 'completed_at', 'expires_at'
        ]
        read_only_fields = [
            'id', 'reference', 'fees', 'net_amount', 'webhook_delivered',
            'webhook_attempts', 'merchant_name', 'created_at', 'updated_at',
            'completed_at', 'expires_at'
        ]


class MpesaRequestSerializer(serializers.ModelSerializer):
    """Serializer for M-Pesa request details"""
    transaction_reference = serializers.CharField(source='transaction.reference', read_only=True)
    
    class Meta:
        model = MpesaRequest
        fields = [
            'id', 'transaction_reference', 'phone_number', 'checkout_request_id',
            'merchant_request_id', 'response_code', 'response_description',
            'customer_message', 'mpesa_receipt', 'transaction_date',
            'result_code', 'result_description', 'created_at',
            'updated_at', 'callback_received_at'
        ]
        read_only_fields = [
            'id', 'transaction_reference', 'checkout_request_id',
            'merchant_request_id', 'response_code', 'response_description',
            'customer_message', 'mpesa_receipt', 'transaction_date',
            'result_code', 'result_description', 'created_at',
            'updated_at', 'callback_received_at'
        ]


class CardPaymentSerializer(serializers.ModelSerializer):
    """Serializer for card payment details"""
    transaction_reference = serializers.CharField(source='transaction.reference', read_only=True)
    
    class Meta:
        model = CardPayment
        fields = [
            'id', 'transaction_reference', 'card_token', 'card_last4',
            'card_brand', 'card_expiry_month', 'card_expiry_year',
            'three_d_secure', 'three_d_secure_url', 'processor_reference',
            'processor_response', 'authorization_code', 'chargeback_reason',
            'chargeback_date', 'created_at', 'updated_at'
        ]
        read_only_fields = [
            'id', 'transaction_reference', 'card_token', 'card_last4',
            'card_brand', 'processor_reference', 'processor_response',
            'authorization_code', 'chargeback_reason', 'chargeback_date',
            'created_at', 'updated_at'
        ]


class PaymentStatusSerializer(serializers.Serializer):
    """Serializer for checking payment status"""
    reference = serializers.CharField(max_length=100, required=True)
    payment_method = serializers.ChoiceField(
        choices=['mpesa', 'card'],
        required=False
    )


class RefundSerializer(serializers.Serializer):
    """Serializer for refund requests"""
    reference = serializers.CharField(max_length=100, required=True)
    amount = serializers.DecimalField(max_digits=15, decimal_places=2, min_value=1)
    reason = serializers.CharField(max_length=255, required=False)
    
    def validate_reference(self, value):
        """Validate that transaction exists and can be refunded"""
        try:
            transaction = Transaction.objects.get(reference=value)
            if transaction.status != 'completed':
                raise serializers.ValidationError(
                    "Only completed transactions can be refunded"
                )
            if transaction.transaction_type != 'payment':
                raise serializers.ValidationError(
                    "Only payments can be refunded"
                )
            return value
        except Transaction.DoesNotExist:
            raise serializers.ValidationError("Transaction not found")
