from django.contrib import admin
from .models import AttendanceRecord, MemberAttendance, ServiceType, AttendanceSettings


@admin.register(AttendanceRecord)
class AttendanceRecordAdmin(admin.ModelAdmin):
    list_display = [
        'church', 'service', 'service_date', 'total_attendance',
        'recorded_by', 'created_at'
    ]
    list_filter = [
        'church', 'service', 'service_date', 'created_at'
    ]
    search_fields = [
        'church__name', 'service__name', 'recorded_by__get_full_name'
    ]
    date_hierarchy = 'service_date'
    readonly_fields = ['created_at', 'updated_at']
    
    fieldsets = (
        ('Basic Information', {
            'fields': ('church', 'service', 'service_date', 'recorded_by')
        }),
        ('Attendance Counts', {
            'fields': (
                'total_attendance', 'male_attendance', 'female_attendance',
                'children_attendance', 'youth_attendance', 'visitors_count',
                'new_converts'
            )
        }),
        ('Additional Information', {
            'fields': ('notes', 'created_at', 'updated_at')
        }),
    )


@admin.register(MemberAttendance)
class MemberAttendanceAdmin(admin.ModelAdmin):
    list_display = [
        'member', 'attendance_record', 'is_present', 'is_visitor',
        'arrival_time', 'departure_time'
    ]
    list_filter = [
        'is_present', 'is_visitor', 'attendance_record__service_date'
    ]
    search_fields = [
        'member__get_full_name', 'member__email', 'attendance_record__church__name'
    ]
    readonly_fields = ['created_at', 'updated_at']
    
    fieldsets = (
        ('Basic Information', {
            'fields': ('attendance_record', 'member', 'is_present', 'is_visitor')
        }),
        ('Time Information', {
            'fields': ('arrival_time', 'departure_time')
        }),
        ('Additional Information', {
            'fields': ('notes', 'created_at', 'updated_at')
        }),
    )


@admin.register(ServiceType)
class ServiceTypeAdmin(admin.ModelAdmin):
    list_display = [
        'name', 'church', 'default_start_time', 'typical_duration_minutes', 'is_active'
    ]
    list_filter = ['church', 'is_active']
    search_fields = ['name', 'church__name']
    readonly_fields = ['created_at', 'updated_at']
    
    fieldsets = (
        ('Basic Information', {
            'fields': ('name', 'church', 'is_active')
        }),
        ('Service Schedule', {
            'fields': ('default_start_time', 'typical_duration_minutes')
        }),
        ('Timestamps', {
            'fields': ('created_at', 'updated_at'),
            'classes': ('collapse',)
        }),
    )


@admin.register(AttendanceSettings)
class AttendanceSettingsAdmin(admin.ModelAdmin):
    list_display = ['church', 'enable_individual_tracking', 'require_usher_approval']
    list_filter = [
        'enable_individual_tracking', 'enable_arrival_departure_times',
        'require_usher_approval', 'allow_self_checkin'
    ]
    search_fields = ['church__name']
    readonly_fields = ['created_at', 'updated_at']
    
    fieldsets = (
        ('Basic Settings', {
            'fields': (
                'enable_individual_tracking', 'enable_arrival_departure_times',
                'require_usher_approval', 'allow_self_checkin'
            )
        }),
        ('Check-in Settings', {
            'fields': (
                'auto_mark_present_threshold_minutes',
                'checkin_window_minutes_before',
                'checkin_window_minutes_after'
            )
        }),
        ('Timestamps', {
            'fields': ('created_at', 'updated_at'),
            'classes': ('collapse',)
        }),
    )
