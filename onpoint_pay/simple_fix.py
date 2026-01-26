#!/usr/bin/env python3
"""
Simple Django environment fix for OnPoint Pay
"""

import subprocess
import sys
import os

def run_command(command):
    """Run a command and return result"""
    try:
        result = subprocess.run(command, shell=True, capture_output=True, text=True)
        return result.returncode == 0, result.stdout, result.stderr
    except Exception as e:
        return False, str(e), str(e)

def main():
    """Main function to fix Django setup"""
    print("🔧 Fixing Django environment...")
    
    # Fix pkg_resources issue by installing setuptools
    commands = [
        ("pip", "install", "--upgrade", "setuptools"),
        ("pip", "install", "--upgrade", "wheel"),
        ("pip", "install", "--upgrade", "packaging"),
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
    
    # Reinstall requirements
    print("\n📦 Reinstalling requirements...")
    success, stdout, stderr = run_command(["pip", "install", "-r", "requirements.txt"])
    if success:
        print("✅ Requirements reinstalled successfully")
    else:
        print("❌ Failed to reinstall requirements")
    
    # Test Django
    print("\n🧪 Testing Django installation...")
    success, stdout, stderr = run_command(["python", "-c", "import django; print('Django', django.get_version())"])
    if success:
        print(f"✅ Django {stdout.strip()} - OK")
    else:
        print(f"❌ Django test failed: {stderr}")
    
    print("\n🎉 Fix complete!")
    print("📋 You can now run:")
    print("   python manage.py runserver")
    print("   python manage.py migrate")

if __name__ == "__main__":
    main()
