from django.contrib import admin
from .models import User, Member, UserSession, PasswordResetToken

@admin.register(User)
class UserAdmin(admin.ModelAdmin):
    list_display = ('email', 'first_name', 'last_name', 'is_active', 'date_joined')
    list_filter = ('is_active', 'date_joined')
    search_fields = ('email', 'first_name', 'last_name')
    ordering = ('-date_joined',)

@admin.register(Member)
class MemberAdmin(admin.ModelAdmin):
    list_display = ('user', 'church', 'membership_number', 'date_joined', 'is_active')
    list_filter = ('is_active', 'date_joined')
    search_fields = ('membership_number', 'user__email', 'church__name')
    ordering = ('-date_joined',)

@admin.register(UserSession)
class UserSessionAdmin(admin.ModelAdmin):
    list_display = ('user', 'session_key', 'ip_address', 'created_at', 'expires_at')
    list_filter = ('created_at', 'expires_at')
    search_fields = ('user__email', 'ip_address')
    ordering = ('-created_at',)
    readonly_fields = ('session_key', 'ip_address', 'user_agent', 'created_at', 'expires_at')

@admin.register(PasswordResetToken)
class PasswordResetTokenAdmin(admin.ModelAdmin):
    list_display = ('user', 'token', 'created_at', 'expires_at', 'is_used')
    list_filter = ('is_used', 'created_at', 'expires_at')
    search_fields = ('user__email', 'token')
    ordering = ('-created_at',)
    readonly_fields = ('token', 'created_at', 'expires_at', 'is_used')
