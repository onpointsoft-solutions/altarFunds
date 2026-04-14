from django.db import models
from django.contrib.auth import get_user_model
from accounts.models import User

User = get_user_model()

class DevotionalShare(models.Model):
    """Track when devotionals are shared"""
    user = models.ForeignKey(User, on_delete=models.CASCADE, related_name='shares')
    devotional = models.ForeignKey('devotionals.Devotional', on_delete=models.CASCADE, related_name='shared_devotionals')
    shared_by = models.ForeignKey(User, on_delete=models.CASCADE, related_name='shared_by_users')
    shared_at = models.DateTimeField(auto_now_add=True)
    message = models.TextField(blank=True, null=True)
    is_read = models.BooleanField(default=False)
    
    class Meta:
        db_table = 'devotional_shares'
        ordering = ['-shared_at']

class PushNotification(models.Model):
    """Push notifications for mobile app"""
    user = models.ForeignKey(User, on_delete=models.CASCADE, related_name='push_notification_records')
    title = models.CharField(max_length=200)
    message = models.TextField()
    notification_type = models.CharField(max_length=50, choices=[
        ('devotional_shared', 'Devotional Shared'),
        ('announcement_posted', 'New Announcement'),
        ('devotional_new', 'New Devotional'),
        ('comment_added', 'New Comment'),
        ('like_received', 'Like Received'),
    ])
    data = models.JSONField(default=dict, blank=True, null=True)
    target_url = models.URLField(blank=True, null=True)
    is_read = models.BooleanField(default=False)
    created_at = models.DateTimeField(auto_now_add=True)
    expires_at = models.DateTimeField(null=True, blank=True)
    
    class Meta:
        db_table = 'push_notifications'
        ordering = ['-created_at']

class NotificationPreference(models.Model):
    """User notification preferences"""
    user = models.OneToOneField(User, on_delete=models.CASCADE, related_name='notification_preferences')
    push_enabled = models.BooleanField(default=True)
    email_enabled = models.BooleanField(default=True)
    devotional_notifications = models.BooleanField(default=True)
    announcement_notifications = models.BooleanField(default=True)
    
    class Meta:
        db_table = 'notification_preferences'

class FCMToken(models.Model):
    """Firebase Cloud Messaging tokens for push notifications"""
    user = models.ForeignKey(User, on_delete=models.CASCADE, related_name='fcm_tokens')
    token = models.CharField(max_length=255, unique=True)
    device_id = models.CharField(max_length=255, blank=True, null=True)
    is_active = models.BooleanField(default=True)
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)
    
    class Meta:
        db_table = 'fcm_tokens'
        ordering = ['-created_at']
    
    def __str__(self):
        return f"{self.user.email} - {self.token[:20]}..."