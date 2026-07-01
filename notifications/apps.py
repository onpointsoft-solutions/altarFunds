from django.apps import AppConfig
import logging

logger = logging.getLogger(__name__)


class NotificationsConfig(AppConfig):
    name = 'notifications'

    def ready(self):
        """
        Called once when Django starts up.
        Pre-warm the Firebase Admin SDK so the first real notification
        doesn't pay the initialisation cost, and so any misconfiguration
        (missing credentials file, wrong project ID) surfaces immediately
        at startup rather than silently failing on the first send.
        """
        try:
            from .firebase_service import get_firebase_app
            get_firebase_app()
            logger.info("Firebase Admin SDK pre-warmed successfully")
        except Exception as exc:
            # Log as warning — server should still start even if Firebase
            # creds are not configured (e.g. local dev without service-account.json).
            logger.warning(
                "Firebase Admin SDK could not be initialised at startup: %s. "
                "Push notifications will fail until this is resolved.", exc
            )
