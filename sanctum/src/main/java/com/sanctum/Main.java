package com.sanctum;

import com.sanctum.config.DatabaseConfig;
import com.sanctum.security.JwtUtil;
import com.sanctum.service.AuthService;
import com.sanctum.view.LoginFrame;
import com.sanctum.view.MainFrame;
import com.sanctum.util.IconDiagnostic;

import javax.swing.*;
import javax.imageio.ImageIO;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.awt.image.BufferedImage;
import java.awt.Image;
import java.io.InputStream;

/**
 * Main entry point for Sanctum Church Management System
 */
public class Main {
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());
    
    public static void main(String[] args) {
        // Run icon diagnostic first
        System.out.println("Running icon diagnostic...");
        IconDiagnostic.diagnoseIcons();
        
        // Set system Look and Feel
        try {
            // Use cross-platform Look and Feel to preserve custom styling
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Could not set cross-platform look and feel", e);
        }

        // Set application icon
        setApplicationIcon();

        // Initialize database
        try {
            DatabaseConfig.initializeDatabase();
            LOGGER.info("Database initialized successfully");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize database", e);
            JOptionPane.showMessageDialog(null, 
                "Failed to initialize database: " + e.getMessage(), 
                "Database Error", 
                JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        // Start application
        SwingUtilities.invokeLater(() -> {
            LoginFrame loginFrame = new LoginFrame();
            loginFrame.setVisible(true);
        });
    }
    
    /**
     * Sets the application icon for all windows using PNG for better compatibility
     */
    private static void setApplicationIcon() {
        try {
            // Try PNG first (better Java compatibility)
            Image iconImage = loadIconFromResources("/images/icon.png");
            
            if (iconImage != null) {
                // Set icon for all future windows
                JFrame.setDefaultLookAndFeelDecorated(true);
                UIManager.put("Frame.icon", iconImage);
                LOGGER.info("Application PNG icon loaded successfully - Size: " + iconImage.getWidth(null) + "x" + iconImage.getHeight(null));
            } else {
                LOGGER.warning("PNG icon failed to load, trying ICO fallback");
                iconImage = loadIconFromResources("/images/icon.ico");
                
                if (iconImage != null) {
                    JFrame.setDefaultLookAndFeelDecorated(true);
                    UIManager.put("Frame.icon", iconImage);
                    LOGGER.info("Application ICO icon loaded successfully as fallback - Size: " + iconImage.getWidth(null) + "x" + iconImage.getHeight(null));
                } else {
                    LOGGER.warning("Both PNG and ICO fallback failed - using default icon");
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to set application icon", e);
        }
    }
    
    /**
     * Load icon image from resources using ImageIO for better format support
     */
    private static Image loadIconFromResources(String path) {
        try {
            InputStream inputStream = Main.class.getResourceAsStream(path);
            if (inputStream == null) {
                LOGGER.warning("Resource not found: " + path);
                return null;
            }
            
            // Use ImageIO to read the image (better for ICO files)
            BufferedImage bufferedImage = ImageIO.read(inputStream);
            inputStream.close();
            
            if (bufferedImage != null) {
                LOGGER.info("Successfully loaded image from " + path + " - Size: " + bufferedImage.getWidth() + "x" + bufferedImage.getHeight());
                return bufferedImage;
            } else {
                LOGGER.warning("Failed to read image from " + path);
                return null;
            }
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error loading image from " + path, e);
            return null;
        }
    }
}
