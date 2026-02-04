from django.contrib import admin
from .models import Devotional, DevotionalComment, DevotionalReaction


@admin.register(Devotional)
class DevotionalAdmin(admin.ModelAdmin):
    list_display = ['title', 'author', 'church', 'date', 'is_published', 'created_at']
    list_filter = ['is_published', 'date', 'church']
    search_fields = ['title', 'content', 'author__email']
    date_hierarchy = 'date'
    ordering = ['-date', '-created_at']


@admin.register(DevotionalComment)
class DevotionalCommentAdmin(admin.ModelAdmin):
    list_display = ['devotional', 'user', 'created_at']
    list_filter = ['created_at']
    search_fields = ['content', 'user__email', 'devotional__title']
    ordering = ['-created_at']


@admin.register(DevotionalReaction)
class DevotionalReactionAdmin(admin.ModelAdmin):
    list_display = ['devotional', 'user', 'reaction_type', 'created_at']
    list_filter = ['reaction_type', 'created_at']
    search_fields = ['user__email', 'devotional__title']
    ordering = ['-created_at']
