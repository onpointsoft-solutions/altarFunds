"""
DEPRECATED — MinimalFirebaseService is retired.

The legacy FCM HTTP v1 API (fcm.googleapis.com/fcm/send) was shut down
by Google in June 2024 and returns HTTP 404 for all requests.

Use FirebaseNotificationService from notifications.firebase_service instead:

    from notifications.firebase_service import FirebaseNotificationService

    FirebaseNotificationService.send_to_user(user, title, body, ...)
    FirebaseNotificationService.notify_new_devotional(devotional)
    FirebaseNotificationService.notify_announcement(announcement)
"""
import warnings


class MinimalFirebaseService:
    """Stub that raises a descriptive error so misuse is immediately visible."""

    def __init__(self):
        warnings.warn(
            "MinimalFirebaseService is deprecated and non-functional. "
            "The FCM legacy HTTP API was shut down in June 2024. "
            "Use FirebaseNotificationService from notifications.firebase_service.",
            DeprecationWarning,
            stacklevel=2,
        )

    def __getattr__(self, name):
        raise NotImplementedError(
            f"MinimalFirebaseService.{name}() is no longer available. "
            "Use FirebaseNotificationService from notifications.firebase_service."
        )
