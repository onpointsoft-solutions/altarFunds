from rest_framework import status, generics, permissions
from rest_framework.decorators import api_view, permission_classes
from rest_framework.response import Response
from rest_framework_simplejwt.tokens import RefreshToken
from django.contrib.auth import login, logout
from django.shortcuts import render, redirect
from django.contrib.auth.decorators import login_required
from django.utils import timezone
from merchants.models import Merchant, ApiKey
from payments.models import AuditLog
from .serializers import (
    MerchantRegistrationSerializer,
    MerchantLoginSerializer,
    MerchantProfileSerializer,
    ApiKeySerializer
)


def login_view(request):
    """Render merchant login page"""
    if request.user.is_authenticated:
        return redirect('/api/v1/dashboard/')
    
    return render(request, 'login.html')


def register_view(request):
    """Render merchant registration page"""
    if request.user.is_authenticated:
        return redirect('/api/v1/dashboard/')
    
    return render(request, 'register.html')


def home_view(request):
    """Render home page"""
    if request.user.is_authenticated:
        return redirect('/api/v1/dashboard/')
    
    return render(request, 'home.html')


class MerchantRegistrationView(generics.CreateAPIView):
    """Register a new merchant"""
    queryset = Merchant.objects.all()
    serializer_class = MerchantRegistrationSerializer
    permission_classes = [permissions.AllowAny]
    
    def create(self, request, *args, **kwargs):
        serializer = self.get_serializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        merchant = serializer.save()
        
        # Log the registration
        AuditLog.objects.create(
            merchant=merchant,
            action='merchant_registered',
            resource_type='merchant',
            resource_id=str(merchant.id),
            ip_address=self.get_client_ip(request),
            user_agent=request.META.get('HTTP_USER_AGENT', ''),
            success=True,
            new_values={'business_name': merchant.business_name, 'email': merchant.email}
        )
        
        # Generate tokens
        refresh = RefreshToken.for_user(merchant)
        
        return Response({
            'message': 'Merchant registered successfully',
            'merchant': MerchantProfileSerializer(merchant).data,
            'tokens': {
                'refresh': str(refresh),
                'access': str(refresh.access_token),
            }
        }, status=status.HTTP_201_CREATED)
    
    def get_client_ip(self, request):
        x_forwarded_for = request.META.get('HTTP_X_FORWARDED_FOR')
        if x_forwarded_for:
            ip = x_forwarded_for.split(',')[0]
        else:
            ip = request.META.get('REMOTE_ADDR')
        return ip


class MerchantLoginView(generics.GenericAPIView):
    """Login endpoint for merchants"""
    serializer_class = MerchantLoginSerializer
    permission_classes = [permissions.AllowAny]
    
    def post(self, request, *args, **kwargs):
        serializer = self.get_serializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        
        merchant = serializer.validated_data['merchant']
        login(request, merchant)
        
        # Update last login
        merchant.last_login_at = timezone.now()
        merchant.save(update_fields=['last_login_at'])
        
        # Log the login
        AuditLog.objects.create(
            merchant=merchant,
            action='merchant_login',
            resource_type='merchant',
            resource_id=str(merchant.id),
            ip_address=self.get_client_ip(request),
            user_agent=request.META.get('HTTP_USER_AGENT', ''),
            success=True,
        )
        
        # Generate tokens
        refresh = RefreshToken.for_user(merchant)
        
        return Response({
            'message': 'Login successful',
            'merchant': MerchantProfileSerializer(merchant).data,
            'tokens': {
                'refresh': str(refresh),
                'access': str(refresh.access_token),
            }
        })
    
    def get_client_ip(self, request):
        x_forwarded_for = request.META.get('HTTP_X_FORWARDED_FOR')
        if x_forwarded_for:
            ip = x_forwarded_for.split(',')[0]
        else:
            ip = request.META.get('REMOTE_ADDR')
        return ip


