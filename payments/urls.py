from django.urls import path, include
from rest_framework.routers import DefaultRouter
from .views import PaymentRequestViewSet, PaymentViewSet, TransactionViewSet, paystack_webhook

app_name = 'payments'

router = DefaultRouter()
router.register(r'requests', PaymentRequestViewSet)
router.register(r'payments', PaymentViewSet)
router.register(r'transactions', TransactionViewSet)

urlpatterns = [
    path('', include(router.urls)),
    path('paystack/webhook/', paystack_webhook, name='paystack-webhook'),
]
