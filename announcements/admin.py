from django.contrib import admin
from .models import Announcement


@admin.register(Announcement)
class AnnouncementAdmin(admin.ModelAdmin):
    list_display = ['title', 'church', 'priority', 'target_audience', 'is_active', 'created_by', 'created_at']
    list_filter = ['priority', 'target_audience', 'is_active', 'church', 'created_at']
    search_fields = ['title', 'content', 'church__name', 'created_by__email']
    readonly_fields = ['created_at', 'updated_at']
    date_hierarchy = 'created_at'
    
    fieldsets = (
        ('Announcement Details', {
            'fields': ('title', 'content', 'priority', 'target_audience')
        }),
        ('Status & Expiration', {
            'fields': ('is_active', 'expires_at')
        }),
        ('Metadata', {
            'fields': ('church', 'created_by', 'created_at', 'updated_at'),
            'classes': ('collapse',)
        }),
    )
