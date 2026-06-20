"""
Celery tasks for accounts app.

All Firebase sync operations run here so they never block
the registration / profile-update HTTP response.
"""
from __future__ import annotations

import logging

from celery import shared_task
from django.contrib.auth import get_user_model

logger = logging.getLogger(__name__)
User = get_user_model()


@shared_task(
    bind=True,
    max_retries=5,
    default_retry_delay=30,          # seconds between retries
    autoretry_for=(Exception,),
    retry_backoff=True,              # exponential: 30s, 60s, 120s, 240s, 480s
    retry_backoff_max=600,
    name='accounts.sync_user_to_firebase',
)
def sync_user_to_firebase(self, user_id: int) -> str:
    """
    Create / update the Firebase Auth account and RTDB snapshot for a user.

    Called immediately after successful Django registration and whenever
    profile data changes. Retried automatically on transient failures.
    """
    try:
        user = User.objects.select_related('church', 'member_profile').get(pk=user_id)
    except User.DoesNotExist:
        logger.error("sync_user_to_firebase: User %d not found — aborting", user_id)
        return 'user_not_found'

    from common.firebase_sync import FirebaseSyncService
    firebase_uid = FirebaseSyncService.sync_user_to_firebase(user)

    if firebase_uid:
        logger.info(
            "sync_user_to_firebase completed: Django user %d → Firebase uid %s",
            user_id, firebase_uid
        )
        return firebase_uid
    else:
        raise RuntimeError(
            f"FirebaseSyncService returned None for user {user_id} — will retry"
        )


@shared_task(
    bind=True,
    max_retries=3,
    default_retry_delay=15,
    autoretry_for=(Exception,),
    retry_backoff=True,
    name='accounts.update_firebase_profile',
)
def update_firebase_profile(self, user_id: int) -> bool:
    """
    Push updated profile data to Firebase RTDB after a profile edit.
    Only updates RTDB — does NOT create a new Firebase Auth account.
    """
    try:
        user = User.objects.select_related('church', 'member_profile').get(pk=user_id)
    except User.DoesNotExist:
        logger.error("update_firebase_profile: User %d not found", user_id)
        return False

    from common.firebase_sync import FirebaseSyncService
    ok = FirebaseSyncService.update_user_profile(user)

    if not ok:
        raise RuntimeError(
            f"update_user_profile failed for user {user_id} — will retry"
        )
    return True


@shared_task(
    bind=True,
    max_retries=3,
    default_retry_delay=10,
    autoretry_for=(Exception,),
    name='accounts.disable_firebase_user',
)
def disable_firebase_user(self, user_id: int) -> bool:
    """Disable the Firebase Auth account when a Django user is suspended."""
    try:
        user = User.objects.get(pk=user_id)
    except User.DoesNotExist:
        return False

    from common.firebase_sync import FirebaseSyncService
    ok = FirebaseSyncService.disable_firebase_user(user)
    if not ok:
        raise RuntimeError(f"disable_firebase_user failed for user {user_id}")
    return True


@shared_task(
    bind=True,
    max_retries=3,
    default_retry_delay=10,
    autoretry_for=(Exception,),
    name='accounts.enable_firebase_user',
)
def enable_firebase_user(self, user_id: int) -> bool:
    """Re-enable the Firebase Auth account when a Django user is unsuspended."""
    try:
        user = User.objects.get(pk=user_id)
    except User.DoesNotExist:
        return False

    from common.firebase_sync import FirebaseSyncService
    ok = FirebaseSyncService.enable_firebase_user(user)
    if not ok:
        raise RuntimeError(f"enable_firebase_user failed for user {user_id}")
    return True
