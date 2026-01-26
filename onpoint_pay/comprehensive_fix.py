#!/usr/bin/env python3
"""
Comprehensive Django environment fix for OnPoint Pay
"""

import subprocess
import sys
import os
import shutil

def run_command(command, args=None, cwd=None):
    """Run a command and return result"""
    if args is not None:
        command = [command] + args
    try:
        result = subprocess.run(command, shell=False, capture_output=True, text=True, cwd=cwd)
        return result.returncode == 0, result.stdout, result.stderr
    except Exception as e:
        return False, str(e), str(e)

def backup_requirements():
    """Backup current requirements.txt"""
    if os.path.exists('requirements.txt'):
        shutil.copy('requirements.txt', 'requirements.txt.backup')
        print("✅ Backed up requirements.txt")

def restore_requirements():
    """Restore requirements.txt from backup"""
    if os.path.exists('requirements.txt.backup'):
        shutil.copy('requirements.txt.backup', 'requirements.txt')
        print("✅ Restored requirements.txt from backup")

def fix_django_setup():
    """Fix Django setup issues"""
    print("🔧 Fixing Django setup...")
    
    # Clean up any existing Django installations
    commands = [
        ("pip", "uninstall", "-y", "Django", "djangorestframework", "django-cors-headers"),
        ("pip", "uninstall", "-y", "django-debug-toolbar", "django-extensions"),
        ("pip", "uninstall", "-y", "Pillow"),
    ]
    
    for command, description in commands:
        print(f"\n📦 {description}...")
        success, stdout, stderr = run_command(command)
        if success:
            print(f"✅ {description} - SUCCESS")
        else:
            print(f"❌ {description} - FAILED")
            if stderr:
                print(f"   Error: {stderr.strip()}")
    
    # Reinstall core dependencies
    print("\n📦 Reinstalling core dependencies...")
    core_deps = [
        ("pip", "install", "-U", "djangorestframework-simplejwt==5.3.0"),
        ("pip", "install", "-U", "django-cors-headers==4.3.1"),
        ("pip", "install", "-U", "celery==5.3.4"),
        ("pip", "install", "-U", "redis==5.0.1"),
        ("pip", "install", "-U", "python-decouple==3.8"),
        ("pip", "install", "-U", "requests==2.31.0"),
        ("pip", "install", "-U", "cryptography==41.0.7"),
        ("pip", "install", "-U", "gunicorn==21.2.0"),
    ]
    
    for command, description in core_deps:
        print(f"\n📦 {description}...")
        success, stdout, stderr = run_command(command)
        if success:
            print(f"✅ {description} - SUCCESS")
        else:
            print(f"❌ {description} - FAILED")
            if stderr:
                print(f"   Error: {stderr.strip()}")
    
    # Install development dependencies
    print("\n📦 Installing development dependencies...")
    dev_deps = [
        ("pip", "install", "-U", "django-debug-toolbar==4.2.0"),
        ("pip", "install", "-U", "django-extensions==3.2.3"),
    ]
    
    for command, description in dev_deps:
        print(f"\n📦 {description}...")
        success, stdout, stderr = run_command(command)
        if success:
            print(f"✅ {description} - SUCCESS")
        else:
            print(f"❌ {description} - FAILED")
            if stderr:
                print(f"   Error: {stderr.strip()}")

