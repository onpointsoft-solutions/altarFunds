#!/bin/bash

# Sanctum Church Management System - Linux Uninstaller
echo "🗑️  Uninstall Sanctum Church Management System"
echo "=========================================="
echo

# Determine installation directory
if [ "$EUID" -eq 0 ]; then
    INSTALL_DIR="/opt/sanctum-church-management"
else
    INSTALL_DIR="$HOME/.local/sanctum-church-management"
fi

echo "⚠️  This will remove Sanctum Church Management System from your system."
echo "Installation directory: $INSTALL_DIR"
echo
read -p "Are you sure you want to continue? (y/N): " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "Uninstallation cancelled."
    exit 0
fi

echo
echo "🗑️  Removing application files..."
if [ -d "$INSTALL_DIR" ]; then
    rm -rf "$INSTALL_DIR"
    echo "✅ Application files removed"
else
    echo "ℹ️  Application directory not found"
fi

echo
echo "🗑️  Removing desktop shortcut..."
DESKTOP_FILE="$HOME/.local/share/applications/sanctum-church-management.desktop"
if [ -f "$DESKTOP_FILE" ]; then
    rm "$DESKTOP_FILE"
    echo "✅ Desktop shortcut removed"
else
    echo "ℹ️  Desktop shortcut not found"
fi

echo
echo "🗑️  Removing command line symlink..."
if [ "$EUID" -ne 0 ]; then
    USER_BIN="$HOME/.local/bin"
    if [ -L "$USER_BIN/sanctum" ]; then
        rm "$USER_BIN/sanctum"
        echo "✅ User symlink removed"
    else
        echo "ℹ️  User symlink not found"
    fi
else
    if [ -L "/usr/local/bin/sanctum" ]; then
        rm "/usr/local/bin/sanctum"
        echo "✅ System symlink removed"
    else
        echo "ℹ️  System symlink not found"
    fi
fi

echo
echo "╔══════════════════════════════════════════════════════════════╗"
echo "║                   Uninstallation Complete!                   ║"
echo "╚══════════════════════════════════════════════════════════════╝"
echo
echo "✅ Sanctum Church Management System has been successfully uninstalled."
echo
echo "🙏 Thank you for using Sanctum Church Management System!"
