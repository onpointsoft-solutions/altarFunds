package com.sanctum.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Modern button with hover effects and rounded corners
 */
public class ModernButton extends JButton {
    private Color backgroundColor = new Color(59, 130, 246); // Primary blue
    private Color hoverColor = new Color(37, 99, 235); // Darker blue
    private Color textColor = Color.WHITE;
    private int cornerRadius = 10;
    private boolean drawBorder = false;
    private Color borderColor = new Color(200, 200, 200);
    
    public ModernButton() {
        setupButton();
    }
    
    public ModernButton(String text) {
        super(text);
        setupButton();
    }
    
    public ModernButton(String text, Color backgroundColor) {
        super(text);
        this.backgroundColor = backgroundColor;
        this.hoverColor = backgroundColor.darker();
        setupButton();
    }
    
    private void setupButton() {
        setOpaque(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setForeground(textColor);
        setFont(new Font("Segoe UI", Font.BOLD, 14));
        setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // Add hover effect
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                setBackground(hoverColor);
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                setBackground(backgroundColor);
            }
        });
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Determine background color based on state
        Color bg = backgroundColor;
        if (getModel().isRollover()) {
            bg = hoverColor;
        }
        if (getModel().isPressed()) {
            bg = hoverColor.darker();
        }
        
        // Draw rounded background
        g2.setColor(bg);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), cornerRadius, cornerRadius);
        
        // Draw border if needed
        if (drawBorder) {
            g2.setColor(bg.darker());
            g2.setStroke(new BasicStroke(1));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, cornerRadius, cornerRadius);
        }
        
        g2.dispose();
        super.paintComponent(g);
    }
    
    // Getters and Setters
    public Color getBackgroundColor() { return backgroundColor; }
    public void setBackgroundColor(Color backgroundColor) { 
        this.backgroundColor = backgroundColor; 
        this.hoverColor = backgroundColor.darker();
    }
    
    public Color getHoverColor() { return hoverColor; }
    public void setHoverColor(Color hoverColor) { this.hoverColor = hoverColor; }
    
    public Color getTextColor() { return textColor; }
    public void setTextColor(Color textColor) { this.textColor = textColor; }
    
    public int getCornerRadius() { return cornerRadius; }
    public void setCornerRadius(int cornerRadius) { this.cornerRadius = cornerRadius; }
    
    public boolean isDrawBorder() { return drawBorder; }
    public void setDrawBorder(boolean drawBorder) { this.drawBorder = drawBorder; }
    
    public Color getBorderColor() { return borderColor; }
    public void setBorderColor(Color borderColor) { this.borderColor = borderColor; }
    
    // Static factory methods for common button styles
    public static ModernButton createPrimaryButton(String text) {
        return new ModernButton(text, new Color(59, 130, 246));
    }
    
    public static ModernButton createSuccessButton(String text) {
        return new ModernButton(text, new Color(16, 185, 129));
    }
    
    public static ModernButton createWarningButton(String text) {
        return new ModernButton(text, new Color(245, 158, 11));
    }
    
    public static ModernButton createDangerButton(String text) {
        return new ModernButton(text, new Color(239, 68, 68));
    }
    
    public static ModernButton createSecondaryButton(String text) {
        ModernButton button = new ModernButton(text, Color.WHITE);
        button.setTextColor(new Color(75, 85, 99));
        button.setBorderColor(new Color(209, 213, 219));
        button.setDrawBorder(true);
        return button;
    }
}
