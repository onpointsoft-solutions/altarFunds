from rest_framework import permissions


class IsUsher(permissions.BasePermission):
    """
    Custom permission to only allow ushers to access attendance endpoints.
    """
    
    def has_permission(self, request, view):
        # Check if user is authenticated
        if not request.user or not request.user.is_authenticated:
            return False
        
        # Check if user has usher role or higher privileges
        return (
            request.user.role in ['usher', 'admin', 'pastor', 'treasurer', 'auditor', 
                                 'denomination_admin', 'system_admin'] or
            request.user.is_superuser
        )
    
    def has_object_permission(self, request, view, obj):
        # Check if user can access the specific object
        if not request.user or not request.user.is_authenticated:
            return False
        
        # Superusers can access everything
        if request.user.is_superuser:
            return True
        
        # Check if user belongs to the same church
        if hasattr(obj, 'church'):
            return obj.church == request.user.church
        
        # For attendance records, check through the church relationship
        if hasattr(obj, 'attendance_record'):
            return obj.attendance_record.church == request.user.church
        
        return False


class CanManageAttendance(permissions.BasePermission):
    """
    Custom permission for users who can manage attendance (admin, pastor, usher).
    """
    
    def has_permission(self, request, view):
        if not request.user or not request.user.is_authenticated:
            return False
        
        return (
            request.user.role in ['usher', 'admin', 'pastor', 'treasurer', 'auditor'] or
            request.user.is_superuser
        )
    
    def has_object_permission(self, request, view, obj):
        if not request.user or not request.user.is_authenticated:
            return False
        
        if request.user.is_superuser:
            return True
        
        # Check church membership
        if hasattr(obj, 'church'):
            return obj.church == request.user.church
        
        if hasattr(obj, 'attendance_record'):
            return obj.attendance_record.church == request.user.church
        
        return False
