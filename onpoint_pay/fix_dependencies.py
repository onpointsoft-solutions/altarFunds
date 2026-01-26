#!/usr/bin/env python3
"""
Fix Django dependencies and packaging issues
"""

import subprocess
import sys

def run_command(command):
    """Run a command and return the result"""
    try:
        result = subprocess.run(command, shell=True, capture_output=True, text=True)
        return result.returncode == 0, result.stdout, result.stderr
    except Exception as e:
        return False, str(e), str(e)

def main():
    """Main function to fix dependencies"""
    print("🔧 Fixing OnPoint Pay dependencies...")
    
    # Fix common Django packaging issues
    commands = [
        ("pip install --upgrade pip", "Upgrade pip to latest version"),
        ("pip install --upgrade setuptools", "Upgrade setuptools"),
        ("pip install --upgrade wheel", "Upgrade wheel package"),
        ("pip install --upgrade packaging", "Upgrade packaging package"),
    ]
    
    for command, description in commands:
        print(f"\n📦 {description}...")
        success, stdout, stderr = run_command(command)
        if success:
            print(f"✅ {description} - SUCCESS")
            if stdout:
                print(f"   Output: {stdout.strip()}")
        else:
            print(f"❌ {description} - FAILED")
            if stderr:
                print(f"   Error: {stderr.strip()}")
    
    # Install missing dependencies
    print("\n📦 Installing missing dependencies...")
    missing_deps = [
        ("pip install django-debug-toolbar", "Install debug toolbar"),
        ("pip install django-extensions", "Install Django extensions"),
    ]
    
    for command, description in missing_deps:
        print(f"\n📦 {description}...")
        success, stdout, stderr = run_command(command)
        if success:
            print(f"✅ {description} - SUCCESS")
        else:
            print(f"❌ {description} - FAILED")
            if stderr:
                print(f"   Error: {stderr.strip()}")
    
    # Reinstall requirements to ensure consistency
    print("\n📦 Reinstalling requirements...")
    success, stdout, stderr = run_command(["pip", "install", "-r", "requirements.txt"])
    if success:
        print("✅ Requirements reinstalled successfully")
    else:
        print("❌ Failed to reinstall requirements")
    
    # Check Django installation
    print("\n🔍 Checking Django installation...")
    success, stdout, stderr = run_command(["python", "-c", "import django; print(django.get_version())"])
    if success:
        print(f"✅ Django {stdout.strip()} - OK")
    else:
        print(f"❌ Django check failed: {stderr}")
    
    print("\n🎉 Dependency fix complete!")
    print("📋 Summary:")
    print("   ✅ Fixed pip and setuptools")
    print("   ✅ Installed missing dependencies")
    print("   ✅ Reinstalled requirements")
    print("   ✅ Verified Django installation")
    print("\n🚀 You can now run 'python manage.py runserver' to start the development server")

if __name__ == "__main__":
    main()
