from rest_framework import viewsets, status, permissions
from rest_framework.decorators import action
from rest_framework.response import Response
from django.utils import timezone
import logging
from .models import ChurchJoinRequest
from .serializers import ChurchJoinRequestSerializer

logger = logging.getLogger(__name__)


class ChurchJoinRequestViewSet(viewsets.ModelViewSet):
    """
    ViewSet for managing church join requests
    Members can create join requests
    Pastors can view and approve/reject requests for their church
    """
    permission_classes = [permissions.IsAuthenticated]
    serializer_class = ChurchJoinRequestSerializer
    
    def get_queryset(self):
        user = self.request.user
        
        try:
            # Pastors and admins can see all requests for their church
            if user.role in ['pastor', 'denomination_admin']:
                if user.church:
                    return ChurchJoinRequest.objects.filter(church=user.church).select_related('user', 'church', 'reviewed_by')
                return ChurchJoinRequest.objects.none()
            
            # Members can only see their own requests
            return ChurchJoinRequest.objects.filter(user=user).select_related('user', 'church', 'reviewed_by')
        except Exception as e:
            logger.error(f"Error in get_queryset: {str(e)}")
            return ChurchJoinRequest.objects.none()
    
    def list(self, request, *args, **kwargs):
        """List all join requests with error handling"""
        try:
            queryset = self.get_queryset()
            serializer = self.get_serializer(queryset, many=True)
            return Response({'results': serializer.data})
        except Exception as e:
            logger.error(f"Error listing join requests: {str(e)}")
            return Response(
                {'error': f'Failed to load join requests: {str(e)}'},
                status=status.HTTP_500_INTERNAL_SERVER_ERROR
            )
    
    @action(detail=True, methods=['post'], url_path='approve')
    def approve(self, request, pk=None):
        """Approve a join request"""
        if request.user.role not in ['pastor', 'denomination_admin']:
            return Response(
                {'error': 'Only pastors and admins can approve join requests'},
                status=status.HTTP_403_FORBIDDEN
            )
        
        join_request = self.get_object()
        
        # Update request status
        join_request.status = 'approved'
        join_request.reviewed_by = request.user
        join_request.reviewed_at = timezone.now()
        join_request.save()
        
        # Assign user to church
        user = join_request.user
        user.church = join_request.church
        user.save()
        
        # Create member profile if it doesn't exist
        from .models import Member
        Member.objects.get_or_create(
            user=user,
            defaults={'church': join_request.church}
        )
        
        return Response({
            'message': 'Join request approved successfully',
            'request': ChurchJoinRequestSerializer(join_request).data
        })
    
    @action(detail=True, methods=['post'], url_path='reject')
    def reject(self, request, pk=None):
        """Reject a join request"""
        if request.user.role not in ['pastor', 'denomination_admin']:
            return Response(
                {'error': 'Only pastors and admins can reject join requests'},
                status=status.HTTP_403_FORBIDDEN
            )
        
        join_request = self.get_object()
        rejection_reason = request.data.get('rejection_reason', '')
        
        if not rejection_reason:
            return Response(
                {'error': 'Rejection reason is required'},
                status=status.HTTP_400_BAD_REQUEST
            )
        
        # Update request status
        join_request.status = 'rejected'
        join_request.reviewed_by = request.user
        join_request.reviewed_at = timezone.now()
        join_request.rejection_reason = rejection_reason
        join_request.save()
        
        return Response({
            'message': 'Join request rejected',
            'request': ChurchJoinRequestSerializer(join_request).data
        })
    
    @action(detail=False, methods=['get'], url_path='pending')
    def pending(self, request):
        """Get all pending join requests"""
        if request.user.role not in ['pastor', 'denomination_admin']:
            return Response(
                {'error': 'Only pastors and admins can view pending requests'},
                status=status.HTTP_403_FORBIDDEN
            )
        
        requests = self.get_queryset().filter(status='pending')
        serializer = self.get_serializer(requests, many=True)
        return Response({'results': serializer.data})
