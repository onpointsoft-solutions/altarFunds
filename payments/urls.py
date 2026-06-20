from django.urls import path, include
from rest_framework.routers import DefaultRouter
from .views import (
    PaymentRequestViewSet,
    PaymentViewSet,
    TransactionViewSet,
    PaystackAccountViewSet,
    # webhook & existing helpers
    paystack_webhook,
    get_church_accounts,
    create_paystack_account,
    initiate_giving_with_account,
    # new transfer / disbursement endpoints
    create_transfer_recipient,
    verify_bank_account,
    list_banks,
    get_disbursement_status,
)

app_name = 'payments'

router = DefaultRouter()
router.register(r'requests',          PaymentRequestViewSet)
router.register(r'payments',          PaymentViewSet)
router.register(r'transactions',      TransactionViewSet)
router.register(r'paystack-accounts', PaystackAccountViewSet, basename='paystack-accounts')

urlpatterns = [
    path('', include(router.urls)),

    # ── Paystack webhook (must be AllowAny / csrf_exempt) ─────────────────
    path('paystack/webhook/',   paystack_webhook,           name='paystack-webhook'),

    # ── Church Paystack account management ────────────────────────────────
    path('church-accounts/',    get_church_accounts,        name='get-church-accounts'),
    path('create-account/',     create_paystack_account,    name='create-paystack-account'),
    path('initiate-giving/',    initiate_giving_with_account, name='initiate-giving-with-account'),

    # ── Paystack Transfer / disbursement ──────────────────────────────────
    # Step 1 (one-time setup): register a church bank account with Paystack
    path('create-transfer-recipient/',  create_transfer_recipient,  name='create-transfer-recipient'),
    # Utility: verify a bank account number before registering
    path('verify-bank-account/',        verify_bank_account,        name='verify-bank-account'),
    # Utility: list Paystack-supported banks and their codes
    path('list-banks/',                 list_banks,                 name='list-banks'),
    # Check disbursement status for a giving transaction
    path('disbursement/<str:transaction_id>/', get_disbursement_status, name='disbursement-status'),
]
