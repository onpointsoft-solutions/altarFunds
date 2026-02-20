from django.db import models
from django.utils.translation import gettext_lazy as _
from common.models import TimeStampedModel, SoftDeleteModel
from accounts.models import User
from churches.models import Church, ChurchService
import uuid


class AttendanceRecord(TimeStampedModel, SoftDeleteModel):
    """Main attendance record for a specific service"""
    
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    church = models.ForeignKey(Church, on_delete=models.CASCADE, related_name='attendance_records')
    service = models.ForeignKey(ChurchService, on_delete=models.CASCADE, related_name='attendance_records')
    recorded_by = models.ForeignKey(User, on_delete=models.SET_NULL, null=True, related_name='recorded_attendances')
    service_date = models.DateField(_('Service Date'))
    total_attendance = models.PositiveIntegerField(_('Total Attendance'), default=0)
    male_attendance = models.PositiveIntegerField(_('Male Attendance'), default=0)
    female_attendance = models.PositiveIntegerField(_('Female Attendance'), default=0)
    children_attendance = models.PositiveIntegerField(_('Children Attendance'), default=0)
    youth_attendance = models.PositiveIntegerField(_('Youth Attendance'), default=0)
    visitors_count = models.PositiveIntegerField(_('Visitors Count'), default=0)
    new_converts = models.PositiveIntegerField(_('New Converts'), default=0)
    notes = models.TextField(_('Notes'), blank=True)
    
    class Meta:
        db_table = 'attendance_records'
        verbose_name = _('Attendance Record')
        verbose_name_plural = _('Attendance Records')
        ordering = ['-service_date', '-created_at']
        unique_together = ['church', 'service', 'service_date']
        indexes = [
            models.Index(fields=['church', 'service_date']),
            models.Index(fields=['service', 'service_date']),
            models.Index(fields=['recorded_by']),
        ]
    
    def __str__(self):
        return f"{self.church.name} - {self.service.name} - {self.service_date}"


class MemberAttendance(TimeStampedModel, SoftDeleteModel):
    """Individual member attendance tracking"""
    
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    attendance_record = models.ForeignKey(AttendanceRecord, on_delete=models.CASCADE, related_name='member_attendances')
    member = models.ForeignKey(User, on_delete=models.CASCADE, related_name='attendances')
    is_present = models.BooleanField(_('Present'), default=True)
    arrival_time = models.TimeField(_('Arrival Time'), null=True, blank=True)
    departure_time = models.TimeField(_('Departure Time'), null=True, blank=True)
    is_visitor = models.BooleanField(_('Visitor'), default=False)
    notes = models.TextField(_('Notes'), blank=True)
    
    class Meta:
        db_table = 'member_attendances'
        verbose_name = _('Member Attendance')
        verbose_name_plural = _('Member Attendances')
        unique_together = ['attendance_record', 'member']
        indexes = [
            models.Index(fields=['attendance_record', 'is_present']),
            models.Index(fields=['member', 'is_present']),
            models.Index(fields=['is_visitor']),
        ]
    
    def __str__(self):
        return f"{self.member.get_full_name()} - {self.attendance_record}"


class ServiceType(TimeStampedModel):
    """Service types for attendance tracking"""
    
    name = models.CharField(_('Service Name'), max_length=100)
    church = models.ForeignKey(Church, on_delete=models.CASCADE, related_name='service_types')
    default_start_time = models.TimeField(_('Default Start Time'))
    typical_duration_minutes = models.PositiveIntegerField(_('Typical Duration (minutes)'), default=60)
    is_active = models.BooleanField(_('Active'), default=True)
    
    class Meta:
        db_table = 'service_types'
        verbose_name = _('Service Type')
        verbose_name_plural = _('Service Types')
        unique_together = ['church', 'name']
        ordering = ['church', 'name']
    
    def __str__(self):
        return f"{self.church.name} - {self.name}"


class AttendanceSettings(TimeStampedModel):
    """Church-specific attendance settings"""
    
    church = models.OneToOneField(Church, on_delete=models.CASCADE, related_name='attendance_settings')
    enable_individual_tracking = models.BooleanField(_('Enable Individual Tracking'), default=True)
    enable_arrival_departure_times = models.BooleanField(_('Enable Arrival/Departure Times'), default=False)
    auto_mark_present_threshold_minutes = models.PositiveIntegerField(
        _('Auto Mark Present Threshold (minutes)'), 
        default=15,
        help_text=_('Automatically mark as present if checked in within this many minutes of service start')
    )
    require_usher_approval = models.BooleanField(_('Require Usher Approval'), default=True)
    allow_self_checkin = models.BooleanField(_('Allow Self Check-in'), default=False)
    checkin_window_minutes_before = models.PositiveIntegerField(_('Check-in Window Before Service (minutes)'), default=30)
    checkin_window_minutes_after = models.PositiveIntegerField(_('Check-in Window After Service (minutes)'), default=60)
    
    class Meta:
        db_table = 'attendance_settings'
        verbose_name = _('Attendance Settings')
        verbose_name_plural = _('Attendance Settings')
    
    def __str__(self):
        return f"{self.church.name} - Attendance Settings"
