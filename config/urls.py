"""URL Configuration for AltarFunds project"""
from django.contrib import admin
from django.urls import path, include
from django.conf import settings
from django.conf.urls.static import static
from django.http import JsonResponse
from rest_framework_simplejwt.views import (
    TokenObtainPairView,
    TokenRefreshView,
)
from django.views.decorators.csrf import csrf_exempt
from admin_management.custom_admin import altar_admin_site

def api_root(request):
    """API root endpoint"""
    return JsonResponse({
        'message': 'Welcome to AltarFunds API',
        'version': '1.0',
        'endpoints': {
            'admin': '/admin/',
            'altar_admin': '/altar-admin/',
            'api': {
                'accounts': '/api/accounts/',
                'churches': '/api/churches/',
                'giving': '/api/giving/',
                'donations': '/api/donations/',
                'expenses': '/api/expenses/',
                'budgets': '/api/budgets/',
                'members': '/api/members/',
                'reports': '/api/reports/',
                'dashboard': '/api/dashboard/',
                'health': '/api/health/',
            },
            'auth': {
                'token': '/api/auth/token/',
                'refresh': '/api/auth/token/refresh/',
                'register': '/register/',
                'login': '/login/',
            }
        }
    })

urlpatterns = [
    # API Root
    path('', api_root, name='api_root'),
    
    # Standard Django Admin (for development)
    path('admin/', admin.site.urls),
    
    # Custom AltarFunds Admin
    path('altar-admin/', altar_admin_site.urls),
    
    # API Authentication
    path('api/auth/token/', csrf_exempt(TokenObtainPairView.as_view()), name='token_obtain_pair'),
    path('api/auth/token/refresh/', csrf_exempt(TokenRefreshView.as_view()), name='token_refresh'),
    
    # API Modules
    path('api/admin/', include('admin_management.urls')),
    path('api/accounts/', include('accounts.urls')),
    path('api/mobile/', include('mobile.urls')),
    path('api/churches/', include('churches.urls')),
    path('api/giving/', include('giving.urls')),
    path('api/payments/', include('payments.urls')),
    path('api/expenses/', include('expenses.urls')),
    path('api/budgets/', include('budgets.urls')),
    path('api/donations/', include('donations.urls')),
    path('api/members/', include('members.urls')),
    path('api/accounting/', include('accounting.urls')),
    path('api/reports/', include('reports.urls')),
    path('api/audit/', include('audit.urls')),
    path('api/notifications/', include('notifications.urls')),
    path('api/', include('devotionals.urls')),
    path('api/', include('notices.urls')),
    
    # Health check
    path('api/health/', include('common.urls')),

    # Dashboard
    path('dashboard/', include('dashboard.urls')),
    
    # API Dashboard endpoints
    path('api/dashboard/', include('dashboard.urls')),

    # Template-based Authentication
    path('', include('accounts.auth_urls')),
]

# Serve static and media files in development
if settings.DEBUG:
    urlpatterns += static(settings.STATIC_URL, document_root=settings.STATIC_ROOT)
    urlpatterns += static(settings.MEDIA_URL, document_root=settings.MEDIA_ROOT)
