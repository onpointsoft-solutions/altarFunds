from rest_framework import serializers
from .models import Devotional, DevotionalComment, DevotionalReaction
from accounts.models import User


class DevotionalAuthorSerializer(serializers.ModelSerializer):
    class Meta:
        model = User
        fields = ['id', 'first_name', 'last_name', 'email']


class DevotionalCommentSerializer(serializers.ModelSerializer):
    user_name = serializers.SerializerMethodField()
    user_avatar = serializers.SerializerMethodField()

    class Meta:
        model = DevotionalComment
        fields = [
            'id', 'devotional', 'user', 'user_name', 'user_avatar',
            'content', 'created_at', 'updated_at',
        ]
        read_only_fields = ['user', 'created_at', 'updated_at']

    def get_user_name(self, obj):
        name = f"{obj.user.first_name} {obj.user.last_name}".strip()
        return name or obj.user.email

    def get_user_avatar(self, obj):
        """Return profile picture URL if available."""
        try:
            pic = obj.user.profile_picture
            if pic:
                request = self.context.get('request')
                if request:
                    return request.build_absolute_uri(pic.url)
                return pic.url
        except Exception:
            pass
        return None


class DevotionalReactionSerializer(serializers.ModelSerializer):
    user_name = serializers.SerializerMethodField()
    user_avatar = serializers.SerializerMethodField()
    emoji = serializers.SerializerMethodField()

    class Meta:
        model = DevotionalReaction
        fields = [
            'id', 'devotional', 'user', 'user_name', 'user_avatar',
            'reaction_type', 'emoji', 'created_at',
        ]
        read_only_fields = ['user', 'created_at']

    def get_user_name(self, obj):
        name = f"{obj.user.first_name} {obj.user.last_name}".strip()
        return name or obj.user.email

    def get_user_avatar(self, obj):
        try:
            pic = obj.user.profile_picture
            if pic:
                request = self.context.get('request')
                if request:
                    return request.build_absolute_uri(pic.url)
                return pic.url
        except Exception:
            pass
        return None

    def get_emoji(self, obj):
        mapping = {
            'love':      '\u2764\ufe0f',
            'pray':      '\U0001f64f',
            'thumbs_up': '\U0001f44d',
            'fire':      '\U0001f525',
            'celebrate': '\U0001f389',
            'like':      '\U0001f44d',
            'amen':      '\U0001f64f',
        }
        return mapping.get(obj.reaction_type, '\u2764\ufe0f')


class DevotionalSerializer(serializers.ModelSerializer):
    """
    Full serializer that matches every field the Android Devotional data class expects:
        id, title, content, scripture_reference, author (string), date,
        banner_image, like_count, comment_count, reactions_count,
        user_reaction, is_bookmarked, is_liked, created_at
    """
    author       = serializers.SerializerMethodField()
    like_count   = serializers.SerializerMethodField()
    comment_count = serializers.SerializerMethodField()
    reactions_count = serializers.SerializerMethodField()
    user_reaction = serializers.SerializerMethodField()
    is_liked     = serializers.SerializerMethodField()
    is_bookmarked = serializers.SerializerMethodField()   # placeholder — no bookmark model yet
    banner_image  = serializers.SerializerMethodField()   # safe read — column may not exist yet

    class Meta:
        model = Devotional
        fields = [
            'id', 'church', 'author', 'title', 'content',
            'scripture_reference', 'date', 'banner_image', 'is_published',
            'like_count', 'comment_count', 'reactions_count',
            'user_reaction', 'is_liked', 'is_bookmarked',
            'created_at', 'updated_at',
        ]
        read_only_fields = ['church', 'created_at', 'updated_at']

    def get_author(self, obj):
        if obj.author:
            name = f"{obj.author.first_name} {obj.author.last_name}".strip()
            return name or obj.author.email
        return 'Unknown'

    def get_banner_image(self, obj):
        """Safe read — returns None if the column doesn't exist yet in the DB."""
        try:
            return getattr(obj, 'banner_image', None)
        except Exception:
            return None

    def get_like_count(self, obj):
        return obj.reactions.filter(reaction_type__in=['like', 'love']).count()

    def get_comment_count(self, obj):
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

    def get_is_liked(self, obj):
        request = self.context.get('request')
        if request and request.user.is_authenticated:
            return obj.reactions.filter(
                user=request.user,
                reaction_type__in=['like', 'love']
            ).exists()
        return False

    def get_is_bookmarked(self, obj):
        # Placeholder — return False until a bookmark model is added
        return False


class DevotionalDetailSerializer(DevotionalSerializer):
    """Detail view that embeds full comments + reactions lists."""
    comments  = DevotionalCommentSerializer(many=True, read_only=True)
    reactions = DevotionalReactionSerializer(many=True, read_only=True)

    class Meta(DevotionalSerializer.Meta):
        fields = DevotionalSerializer.Meta.fields + ['comments', 'reactions']
