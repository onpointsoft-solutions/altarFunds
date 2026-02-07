from rest_framework import serializers
from .models import Suggestion
from accounts.models import User


class SuggestionSerializer(serializers.ModelSerializer):
    member_name = serializers.SerializerMethodField()
    member_email = serializers.SerializerMethodField()
    status_display = serializers.CharField(source='get_status_display', read_only=True)
    category_display = serializers.CharField(source='get_category_display', read_only=True)
    reviewed_by_name = serializers.SerializerMethodField()
    
    class Meta:
        model = Suggestion
        fields = [
            'id', 'church', 'member', 'member_name', 'member_email',
            'title', 'description', 'category', 'category_display',
            'status', 'status_display', 'is_anonymous',
            'reviewed_by', 'reviewed_by_name', 'reviewed_at',
            'response', 'created_at', 'updated_at'
        ]
        read_only_fields = ['church', 'member', 'reviewed_by', 'reviewed_at', 'created_at', 'updated_at']
    
    def get_member_name(self, obj):
        if obj.is_anonymous:
            return "Anonymous"
        return obj.member.get_full_name()
    
    def get_member_email(self, obj):
        if obj.is_anonymous:
            return None
        return obj.member.email
    
    def get_reviewed_by_name(self, obj):
        if obj.reviewed_by:
            return obj.reviewed_by.get_full_name()
        return None


class SuggestionCreateSerializer(serializers.ModelSerializer):
    class Meta:
        model = Suggestion
        fields = ['title', 'description', 'category', 'is_anonymous']
    
    def create(self, validated_data):
        request = self.context.get('request')
        validated_data['member'] = request.user
        validated_data['church'] = request.user.church
        return super().create(validated_data)


class SuggestionResponseSerializer(serializers.ModelSerializer):
    class Meta:
        model = Suggestion
        fields = ['status', 'response']
    
    def update(self, instance, validated_data):
        request = self.context.get('request')
        instance.status = validated_data.get('status', instance.status)
        instance.response = validated_data.get('response', instance.response)
        instance.reviewed_by = request.user
        from django.utils import timezone
        instance.reviewed_at = timezone.now()
        instance.save()
        return instance
