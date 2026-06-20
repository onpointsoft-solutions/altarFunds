"""
Firebase Auth + Realtime Database sync service.

Responsibility:
  - When a user registers on Django, mirror them to Firebase Auth
    (so the mobile app can use Firebase token-based auth directly).
  - Write a structured profile snapshot to Firebase Realtime Database
    for real-time reads on the mobile clients (presence, profile card,
    church directory, etc.).
  - On profile update, keep the RTDB snapshot in sync.
  - On soft-delete / deactivation, disable the Firebase Auth account.

Architecture:
  - All public methods are called from Celery tasks (accounts/tasks.py)
    so the HTTP response is NEVER blocked.
  - Every method is idempotent — safe to retry on failure.
  - firebase_uid is stored on User.firebase_uid for cross-reference.

Firebase Realtime Database structure written by this module:
  /users/{firebase_uid}/
      profile/
          django_id          : int
          email              : str
          first_name         : str
          last_name          : str
          full_name          : str
          phone_number       : str | null
          role               : str
          profile_picture    : str | null
          is_active          : bool
          created_at         : ISO-8601 str
      church/
          id                 : int | null
          name               : str | null
          code               : str | null
          city               : str | null
          type               : str | null
      member/
          membership_number  : str | null
          membership_status  : str
          membership_date    : str | null

  /churches/{church_id}/members/{firebase_uid}/
      name                   : str
      role                   : str
      is_active              : bool

  /presence/{firebase_uid}/
      online                 : false        # client updates this
      last_seen              : null         # client updates this

Usage:
    from common.firebase_sync import FirebaseSyncService
    FirebaseSyncService.sync_user_to_firebase(user)
"""
from __future__ import annotations

import logging
from typing import Any, Dict, Optional

logger = logging.getLogger(__name__)


