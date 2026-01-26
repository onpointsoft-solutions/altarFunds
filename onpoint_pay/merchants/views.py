from django.shortcuts import render
from rest_framework import generics, permissions
from rest_framework.response import Response
from authentication.serializers import MerchantRegistrationSerializer, MerchantLoginSerializer, MerchantProfileSerializer
from django.contrib.auth import login as auth_login
from django.contrib.auth.models import AnonymousUser
from .models import Merchant


class MerchantRegistrationView(generics.CreateAPIView):
    """Register a new merchant"""
    permission_classes = [permissions.AllowAny]
    serializer_class = MerchantRegistrationSerializer
    
    def post(self, request, *args, **kwargs):
        serializer = self.get_serializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        merchant = serializer.save()
        
        return Response({
            'message': 'Merchant registered successfully',
            'merchant': {
                'id': str(merchant.id),
                'business_name': merchant.business_name,
                'email': merchant.email,
                'is_verified': merchant.is_verified,
            }
        }, status=201)


class MerchantLoginView(generics.GenericAPIView):
    """Login endpoint for merchants"""
    permission_classes = [permissions.AllowAny]
    serializer_class = MerchantLoginSerializer
    
    def post(self, request, *args, **kwargs):
        serializer = self.get_serializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        
        user = serializer.validated_data['user']
        auth_login(request, user)
        
        return Response({
            'message': 'Login successful',
            'merchant': {
                'id': str(user.id),
                'business_name': user.business_name,
                'email': user.email,
                'is_verified': user.is_verified,
            },
            'tokens': {
                'access': str(user.auth_token.access_token),
                'refresh': str(user.auth_token.refresh_token),
            }
        })


class MerchantProfileView(generics.RetrieveUpdateAPIView):
    """Get and update merchant profile"""
    permission_classes = [permissions.IsAuthenticated]
    
    def get_object(self):
        return self.request.user
    
    def update(self, request, *args, **kwargs):
        merchant = self.get_object()
        serializer = MerchantProfileSerializer(
            merchant,
            data=request.data,
            partial=True
        )
        serializer.is_valid(raise_exception=True)
        serializer.save()
        
        return Response({
            'message': 'Profile updated successfully',
            'merchant': serializer.data
        })


class ApiKeyListView(generics.ListCreateAPIView):
    """List and create API keys"""
    permission_classes = [permissions.IsAuthenticated]
    
    def get_queryset(self):
        return self.request.user.api_keys.all()
    
    def perform_create(self, serializer):
        serializer.save(owner=self.request.user)
        return serializer


class ApiKeyDetailView(generics.RetrieveUpdateDestroyAPIView):
    """Retrieve, update, and delete API keys"""
    permission_classes = [permissions.IsAuthenticated]
    
    def get_queryset(self):
        return self.request.user.api_keys.all()
