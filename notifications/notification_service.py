"""
NotificationService — single entry point for all notification sending.

Design principles:
  - All FCM sends are dispatched to Celery tasks so the caller's HTTP
    response is never blocked.
  - Every notification is persisted to the DB BEFORE being enqueued,
    so we have an audit trail even if the task queue is lost.
  - Idempotency: callers may pass a `dedup_key`; if a notification with
    that key already exists and was successfully sent, the call is a no-op.
  - Invalid / expired FCM tokens are automatically deactivated on delivery
    failure so they are never used again.
  - Respects per-user NotificationPreference opt-outs.
  - Multi-device: sends to every active token a user has registered.
"""
from __future__ import annotations

import logging
from datetime import datetime
from typing import Any, Dict, List, Optional

from django.db import transaction
from django.utils import timezone

from .models import (
    FCMToken, PushNotification, NotificationPreference,
    PRIORITY_NORMAL, PRIORITY_HIGH,
    NOTIFICATION_TYPE_CHOICES,
)

logger = logging.getLogger(__name__)

# Token errors from Firebase that mean the token is permanently invalid
INVALID_TOKEN_ERRORS = frozenset({
    'registration-token-not-registered',
    'invalid-registration-token',
    'invalid-argument',
    'mismatched-credential',
})


class NotificationService:
    """
    Reusable notification service.  Import and call from anywhere:

        from notifications.notification_service import NotificationService

        NotificationService.send(
            user          = user,
            title         = "New announcement",
            message       = "Sunday service is at 9 am",
            notification_type = "announcement_posted",
            data          = {"announcement_id": 42},
        )
    """

    # ── Public API ────────────────────────────────────────────────────────

    @classmethod
    def send(
        cls,
        user,
        title: str,
        message: str,
        notification_type: str = 'general',
        data: Optional[Dict[str, Any]] = None,
        target_url: Optional[str] = None,
        priority: str = PRIORITY_NORMAL,
        dedup_key: Optional[str] = None,
        scheduled_for: Optional[datetime] = None,
        expires_in_hours: Optional[int] = 48,
    ) -> Optional[PushNotification]:
        """
        Persist a PushNotification and enqueue a Celery task to deliver it.

        Returns the PushNotification instance (even if delivery is async),
        or None if the notification was deduplicated / preference-blocked.
        """
        # ── 1. Preference check ───────────────────────────────────────────
        prefs = cls._get_preferences(user)
        if not prefs.allows_type(notification_type):
            logger.debug(
                "Notification suppressed by user preference: "
                "user=%s type=%s", user.pk, notification_type
            )
            return None

        # ── 2. Deduplication ──────────────────────────────────────────────
        if dedup_key:
            existing = PushNotification.objects.filter(
                user=user,
                dedup_key=dedup_key,
                delivery_status__in=['queued', 'sent'],
            ).first()
            if existing:
                logger.debug(
                    "Duplicate notification suppressed: dedup_key=%s", dedup_key
                )
                return existing

        # ── 3. Persist before enqueue ─────────────────────────────────────
        expires_at = None
        if expires_in_hours:
            expires_at = timezone.now() + timezone.timedelta(hours=expires_in_hours)

        notification = PushNotification.objects.create(
            user=user,
            title=title,
            message=message,
            notification_type=notification_type,
            priority=priority,
            data=data or {},
            target_url=target_url,
            dedup_key=dedup_key,
            scheduled_for=scheduled_for,
            expires_at=expires_at,
            delivery_status='queued',
        )

        # ── 4. Enqueue delivery ───────────────────────────────────────────
        if scheduled_for and scheduled_for > timezone.now():
            delay = (scheduled_for - timezone.now()).total_seconds()
            from .tasks import deliver_notification
            deliver_notification.apply_async(
                args=[notification.pk],
                countdown=int(delay),
            )
            logger.info(
                "Notification %d scheduled for %s (%.0fs from now)",
                notification.pk, scheduled_for, delay
            )
        else:
            from .tasks import deliver_notification
            deliver_notification.delay(notification.pk)
            logger.info(
                "Notification %d enqueued for immediate delivery", notification.pk
            )

        return notification

    @classmethod
    def send_bulk(
        cls,
        users: List,
        title: str,
        message: str,
        notification_type: str = 'general',
        data: Optional[Dict[str, Any]] = None,
        target_url: Optional[str] = None,
        priority: str = PRIORITY_NORMAL,
        dedup_key_prefix: Optional[str] = None,
    ) -> int:
        """
        Send the same notification to multiple users efficiently.
        Returns the number of notifications actually enqueued.
        """
        count = 0
        for user in users:
            dedup_key = f"{dedup_key_prefix}:{user.pk}" if dedup_key_prefix else None
            result = cls.send(
                user=user,
                title=title,
                message=message,
                notification_type=notification_type,
                data=data,
                target_url=target_url,
                priority=priority,
                dedup_key=dedup_key,
            )
            if result:
                count += 1
        return count

    @classmethod
    def send_to_church(
        cls,
        church,
        title: str,
        message: str,
        notification_type: str = 'general',
        data: Optional[Dict[str, Any]] = None,
        target_url: Optional[str] = None,
        priority: str = PRIORITY_NORMAL,
        dedup_key_prefix: Optional[str] = None,
    ) -> int:
        """
        Send a notification to all members of a church.
        Uses prefetch to avoid N+1 queries.
        """
        from django.contrib.auth import get_user_model
        User = get_user_model()

        users = (
            User.objects
            .filter(church=church, is_active=True)
            .prefetch_related('notification_preferences', 'fcm_tokens')
        )
        return cls.send_bulk(
            users=list(users),
            title=title,
            message=message,
            notification_type=notification_type,
            data=data,
            target_url=target_url,
            priority=priority,
            dedup_key_prefix=dedup_key_prefix,
        )

    # ── FCM delivery (called from the Celery task) ────────────────────────

    @classmethod
    def deliver(cls, notification_pk: int) -> bool:
        """
        Fetch the PushNotification, get the user's FCM tokens, and send
        via Firebase Admin SDK.  Called exclusively from the Celery task.

        Returns True on full or partial success, False if every token failed.
        """
        try:
            notification = (
                PushNotification.objects
                .select_related('user')
                .get(pk=notification_pk)
            )
        except PushNotification.DoesNotExist:
            logger.error("PushNotification %d not found", notification_pk)
            return False

        # Guard: expired
        if notification.is_expired:
            notification.mark_expired()
            logger.info("Notification %d expired — skipped", notification_pk)
            return False

        # Guard: already sent successfully
        if notification.delivery_status == 'sent':
            logger.debug("Notification %d already sent — skipped", notification_pk)
            return True

        tokens = (
            FCMToken.objects
            .filter(user=notification.user, is_active=True)
            .only('id', 'token')
        )

        if not tokens.exists():
            logger.warning(
                "No active FCM tokens for user %s (notification %d)",
                notification.user.pk, notification_pk
            )
            notification.mark_failed(increment_retry=False)
            return False

        success = cls._dispatch_to_fcm(notification, list(tokens))

        if success:
            notification.mark_sent()
            logger.info("Notification %d delivered successfully", notification_pk)
        else:
            notification.mark_failed()
            logger.warning(
                "Notification %d delivery failed (attempt %d/%d)",
                notification_pk, notification.retry_count, notification.max_retries
            )

        return success

    # ── Internal helpers ──────────────────────────────────────────────────

    @classmethod
    def _dispatch_to_fcm(
        cls,
        notification: PushNotification,
        tokens: List[FCMToken],
    ) -> bool:
        """
        Build a Firebase MulticastMessage and send to all tokens at once.
        Deactivates invalid tokens automatically.
        Returns True if at least one token received the message.
        """
        try:
            from firebase_admin import messaging as fb_messaging
        except ImportError:
            logger.error("firebase_admin not installed — cannot deliver notification")
            return False

        token_strings = [t.token for t in tokens]
        token_map     = {t.token: t for t in tokens}

        # Always use data-only payload so the Android app controls display.
        # Include a 'notification' block as well so iOS / background handling
        # works out of the box.
        android_priority = 'high' if notification.priority == PRIORITY_HIGH else 'normal'

        multicast = fb_messaging.MulticastMessage(
            tokens=token_strings,
            # notification block — used by system tray on iOS and when app is killed on Android
            notification=fb_messaging.Notification(
                title=notification.title,
                body=notification.message,
            ),
            # data block — used by our Android service for full client control
            data={
                'type':              notification.notification_type,
                'title':             notification.title,
                'message':           notification.message,
                'notification_id':   str(notification.pk),
                'target_url':        notification.target_url or '',
                **{k: str(v) for k, v in notification.data.items()},
            },
            android=fb_messaging.AndroidConfig(
                priority=android_priority,
                ttl=timezone.timedelta(hours=48),
                notification=fb_messaging.AndroidNotification(
                    icon='ic_notifications',
                    color='#B8935A',
                    channel_id=cls._channel_id_for_type(notification.notification_type),
                    default_sound=True,
                    default_vibrate_timings=True,
                ),
            ),
            apns=fb_messaging.APNSConfig(
                payload=fb_messaging.APNSPayload(
                    aps=fb_messaging.Aps(
                        sound='default',
                        badge=1,
                        content_available=True,
                    )
                )
            ),
        )

        try:
            batch_response = fb_messaging.send_multicast(multicast)
        except Exception as exc:
            logger.exception(
                "Firebase send_multicast raised an exception for notification %d: %s",
                notification.pk, exc
            )
            return False

        success_count = batch_response.success_count
        failure_count = batch_response.failure_count

        logger.info(
            "FCM multicast for notification %d: %d sent, %d failed",
            notification.pk, success_count, failure_count
        )

        # Deactivate any invalid tokens returned in the responses
        if failure_count > 0:
            invalid_tokens: List[int] = []
            for idx, resp in enumerate(batch_response.responses):
                if not resp.success:
                    error_code = getattr(resp.exception, 'code', '') or ''
                    if error_code in INVALID_TOKEN_ERRORS:
                        token_str = token_strings[idx]
                        fcm_obj   = token_map.get(token_str)
                        if fcm_obj:
                            invalid_tokens.append(fcm_obj.pk)
                            logger.info(
                                "Deactivating invalid FCM token (pk=%d) for user %s: %s",
                                fcm_obj.pk, notification.user.pk, error_code
                            )

            if invalid_tokens:
                FCMToken.objects.filter(pk__in=invalid_tokens).update(is_active=False)

        return success_count > 0

    @staticmethod
    def _channel_id_for_type(notification_type: str) -> str:
        """Return the Android notification channel ID matching the type."""
        return {
            'devotional_new':    'ch_devotionals',
            'devotional_shared': 'ch_devotionals',
            'announcement_posted': 'ch_announcements',
            'church_event':      'ch_events',
            'giving_reminder':   'ch_giving',
        }.get(notification_type, 'ch_general')

    @staticmethod
    def _get_preferences(user) -> NotificationPreference:
        """Return (or create with defaults) the user's notification preferences."""
        prefs, _ = NotificationPreference.objects.get_or_create(user=user)
        return prefs
