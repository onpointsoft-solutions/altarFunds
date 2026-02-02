"""
Enhanced Authentication Views for AltarFunds Admin
"""

from django.contrib.auth import views as auth_views
from django.contrib.auth.forms import PasswordChangeForm, PasswordResetForm, SetPasswordForm
from django.contrib.auth.mixins import LoginRequiredMixin
from django.contrib import messages
from django.shortcuts import render, redirect
from django.urls import reverse_lazy
from django.views.generic import UpdateView, TemplateView
from django.utils.decorators import method_decorator
from django.views.decorators.csrf import csrf_protect
from django.views.decorators.debug import sensitive_post_parameters
from django.contrib.auth import update_session_auth_hash
from django.contrib.auth.models import User
from django.contrib.auth.forms import UserCreationForm
from django.http import JsonResponse
from django.views import View
import json
from datetime import datetime

class EnhancedLoginView(auth_views.LoginView):
    """Enhanced login view with modern UI and additional features"""
    template_name = 'admin/enhanced_login.html'
    redirect_authenticated_user = True
    
    def get_context_data(self, **kwargs):
        context = super().get_context_data(**kwargs)
        context['title'] = 'Sign In - AltarFunds Admin'
        context['site_title'] = 'AltarFunds'
        context['site_header'] = 'AltarFunds Admin'
        context['show_password_reset'] = True
        context['show_register'] = True
        context['login_attempts'] = self.request.session.get('login_attempts', 0)
        return context
    
    def form_invalid(self, form):
        # Track failed login attempts
        attempts = self.request.session.get('login_attempts', 0) + 1
        self.request.session['login_attempts'] = attempts
        
        # Add security message after multiple failed attempts
        if attempts >= 3:
            messages.warning(self.request, 
                f'Multiple failed login attempts detected. Please verify your credentials.')
        
        return super().form_invalid(form)
    
    def form_valid(self, form):
        # Clear login attempts on successful login
        if 'login_attempts' in self.request.session:
            del self.request.session['login_attempts']
        
        # Add success message
        messages.success(self.request, f'Welcome back, {form.get_user().get_full_name() or form.get_user().username}!')
        
        return super().form_valid(form)

class EnhancedLogoutView(auth_views.LogoutView):
    """Enhanced logout view with confirmation"""
    
    def dispatch(self, request, *args, **kwargs):
        messages.info(request, 'You have been successfully logged out.')
        return super().dispatch(request, *args, **kwargs)

@method_decorator(csrf_protect, name='dispatch')
@method_decorator(sensitive_post_parameters('old_password', 'new_password1', 'new_password2'), name='dispatch')
class EnhancedPasswordChangeView(LoginRequiredMixin, auth_views.PasswordChangeView):
    """Enhanced password change view"""
    template_name = 'admin/enhanced_password_change.html'
    success_url = reverse_lazy('admin_management:enhanced_password_change_done')
    
    def get_context_data(self, **kwargs):
        context = super().get_context_data(**kwargs)
        context['title'] = 'Change Password - AltarFunds Admin'
        context['site_title'] = 'AltarFunds'
        context['site_header'] = 'AltarFunds Admin'
        return context
    
    def form_valid(self, form):
        messages.success(self.request, 'Your password has been successfully changed.')
        return super().form_valid(form)

class EnhancedPasswordChangeDoneView(LoginRequiredMixin, auth_views.PasswordChangeDoneView):
    """Enhanced password change done view"""
    template_name = 'admin/enhanced_password_change_done.html'
    
    def get_context_data(self, **kwargs):
        context = super().get_context_data(**kwargs)
        context['title'] = 'Password Changed - AltarFunds Admin'
        context['site_title'] = 'AltarFunds'
        context['site_header'] = 'AltarFunds Admin'
        return context

