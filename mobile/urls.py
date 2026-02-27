from django.urls import path
from . import views
from giving.views import create_giving_transaction, giving_categories
from giving.models import GivingTransaction
from giving.serializers import GivingTransactionSerializer
from rest_framework import generics

app_name = 'mobile'

# Mobile Giving ViewSet
class MobileGivingTransactionListView(generics.ListAPIView):
    """Mobile API to list giving transactions for current user"""
    serializer_class = GivingTransactionSerializer
    permission_classes = ['rest_framework.permissions.IsAuthenticated']
    
    def get_queryset(self):
        user = self.request.user
        if hasattr(user, 'member_profile'):
            return GivingTransaction.objects.filter(member=user.member_profile).order_by('-created_at')
        return GivingTransaction.objects.none()

urlpatterns = [
    # Authentication
    path('login/', views.MobileLoginView.as_view(), name='login'),
    path('google-login/', views.MobileGoogleLoginView.as_view(), name='google-login'),
    path('register/', views.MobileRegisterView.as_view(), name='register'),
    path('register-device/', views.MobileRegisterDeviceView.as_view(), name='register-device'),
    path('devices/', views.MobileDeviceListView.as_view(), name='device-list'),
    path('devices/<int:pk>/', views.MobileDeviceDetailView.as_view(), name='device-detail'),
    
    # Enhanced Dashboard
    path('dashboard/', views.MobileEnhancedDashboardView.as_view(), name='enhanced-dashboard'),
    
    # App Configuration
    path('config/', views.MobileAppConfigView.as_view(), name='app-config'),
    
    # User Profile & Data
    path('profile/', views.MobileUserProfileView.as_view(), name='user-profile'),
    path('giving-summary/', views.MobileGivingSummaryView.as_view(), name='giving-summary'),
    path('church-info/', views.MobileChurchInfoView.as_view(), name='church-info'),
    path('quick-actions/', views.MobileQuickActionsView.as_view(), name='quick-actions'),
    
    # Giving/Donations
    path('donations/', create_giving_transaction, name='create-donation'),
    path('giving-transactions/', MobileGivingTransactionListView.as_view(), name='giving-transactions'),
    path('giving-categories/', giving_categories, name='giving-categories'),
    
    # Notifications
    path('notifications/', views.MobileNotificationListView.as_view(), name='notification-list'),
    path('notifications/<int:pk>/', views.MobileNotificationDetailView.as_view(), name='notification-detail'),
    path('notifications/send/', views.mobile_send_push_notification, name='send-push-notification'),
    
    # Analytics
    path('analytics/track/', views.mobile_track_analytics, name='track-analytics'),
    
    # Feedback
    path('feedback/submit/', views.mobile_submit_feedback, name='submit-feedback'),
]
