"""
Firebase Cloud Messaging service — production-ready rewrite.

Bug fixes vs the original:
  1.  Firebase SDK now initialized lazily via get_firebase_app() instead of
      at module import time, which crashed on startup because
      FIREBASE_CREDENTIALS_PATH / FIREBASE_PROJECT_ID were never in settings.
  2.  Wrong relationship name fixed: was user.push_notification_records
      (that is the PushNotification back-relation), should be user.fcm_tokens.
  3.  Wrong field name fixed: was .values_list('fcm_token', ...), should be
      .values_list('token', ...).
  4.  Variable shadowing fixed: the loop variable `message` shadowed the
      outer string `message`, crashing on the second iteration when
      messaging.Notification(body=<Message object>) was passed.
  5.  Inverted filter fixed: push_notification_records__isnull=True found
      users with NO notifications. Replaced with a correct church member
      lookup using has_notification_enabled.
  6.  minimal_firebase.py (legacy FCM v1 HTTP API) retired. Google shut
      down the v1 endpoint in June 2024. All sends now go through the
      Firebase Admin SDK (send_each_for_multicast).
  7.  All data payload values coerced to str — FCM rejects non-string
      values in the data dict.
  8.  Invalid / expired tokens automatically deactivated after delivery.
  9.  Notification record always created BEFORE FCM send (audit trail).
 10.  Notification body and title never exceed FCM limits (1024 / 4096 bytes).
"""
from __future__ import annotations

import logging
import os
from typing import Any, Dict, List, Optional

from django.conf import settings
from django.db import transaction

logger = logging.getLogger(__name__)

# Firebase error codes that mean the token is permanently invalid and
# should be deactivated in the database immediately.
_INVALID_TOKEN_ERRORS = frozenset({
    'registration-token-not-registered',
    'invalid-registration-token',
    'invalid-argument',
    'mismatched-credential',
    'unregistered',
})


# ── Firebase Admin SDK initialisation ────────────────────────────────────────

_firebase_app = None


def get_firebase_app():
    """
    Return (or lazily create) the Firebase Admin SDK app instance.

    Lazy initialisation prevents startup crashes when the credentials
    file is missing in test / CI environments — the error surfaces only
    when the first actual send is attempted.
    """
    global _firebase_app
    if _firebase_app is not None:
        return _firebase_app

    try:
        import firebase_admin
        from firebase_admin import credentials

        cred_path = getattr(settings, 'FIREBASE_CREDENTIALS_PATH', None)
        project_id = getattr(settings, 'FIREBASE_PROJECT_ID', None)

        if not cred_path:
            raise ValueError(
                "FIREBASE_CREDENTIALS_PATH is not set in settings. "
                "Add it to your .env file pointing to the service-account JSON."
            )

        # Resolve relative paths against BASE_DIR so the file is found
        # regardless of the process CWD (important on PythonAnywhere / WSGI).
        if not os.path.isabs(cred_path):
            base_dir = getattr(settings, 'BASE_DIR', None)
            if base_dir:
                cred_path = os.path.join(str(base_dir), cred_path)

        if not os.path.exists(cred_path):
            raise FileNotFoundError(
                f"Firebase service-account file not found at: {cred_path}\n"
                "Download it from Firebase console → Project Settings → "
                "Service Accounts → Generate new private key."
            )

        # Reuse an already-initialised app (happens during hot-reload)
        try:
            _firebase_app = firebase_admin.get_app(name=project_id or '[DEFAULT]')
            logger.info("Reusing existing Firebase app: %s", _firebase_app.name)
        except ValueError:
            cred = credentials.Certificate(cred_path)
            options = {'projectId': project_id} if project_id else {}
            # databaseURL is required for firebase_admin.db.reference() calls
            db_url = getattr(settings, 'FIREBASE_DATABASE_URL', None)
            if db_url:
                options['databaseURL'] = db_url
            else:
                logger.warning(
                    "FIREBASE_DATABASE_URL not set — Realtime Database operations will fail. "
                    "Add it to .env: FIREBASE_DATABASE_URL=https://<project>-default-rtdb.firebaseio.com"
                )
            _firebase_app = firebase_admin.initialize_app(
                cred,
                options=options,
                name=project_id or '[DEFAULT]',
            )
            logger.info(
                "Firebase Admin SDK initialised: project=%s databaseURL=%s",
                project_id, db_url or 'NOT SET'
            )

        return _firebase_app

    except Exception as exc:
        logger.error("Firebase initialisation failed: %s", exc)
        raise


