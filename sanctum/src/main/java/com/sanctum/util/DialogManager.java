package com.sanctum.util;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Dialog manager for Sanctum.
 *
 * Previous version tried to stop the (full-screen-exclusive) main frame from
 * minimizing by forcing setAlwaysOnTop on/off, blocking the EDT with
 * Thread.sleep(), and disabling the dialog's focusable-window-state. That
 * actively caused the problems it was trying to fix:
 *   - Thread.sleep() on the EDT froze the whole UI for ~100-150ms every time
 *     a dialog opened.
 *   - setFocusableWindowState(false) meant dialogs could never receive
 *     keyboard focus, so text fields inside them couldn't be typed into.
 *   - Fighting focus changes with toFront() on windowDeactivated() caused
 *     visible flicker and stole focus back from the user.
 *
 * The actual minimizing was caused by the main frame using OS-level
 * full-screen-exclusive mode (GraphicsDevice#setFullScreenWindow), which
 * Windows breaks (iconifying the frame) whenever another top-level window
 * gets focus. That's fixed at the source in TreasurerDashboardFrame by using
 * a normal maximized undecorated frame instead. This class no longer needs
 * to compensate for it.
 */
public class DialogManager {

    /** Creates a modal dialog with sane, non-hacky defaults. */
    public static JDialog createModalDialog(Frame parent, String title) {
        JDialog dialog = new JDialog(parent, title, true);
        configureDialog(dialog);
        return dialog;
    }

    /** Creates a modal dialog with sane, non-hacky defaults (Window owner). */
    public static JDialog createModalDialog(Window parent, String title) {
        JDialog dialog = new JDialog(parent, title, Dialog.ModalityType.APPLICATION_MODAL);
        configureDialog(dialog);
        return dialog;
    }

    private static void configureDialog(JDialog dialog) {
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setResizable(true);
        // Let it behave like a normal window: focusable, no always-on-top
        // tricks. This is what actually lets text fields receive input.
        dialog.addWindowListener(new WindowAdapter() {
            @Override public void windowOpened(WindowEvent e) {
                dialog.requestFocusInWindow();
            }
        });
    }

    /**
     * Show a dialog. No sleeps, no always-on-top juggling — just shows it.
     * Safe to call from the EDT (the normal case) or off it.
     */
    public static void showDialogEnhanced(JDialog dialog) {
        if (SwingUtilities.isEventDispatchThread()) {
            dialog.setVisible(true);
        } else {
            SwingUtilities.invokeLater(() -> dialog.setVisible(true));
        }
    }

    /**
     * Confirmation dialog with Yes/No/Cancel buttons styled to match the app.
     */
    public static int showConfirmDialog(Component parent, String message, String title, int optionType) {
        Window owner = getParentWindow(parent);
        JDialog dialog = createModalDialog(owner, title);

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel messageLabel = new JLabel("<html><div style='width:240px'>" + message + "</div></html>");
        messageLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        panel.add(messageLabel, BorderLayout.CENTER);

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

        final int[] result = {JOptionPane.CANCEL_OPTION};
        yesButton.addActionListener(e -> { result[0] = JOptionPane.YES_OPTION; dialog.dispose(); });
        noButton.addActionListener(e -> { result[0] = JOptionPane.NO_OPTION; dialog.dispose(); });
        cancelButton.addActionListener(e -> { result[0] = JOptionPane.CANCEL_OPTION; dialog.dispose(); });
        dialog.getRootPane().setDefaultButton(yesButton);

        dialog.setVisible(true); // blocks until disposed (modal) — fine, no sleeps involved
        return result[0];
    }

    /**
     * Simple OK message dialog.
     */
    public static void showMessageDialog(Component parent, String message, String title, int messageType) {
        Window owner = getParentWindow(parent);
        JDialog dialog = createModalDialog(owner, title);

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
        dialog.getRootPane().setDefaultButton(okButton);

        dialog.setContentPane(panel);
        dialog.pack();
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
    }

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

    private static Window getParentWindow(Component component) {
        if (component == null) return null;
        Component parent = component.getParent();
        while (parent != null && !(parent instanceof Window)) {
            parent = parent.getParent();
        }
        return (Window) parent;
    }
}