from django.contrib import admin
from django.utils.html import format_html
from django.utils.translation import gettext_lazy as _
from .models import (
    MobileDevice, MobileAppSettings, MobileAppVersion, 
    UserSession, MobileNotification, MobileAppAnalytics, MobileAppFeedback
)
from common.admin import BaseAdmin, colored_status


@admin.register(MobileDevice)
class MobileDeviceAdmin(BaseAdmin):
    """Mobile device admin"""
    
    list_display = [
        'user_email', 'device_type_display', 'device_token_short',
        'app_version', 'status_colored', 'last_seen', 'created_at'
    ]
    list_filter = [
        'device_type', 'status', 'app_version', 'created_at'
    ]
    search_fields = [
        'user__email', 'user__first_name', 'user__last_name',
        'device_token', 'device_id'
    ]
    ordering = ['-created_at']
    
    fieldsets = (
        (None, {
            'fields': ('user', 'device_token', 'device_type', 'device_id')
        }),
        ('Device Information', {
            'fields': ('app_version', 'os_version')
        }),
        ('Status', {
            'fields': ('status', 'last_seen')
        }),
        ('Timestamps', {
            'fields': ('created_at', 'updated_at'),
            'classes': ('collapse',)
        }),
    )
    
    readonly_fields = ['created_at', 'updated_at']
    
    def user_email(self, obj):
        return obj.user.email
    user_email.short_description = 'User'
    
    def device_type_display(self, obj):
        color = {
            'android': 'green',
            'ios': 'blue',
            'web': 'orange'
        }.get(obj.device_type, 'black')
        
        return format_html(
            '<span style="color: {}; font-weight: bold;">{}</span>',
            color,
            obj.get_device_type_display()
        )
    device_type_display.short_description = 'Device Type'
    
    def device_token_short(self, obj):
        return str(obj.device_token)[:20] + '...' if len(obj.device_token) > 20 else obj.device_token
    device_token_short.short_description = 'Device Token'
    
    def status_colored(self, obj):
        colors = {
            'active': 'green',
            'inactive': 'orange',
            'disabled': 'red'
        }
        color = colors.get(obj.status, 'black')
        return colored_status(obj, color)
    status_colored.short_description = 'Status'


@admin.register(MobileAppSettings)
class MobileAppSettingsAdmin(BaseAdmin):
    """Mobile app settings admin"""
    
    list_display = ['key', 'value_preview', 'description', 'is_active_colored']
    list_filter = ['is_active']
    search_fields = ['key', 'description']
    ordering = ['key']
    
    def value_preview(self, obj):
        if isinstance(obj.value, dict):
            return format_html('<code>{}</code>', str(obj.value)[:50] + '...')
        return str(obj.value)[:50] + '...' if len(str(obj.value)) > 50 else str(obj.value)
    value_preview.short_description = 'Value'
    
    def is_active_colored(self, obj):
        if obj.is_active:
            color = 'green'
            status = 'Active'
        else:
            color = 'red'
            status = 'Inactive'
        
        return format_html(
            '<span style="color: {}; font-weight: bold;">{}</span>',
            color,
            status
        )
    is_active_colored.short_description = 'Status'


@admin.register(MobileAppVersion)
class MobileAppVersionAdmin(BaseAdmin):
    """Mobile app version admin"""
    
    list_display = [
        'platform_display', 'version', 'build_number', 'status_display',
        'is_mandatory_colored', 'created_at'
    ]
    list_filter = [
        'platform', 'status', 'is_mandatory', 'created_at'
    ]
    search_fields = ['version', 'release_notes']
    ordering = ['-created_at']
    
    def platform_display(self, obj):
        color = {
            'android': 'green',
            'ios': 'blue'
        }.get(obj.platform, 'black')
        
        return format_html(
            '<span style="color: {}; font-weight: bold;">{}</span>',
            color,
            obj.get_platform_display()
        )
    platform_display.short_description = 'Platform'
    
    def status_display(self, obj):
        colors = {
            'development': 'gray',
            'testing': 'orange',
            'production': 'green',
            'deprecated': 'red'
        }
        color = colors.get(obj.status, 'black')
        return colored_status(obj, color)
    status_display.short_description = 'Status'
    
    def is_mandatory_colored(self, obj):
        if obj.is_mandatory:
            color = 'red'
            status = 'Mandatory'
        else:
            color = 'green'
            status = 'Optional'
        
        return format_html(
            '<span style="color: {}; font-weight: bold;">{}</span>',
            color,
            status
        )
    is_mandatory_colored.short_description = 'Update Type'


