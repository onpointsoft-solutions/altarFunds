from rest_framework import viewsets, status, permissions
from rest_framework.decorators import action
from rest_framework.response import Response
from django.utils import timezone
from .models import Suggestion
from .serializers import (
    SuggestionSerializer,
    SuggestionCreateSerializer,
    SuggestionResponseSerializer
)


class SuggestionViewSet(viewsets.ModelViewSet):
    """
    ViewSet for managing member suggestions
    Members can create and view their own suggestions
    Pastors can view all suggestions and respond to them
    """
    permission_classes = [permissions.IsAuthenticated]
    
    def get_queryset(self):
        user = self.request.user
        
        # Pastors can see all suggestions for their church
        if user.role == 'pastor':
            return Suggestion.objects.filter(church=user.church)
        
        # Members can only see their own suggestions
        return Suggestion.objects.filter(member=user)
    
    def get_serializer_class(self):
        if self.action == 'create':
            return SuggestionCreateSerializer
        elif self.action == 'respond':
            return SuggestionResponseSerializer
        return SuggestionSerializer
    
    def create(self, request, *args, **kwargs):
        """Members submit suggestions"""
        if request.user.role not in ['member']:
            return Response(
                {'error': 'Only members can submit suggestions'},
                status=status.HTTP_403_FORBIDDEN
            )
        
        if not request.user.church:
            return Response(
                {'error': 'You must be part of a church to submit suggestions'},
                status=status.HTTP_400_BAD_REQUEST
            )
        
        serializer = self.get_serializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        self.perform_create(serializer)
        
        return Response(
            SuggestionSerializer(serializer.instance).data,
            status=status.HTTP_201_CREATED
        )
    
    @action(detail=True, methods=['post'], url_path='respond')
    def respond(self, request, pk=None):
        """Pastor responds to a suggestion"""
        if request.user.role != 'pastor':
            return Response(
                {'error': 'Only pastors can respond to suggestions'},
                status=status.HTTP_403_FORBIDDEN
            )
        
        suggestion = self.get_object()
        serializer = self.get_serializer(suggestion, data=request.data, partial=True)
        serializer.is_valid(raise_exception=True)
        serializer.save()
        
        return Response(SuggestionSerializer(suggestion).data)
    
    @action(detail=False, methods=['get'], url_path='pending')
    def pending(self, request):
        """Get all pending suggestions (for pastors)"""
        if request.user.role != 'pastor':
            return Response(
                {'error': 'Only pastors can view pending suggestions'},
                status=status.HTTP_403_FORBIDDEN
            )
        
        suggestions = self.get_queryset().filter(status='pending')
        serializer = self.get_serializer(suggestions, many=True)
        return Response(serializer.data)
    
    @action(detail=False, methods=['get'], url_path='statistics')
    def statistics(self, request):
        """Get suggestion statistics (for pastors)"""
        if request.user.role != 'pastor':
            return Response(
                {'error': 'Only pastors can view statistics'},
                status=status.HTTP_403_FORBIDDEN
            )
        
        queryset = self.get_queryset()
        stats = {
            'total': queryset.count(),
            'pending': queryset.filter(status='pending').count(),
            'reviewed': queryset.filter(status='reviewed').count(),
            'implemented': queryset.filter(status='implemented').count(),
            'rejected': queryset.filter(status='rejected').count(),
            'by_category': {}
        }
        
        # Count by category
        for choice in Suggestion.CATEGORY_CHOICES:
            category_code = choice[0]
            stats['by_category'][category_code] = queryset.filter(category=category_code).count()
        
        return Response(stats)
