"""
Enhanced URLs for AltarFunds Admin Management
"""

from django.urls import path, include
from django.contrib.auth import views as auth_views
from . import auth_views as custom_auth_views
from .custom_admin import altar_admin_site

app_name = 'admin_management'

urlpatterns = [
    # Custom admin site
    path('altar-admin/', altar_admin_site.urls),
    
    # Enhanced authentication views
    path('login/', custom_auth_views.EnhancedLoginView.as_view(), name='enhanced_login'),
    path('logout/', custom_auth_views.EnhancedLogoutView.as_view(), name='enhanced_logout'),
    path('password-change/', custom_auth_views.EnhancedPasswordChangeView.as_view(), name='enhanced_password_change'),
    path('password-change/done/', custom_auth_views.EnhancedPasswordChangeDoneView.as_view(), name='enhanced_password_change_done'),
    path('password-reset/', custom_auth_views.EnhancedPasswordResetView.as_view(), name='enhanced_password_reset'),
    path('password-reset/done/', custom_auth_views.EnhancedPasswordResetDoneView.as_view(), name='enhanced_password_reset_done'),
    path('reset/<uidb64>/<token>/', custom_auth_views.EnhancedPasswordResetConfirmView.as_view(), name='enhanced_password_reset_confirm'),
    path('reset/done/', custom_auth_views.EnhancedPasswordResetCompleteView.as_view(), name='enhanced_password_reset_complete'),
    
    # Dashboard
    path('dashboard/', altar_admin_site.admin_view(altar_admin_site.dashboard_view), name='enhanced_dashboard'),
    
    # API endpoints for dashboard
    path('api/stats/', altar_admin_site.admin_view(altar_admin_site.api_stats), name='api_stats'),
    path('api/health/', altar_admin_site.admin_view(altar_admin_site.system_health), name='api_health'),
    
    # Profile management
    path('profile/', custom_auth_views.ProfileView.as_view(), name='profile'),
    path('profile/edit/', custom_auth_views.ProfileEditView.as_view(), name='profile_edit'),
    
    # Settings
    path('settings/', custom_auth_views.SettingsView.as_view(), name='settings'),
    
    # Help and documentation
    path('help/', custom_auth_views.HelpView.as_view(), name='help'),
    path('documentation/', custom_auth_views.DocumentationView.as_view(), name='documentation'),
]

# Include default admin URLs for backward compatibility
urlpatterns += [
    path('', include('django.contrib.admin.urls')),
]
