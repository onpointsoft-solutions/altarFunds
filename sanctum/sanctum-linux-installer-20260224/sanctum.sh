#!/bin/bash

# Sanctum Church Management System - Portable Launcher
echo "🏛️ Sanctum Church Management System - Portable Version"
echo "======================================================"
echo

# Check Java installation
echo "🔍 Checking Java installation..."
if ! command -v java &> /dev/null; then
    echo "❌ Java is not installed!"
    echo "Please install Java JRE 11 or higher:"
    echo "  Ubuntu/Debian: sudo apt install openjdk-11-jre"
    echo "  Fedora: sudo dnf install java-11-openjdk"
    echo "  Arch: sudo pacman -S jre11-openjdk"
    echo
    exit 1
else
    JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2)
    echo "✅ Java found: $JAVA_VERSION"
fi

echo
echo "🚀 Starting Sanctum..."
cd "$(dirname "$0")"
java -jar SanctumChurchManagement.jar

if [ $? -ne 0 ]; then
    echo
    echo "❌ Application failed to start"
    echo "Please check that Java is properly installed"
    exit 1
fi

echo
echo "🙏 Thank you for using Sanctum Church Management System!"
