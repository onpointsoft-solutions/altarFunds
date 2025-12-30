from django.db import models
from django.utils.translation import gettext_lazy as _
from common.models import TimeStampedModel
from accounts.models import User, Member


class MobileDevice(TimeStampedModel):
    """Mobile device registration for push notifications"""
    
    DEVICE_TYPE_CHOICES = [
        ('android', _('Android')),
        ('ios', _('iOS')),
        ('web', _('Web')),
    ]
    
    STATUS_CHOICES = [
        ('active', _('Active')),
        ('inactive', _('Inactive')),
        ('disabled', _('Disabled')),
    ]
    
    user = models.ForeignKey(
        User,
        on_delete=models.CASCADE,
        related_name='mobile_devices'
    )
    device_token = models.CharField(_('Device Token'), max_length=255, unique=True)
    device_type = models.CharField(
        _('Device Type'),
        max_length=10,
        choices=DEVICE_TYPE_CHOICES
    )
    device_id = models.CharField(_('Device ID'), max_length=100, blank=True)
    app_version = models.CharField(_('App Version'), max_length=20, blank=True)
    os_version = models.CharField(_('OS Version'), max_length=20, blank=True)
    
    # Status
    status = models.CharField(
        _('Status'),
        max_length=10,
        choices=STATUS_CHOICES,
        default='active'
    )
    last_seen = models.DateTimeField(_('Last Seen'), null=True, blank=True)
    
    class Meta:
        db_table = 'mobile_devices'
        verbose_name = _('Mobile Device')
        verbose_name_plural = _('Mobile Devices')
        ordering = ['-created_at']
        indexes = [
            models.Index(fields=['user']),
            models.Index(fields=['device_token']),
            models.Index(fields=['device_type']),
            models.Index(fields=['status']),
        ]
    
    def __str__(self):
        return f"{self.user.email} - {self.get_device_type_display()}"


class MobileAppSettings(TimeStampedModel):
    """Mobile app configuration settings"""
    
    key = models.CharField(_('Setting Key'), max_length=100, unique=True)
    value = models.JSONField(_('Setting Value'))
    description = models.TextField(_('Description'), blank=True)
    is_active = models.BooleanField(_('Active'), default=True)
    
    class Meta:
        db_table = 'mobile_app_settings'
        verbose_name = _('Mobile App Setting')
        verbose_name_plural = _('Mobile App Settings')
        ordering = ['key']
    
    def __str__(self):
        return self.key


class MobileAppVersion(TimeStampedModel):
    """Mobile app version management"""
    
    PLATFORM_CHOICES = [
        ('android', _('Android')),
        ('ios', _('iOS')),
    ]
    
    STATUS_CHOICES = [
        ('development', _('Development')),
        ('testing', _('Testing')),
        ('production', _('Production')),
        ('deprecated', _('Deprecated')),
    ]
    
    platform = models.CharField(
        _('Platform'),
        max_length=10,
        choices=PLATFORM_CHOICES
    )
    version = models.CharField(_('Version'), max_length=20)
    build_number = models.PositiveIntegerField(_('Build Number'))
    status = models.CharField(
        _('Status'),
        max_length=15,
        choices=STATUS_CHOICES,
        default='development'
    )
    
    # Update Information
    is_mandatory = models.BooleanField(_('Mandatory Update'), default=False)
    update_message = models.TextField(_('Update Message'), blank=True)
    download_url = models.URLField(_('Download URL'), blank=True)
    
    # Compatibility
    min_supported_version = models.CharField(_('Min Supported Version'), max_length=20, blank=True)
    release_notes = models.TextField(_('Release Notes'), blank=True)
    
    class Meta:
        db_table = 'mobile_app_versions'
        verbose_name = _('Mobile App Version')
        verbose_name_plural = _('Mobile App Versions')
        ordering = ['-created_at']
        unique_together = ['platform', 'version']
        indexes = [
            models.Index(fields=['platform']),
            models.Index(fields=['status']),
            models.Index(fields=['is_mandatory']),
        ]
    
    def __str__(self):
        return f"{self.get_platform_display()} v{self.version}"


class UserSession(TimeStampedModel):
    """User session tracking for mobile app"""
    
    user = models.ForeignKey(
        User,
        on_delete=models.CASCADE,
        related_name='mobile_sessions'
    )
    device = models.ForeignKey(
        MobileDevice,
        on_delete=models.SET_NULL,
        null=True,
        blank=True,
        related_name='sessions'
    )
    
    # Session Information
    session_token = models.CharField(_('Session Token'), max_length=255)
    ip_address = models.GenericIPAddressField(_('IP Address'))
    user_agent = models.TextField(_('User Agent'), blank=True)
    
    # Status
    is_active = models.BooleanField(_('Active'), default=True)
    last_activity = models.DateTimeField(_('Last Activity'), auto_now=True)
    expires_at = models.DateTimeField(_('Expires At'))
    
    class Meta:
        db_table = 'mobile_user_sessions'
        verbose_name = _('User Session')
        verbose_name_plural = _('User Sessions')
        ordering = ['-created_at']
        indexes = [
            models.Index(fields=['user']),
            models.Index(fields=['device']),
            models.Index(fields=['session_token']),
            models.Index(fields=['is_active']),
            models.Index(fields=['expires_at']),
        ]
    
    def __str__(self):
        return f"{self.user.email} - {self.created_at}"


