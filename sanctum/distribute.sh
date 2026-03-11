#!/bin/bash

# Sanctum Church Management System - Distribution Script
# This script creates comprehensive distribution packages for all platforms

echo "🏛️ Sanctum Church Management System - Distribution Builder"
echo "=========================================================="

# Check if we're in the correct directory
if [ ! -f "pom.xml" ]; then
    echo "❌ Error: pom.xml not found. Please run this script from the sanctum directory."
    exit 1
fi

# Create distribution directory
DIST_BASE="sanctum-distributions"
rm -rf "$DIST_BASE"
mkdir -p "$DIST_BASE"

# Get current date and version
DATE=$(date +%Y%m%d)
VERSION="1.0.0"

echo "📦 Creating distribution packages for version $VERSION ($DATE)"

# First, build the application
echo ""
echo "🔨 Building application..."
./build.sh
if [ $? -ne 0 ]; then
    echo "❌ Build failed!"
    exit 1
fi

# Create platform-specific directories
echo ""
echo "📁 Creating distribution directories..."

WINDOWS_DIR="$DIST_BASE/windows-$DATE"
LINUX_DIR="$DIST_BASE/linux-$DATE"
MAC_DIR="$DIST_BASE/mac-$DATE"
UNIVERSAL_DIR="$DIST_BASE/universal-$DATE"

mkdir -p "$WINDOWS_DIR" "$LINUX_DIR" "$MAC_DIR" "$UNIVERSAL_DIR"

# Windows Distribution
echo ""
echo "🪟 Creating Windows distribution..."
cp "sanctum-windows-installer-$DATE.zip" "$WINDOWS_DIR/"
cp "target/Setup.exe" "$WINDOWS_DIR/"
cp "target/Sanctum.exe" "$WINDOWS_DIR/"
cp "target/Uninstall.exe" "$WINDOWS_DIR/"
cp "target/README.txt" "$WINDOWS_DIR/"
cp "target/LICENSE.txt" "$WINDOWS_DIR/"
cp "target/VERSION.txt" "$WINDOWS_DIR/"
cp "target/SanctumChurchManagement.jar" "$WINDOWS_DIR/"

# Create Windows installer script
cat > "$WINDOWS_DIR/INSTALL.bat" << 'EOF'
@echo off
title Sanctum Church Management System - Installation
echo 🏛️ Sanctum Church Management System
echo ================================
echo.
echo Welcome to the Sanctum Church Management System installer!
echo.
echo This will install Sanctum on your Windows system.
echo.
pause

REM Check Java
echo Checking Java installation...
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo.
    echo ❌ Java is not installed!
    echo Please install Java JRE 11 or higher from:
    echo https://www.oracle.com/java/technologies/downloads/
    echo.
    pause
    exit /b 1
)

echo ✅ Java found, starting installation...
echo.

REM Run installer
Setup.exe

echo.
echo Installation complete! You can now run Sanctum from your desktop.
pause
EOF

# Linux Distribution
echo ""
echo "🐧 Creating Linux distribution..."
cp "sanctum-linux-installer-$DATE.zip" "$LINUX_DIR/"
cp "target/install.sh" "$LINUX_DIR/"
cp "target/sanctum.sh" "$LINUX_DIR/"
cp "target/uninstall.sh" "$LINUX_DIR/"
cp "target/README-Linux.txt" "$LINUX_DIR/"
cp "target/LICENSE-Linux.txt" "$LINUX_DIR/"
cp "target/VERSION-Linux.txt" "$LINUX_DIR/"
cp "target/SanctumChurchManagement.jar" "$LINUX_DIR/"

# Create Linux installer script
cat > "$LINUX_DIR/INSTALL.sh" << 'EOF'
#!/bin/bash

echo "🏛️ Sanctum Church Management System - Linux Installation"
echo "======================================================"
echo

# Check Java
echo "🔍 Checking Java installation..."
if ! command -v java &> /dev/null; then
    echo "❌ Java is not installed!"
    echo "Please install Java JRE 11 or higher:"
    echo "  Ubuntu/Debian: sudo apt install openjdk-11-jre"
    echo "  Fedora: sudo dnf install java-11-openjdk"
    echo "  Arch: sudo pacman -S jre11-openjdk"
    echo
    exit 1
fi

echo "✅ Java found, starting installation..."
echo

# Make scripts executable
chmod +x install.sh sanctum.sh uninstall.sh

# Run installer
./install.sh

echo
echo "Installation complete! You can now run Sanctum from your application menu or by typing 'sanctum'."
EOF

chmod +x "$LINUX_DIR/INSTALL.sh"

# Mac Distribution (placeholder for future development)
echo ""
echo "🍎 Creating Mac distribution..."
cat > "$MAC_DIR/README.txt" << EOF
Sanctum Church Management System - Mac Version
===============================================

Coming Soon!
-----------

Mac support is currently under development.

