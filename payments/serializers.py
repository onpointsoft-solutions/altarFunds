from rest_framework import serializers
from .models import PaymentRequest, Payment, Transaction, PaystackAccount, PaymentReconciliation, PaymentDetail

class PaystackAccountSerializer(serializers.ModelSerializer):
    """Serializer for Paystack account management"""
    
    class Meta:
        model = PaystackAccount
        fields = [
            'id', 'church', 'account_name', 'account_code', 'account_type',
            'business_number', 'is_active', 'is_default', 'created_at', 'updated_at'
        ]
        read_only_fields = ['id', 'created_at', 'updated_at']
    
    def validate(self, data):
        """Validate that only one default account exists per church"""
        if data.get('is_default', False):
            church = data.get('church') or self.instance.church
            existing_default = PaystackAccount.objects.filter(
                church=church, 
                is_default=True
            ).exclude(id=self.instance.id if self.instance else None)
            
            if existing_default.exists():
                raise serializers.ValidationError(
                    "This church already has a default Paystack account."
                )
        return data

class PaymentRequestSerializer(serializers.ModelSerializer):
    """Enhanced payment request serializer with account details"""
    paystack_account_details = serializers.SerializerMethodField()
    
    class Meta:
        model = PaymentRequest
        fields = '__all__'
    
    def get_paystack_account_details(self, obj):
        """Get paystack account details for response"""
        if obj.paystack_account:
            return {
                'id': obj.paystack_account.id,
                'account_name': obj.paystack_account.account_name,
                'account_code': obj.paystack_account.account_code,
                'account_type': obj.paystack_account.account_type
            }
        return None

class PaymentSerializer(serializers.ModelSerializer):
    class Meta:
        model = Payment
        fields = '__all__'

class TransactionSerializer(serializers.ModelSerializer):
    class Meta:
        model = Transaction
        fields = '__all__'

class ChurchAccountListSerializer(serializers.Serializer):
    """Serializer for listing available church accounts for giving"""
    accounts = serializers.SerializerMethodField()
    
    def get_accounts(self, obj):
        """Get all active Paystack accounts for the church"""
        church = self.context.get('church')
        if not church:
            return []
        
        accounts = PaystackAccount.objects.filter(
            church=church,
            is_active=True
        ).order_by('-is_default', 'account_name')
        
        return [
            {
                'id': account.id,
                'account_name': account.account_name,
                'account_code': account.account_code,
                'account_type': account.account_type,
                'is_default': account.is_default,
                'business_number': account.business_number
            }
            for account in accounts
        ]


class PaymentReconciliationSerializer(serializers.ModelSerializer):
    """Serializer for payment reconciliation"""
    payment_request_details = serializers.SerializerMethodField()
    reviewed_by_name = serializers.SerializerMethodField()
    
    class Meta:
        model = PaymentReconciliation
        fields = [
            'id', 'payment_request', 'payment_request_details', 'bank_transaction_id',
            'bank_reference', 'reconciliation_status', 'matched_amount', 'matched_date',
            'confidence_score', 'reconciliation_notes', 'reviewed_by', 'reviewed_by_name',
            'reviewed_at', 'review_notes', 'created_at', 'updated_at'
        ]
        read_only_fields = ['id', 'created_at', 'updated_at']
    
    def get_payment_request_details(self, obj):
        """Get payment request details"""
        if obj.payment_request:
            return {
                'id': obj.payment_request.id,
                'amount': float(obj.payment_request.amount),
                'status': obj.payment_request.status,
                'transaction_reference': obj.payment_request.transaction_reference,
                'created_at': obj.payment_request.created_at
            }
        return None
    
    def get_reviewed_by_name(self, obj):
        """Get reviewer name"""
        if obj.reviewed_by:
            return obj.reviewed_by.get_full_name() or obj.reviewed_by.email
        return None


class PaymentDetailSerializer(serializers.ModelSerializer):
    """Serializer for payment details management"""
    payment_request_details = serializers.SerializerMethodField()
    verified_by_name = serializers.SerializerMethodField()
    
    class Meta:
        model = PaymentDetail
        fields = [
            'id', 'payment_request', 'payment_request_details', 'detail_type',
            'bank_name', 'account_number', 'transaction_id', 'reference_number',
            'actual_amount', 'transaction_date', 'settlement_date', 'payer_name',
            'payer_account', 'notes', 'is_verified', 'verified_by', 'verified_by_name',
            'verified_at', 'receipt_image', 'supporting_document', 'created_at', 'updated_at'
        ]
        read_only_fields = ['id', 'created_at', 'updated_at']
    
    def get_payment_request_details(self, obj):
        """Get payment request details"""
        if obj.payment_request:
            return {
                'id': obj.payment_request.id,
                'amount': float(obj.payment_request.amount),
                'status': obj.payment_request.status,
                'transaction_reference': obj.payment_request.transaction_reference,
                'created_at': obj.payment_request.created_at
            }
        return None
    
    def get_verified_by_name(self, obj):
        """Get verifier name"""
        if obj.verified_by:
            return obj.verified_by.get_full_name() or obj.verified_by.email
        return None


class ReconciliationSummarySerializer(serializers.Serializer):
    """Serializer for reconciliation summary statistics"""
    total_payments = serializers.IntegerField()
    pending_reconciliation = serializers.IntegerField()
    matched_payments = serializers.IntegerField()
    unmatched_payments = serializers.IntegerField()
    manual_review_required = serializers.IntegerField()
    total_amount_reconciled = serializers.DecimalField(max_digits=15, decimal_places=2)
    average_confidence_score = serializers.FloatField()
