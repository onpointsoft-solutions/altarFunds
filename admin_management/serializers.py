from rest_framework import serializers
from django.db import models
from accounts.models import User
from churches.models import Church
from giving.models import GivingTransaction, GivingCategory
from payments.models import Payment

class SuperAdminStatsSerializer(serializers.Serializer):
    total_churches = serializers.IntegerField()
    active_churches = serializers.IntegerField()
    total_users = serializers.IntegerField()
    active_users = serializers.IntegerField()
    total_revenue = serializers.DecimalField(max_digits=15, decimal_places=2)
    monthly_revenue = serializers.DecimalField(max_digits=15, decimal_places=2)
    total_transactions = serializers.IntegerField()
    monthly_transactions = serializers.IntegerField()

class ChurchAdminSerializer(serializers.ModelSerializer):
    subscription_plan = serializers.StringRelatedField(read_only=True)
    member_count = serializers.SerializerMethodField()
    monthly_revenue = serializers.SerializerMethodField()
    
    class Meta:
        model = Church
        fields = [
            'id', 'name', 'code', 'email', 'phone', 'address', 'city', 'country',
            'is_active', 'subscription_plan', 'member_count', 'monthly_revenue',
            'created_at', 'updated_at'
        ]
    
    def get_member_count(self, obj):
        return User.objects.filter(church=obj).count()
    
    def get_monthly_revenue(self, obj):
        from django.utils import timezone
        from datetime import timedelta
        return Payment.objects.filter(
            church=obj,
            created_at__gte=timezone.now() - timedelta(days=30)
        ).aggregate(total=models.Sum('amount'))['total'] or 0

class SystemNotificationSerializer(serializers.ModelSerializer):
    class Meta:
        model = 'admin_management.SystemNotification'
        fields = ['id', 'title', 'message', 'type', 'is_active', 'created_at']

class ChurchActivitySerializer(serializers.ModelSerializer):
    user_name = serializers.StringRelatedField(source='user', read_only=True)
    church_name = serializers.StringRelatedField(source='church', read_only=True)
    
    class Meta:
        model = 'admin_management.ChurchActivity'
        fields = ['id', 'church', 'church_name', 'user', 'user_name', 'action', 'description', 'created_at']

class SubscriptionPlanSerializer(serializers.ModelSerializer):
    class Meta:
        model = 'admin_management.SubscriptionPlan'
        fields = ['id', 'name', 'price', 'duration', 'features', 'is_active']

class ChurchAnalyticsSerializer(serializers.Serializer):
    memberGrowth = serializers.ListField()
    donationTrends = serializers.ListField()
    engagementMetrics = serializers.DictField()

class SystemHealthSerializer(serializers.Serializer):
    status = serializers.CharField()
    services = serializers.ListField()
    metrics = serializers.DictField()
