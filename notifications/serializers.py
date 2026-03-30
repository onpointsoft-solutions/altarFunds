from rest_framework import serializers
from .models import DevotionalShare, PushNotification, NotificationPreference

class DevotionalShareSerializer(serializers.ModelSerializer):
    """Serializer for devotional sharing"""
    
    class Meta:
        model = DevotionalShare
        fields = [
            'id', 'user', 'devotional', 'shared_by', 
            'shared_at', 'message', 'is_read'
        ]
        read_only_fields = ['id', 'shared_at']

class PushNotificationSerializer(serializers.ModelSerializer):
    """Serializer for push notifications"""
    
    class Meta:
        model = PushNotification
        fields = [
            'id', 'user', 'title', 'message', 'notification_type',
            'data', 'target_url', 'is_read', 'created_at', 'expires_at'
        ]
        read_only_fields = ['id', 'created_at']

class NotificationPreferenceSerializer(serializers.ModelSerializer):
    """Serializer for notification preferences"""
    
    class Meta:
        model = NotificationPreference
        fields = [
            'push_enabled', 'email_enabled', 
            'devotional_notifications', 'announcement_notifications'
        ]
