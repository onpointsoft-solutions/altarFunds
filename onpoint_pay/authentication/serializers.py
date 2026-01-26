from rest_framework import serializers
from django.contrib.auth import authenticate
from merchants.models import Merchant, ApiKey


class MerchantRegistrationSerializer(serializers.ModelSerializer):
    """Serializer for merchant registration"""
    password = serializers.CharField(write_only=True, min_length=8)
    confirm_password = serializers.CharField(write_only=True)
    
    class Meta:
        model = Merchant
        fields = [
            'business_name', 'business_email', 'business_phone', 
            'first_name', 'last_name', 'password', 'confirm_password',
            'business_description', 'business_address', 'business_type',
            'registration_number', 'tax_identification', 'website'
        ]
    
    def validate(self, attrs):
        if attrs['password'] != attrs['confirm_password']:
            raise serializers.ValidationError("Passwords don't match")
        return attrs
    
    def create(self, validated_data):
        validated_data.pop('confirm_password')
        password = validated_data.pop('password')
        merchant = Merchant.objects.create_user(**validated_data)
        merchant.set_password(password)
        merchant.save()
        return merchant


class MerchantLoginSerializer(serializers.Serializer):
    """Serializer for merchant login"""
    email = serializers.EmailField()
    password = serializers.CharField()
    
    def validate(self, attrs):
        email = attrs.get('email')
        password = attrs.get('password')
        
        if email and password:
            merchant = authenticate(
                request=self.context.get('request'),
                username=email,
                password=password
            )
            
            if not merchant:
                raise serializers.ValidationError(
                    'Unable to log in with provided credentials.'
                )
            
            if not merchant.is_active:
                raise serializers.ValidationError('Merchant account is disabled.')
            
            attrs['merchant'] = merchant
            return attrs
        else:
            raise serializers.ValidationError(
                'Must include email and password.'
            )


class MerchantProfileSerializer(serializers.ModelSerializer):
    """Serializer for merchant profile"""
    full_name = serializers.ReadOnlyField()
    
    class Meta:
        model = Merchant
        fields = [
            'id', 'business_name', 'business_email', 'business_phone',
            'first_name', 'last_name', 'full_name', 'business_description',
            'business_address', 'business_type', 'registration_number',
            'tax_identification', 'website', 'logo', 'is_verified',
            'verification_date', 'default_currency', 'settlement_bank_account',
            'settlement_frequency', 'is_active', 'is_suspended',
            'suspension_reason', 'daily_transaction_limit',
            'monthly_transaction_limit', 'created_at', 'updated_at'
        ]
        read_only_fields = [
            'id', 'is_verified', 'verification_date', 'created_at', 'updated_at'
        ]


class ApiKeySerializer(serializers.ModelSerializer):
    """Serializer for API keys"""
    
    class Meta:
        model = ApiKey
        fields = [
            'id', 'name', 'key_type', 'public_key', 'secret_key',
            'permissions', 'allowed_ips', 'allowed_domains', 'is_active',
            'expires_at', 'last_used_at', 'usage_count', 'created_at'
        ]
        read_only_fields = [
            'id', 'public_key', 'secret_key', 'last_used_at', 'created_at'
        ]
