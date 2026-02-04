from django.db import models
from django.utils.translation import gettext_lazy as _
from accounts.models import User
from churches.models import Church


class Notice(models.Model):
    """Notice board model"""
    
    PRIORITY_CHOICES = [
        ('low', _('Low')),
        ('medium', _('Medium')),
        ('high', _('High')),
    ]
    
    CATEGORY_CHOICES = [
        ('announcement', _('Announcement')),
        ('event', _('Event')),
        ('reminder', _('Reminder')),
        ('urgent', _('Urgent')),
    ]
    
    church = models.ForeignKey(
        Church,
        on_delete=models.CASCADE,
        related_name='notices',
        verbose_name=_('Church')
    )
    author = models.ForeignKey(
        User,
        on_delete=models.CASCADE,
        related_name='notices',
        verbose_name=_('Author')
    )
    title = models.CharField(max_length=255, verbose_name=_('Title'))
    content = models.TextField(verbose_name=_('Content'))
    priority = models.CharField(
        max_length=20,
        choices=PRIORITY_CHOICES,
        default='medium',
        verbose_name=_('Priority')
    )
    category = models.CharField(
        max_length=50,
        choices=CATEGORY_CHOICES,
        default='announcement',
        verbose_name=_('Category')
    )
    date = models.DateField(verbose_name=_('Date'))
    is_published = models.BooleanField(default=True, verbose_name=_('Published'))
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)
    
    class Meta:
        db_table = 'notices'
        ordering = ['-date', '-created_at']
        verbose_name = _('Notice')
        verbose_name_plural = _('Notices')
    
    def __str__(self):
        return f"{self.title} - {self.date}"
