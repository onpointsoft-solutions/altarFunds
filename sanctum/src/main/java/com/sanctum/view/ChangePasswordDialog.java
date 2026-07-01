package com.sanctum.view;

import com.sanctum.api.SanctumApiClient;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Modal dialog for changing the currently-logged-in user's password.
 *
 * API call: POST /api/accounts/password/change/
 * Body:     { "current_password": "...",
 *             "new_password": "...",
 *             "new_password_confirm": "..." }
 * Auth:     Bearer token (user must be logged in)
 *
 * This dialog is role-agnostic and can be opened from any dashboard by
 * calling the static helper:
 *
 *     ChangePasswordDialog.show(parentFrame);
 */
public class ChangePasswordDialog extends JDialog {

    // ── Brand colours (matches all dashboard frames) ──────────────────────
    private static final Color C_BG        = new Color(14,  46,  42);
    private static final Color C_SURFACE   = new Color(19,  58,  54);
    private static final Color C_CARD      = new Color(28,  47,  44);
    private static final Color C_BORDER    = new Color(42,  74,  69);
    private static final Color C_GOLD      = new Color(212, 175,  55);
    private static final Color C_GOLD_DIM  = new Color(212, 175,  55, 30);
    private static final Color C_TEXT      = new Color(255, 255, 255);
    private static final Color C_TEXT_MID  = new Color(207, 207, 207);
    private static final Color C_TEXT_DIM  = new Color(156, 163, 175);
    private static final Color C_ERROR     = new Color(244,  67,  54);
    private static final Color C_SUCCESS   = new Color( 76, 175,  80);
    private static final Color C_WARN      = new Color(255, 165,   0);

    // ── Fonts ─────────────────────────────────────────────────────────────
    private static final Font F_TITLE  = new Font("Segoe UI",       Font.BOLD,  17);
    private static final Font F_LABEL  = new Font("Segoe UI",       Font.BOLD,  12);
    private static final Font F_FIELD  = new Font("Segoe UI",       Font.PLAIN, 13);
    private static final Font F_SMALL  = new Font("Segoe UI",       Font.PLAIN, 11);
    private static final Font F_BTN    = new Font("Segoe UI",       Font.BOLD,  13);
    private static final Font F_MONO   = new Font("JetBrains Mono", Font.PLAIN, 11);

    // ── Widgets ───────────────────────────────────────────────────────────
    private JPasswordField currentPwField;
    private JPasswordField newPwField;
    private JPasswordField confirmPwField;

    private JProgressBar   strengthBar;
    private JLabel         strengthLabel;
    private JLabel         statusLabel;
    private JButton        saveBtn;
    private JButton        cancelBtn;

    // ── Constructor ───────────────────────────────────────────────────────

    public ChangePasswordDialog(Frame owner) {
        super(owner, "Change Password", true);
        buildUI();
        wireEvents();
        // Use a fixed generous size so all fields are fully visible
        setSize(500, 560);
        setMinimumSize(new Dimension(460, 500));
        setResizable(true);
        setLocationRelativeTo(owner);
    }

    // ── Static convenience opener ─────────────────────────────────────────

    /**
     * Open the dialog from any frame.
     *
     * @param owner the parent JFrame (used to centre the dialog)
     */
    public static void show(Frame owner) {
        new ChangePasswordDialog(owner).setVisible(true);
    }

    // ── UI construction ───────────────────────────────────────────────────

