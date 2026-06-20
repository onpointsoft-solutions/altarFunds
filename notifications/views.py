"""
Notification API views — production-ready fixes:

  1. get_notifications: was calling .count() AFTER .update(is_read=True),
     which always returned 0 because the queryset was already evaluated.
     Fixed by capturing count before marking read, and adding pagination.

  2. FCMToken registration: upsert instead of create — if the same token
     arrives twice (retry / app reinstall) we activate it rather than
     creating a duplicate that hits the unique constraint.

  3. share_devotional: now calls FirebaseNotificationService to actually
     send the push notification instead of only creating a DB record.

  4. update_notification_preferences: was overwriting all fields with True
     on update_or_create defaults, then applying the request data.
     Fixed to only touch fields explicitly present in the request.

  5. Consistent error response shape: {'success': False, 'message': '...'}.
"""
from rest_framework import generics, viewsets, status
from rest_framework.decorators import api_view, permission_classes, action
from rest_framework.pagination import PageNumberPagination
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from django.db.models import Q
from django.utils import timezone

from .models import FCMToken, DevotionalShare, PushNotification, NotificationPreference
from .serializers import (
    FCMTokenSerializer, DevotionalShareSerializer,
    PushNotificationSerializer, NotificationPreferenceSerializer,
)
import logging

logger = logging.getLogger(__name__)


# ── Pagination ────────────────────────────────────────────────────────────────

class NotificationPagination(PageNumberPagination):
    page_size            = 20
    page_size_query_param = 'page_size'
    max_page_size        = 100


# ── Devotional shares ─────────────────────────────────────────────────────────

class DevotionalShareListView(generics.ListAPIView):
    """List devotional shares for the authenticated user."""
    permission_classes = [IsAuthenticated]
    serializer_class   = DevotionalShareSerializer
    pagination_class   = NotificationPagination

    def get_queryset(self):
        user = self.request.user
        return (
            DevotionalShare.objects
            .filter(Q(user=user) | Q(shared_by=user))
            .select_related('devotional', 'user', 'shared_by')
            .order_by('-shared_at')
        )


@api_view(['POST'])
@permission_classes([IsAuthenticated])
def share_devotional(request):
    """
    Share a devotional with the requesting user's church members.
    Also dispatches a FCM push notification to the recipient(s).
    """
    devotional_id = request.data.get('devotional_id')
    share_message = request.data.get('message', '')

    if not devotional_id:
        return Response(
            {'success': False, 'message': 'devotional_id is required'},
            status=status.HTTP_400_BAD_REQUEST,
        )

    try:
        from devotionals.models import Devotional
        devotional = Devotional.objects.get(pk=devotional_id)
    except Exception:
        return Response(
            {'success': False, 'message': 'Devotional not found'},
            status=status.HTTP_404_NOT_FOUND,
        )

    share = DevotionalShare.objects.create(
        user=request.user,
        devotional=devotional,
        shared_by=request.user,
        message=share_message,
    )

    # Persist DB record for the sharer's own inbox
    PushNotification.objects.create(
        user=request.user,
        title='You Shared a Devotional',
        message=f'You shared: {devotional.title}',
        notification_type='devotional_shared',
        data={
            'devotional_id': str(devotional_id),
            'share_id':      str(share.id),
            'action':        'share',
        },
    )

    # Fire FCM notification to church members (async-safe; call from Celery in production)
    try:
        from .firebase_service import FirebaseNotificationService
        FirebaseNotificationService.send_to_church(
            church=devotional.church,
            title='A Devotional Was Shared',
            body=f'{request.user.get_full_name() or request.user.email} shared "{devotional.title}"',
            notification_type='devotional_shared',
            data={
                'devotional_id': str(devotional.id),
                'share_id':      str(share.id),
                'target_url':    f'/devotionals/{devotional.id}/',
            },
            exclude_user=request.user,
        )
    except Exception as exc:
        # Non-fatal — share record is already saved
        logger.error("FCM share notification failed: %s", exc)

    return Response({
        'success':  True,
        'message':  'Devotional shared successfully',
        'share_id': share.id,
    }, status=status.HTTP_201_CREATED)


# ── Notification inbox ────────────────────────────────────────────────────────

@api_view(['GET'])
@permission_classes([IsAuthenticated])
def get_notifications(request):
    """
    Return the user's unread notifications and mark them as read.

    BUG FIX: original code called .count() AFTER .update(is_read=True),
    which returned 0 because the queryset was already evaluated and
    .count() on an updated queryset reflects the post-update state.
    Fixed by evaluating the queryset, capturing the count, then marking read.
    """
    try:
        qs = (
            PushNotification.objects
            .filter(user=request.user, is_read=False)
            .order_by('-created_at')[:20]
        )

        # Evaluate the queryset BEFORE marking read
        notifications = list(qs)
        count = len(notifications)

        # Mark as read in bulk
        ids = [n.pk for n in notifications]
        if ids:
            PushNotification.objects.filter(pk__in=ids).update(is_read=True)

        serializer = PushNotificationSerializer(notifications, many=True)

        return Response({
            'success': True,
            'count':   count,
            'results': serializer.data,
        })

    except Exception as exc:
        logger.exception("get_notifications error: %s", exc)
        return Response(
            {'success': False, 'message': 'Failed to retrieve notifications'},
            status=status.HTTP_500_INTERNAL_SERVER_ERROR,
        )


