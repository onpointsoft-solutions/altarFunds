package com.sanctum.util;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.Method;

/**
 * Enhanced dialog manager to prevent minimizing and improve dialog behavior
 */
public class DialogManager {
    
    private static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().contains("windows");
    
    /**
     * Creates a modal dialog with enhanced properties to prevent minimizing
     */
    public static JDialog createModalDialog(Frame parent, String title) {
        JDialog dialog = new JDialog(parent, title, true);
        
        // Enhanced dialog configuration
        configureDialog(dialog);
        
        return dialog;
    }
    
    /**
     * Creates a modal dialog with enhanced properties to prevent minimizing (Window version)
     */
    public static JDialog createModalDialog(Window parent, String title) {
        JDialog dialog = new JDialog(parent, title);
        
        // Enhanced dialog configuration
        configureDialog(dialog);
        
        return dialog;
    }
    
    /**
     * Apply enhanced configuration to dialog to prevent minimizing
     */
    private static void configureDialog(JDialog dialog) {
        // Basic dialog settings
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setResizable(true);
        dialog.setFocusable(true);
        dialog.setAutoRequestFocus(true);
        
        if (IS_WINDOWS) {
            // Windows-specific enhancements
            try {
                // Prevent focus stealing
                dialog.setFocusableWindowState(false);
                
                // Set dialog type to normal (prevents some minimize behaviors)
                dialog.setType(Window.Type.NORMAL);
                
                // Try to set always on top temporarily during initialization
                dialog.setAlwaysOnTop(true);
                
                // Windows-specific properties
                try {
                    // Try to access Windows-specific methods if available
                    Class<?> awtUtilitiesClass = Class.forName("com.sun.awt.AWTUtilities");
                    if (awtUtilitiesClass != null) {
                        Method setWindowOpaque = awtUtilitiesClass.getMethod("setWindowOpaque", Window.class, boolean.class);
                        setWindowOpaque.invoke(null, dialog, false);
                    }
                } catch (Exception e) {
                    // Ignore if AWTUtilities not available
                }
                
            } catch (Exception e) {
                System.err.println("Windows dialog enhancement failed: " + e.getMessage());
            }
        }
        
        // Add window listener to handle focus and minimize events
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                // Ensure dialog stays on top when opened
                dialog.toFront();
                dialog.requestFocus();
                
                // Reset always on top after a short delay (Windows)
                if (IS_WINDOWS) {
                    SwingUtilities.invokeLater(() -> {
                        try {
                            Thread.sleep(100);
                            dialog.setAlwaysOnTop(false);
                            dialog.toFront();
                        } catch (InterruptedException ex) {
                            Thread.currentThread().interrupt();
                        }
                    });
                }
            }
            
            @Override
            public void windowActivated(WindowEvent e) {
                // Ensure dialog comes to front when activated
                dialog.toFront();
                dialog.requestFocus();
            }
            
            @Override
            public void windowDeactivated(WindowEvent e) {
                // Try to prevent minimizing by bringing back to front
                if (IS_WINDOWS && dialog.isVisible()) {
                    SwingUtilities.invokeLater(() -> {
                        try {
                            Thread.sleep(50);
                            if (dialog.isVisible()) {
                                dialog.toFront();
                            }
                        } catch (InterruptedException ex) {
                            Thread.currentThread().interrupt();
                        }
                    });
                }
            }
            
