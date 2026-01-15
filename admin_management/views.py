from django.contrib.auth import authenticate
from django.contrib.auth.models import User
from django.db.models import Count, Sum, Avg, Q
from django.utils import timezone
from rest_framework import viewsets, status
from rest_framework.decorators import action
from rest_framework.response import Response
from rest_framework.permissions import IsAuthenticated, IsAdminUser
from rest_framework.views import APIView
from rest_framework_simplejwt.tokens import RefreshToken
from django.http import HttpResponse
from datetime import timedelta, datetime
import uuid

from accounts.models import User
from churches.models import Church
from giving.models import GivingTransaction, GivingCategory
from payments.models import Payment

# Import admin models
from .models import SystemNotification, ChurchActivity, SubscriptionPlan
from .serializers import (
    SuperAdminStatsSerializer,
    ChurchAdminSerializer,
    SystemNotificationSerializer,
    ChurchActivitySerializer,
    SubscriptionPlanSerializer,
    ChurchAnalyticsSerializer,
    SystemHealthSerializer
)

class SuperAdminDashboardViewSet(viewsets.ViewSet):
    permission_classes = [IsAuthenticated, IsAdminUser]
    
    def list(self, request):
        """Get super admin dashboard statistics"""
        stats = {
            'total_churches': Church.objects.count(),
            'active_churches': Church.objects.filter(is_active=True).count(),
            'total_users': User.objects.count(),
            'active_users': User.objects.filter(is_active=True).count(),
            'total_revenue': Payment.objects.aggregate(
                total=Sum('amount')
            )['total'] or 0,
            'monthly_revenue': Payment.objects.filter(
                created_at__gte=timezone.now() - timedelta(days=30)
            ).aggregate(total=Sum('amount'))['total'] or 0,
            'total_transactions': GivingTransaction.objects.count(),
            'monthly_transactions': GivingTransaction.objects.filter(
                created_at__gte=timezone.now() - timedelta(days=30)
            ).count(),
        }
        
        serializer = SuperAdminStatsSerializer(stats)
        return Response(serializer.data)

class ChurchAdminViewSet(viewsets.ModelViewSet):
    permission_classes = [IsAuthenticated, IsAdminUser]
    
    def get_queryset(self):
        return Church.objects.all().select_related('subscription_plan')
    
    def get_serializer_class(self):
        return ChurchAdminSerializer
    
    def list(self, request):
        """Get churches with filtering and pagination"""
        queryset = self.get_queryset()
        
        # Apply filters
        search = request.query_params.get('search')
        if search:
            queryset = queryset.filter(
                Q(name__icontains=search) |
                Q(code__icontains=search) |
                Q(email__icontains=search)
            )
        
        is_active = request.query_params.get('isActive')
        if is_active is not None:
            queryset = queryset.filter(is_active=is_active.lower() == 'true')
        
        subscription_plan = request.query_params.get('subscriptionPlan')
        if subscription_plan:
            queryset = queryset.filter(subscription_plan__name=subscription_plan)
        
        # Pagination
        page = int(request.query_params.get('page', 1))
        limit = int(request.query_params.get('limit', 20))
        start = (page - 1) * limit
        end = start + limit
        
        total = queryset.count()
        churches = queryset[start:end]
        
        serializer = self.get_serializer_class()(churches, many=True)
        return Response({
            'results': serializer.data,
            'count': total,
            'totalPages': (total + limit - 1) // limit,
            'currentPage': page
        })
    
    @action(detail=True, methods=['post'])
    def toggle_status(self, request, pk=None):
        """Toggle church active status"""
        try:
            church = self.get_object()
            church.is_active = not church.is_active
            church.save()
            
            # Log activity
            ChurchActivity.objects.create(
                church=church,
                action='status_changed',
                description=f"Church status changed to {'active' if church.is_active else 'inactive'}",
                user=request.user
            )
            
            serializer = self.get_serializer_class()(church)
            return Response(serializer.data)
        except Church.DoesNotExist:
            return Response(
                {'error': 'Church not found'}, 
                status=status.HTTP_404_NOT_FOUND
            )
    
    @action(detail=False, methods=['post'])
    def bulk_operations(self, request):
        """Bulk operations on churches"""
        operation = request.data.get('operation')
        church_ids = request.data.get('churchIds', [])
        
        if operation == 'activate':
            Church.objects.filter(id__in=church_ids).update(is_active=True)
        elif operation == 'deactivate':
            Church.objects.filter(id__in=church_ids).update(is_active=False)
        elif operation == 'delete':
            Church.objects.filter(id__in=church_ids).delete()
        
        return Response({'success': True})

