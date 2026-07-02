from django.contrib import admin
from .models import DemoBooking


@admin.register(DemoBooking)
class DemoBookingAdmin(admin.ModelAdmin):
    list_display  = ['full_name', 'church_name', 'email', 'phone_number',
                     'demo_type', 'demo_date', 'status', 'created_at']
    list_filter   = ['status', 'demo_type', 'church_size', 'created_at']
    search_fields = ['first_name', 'last_name', 'email', 'church_name', 'phone_number']
    ordering      = ['-created_at']
    readonly_fields = ['created_at', 'updated_at']
    fieldsets = (
        ('Contact', {'fields': ('first_name', 'last_name', 'email', 'phone_number')}),
        ('Church',  {'fields': ('church_name', 'church_size')}),
        ('Demo',    {'fields': ('demo_type', 'demo_date', 'demo_time', 'specific_needs')}),
        ('Admin',   {'fields': ('status', 'notes', 'created_at', 'updated_at')}),
    )
