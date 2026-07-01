from django.db.models.signals import post_save, pre_save
from django.dispatch import receiver
from django.contrib.auth import get_user_model
from .models import Member, UserSession
import logging

logger = logging.getLogger('altar_funds')
User = get_user_model()


def _get_notification_service():
    """Lazy import so a missing common.services never breaks app startup."""
    try:
        from common.services import NotificationService
        return NotificationService
    except ImportError:
        return None


@receiver(post_save, sender=User)
def user_created(sender, instance, created, **kwargs):
    """Handle user creation"""
    if created:
        # Create member profile if it doesn't exist
        Member.objects.get_or_create(user=instance)
        
        # Log user creation
        logger.info(f"New user created: {instance.email} (ID: {instance.id})")
        
        # Send welcome notification (non-fatal if service unavailable)
        ns = _get_notification_service()
        if ns:
            try:
                ns.send_welcome_notification(instance)
            except Exception as exc:
                logger.warning("Could not send welcome notification: %s", exc)


@receiver(pre_save, sender=User)
def user_pre_save(sender, instance, **kwargs):
    """Handle user pre-save operations"""
    # Normalize email
    if instance.email:
        instance.email = instance.email.lower()


@receiver(post_save, sender=User)
def user_updated(sender, instance, created, **kwargs):
    """Handle user updates"""
    if not created:
        # Check for role changes
        if hasattr(instance, '_original_role') and instance._original_role != instance.role:
            logger.info(f"User role changed: {instance.email} - {instance._original_role} -> {instance.role}")
            
            # Send role change notification (non-fatal)
            ns = _get_notification_service()
            if ns:
                try:
                    ns.send_role_change_notification(
                        instance,
                        instance._original_role,
                        instance.role
                    )
                except Exception as exc:
                    logger.warning("Could not send role change notification: %s", exc)


@receiver(post_save, sender=Member)
def member_created(sender, instance, created, **kwargs):
    """Handle member creation"""
    if created and instance.user.church:
        # Generate membership number
        instance.generate_membership_number()
        
        logger.info(f"Member profile created for: {instance.user.email}")


@receiver(post_save, sender=UserSession)
def session_created(sender, instance, created, **kwargs):
    """Handle session creation"""
    if created:
        logger.info(f"New session created for: {instance.user.email} from {instance.ip_address}")


# Store original role for change detection
@receiver(pre_save, sender=User)
def store_original_role(sender, instance, **kwargs):
    """Store original role before save"""
    if instance.pk:
        try:
            instance._original_role = User.objects.get(pk=instance.pk).role
        except User.DoesNotExist:
            pass
