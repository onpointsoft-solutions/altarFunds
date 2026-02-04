from django.db import models
from django.utils.translation import gettext_lazy as _
from accounts.models import User
from churches.models import Church


class Devotional(models.Model):
    """Daily devotional model"""
    
    church = models.ForeignKey(
        Church,
        on_delete=models.CASCADE,
        related_name='devotionals',
        verbose_name=_('Church')
    )
    author = models.ForeignKey(
        User,
        on_delete=models.CASCADE,
        related_name='devotionals',
        verbose_name=_('Author')
    )
    title = models.CharField(max_length=255, verbose_name=_('Title'))
    content = models.TextField(verbose_name=_('Content'))
    scripture_reference = models.CharField(
        max_length=255,
        blank=True,
        null=True,
        verbose_name=_('Scripture Reference')
    )
    date = models.DateField(verbose_name=_('Date'))
    is_published = models.BooleanField(default=True, verbose_name=_('Published'))
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)
    
    class Meta:
        db_table = 'devotionals'
        ordering = ['-date', '-created_at']
        verbose_name = _('Devotional')
        verbose_name_plural = _('Devotionals')
    
    def __str__(self):
        return f"{self.title} - {self.date}"


class DevotionalComment(models.Model):
    """Comments on devotionals"""
    
    devotional = models.ForeignKey(
        Devotional,
        on_delete=models.CASCADE,
        related_name='comments',
        verbose_name=_('Devotional')
    )
    user = models.ForeignKey(
        User,
        on_delete=models.CASCADE,
        related_name='devotional_comments',
        verbose_name=_('User')
    )
    content = models.TextField(verbose_name=_('Comment'))
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)
    
    class Meta:
        db_table = 'devotional_comments'
        ordering = ['-created_at']
        verbose_name = _('Devotional Comment')
        verbose_name_plural = _('Devotional Comments')
    
    def __str__(self):
        return f"Comment by {self.user.email} on {self.devotional.title}"


class DevotionalReaction(models.Model):
    """Reactions to devotionals"""
    
    REACTION_CHOICES = [
        ('like', _('Like')),
        ('love', _('Love')),
        ('pray', _('Praying')),
        ('amen', _('Amen')),
    ]
    
    devotional = models.ForeignKey(
        Devotional,
        on_delete=models.CASCADE,
        related_name='reactions',
        verbose_name=_('Devotional')
    )
    user = models.ForeignKey(
        User,
        on_delete=models.CASCADE,
        related_name='devotional_reactions',
        verbose_name=_('User')
    )
    reaction_type = models.CharField(
        max_length=20,
        choices=REACTION_CHOICES,
        verbose_name=_('Reaction Type')
    )
    created_at = models.DateTimeField(auto_now_add=True)
    
    class Meta:
        db_table = 'devotional_reactions'
        unique_together = ['devotional', 'user']
        verbose_name = _('Devotional Reaction')
        verbose_name_plural = _('Devotional Reactions')
    
    def __str__(self):
        return f"{self.user.email} - {self.reaction_type} on {self.devotional.title}"
