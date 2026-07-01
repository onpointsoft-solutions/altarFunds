from django.contrib import admin
from django.utils.html import format_html
from .models import FCMToken, PushNotification, NotificationPreference, DevotionalShare


@admin.register(FCMToken)
class FCMTokenAdmin(admin.ModelAdmin):
    list_display  = ('user', 'platform', 'device_id', 'is_active', 'last_used', 'created_at')
    list_filter   = ('platform', 'is_active')
    search_fields = ('user__email', 'device_id', 'token')
    readonly_fields = ('token', 'created_at', 'updated_at', 'last_used')
    ordering      = ('-created_at',)
    actions       = ['deactivate_tokens']

    @admin.action(description='Deactivate selected tokens')
    def deactivate_tokens(self, request, queryset):
        updated = queryset.update(is_active=False)
        self.message_user(request, f'{updated} token(s) deactivated.')


@admin.register(PushNotification)
class PushNotificationAdmin(admin.ModelAdmin):
    list_display  = (
        'id', 'user', 'notification_type', 'title_truncated',
        'delivery_status', 'priority', 'is_read', 'retry_count', 'created_at',
    )
    list_filter   = ('notification_type', 'delivery_status', 'priority', 'is_read')
    search_fields = ('user__email', 'title', 'message', 'dedup_key')
    readonly_fields = ('created_at', 'sent_at', 'delivery_status', 'retry_count')
    ordering      = ('-created_at',)
    actions       = ['mark_as_read', 'retry_delivery']

    def title_truncated(self, obj):
        return obj.title[:60] + '…' if len(obj.title) > 60 else obj.title
    title_truncated.short_description = 'Title'

    @admin.action(description='Mark selected notifications as read')
    def mark_as_read(self, request, queryset):
        updated = queryset.update(is_read=True)
        self.message_user(request, f'{updated} notification(s) marked as read.')

    @admin.action(description='Retry delivery for selected notifications')
    def retry_delivery(self, request, queryset):
        from .tasks import deliver_notification
        count = 0
        for notif in queryset.filter(delivery_status='failed'):
            deliver_notification.delay(notif.pk)
            count += 1
        self.message_user(request, f'{count} notification(s) re-queued for delivery.')


@admin.register(NotificationPreference)
class NotificationPreferenceAdmin(admin.ModelAdmin):
    list_display  = (
        'user', 'push_enabled', 'email_enabled',
        'devotional_notifications', 'announcement_notifications',
        'giving_notifications', 'event_notifications',
    )
    search_fields = ('user__email',)


@admin.register(DevotionalShare)
class DevotionalShareAdmin(admin.ModelAdmin):
    list_display  = ('shared_by', 'user', 'devotional', 'shared_at', 'is_read')
    list_filter   = ('is_read',)
    search_fields = ('shared_by__email', 'user__email')
    ordering      = ('-shared_at',)
