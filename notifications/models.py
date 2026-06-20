"""
Notification models — production-ready with:
  - Priority levels
  - Deduplication key
  - Delivery status tracking
  - Per-device platform tracking
  - Comprehensive indexes
"""
from django.db import models
from django.contrib.auth import get_user_model
from django.utils import timezone

User = get_user_model()


# ── Notification type choices (extend here when adding new types) ────────────
NOTIFICATION_TYPE_CHOICES = [
    ('devotional_shared',    'Devotional Shared'),
    ('announcement_posted',  'New Announcement'),
    ('devotional_new',       'New Devotional'),
    ('comment_added',        'New Comment'),
    ('like_received',        'Like Received'),
    ('giving_reminder',      'Giving Reminder'),
    ('church_event',         'Church Event'),
    ('prayer_request',       'Prayer Request'),
    ('payment_received',     'Payment Received'),
    ('general',              'General'),
]

PRIORITY_HIGH   = 'high'
PRIORITY_NORMAL = 'normal'
PRIORITY_LOW    = 'low'

PRIORITY_CHOICES = [
    (PRIORITY_HIGH,   'High'),
    (PRIORITY_NORMAL, 'Normal'),
    (PRIORITY_LOW,    'Low'),
]

PLATFORM_ANDROID = 'android'
PLATFORM_IOS     = 'ios'
PLATFORM_WEB     = 'web'

PLATFORM_CHOICES = [
    (PLATFORM_ANDROID, 'Android'),
    (PLATFORM_IOS,     'iOS'),
    (PLATFORM_WEB,     'Web'),
]


class FCMToken(models.Model):
    """
    Firebase Cloud Messaging device token.

    One user → many devices.
    Same device_id + user → always one active token (old one deactivated on update).
    """
    user        = models.ForeignKey(User, on_delete=models.CASCADE, related_name='fcm_tokens')
    token       = models.CharField(max_length=255, unique=True)
    device_id   = models.CharField(max_length=255, blank=True, null=True)
    platform    = models.CharField(max_length=10, choices=PLATFORM_CHOICES,
                                   default=PLATFORM_ANDROID)
    is_active   = models.BooleanField(default=True, db_index=True)
    last_used   = models.DateTimeField(null=True, blank=True)
    created_at  = models.DateTimeField(auto_now_add=True)
    updated_at  = models.DateTimeField(auto_now=True)

    class Meta:
        db_table = 'fcm_tokens'
        ordering = ['-created_at']
        indexes = [
            models.Index(fields=['user', 'is_active']),
            models.Index(fields=['device_id']),
            models.Index(fields=['updated_at']),
        ]

    def __str__(self):
        return f"{self.user.email} [{self.platform}] – {self.token[:24]}…"

    def mark_used(self):
        self.last_used = timezone.now()
        self.save(update_fields=['last_used'])


