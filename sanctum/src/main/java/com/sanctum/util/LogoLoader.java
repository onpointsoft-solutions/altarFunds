package com.sanctum.util;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;

/**
 * Utility class for loading and managing application logos
 */
public class LogoLoader {
    
    private static final String LOGO_PATH = "/images/logo.png";
    private static final String FALLBACK_LOGO_PATH = "/images/sanctum-logo.png";
    private static Image cachedLogo = null;
    
    /**
     * Loads the application logo from resources
     * @return Image object or null if not found
     */
    public static Image loadLogo() {
        if (cachedLogo != null) {
            return cachedLogo;
        }
        
        try {
            // Try primary logo path first
            InputStream logoStream = LogoLoader.class.getResourceAsStream(LOGO_PATH);
            if (logoStream != null) {
                cachedLogo = ImageIO.read(logoStream);
                logoStream.close();
                return cachedLogo;
            }
            
            // Try fallback logo path
            logoStream = LogoLoader.class.getResourceAsStream(FALLBACK_LOGO_PATH);
            if (logoStream != null) {
                cachedLogo = ImageIO.read(logoStream);
                logoStream.close();
                return cachedLogo;
            }
            
            System.out.println("Logo not found at: " + LOGO_PATH + " or " + FALLBACK_LOGO_PATH);
            
        } catch (IOException e) {
            System.err.println("Error loading logo: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Creates a JLabel with the loaded logo
     * @param size preferred size for the logo
     * @return JLabel with logo or fallback
     */
    public static JLabel createLogoLabel(Dimension size) {
        Image logo = loadLogo();
        
        if (logo != null) {
            // Scale logo to fit size
            Image scaledLogo = logo.getScaledInstance(
                size.width, size.height, Image.SCALE_SMOOTH);
            
            JLabel logoLabel = new JLabel(new ImageIcon(scaledLogo));
            logoLabel.setPreferredSize(size);
            logoLabel.setMaximumSize(size);
            logoLabel.setMinimumSize(size);
            return logoLabel;
        } else {
            // Return fallback text-based logo
            return createFallbackLogo(size);
        }
    }
    
    /**
     * Creates a fallback text-based logo when image is not available
     * @param size preferred size
     * @return JLabel with text logo
     */
    private static JLabel createFallbackLogo(Dimension size) {
        JLabel logoLabel = new JLabel("🏛️", SwingConstants.CENTER) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Draw custom church logo
                int logoSize = Math.min(size.width, size.height) - 20;
                int x = (getWidth() - logoSize) / 2;
                int y = (getHeight() - logoSize) / 2;
                
                // Church building
                g2.setColor(new Color(0, 212, 255));
                
                // Base
                g2.fillRect(x + logoSize/4, y + logoSize*2/3, logoSize/2, logoSize/3);
                
                // Roof
                int[] roofX = {x, x + logoSize/2, x + logoSize};
                int[] roofY = {y + logoSize*2/3, y + logoSize/3, y + logoSize*2/3};
                g2.fillPolygon(roofX, roofY, 3);
                
                // Cross
                g2.fillRect(x + logoSize/2 - 2, y + logoSize/4, 4, logoSize/3);
                g2.fillRect(x + logoSize/2 - logoSize/8, y + logoSize/3 - 2, logoSize/4, 4);
                
                // Door
                g2.setColor(new Color(10, 14, 26));
                g2.fillRect(x + logoSize/2 - 8, y + logoSize*3/4, 16, logoSize/4);
                
                // Windows
                g2.fillRect(x + logoSize/4 + 4, y + logoSize/2 + 4, 8, 8);
                g2.fillRect(x + 3*logoSize/4 - 12, y + logoSize/2 + 4, 8, 8);
                
                // Glow effect
                g2.setColor(new Color(0, 212, 255, 30));
                g2.fillRoundRect(x - 5, y - 5, logoSize + 10, logoSize + 10, 15, 15);
                
                g2.dispose();
            }
        };
        
        logoLabel.setFont(new Font("Monospaced", Font.BOLD, 48));
        logoLabel.setForeground(new Color(0, 212, 255));
        logoLabel.setPreferredSize(size);
        logoLabel.setToolTipText("Place your logo.png in src/main/resources/images/");
        
        return logoLabel;
    }
    
    /**
     * Clears cached logo (useful for theme changes or updates)
     */
    public static void clearCache() {
        cachedLogo = null;
    }
    
    /**
     * Checks if logo file exists
     * @return true if logo file is available
     */
    public static boolean hasLogo() {
        return loadLogo() != null;
    }
}
