from rest_framework import serializers
from .models import DevotionalShare, PushNotification, NotificationPreference, FCMToken

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


class FCMTokenSerializer(serializers.ModelSerializer):
    """Serializer for FCM tokens"""
    
    class Meta:
        model = FCMToken
        fields = ['token', 'device_id', 'is_active']
        extra_kwargs = {
            'token': {'write_only': True},
            'device_id': {'write_only': True}
        }
    
    def validate_token(self, value):
        """Validate FCM token format"""
        if not value or len(value) < 100:
            raise serializers.ValidationError("Invalid FCM token format")
        return value
