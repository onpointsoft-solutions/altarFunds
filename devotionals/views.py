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


@method_decorator(csrf_exempt, name='dispatch')
class DevotionalViewSet(viewsets.ModelViewSet):
    """ViewSet for managing devotionals"""
    
    serializer_class = DevotionalSerializer
    permission_classes = [permissions.IsAuthenticated]
    
    def get_queryset(self):
        """Filter devotionals by user's church"""
        user = self.request.user
        if user.church:
            return Devotional.objects.filter(church=user.church, is_published=True)
        return Devotional.objects.none()
    
    def get_serializer_class(self):
        """Use detailed serializer for retrieve action"""
        if self.action == 'retrieve':
            return DevotionalDetailSerializer
        return DevotionalSerializer
    
    def perform_create(self, serializer):
        """Set author and church when creating devotional"""
        serializer.save(
            author=self.request.user,
            church=self.request.user.church
        )
    
    @action(detail=True, methods=['post'])
    def comment(self, request, pk=None):
        """Add a comment to a devotional"""
        devotional = self.get_object()
        serializer = DevotionalCommentSerializer(data=request.data)
        
        if serializer.is_valid():
            serializer.save(
                user=request.user,
                devotional=devotional
            )
            return Response(serializer.data, status=status.HTTP_201_CREATED)
        return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)
    
    @action(detail=True, methods=['post'])
    def react(self, request, pk=None):
        """Add or update a reaction to a devotional"""
        devotional = self.get_object()
        reaction_type = request.data.get('reaction_type')
        
        if not reaction_type:
            return Response(
                {'error': 'reaction_type is required'},
                status=status.HTTP_400_BAD_REQUEST
            )
        
        # Update or create reaction
        reaction, created = DevotionalReaction.objects.update_or_create(
            devotional=devotional,
            user=request.user,
            defaults={'reaction_type': reaction_type}
        )
        
        serializer = DevotionalReactionSerializer(reaction)
        return Response(serializer.data, status=status.HTTP_200_OK)
    
    @action(detail=True, methods=['delete'])
    def unreact(self, request, pk=None):
        """Remove a reaction from a devotional"""
        devotional = self.get_object()
        
        try:
            reaction = DevotionalReaction.objects.get(
                devotional=devotional,
                user=request.user
            )
            reaction.delete()
            return Response(status=status.HTTP_204_NO_CONTENT)
        except DevotionalReaction.DoesNotExist:
            return Response(
                {'error': 'No reaction found'},
                status=status.HTTP_404_NOT_FOUND
            )


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
