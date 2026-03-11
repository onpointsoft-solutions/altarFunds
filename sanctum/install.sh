#!/bin/bash

# Sanctum Church Management System - Local Install Script
# This script builds and installs the application locally

echo "🏛️ Sanctum Church Management System - Local Installation"
echo "========================================================"

# Check if we're in the correct directory
if [ ! -f "pom.xml" ]; then
    echo "❌ Error: pom.xml not found. Please run this script from the sanctum directory."
    exit 1
fi

# Check Java installation
echo "🔍 Checking Java installation..."
if ! command -v java &> /dev/null; then
    echo "❌ Java is not installed!"
    echo "Please install Java JRE 11 or higher:"
    echo "  Ubuntu/Debian: sudo apt install openjdk-11-jre"
    echo "  Fedora: sudo dnf install java-11-openjdk"
    echo "  Arch: sudo pacman -S jre11-openjdk"
    exit 1
else
    JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2)
    echo "✅ Java found: $JAVA_VERSION"
fi

# Check Maven installation
echo "🔍 Checking Maven installation..."
if ! command -v mvn &> /dev/null; then
    echo "❌ Maven is not installed!"
    echo "Please install Maven:"
    echo "  Ubuntu/Debian: sudo apt install maven"
    echo "  Fedora: sudo dnf install maven"
    echo "  Arch: sudo pacman -S maven"
    exit 1
else
    MVN_VERSION=$(mvn -version | head -n 1 | cut -d' ' -f3)
    echo "✅ Maven found: $MVN_VERSION"
fi

# Build the application
echo ""
echo "🔨 Building Sanctum..."
mvn clean package -DskipTests

if [ $? -ne 0 ]; then
    echo "❌ Build failed!"
    exit 1
fi

echo "✅ Build successful!"

# Determine installation directory
if [ "$EUID" -eq 0 ]; then
    INSTALL_DIR="/opt/sanctum-church-management"
    echo "🔧 System-wide installation to $INSTALL_DIR"
else
    INSTALL_DIR="$HOME/.local/sanctum-church-management"
    echo "🏠 User installation to $INSTALL_DIR"
fi

# Create installation directory
echo ""
echo "📁 Creating installation directory..."
mkdir -p "$INSTALL_DIR"
if [ $? -eq 0 ]; then
    echo "✅ Created: $INSTALL_DIR"
else
    echo "❌ Failed to create installation directory"
    exit 1
fi

# Copy application files
echo ""
echo "📦 Copying application files..."
JAR_FILE=$(find target -name "sanctum-church-management-*.jar" -not -name "*-sources.jar" -not -name "*-javadoc.jar" | head -n 1)
cp "$JAR_FILE" "$INSTALL_DIR/SanctumChurchManagement.jar"
if [ $? -eq 0 ]; then
    echo "✅ Application files copied"
else
    echo "❌ Failed to copy application files"
    exit 1
fi

# Create launcher script
echo ""
echo "🚀 Creating launcher script..."
cat > "$INSTALL_DIR/sanctum" << 'LAUNCHER_EOF'
#!/bin/bash

# Sanctum Church Management System Launcher
cd "$(dirname "$0")"
echo "🏛️ Starting Sanctum Church Management System..."
java -jar SanctumChurchManagement.jar
LAUNCHER_EOF

chmod +x "$INSTALL_DIR/sanctum"

# Create desktop shortcut
echo ""
echo "🖥️  Creating desktop shortcut..."
DESKTOP_DIR="$HOME/.local/share/applications"
mkdir -p "$DESKTOP_DIR"

cat > "$DESKTOP_DIR/sanctum-church-management.desktop" << DESKTOP_EOF
[Desktop Entry]
Version=1.0
Type=Application
Name=Sanctum Church Management System
Comment=Church management and administration system
Exec=$INSTALL_DIR/sanctum
Icon=applications-office
Terminal=false
Categories=Office;Management;
DESKTOP_EOF

# Create symlink in user bin directory
if [ "$EUID" -ne 0 ]; then
    USER_BIN="$HOME/.local/bin"
    mkdir -p "$USER_BIN"
    ln -sf "$INSTALL_DIR/sanctum" "$USER_BIN/sanctum" 2>/dev/null || true
    echo "✅ Created symlink: $USER_BIN/sanctum"
else
    # System-wide installation
    ln -sf "$INSTALL_DIR/sanctum" "/usr/local/bin/sanctum" 2>/dev/null || true
    echo "✅ Created symlink: /usr/local/bin/sanctum"
fi

echo ""
echo "╔══════════════════════════════════════════════════════════════╗"
echo "║                  Installation Complete!                     ║"
echo "╚══════════════════════════════════════════════════════════════╝"
echo ""
echo "✅ Sanctum Church Management System has been successfully installed!"
echo ""
echo "📁 Installation Location: $INSTALL_DIR"
echo "🖥️  Desktop Shortcut: Available in application menu"
echo "🚀 Command Line: sanctum"
echo ""
echo "🌐 Demo Accounts:"
echo "   Admin: admin@altarfunds.com / admin123"
echo "   Pastor: pastor@altarfunds.com / pastor123"
echo "   Treasurer: treasurer@altarfunds.com / treasurer123"
echo "   Secretary: secretary@altarfunds.com / secretary123"
echo "   Usher: usher@altarfunds.com / usher123"
echo "   Member: test@altarfunds.com / test123"
echo ""
echo "🚀 You can now run Sanctum from:"
echo "   • Application menu (Sanctum Church Management System)"
echo "   • Command line: sanctum"
echo "   • Direct: $INSTALL_DIR/sanctum"
echo ""
echo "🙏 Thank you for choosing Sanctum Church Management System!"