class FirebaseSyncService:
    """
    Sync Django user data to Firebase Auth + Firebase Realtime Database.
    All methods are safe to call multiple times (idempotent).
    """

    # ── Firebase Admin SDK helpers ────────────────────────────────────────

    @staticmethod
    def _get_firebase_app():
        """Return the initialized Firebase app (lazy, from notifications module)."""
        from notifications.firebase_service import get_firebase_app
        return get_firebase_app()

    @staticmethod
    def _get_auth():
        from firebase_admin import auth
        return auth

    @staticmethod
    def _get_db():
        from firebase_admin import db
        return db

    # ── Public API ────────────────────────────────────────────────────────

    @classmethod
    def sync_user_to_firebase(cls, user) -> Optional[str]:
        """
        Full sync: create/update Firebase Auth account AND write RTDB snapshot.

        Returns the Firebase UID on success, None on failure.
        Safe to call on every save — uses update_user() if UID already exists.
        """
        try:
            firebase_uid = cls._sync_auth(user)
            if firebase_uid:
                cls._sync_rtdb_profile(user, firebase_uid)
                cls._sync_rtdb_church_member(user, firebase_uid)
                cls._init_presence(firebase_uid)
            return firebase_uid
        except Exception as exc:
            logger.exception(
                "FirebaseSyncService.sync_user_to_firebase failed for user %s: %s",
                user.pk, exc
            )
            return None

    @classmethod
    def update_user_profile(cls, user) -> bool:
        """
        Update RTDB profile snapshot after a Django profile change.
        Requires user.firebase_uid to already be set.
        """
        if not user.firebase_uid:
            # Not yet synced — do a full sync instead
            uid = cls.sync_user_to_firebase(user)
            return uid is not None
        try:
            cls._sync_rtdb_profile(user, user.firebase_uid)
            cls._sync_rtdb_church_member(user, user.firebase_uid)
            return True
        except Exception as exc:
            logger.exception(
                "FirebaseSyncService.update_user_profile failed for user %s: %s",
                user.pk, exc
            )
            return False

    @classmethod
    def disable_firebase_user(cls, user) -> bool:
        """
        Disable the Firebase Auth account when a Django user is deactivated.
        This prevents the user from signing in via Firebase even if their
        Django account is suspended.
        """
        if not user.firebase_uid:
            return True  # Nothing to disable
        try:
            auth = cls._get_auth()
            app = cls._get_firebase_app()
            auth.update_user(user.firebase_uid, disabled=True, app=app)
            logger.info(
                "Firebase Auth account disabled for user %s (uid=%s)",
                user.pk, user.firebase_uid
            )
            return True
        except Exception as exc:
            logger.exception(
                "FirebaseSyncService.disable_firebase_user failed for user %s: %s",
                user.pk, exc
            )
            return False

    @classmethod
    def enable_firebase_user(cls, user) -> bool:
        """Re-enable Firebase Auth account when a Django user is unsuspended."""
        if not user.firebase_uid:
            return True
        try:
            auth = cls._get_auth()
            app = cls._get_firebase_app()
            auth.update_user(user.firebase_uid, disabled=False, app=app)
            logger.info(
                "Firebase Auth account enabled for user %s (uid=%s)",
                user.pk, user.firebase_uid
            )
            return True
        except Exception as exc:
            logger.exception(
                "FirebaseSyncService.enable_firebase_user failed for user %s: %s",
                user.pk, exc
            )
            return False

    @classmethod
    def delete_firebase_user(cls, user) -> bool:
        """
        Hard-delete the Firebase Auth account and RTDB data.
        Only call this when permanently deleting a Django user.
        """
        if not user.firebase_uid:
            return True
        try:
            auth = cls._get_auth()
            db   = cls._get_db()
            app  = cls._get_firebase_app()

            # Remove RTDB data first (so no orphaned data if auth delete fails)
            db.reference(f"/users/{user.firebase_uid}", app=app).delete()
            if user.church_id:
                db.reference(
                    f"/churches/{user.church_id}/members/{user.firebase_uid}",
                    app=app
                ).delete()
            db.reference(f"/presence/{user.firebase_uid}", app=app).delete()

            # Delete Firebase Auth account
            auth.delete_user(user.firebase_uid, app=app)

            logger.info(
                "Firebase Auth + RTDB data deleted for user %s (uid=%s)",
                user.pk, user.firebase_uid
            )
            return True
        except Exception as exc:
            logger.exception(
                "FirebaseSyncService.delete_firebase_user failed for user %s: %s",
                user.pk, exc
            )
            return False

    # ── Internal helpers ──────────────────────────────────────────────────

    @classmethod
    def _sync_auth(cls, user) -> Optional[str]:
        """
        Create or update the Firebase Auth account for a Django user.

        Strategy:
          1. If user.firebase_uid is set → update_user() (profile sync).
          2. If not set → try create_user(). If the email already exists in
             Firebase (e.g. the user registered via mobile first), fetch the
             existing UID and adopt it.

        Returns the Firebase UID.
        """
        auth = cls._get_auth()
        app  = cls._get_firebase_app()

        display_name = f"{user.first_name} {user.last_name}".strip() or user.email
        photo_url    = user.profile_picture or None

        if user.firebase_uid:
            # Update existing Firebase Auth account
            try:
                auth.update_user(
                    user.firebase_uid,
                    email=user.email,
                    display_name=display_name,
                    photo_url=photo_url,
                    disabled=not user.is_active,
                    app=app,
                )
                logger.info(
                    "Firebase Auth updated for user %s (uid=%s)",
                    user.pk, user.firebase_uid
                )
                return user.firebase_uid
            except auth.UserNotFoundError:
                # UID was stale — fall through to create a fresh account
                logger.warning(
                    "Stale firebase_uid %s for user %s — re-creating",
                    user.firebase_uid, user.pk
                )

        # Create new Firebase Auth account
        try:
            firebase_user = auth.create_user(
                email=user.email,
                email_verified=user.is_email_verified,
                display_name=display_name,
                photo_url=photo_url,
                disabled=not user.is_active,
                app=app,
            )
            firebase_uid = firebase_user.uid

        except auth.EmailAlreadyExistsError:
            # Email is already registered in Firebase (mobile app registration path).
            # Adopt the existing Firebase account.
            existing = auth.get_user_by_email(user.email, app=app)
            firebase_uid = existing.uid
            logger.info(
                "Adopted existing Firebase Auth account for user %s (uid=%s)",
                user.pk, firebase_uid
            )

        # Persist the UID on the Django model
        user.firebase_uid = firebase_uid
        user.save(update_fields=['firebase_uid'])

        logger.info(
            "Firebase Auth account created for user %s (uid=%s)",
            user.pk, firebase_uid
        )
        return firebase_uid

    @classmethod
    def _sync_rtdb_profile(cls, user, firebase_uid: str) -> None:
        """Write the user profile snapshot to RTDB at /users/{uid}/profile."""
        db  = cls._get_db()
        app = cls._get_firebase_app()

        # Safely access member profile and church
        membership_number = None
        membership_status = 'new_member'
        membership_date   = None
        try:
            mp = user.member_profile
            membership_number = mp.membership_number
            membership_status = mp.membership_status
            membership_date   = mp.membership_date.isoformat() if mp.membership_date else None
        except Exception:
            pass

        church_data = cls._church_snapshot(user)

        profile_data: Dict[str, Any] = {
            'profile': {
                'django_id':       user.pk,
                'email':           user.email,
                'first_name':      user.first_name,
                'last_name':       user.last_name,
                'full_name':       f"{user.first_name} {user.last_name}".strip(),
                'phone_number':    user.phone_number or None,
                'role':            user.role,
                'profile_picture': user.profile_picture or None,
                'is_active':       user.is_active,
                'created_at':      user.date_joined.isoformat() if user.date_joined else None,
            },
            'church': church_data,
            'member': {
                'membership_number': membership_number,
                'membership_status': membership_status,
                'membership_date':   membership_date,
            },
        }

        db.reference(f"/users/{firebase_uid}", app=app).update(profile_data)
        logger.info("RTDB /users/%s updated for Django user %s", firebase_uid, user.pk)

    @classmethod
    def _sync_rtdb_church_member(cls, user, firebase_uid: str) -> None:
        """
        Write a lightweight member entry to /churches/{church_id}/members/{uid}.
        This lets the church directory be read without fetching every /users/ node.
        """
        if not user.church_id:
            return

        db  = cls._get_db()
        app = cls._get_firebase_app()

        db.reference(
            f"/churches/{user.church_id}/members/{firebase_uid}",
            app=app,
        ).set({
            'name':      f"{user.first_name} {user.last_name}".strip(),
            'role':      user.role,
            'is_active': user.is_active,
        })
        logger.info(
            "RTDB /churches/%s/members/%s updated",
            user.church_id, firebase_uid
        )

    @classmethod
    def _init_presence(cls, firebase_uid: str) -> None:
        """
        Create a presence placeholder at /presence/{uid}.
        The mobile client overwrites `online` and `last_seen` at runtime
        using Firebase's onDisconnect() API for real-time presence.
        """
        db  = cls._get_db()
        app = cls._get_firebase_app()

        ref = db.reference(f"/presence/{firebase_uid}", app=app)
        # Only write if not already present — don't overwrite live client data
        if ref.get() is None:
            ref.set({'online': False, 'last_seen': None})
            logger.debug("RTDB /presence/%s initialised", firebase_uid)

    @staticmethod
    def _church_snapshot(user) -> Dict[str, Any]:
        """Build the church sub-object for the RTDB profile node."""
        if not user.church_id:
            return {'id': None, 'name': None, 'code': None, 'city': None, 'type': None}
        try:
            c = user.church
            return {
                'id':   c.pk,
                'name': c.name,
                'code': getattr(c, 'church_code', None),
                'city': getattr(c, 'city', None),
                'type': getattr(c, 'church_type', None),
            }
        except Exception:
            return {'id': user.church_id, 'name': None, 'code': None, 'city': None, 'type': None}