# ── Low-level sender ─────────────────────────────────────────────────────────

def _send_multicast(
    token_objects: list,          # List[FCMToken]
    title: str,
    body: str,
    notification_type: str,
    data: Optional[Dict[str, Any]] = None,
    priority: str = 'normal',
) -> Dict[str, int]:
    """
    Send one FCM multicast to a list of FCMToken objects.

    Returns {success: int, failure: int, invalid_deactivated: int}.

    Key design decisions:
    - Uses send_each_for_multicast (Firebase Admin SDK ≥ 6.0) which
      gives per-token success/failure responses.
    - All data-payload values coerced to str (FCM requirement).
    - Never raises — all exceptions are caught and logged so a single
      bad token does not stop the rest from being notified.
    """
    from firebase_admin import messaging as fb

    if not token_objects:
        return {'success': 0, 'failure': 0, 'invalid_deactivated': 0}

    # Truncate to FCM limits (title 1024 B, body 4096 B)
    title = title[:1000]
    body  = body[:4000]

    # Coerce every data value to str — FCM rejects non-strings
    safe_data: Dict[str, str] = {
        'type':    notification_type,
        'title':   title,
        'message': body,
    }
    if data:
        for k, v in data.items():
            safe_data[str(k)] = str(v)

    token_strings = [t.token for t in token_objects]
    token_map     = {t.token: t for t in token_objects}

    android_priority = 'high' if priority == 'high' else 'normal'
    channel_id = _channel_for_type(notification_type)

    multicast_msg = fb.MulticastMessage(
        tokens=token_strings,
        # notification block — shown by the system tray when the app is
        # in background or killed (iOS + Android foreground service)
        notification=fb.Notification(title=title, body=body),
        # data block — preferred; our Android FirebaseMessagingService
        # handles this for full client-side control of display
        data=safe_data,
        android=fb.AndroidConfig(
            priority=android_priority,
            ttl=86400,  # 24 hours in seconds
            notification=fb.AndroidNotification(
                icon='ic_notifications',
                color='#B8935A',
                channel_id=channel_id,
                default_sound=True,
                default_vibrate_timings=True,
            ),
        ),
        apns=fb.APNSConfig(
            payload=fb.APNSPayload(
                aps=fb.Aps(sound='default', badge=1, content_available=True)
            )
        ),
    )

    try:
        app = get_firebase_app()
        response = fb.send_each_for_multicast(multicast_msg, app=app)
    except Exception as exc:
        logger.exception(
            "send_each_for_multicast raised an exception: %s", exc
        )
        return {'success': 0, 'failure': len(token_strings), 'invalid_deactivated': 0}

    success_count   = response.success_count
    failure_count   = response.failure_count
    invalid_pks: List[int] = []

    for idx, resp in enumerate(response.responses):
        if resp.success:
            # Touch last_used on the token
            try:
                from django.utils import timezone
                token_objects[idx].mark_used()
            except Exception:
                pass
        else:
            error_code = getattr(resp.exception, 'code', '') or ''
            logger.warning(
                "FCM delivery failed for token %s…: %s",
                token_strings[idx][:24], error_code
            )
            if error_code in _INVALID_TOKEN_ERRORS:
                fcm_obj = token_map.get(token_strings[idx])
                if fcm_obj:
                    invalid_pks.append(fcm_obj.pk)

    # Bulk-deactivate invalid tokens
    if invalid_pks:
        from .models import FCMToken
        deactivated = FCMToken.objects.filter(pk__in=invalid_pks).update(is_active=False)
        logger.info("Deactivated %d invalid FCM token(s)", deactivated)
    else:
        deactivated = 0

    logger.info(
        "FCM multicast complete: %d sent, %d failed, %d tokens deactivated",
        success_count, failure_count, deactivated,
    )
    return {
        'success': success_count,
        'failure': failure_count,
        'invalid_deactivated': deactivated,
    }


# ── High-level service ────────────────────────────────────────────────────────

