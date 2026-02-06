from rest_framework import serializers
from .models import Announcement
from accounts.serializers import UserProfileSerializer


class AnnouncementSerializer(serializers.ModelSerializer):
    """Serializer for Announcement model"""
    
    created_by_name = serializers.CharField(source='created_by.full_name', read_only=True)
    church_name = serializers.CharField(source='church.name', read_only=True)
    priority_display = serializers.CharField(source='get_priority_display', read_only=True)
    target_audience_display = serializers.CharField(source='get_target_audience_display', read_only=True)
    is_expired = serializers.BooleanField(read_only=True)
    
    class Meta:
        model = Announcement
        fields = [
            'id', 'title', 'content', 'priority', 'priority_display',
            'target_audience', 'target_audience_display', 'church', 'church_name',
            'created_by', 'created_by_name', 'is_active', 'expires_at',
            'is_expired', 'created_at', 'updated_at'
        ]
        read_only_fields = ['created_by', 'church', 'created_at', 'updated_at']


class AnnouncementCreateSerializer(serializers.ModelSerializer):
    """Serializer for creating announcements"""
    
    class Meta:
        model = Announcement
        fields = [
            'title', 'content', 'priority', 'target_audience',
            'is_active', 'expires_at'
        ]
    
    def validate_title(self, value):
        """Validate title is not empty"""
        if not value or not value.strip():
            raise serializers.ValidationError("Title cannot be empty")
        return value.strip()
    
    def validate_content(self, value):
        """Validate content is not empty"""
        if not value or not value.strip():
            raise serializers.ValidationError("Content cannot be empty")
        return value.strip()