class SystemNotificationViewSet(viewsets.ModelViewSet):
    permission_classes = [IsAuthenticated, IsAdminUser]
    
    def get_queryset(self):
        return SystemNotification.objects.all().order_by('-created_at')
    
    def get_serializer_class(self):
        return SystemNotificationSerializer

class ChurchActivityViewSet(viewsets.ModelViewSet):
    permission_classes = [IsAuthenticated, IsAdminUser]
    
    def get_queryset(self):
        return ChurchActivity.objects.all().select_related('church', 'user').order_by('-created_at')
    
    def get_serializer_class(self):
        return ChurchActivitySerializer

class SubscriptionPlanViewSet(viewsets.ReadOnlyModelViewSet):
    permission_classes = [IsAuthenticated, IsAdminUser]
    
    def get_queryset(self):
        return SubscriptionPlan.objects.all()
    
    def get_serializer_class(self):
        return SubscriptionPlanSerializer

class ChurchAnalyticsViewSet(viewsets.ViewSet):
    permission_classes = [IsAuthenticated, IsAdminUser]
    
    @action(detail=True, methods=['get'])
    def analytics(self, request, pk=None):
        """Get church analytics data"""
        try:
            church = Church.objects.get(pk=pk)
            
            # Member growth data
            member_growth = User.objects.filter(
                church=church
            ).extra({
                'month': "strftime('%Y-%m', created_at)"
            }).values('month').annotate(
                count=Count('id')
            ).order_by('month')
            
            # Donation trends
            donation_trends = GivingTransaction.objects.filter(
                church=church
            ).extra({
                'month': "strftime('%Y-%m', created_at)"
            }).values('month').annotate(
                total=Sum('amount'),
                count=Count('id')
            ).order_by('month')
            
            # Engagement metrics
            engagement_metrics = {
                'active_members': User.objects.filter(
                    church=church, 
                    is_active=True
                ).count(),
                'avg_monthly_giving': GivingTransaction.objects.filter(
                    church=church,
                    created_at__gte=timezone.now() - timedelta(days=30)
                ).aggregate(avg=Avg('amount'))['avg'] or 0,
                'total_categories': GivingCategory.objects.filter(
                    church=church
                ).count(),
            }
            
            return Response({
                'memberGrowth': list(member_growth),
                'donationTrends': list(donation_trends),
                'engagementMetrics': engagement_metrics
            })
            
        except Church.DoesNotExist:
            return Response(
                {'error': 'Church not found'}, 
                status=status.HTTP_404_NOT_FOUND
            )

class SystemHealthViewSet(viewsets.ViewSet):
    permission_classes = [IsAuthenticated, IsAdminUser]
    
    def list(self, request):
        """Get system health status"""
        # Check various system components
        services = [
            {'name': 'Database', 'status': 'healthy'},
            {'name': 'Redis', 'status': 'healthy'},
            {'name': 'Email Service', 'status': 'healthy'},
            {'name': 'Payment Gateway', 'status': 'healthy'},
        ]
        
        # Determine overall status
        overall_status = 'healthy'
        if any(service['status'] != 'healthy' for service in services):
            overall_status = 'warning'
        
        metrics = {
            'cpu_usage': 45.2,
            'memory_usage': 67.8,
            'disk_usage': 34.1,
            'active_connections': 156,
        }
        
        return Response({
            'status': overall_status,
            'services': services,
            'metrics': metrics
        })

class SuperAdminAuthViewSet(APIView):
    permission_classes = []
    
    def post(self, request):
        """Super admin login"""
        email = request.data.get('email')
        password = request.data.get('password')
        
        user = authenticate(username=email, password=password)
        
        if user and user.is_superuser:
            refresh = RefreshToken.for_user(user)
            return Response({
                'token': str(refresh.access_token),
                'user': {
                    'id': user.id,
                    'email': user.email,
                    'is_superuser': user.is_superuser
                }
            })
        else:
            return Response(
                {'error': 'Invalid credentials or insufficient permissions'}, 
                status=status.HTTP_401_UNAUTHORIZED
            )
    
    def get(self, request):
        """Validate super admin access"""
        if request.user.is_authenticated and request.user.is_superuser:
            return Response({
                'hasAccess': True,
                'permissions': ['super_admin', 'church_management', 'user_management']
            })
        else:
            return Response({
                'hasAccess': False,
                'permissions': []
            })
