"""
Celery tasks for notification delivery.

Design:
  - deliver_notification:   Sends a single PushNotification (by PK) via Firebase.
                            Used by NotificationService.send() for immediate delivery.
  - retry_failed_notifications: Periodic task that picks up failed/stuck notifications
                            and re-queues them if they haven't exceeded max_retries.
  - send_church_notification: Fan-out task for broadcast sends — creates one
                            deliver_notification task per user to avoid one long
                            blocking task holding a worker.

All tasks use autoretry_for + exponential backoff so transient Firebase
outages are handled automatically without manual intervention.
"""
from __future__ import annotations

import logging

from celery import shared_task
from django.utils import timezone

logger = logging.getLogger(__name__)


@shared_task(
    bind=True,
    autoretry_for=(Exception,),
    retry_backoff=True,           # exponential: 1s, 2s, 4s, …
    retry_backoff_max=300,        # cap at 5 minutes
    max_retries=3,
    default_retry_delay=60,
    name='notifications.deliver_notification',
)
def deliver_notification(self, notification_pk: int) -> dict:
    """
    Deliver a single PushNotification to all active FCM tokens for its user.

    Called by NotificationService.send() after the DB record is created.
    Returns a result dict so the Celery result backend can log it.
    """
    from .notification_service import NotificationService

    logger.info(
        "Task deliver_notification: pk=%d attempt=%d",
        notification_pk, self.request.retries + 1
    )

    try:
        success = NotificationService.deliver(notification_pk)
        if success:
            logger.info("Notification %d delivered successfully", notification_pk)
            return {'status': 'delivered', 'pk': notification_pk}
        else:
            logger.warning(
                "Notification %d delivery failed (all tokens failed or no tokens)",
                notification_pk
            )
            return {'status': 'failed', 'pk': notification_pk}

    except Exception as exc:
        logger.exception(
            "Unhandled error delivering notification %d: %s", notification_pk, exc
        )
        # Let Celery autoretry handle this
        raise


@shared_task(name='notifications.retry_failed_notifications')
def retry_failed_notifications() -> dict:
    """
    Periodic cleanup task — re-queues failed notifications that still have
    retry budget remaining and haven't expired yet.

    Schedule this in CELERY_BEAT_SCHEDULE to run every 30 minutes:

        'retry-failed-notifications': {
            'task': 'notifications.retry_failed_notifications',
            'schedule': 1800,   # 30 minutes
        },
    """
    from .models import PushNotification

    now = timezone.now()
    candidates = PushNotification.objects.filter(
        delivery_status='failed',
        retry_count__lt=3,          # still has retries left
    ).exclude(
        expires_at__lte=now,        # not yet expired
    ).values_list('pk', flat=True)[:50]  # cap to avoid overloading the queue

    count = 0
    for pk in candidates:
        deliver_notification.delay(pk)
        count += 1

    logger.info("retry_failed_notifications: re-queued %d notifications", count)
    return {'requeued': count}


@shared_task(name='notifications.send_church_notification')
def send_church_notification(
    church_pk: int,
    title: str,
    message: str,
    notification_type: str = 'general',
    data: dict | None = None,
    target_url: str | None = None,
    priority: str = 'normal',
    exclude_user_pk: int | None = None,
) -> dict:
    """
    Fan-out task: sends a notification to all members of a church.

    Instead of one big multicast that blocks a worker for minutes on large
    churches, this creates one deliver_notification task per user so the
    load is spread across workers and individual failures don't affect others.
    """
    from django.contrib.auth import get_user_model
    from .notification_service import NotificationService

    User = get_user_model()

    users = (
        User.objects
        .filter(church_id=church_pk, is_active=True)
        .select_related('notification_preferences')
    )
    if exclude_user_pk:
        users = users.exclude(pk=exclude_user_pk)

    count = 0
    for user in users:
        notif = NotificationService.send(
            user=user,
            title=title,
            message=message,
            notification_type=notification_type,
            data=data,
            target_url=target_url,
            priority=priority,
        )
        if notif:
            count += 1

    logger.info(
        "send_church_notification: church=%d sent=%d type=%s",
        church_pk, count, notification_type
    )
    return {'church_pk': church_pk, 'sent': count}