            @Override
            public void windowStateChanged(WindowEvent e) {
                // Handle state changes to prevent minimizing
                if (IS_WINDOWS && e.getNewState() == Frame.ICONIFIED) {
                    SwingUtilities.invokeLater(() -> {
                        // Restore from minimized state
                        //dialog.setExtendedState(Frame.NORMAL);
                        dialog.toFront();
                        dialog.requestFocus();
                    });
                }
            }
        });
    }
    
    /**
     * Show dialog with enhanced display logic to prevent blinking and minimizing
     */
    public static void showDialogEnhanced(JDialog dialog) {
        if (!IS_WINDOWS) {
            dialog.setVisible(true);
            return;
        }
        
        // Windows-specific enhanced show
        SwingUtilities.invokeLater(() -> {
            try {
                // Pre-show configuration
                dialog.setAlwaysOnTop(true);
                
                // Small delay to ensure proper initialization
                Thread.sleep(30);
                
                // Show dialog
                dialog.setVisible(true);
                dialog.toFront();
                dialog.requestFocus();
                
                // Additional delay before removing always on top
                Thread.sleep(100);
                dialog.setAlwaysOnTop(false);
                
                // Final front and focus
                dialog.toFront();
                dialog.requestFocus();
                dialog.repaint();
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }
    
    /**
     * Create a confirmation dialog with enhanced properties
     */
    public static int showConfirmDialog(Component parent, String message, String title, int optionType) {
        // Create custom dialog instead of JOptionPane to have better control
        JDialog dialog = createModalDialog(getParentWindow(parent), title);
        
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Message label
        JLabel messageLabel = new JLabel("<html><div style='width:200px'>" + message + "</div></html>");
        messageLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        panel.add(messageLabel, BorderLayout.CENTER);
        
        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        
        JButton yesButton = new JButton("Yes");
        JButton noButton = new JButton("No");
        JButton cancelButton = new JButton("Cancel");
        
        styleDialogButton(yesButton);
        styleDialogButton(noButton);
        styleDialogButton(cancelButton);
        
        buttonPanel.add(yesButton);
        if (optionType == JOptionPane.YES_NO_OPTION || optionType == JOptionPane.YES_NO_CANCEL_OPTION) {
            buttonPanel.add(noButton);
        }
        if (optionType == JOptionPane.YES_NO_CANCEL_OPTION) {
            buttonPanel.add(cancelButton);
        }
        
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        dialog.setContentPane(panel);
        dialog.pack();
        dialog.setLocationRelativeTo(parent);
        
        // Handle button actions
        final int[] result = {JOptionPane.CANCEL_OPTION};
        
        yesButton.addActionListener(e -> {
            result[0] = JOptionPane.YES_OPTION;
            dialog.dispose();
        });
        
        noButton.addActionListener(e -> {
            result[0] = JOptionPane.NO_OPTION;
            dialog.dispose();
        });
        
        cancelButton.addActionListener(e -> {
            result[0] = JOptionPane.CANCEL_OPTION;
            dialog.dispose();
        });
        
        showDialogEnhanced(dialog);
        
        return result[0];
    }

    public static void showMessageDialog(Component parent, String message, String title, int messageType) {
        Window parentWindow = getParentWindow(parent);
        JDialog dialog = createModalDialog(parentWindow, title);

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        JLabel messageLabel = new JLabel("<html><div style='width:320px'>" + message + "</div></html>");
        messageLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        panel.add(messageLabel, BorderLayout.CENTER);

        JButton okButton = new JButton("OK");
        styleDialogButton(okButton);
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        buttonPanel.add(okButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        okButton.addActionListener(e -> dialog.dispose());

        dialog.setContentPane(panel);
        dialog.pack();
        dialog.setLocationRelativeTo(parent);
        showDialogEnhanced(dialog);
    }
    
    /**
     * Style dialog buttons consistently
     */
    private static void styleDialogButton(JButton button) {
        button.setPreferredSize(new Dimension(80, 30));
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setBackground(new Color(70, 130, 180));
        button.setForeground(Color.WHITE);
        button.setFont(new Font("SansSerif", Font.BOLD, 12));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }
    
    /**
     * Get parent window from component
     */
    private static Window getParentWindow(Component component) {
        if (component == null) {
            return null;
        }
        
        Component parent = component.getParent();
        while (parent != null && !(parent instanceof Window)) {
            parent = parent.getParent();
        }
        
        return (Window) parent;
    }
}
