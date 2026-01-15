from django.urls import path
from .auth_views import SuperAdminLoginView, SuperAdminValidateView

app_name = 'admin_management_auth'

urlpatterns = [
    path('login/', SuperAdminLoginView, name='super-admin-login'),
    path('validate/', SuperAdminValidateView, name='validate-super-admin'),
]
