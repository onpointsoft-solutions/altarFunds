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
        JLabel logoLabel = new JLabel("", SwingConstants.CENTER) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int logoSize = Math.min(size.width, size.height) - 16;
                int x = (getWidth()  - logoSize) / 2;
                int y = (getHeight() - logoSize) / 2;

                // Outer glow ring
                g2.setColor(new Color(212, 175, 55, 25));
                g2.fillOval(x - 6, y - 6, logoSize + 12, logoSize + 12);

                // Gold circle background
                g2.setColor(new Color(212, 175, 55, 40));
                g2.fillOval(x, y, logoSize, logoSize);

                // Gold border ring
                g2.setColor(new Color(212, 175, 55));
                g2.setStroke(new java.awt.BasicStroke(2f));
                g2.drawOval(x, y, logoSize, logoSize);

                // ── Church building (gold on dark green) ──────────────
                g2.setColor(new Color(212, 175, 55));

                // Base rectangle
                g2.fillRect(x + logoSize/4, y + logoSize*58/100,
                            logoSize/2,     logoSize*42/100);

                // Roof triangle
                int[] rx = {x + logoSize/8, x + logoSize/2, x + logoSize*7/8};
                int[] ry = {y + logoSize*58/100, y + logoSize*30/100, y + logoSize*58/100};
                g2.fillPolygon(rx, ry, 3);

                // Cross — vertical
                g2.fillRect(x + logoSize/2 - 2, y + logoSize*10/100, 4, logoSize*22/100);
                // Cross — horizontal
                g2.fillRect(x + logoSize/2 - logoSize/9, y + logoSize*18/100,
                            logoSize*2/9, 4);

                // Door cutout (dark so it looks recessed)
                g2.setColor(new Color(14, 46, 42));
                g2.fillRoundRect(x + logoSize/2 - logoSize/12, y + logoSize*72/100,
                                 logoSize/6, logoSize*28/100, 4, 4);

                // Two window cutouts
                g2.fillRect(x + logoSize*3/10, y + logoSize*63/100, logoSize/9, logoSize/9);
                g2.fillRect(x + logoSize*59/100, y + logoSize*63/100, logoSize/9, logoSize/9);

                g2.dispose();
                // Do NOT call super — we drew everything ourselves
            }
        };

        logoLabel.setPreferredSize(size);
        logoLabel.setMinimumSize(size);
        logoLabel.setMaximumSize(size);
        logoLabel.setToolTipText("Place logo.png in src/main/resources/images/");
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
