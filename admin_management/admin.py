"""
Custom admin registrations for AltarFunds models
"""

from django.contrib import admin
from django.utils.html import format_html
from django.db.models import Count, Sum, Avg
from django.utils.safestring import mark_safe
from django.urls import reverse
from django.utils import timezone
from datetime import timedelta

from churches.models import Church
from accounts.models import User
from giving.models import GivingTransaction, GivingCategory
from payments.models import Payment
from mobile.models import MobileDevice

# Import the custom admin site
from .custom_admin import altar_admin_site


@admin.register(Church, site=altar_admin_site)
class ChurchAdmin(admin.ModelAdmin):
    """Custom Church admin with enhanced features"""
    list_display = (
        'name', 'church_code', 'city', 'senior_pastor_name', 
        'member_count', 'total_giving', 'verification_status', 'is_active'
    )
    list_filter = (
        'is_active', 'is_verified', 'church_type', 
        'city', 'created_at'
    )
    search_fields = ('name', 'church_code', 'senior_pastor_name', 'city')
    ordering = ('-created_at',)
    
    fieldsets = (
        ('Basic Information', {
            'fields': (
                'name', 'church_code', 'church_type', 'description',
                'senior_pastor_name', 'senior_pastor_phone', 'senior_pastor_email'
            )
        }),
        ('Contact Information', {
            'fields': (
                'email', 'phone_number', 'website',
                'address_line1', 'address_line2', 'city', 'county'
            )
        }),
        ('Status & Settings', {
            'fields': (
                'is_active', 'is_verified', 'registration_number',
                'established_date', 'denomination'
            )
        }),
        ('Statistics', {
            'fields': (
                'member_count', 'average_attendance',
                'campus_count', 'department_count'
            ),
            'classes': ('collapse',)
        }),
    )
    
    readonly_fields = ('member_count', 'total_giving')
    
    def member_count(self, obj):
        return obj.member_count
    member_count.short_description = 'Members'
    
    def total_giving(self, obj):
        total = obj.givingtransaction_set.aggregate(
            total=Sum('amount')
        )['total'] or 0
        return f'${total:,.2f}'
    total_giving.short_description = 'Total Giving'
    
    def verification_status(self, obj):
        if obj.is_verified:
            return format_html(
                '<span style="color: #10b981;">✓ Verified</span>'
            )
        return format_html(
            '<span style="color: #f59e0b;">⏳ Pending</span>'
        )
    verification_status.short_description = 'Status'


@admin.register(GivingTransaction, site=altar_admin_site)
class GivingTransactionAdmin(admin.ModelAdmin):
    """Custom Giving Transaction admin"""
    list_display = (
        'transaction_id', 'user_email', 'church_name', 'category', 
        'formatted_amount', 'payment_method', 'status_badge', 'created_at'
    )
    list_filter = (
        'status', 'payment_method', 'category',
        'created_at', 'church'
    )
    search_fields = (
        'user__email', 'church__name', 'reference', 'transaction_id'
    )
    ordering = ('-created_at',)
    list_per_page = 50
    date_hierarchy = 'created_at'
    
    fieldsets = (
        ('Transaction Details', {
            'fields': (
                'user', 'church', 'category', 'amount',
                'payment_method', 'status'
            )
        }),
        ('Additional Information', {
            'fields': (
                'reference', 'notes', 'receipt_number'
            ),
            'classes': ('collapse',)
        }),
        ('Timestamps', {
            'fields': ('created_at', 'updated_at'),
            'classes': ('collapse',)
        }),
    )
    
    readonly_fields = ('created_at', 'updated_at', 'transaction_id')
    
    actions = ['mark_as_completed', 'mark_as_failed', 'export_transactions']
    
    def transaction_id(self, obj):
        return obj.reference or f'TXN-{obj.id}'
    transaction_id.short_description = 'Transaction ID'
    
    def user_email(self, obj):
        return obj.user.email if obj.user else 'N/A'
    user_email.short_description = 'User'
    user_email.admin_order_field = 'user__email'
    
    def church_name(self, obj):
        return obj.church.name if obj.church else 'N/A'
    church_name.short_description = 'Church'
    church_name.admin_order_field = 'church__name'
    
    def formatted_amount(self, obj):
        return format_html(
            '<strong style="color: #10b981;">${:,.2f}</strong>',
            obj.amount
        )
    formatted_amount.short_description = 'Amount'
    formatted_amount.admin_order_field = 'amount'
    
    def status_badge(self, obj):
        colors = {
            'completed': '#10b981',
            'pending': '#f59e0b',
            'failed': '#ef4444',
            'refunded': '#6366f1'
        }
        color = colors.get(obj.status, '#64748b')
        return format_html(
            '<span style="background: {}; color: white; padding: 0.25rem 0.75rem; '
            'border-radius: 9999px; font-size: 0.75rem; font-weight: 600; '
            'text-transform: uppercase;">{}</span>',
            color, obj.get_status_display()
        )
    status_badge.short_description = 'Status'
    
    def mark_as_completed(self, request, queryset):
        updated = queryset.update(status='completed')
        self.message_user(request, f'{updated} transactions marked as completed.')
    mark_as_completed.short_description = 'Mark selected as completed'
    
    def mark_as_failed(self, request, queryset):
        updated = queryset.update(status='failed')
        self.message_user(request, f'{updated} transactions marked as failed.')
    mark_as_failed.short_description = 'Mark selected as failed'
    
    def export_transactions(self, request, queryset):
        # This would export to CSV - simplified for now
        self.message_user(request, f'{queryset.count()} transactions ready for export.')
    export_transactions.short_description = 'Export selected transactions'


