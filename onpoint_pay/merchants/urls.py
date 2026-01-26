from django.urls import path
from . import views

urlpatterns = [
    path('profile/', views.MerchantProfileView.as_view(), name='merchant-profile'),
    path('api-keys/', views.ApiKeyListView.as_view(), name='api-key-list'),
    path('api-keys/<uuid:pk>/', views.ApiKeyDetailView.as_view(), name='api-key-detail'),
]
