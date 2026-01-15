from django.contrib.auth import authenticate
from django.contrib.auth.models import User
from rest_framework import status
from rest_framework.response import Response
from rest_framework_simplejwt.tokens import RefreshToken

class SuperAdminLoginView:
    def __init__(self):
        pass
    
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

class SuperAdminValidateView:
    def __init__(self):
        pass
    
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
