from django.db.models.signals import post_save
from django.dispatch import receiver
from .models import MobileDevice, MobileNotification
from accounts.models import User
import logging

logger = logging.getLogger('altar_funds')


@receiver(post_save, sender=User)
def user_created_handler(sender, instance, created, **kwargs):
    """Handle user creation for mobile app"""
    if created:
        # Send welcome notification if user has mobile devices
        from .services import MobileNotificationService
        
        try:
            MobileNotificationService.send_push_notification(
                users=[instance.id],
                title='Welcome to AltarFunds',
                message='Your account has been created successfully!',
                notification_type='system'
            )
        except Exception as e:
            logger.error(f"Failed to send welcome notification: {e}")


@receiver(post_save, sender=MobileDevice)
def device_registered_handler(sender, instance, created, **kwargs):
    """Handle mobile device registration"""
    if created:
        logger.info(f"Mobile device registered: {instance.device_token} for {instance.user.email}")
        
        # Track analytics event
        from .services import MobileAnalyticsService
        
        try:
            MobileAnalyticsService.track_event(
                user=instance.user,
                device=instance,
                event_type='device',
                event_name='device_registered',
                event_data={
                    'device_type': instance.device_type,
                    'app_version': instance.app_version,
                    'os_version': instance.os_version
                }
            )
        except Exception as e:
            logger.error(f"Failed to track device registration: {e}")


@receiver(post_save, sender=MobileNotification)
def notification_created_handler(sender, instance, created, **kwargs):
    """Handle mobile notification creation"""
    if created:
        logger.info(f"Mobile notification created: {instance.title} for {instance.user.email}")
        
        # Track analytics event
        from .services import MobileAnalyticsService
        
        try:
            MobileAnalyticsService.track_event(
                user=instance.user,
                device=instance.device,
                event_type='notification',
                event_name='notification_created',
                event_data={
                    'notification_type': instance.notification_type,
                    'title': instance.title
                }
            )
        except Exception as e:
            logger.error(f"Failed to track notification creation: {e}")
