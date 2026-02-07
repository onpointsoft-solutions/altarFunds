from django.db import models
from django.utils.translation import gettext_lazy as _
from accounts.models import User
from churches.models import Church


class Suggestion(models.Model):
    """Member suggestions to church leadership"""
    
    STATUS_CHOICES = [
        ('pending', _('Pending')),
        ('reviewed', _('Reviewed')),
        ('implemented', _('Implemented')),
        ('rejected', _('Rejected')),
    ]
    
    CATEGORY_CHOICES = [
        ('general', _('General')),
        ('worship', _('Worship')),
        ('ministry', _('Ministry')),
        ('facilities', _('Facilities')),
        ('events', _('Events')),
        ('finance', _('Finance')),
        ('other', _('Other')),
    ]
    
    church = models.ForeignKey(
        Church,
        on_delete=models.CASCADE,
        related_name='suggestions',
        verbose_name=_('Church')
    )
    member = models.ForeignKey(
        User,
        on_delete=models.CASCADE,
        related_name='suggestions',
        verbose_name=_('Member')
    )
    title = models.CharField(max_length=255, verbose_name=_('Title'))
    description = models.TextField(verbose_name=_('Description'))
    category = models.CharField(
        max_length=50,
        choices=CATEGORY_CHOICES,
        default='general',
        verbose_name=_('Category')
    )
    status = models.CharField(
        max_length=20,
        choices=STATUS_CHOICES,
        default='pending',
        verbose_name=_('Status')
    )
    is_anonymous = models.BooleanField(
        default=False,
        verbose_name=_('Submit Anonymously')
    )
    reviewed_by = models.ForeignKey(
        User,
        on_delete=models.SET_NULL,
        null=True,
        blank=True,
        related_name='reviewed_suggestions',
        verbose_name=_('Reviewed By')
    )
    reviewed_at = models.DateTimeField(
        null=True,
        blank=True,
        verbose_name=_('Reviewed At')
    )
    response = models.TextField(
        blank=True,
        null=True,
        verbose_name=_('Pastor Response')
    )
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)
    
    class Meta:
        db_table = 'suggestions'
        ordering = ['-created_at']
        verbose_name = _('Suggestion')
        verbose_name_plural = _('Suggestions')
    
    def __str__(self):
        return f"{self.title} - {self.member.email if not self.is_anonymous else 'Anonymous'}"
