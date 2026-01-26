from django.urls import path
from . import views

urlpatterns = [
    path('', views.home_view, name='home-page'),
    path('login/', views.login_view, name='merchant-login-page'),
    path('register/', views.register_view, name='merchant-register-page'),
    path('register/', views.MerchantRegistrationView.as_view(), name='merchant-register'),
    path('login/', views.MerchantLoginView.as_view(), name='merchant-login'),
    path('logout/', views.logout_view, name='merchant-logout'),
    path('profile/', views.MerchantProfileView.as_view(), name='merchant-profile'),
    path('api-keys/', views.ApiKeyListView.as_view(), name='api-key-list'),
    path('api-keys/<uuid:pk>/', views.ApiKeyDetailView.as_view(), name='api-key-detail'),
]
