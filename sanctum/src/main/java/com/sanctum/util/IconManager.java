package com.sanctum.util;

import javax.swing.*;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.util.HashMap;
import java.util.Map;

/**
 * IconManager - A lasting solution for managing emoji and icon rendering
 * Provides consistent icon rendering across the application with fallback support
 */
public class IconManager {
    
    // Icon cache for performance
    private static final Map<String, Icon> iconCache = new HashMap<>();
    
    // Default icon colors
    private static final Color DEFAULT_COLOR = new Color(212, 175, 55); // Gold
    private static final Color SUCCESS_COLOR = new Color(76, 175, 80);  // Green
    private static final Color DANGER_COLOR = new Color(244, 67, 54);   // Red
    private static final Color INFO_COLOR = new Color(33, 150, 243);    // Blue
    
    // Icon size constants
    public static final int SIZE_SMALL = 16;
    public static final int SIZE_MEDIUM = 24;
    public static final int SIZE_LARGE = 32;
    public static final int SIZE_EXTRA_LARGE = 48;
    
    /**
     * Get an icon with the specified emoji/character and size
     */
    public static Icon getIcon(String emoji, int size) {
        return getIcon(emoji, size, DEFAULT_COLOR);
    }
    
    /**
     * Get an icon with the specified emoji/character, size, and color
     */
    public static Icon getIcon(String emoji, int size, Color color) {
        String key = emoji + "_" + size + "_" + color.getRGB();
        
        if (iconCache.containsKey(key)) {
            return iconCache.get(key);
        }
        
        Icon icon = new EmojiIcon(emoji, size, color);
        iconCache.put(key, icon);
        return icon;
    }
    
    /**
     * Get a themed icon based on the icon type
     */
    public static Icon getThemedIcon(String iconType, int size) {
        switch (iconType.toLowerCase()) {
            case "announcement":
            case "announcements":
                return getIcon("📢", size, DEFAULT_COLOR);
            case "staff":
            case "user":
            case "people":
                return getIcon("👨‍💼", size, DEFAULT_COLOR);
            case "member":
            case "members":
                return getIcon("👥", size, DEFAULT_COLOR);
            case "donation":
            case "money":
            case "giving":
                return getIcon("💰", size, DEFAULT_COLOR);
            case "event":
            case "calendar":
                return getIcon("📅", size, DEFAULT_COLOR);
            case "settings":
            case "config":
                return getIcon("⚙️", size, DEFAULT_COLOR);
            case "success":
            case "check":
                return getIcon("✅", size, SUCCESS_COLOR);
            case "error":
            case "danger":
                return getIcon("❌", size, DANGER_COLOR);
            case "info":
            case "help":
                return getIcon("ℹ️", size, INFO_COLOR);
            case "warning":
                return getIcon("⚠️", size, new Color(255, 152, 0));
            case "loading":
            case "progress":
                return getIcon("⏳", size, DEFAULT_COLOR);
            case "save":
            case "download":
                return getIcon("💾", size, DEFAULT_COLOR);
            case "edit":
            case "modify":
                return getIcon("✏️", size, DEFAULT_COLOR);
            case "delete":
            case "remove":
                return getIcon("🗑️", size, DANGER_COLOR);
            case "email":
            case "mail":
                return getIcon("📧", size, DEFAULT_COLOR);
            case "phone":
            case "call":
                return getIcon("📞", size, DEFAULT_COLOR);
            case "prayer":
                return getIcon("🙏", size, DEFAULT_COLOR);
            case "devotional":
            case "bible":
                return getIcon("📖", size, DEFAULT_COLOR);
            case "counseling":
            case "therapy":
                return getIcon("💬", size, DEFAULT_COLOR);
            case "church":
            case "building":
                return getIcon("⛪", size, DEFAULT_COLOR);
            case "music":
            case "worship":
                return getIcon("🎵", size, DEFAULT_COLOR);
            case "youth":
            case "young":
                return getIcon("👦", size, DEFAULT_COLOR);
            case "outreach":
            case "mission":
                return getIcon("🌍", size, DEFAULT_COLOR);
            default:
                return getIcon("📄", size, DEFAULT_COLOR); // Default document icon
        }
    }
    
    /**
     * Create a JLabel with an icon
     */
    public static JLabel createIconLabel(String iconType, int size) {
        JLabel label = new JLabel(getThemedIcon(iconType, size));
        label.setOpaque(false);
        return label;
    }
    
    /**
     * Create a JLabel with an icon and text
     */
    public static JLabel createIconLabel(String iconType, int size, String text) {
        JLabel label = new JLabel(text, getThemedIcon(iconType, size), SwingConstants.LEFT);
        label.setOpaque(false);
        return label;
    }
    
    /**
     * Create a JButton with an icon
     */
    public static JButton createIconButton(String iconType, int size, String text) {
        JButton button = new JButton(text, getThemedIcon(iconType, size));
        button.setOpaque(false);
        return button;
    }
    
    /**
     * Custom Icon implementation for emoji rendering
     */
    private static class EmojiIcon implements Icon {
        private final String emoji;
        private final int size;
        private final Color color;
        
        public EmojiIcon(String emoji, int size, Color color) {
            this.emoji = emoji;
            this.size = size;
            this.color = color;
        }
        
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            
            // Set up font for emoji rendering
            Font font = getEmojiFont(size);
            g2.setFont(font);
            g2.setColor(color);
            
            // Draw the emoji centered in the icon bounds
            FontRenderContext frc = new FontRenderContext(new AffineTransform(), true, true);
            GlyphVector gv = font.createGlyphVector(frc, emoji);
            
            int textWidth = (int) gv.getVisualBounds().getWidth();
            int textHeight = (int) gv.getVisualBounds().getHeight();
            
            int centerX = x + (size - textWidth) / 2;
            int centerY = y + (size - textHeight) / 2 + (int) (textHeight * 0.8); // Adjust for baseline
            
            g2.drawString(emoji, centerX, centerY);
            g2.dispose();
        }
        
        private Font getEmojiFont(int size) {
            // Try different fonts that support emoji
            String[] emojiFonts = {
                "Segoe UI Emoji",
                "Apple Color Emoji",
                "Noto Color Emoji",
                "Twemoji Mozilla",
                "EmojiOne Mozilla",
                "Arial Unicode MS"
            };
            
            for (String fontName : emojiFonts) {
                Font font = new Font(fontName, Font.PLAIN, size);
                if (font.canDisplayUpTo(emoji) == -1) {
                    return font;
                }
            }
            
            // Fallback to default font
            return new Font(Font.SANS_SERIF, Font.PLAIN, size);
        }
        
        @Override
        public int getIconWidth() {
            return size;
        }
        
        @Override
        public int getIconHeight() {
            return size;
        }
    }
    
    /**
     * Clear the icon cache (useful for testing or memory management)
     */
    public static void clearCache() {
        iconCache.clear();
    }
    
    /**
     * Get cache statistics
     */
    public static String getCacheStats() {
        return "Icon cache contains " + iconCache.size() + " cached icons";
    }
}
