from rest_framework import serializers
from .models import Devotional, DevotionalComment, DevotionalReaction
from accounts.models import User


class DevotionalAuthorSerializer(serializers.ModelSerializer):
    """Serializer for devotional author info"""
    
    class Meta:
        model = User
        fields = ['id', 'first_name', 'last_name', 'email']


class DevotionalCommentSerializer(serializers.ModelSerializer):
    """Serializer for devotional comments"""
    
    user_name = serializers.SerializerMethodField()
    
    class Meta:
        model = DevotionalComment
        fields = ['id', 'devotional', 'user', 'user_name', 'content', 'created_at', 'updated_at']
        read_only_fields = ['user', 'created_at', 'updated_at']
    
    def get_user_name(self, obj):
        return f"{obj.user.first_name} {obj.user.last_name}"


class DevotionalReactionSerializer(serializers.ModelSerializer):
    """Serializer for devotional reactions"""
    
    class Meta:
        model = DevotionalReaction
        fields = ['id', 'devotional', 'user', 'reaction_type', 'created_at']
        read_only_fields = ['user', 'created_at']


class DevotionalSerializer(serializers.ModelSerializer):
    """Serializer for devotionals"""
    
    author = serializers.SerializerMethodField()
    comments_count = serializers.SerializerMethodField()
    reactions_count = serializers.SerializerMethodField()
    user_reaction = serializers.SerializerMethodField()
    
    class Meta:
        model = Devotional
        fields = [
            'id', 'church', 'author', 'title', 'content',
            'scripture_reference', 'date', 'is_published', 'comments_count',
            'reactions_count', 'user_reaction', 'created_at', 'updated_at'
        ]
        read_only_fields = ['church', 'created_at', 'updated_at']
    
    def get_author(self, obj):
        return f"{obj.author.first_name} {obj.author.last_name}" if obj.author.first_name else obj.author.email
    
    def get_comments_count(self, obj):
        return obj.comments.count()
    
    def get_reactions_count(self, obj):
        return obj.reactions.count()
    
    def get_user_reaction(self, obj):
        request = self.context.get('request')
        if request and request.user.is_authenticated:
            reaction = obj.reactions.filter(user=request.user).first()
            if reaction:
                return reaction.reaction_type
        return None


class DevotionalDetailSerializer(DevotionalSerializer):
    """Detailed serializer for devotionals with comments and reactions"""
    
    comments = DevotionalCommentSerializer(many=True, read_only=True)
    reactions = DevotionalReactionSerializer(many=True, read_only=True)
    
    class Meta(DevotionalSerializer.Meta):
        fields = DevotionalSerializer.Meta.fields + ['comments', 'reactions']
