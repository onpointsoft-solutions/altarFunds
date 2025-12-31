from django import forms
from django.contrib.auth import get_user_model

User = get_user_model()

class RegisterForm(forms.ModelForm):
    """Form for user registration."""
    password = forms.CharField(
        widget=forms.PasswordInput(attrs={'class': 'form-control form-control-lg'})
    )
    password_confirm = forms.CharField(
        widget=forms.PasswordInput(attrs={'class': 'form-control form-control-lg'}), 
        label="Confirm Password"
    )

    class Meta:
        model = User
        fields = ['first_name', 'last_name', 'email', 'password']
        widgets = {
            'first_name': forms.TextInput(attrs={'class': 'form-control form-control-lg'}),
            'last_name': forms.TextInput(attrs={'class': 'form-control form-control-lg'}),
            'email': forms.EmailInput(attrs={'class': 'form-control form-control-lg'}),
        }

    def clean_email(self):
        email = self.cleaned_data.get('email')
        if User.objects.filter(email=email).exists():
            raise forms.ValidationError("A user with this email already exists.")
        return email

    def clean_password_confirm(self):
        password = self.cleaned_data.get('password')
        password_confirm = self.cleaned_data.get('password_confirm')
        if password and password_confirm and password != password_confirm:
            raise forms.ValidationError("Passwords don't match")
        return password_confirm

    def save(self, commit=True):
        user = super().save(commit=False)
        # Set username to email since we're using email as username field
        user.username = user.email
        user.set_password(self.cleaned_data["password"])
        if commit:
            user.save()
        return user

class LoginForm(forms.Form):
    """Form for user login."""
    email = forms.EmailField(widget=forms.EmailInput(attrs={'class': 'form-control form-control-lg'}))
    password = forms.CharField(widget=forms.PasswordInput(attrs={'class': 'form-control form-control-lg'}))