For now, Mac users can:
1. Use the Linux version with compatibility tools
2. Use the web version when available
3. Contact support for Mac-specific installation

System Requirements (when available):
- macOS 10.15 or higher
- Java JRE 11 or higher
- 100MB free disk space
- 2GB RAM minimum

Thank you for your patience!
EOF

cat > "$MAC_DIR/LICENSE.txt" < "target/LICENSE.txt"
cat > "$MAC_DIR/VERSION.txt" < "target/VERSION.txt"

# Universal Distribution (Java JAR only)
echo ""
echo "🌐 Creating Universal distribution..."
cp "target/SanctumChurchManagement.jar" "$UNIVERSAL_DIR/"
cp "target/LICENSE.txt" "$UNIVERSAL_DIR/"
cp "target/VERSION.txt" "$UNIVERSAL_DIR/"

# Create universal README
cat > "$UNIVERSAL_DIR/README.txt" << 'EOF'
Sanctum Church Management System - Universal Java Version
==========================================================

This is the universal Java version that runs on any platform with Java.

Installation:
------------

1. Ensure Java JRE 11 or higher is installed
2. Run: java -jar SanctumChurchManagement.jar

Demo Accounts:
--------------
Email: admin@altarfunds.com     Password: admin123     (Admin)
Email: pastor@altarfunds.com    Password: pastor123    (Pastor)
Email: treasurer@altarfunds.com Password: treasurer123 (Treasurer)
Email: secretary@altarfunds.com Password: secretary123 (Secretary)
Email: usher@altarfunds.com     Password: usher123     (Usher)
Email: test@altarfunds.com      Password: test123      (Member)

Features:
--------
- User authentication with role-based access
- Church member management
- Financial tracking and reporting
- Event scheduling and management
- Communication tools
- Full-screen immersive interface
- Built-in SQLite database

System Requirements:
-------------------
- Java Runtime Environment (JRE) 11 or higher
- 100MB free disk space
- 2GB RAM minimum
- Any operating system (Windows, Linux, Mac, etc.)

Troubleshooting:
---------------
- If the application doesn't start, ensure Java 11+ is installed
- Download Java from: https://www.oracle.com/java/technologies/downloads/
- For memory issues, increase heap size: java -Xmx2g -jar SanctumChurchManagement.jar

Thank you for using Sanctum Church Management System!
EOF

# Create main distribution README
cat > "$DIST_BASE/README.txt" << EOF
Sanctum Church Management System - Distribution Package
========================================================

Version: $VERSION
Build Date: $(date)
Platforms: Windows, Linux, Universal Java

This distribution package contains installers for multiple platforms.

Platform-Specific Installers:
-----------------------------

Windows 🪟:
- Run: windows-$DATE/INSTALL.bat
- Or extract: sanctum-windows-installer-$DATE.zip
- Automatic installation with desktop shortcuts

Linux 🐧:
- Run: linux-$DATE/INSTALL.sh
- Or extract: sanctum-linux-installer-$DATE.zip
- Automatic installation with desktop integration

Universal Java 🌐:
- Platform-independent JAR file
- Requires Java 11 or higher
- Run: java -jar SanctumChurchManagement.jar

Quick Start:
-----------

1. Choose your platform directory
2. Run the appropriate installer
3. Launch the application
4. Login with demo credentials

Demo Accounts:
--------------
Admin: admin@altarfunds.com / admin123
Pastor: pastor@altarfunds.com / pastor123
Treasurer: treasurer@altarfunds.com / treasurer123
Secretary: secretary@altarfunds.com / secretary123
Usher: usher@altarfunds.com / usher123
Member: test@altarfunds.com / test123

System Requirements:
-------------------
- Windows 7+ / Linux / Mac (for universal version)
- Java Runtime Environment (JRE) 11 or higher
- 100MB free disk space
- 2GB RAM minimum

Support:
--------
For technical support, questions, or contributions:
- Check the documentation
- Visit our website
- Contact the development team

Thank you for choosing Sanctum Church Management System!
EOF

# Create distribution information file
cat > "$DIST_BASE/DISTRIBUTION_INFO.txt" << EOF
Sanctum Church Management System - Distribution Information
===========================================================

Version: $VERSION
Build Date: $(date)
Build Number: $DATE

Package Contents:
----------------

Windows Package (windows-$DATE/):
- Setup.exe - Automatic installer
- Sanctum.exe - Portable version
- Uninstall.exe - Complete uninstaller
- INSTALL.bat - Installation script
- README.txt - Windows-specific instructions
- LICENSE.txt - License agreement
- VERSION.txt - Version information
- SanctumChurchManagement.jar - Main application
- sanctum-windows-installer-$DATE.zip - Complete package

