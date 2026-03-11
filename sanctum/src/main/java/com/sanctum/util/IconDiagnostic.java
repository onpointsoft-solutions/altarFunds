package com.sanctum.util;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.URL;

/**
 * Utility class to diagnose icon loading issues
 */
public class IconDiagnostic {
    
    /**
     * Test loading of icon files and provide detailed diagnostic information
     */
    public static void diagnoseIcons() {
        System.out.println("=== ICON DIAGNOSTIC REPORT ===");
        
        // Test ICO file
        testIconFile("/images/icon.ico", "ICO");
        
        // Test PNG file
        testIconFile("/images/icon.png", "PNG");
        
        // Test if resources directory exists
        testResourceDirectory();
        
        System.out.println("=== END DIAGNOSTIC REPORT ===");
    }
    
    private static void testIconFile(String path, String type) {
        System.out.println("\n--- Testing " + type + " Icon: " + path + " ---");
        
        try {
            URL url = IconDiagnostic.class.getResource(path);
            if (url == null) {
                System.out.println("❌ File not found in resources");
                return;
            }
            
            System.out.println("✅ File found at: " + url);
            
            // Load the icon
            ImageIcon icon = new ImageIcon(url);
            Image image = icon.getImage();
            
            if (image == null) {
                System.out.println("❌ Failed to load image");
                return;
            }
            
            System.out.println("✅ Image loaded successfully");
            
            // Get dimensions
            int width = icon.getIconWidth();
            int height = icon.getIconHeight();
            
            System.out.println("📏 Dimensions: " + width + "x" + height);
            
            if (width <= 0 || height <= 0) {
                System.out.println("❌ Invalid dimensions");
                return;
            }
            
            // Test if image is valid
            BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = bufferedImage.createGraphics();
            g2d.drawImage(image, 0, 0, null);
            g2d.dispose();
            
            System.out.println("✅ Image is renderable");
            
            // Color information
            int pixel = bufferedImage.getRGB(0, 0);
            Color color = new Color(pixel, true);
            System.out.println("🎨 Sample pixel color: " + color);
            
        } catch (Exception e) {
            System.out.println("❌ Error loading " + type + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void testResourceDirectory() {
        System.out.println("\n--- Testing Resources Directory ---");
        
        try {
            URL imagesDir = IconDiagnostic.class.getResource("/images");
            if (imagesDir == null) {
                System.out.println("❌ /images directory not found");
                return;
            }
            
            System.out.println("✅ /images directory found: " + imagesDir);
            
            // List files in directory (if possible)
            System.out.println("📁 Available files in /images:");
            
            URL logoUrl = IconDiagnostic.class.getResource("/images/logo.png");
            if (logoUrl != null) {
                System.out.println("  ✅ logo.png");
            } else {
                System.out.println("  ❌ logo.png not found");
            }
            
            URL icoUrl = IconDiagnostic.class.getResource("/images/icon.ico");
            if (icoUrl != null) {
                System.out.println("  ✅ icon.ico");
            } else {
                System.out.println("  ❌ icon.ico not found");
            }
            
            URL pngUrl = IconDiagnostic.class.getResource("/images/icon.png");
            if (pngUrl != null) {
                System.out.println("  ✅ icon.png");
            } else {
                System.out.println("  ❌ icon.png not found");
            }
            
        } catch (Exception e) {
            System.out.println("❌ Error checking resources: " + e.getMessage());
        }
    }
    
    /**
     * Create a test window to display icons
     */
    public static void showIconTestWindow() {
        JFrame frame = new JFrame("Icon Test Window");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(400, 300);
        frame.setLayout(new GridLayout(2, 2, 10, 10));
        
        // Test ICO
        try {
            ImageIcon icoIcon = new ImageIcon(IconDiagnostic.class.getResource("/images/icon.ico"));
            JLabel icoLabel = new JLabel("ICO", icoIcon, SwingConstants.CENTER);
            icoLabel.setToolTipText("ICO: " + icoIcon.getIconWidth() + "x" + icoIcon.getIconHeight());
            frame.add(icoLabel);
        } catch (Exception e) {
            frame.add(new JLabel("ICO: Failed"));
        }
        
        // Test PNG
        try {
            ImageIcon pngIcon = new ImageIcon(IconDiagnostic.class.getResource("/images/icon.png"));
            JLabel pngLabel = new JLabel("PNG", pngIcon, SwingConstants.CENTER);
            pngLabel.setToolTipText("PNG: " + pngIcon.getIconWidth() + "x" + pngIcon.getIconHeight());
            frame.add(pngLabel);
        } catch (Exception e) {
            frame.add(new JLabel("PNG: Failed"));
        }
        
        // Test Logo
        try {
            ImageIcon logoIcon = new ImageIcon(IconDiagnostic.class.getResource("/images/logo.png"));
            JLabel logoLabel = new JLabel("Logo", logoIcon, SwingConstants.CENTER);
            logoLabel.setToolTipText("Logo: " + logoIcon.getIconWidth() + "x" + logoIcon.getIconHeight());
            frame.add(logoLabel);
        } catch (Exception e) {
            frame.add(new JLabel("Logo: Failed"));
        }
        
        // Info
        frame.add(new JLabel("<html><center>Check console<br>for detailed<br>diagnostic info</center></html>"));
        
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
