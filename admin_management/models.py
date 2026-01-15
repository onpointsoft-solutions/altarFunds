from django.db import models
from django.contrib.auth import get_user_model

User = get_user_model()

# Import admin models

class SystemNotification(models.Model):
    """System-wide notifications for admin dashboard"""
    
    NOTIFICATION_TYPES = [
        ('info', 'Information'),
        ('warning', 'Warning'),
        ('error', 'Error'),
        ('success', 'Success'),
    ]
    
    title = models.CharField(max_length=200)
    message = models.TextField()
    type = models.CharField(max_length=20, choices=NOTIFICATION_TYPES, default='info')
    is_active = models.BooleanField(default=True)
    created_at = models.DateTimeField(auto_now_add=True)
    
    class Meta:
        ordering = ['-created_at']
        verbose_name = 'System Notification'
        verbose_name_plural = 'System Notifications'
    
    def __str__(self):
        return f"{self.title} - {self.created_at.strftime('%Y-%m-%d')}"

class ChurchActivity(models.Model):
    """Track church-related activities for audit trail"""
    
    ACTION_TYPES = [
        ('created', 'Church Created'),
        ('updated', 'Church Updated'),
        ('status_changed', 'Status Changed'),
        ('subscription_changed', 'Subscription Changed'),
        ('admin_added', 'Admin Added'),
        ('admin_removed', 'Admin Removed'),
    ]
    
    church = models.ForeignKey('churches.Church', on_delete=models.CASCADE, related_name='activities')
    user = models.ForeignKey(User, on_delete=models.CASCADE, related_name='church_activities')
    action = models.CharField(max_length=50, choices=ACTION_TYPES)
    description = models.TextField()
    created_at = models.DateTimeField(auto_now_add=True)
    
    class Meta:
        ordering = ['-created_at']
        verbose_name = 'Church Activity'
        verbose_name_plural = 'Church Activities'
    
    def __str__(self):
        return f"{self.church.name} - {self.action} by {self.user.email}"

class SubscriptionPlan(models.Model):
    """Subscription plans for churches"""
    
    name = models.CharField(max_length=100)
    description = models.TextField(blank=True)
    price = models.DecimalField(max_digits=10, decimal_places=2)
    duration = models.IntegerField(help_text="Duration in months")
    features = models.JSONField(default=dict, help_text="List of features for this plan")
    is_active = models.BooleanField(default=True)
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)
    
    class Meta:
        ordering = ['price']
        verbose_name = 'Subscription Plan'
        verbose_name_plural = 'Subscription Plans'
    
    def __str__(self):
        return f"{self.name} - ${self.price}/{self.duration} months"