Linux Package (linux-$DATE/):
- install.sh - Automatic installer
- sanctum.sh - Portable version
- uninstall.sh - Complete uninstaller
- INSTALL.sh - Installation script
- README-Linux.txt - Linux-specific instructions
- LICENSE-Linux.txt - License agreement
- VERSION-Linux.txt - Version information
- SanctumChurchManagement.jar - Main application
- sanctum-linux-installer-$DATE.zip - Complete package

Mac Package (mac-$DATE/):
- README.txt - Mac status and information
- LICENSE.txt - License agreement
- VERSION.txt - Version information
- (Full Mac support coming soon)

Universal Package (universal-$DATE/):
- SanctumChurchManagement.jar - Platform-independent JAR
- README.txt - Universal instructions
- LICENSE.txt - License agreement
- VERSION.txt - Version information

Distribution Methods:
-------------------

1. Platform-Specific: Use windows-$DATE/ or linux-$DATE/ directories
2. ZIP Packages: Use the platform-specific ZIP files
3. Universal: Use universal-$DATE/ for any Java-enabled platform

Installation Types:
------------------

1. Automatic Installation: Setup.exe (Windows) or install.sh (Linux)
2. Portable Mode: Sanctum.exe (Windows) or sanctum.sh (Linux)
3. Universal Java: java -jar SanctumChurchManagement.jar

Features:
--------
- Cross-platform support (Windows, Linux, Universal Java)
- Automatic and portable installation options
- Desktop integration and shortcuts
- Built-in uninstallers
- Java detection and validation
- Professional installation experience
- Comprehensive documentation
- Demo accounts for testing

Technical Details:
-----------------
- Built with Maven shade plugin
- Embedded SQLite database
- JWT authentication
- REST API backend
- Swing UI with full-screen support
- Comprehensive logging system

Thank you for using Sanctum Church Management System!
EOF

# Create version and build information
cat > "$DIST_BASE/VERSION.txt" << EOF
Sanctum Church Management System
Version: $VERSION
Build Date: $(date)
Build Number: $DATE
Distribution Type: Multi-Platform

Platforms Supported:
- Windows 7, 8, 10, 11
- Linux (Ubuntu, Fedora, Arch, etc.)
- Universal Java (any platform with Java 11+)

Installation Types:
- Automatic installer (Windows/Linux)
- Portable executable (Windows/Linux)
- Universal JAR (all platforms)

Features:
- User authentication with role-based access
- Church member management
- Financial tracking and reporting
- Event scheduling and management
- Communication tools
- Full-screen immersive interface
- Built-in SQLite database
- Cross-platform compatibility

Thank you for choosing Sanctum Church Management System!
EOF

# Copy license to main directory
cp "target/LICENSE.txt" "$DIST_BASE/"

# Create final distribution ZIP
echo ""
echo "📦 Creating final distribution package..."
cd "$DIST_BASE"
zip -r "../sanctum-complete-distribution-$DATE.zip" .
cd ..

# Create platform-specific ZIPs
echo ""
echo "📦 Creating platform-specific packages..."
cd "$DIST_BASE"
zip -r "../sanctum-windows-$DATE.zip" "windows-$DATE/"
zip -r "../sanctum-linux-$DATE.zip" "linux-$DATE/"
zip -r "../sanctum-universal-$DATE.zip" "universal-$DATE/"
cd ..

# Generate checksums
echo ""
echo "🔐 Generating checksums..."
cd "$DIST_BASE"
md5sum *.zip > ../MD5_CHECKSUMS.txt
sha256sum *.zip > ../SHA256_CHECKSUMS.txt
cd ..

# Clean up temporary directories
echo ""
echo "🧹 Cleaning up temporary directories..."
rm -rf "$DIST_BASE"

echo ""
echo "✅ Distribution creation completed successfully!"
echo ""
echo "📦 Distribution packages created:"
echo "   - sanctum-complete-distribution-$DATE.zip (All platforms)"
echo "   - sanctum-windows-$DATE.zip (Windows only)"
echo "   - sanctum-linux-$DATE.zip (Linux only)"
echo "   - sanctum-universal-$DATE.zip (Universal Java only)"
echo "   - sanctum-windows-installer-$DATE.zip (Windows installer)"
echo "   - sanctum-linux-installer-$DATE.zip (Linux installer)"
echo ""
echo "🔐 Checksum files created:"
echo "   - MD5_CHECKSUMS.txt"
echo "   - SHA256_CHECKSUMS.txt"
echo ""
echo "📋 Distribution contents:"
echo "   ✅ Windows installer with automatic setup"
echo "   ✅ Linux installer with desktop integration"
echo "   ✅ Universal Java JAR for any platform"
echo "   ✅ Portable executables for both platforms"
echo "   ✅ Complete uninstallers"
echo "   ✅ Comprehensive documentation"
echo "   ✅ Demo accounts and testing credentials"
echo "   ✅ Professional installation experience"
echo ""
echo "🚀 Ready for distribution!"
echo ""
echo "🙏 Sanctum Church Management System - Multi-Platform Distribution Complete!"