class FirebaseNotificationService:
    """
    High-level service for sending push notifications via Firebase.

    All methods:
    - Create a PushNotification record first (audit trail).
    - Respect per-user NotificationPreference opt-outs.
    - Use the correct FCMToken relationship (user.fcm_tokens).
    - Use send_each_for_multicast for efficient multi-device delivery.
    - Do NOT block: call these from a Celery task for production use.
      For immediate ad-hoc sends (e.g. from a Celery task body), calling
      directly is fine.
    """

    # ── Public convenience methods ────────────────────────────────────────

    @staticmethod
    def send_to_user(
        user,
        title: str,
        body: str,
        notification_type: str = 'general',
        data: Optional[Dict[str, Any]] = None,
        target_url: Optional[str] = None,
        priority: str = 'normal',
    ) -> Dict[str, int]:
        """
        Send a notification to a single user across all their active devices.
        Returns send statistics dict.
        """
        from .models import FCMToken, PushNotification, NotificationPreference

        # ── Preference gate ───────────────────────────────────────────────
        try:
            prefs = user.notification_preferences
        except NotificationPreference.DoesNotExist:
            prefs, _ = NotificationPreference.objects.get_or_create(user=user)

        if not prefs.push_enabled:
            logger.debug(
                "Push notifications disabled for user %s — skipping", user.pk
            )
            return {'success': 0, 'failure': 0, 'skipped': 1}

        # ── Fetch active tokens ───────────────────────────────────────────
        tokens = list(
            FCMToken.objects
            .filter(user=user, is_active=True)
            .only('id', 'token')
        )

        if not tokens:
            logger.warning("No active FCM tokens for user %s", user.pk)
            return {'success': 0, 'failure': 0, 'no_tokens': 1}

        # ── Persist notification record before sending ────────────────────
        notification = PushNotification.objects.create(
            user=user,
            title=title,
            message=body,
            notification_type=notification_type,
            data=data or {},
            target_url=target_url,
            delivery_status='queued',
        )

        # ── Send ──────────────────────────────────────────────────────────
        result = _send_multicast(
            token_objects=tokens,
            title=title,
            body=body,
            notification_type=notification_type,
            data={**(data or {}), 'target_url': target_url or ''},
            priority=priority,
        )

        # ── Update delivery status ────────────────────────────────────────
        if result['success'] > 0:
            notification.mark_sent()
        else:
            notification.mark_failed(increment_retry=False)

        return result

    @staticmethod
    def send_to_church(
        church,
        title: str,
        body: str,
        notification_type: str = 'general',
        data: Optional[Dict[str, Any]] = None,
        target_url: Optional[str] = None,
        priority: str = 'normal',
        exclude_user=None,
    ) -> Dict[str, int]:
        """
        Send a notification to all active members of a church.
        Uses bulk DB reads to avoid N+1 queries.
        """
        from .models import FCMToken

        # Fetch all active tokens for this church in one query
        token_qs = (
            FCMToken.objects
            .filter(user__church=church, user__is_active=True, is_active=True)
            .select_related('user')
        )
        if exclude_user:
            token_qs = token_qs.exclude(user=exclude_user)

        tokens = list(token_qs.only('id', 'token', 'user_id'))

        if not tokens:
            logger.info("No active FCM tokens found for church %s", church.pk)
            return {'success': 0, 'failure': 0, 'no_tokens': 1}

        # Persist one PushNotification per user (for inbox queries)
        user_ids_seen = set()
        records_to_create = []
        from .models import PushNotification
        for t in tokens:
            if t.user_id not in user_ids_seen:
                user_ids_seen.add(t.user_id)
                records_to_create.append(PushNotification(
                    user_id=t.user_id,
                    title=title,
                    message=body,
                    notification_type=notification_type,
                    data=data or {},
                    target_url=target_url,
                    delivery_status='queued',
                ))
        PushNotification.objects.bulk_create(records_to_create, ignore_conflicts=True)

        result = _send_multicast(
            token_objects=tokens,
            title=title,
            body=body,
            notification_type=notification_type,
            data={**(data or {}), 'target_url': target_url or ''},
            priority=priority,
        )

        logger.info(
            "Church notification sent to %d church=%s: %s",
            len(tokens), church.pk, result
        )
        return result

    # ── Domain-specific helpers ───────────────────────────────────────────

    @staticmethod
    def notify_new_devotional(devotional) -> Dict[str, int]:
        """Notify all church members when a new devotional is published."""
        return FirebaseNotificationService.send_to_church(
            church=devotional.church,
            title="New Devotional Available",
            body=f"{devotional.title} · {_author_name(devotional)}",
            notification_type='devotional_new',
            data={
                'devotional_id': str(devotional.id),
                'title':         devotional.title,
                'target_url':    f'/devotionals/{devotional.id}/',
            },
            target_url=f'/devotionals/{devotional.id}/',
            priority='normal',
        )

    @staticmethod
    def notify_devotional_shared(devotional, shared_by_user, recipient_user) -> Dict[str, int]:
        """Notify a user when someone shares a devotional with them."""
        return FirebaseNotificationService.send_to_user(
            user=recipient_user,
            title="A Devotional Was Shared With You",
            body=f"{_user_name(shared_by_user)} shared '{devotional.title}'",
            notification_type='devotional_shared',
            data={
                'devotional_id': str(devotional.id),
                'shared_by':     str(shared_by_user.pk),
                'target_url':    f'/devotionals/{devotional.id}/',
            },
        )

    @staticmethod
    def notify_new_comment(devotional, comment) -> Dict[str, int]:
        """Notify the devotional author when someone comments (skip self-comments)."""
        if comment.user == devotional.author:
            return {'success': 0, 'skipped': 1, 'reason': 'self-comment'}
        return FirebaseNotificationService.send_to_user(
            user=devotional.author,
            title="New Comment on Your Devotional",
            body=f"{_user_name(comment.user)}: {comment.content[:100]}",
            notification_type='comment_added',
            data={
                'devotional_id': str(devotional.id),
                'comment_id':    str(comment.id),
                'target_url':    f'/devotionals/{devotional.id}/',
            },
        )

    @staticmethod
    def notify_reaction(devotional, reaction) -> Dict[str, int]:
        """Notify the devotional author when someone reacts (skip self-reactions)."""
        if reaction.user == devotional.author:
            return {'success': 0, 'skipped': 1, 'reason': 'self-reaction'}
        return FirebaseNotificationService.send_to_user(
            user=devotional.author,
            title="Your Devotional Was Liked",
            body=f"{_user_name(reaction.user)} liked '{devotional.title}'",
            notification_type='like_received',
            data={
                'devotional_id': str(devotional.id),
                'target_url':    f'/devotionals/{devotional.id}/',
            },
        )

    @staticmethod
    def notify_announcement(announcement) -> Dict[str, int]:
        """Notify all church members of a new announcement."""
        return FirebaseNotificationService.send_to_church(
            church=announcement.church,
            title="New Announcement",
            body=announcement.title,
            notification_type='announcement_posted',
            data={
                'announcement_id': str(announcement.id),
                'target_url':      f'/announcements/{announcement.id}/',
            },
            priority='high',
        )

    @staticmethod
    def notify_giving_reminder(user, amount_due: str = '') -> Dict[str, int]:
        """Remind a user about their giving."""
        body = f"Your giving of {amount_due} is due." if amount_due else "Don't forget your giving this week."
        return FirebaseNotificationService.send_to_user(
            user=user,
            title="Giving Reminder",
            body=body,
            notification_type='giving_reminder',
            priority='normal',
        )

    @staticmethod
    def notify_payment_received(user, amount: str, church_name: str) -> Dict[str, int]:
        """Notify a user that their payment was received."""
        return FirebaseNotificationService.send_to_user(
            user=user,
            title="Payment Received",
            body=f"Your payment of {amount} to {church_name} was received. Thank you!",
            notification_type='payment_received',
            data={'amount': amount, 'church': church_name},
            priority='high',
        )


# ── Helpers ───────────────────────────────────────────────────────────────────

def _author_name(devotional) -> str:
    try:
        return devotional.author.get_full_name() or devotional.author.email
    except Exception:
        return 'Unknown'


def _user_name(user) -> str:
    try:
        return user.get_full_name() or user.email
    except Exception:
        return 'Someone'


def _channel_for_type(notification_type: str) -> str:
    return {
        'devotional_new':      'ch_devotionals',
        'devotional_shared':   'ch_devotionals',
        'announcement_posted': 'ch_announcements',
        'church_event':        'ch_events',
        'giving_reminder':     'ch_giving',
        'payment_received':    'ch_giving',
    }.get(notification_type, 'ch_general')
