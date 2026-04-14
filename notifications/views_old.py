from rest_framework import generics, viewsets, status
from rest_framework.decorators import api_view, permission_classes, action
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from django.db.models import Q
from django.utils import timezone
from datetime import timedelta

from .models import FCMToken, DevotionalShare, PushNotification, NotificationPreference
from .serializers import FCMTokenSerializer, DevotionalShareSerializer, PushNotificationSerializer
import logging

logger = logging.getLogger(__name__)

class DevotionalShareListView(generics.ListAPIView):
    """Get devotional shares for a user"""
    permission_classes = [IsAuthenticated]
    
    def get_queryset(self):
        user = self.request.user
        return DevotionalShare.objects.filter(
            Q(user=user) | Q(shared_by=user)
        ).select_related('devotional', 'user', 'shared_by').order_by('-shared_at')
    
    def get_serializer_class(self):
        return DevotionalShareSerializer

@api_view(['POST'])
@permission_classes([IsAuthenticated])
def share_devotional(request):
    """Share a devotional"""
    try:
        devotional_id = request.data.get('devotional_id')
        message = request.data.get('message', '')
        
        # Create share record
        share = DevotionalShare.objects.create(
            user=request.user,
            devotional_id=devotional_id,
            shared_by=request.user,
            message=message
        )
        
        # Create push notification for followers
        # This would integrate with a followers system
        # For now, create a simple notification
        PushNotification.objects.create(
            user=request.user,
            title='Devotional Shared',
            message=f'You shared a devotional: {share.devotional.title}',
            notification_type='devotional_shared',
            data={
                'devotional_id': devotional_id,
                'share_id': share.id,
                'action': 'share'
            }
        )
        
        return Response({
            'success': True,
            'message': 'Devotional shared successfully',
            'share_id': share.id
        }, status=status.HTTP_201_CREATED)
        
    except Exception as e:
        return Response({
            'success': False,
            'error': str(e)
        }, status=status.HTTP_400_BAD_REQUEST)

@api_view(['GET'])
@permission_classes([IsAuthenticated])
def get_notifications(request):
    """Get user notifications"""
    try:
        notifications = PushNotification.objects.filter(
            user=request.user,
            is_read=False
        ).order_by('-created_at')[:20]
        
        # Mark notifications as read
        notifications.update(is_read=True)
        
        serializer = PushNotificationSerializer(notifications, many=True)
        
        return Response({
            'success': True,
            'count': notifications.count(),
            'results': serializer.data
        })
        
    except Exception as e:
        return Response({
            'success': False,
            'error': str(e)
        }, status=status.HTTP_500_INTERNAL_SERVER_ERROR)

@api_view(['POST'])
@permission_classes([IsAuthenticated])
def update_notification_preferences(request):
    """Update user notification preferences"""
    try:
        preferences, created = NotificationPreference.objects.update_or_create(
            user=request.user,
            defaults={
                'push_enabled': True,
                'email_enabled': True,
                'devotional_notifications': True,
                'announcement_notifications': True
            }
        )
        
        # Update specific preferences
        if 'push_enabled' in request.data:
            preferences.push_enabled = request.data['push_enabled']
        if 'email_enabled' in request.data:
            preferences.email_enabled = request.data['email_enabled']
        if 'devotional_notifications' in request.data:
            preferences.devotional_notifications = request.data['devotional_notifications']
        if 'announcement_notifications' in request.data:
            preferences.announcement_notifications = request.data['announcement_notifications']
        
        preferences.save()
        
        return Response({
            'success': True,
            'message': 'Notification preferences updated',
            'preferences': {
                'push_enabled': preferences.push_enabled,
                'email_enabled': preferences.email_enabled,
                'devotional_notifications': preferences.devotional_notifications,
                'announcement_notifications': preferences.announcement_notifications
            }
        })
        
    except Exception as e:
        return Response({
            'success': False,
            'error': str(e)
        }, status=status.HTTP_400_BAD_REQUEST)