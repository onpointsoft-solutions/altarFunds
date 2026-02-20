from rest_framework import serializers
from .models import AttendanceRecord, MemberAttendance, ServiceType, AttendanceSettings
from accounts.serializers import UserSerializer
from churches.serializers import ChurchSerializer, ChurchServiceSerializer


class ServiceTypeSerializer(serializers.ModelSerializer):
    """Serializer for ServiceType model"""
    
    class Meta:
        model = ServiceType
        fields = [
            'id', 'name', 'church', 'default_start_time', 
            'typical_duration_minutes', 'is_active'
        ]
        read_only_fields = ['id']


class AttendanceRecordSerializer(serializers.ModelSerializer):
    """Serializer for AttendanceRecord model"""
    
    church_name = serializers.CharField(source='church.name', read_only=True)
    service_name = serializers.CharField(source='service.name', read_only=True)
    recorded_by_name = serializers.CharField(source='recorded_by.get_full_name', read_only=True)
    
    class Meta:
        model = AttendanceRecord
        fields = [
            'id', 'church', 'church_name', 'service', 'service_name',
            'recorded_by', 'recorded_by_name', 'service_date',
            'total_attendance', 'male_attendance', 'female_attendance',
            'children_attendance', 'youth_attendance', 'visitors_count',
            'new_converts', 'notes', 'created_at', 'updated_at'
        ]
        read_only_fields = ['id', 'created_at', 'updated_at']
    
    def validate(self, data):
        """Validate that total attendance equals sum of categories"""
        total = data.get('total_attendance', 0)
        male = data.get('male_attendance', 0)
        female = data.get('female_attendance', 0)
        children = data.get('children_attendance', 0)
        youth = data.get('youth_attendance', 0)
        
        calculated_total = male + female + children + youth
        if total != calculated_total:
            raise serializers.ValidationError(
                f"Total attendance ({total}) must equal sum of categories ({calculated_total})"
            )
        return data


class MemberAttendanceSerializer(serializers.ModelSerializer):
    """Serializer for MemberAttendance model"""
    
    member_name = serializers.CharField(source='member.get_full_name', read_only=True)
    member_email = serializers.CharField(source='member.email', read_only=True)
    
    class Meta:
        model = MemberAttendance
        fields = [
            'id', 'attendance_record', 'member', 'member_name', 'member_email',
            'is_present', 'arrival_time', 'departure_time', 'is_visitor', 'notes',
            'created_at', 'updated_at'
        ]
        read_only_fields = ['id', 'created_at', 'updated_at']


class AttendanceRecordDetailSerializer(AttendanceRecordSerializer):
    """Detailed serializer including member attendances"""
    
    member_attendances = MemberAttendanceSerializer(many=True, read_only=True)
    
    class Meta(AttendanceRecordSerializer.Meta):
        fields = AttendanceRecordSerializer.Meta.fields + ['member_attendances']


class AttendanceSettingsSerializer(serializers.ModelSerializer):
    """Serializer for AttendanceSettings model"""
    
    church_name = serializers.CharField(source='church.name', read_only=True)
    
    class Meta:
        model = AttendanceSettings
        fields = [
            'id', 'church', 'church_name', 'enable_individual_tracking',
            'enable_arrival_departure_times', 'auto_mark_present_threshold_minutes',
            'require_usher_approval', 'allow_self_checkin',
            'checkin_window_minutes_before', 'checkin_window_minutes_after',
            'created_at', 'updated_at'
        ]
        read_only_fields = ['id', 'created_at', 'updated_at']


class QuickAttendanceSerializer(serializers.Serializer):
    """Quick attendance marking serializer for ushers"""
    
    member_ids = serializers.ListField(
        child=serializers.UUIDField(),
        help_text="List of member UUIDs to mark as present"
    )
    service_date = serializers.DateField(help_text="Service date")
    service_id = serializers.UUIDField(help_text="Service UUID")
    notes = serializers.CharField(required=False, allow_blank=True, help_text="Optional notes")
    
    def validate_member_ids(self, value):
        if not value:
            raise serializers.ValidationError("At least one member must be selected")
        return value