class EnhancedPasswordResetView(auth_views.PasswordResetView):
    """Enhanced password reset view"""
    template_name = 'admin/enhanced_password_reset.html'
    email_template_name = 'admin/password_reset_email.html'
    subject_template_name = 'admin/password_reset_subject.txt'
    success_url = reverse_lazy('admin_management:enhanced_password_reset_done')
    
    def get_context_data(self, **kwargs):
        context = super().get_context_data(**kwargs)
        context['title'] = 'Reset Password - AltarFunds Admin'
        context['site_title'] = 'AltarFunds'
        context['site_header'] = 'AltarFunds Admin'
        return context
    
    def form_valid(self, form):
        messages.success(self.request, 'Password reset instructions have been sent to your email.')
        return super().form_valid(form)

class EnhancedPasswordResetDoneView(auth_views.PasswordResetDoneView):
    """Enhanced password reset done view"""
    template_name = 'admin/enhanced_password_reset_done.html'
    
    def get_context_data(self, **kwargs):
        context = super().get_context_data(**kwargs)
        context['title'] = 'Password Reset Sent - AltarFunds Admin'
        context['site_title'] = 'AltarFunds'
        context['site_header'] = 'AltarFunds Admin'
        return context

@method_decorator(sensitive_post_parameters(), name='dispatch')
class EnhancedPasswordResetConfirmView(auth_views.PasswordResetConfirmView):
    """Enhanced password reset confirm view"""
    template_name = 'admin/enhanced_password_reset_confirm.html'
    success_url = reverse_lazy('admin_management:enhanced_password_reset_complete')
    
    def get_context_data(self, **kwargs):
        context = super().get_context_data(**kwargs)
        context['title'] = 'Set New Password - AltarFunds Admin'
        context['site_title'] = 'AltarFunds'
        context['site_header'] = 'AltarFunds Admin'
        return context
    
    def form_valid(self, form):
        messages.success(self.request, 'Your password has been successfully reset.')
        return super().form_valid(form)

class EnhancedPasswordResetCompleteView(auth_views.PasswordResetCompleteView):
    """Enhanced password reset complete view"""
    template_name = 'admin/enhanced_password_reset_complete.html'
    
    def get_context_data(self, **kwargs):
        context = super().get_context_data(**kwargs)
        context['title'] = 'Password Reset Complete - AltarFunds Admin'
        context['site_title'] = 'AltarFunds'
        context['site_header'] = 'AltarFunds Admin'
        return context

class ProfileView(LoginRequiredMixin, TemplateView):
    """User profile view"""
    template_name = 'admin/profile.html'
    
    def get_context_data(self, **kwargs):
        context = super().get_context_data(**kwargs)
        context['title'] = 'My Profile - AltarFunds Admin'
        context['site_title'] = 'AltarFunds'
        context['site_header'] = 'AltarFunds Admin'
        context['user'] = self.request.user
        return context

class ProfileEditView(LoginRequiredMixin, UpdateView):
    """Profile edit view"""
    model = User
    template_name = 'admin/profile_edit.html'
    fields = ['first_name', 'last_name', 'email']
    success_url = reverse_lazy('admin_management:profile')
    
    def get_object(self):
        return self.request.user
    
    def get_context_data(self, **kwargs):
        context = super().get_context_data(**kwargs)
        context['title'] = 'Edit Profile - AltarFunds Admin'
        context['site_title'] = 'AltarFunds'
        context['site_header'] = 'AltarFunds Admin'
        return context
    
    def form_valid(self, form):
        messages.success(self.request, 'Your profile has been successfully updated.')
        return super().form_valid(form)

class SettingsView(LoginRequiredMixin, TemplateView):
    """Settings view"""
    template_name = 'admin/settings.html'
    
    def get_context_data(self, **kwargs):
        context = super().get_context_data(**kwargs)
        context['title'] = 'Settings - AltarFunds Admin'
        context['site_title'] = 'AltarFunds'
        context['site_header'] = 'AltarFunds Admin'
        return context

