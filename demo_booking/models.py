from django.db import models
from django.utils import timezone


class DemoBooking(models.Model):
    DEMO_TYPE_CHOICES = [
        ('online',   'Online Demo'),
        ('inperson', 'In-Person Visit'),
        ('phone',    'Phone Call'),
    ]
    CHURCH_SIZE_CHOICES = [
        ('small',  'Small (1-50)'),
        ('medium', 'Medium (51-200)'),
        ('large',  'Large (201-500)'),
        ('xlarge', 'Extra Large (500+)'),
    ]
    STATUS_CHOICES = [
        ('pending',   'Pending'),
        ('confirmed', 'Confirmed'),
        ('completed', 'Completed'),
        ('cancelled', 'Cancelled'),
    ]

    first_name    = models.CharField(max_length=100)
    last_name     = models.CharField(max_length=100)
    email         = models.EmailField()
    phone_number  = models.CharField(max_length=30)
    church_name   = models.CharField(max_length=200)
    church_size   = models.CharField(max_length=20, choices=CHURCH_SIZE_CHOICES, blank=True)
    demo_date     = models.DateField(null=True, blank=True)
    demo_time     = models.TimeField(null=True, blank=True)
    demo_type     = models.CharField(max_length=20, choices=DEMO_TYPE_CHOICES, default='online')
    specific_needs= models.TextField(blank=True)
    status        = models.CharField(max_length=20, choices=STATUS_CHOICES, default='pending')
    created_at    = models.DateTimeField(auto_now_add=True)
    updated_at    = models.DateTimeField(auto_now=True)
    notes         = models.TextField(blank=True, help_text='Internal notes from sales team')

    class Meta:
        db_table = 'demo_bookings'
        ordering = ['-created_at']

    def __str__(self):
        return f"{self.first_name} {self.last_name} — {self.church_name} ({self.status})"

    @property
    def full_name(self):
        return f"{self.first_name} {self.last_name}"