@admin.register(GivingCategory, site=altar_admin_site)
class GivingCategoryAdmin(admin.ModelAdmin):
    """Custom Giving Category admin"""
    list_display = (
        'name', 'church', 'tax_status', 'active_status',
        'transaction_count', 'total_amount'
    )
    list_filter = (
        'is_tax_deductible', 'is_active', 'church'
    )
    search_fields = (
        'name', 'church__name', 'description'
    )
    ordering = ('church', 'name')
    list_editable = ('is_active',) if 'is_active' in locals() else ()
    
    fieldsets = (
        ('Category Information', {
            'fields': (
                'name', 'church', 'description',
                'is_tax_deductible', 'is_active'
            )
        }),
        ('Budget Settings', {
            'fields': (
                'has_target', 'monthly_target', 'annual_target'
            ),
            'classes': ('collapse',)
        }),
    )
    
    actions = ['activate_categories', 'deactivate_categories']
    
    def tax_status(self, obj):
        if obj.is_tax_deductible:
            return format_html(
                '<span style="color: #10b981;">✓ Tax Deductible</span>'
            )
        return format_html(
            '<span style="color: #64748b;">Non-Deductible</span>'
        )
    tax_status.short_description = 'Tax Status'
    
    def active_status(self, obj):
        if obj.is_active:
            return format_html(
                '<span style="color: #10b981;">● Active</span>'
            )
        return format_html(
            '<span style="color: #ef4444;">● Inactive</span>'
        )
    active_status.short_description = 'Status'
    
    def transaction_count(self, obj):
        count = obj.givingtransaction_set.count()
        return format_html(
            '<strong>{}</strong> transactions',
            count
        )
    transaction_count.short_description = 'Transactions'
    
    def total_amount(self, obj):
        total = obj.givingtransaction_set.aggregate(
            total=Sum('amount')
        )['total'] or 0
        return format_html(
            '<strong style="color: #10b981;">${:,.2f}</strong>',
            total
        )
    total_amount.short_description = 'Total Amount'
    
    def activate_categories(self, request, queryset):
        updated = queryset.update(is_active=True)
        self.message_user(request, f'{updated} categories activated.')
    activate_categories.short_description = 'Activate selected categories'
    
    def deactivate_categories(self, request, queryset):
        updated = queryset.update(is_active=False)
        self.message_user(request, f'{updated} categories deactivated.')
    deactivate_categories.short_description = 'Deactivate selected categories'