class PushNotification(models.Model):
    """
    Persisted push notification record.

    Always created in the database BEFORE being dispatched to FCM.
    The `sent_at` / `delivered` / `failed` fields track delivery lifecycle.
    `dedup_key` prevents the same logical notification from being stored
    or delivered more than once (idempotency).
    """

    DELIVERY_STATUS_CHOICES = [
        ('queued',     'Queued'),
        ('sent',       'Sent'),
        ('delivered',  'Delivered'),
        ('failed',     'Failed'),
        ('expired',    'Expired'),
    ]

    user              = models.ForeignKey(User, on_delete=models.CASCADE,
                                          related_name='push_notification_records')
    title             = models.CharField(max_length=200)
    message           = models.TextField()
    notification_type = models.CharField(max_length=50,
                                         choices=NOTIFICATION_TYPE_CHOICES,
                                         default='general', db_index=True)
    priority          = models.CharField(max_length=10, choices=PRIORITY_CHOICES,
                                         default=PRIORITY_NORMAL, db_index=True)
    data              = models.JSONField(default=dict, blank=True)
    target_url        = models.URLField(blank=True, null=True)

    # Lifecycle
    is_read           = models.BooleanField(default=False, db_index=True)
    delivery_status   = models.CharField(max_length=20,
                                         choices=DELIVERY_STATUS_CHOICES,
                                         default='queued', db_index=True)
    retry_count       = models.PositiveSmallIntegerField(default=0)
    max_retries       = models.PositiveSmallIntegerField(default=3)

    # Idempotency — callers set this to prevent duplicate delivery
    # e.g.  "devotional_new:42:user:7"
    dedup_key         = models.CharField(max_length=255, blank=True, null=True,
                                         db_index=True)

    # Scheduling
    scheduled_for     = models.DateTimeField(null=True, blank=True, db_index=True)

    # Timestamps
    created_at        = models.DateTimeField(auto_now_add=True, db_index=True)
    sent_at           = models.DateTimeField(null=True, blank=True)
    expires_at        = models.DateTimeField(null=True, blank=True)

    class Meta:
        db_table = 'push_notifications'
        ordering = ['-created_at']
        indexes = [
            models.Index(fields=['user', 'is_read']),
            models.Index(fields=['user', 'delivery_status']),
            models.Index(fields=['scheduled_for', 'delivery_status']),
            models.Index(fields=['dedup_key']),
        ]

    def __str__(self):
        return f"[{self.notification_type}] {self.title} → {self.user.email}"

    @property
    def is_expired(self):
        return self.expires_at is not None and timezone.now() > self.expires_at

    @property
    def can_retry(self):
        return (
            self.delivery_status == 'failed' and
            self.retry_count < self.max_retries
        )

    def mark_sent(self):
        self.delivery_status = 'sent'
        self.sent_at = timezone.now()
        self.save(update_fields=['delivery_status', 'sent_at'])

    def mark_failed(self, increment_retry: bool = True):
        if increment_retry:
            self.retry_count += 1
        self.delivery_status = 'failed' if self.retry_count >= self.max_retries else 'queued'
        self.save(update_fields=['delivery_status', 'retry_count'])

    def mark_expired(self):
        self.delivery_status = 'expired'
        self.save(update_fields=['delivery_status'])


class NotificationPreference(models.Model):
    """Per-user notification opt-in/out settings."""

    user                      = models.OneToOneField(
        User, on_delete=models.CASCADE,
        related_name='notification_preferences'
    )
    push_enabled              = models.BooleanField(default=True)
    email_enabled             = models.BooleanField(default=True)
    devotional_notifications  = models.BooleanField(default=True)
    announcement_notifications= models.BooleanField(default=True)
    giving_notifications      = models.BooleanField(default=True)
    event_notifications       = models.BooleanField(default=True)
    updated_at                = models.DateTimeField(auto_now=True)

    class Meta:
        db_table = 'notification_preferences'

    def allows_type(self, notification_type: str) -> bool:
        """Return True if the user has opted in to this notification type."""
        if not self.push_enabled:
            return False
        type_map = {
            'devotional_new':    self.devotional_notifications,
            'devotional_shared': self.devotional_notifications,
            'announcement_posted': self.announcement_notifications,
            'giving_reminder':   self.giving_notifications,
            'church_event':      self.event_notifications,
        }
        # Default to True for unmapped types (e.g. 'general')
        return type_map.get(notification_type, True)


class DevotionalShare(models.Model):
    """Track when devotionals are shared between users."""
    user       = models.ForeignKey(User, on_delete=models.CASCADE, related_name='shares')
    devotional = models.ForeignKey('devotionals.Devotional', on_delete=models.CASCADE,
                                   related_name='shared_devotionals')
    shared_by  = models.ForeignKey(User, on_delete=models.CASCADE,
                                   related_name='shared_by_users')
    shared_at  = models.DateTimeField(auto_now_add=True, db_index=True)
    message    = models.TextField(blank=True, null=True)
    is_read    = models.BooleanField(default=False)

    class Meta:
        db_table = 'devotional_shares'
        ordering = ['-shared_at']
        indexes = [
            models.Index(fields=['user', 'is_read']),
        ]
