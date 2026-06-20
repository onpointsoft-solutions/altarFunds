from django.urls import path, include
from rest_framework.routers import DefaultRouter
from . import views

app_name = 'notifications'

router = DefaultRouter()
router.register(r'fcm-tokens', views.FCMTokenViewSet, basename='fcm-tokens')

urlpatterns = [
    # FCM token management (register, list, unregister)
    path('', include(router.urls)),

    # Devotional sharing
    path('shares/',  views.DevotionalShareListView.as_view(), name='devotional-shares'),
    path('share/',   views.share_devotional,                  name='share-devotional'),

    # Notification inbox
    path('push/',       views.get_notifications,    name='get-notifications'),
    path('push/all/',   views.get_all_notifications, name='get-all-notifications'),
    path('push/read/',  views.mark_all_read,         name='mark-all-read'),

    # User preferences (GET + POST)
    path('preferences/', views.notification_preferences, name='preferences'),
]
