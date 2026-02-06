from django.db import models
from accounts.models import User
from churches.models import Church


class Announcement(models.Model):
    """Model for church announcements/notices"""
    
    PRIORITY_CHOICES = [
        ('low', 'Low'),
        ('medium', 'Medium'),
        ('high', 'High'),
        ('urgent', 'Urgent'),
    ]
    
    TARGET_AUDIENCE_CHOICES = [
        ('all', 'All Staff'),
        ('pastor', 'Pastors Only'),
        ('treasurer', 'Treasurers Only'),
        ('admin', 'Admins Only'),
    ]
    
    title = models.CharField(max_length=200)
    content = models.TextField()
    priority = models.CharField(max_length=10, choices=PRIORITY_CHOICES, default='medium')
    target_audience = models.CharField(max_length=20, choices=TARGET_AUDIENCE_CHOICES, default='all')
    church = models.ForeignKey(Church, on_delete=models.CASCADE, related_name='announcements')
    created_by = models.ForeignKey(User, on_delete=models.CASCADE, related_name='created_announcements')
    is_active = models.BooleanField(default=True)
    expires_at = models.DateTimeField(null=True, blank=True, help_text='Optional expiration date')
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)
    
    class Meta:
        ordering = ['-created_at']
        indexes = [
            models.Index(fields=['church', 'is_active']),
            models.Index(fields=['target_audience', 'is_active']),
        ]
    
    def __str__(self):
        return f"{self.title} - {self.church.name}"
    
    def is_expired(self):
        """Check if announcement has expired"""
        if self.expires_at:
            from django.utils import timezone
            return timezone.now() > self.expires_at
        return False
