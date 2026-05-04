"""
Firebase Cloud Messaging service for sending push notifications
"""
import json
import logging
from django.conf import settings
from firebase_admin import credentials, initialize_app, messaging
from notifications.models import PushNotification, FCMToken
from django.contrib.auth import get_user_model

User = get_user_model()
logger = logging.getLogger(__name__)

# Initialize Firebase Admin SDK
try:
    cred = credentials.Certificate(settings.FIREBASE_CREDENTIALS_PATH)
    initialize_app(cred, name=settings.FIREBASE_PROJECT_ID)
    logger.info("Firebase Admin SDK initialized successfully")
except ValueError:
    # App is already initialized
    logger.info("Firebase Admin SDK already initialized")
except Exception as e:
    logger.error(f"Failed to initialize Firebase: {e}")


class FirebaseNotificationService:
    """Service for sending Firebase push notifications"""
    
    @staticmethod
    def send_devotional_notification(devotional, notification_type='devotional_new'):
        """
        Send push notification when a new devotional is created
        """
        try:
            # Get all users from the same church who have push notifications enabled
            target_users = User.objects.filter(
                church=devotional.church,
                push_notification_records__isnull=True  # Users who exist in push notification records
            ).distinct()
            
            if not target_users.exists():
                logger.info("No users to notify for new devotional")
                return
            
            # Create notification title and message
            title = "New Devotional Available"
            message = f"{devotional.title} by {devotional.author.get_full_name()}"
            
            # Prepare notification data
            notification_data = {
                'title': title,
                'body': message,
                'notification_type': notification_type,
                'data': {
                    'devotional_id': devotional.id,
                    'title': devotional.title,
                    'author': devotional.author.get_full_name(),
                    'target_url': f'/devotionals/{devotional.id}/'
                }
            }
            
            # Send to individual users
            success_count = 0
            failure_count = 0
            
            for user in target_users:
                try:
                    # Get user's FCM tokens
                    user_tokens = user.push_notification_records.filter(
                        is_active=True
                    ).values_list('fcm_token', flat=True)
                    
                    if user_tokens:
                        # Create push notification record
                        push_notification = PushNotification.objects.create(
                            user=user,
                            title=title,
                            message=message,
                            notification_type=notification_type,
                            data=notification_data['data'],
                            target_url=f"/devotionals/{devotional.id}/"
                        )
                        
                        # Send to all tokens for this user
                        for token in user_tokens:
                            message = messaging.Message(
                                notification=messaging.Notification(
                                    title=title,
                                    body=message
                                ),
                                data=notification_data['data'],
                                token=token,
                                android=messaging.AndroidConfig(
                                    priority='high',
                                    notification=messaging.AndroidNotification(
                                        click_action='FLUTTER_NOTIFICATION_CLICK',
                                        icon='ic_notification',
                                        color='#B8935A'
                                    )
                                )
                            )
                            
                            # Send the message
                            messaging.send(message)
                            success_count += 1
                            
                        logger.info(f"Sent notification to user {user.email}")
                    
                except Exception as e:
                    logger.error(f"Failed to send notification to user {user.email}: {e}")
                    failure_count += 1
            
            logger.info(f"Notification sending complete: {success_count} success, {failure_count} failures")
            return {
                'success': success_count,
                'failures': failure_count,
                'total': success_count + failure_count
            }
            
        except Exception as e:
            logger.error(f"Error sending devotional notification: {e}")
            return {'success': 0, 'failures': 1, 'error': str(e)}
    
    @staticmethod
    def send_comment_notification(devotional, comment, notification_type='comment_added'):
        """
        Send push notification when a new comment is added
        """
        try:
            # Notify devotional author
            target_users = [devotional.author]
            
            title = "New Comment on Your Devotional"
            message = f"{comment.user.get_full_name()}: {comment.content[:50]}..."
            
            notification_data = {
                'title': title,
                'body': message,
                'notification_type': notification_type,
                'data': {
                    'devotional_id': devotional.id,
                    'title': devotional.title,
                    'comment_id': comment.id,
                    'commenter': comment.user.get_full_name(),
                    'target_url': f'/devotionals/{devotional.id}/'
                }
            }
            
            return FirebaseNotificationService._send_to_users(
                target_users, title, message, notification_type, notification_data['data']
            )
            
        except Exception as e:
            logger.error(f"Error sending comment notification: {e}")
            return {'success': 0, 'failures': 1, 'error': str(e)}
    
    @staticmethod
    def send_reaction_notification(devotional, reaction, notification_type='like_received'):
        """
        Send push notification when someone likes a devotional
        """
        try:
            # Notify devotional author (but not if they liked their own)
            if reaction.user == devotional.author:
                return {'success': 0, 'message': 'Self-like, no notification sent'}
            
            target_users = [devotional.author]
            
            title = "Your Devotional Was Liked"
            message = f"{reaction.user.get_full_name()} liked your devotional"
            
            notification_data = {
                'title': title,
                'body': message,
                'notification_type': notification_type,
                'data': {
                    'devotional_id': devotional.id,
                    'title': devotional.title,
                    'liker': reaction.user.get_full_name(),
                    'target_url': f'/devotionals/{devotional.id}/'
                }
            }
            
            return FirebaseNotificationService._send_to_users(
                target_users, title, message, notification_type, notification_data['data']
            )
            
        except Exception as e:
            logger.error(f"Error sending reaction notification: {e}")
            return {'success': 0, 'failures': 1, 'error': str(e)}
    
    @staticmethod
    def _send_to_users(users, title, message, notification_type, data):
        """
        Helper method to send notification to multiple users
        """
        success_count = 0
        failure_count = 0
        
        for user in users:
            try:
                # Get user's FCM tokens
                user_tokens = user.push_notification_records.filter(
                    is_active=True
                ).values_list('fcm_token', flat=True)
                
                if user_tokens:
                    # Create push notification record
                    push_notification = PushNotification.objects.create(
                        user=user,
                        title=title,
                        message=message,
                        notification_type=notification_type,
                        data=data,
                        target_url=data.get('target_url', '')
                    )
                    
                    # Send to all tokens for this user
                    for token in user_tokens:
                        message_obj = messaging.Message(
                            notification=messaging.Notification(
                                title=title,
                                body=message
                            ),
                            data=data,
                            token=token,
                            android=messaging.AndroidConfig(
                                priority='high',
                                notification=messaging.AndroidNotification(
                                    click_action='FLUTTER_NOTIFICATION_CLICK',
                                    icon='ic_notification',
                                    color='#B8935A'
                                )
                            )
                        )
                        
                        # Send the message
                        messaging.send(message_obj)
                        success_count += 1
                        
                    logger.info(f"Sent notification to user {user.email}")
                
            except Exception as e:
                logger.error(f"Failed to send notification to user {user.email}: {e}")
                failure_count += 1
        
        return {
            'success': success_count,
            'failures': failure_count,
            'total': success_count + failure_count
        }
