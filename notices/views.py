from rest_framework import viewsets, permissions
from django.utils.decorators import method_decorator
from django.views.decorators.csrf import csrf_exempt
from .models import Notice
from .serializers import NoticeSerializer


@method_decorator(csrf_exempt, name='dispatch')
class NoticeViewSet(viewsets.ModelViewSet):
    """ViewSet for managing notices"""
    
    serializer_class = NoticeSerializer
    permission_classes = [permissions.IsAuthenticated]
    
    def get_queryset(self):
        """Filter notices by user's church"""
        user = self.request.user
        
        # System admins can see all notices
        if user.role == 'system_admin':
            return Notice.objects.filter(is_published=True)
        
        # Other users see notices from their church
        if user.church:
            return Notice.objects.filter(church=user.church, is_published=True)
        return Notice.objects.none()
    
    def perform_create(self, serializer):
        """Set author and church when creating notice"""
        user = self.request.user
        
        # Ensure user has a church before creating notice
        if not user.church:
            from rest_framework.exceptions import ValidationError
            raise ValidationError("You must be assigned to a church to create notices")
        
        serializer.save(
            author=user,
            church=user.church
        )