class MerchantProfileView(generics.RetrieveUpdateAPIView):
    """Get and update merchant profile"""
    serializer_class = MerchantProfileSerializer
    permission_classes = [permissions.IsAuthenticated]
    
    def get_object(self):
        return self.request.user
    
    def update(self, request, *args, **kwargs):
        partial = kwargs.pop('partial', False)
        instance = self.get_object()
        serializer = self.get_serializer(instance, data=request.data, partial=partial)
        serializer.is_valid(raise_exception=True)
        
        # Store old values for audit
        old_values = {
            'business_name': instance.business_name,
            'business_phone': instance.business_phone,
            'business_address': instance.business_address,
        }
        
        merchant = serializer.save()
        
        # Log the profile update
        AuditLog.objects.create(
            merchant=merchant,
            action='profile_updated',
            resource_type='merchant',
            resource_id=str(merchant.id),
            ip_address=self.get_client_ip(request),
            user_agent=request.META.get('HTTP_USER_AGENT', ''),
            success=True,
            old_values=old_values,
            new_values=serializer.validated_data
        )
        
        return Response({
            'message': 'Profile updated successfully',
            'merchant': serializer.data
        })
    
    def get_client_ip(self, request):
        x_forwarded_for = request.META.get('HTTP_X_FORWARDED_FOR')
        if x_forwarded_for:
            ip = x_forwarded_for.split(',')[0]
        else:
            ip = request.META.get('REMOTE_ADDR')
        return ip


class ApiKeyListView(generics.ListCreateAPIView):
    """List and create API keys for merchant"""
    serializer_class = ApiKeySerializer
    permission_classes = [permissions.IsAuthenticated]
    
    def get_queryset(self):
        return ApiKey.objects.filter(merchant=self.request.user)
    
    def perform_create(self, serializer):
        api_key = serializer.save(merchant=self.request.user)
        
        # Log API key creation
        AuditLog.objects.create(
            merchant=self.request.user,
            action='api_key_created',
            resource_type='api_key',
            resource_id=str(api_key.id),
            ip_address=self.get_client_ip(self.request),
            user_agent=self.request.META.get('HTTP_USER_AGENT', ''),
            success=True,
            new_values={'name': api_key.name, 'key_type': api_key.key_type}
        )
    
    def get_client_ip(self, request):
        x_forwarded_for = request.META.get('HTTP_X_FORWARDED_FOR')
        if x_forwarded_for:
            ip = x_forwarded_for.split(',')[0]
        else:
            ip = request.META.get('REMOTE_ADDR')
        return ip


class ApiKeyDetailView(generics.RetrieveUpdateDestroyAPIView):
    """Retrieve, update and delete API key"""
    serializer_class = ApiKeySerializer
    permission_classes = [permissions.IsAuthenticated]
    
    def get_queryset(self):
        return ApiKey.objects.filter(merchant=self.request.user)
    
    def perform_destroy(self, instance):
        # Log API key deletion
        AuditLog.objects.create(
            merchant=self.request.user,
            action='api_key_deleted',
            resource_type='api_key',
            resource_id=str(instance.id),
            ip_address=self.get_client_ip(self.request),
            user_agent=self.request.META.get('HTTP_USER_AGENT', ''),
            success=True,
            old_values={'name': instance.name, 'key_type': instance.key_type}
        )
        super().perform_destroy(instance)
    
    def get_client_ip(self, request):
        x_forwarded_for = request.META.get('HTTP_X_FORWARDED_FOR')
        if x_forwarded_for:
            ip = x_forwarded_for.split(',')[0]
        else:
            ip = request.META.get('REMOTE_ADDR')
        return ip


@api_view(['POST'])
@permission_classes([permissions.IsAuthenticated])
def logout_view(request):
    """Logout endpoint"""
    try:
        refresh_token = request.data.get('refresh_token')
        if refresh_token:
            token = RefreshToken(refresh_token)
            token.blacklist()
        
        # Log the logout
        AuditLog.objects.create(
            merchant=request.user,
            action='merchant_logout',
            resource_type='merchant',
            resource_id=str(request.user.id),
            ip_address=get_client_ip(request),
            user_agent=request.META.get('HTTP_USER_AGENT', ''),
            success=True,
        )
        
        return Response({'message': 'Logout successful'})
    except Exception as e:
        return Response(
            {'error': 'Invalid token'}, 
            status=status.HTTP_400_BAD_REQUEST
        )


def get_client_ip(request):
    """Helper function to get client IP"""
    x_forwarded_for = request.META.get('HTTP_X_FORWARDED_FOR')
    if x_forwarded_for:
        ip = x_forwarded_for.split(',')[0]
    else:
        ip = request.META.get('REMOTE_ADDR')
    return ip
