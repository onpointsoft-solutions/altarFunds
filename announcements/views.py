from rest_framework import viewsets, permissions, status
from rest_framework.decorators import action
from rest_framework.response import Response
from django.utils.decorators import method_decorator
from django.views.decorators.csrf import csrf_exempt
from django.utils import timezone
from .models import Announcement
from .serializers import AnnouncementSerializer, AnnouncementCreateSerializer
from common.permissions import IsChurchAdmin


@method_decorator(csrf_exempt, name='dispatch')
class AnnouncementViewSet(viewsets.ModelViewSet):
    """ViewSet for managing announcements"""
    
    permission_classes = [permissions.IsAuthenticated]
    
    def get_queryset(self):
        """Filter announcements based on user role"""
        user = self.request.user
        
        if not user.church:
            return Announcement.objects.none()
        
        # Base queryset - active announcements from user's church
        queryset = Announcement.objects.filter(
            church=user.church,
            is_active=True
        )
        
        # Filter by target audience based on user role
        if user.role == 'pastor':
            queryset = queryset.filter(target_audience__in=['all', 'pastor'])
        elif user.role == 'treasurer':
            queryset = queryset.filter(target_audience__in=['all', 'treasurer'])
        elif user.role in ['admin', 'denomination_admin', 'system_admin']:
            # Admins see all announcements
            pass
        else:
            # Other roles see only 'all' announcements
            queryset = queryset.filter(target_audience='all')
        
        # Exclude expired announcements
        queryset = queryset.exclude(
            expires_at__isnull=False,
            expires_at__lt=timezone.now()
        )
        
        return queryset
    
    def get_serializer_class(self):
        """Use different serializers for create/update vs list/retrieve"""
        if self.action in ['create', 'update', 'partial_update']:
            return AnnouncementCreateSerializer
        return AnnouncementSerializer
    
    def get_permissions(self):
        """Only admins can create, update, or delete announcements"""
        if self.action in ['create', 'update', 'partial_update', 'destroy']:
            return [IsChurchAdmin()]
        return super().get_permissions()
    
    def perform_create(self, serializer):
        """Set created_by and church when creating announcement"""
        serializer.save(
            created_by=self.request.user,
            church=self.request.user.church
        )
    
    @action(detail=True, methods=['post'], permission_classes=[IsChurchAdmin])
    def deactivate(self, request, pk=None):
        """Deactivate an announcement"""
        announcement = self.get_object()
        announcement.is_active = False
        announcement.save()
        
        return Response({
            'message': 'Announcement deactivated successfully'
        }, status=status.HTTP_200_OK)
    
    @action(detail=True, methods=['post'], permission_classes=[IsChurchAdmin])
    def activate(self, request, pk=None):
        """Activate an announcement"""
        announcement = self.get_object()
        announcement.is_active = True
        announcement.save()
        
        return Response({
            'message': 'Announcement activated successfully'
        }, status=status.HTTP_200_OK)