@admin.register(User, site=altar_admin_site)
class UserAdmin(admin.ModelAdmin):
    """Custom User admin"""
    list_display = (
        'email', 'full_name', 'role_badge', 'church',
        'status_indicator', 'verification_status', 'date_joined'
    )
    list_filter = (
        'role', 'is_active', 'is_email_verified',
        'date_joined', 'church'
    )
    search_fields = (
        'email', 'first_name', 'last_name', 'phone_number'
    )
    ordering = ('-date_joined',)
    list_per_page = 50
    date_hierarchy = 'date_joined'
    
    fieldsets = (
        ('User Information', {
            'fields': (
                'email', 'first_name', 'last_name',
                'phone_number', 'role', 'church'
            )
        }),
        ('Status', {
            'fields': (
                'is_active', 'is_verified', 'is_staff', 'is_superuser'
            )
        }),
        ('Permissions', {
            'fields': ('groups', 'user_permissions'),
            'classes': ('collapse',)
        }),
        ('Timestamps', {
            'fields': ('date_joined', 'last_login'),
            'classes': ('collapse',)
        }),
    )
    
    readonly_fields = ('date_joined', 'last_login')
    
    actions = ['verify_users', 'activate_users', 'deactivate_users']
    
    def full_name(self, obj):
        return f"{obj.first_name} {obj.last_name}" if obj.first_name else obj.email
    full_name.short_description = 'Name'
    
    def role_badge(self, obj):
        colors = {
            'member': '#6366f1',
            'pastor': '#10b981',
            'treasurer': '#f59e0b',
            'admin': '#ef4444',
            'system_admin': '#8b5cf6'
        }
        color = colors.get(obj.role, '#64748b')
        return format_html(
            '<span style="background: {}; color: white; padding: 0.25rem 0.75rem; '
            'border-radius: 9999px; font-size: 0.75rem; font-weight: 600; '
            'text-transform: uppercase;">{}</span>',
            color, obj.get_role_display() if hasattr(obj, 'get_role_display') else obj.role
        )
    role_badge.short_description = 'Role'
    
    def status_indicator(self, obj):
        if obj.is_active:
            return format_html(
                '<span style="color: #10b981; font-weight: 600;">● Active</span>'
            )
        return format_html(
            '<span style="color: #ef4444; font-weight: 600;">● Inactive</span>'
        )
    status_indicator.short_description = 'Status'
    
    def verification_status(self, obj):
        if obj.is_email_verified:
            return format_html(
                '<span style="color: #10b981;">✓ Verified</span>'
            )
        return format_html(
            '<span style="color: #f59e0b;">⏳ Pending</span>'
        )
    verification_status.short_description = 'Verification'
    
    def verify_users(self, request, queryset):
        updated = queryset.update(is_email_verified=True)
        self.message_user(request, f'{updated} users verified.')
    verify_users.short_description = 'Verify selected users'
    
    def activate_users(self, request, queryset):
        updated = queryset.update(is_active=True)
        self.message_user(request, f'{updated} users activated.')
    activate_users.short_description = 'Activate selected users'
    
    def deactivate_users(self, request, queryset):
        updated = queryset.update(is_active=False)
        self.message_user(request, f'{updated} users deactivated.')
    deactivate_users.short_description = 'Deactivate selected users'


@admin.register(MobileDevice, site=altar_admin_site)
class MobileDeviceAdmin(admin.ModelAdmin):
    """Custom Mobile Device admin"""
    list_display = (
        'device_name', 'user', 'device_type', 'app_version',
        'status', 'last_seen'
    )
    list_filter = (
        'device_type', 'status', 'app_version',
        'last_seen'
    )
    search_fields = (
        'user__email', 'device_id', 'device_token'
    )
    ordering = ('-last_seen',)
    
    def device_name(self, obj):
        return obj.device_id or obj.device_token or 'Unknown'
    device_name.short_description = 'Device Name'


# Custom admin site configuration
altar_admin_site.site_header = 'AltarFunds Administration'
altar_admin_site.site_title = 'AltarFunds Admin'
altar_admin_site.index_title = 'Dashboard'

# Add custom CSS and JS
altar_admin_site.site_header = format_html(
    '<i class="fas fa-church"></i> {}',
    altar_admin_site.site_header
)
