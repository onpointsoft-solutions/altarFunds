from rest_framework import serializers
from .models import DevotionalShare, PushNotification, NotificationPreference, FCMToken


class FCMTokenSerializer(serializers.ModelSerializer):
    """
    FCM token serializer — write-only token for security.
    The 'token' field is write-only so it is never returned in API responses.
    """

    class Meta:
        model  = FCMToken
        fields = ['id', 'token', 'device_id', 'platform', 'is_active', 'created_at']
        read_only_fields = ['id', 'is_active', 'created_at']
        extra_kwargs = {
            'token':     {'write_only': True},
            'device_id': {'required': False},
            'platform':  {'required': False},
        }

    def validate_token(self, value):
        if not value or len(value.strip()) < 100:
            raise serializers.ValidationError(
                "FCM token must be at least 100 characters."
            )
        return value.strip()


class DevotionalShareSerializer(serializers.ModelSerializer):
    shared_by_name = serializers.SerializerMethodField()
    devotional_title = serializers.CharField(source='devotional.title', read_only=True)

    class Meta:
        model  = DevotionalShare
        fields = [
            'id', 'user', 'devotional', 'devotional_title',
            'shared_by', 'shared_by_name', 'shared_at', 'message', 'is_read',
        ]
        read_only_fields = ['id', 'shared_at']

    def get_shared_by_name(self, obj):
        try:
            return obj.shared_by.get_full_name() or obj.shared_by.email
        except Exception:
            return None


class PushNotificationSerializer(serializers.ModelSerializer):
    class Meta:
        model  = PushNotification
        fields = [
            'id', 'title', 'message', 'notification_type', 'priority',
            'data', 'target_url', 'is_read', 'delivery_status',
            'created_at', 'expires_at',
        ]
        read_only_fields = ['id', 'created_at', 'delivery_status']


class NotificationPreferenceSerializer(serializers.ModelSerializer):
    class Meta:
        model  = NotificationPreference
        fields = [
            'push_enabled', 'email_enabled',
            'devotional_notifications', 'announcement_notifications',
            'giving_notifications', 'event_notifications',
        ]