@admin.register(UserSession)
class UserSessionAdmin(BaseAdmin):
    """User session admin"""
    
    list_display = [
        'user_email', 'device_info', 'session_token_short',
        'ip_address', 'is_active_colored', 'expires_at', 'last_activity'
    ]
    list_filter = [
        'is_active', 'created_at', 'expires_at'
    ]
    search_fields = [
        'user__email', 'session_token', 'ip_address'
    ]
    ordering = ['-created_at']
    
    def user_email(self, obj):
        return obj.user.email
    user_email.short_description = 'User'
    
    def device_info(self, obj):
        if obj.device:
            return f"{obj.device.get_device_type_display()} ({obj.device.app_version})"
        return 'No device'
    device_info.short_description = 'Device'
    
    def session_token_short(self, obj):
        return str(obj.session_token)[:20] + '...' if len(obj.session_token) > 20 else obj.session_token
    session_token_short.short_description = 'Session Token'
    
    def is_active_colored(self, obj):
        if obj.is_active:
            color = 'green'
            status = 'Active'
        else:
            color = 'red'
            status = 'Inactive'
        
        return format_html(
            '<span style="color: {}; font-weight: bold;">{}</span>',
            color,
            status
        )
    is_active_colored.short_description = 'Status'


@admin.register(MobileNotification)
class MobileNotificationAdmin(BaseAdmin):
    """Mobile notification admin"""
    
    list_display = [
        'user_email', 'title', 'notification_type_display',
        'status_display', 'sent_at', 'delivered_at', 'created_at'
    ]
    list_filter = [
        'notification_type', 'status', 'sent_at', 'created_at'
    ]
    search_fields = [
        'user__email', 'title', 'message'
    ]
    ordering = ['-created_at']
    
    def user_email(self, obj):
        return obj.user.email
    user_email.short_description = 'User'
    
    def notification_type_display(self, obj):
        color = {
            'giving': 'green',
            'church': 'blue',
            'event': 'orange',
            'announcement': 'purple',
            'system': 'gray'
        }
        color = color.get(obj.notification_type, 'black')
        
        return format_html(
            '<span style="color: {}; font-weight: bold;">{}</span>',
            color,
            obj.get_notification_type_display()
        )
    notification_type_display.short_description = 'Type'
    
    def status_display(self, obj):
        colors = {
            'pending': 'orange',
            'sent': 'blue',
            'delivered': 'green',
            'failed': 'red',
            'opened': 'purple'
        }
        color = colors.get(obj.status, 'black')
        return colored_status(obj, color)
    status_display.short_description = 'Status'


@admin.register(MobileAppAnalytics)
class MobileAppAnalyticsAdmin(BaseAdmin):
    """Mobile app analytics admin"""
    
    list_display = [
        'user_email', 'device_info', 'event_type', 'event_name',
        'screen_name', 'event_timestamp'
    ]
    list_filter = [
        'event_type', 'event_name', 'screen_name', 'event_timestamp'
    ]
    search_fields = [
        'user__email', 'event_name', 'screen_name'
    ]
    ordering = ['-event_timestamp']
    
    def user_email(self, obj):
        return obj.user.email
    user_email.short_description = 'User'
    
    def device_info(self, obj):
        if obj.device:
            return obj.device.get_device_type_display()
        return 'No device'
    device_info.short_description = 'Device'


@admin.register(MobileAppFeedback)
class MobileAppFeedbackAdmin(BaseAdmin):
    """Mobile app feedback admin"""
    
    list_display = [
        'user_email', 'title', 'feedback_type_display', 'rating_display',
        'status_display', 'responded_by', 'created_at'
    ]
    list_filter = [
        'feedback_type', 'status', 'rating', 'created_at'
    ]
    search_fields = [
        'user__email', 'title', 'description'
    ]
    ordering = ['-created_at']
    
    def user_email(self, obj):
        return obj.user.email
    user_email.short_description = 'User'
    
    def feedback_type_display(self, obj):
        color = {
            'bug': 'red',
            'feature': 'blue',
            'general': 'green',
            'rating': 'orange'
        }
        color = color.get(obj.feedback_type, 'black')
        
        return format_html(
            '<span style="color: {}; font-weight: bold;">{}</span>',
            color,
            obj.get_feedback_type_display()
        )
    feedback_type_display.short_description = 'Type'
    
    def rating_display(self, obj):
        if obj.rating:
            stars = '★' * obj.rating + '☆' * (5 - obj.rating)
            return format_html(
                '<span style="color: gold; font-weight: bold;">{}</span>',
                stars
            )
        return '-'
    rating_display.short_description = 'Rating'
    
    def status_display(self, obj):
        colors = {
            'open': 'orange',
            'in_progress': 'blue',
            'resolved': 'green',
            'closed': 'gray'
        }
        color = colors.get(obj.status, 'black')
        return colored_status(obj, color)
    status_display.short_description = 'Status'
    
    def responded_by(self, obj):
        return obj.responded_by.get_full_name() if obj.responded_by else '-'
    responded_by.short_description = 'Responded By'