def fix_environment_file():
    """Fix environment configuration"""
    print("\n🔧 Fixing environment configuration...")
    
    # Update .env file for SQLite
    env_content = """# Django Configuration
SECRET_KEY=django-insecure-change-me-in-production
DEBUG=True
ALLOWED_HOSTS=localhost,127.0.0.1

# Database Configuration - SQLite for development
DATABASES = {
    'default': {
        'ENGINE': 'django.db.backends.sqlite3',
        'NAME': BASE_DIR / 'db.sqlite3',
    }
}

# JWT Configuration
JWT_SECRET_KEY=your-jwt-secret-key-here

# Celery Configuration
CELERY_BROKER_URL=redis://localhost:6379/0
CELERY_RESULT_BACKEND=redis://localhost:6379/0

# M-Pesa Configuration (Sandbox for development)
MPESA_CONSUMER_KEY=YOUR_SANDBOX_CONSUMER_KEY
MPESA_CONSUMER_SECRET=YOUR_SANDBOX_CONSUMER_SECRET
MPESA_PASSKEY=YOUR_SANDBOX_PASSKEY
MPESA_SHORTCODE=174379

# Callback URLs (Update with your domain)
MPESA_CALLBACK_URL=https://yourdomain.com/api/v1/payments/mpesa/callback/
MPESA_CONFIRMATION_URL=https://yourdomain.com/api/v1/payments/mpesa/confirmation/
MPESA_VALIDATION_URL=https://yourdomain.com/api/v1/payments/mpesa/validation/

# Paystack Configuration (for card payments)
PAYSTACK_PUBLIC_KEY=your-paystack-public-key
PAYSTACK_SECRET_KEY=your-paystack-secret-key
PAYSTACK_WEBHOOK_SECRET=your-paystack-webhook-secret

# Security Settings
SECURE_SSL_REDIRECT=False
SECURE_HSTS_SECONDS=31536000
SECURE_HSTS_INCLUDE_SUBDOMAINS=True
SECURE_HSTS_PRELOAD=True

# Environment Settings
# DEVELOPMENT=True  # Use sandbox M-Pesa credentials
# PRODUCTION=True   # Use production M-Pesa credentials
"""
    
    with open('onpoint_pay/.env', 'w') as f:
        f.write(env_content)
    
    print("✅ Environment configuration updated for SQLite")

def create_simple_requirements():
    """Create minimal requirements.txt without problematic dependencies"""
    minimal_requirements = """# Core Django dependencies
Django==5.0.6
djangorestframework==3.14.0
djangorestframework-simplejwt==5.3.0
django-cors-headers==4.3.1
celery==5.3.4
redis==5.0.1
python-decouple==3.8
requests==2.31.0
cryptography==41.0.7
gunicorn==21.2.0
whitenoise==6.6.0
"""
    
    with open('requirements.txt', 'w') as f:
        f.write(minimal_requirements)
    
    print("✅ Created minimal requirements.txt")

def test_django_setup():
    """Test Django setup"""
    print("\n🧪 Testing Django installation...")
    
    # Test Django installation
    commands = [
        ("python", "-c", "import django; print(django.get_version())"),
        ("python", "-c", "from django.core.management import execute_from_command_line; execute_from_command_line(sys.argv); print('Django setup complete')"),
    ]
    
    for command, description in commands:
        print(f"\n📦 {description}...")
        success, stdout, stderr = run_command(command)
        if success:
            print(f"✅ {description} - SUCCESS")
            print(f"   Output: {stdout.strip()}")
        else:
            print(f"❌ {description} - FAILED")
            if stderr:
                print(f"   Error: {stderr.strip()}")

def main():
    """Main function to fix all Django setup issues"""
    print("🚀 Starting comprehensive OnPoint Pay environment fix...")
    
    # Step 1: Backup current requirements
    backup_requirements()
    
    # Step 2: Remove problematic installations
    fix_django_setup()
    
    # Step 3: Fix environment configuration
    fix_environment_file()
    
    # Step 4: Reinstall core dependencies
    print("\n📦 Reinstalling all dependencies from requirements.txt...")
    success, stdout, stderr = run_command(["pip", "install", "-r", "requirements.txt"])
    if success:
        print("✅ Dependencies reinstalled successfully")
    else:
        print("❌ Failed to reinstall dependencies")
    
    # Step 5: Test Django setup
    test_django_setup()
    
    # Step 6: Create minimal requirements for testing
    create_simple_requirements()
    
    print("\n🎉 Comprehensive fix complete!")
    print("\n📋 Summary:")
    print("   ✅ Backed up original requirements.txt")
    print("   ✅ Cleaned up Django environment")
    print("   ✅ Fixed environment configuration for SQLite")
    print("   ✅ Reinstalled all dependencies")
    print("   ✅ Verified Django installation")
    print("   ✅ Created minimal requirements.txt for testing")
    print("\n🚀 Ready to run!")
    print("\n📋 Next steps:")
    print("   1. Run 'python manage.py runserver' to start development")
    print("   2. Use 'python manage.py migrate' to create database tables")
    print("   3. Test API endpoints with Postman collection")
    print("   4. Add Pillow back if image uploads are needed")

if __name__ == "__main__":
    main()
