from django.urls import path
from . import views
from .webhooks import mpesa_views

urlpatterns = [
    # Payment endpoints
    path('initiate/', views.PaymentInitiateView.as_view(), name='payment-initiate'),
    path('status/', views.PaymentStatusView.as_view(), name='payment-status'),
    path('transactions/', views.TransactionListView.as_view(), name='transaction-list'),
    path('transactions/<uuid:pk>/', views.TransactionDetailView.as_view(), name='transaction-detail'),
    path('refund/', views.refund_payment, name='payment-refund'),
    
    # M-Pesa webhook endpoints
    path('mpesa/callback/', mpesa_views.mpesa_callback, name='mpesa-callback'),
    path('mpesa/confirmation/', mpesa_views.mpesa_confirmation, name='mpesa-confirmation'),
    path('mpesa/validation/', mpesa_views.mpesa_validation, name='mpesa-validation'),
]
