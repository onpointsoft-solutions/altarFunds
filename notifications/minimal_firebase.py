"""
Minimal Firebase Cloud Messaging setup for push notifications
Alternative to full firebase-admin SDK
"""
import requests
import json
import logging
from django.conf import settings

logger = logging.getLogger(__name__)


class MinimalFirebaseService:
    """
    Lightweight Firebase service using HTTP API instead of full SDK
    """
    
    @staticmethod
    def get_fcm_server_key():
        """Get FCM server key from settings or environment"""
        return getattr(settings, 'FCM_SERVER_KEY', None)
    
    @staticmethod
    def send_notification(token, title, message, data=None):
        """
        Send notification using FCM HTTP API
        """
        server_key = MinimalFirebaseService.get_fcm_server_key()
        if not server_key:
            logger.error("FCM_SERVER_KEY not configured")
            return False
        
        url = "https://fcm.googleapis.com/fcm/send"
        headers = {
            'Authorization': f'key={server_key}',
            'Content-Type': 'application/json'
        }
        
        payload = {
            'to': token,
            'notification': {
                'title': title,
                'body': message,
                'sound': 'default'
            }
        }
        
        if data:
            payload['data'] = data
        
        try:
            response = requests.post(url, json=payload, headers=headers, timeout=10)
            if response.status_code == 200:
                logger.info(f"Notification sent successfully to {token}")
                return True
            else:
                logger.error(f"FCM Error: {response.status_code} - {response.text}")
                return False
        except Exception as e:
            logger.error(f"Failed to send notification: {e}")
            return False
    
    @staticmethod
    def send_devotional_notification(devotional, notification_type='devotional_new'):
        """
        Send devotional notification using minimal setup
        """
        try:
            # Get FCM tokens from database
            from notifications.models import FCMToken
            tokens = FCMToken.objects.filter(
                is_active=True,
                user__church=devotional.church
            ).values_list('token', flat=True)
            
            title = "New Devotional Available"
            message = f"{devotional.title} by {devotional.author.get_full_name()}"
            
            data = {
                'devotional_id': devotional.id,
                'title': devotional.title,
                'author': devotional.author.get_full_name(),
                'target_url': f'/devotionals/{devotional.id}/'
            }
            
            success_count = 0
            for token in tokens:
                if MinimalFirebaseService.send_notification(token, title, message, data):
                    success_count += 1
            
            logger.info(f"Sent {success_count} notifications for new devotional")
            return {'success': success_count, 'total': len(tokens)}
            
        except Exception as e:
            logger.error(f"Error sending devotional notification: {e}")
            return {'success': 0, 'error': str(e)}
