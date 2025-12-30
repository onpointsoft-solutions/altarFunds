from django.contrib import admin
from .models import PaymentRequest, Payment, Transaction

@admin.register(PaymentRequest)
class PaymentRequestAdmin(admin.ModelAdmin):
    list_display = ('user', 'amount', 'payment_method', 'status', 'created_at')
    list_filter = ('status', 'payment_method', 'created_at')
    search_fields = ('user__email', 'transaction_reference')
    ordering = ('-created_at',)

@admin.register(Payment)
class PaymentAdmin(admin.ModelAdmin):
    list_display = ('payment_request', 'amount', 'payment_method', 'status', 'processed_at')
    list_filter = ('status', 'payment_method', 'processed_at')
    search_fields = ('payment_request__user__email', 'transaction_reference')
    ordering = ('-processed_at',)

@admin.register(Transaction)
class TransactionAdmin(admin.ModelAdmin):
    list_display = ('payment', 'amount', 'status', 'created_at')
    list_filter = ('status', 'created_at')
    search_fields = ('payment__transaction_reference', 'amount')
    ordering = ('-created_at',)
