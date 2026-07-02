from rest_framework import viewsets, status, permissions
from rest_framework.decorators import action
from rest_framework.response import Response
from django.utils.decorators import method_decorator
from django.views.decorators.csrf import csrf_exempt
from .models import Devotional, DevotionalComment, DevotionalReaction
from .serializers import (
    DevotionalSerializer,
    DevotionalDetailSerializer,
    DevotionalCommentSerializer,
    DevotionalReactionSerializer
)
from common.permissions import IsChurchAdmin
from notifications.firebase_service import FirebaseNotificationService


@method_decorator(csrf_exempt, name='dispatch')
class DevotionalViewSet(viewsets.ModelViewSet):
    """ViewSet for managing devotionals"""
    
    serializer_class = DevotionalSerializer
    permission_classes = [permissions.IsAuthenticated]
    
    def get_queryset(self):
        """Filter devotionals by user's church, deferring banner_image if not yet migrated."""
        from django.db import connection
        user = self.request.user

        qs = Devotional.objects.none()
        if user.church:
            qs = Devotional.objects.filter(church=user.church, is_published=True)

        # Check whether the banner_image column exists in the DB.
        # If not (migration pending), defer it so the SELECT doesn't fail.
        try:
            columns = [col.name for col in connection.introspection.get_table_description(
                connection.cursor(), 'devotionals'
            )]
            if 'banner_image' not in columns:
                qs = qs.defer('banner_image')
        except Exception:
            pass  # introspection failure — leave queryset as-is

        return qs
    
    def get_serializer_class(self):
        """Use detailed serializer for retrieve action"""
        if self.action == 'retrieve':
            return DevotionalDetailSerializer
        return DevotionalSerializer
    
    def perform_create(self, serializer):
        """Set author and church when creating devotional"""
        devotional = serializer.save(
            author=self.request.user,
            church=self.request.user.church
        )
        
        # Send Firebase push notification to church members
        try:
            FirebaseNotificationService.notify_new_devotional(devotional)
        except Exception as e:
            # Log error but don't fail the creation
            import logging
            logger = logging.getLogger(__name__)
            logger.error(f"Failed to send Firebase notification: {e}")
        
        return devotional
    
    @action(detail=True, methods=['get'])
    def comments(self, request, pk=None):
        """Get all comments for a devotional."""
        devotional = self.get_object()
        comments = DevotionalComment.objects.filter(
            devotional=devotional
        ).order_by('-created_at')
        serializer = DevotionalCommentSerializer(
            comments, many=True, context={'request': request}
        )
        return Response(serializer.data)

    @action(detail=True, methods=['post'])
    def comment(self, request, pk=None):
        """Add a comment to a devotional."""
        devotional = self.get_object()
        serializer = DevotionalCommentSerializer(
            data=request.data, context={'request': request}
        )
        if serializer.is_valid():
            serializer.save(user=request.user, devotional=devotional)
            return Response(serializer.data, status=status.HTTP_201_CREATED)
        return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)

    @action(detail=True, methods=['get'])
    def reactions(self, request, pk=None):
        """Get all reactions for a devotional."""
        devotional = self.get_object()
        reactions = DevotionalReaction.objects.filter(
            devotional=devotional
        ).order_by('-created_at')
        serializer = DevotionalReactionSerializer(
            reactions, many=True, context={'request': request}
        )
        return Response(serializer.data)
    
    @action(detail=True, methods=['post', 'delete'])
    def react(self, request, pk=None):
        """Add, update, or remove a reaction on a devotional."""
        devotional = self.get_object()

        # ── DELETE — remove reaction ──────────────────────────────────────
        if request.method == 'DELETE':
            reaction_type = request.query_params.get('reaction_type')
            qs = DevotionalReaction.objects.filter(
                devotional=devotional, user=request.user
            )
            if reaction_type:
                qs = qs.filter(reaction_type=reaction_type)
            deleted, _ = qs.delete()
            if deleted:
                return Response(status=status.HTTP_204_NO_CONTENT)
            return Response(
                {'error': 'No reaction found'},
                status=status.HTTP_404_NOT_FOUND
            )

        # ── POST — add/update reaction ────────────────────────────────────
        reaction_type = request.data.get('reaction_type')
        if not reaction_type:
            return Response(
                {'error': 'reaction_type is required'},
                status=status.HTTP_400_BAD_REQUEST
            )

        VALID_TYPES = {'like', 'love', 'pray', 'amen', 'thumbs_up', 'fire', 'celebrate'}
        if reaction_type not in VALID_TYPES:
            return Response(
                {'error': f'Invalid reaction_type. Choose from: {", ".join(sorted(VALID_TYPES))}'},
                status=status.HTTP_400_BAD_REQUEST
            )

        reaction, created = DevotionalReaction.objects.update_or_create(
            devotional=devotional,
            user=request.user,
            defaults={'reaction_type': reaction_type},
        )

        # Build the response the Android LikeResponse model expects:
        # { success, message, is_liked, like_count }
        is_liked = reaction_type in ('like', 'love')
        like_count = devotional.reactions.filter(
            reaction_type__in=['like', 'love']
        ).count()

        return Response({
            'success':    True,
            'message':    'Reaction added' if created else 'Reaction updated',
            'is_liked':   is_liked,
            'like_count': like_count,
            # Also return the full reaction object
            'reaction': DevotionalReactionSerializer(reaction, context={'request': request}).data,
        }, status=status.HTTP_200_OK)


@method_decorator(csrf_exempt, name='dispatch')
class DevotionalCommentViewSet(viewsets.ModelViewSet):
    """ViewSet for managing devotional comments"""
    
    serializer_class = DevotionalCommentSerializer
    permission_classes = [permissions.IsAuthenticated]
    
    def get_queryset(self):
        """Filter comments by user's church"""
        user = self.request.user
        if user.church:
            return DevotionalComment.objects.filter(
                devotional__church=user.church
            )
        return DevotionalComment.objects.none()
    
    def perform_create(self, serializer):
        """Set user when creating comment"""
        serializer.save(user=self.request.user)
