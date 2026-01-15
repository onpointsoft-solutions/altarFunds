from django.urls import path, include
from rest_framework.routers import DefaultRouter
from .views import (
    SuperAdminDashboardViewSet,
    ChurchAdminViewSet,
    SystemNotificationViewSet,
    ChurchActivityViewSet,
    SubscriptionPlanViewSet,
    ChurchAnalyticsViewSet,
    SystemHealthViewSet
)

router = DefaultRouter()
router.register(r'dashboard/stats', SuperAdminDashboardViewSet, basename='super-admin-dashboard')
router.register(r'churches', ChurchAdminViewSet, basename='admin-churches')
router.register(r'notifications', SystemNotificationViewSet, basename='admin-notifications')
router.register(r'activity', ChurchActivityViewSet, basename='admin-activity')
router.register(r'subscription-plans', SubscriptionPlanViewSet, basename='admin-subscription-plans')
router.register(r'system/health', SystemHealthViewSet, basename='admin-system-health')

app_name = 'admin_management'

urlpatterns = [
    path('', include(router.urls)),
    path('auth/', include('admin_management.auth_urls')),
]
