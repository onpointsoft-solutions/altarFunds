from django.urls import path
from . import views

app_name = 'notifications'

urlpatterns = [
    # Devotional sharing
    path('shares/', views.DevotionalShareListView.as_view(), name='devotional-shares'),
    path('share/', views.share_devotional, name='share-devotional'),
    
    # Push notifications
    path('push/', views.get_notifications, name='get-notifications'),
    path('preferences/', views.update_notification_preferences, name='update-notification-preferences'),
]