    private void buildUI() {
        // Dark dialog background
        JPanel root = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(C_BG);
                g2.fillRect(0, 0, getWidth(), getHeight());
                // gold top accent line
                g2.setColor(C_GOLD);
                g2.fillRect(0, 0, getWidth(), 3);
                g2.dispose();
            }
        };
        root.setOpaque(false);
        root.setBorder(new EmptyBorder(24, 32, 24, 32));
        // No fixed preferred size — let the dialog size drive layout

        root.add(buildHeader(),  BorderLayout.NORTH);
        root.add(buildForm(),    BorderLayout.CENTER);
        root.add(buildFooter(),  BorderLayout.SOUTH);

        getContentPane().setBackground(C_BG);
        getContentPane().add(root);
    }

    private JPanel buildHeader() {
        JPanel p = new JPanel(new BorderLayout(10, 0));
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(0, 0, 18, 0));

        JLabel icon = new JLabel("🔑");
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 28));

        JPanel text = new JPanel(new BorderLayout(0, 3));
        text.setOpaque(false);
        JLabel title = new JLabel("Change Password");
        title.setFont(F_TITLE);
        title.setForeground(C_GOLD);

        JLabel sub = new JLabel("Enter your current password and choose a new one.");
        sub.setFont(F_SMALL);
        sub.setForeground(C_TEXT_DIM);

        text.add(title, BorderLayout.NORTH);
        text.add(sub,   BorderLayout.CENTER);

        p.add(icon, BorderLayout.WEST);
        p.add(text, BorderLayout.CENTER);
        return p;
    }

    private JPanel buildForm() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        // ── Current password ──────────────────────────────────────────
        p.add(fieldLabel("CURRENT PASSWORD"));
        p.add(Box.createVerticalStrut(5));
        currentPwField = makePasswordField("Enter your current password");
        p.add(currentPwField);

        p.add(Box.createVerticalStrut(14));

        // ── New password ──────────────────────────────────────────────
        p.add(fieldLabel("NEW PASSWORD  (min 12 characters)"));
        p.add(Box.createVerticalStrut(5));
        newPwField = makePasswordField("At least 12 characters");
        p.add(newPwField);

        // Strength bar
        p.add(Box.createVerticalStrut(5));
        strengthBar = new JProgressBar(0, 100);
        strengthBar.setStringPainted(false);
        strengthBar.setForeground(C_ERROR);
        strengthBar.setBackground(C_BORDER);
        strengthBar.setBorderPainted(false);
        strengthBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 5));
        strengthBar.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(strengthBar);

        p.add(Box.createVerticalStrut(3));
        strengthLabel = new JLabel(" ");
        strengthLabel.setFont(F_SMALL);
        strengthLabel.setForeground(C_TEXT_DIM);
        strengthLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(strengthLabel);

        p.add(Box.createVerticalStrut(14));

        // ── Confirm new password ──────────────────────────────────────
        p.add(fieldLabel("CONFIRM NEW PASSWORD"));
        p.add(Box.createVerticalStrut(5));
        confirmPwField = makePasswordField("Repeat new password");
        p.add(confirmPwField);

        // Password requirements hint
        p.add(Box.createVerticalStrut(12));
        JLabel hint = new JLabel("<html><span style='color:#9ca3af;font-family:monospace;font-size:10px;'>"
            + "Requirements: 12+ chars · uppercase · lowercase · number · symbol</span></html>");
        hint.setFont(F_MONO);
        hint.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(hint);

        p.add(Box.createVerticalStrut(14));

        // Status message
        statusLabel = new JLabel(" ");
        statusLabel.setFont(F_SMALL);
        statusLabel.setForeground(C_TEXT_MID);
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(statusLabel);

        return p;
    }

    private JPanel buildFooter() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(16, 0, 0, 0));

        cancelBtn = makeOutlineButton("Cancel", C_TEXT_DIM);
        saveBtn   = makePrimaryButton("Save Password");

        p.add(cancelBtn);
        p.add(saveBtn);
        return p;
    }

    // ── Event wiring ──────────────────────────────────────────────────────

    private void wireEvents() {
        // Live strength meter as user types
        newPwField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            private void update() { updateStrengthMeter(); }
            public void insertUpdate(javax.swing.event.DocumentEvent e)  { update(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e)  { update(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { update(); }
        });

        saveBtn.addActionListener(e -> attemptChange());
        cancelBtn.addActionListener(e -> dispose());

        // Enter submits
        KeyAdapter enter = new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) attemptChange();
            }
        };
        currentPwField.addKeyListener(enter);
        newPwField.addKeyListener(enter);
        confirmPwField.addKeyListener(enter);

        // ESC cancels
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "esc");
        getRootPane().getActionMap().put("esc", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { dispose(); }
        });
    }

    // ── Change-password action ─────────────────────────────────────────────

    private void attemptChange() {
        String currentPw  = new String(currentPwField.getPassword());
        String newPw       = new String(newPwField.getPassword());
        String confirmPw   = new String(confirmPwField.getPassword());

        // ── Client-side validation ────────────────────────────────────
        if (currentPw.isEmpty()) {
            showError("Please enter your current password.");
            currentPwField.requestFocusInWindow();
            return;
        }
        if (newPw.length() < 12) {
            showError("New password must be at least 12 characters.");
            newPwField.requestFocusInWindow();
            return;
        }
        if (scorePassword(newPw) < 34) {
            showError("Password too weak — add uppercase, numbers, or symbols.");
            newPwField.requestFocusInWindow();
            return;
        }
        if (!newPw.equals(confirmPw)) {
            showError("Passwords do not match.");
            confirmPwField.requestFocusInWindow();
            return;
        }
        if (newPw.equals(currentPw)) {
            showError("New password must differ from your current password.");
            newPwField.requestFocusInWindow();
            return;
        }

        setLoading(true);

        SanctumApiClient.changePassword(currentPw, newPw, confirmPw)
            .thenAccept(result -> SwingUtilities.invokeLater(() -> {
                setLoading(false);
                if (result.success) {
                    showSuccess("Password changed successfully!");
                    // Clear fields for security
                    currentPwField.setText("");
                    newPwField.setText("");
                    confirmPwField.setText("");
                    // Close dialog after a short pause
                    Timer t = new Timer(1800, e -> dispose());
                    t.setRepeats(false);
                    t.start();
                } else {
                    String msg = result.message != null ? result.message
                                                        : "Failed — please check your current password.";
                    showError(msg);
                }
            }))
            .exceptionally(ex -> {
                SwingUtilities.invokeLater(() -> {
                    setLoading(false);
                    showError("Network error. Please check your connection and try again.");
                });
                return null;
            });
    }

    // ── Strength meter ─────────────────────────────────────────────────────

    private void updateStrengthMeter() {
        String pw = new String(newPwField.getPassword());
        int score = scorePassword(pw);
        strengthBar.setValue(score);

        if (pw.isEmpty()) {
            strengthBar.setForeground(C_BORDER);
            strengthLabel.setText(" ");
        } else if (score < 34) {
            strengthBar.setForeground(C_ERROR);
            strengthLabel.setText("Weak");
            strengthLabel.setForeground(C_ERROR);
        } else if (score < 67) {
            strengthBar.setForeground(C_WARN);
            strengthLabel.setText("Fair — add more variety");
            strengthLabel.setForeground(C_WARN);
        } else {
            strengthBar.setForeground(C_SUCCESS);
            strengthLabel.setText("Strong ✓");
            strengthLabel.setForeground(C_SUCCESS);
        }
    }

    /** Simple 0–100 password strength score. */
    private int scorePassword(String pw) {
        if (pw == null || pw.isEmpty()) return 0;
        int s = 0;
        if (pw.length() >= 8)                       s += 20;
        if (pw.length() >= 12)                      s += 20;
        if (pw.length() >= 16)                      s += 10;
        if (pw.matches(".*[A-Z].*"))                s += 15;
        if (pw.matches(".*[a-z].*"))                s += 10;
        if (pw.matches(".*\\d.*"))                  s += 15;
        if (pw.matches(".*[^A-Za-z0-9].*"))         s += 10;
        return Math.min(s, 100);
    }

    // ── State helpers ─────────────────────────────────────────────────────

    private void setLoading(boolean loading) {
        saveBtn.setEnabled(!loading);
        cancelBtn.setEnabled(!loading);
        currentPwField.setEnabled(!loading);
        newPwField.setEnabled(!loading);
        confirmPwField.setEnabled(!loading);
        if (loading) {
            statusLabel.setText("Updating password…");
            statusLabel.setForeground(C_GOLD);
        }
    }

    private void showError(String msg) {
        statusLabel.setText("✗  " + msg);
        statusLabel.setForeground(C_ERROR);
    }

    private void showSuccess(String msg) {
        statusLabel.setText("✓  " + msg);
        statusLabel.setForeground(C_SUCCESS);
    }

    // ── Widget factories ──────────────────────────────────────────────────

    private JLabel fieldLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("JetBrains Mono", Font.BOLD, 10));
        lbl.setForeground(C_TEXT_DIM);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }

    private JPasswordField makePasswordField(String tooltip) {
        JPasswordField f = new JPasswordField();
        f.setFont(F_FIELD);
        f.setForeground(C_TEXT);
        f.setBackground(C_CARD);
        f.setCaretColor(C_GOLD);
        f.setToolTipText(tooltip);
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        f.setAlignmentX(Component.LEFT_ALIGNMENT);
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(C_BORDER),
            BorderFactory.createEmptyBorder(9, 12, 9, 12)
        ));
        f.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                f.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(C_GOLD, 2),
                    BorderFactory.createEmptyBorder(8, 11, 8, 11)
                ));
            }
            public void focusLost(FocusEvent e) {
                f.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(C_BORDER),
                    BorderFactory.createEmptyBorder(9, 12, 9, 12)
                ));
            }
        });
        return f;
    }

    private JButton makePrimaryButton(String text) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean hover   = getModel().isRollover();
                boolean pressed = getModel().isPressed();
                boolean enabled = isEnabled();

                Color fill = enabled
                    ? (pressed ? new Color(212, 175, 55, 60)
                               : hover ? new Color(212, 175, 55, 40)
                                       : new Color(212, 175, 55, 22))
                    : new Color(212, 175, 55, 10);

                g2.setColor(fill);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);

                g2.setColor(enabled ? C_GOLD : C_TEXT_DIM);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);

                g2.setFont(F_BTN);
                g2.setColor(enabled ? C_GOLD : C_TEXT_DIM);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(text,
                    (getWidth() - fm.stringWidth(text)) / 2,
                    (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        };
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(148, 40));
        return btn;
    }

    private JButton makeOutlineButton(String text, Color fg) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isRollover()) {
                    g2.setColor(new Color(255, 255, 255, 15));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                }
                g2.setColor(new Color(fg.getRed(), fg.getGreen(), fg.getBlue(), 100));
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.setFont(F_BTN);
                g2.setColor(fg);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(text,
                    (getWidth() - fm.stringWidth(text)) / 2,
                    (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        };
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(100, 40));
        return btn;
    }
}
