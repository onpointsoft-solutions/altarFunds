from django.urls import path, include
from rest_framework.routers import DefaultRouter
from . import views

app_name = 'notifications'

router = DefaultRouter()
router.register(r'fcm-tokens', views.FCMTokenViewSet, basename='fcm-tokens')

urlpatterns = [
    # FCM Token management
    path('', include(router.urls)),
    
    # Devotional sharing
    path('shares/', views.DevotionalShareListView.as_view(), name='devotional-shares'),
    path('share/', views.share_devotional, name='share-devotional'),
    
    # Push notifications
    path('push/', views.get_notifications, name='get-notifications'),
    path('preferences/', views.update_notification_preferences, name='update-notification-preferences'),
]
