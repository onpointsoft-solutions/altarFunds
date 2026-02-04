from django.contrib import admin
from .models import Notice


@admin.register(Notice)
class NoticeAdmin(admin.ModelAdmin):
    list_display = ['title', 'church', 'author', 'priority', 'category', 'date', 'is_published']
    list_filter = ['priority', 'category', 'is_published', 'date']
    search_fields = ['title', 'content', 'author__email']
    date_hierarchy = 'date'
    ordering = ['-date', '-created_at']