class MobileNotification(TimeStampedModel):
    """Mobile push notification records"""
    
    NOTIFICATION_TYPE_CHOICES = [
        ('giving', _('Giving')),
        ('church', _('Church')),
        ('event', _('Event')),
        ('announcement', _('Announcement')),
        ('system', _('System')),
    ]
    
    STATUS_CHOICES = [
        ('pending', _('Pending')),
        ('sent', _('Sent')),
        ('delivered', _('Delivered')),
        ('failed', _('Failed')),
        ('opened', _('Opened')),
    ]
    
    # Recipient
    user = models.ForeignKey(
        User,
        on_delete=models.CASCADE,
        related_name='mobile_notifications'
    )
    device = models.ForeignKey(
        MobileDevice,
        on_delete=models.CASCADE,
        related_name='notifications'
    )
    
    # Content
    notification_type = models.CharField(
        _('Notification Type'),
        max_length=20,
        choices=NOTIFICATION_TYPE_CHOICES
    )
    title = models.CharField(_('Title'), max_length=200)
    message = models.TextField(_('Message'))
    data = models.JSONField(_('Additional Data'), null=True, blank=True)
    
    # Status
    status = models.CharField(
        _('Status'),
        max_length=20,
        choices=STATUS_CHOICES,
        default='pending'
    )
    sent_at = models.DateTimeField(_('Sent At'), null=True, blank=True)
    delivered_at = models.DateTimeField(_('Delivered At'), null=True, blank=True)
    opened_at = models.DateTimeField(_('Opened At'), null=True, blank=True)
    
    # Response
    response_data = models.JSONField(_('Response Data'), null=True, blank=True)
    error_message = models.TextField(_('Error Message'), blank=True)
    
    class Meta:
        db_table = 'mobile_notifications'
        verbose_name = _('Mobile Notification')
        verbose_name_plural = _('Mobile Notifications')
        ordering = ['-created_at']
        indexes = [
            models.Index(fields=['user']),
            models.Index(fields=['device']),
            models.Index(fields=['notification_type']),
            models.Index(fields=['status']),
            models.Index(fields=['created_at']),
        ]
    
    def __str__(self):
        return f"{self.user.email} - {self.title}"


class MobileAppAnalytics(TimeStampedModel):
    """Mobile app usage analytics"""
    
    user = models.ForeignKey(
        User,
        on_delete=models.CASCADE,
        related_name='mobile_analytics'
    )
    device = models.ForeignKey(
        MobileDevice,
        on_delete=models.CASCADE,
        related_name='analytics'
    )
    
    # Event Information
    event_type = models.CharField(_('Event Type'), max_length=50)
    event_name = models.CharField(_('Event Name'), max_length=100)
    event_data = models.JSONField(_('Event Data'), null=True, blank=True)
    
    # Context
    screen_name = models.CharField(_('Screen Name'), max_length=100, blank=True)
    session_id = models.CharField(_('Session ID'), max_length=100, blank=True)
    
    # Timestamps
    event_timestamp = models.DateTimeField(_('Event Timestamp'))
    
    class Meta:
        db_table = 'mobile_app_analytics'
        verbose_name = _('Mobile App Analytic')
        verbose_name_plural = _('Mobile App Analytics')
        ordering = ['-event_timestamp']
        indexes = [
            models.Index(fields=['user']),
            models.Index(fields=['device']),
            models.Index(fields=['event_type']),
            models.Index(fields=['event_timestamp']),
        ]
    
    def __str__(self):
        return f"{self.user.email} - {self.event_type}"


class MobileAppFeedback(TimeStampedModel):
    """Mobile app user feedback"""
    
    FEEDBACK_TYPE_CHOICES = [
        ('bug', _('Bug Report')),
        ('feature', _('Feature Request')),
        ('general', _('General Feedback')),
        ('rating', _('Rating')),
    ]
    
    STATUS_CHOICES = [
        ('open', _('Open')),
        ('in_progress', _('In Progress')),
        ('resolved', _('Resolved')),
        ('closed', _('Closed')),
    ]
    
    user = models.ForeignKey(
        User,
        on_delete=models.CASCADE,
        related_name='mobile_feedback'
    )
    device = models.ForeignKey(
        MobileDevice,
        on_delete=models.SET_NULL,
        null=True,
        blank=True,
        related_name='feedback'
    )
    
    # Feedback Details
    feedback_type = models.CharField(
        _('Feedback Type'),
        max_length=20,
        choices=FEEDBACK_TYPE_CHOICES
    )
    title = models.CharField(_('Title'), max_length=200)
    description = models.TextField(_('Description'))
    
    # Rating (for rating feedback)
    rating = models.PositiveIntegerField(_('Rating'), null=True, blank=True)
    
    # Status
    status = models.CharField(
        _('Status'),
        max_length=20,
        choices=STATUS_CHOICES,
        default='open'
    )
    
    # Response
    admin_response = models.TextField(_('Admin Response'), blank=True)
    responded_by = models.ForeignKey(
        User,
        on_delete=models.SET_NULL,
        null=True,
        blank=True,
        related_name='responded_feedback'
    )
    responded_at = models.DateTimeField(_('Responded At'), null=True, blank=True)
    
    class Meta:
        db_table = 'mobile_app_feedback'
        verbose_name = _('Mobile App Feedback')
        verbose_name_plural = _('Mobile App Feedback')
        ordering = ['-created_at']
        indexes = [
            models.Index(fields=['user']),
            models.Index(fields=['feedback_type']),
            models.Index(fields=['status']),
            models.Index(fields=['rating']),
        ]
    
    def __str__(self):
        return f"{self.user.email} - {self.title}"