@api_view(['GET'])
@permission_classes([IsAuthenticated])
def get_all_notifications(request):
    """Paginated list of all notifications (read + unread) for the user."""
    paginator = NotificationPagination()
    qs = (
        PushNotification.objects
        .filter(user=request.user)
        .order_by('-created_at')
    )
    page = paginator.paginate_queryset(qs, request)
    serializer = PushNotificationSerializer(page, many=True)
    return paginator.get_paginated_response(serializer.data)


@api_view(['POST'])
@permission_classes([IsAuthenticated])
def mark_all_read(request):
    """Mark all unread notifications as read for the requesting user."""
    updated = PushNotification.objects.filter(
        user=request.user, is_read=False
    ).update(is_read=True)
    return Response({'success': True, 'marked_read': updated})


# ── Notification preferences ──────────────────────────────────────────────────

@api_view(['GET', 'POST'])
@permission_classes([IsAuthenticated])
def notification_preferences(request):
    """
    GET  — return current preferences.
    POST — update only the fields present in the request body.

    BUG FIX: original update_or_create used 'defaults' that reset all
    preferences to True on every request, then applied the patch on top.
    This meant a POST with only {"push_enabled": false} would first
    set all fields to True and then set push_enabled to False —
    silently undoing all previous user customisations.
    """
    prefs, _ = NotificationPreference.objects.get_or_create(user=request.user)

    if request.method == 'GET':
        return Response({
            'success':     True,
            'preferences': NotificationPreferenceSerializer(prefs).data,
        })

    # PATCH-style update — only touch keys that were sent
    allowed = {
        'push_enabled', 'email_enabled',
        'devotional_notifications', 'announcement_notifications',
        'giving_notifications', 'event_notifications',
    }
    updated_fields = []
    for field in allowed:
        if field in request.data:
            value = request.data[field]
            if not isinstance(value, bool):
                return Response(
                    {'success': False, 'message': f'{field} must be a boolean'},
                    status=status.HTTP_400_BAD_REQUEST,
                )
            setattr(prefs, field, value)
            updated_fields.append(field)

    if updated_fields:
        prefs.save(update_fields=updated_fields)

    return Response({
        'success':     True,
        'message':     'Preferences updated',
        'preferences': NotificationPreferenceSerializer(prefs).data,
    })


# ── FCM token management ──────────────────────────────────────────────────────

class FCMTokenViewSet(viewsets.ModelViewSet):
    """
    Register / list / deactivate FCM device tokens.

    Registration is idempotent:
    - If the same token is sent again (app reinstall, retry), we
      reactivate it instead of creating a duplicate that hits the
      UNIQUE constraint.
    - If a different token is sent for the same device_id, we deactivate
      the old token and activate the new one.
    """
    serializer_class   = FCMTokenSerializer
    permission_classes = [IsAuthenticated]

    def get_queryset(self):
        return FCMToken.objects.filter(user=self.request.user, is_active=True)

    def create(self, request, *args, **kwargs):
        """
        Upsert FCM token — idempotent registration.
        """
        token     = request.data.get('token', '').strip()
        device_id = request.data.get('device_id', '').strip() or None
        platform  = request.data.get('platform', 'android').lower()

        if not token or len(token) < 100:
            return Response(
                {'success': False, 'message': 'Invalid FCM token (must be ≥ 100 characters)'},
                status=status.HTTP_400_BAD_REQUEST,
            )

        if platform not in ('android', 'ios', 'web'):
            platform = 'android'

        # 1. Deactivate any other token for the same device_id (token rotated)
        if device_id:
            FCMToken.objects.filter(
                user=request.user,
                device_id=device_id,
                is_active=True,
            ).exclude(token=token).update(is_active=False)

        # 2. Upsert: update if token already exists (any user!), else create
        obj, created = FCMToken.objects.update_or_create(
            token=token,
            defaults={
                'user':      request.user,
                'device_id': device_id,
                'platform':  platform,
                'is_active': True,
            },
        )

        action = 'registered' if created else 'reactivated'
        logger.info(
            "FCM token %s for user %s (device_id=%s platform=%s)",
            action, request.user.email, device_id, platform,
        )

        return Response({
            'success':  True,
            'message':  f'Token {action} successfully',
            'token_id': obj.pk,
        }, status=status.HTTP_201_CREATED if created else status.HTTP_200_OK)

    @action(detail=False, methods=['delete'])
    def unregister(self, request):
        """Deactivate all FCM tokens for this user (logout)."""
        count = FCMToken.objects.filter(
            user=request.user, is_active=True
        ).update(is_active=False)
        logger.info("Deactivated %d FCM token(s) for user %s on logout", count, request.user.email)
        return Response({'success': True, 'deactivated': count})

    @action(detail=False, methods=['delete'], url_path='unregister-device')
    def unregister_device(self, request):
        """Deactivate the token for a specific device_id."""
        device_id = request.data.get('device_id') or request.query_params.get('device_id')
        if not device_id:
            return Response(
                {'success': False, 'message': 'device_id is required'},
                status=status.HTTP_400_BAD_REQUEST,
            )
        count = FCMToken.objects.filter(
            user=request.user, device_id=device_id, is_active=True
        ).update(is_active=False)
        return Response({'success': True, 'deactivated': count})