class HelpView(LoginRequiredMixin, TemplateView):
    """Help view"""
    template_name = 'admin/help.html'
    
    def get_context_data(self, **kwargs):
        context = super().get_context_data(**kwargs)
        context['title'] = 'Help - AltarFunds Admin'
        context['site_title'] = 'AltarFunds'
        context['site_header'] = 'AltarFunds Admin'
        return context

class DocumentationView(LoginRequiredMixin, TemplateView):
    """Documentation view"""
    template_name = 'admin/documentation.html'
    
    def get_context_data(self, **kwargs):
        context = super().get_context_data(**kwargs)
        context['title'] = 'Documentation - AltarFunds Admin'
        context['site_title'] = 'AltarFunds'
        context['site_header'] = 'AltarFunds Admin'
        return context

class APITokenView(LoginRequiredMixin, View):
    """API token management view"""
    
    def get(self, request):
        """Get or generate API token for user"""
        from rest_framework.authtoken.models import Token
        
        token, created = Token.objects.get_or_create(user=request.user)
        
        return JsonResponse({
            'token': token.key,
            'created': created,
            'created_at': token.created.isoformat()
        })
    
    def post(self, request):
        """Regenerate API token"""
        from rest_framework.authtoken.models import Token
        
        # Delete existing token
        Token.objects.filter(user=request.user).delete()
        
        # Create new token
        token = Token.objects.create(user=request.user)
        
        return JsonResponse({
            'token': token.key,
            'created': True,
            'created_at': token.created.isoformat()
        })

class TwoFactorSetupView(LoginRequiredMixin, TemplateView):
    """Two-factor authentication setup view"""
    template_name = 'admin/two_factor_setup.html'
    
    def get_context_data(self, **kwargs):
        context = super().get_context_data(**kwargs)
        context['title'] = 'Two-Factor Authentication - AltarFunds Admin'
        context['site_title'] = 'AltarFunds'
        context['site_header'] = 'AltarFunds Admin'
        return context

class SecurityLogView(LoginRequiredMixin, TemplateView):
    """Security log view"""
    template_name = 'admin/security_log.html'
    
    def get_context_data(self, **kwargs):
        context = super().get_context_data(**kwargs)
        context['title'] = 'Security Log - AltarFunds Admin'
        context['site_title'] = 'AltarFunds'
        context['site_header'] = 'AltarFunds Admin'
        
        # Get recent security events
        from audit.models import AuditLog
        context['recent_events'] = AuditLog.objects.filter(
            user=self.request.user
        ).order_by('-timestamp')[:20]
        
        return context

# Enhanced form classes
class EnhancedPasswordChangeForm(PasswordChangeForm):
    """Enhanced password change form with additional validation"""
    
    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.fields['old_password'].widget.attrs.update({
            'class': 'form-control',
            'placeholder': 'Enter current password'
        })
        self.fields['new_password1'].widget.attrs.update({
            'class': 'form-control',
            'placeholder': 'Enter new password'
        })
        self.fields['new_password2'].widget.attrs.update({
            'class': 'form-control',
            'placeholder': 'Confirm new password'
        })
    
    def clean_new_password1(self):
        password = self.cleaned_data.get('new_password1')
        
        # Additional password strength validation
        if len(password) < 8:
            raise forms.ValidationError("Password must be at least 8 characters long.")
        
        if not any(char.isdigit() for char in password):
            raise forms.ValidationError("Password must contain at least one digit.")
        
        if not any(char.isupper() for char in password):
            raise forms.ValidationError("Password must contain at least one uppercase letter.")
        
        if not any(char.islower() for char in password):
            raise forms.ValidationError("Password must contain at least one lowercase letter.")
        
        return password

class EnhancedPasswordResetForm(PasswordResetForm):
    """Enhanced password reset form"""
    
    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.fields['email'].widget.attrs.update({
            'class': 'form-control',
            'placeholder': 'Enter your email address'
        })
