package com.sanctum.util;

import javax.swing.JDialog;
import java.awt.Window;
import java.lang.reflect.Method;

/**
 * Windows-specific utilities to fix dialog blinking issues
 */
public class WindowsDialogFix {
    
    private static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().contains("windows");
    
    /**
     * Apply Windows-specific fixes to prevent dialog blinking
     */
    public static void fixDialogBlinking(JDialog dialog) {
        if (!IS_WINDOWS) {
            return;
        }
        
        try {
            // Disable focus stealing prevention on Windows
            dialog.setFocusableWindowState(false);
            dialog.setAutoRequestFocus(false);
            
            // Set Windows-specific properties
            dialog.setAlwaysOnTop(false);
            dialog.setType(java.awt.Window.Type.NORMAL);
            
        } catch (Exception e) {
            System.err.println("Windows dialog fix failed: " + e.getMessage());
        }
    }
    
    /**
     * Show dialog with Windows-specific timing to prevent blinking
     */
    public static void showDialogSmoothly(JDialog dialog) {
        if (!IS_WINDOWS) {
            dialog.setVisible(true);
            return;
        }
        
        // Windows-specific delayed show to prevent blinking
        javax.swing.SwingUtilities.invokeLater(() -> {
            try {
                Thread.sleep(50); // Small delay for Windows
                dialog.setVisible(true);
                dialog.toFront();
                dialog.repaint();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }
}
