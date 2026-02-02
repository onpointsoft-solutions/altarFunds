from rest_framework import permissions
from django.contrib.auth.models import AnonymousUser


class IsOwnerOrReadOnly(permissions.BasePermission):
    """Allow read-only access to anyone, but write access only to owner"""
    
    def has_object_permission(self, request, view, obj):
        if request.method in permissions.SAFE_METHODS:
            return True
        
        # Check if object has an owner field
        if hasattr(obj, 'user'):
            return obj.user == request.user
        elif hasattr(obj, 'created_by'):
            return obj.created_by == request.user
        elif hasattr(obj, 'member'):
            return obj.member.user == request.user
        
        return False


class IsChurchAdmin(permissions.BasePermission):
    """Allow access only to church administrators"""
    
    def has_permission(self, request, view):
        if not request.user or isinstance(request.user, AnonymousUser):
            return False
        
        return request.user.role in ['admin', 'pastor', 'treasurer', 'auditor', 'denomination_admin', 'system_admin']


class IsDenominationAdmin(permissions.BasePermission):
    """Allow access only to denomination administrators"""
    
    def has_permission(self, request, view):
        if not request.user or isinstance(request.user, AnonymousUser):
            return False
        
        return request.user.role in ['denomination_admin', 'system_admin']


class IsSystemAdmin(permissions.BasePermission):
    """Allow access only to system administrators"""
    
    def has_permission(self, request, view):
        if not request.user or isinstance(request.user, AnonymousUser):
            return False
        
        return request.user.role == 'system_admin'


class IsMemberOfChurch(permissions.BasePermission):
    """Allow access only to members of the same church"""
    
    def has_object_permission(self, request, view, obj):
        if not request.user or isinstance(request.user, AnonymousUser):
            return False
        
        # Get user's church
        user_church = getattr(request.user, 'church', None)
        if not user_church:
            return False
        
        # Check if object belongs to the same church
        if hasattr(obj, 'church'):
            return obj.church == user_church
        elif hasattr(obj, 'member') and hasattr(obj.member, 'church'):
            return obj.member.church == user_church
        elif hasattr(obj, 'campus') and hasattr(obj.campus, 'church'):
            return obj.campus.church == user_church
        
        return False


class CanViewChurchFinances(permissions.BasePermission):
    """Allow access to users who can view church finances"""
    
    def has_permission(self, request, view):
        if not request.user or isinstance(request.user, AnonymousUser):
            return False
        
        return request.user.role in ['admin', 'pastor', 'treasurer', 'auditor', 'denomination_admin', 'system_admin']


class CanManageChurchFinances(permissions.BasePermission):
    """Allow access to users who can manage church finances"""
    
    def has_permission(self, request, view):
        if not request.user or isinstance(request.user, AnonymousUser):
            return False
        
        return request.user.role in ['admin', 'treasurer', 'denomination_admin', 'system_admin']


class CanApproveExpenses(permissions.BasePermission):
    """Allow access to users who can approve expenses"""
    
    def has_permission(self, request, view):
        if not request.user or isinstance(request.user, AnonymousUser):
            return False
        
        return request.user.role in ['admin', 'pastor', 'denomination_admin', 'system_admin']


class IsMember(permissions.BasePermission):
    """Allow access to authenticated members"""
    
    def has_permission(self, request, view):
        return request.user and request.user.is_authenticated


class CanApproveChurches(permissions.BasePermission):
    """Allow access to users who can approve church registrations"""
    
    def has_permission(self, request, view):
        if not request.user or isinstance(request.user, AnonymousUser):
            return False
        
        return request.user.role == 'system_admin' or request.user.is_superuser


class CanManageChurch(permissions.BasePermission):
    """Allow access to users who can manage their church"""
    
    def has_permission(self, request, view):
        if not request.user or isinstance(request.user, AnonymousUser):
            return False
        
        return request.user.role in ['admin', 'pastor', 'treasurer', 'auditor', 'denomination_admin', 'system_admin']
    
    def has_object_permission(self, request, view, obj):
        # System admins can manage any church
        if request.user.role == 'system_admin':
            return True
        
        # Church admins can only manage their own church
        if hasattr(obj, 'church'):
            return obj.church == request.user.church
        
        # For church objects themselves
        return obj == request.user.church


class CanViewPayments(permissions.BasePermission):
    """Allow access to users who can view payment records"""
    
    def has_permission(self, request, view):
        if not request.user or isinstance(request.user, AnonymousUser):
            return False
        
        # Members can view their own payments
        # Church admins can view church payments
        # System admins can view all payments
        return request.user.is_authenticated
    
    def has_object_permission(self, request, view, obj):
        # System admins can view all
        if request.user.role == 'system_admin':
            return True
        
        # Church admins can view their church's payments
        if request.user.role in ['admin', 'pastor', 'treasurer', 'auditor']:
            if hasattr(obj, 'church'):
                return obj.church == request.user.church
            elif hasattr(obj, 'giving') and hasattr(obj.giving, 'church'):
                return obj.giving.church == request.user.church
        
        # Members can view their own payments
        if hasattr(obj, 'user'):
            return obj.user == request.user
        elif hasattr(obj, 'giving') and hasattr(obj.giving, 'member'):
            return obj.giving.member.user == request.user
        
        return False


class CanManageMembers(permissions.BasePermission):
    """Allow access to users who can manage church members"""
    
    def has_permission(self, request, view):
        if not request.user or isinstance(request.user, AnonymousUser):
            return False
        
        return request.user.role in ['admin', 'pastor', 'treasurer', 'auditor', 'denomination_admin', 'system_admin']
    
    def has_object_permission(self, request, view, obj):
        # System admins can manage all members
        if request.user.role == 'system_admin':
            return True
        
        # Church admins can only manage members of their church
        if hasattr(obj, 'church'):
            return obj.church == request.user.church
        elif hasattr(obj, 'user') and hasattr(obj.user, 'church'):
            return obj.user.church == request.user.church
        
        return False


class IsOwnerOrChurchAdmin(permissions.BasePermission):
    """Allow access to owner or church administrators"""
    
    def has_object_permission(self, request, view, obj):
        # System admins have full access
        if request.user.role == 'system_admin':
            return True
        
        # Church admins can access church data
        if request.user.role in ['admin', 'pastor', 'treasurer', 'auditor']:
            if hasattr(obj, 'church'):
                return obj.church == request.user.church
            elif hasattr(obj, 'member') and hasattr(obj.member, 'church'):
                return obj.member.church == request.user.church
        
        # Owners can access their own data
        if hasattr(obj, 'user'):
            return obj.user == request.user
        elif hasattr(obj, 'member'):
            return obj.member.user == request.user
        elif hasattr(obj, 'created_by'):
            return obj.created_by == request.user
        
        return False
