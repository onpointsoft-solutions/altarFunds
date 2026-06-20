package com.sanctum.view;

import com.sanctum.api.SanctumApiClient;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.concurrent.CompletableFuture;

/**
 * Two-step password reset panel.
 *
 * Step 1 – Enter email  → calls POST /api/accounts/password/reset/
 *           Backend sends a reset token to the user's email.
 *
 * Step 2 – Enter token + new password → calls POST /api/accounts/password/reset/confirm/
 *           On success the user is taken back to LoginFrame.
 */
public class ForgotPasswordFrame extends JFrame {

    // ── Brand colours (matches LoginFrame) ──────────────────────────────────
    private static final Color C_BG         = new Color(14,  46,  42);
    private static final Color C_SURFACE    = new Color(19,  58,  54);
    private static final Color C_CARD       = new Color(28,  47,  44);
    private static final Color C_BORDER     = new Color(42,  74,  69);
    private static final Color C_BORDER_LT  = new Color(66, 115, 107);
    private static final Color C_GOLD       = new Color(212, 175,  55);
    private static final Color C_GOLD_HOVER = new Color(230, 199, 102);
    private static final Color C_TEXT       = new Color(255, 255, 255);
    private static final Color C_TEXT_MID   = new Color(207, 207, 207);
    private static final Color C_TEXT_DIM   = new Color(156, 163, 175);
    private static final Color C_ERROR      = new Color(244,  67,  54);
    private static final Color C_SUCCESS    = new Color( 76, 175,  80);

    // ── Fonts ────────────────────────────────────────────────────────────────
    private static final Font FONT_TITLE  = new Font("Monospaced", Font.BOLD,  22);
    private static final Font FONT_LABEL  = new Font("Monospaced", Font.BOLD,  11);
    private static final Font FONT_VALUE  = new Font("Monospaced", Font.BOLD,  15);
    private static final Font FONT_SMALL  = new Font("Monospaced", Font.PLAIN, 11);
    private static final Font FONT_BUTTON = new Font("Monospaced", Font.BOLD,  13);

    // ── State ────────────────────────────────────────────────────────────────
    /** Which step of the reset flow we are on (1 = email, 2 = token + new pw). */
    private int step = 1;

    // Step-1 widgets
    private JTextField  emailField;
    private JButton     requestResetButton;

    // Step-2 widgets
    private JTextField     tokenField;
    private JPasswordField newPasswordField;
    private JPasswordField confirmPasswordField;
    private JButton        confirmResetButton;

    // Shared
    private JLabel     statusLabel;
    private JPanel     step1Panel;
    private JPanel     step2Panel;
    private JPanel     cardPanel;          // the white card that holds both steps
    private JLabel     cardTitleLabel;

    // Window drag
    private int mouseX, mouseY;

    public ForgotPasswordFrame() {
        initFrame();
        buildLayout();
        wireEvents();
    }

    // ── Frame bootstrap ──────────────────────────────────────────────────────

    private void initFrame() {
        setTitle("Sanctum — Reset Password");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        if (gd.isFullScreenSupported()) {
            setUndecorated(true);
            gd.setFullScreenWindow(this);
        } else {
            setExtendedState(JFrame.MAXIMIZED_BOTH);
            setUndecorated(true);
        }
        getContentPane().setBackground(C_BG);
    }

    // ── Layout ───────────────────────────────────────────────────────────────

    private void buildLayout() {
        setLayout(new BorderLayout());
        add(buildTopBar(),     BorderLayout.NORTH);
        add(buildBackground(), BorderLayout.CENTER);
    }

    /** Dark top bar with window controls (mirrors LoginFrame). */
    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(C_SURFACE);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(C_BORDER);
                g2.fillRect(0, getHeight() - 1, getWidth(), 1);
                g2.dispose();
            }
        };
        bar.setOpaque(false);
        bar.setPreferredSize(new Dimension(0, 30));

        JLabel title = new JLabel("  Sanctum — Password Reset");
        title.setFont(FONT_SMALL);
        title.setForeground(C_TEXT_MID);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        controls.setOpaque(false);
        controls.add(makeBarButton("─", e -> setState(JFrame.ICONIFIED)));
        controls.add(makeBarButton("✕", e -> returnToLogin()));

        bar.add(title,    BorderLayout.WEST);
        bar.add(controls, BorderLayout.EAST);

        // drag-to-move
        bar.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) { mouseX = e.getX(); mouseY = e.getY(); }
        });
        bar.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                setLocation(e.getXOnScreen() - mouseX, e.getYOnScreen() - mouseY);
            }
        });
        return bar;
    }

    private JButton makeBarButton(String text, ActionListener al) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                if (getModel().isRollover()) {
                    g2.setColor(new Color(255, 255, 255, 30));
                    g2.fillRect(0, 0, getWidth(), getHeight());
                }
                g2.setColor(C_TEXT_MID);
                g2.setFont(FONT_SMALL);
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
        btn.setPreferredSize(new Dimension(40, 30));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(al);
        return btn;
    }

    /** Full-window dark gradient background containing the centred card. */
    private JPanel buildBackground() {
        JPanel bg = new JPanel(new GridBagLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0, C_BG, getWidth(), getHeight(), C_SURFACE));
                g2.fillRect(0, 0, getWidth(), getHeight());
                // decorative blobs
                g2.setColor(new Color(C_GOLD.getRed(), C_GOLD.getGreen(), C_GOLD.getBlue(), 18));
                g2.fillOval(getWidth() - 220, 80,  180, 180);
                g2.fillOval(50, getHeight() - 180, 120, 120);
                g2.dispose();
            }
        };
        bg.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.weightx = 1; gbc.weighty = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        bg.add(buildCard(), gbc);

        return bg;
    }

    /** The centred card containing the two-step form. */
    private JPanel buildCard() {
        cardPanel = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(C_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.setColor(C_BORDER);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                // gold top-edge glow
                g2.setColor(new Color(C_GOLD.getRed(), C_GOLD.getGreen(), C_GOLD.getBlue(), 40));
                g2.fillRoundRect(1, 1, getWidth() - 2, 5, 4, 4);
                g2.dispose();
            }
        };
        cardPanel.setOpaque(false);
        cardPanel.setPreferredSize(new Dimension(480, 0));
        cardPanel.setBorder(BorderFactory.createEmptyBorder(36, 44, 36, 44));

        // ── Inner form wrapper ──
        JPanel formWrapper = new JPanel();
        formWrapper.setOpaque(false);
        formWrapper.setLayout(new BoxLayout(formWrapper, BoxLayout.Y_AXIS));

        // icon + title
        JLabel icon = new JLabel("🔐", SwingConstants.CENTER);
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 40));
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);

        cardTitleLabel = new JLabel("FORGOT PASSWORD", SwingConstants.CENTER);
        cardTitleLabel.setFont(FONT_TITLE);
        cardTitleLabel.setForeground(C_GOLD);
        cardTitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitle = new JLabel("Enter your email to receive a reset token", SwingConstants.CENTER);
        subtitle.setFont(FONT_SMALL);
        subtitle.setForeground(C_TEXT_DIM);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        statusLabel = new JLabel(" ", SwingConstants.CENTER);
        statusLabel.setFont(FONT_SMALL);
        statusLabel.setForeground(C_TEXT_MID);
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Build both step panels
        step1Panel = buildStep1Panel();
        step2Panel = buildStep2Panel();
        step2Panel.setVisible(false);

        // Back to login link
        JPanel backRow = buildBackLink();

        formWrapper.add(icon);
        formWrapper.add(Box.createVerticalStrut(10));
        formWrapper.add(cardTitleLabel);
        formWrapper.add(Box.createVerticalStrut(6));
        formWrapper.add(subtitle);
        formWrapper.add(Box.createVerticalStrut(22));
        formWrapper.add(step1Panel);
        formWrapper.add(step2Panel);
        formWrapper.add(Box.createVerticalStrut(10));
        formWrapper.add(statusLabel);
        formWrapper.add(Box.createVerticalStrut(18));
        formWrapper.add(backRow);

        cardPanel.add(formWrapper, BorderLayout.CENTER);
        return cardPanel;
    }

    // ── Step 1: email request ────────────────────────────────────────────────

    private JPanel buildStep1Panel() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        JLabel emailLbl = makeFieldLabel("EMAIL ADDRESS");
        emailField = makeTextField("your@email.com");
        requestResetButton = makeGlowButton("SEND RESET TOKEN", C_GOLD);
        requestResetButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        requestResetButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));

        p.add(emailLbl);
        p.add(Box.createVerticalStrut(6));
        p.add(emailField);
        p.add(Box.createVerticalStrut(18));
        p.add(requestResetButton);
        return p;
    }

    // ── Step 2: token + new password ─────────────────────────────────────────

    private JPanel buildStep2Panel() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        // info banner
        JLabel info = new JLabel("<html><center>Check your email for the reset token<br>and enter it below.</center></html>", SwingConstants.CENTER);
        info.setFont(FONT_SMALL);
        info.setForeground(C_TEXT_DIM);
        info.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel tokenLbl    = makeFieldLabel("RESET TOKEN");
        tokenField         = makeTextField("Paste token here");

        JLabel newPwLbl    = makeFieldLabel("NEW PASSWORD  (min 12 chars)");
        newPasswordField   = makePasswordField();

        JLabel confirmLbl  = makeFieldLabel("CONFIRM PASSWORD");
        confirmPasswordField = makePasswordField();

        // password-strength indicator bar
        JProgressBar strengthBar = new JProgressBar(0, 100);
        strengthBar.setStringPainted(false);
        strengthBar.setForeground(C_ERROR);
        strengthBar.setBackground(C_BORDER);
        strengthBar.setBorderPainted(false);
        strengthBar.setPreferredSize(new Dimension(0, 4));
        strengthBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 4));
        JLabel strengthLbl = new JLabel(" ");
        strengthLbl.setFont(FONT_SMALL);
        strengthLbl.setForeground(C_TEXT_DIM);
        strengthLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        // live strength meter
        newPasswordField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            private void update() {
                String pw = new String(newPasswordField.getPassword());
                int score = scorePassword(pw);
                strengthBar.setValue(score);
                if (score < 34) {
                    strengthBar.setForeground(C_ERROR);
                    strengthLbl.setText(pw.isEmpty() ? " " : "Weak");
                    strengthLbl.setForeground(C_ERROR);
                } else if (score < 67) {
                    strengthBar.setForeground(new Color(255, 165, 0));
                    strengthLbl.setText("Fair");
                    strengthLbl.setForeground(new Color(255, 165, 0));
                } else {
                    strengthBar.setForeground(C_SUCCESS);
                    strengthLbl.setText("Strong ✓");
                    strengthLbl.setForeground(C_SUCCESS);
                }
            }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { update(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { update(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { update(); }
        });

        confirmResetButton = makeGlowButton("SET NEW PASSWORD", C_GOLD);
        confirmResetButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        confirmResetButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));

        // "Resend token" link
        JButton resendBtn = makeLinkButton("Didn't receive the email? Resend");
        resendBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        resendBtn.addActionListener(e -> requestPasswordReset(true));

        p.add(info);
        p.add(Box.createVerticalStrut(14));
        p.add(tokenLbl);
        p.add(Box.createVerticalStrut(5));
        p.add(tokenField);
        p.add(Box.createVerticalStrut(14));
        p.add(newPwLbl);
        p.add(Box.createVerticalStrut(5));
        p.add(newPasswordField);
        p.add(Box.createVerticalStrut(4));
        p.add(strengthBar);
        p.add(strengthLbl);
        p.add(Box.createVerticalStrut(10));
        p.add(confirmLbl);
        p.add(Box.createVerticalStrut(5));
        p.add(confirmPasswordField);
        p.add(Box.createVerticalStrut(20));
        p.add(confirmResetButton);
        p.add(Box.createVerticalStrut(10));
        p.add(resendBtn);
        return p;
    }

    private JPanel buildBackLink() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        row.setOpaque(false);
        JLabel lbl = new JLabel("Remember your password? ");
        lbl.setFont(FONT_SMALL);
        lbl.setForeground(C_TEXT_MID);
        JButton back = makeLinkButton("Back to Login");
        back.addActionListener(e -> returnToLogin());
        row.add(lbl);
        row.add(back);
        return row;
    }

    // ── Event wiring ─────────────────────────────────────────────────────────

    private void wireEvents() {
        requestResetButton.addActionListener(e -> requestPasswordReset(false));
        confirmResetButton.addActionListener(e -> confirmPasswordReset());

        // Enter key on all fields
        KeyAdapter enter = new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    if (step == 1) requestResetButton.doClick();
                    else           confirmResetButton.doClick();
                }
            }
        };
        emailField.addKeyListener(enter);
        tokenField.addKeyListener(enter);
        newPasswordField.addKeyListener(enter);
        confirmPasswordField.addKeyListener(enter);

        // ESC closes
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "esc");
        getRootPane().getActionMap().put("esc", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { returnToLogin(); }
        });
    }

    // ── Step-1 action ─────────────────────────────────────────────────────────

    /**
     * @param resend true when called from the "Resend" link inside step 2
     */
    private void requestPasswordReset(boolean resend) {
        String email = emailField.getText().trim();

        if (email.isEmpty() || email.equals("your@email.com")) {
            showError("Please enter your email address.");
            return;
        }
        if (!email.matches("^[A-Za-z0-9+._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            showError("Please enter a valid email address.");
            return;
        }

        setLoading(true, resend ? "Resending…" : "Sending reset token…");

        SanctumApiClient.requestPasswordReset(email)
            .thenAccept(result -> SwingUtilities.invokeLater(() -> {
                setLoading(false, null);
                if (result.success) {
                    if (step == 1) advanceToStep2();
                    showSuccess("Reset token sent — check your inbox.");
                } else {
                    showError(result.message != null ? result.message
                                                     : "Email not found. Please check and try again.");
                }
            }))
            .exceptionally(ex -> {
                SwingUtilities.invokeLater(() -> {
                    setLoading(false, null);
                    showError("Network error. Please check your connection.");
                });
                return null;
            });
    }

    /** Switch the card to show step-2 widgets. */
    private void advanceToStep2() {
        step = 2;
        step1Panel.setVisible(false);
        step2Panel.setVisible(true);
        cardTitleLabel.setText("RESET PASSWORD");
        statusLabel.setText(" ");
        pack();
        setLocationRelativeTo(null);
        tokenField.requestFocusInWindow();
    }

    // ── Step-2 action ─────────────────────────────────────────────────────────

    private void confirmPasswordReset() {
        String token       = tokenField.getText().trim();
        String newPw       = new String(newPasswordField.getPassword());
        String confirmPw   = new String(confirmPasswordField.getPassword());

        // ── client-side validation ──
        if (token.isEmpty() || token.equals("Paste token here")) {
            showError("Please paste the token from your email.");
            tokenField.requestFocusInWindow();
            return;
        }
        if (newPw.length() < 12) {
            showError("Password must be at least 12 characters.");
            newPasswordField.requestFocusInWindow();
            return;
        }
        if (!newPw.equals(confirmPw)) {
            showError("Passwords do not match.");
            confirmPasswordField.requestFocusInWindow();
            return;
        }
        if (scorePassword(newPw) < 34) {
            showError("Password is too weak. Add uppercase letters, numbers, or symbols.");
            newPasswordField.requestFocusInWindow();
            return;
        }

        setLoading(true, "Resetting password…");
        confirmResetButton.setEnabled(false);

        SanctumApiClient.confirmPasswordReset(token, newPw, confirmPw)
            .thenAccept(result -> SwingUtilities.invokeLater(() -> {
                setLoading(false, null);
                confirmResetButton.setEnabled(true);
                if (result.success) {
                    showSuccess("Password reset! Returning to login…");
                    Timer t = new Timer(2000, e -> returnToLogin());
                    t.setRepeats(false);
                    t.start();
                } else {
                    String msg = result.message != null ? result.message : "Reset failed. The token may be invalid or expired.";
                    showError(msg);
                }
            }))
            .exceptionally(ex -> {
                SwingUtilities.invokeLater(() -> {
                    setLoading(false, null);
                    confirmResetButton.setEnabled(true);
                    showError("Network error. Please try again.");
                });
                return null;
            });
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void returnToLogin() {
        dispose();
        new LoginFrame().setVisible(true);
    }

    private void showError(String msg) {
        statusLabel.setText("✗ " + msg);
        statusLabel.setForeground(C_ERROR);
    }

    private void showSuccess(String msg) {
        statusLabel.setText("✓ " + msg);
        statusLabel.setForeground(C_SUCCESS);
    }

    private void setLoading(boolean loading, String msg) {
        requestResetButton.setEnabled(!loading);
        confirmResetButton.setEnabled(!loading);
        emailField.setEnabled(!loading);
        tokenField.setEnabled(!loading);
        newPasswordField.setEnabled(!loading);
        confirmPasswordField.setEnabled(!loading);
        if (loading && msg != null) {
            statusLabel.setText(msg);
            statusLabel.setForeground(C_GOLD);
        }
    }

    /**
     * Simple password strength scorer (0–100).
     * Checks length, uppercase, digits, and special characters.
     */
    private int scorePassword(String pw) {
        if (pw == null || pw.isEmpty()) return 0;
        int score = 0;
        if (pw.length() >= 8)  score += 20;
        if (pw.length() >= 12) score += 20;
        if (pw.length() >= 16) score += 10;
        if (pw.matches(".*[A-Z].*"))             score += 15;
        if (pw.matches(".*[a-z].*"))             score += 10;
        if (pw.matches(".*\\d.*"))               score += 15;
        if (pw.matches(".*[^A-Za-z0-9].*"))      score += 10;
        return Math.min(score, 100);
    }

    // ── Widget factory helpers ────────────────────────────────────────────────

    private JLabel makeFieldLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(FONT_LABEL);
        lbl.setForeground(C_TEXT_MID);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }

    private JTextField makeTextField(String placeholder) {
        JTextField field = new JTextField(placeholder);
        field.setFont(FONT_VALUE);
        field.setForeground(C_TEXT_DIM);
        field.setBackground(C_CARD);
        field.setCaretColor(C_GOLD);
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(C_BORDER),
            BorderFactory.createEmptyBorder(10, 14, 10, 14)
        ));
        field.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                if (field.getText().equals(placeholder)) {
                    field.setText("");
                    field.setForeground(C_TEXT);
                }
                field.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(C_GOLD, 2),
                    BorderFactory.createEmptyBorder(9, 13, 9, 13)
                ));
            }
            public void focusLost(FocusEvent e) {
                if (field.getText().isEmpty()) {
                    field.setText(placeholder);
                    field.setForeground(C_TEXT_DIM);
                }
                field.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(C_BORDER),
                    BorderFactory.createEmptyBorder(10, 14, 10, 14)
                ));
            }
        });
        return field;
    }

    private JPasswordField makePasswordField() {
        JPasswordField field = new JPasswordField();
        field.setFont(FONT_VALUE);
        field.setForeground(C_TEXT);
        field.setBackground(C_CARD);
        field.setCaretColor(C_GOLD);
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(C_BORDER),
            BorderFactory.createEmptyBorder(10, 14, 10, 14)
        ));
        field.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(C_GOLD, 2),
                    BorderFactory.createEmptyBorder(9, 13, 9, 13)
                ));
            }
            public void focusLost(FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(C_BORDER),
                    BorderFactory.createEmptyBorder(10, 14, 10, 14)
                ));
            }
        });
        return field;
    }

    private JButton makeGlowButton(String text, Color accent) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean hover   = getModel().isRollover();
                boolean pressed = getModel().isPressed();
                boolean enabled = isEnabled();

                Color bg = enabled
                    ? (pressed ? new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 60)
                               : hover ? new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 40)
                                       : new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 20))
                    : new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 10);

                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);

                g2.setColor(enabled ? accent : C_TEXT_DIM);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);

                g2.setFont(FONT_BUTTON);
                g2.setColor(enabled ? accent : C_TEXT_DIM);
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
        btn.setPreferredSize(new Dimension(0, 48));
        return btn;
    }

    private JButton makeLinkButton(String text) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Color c = getModel().isRollover() ? C_GOLD_HOVER : C_GOLD;
                g2.setFont(FONT_SMALL);
                g2.setColor(c);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(text,
                    (getWidth() - fm.stringWidth(text)) / 2,
                    (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                // underline
                int tx = (getWidth() - fm.stringWidth(text)) / 2;
                int ty = (getHeight() + fm.getAscent() - fm.getDescent()) / 2 + 2;
                g2.drawLine(tx, ty, tx + fm.stringWidth(text), ty);
                g2.dispose();
            }
        };
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(250, 22));
        return btn;
    }

    // Allow makeLinkButton to reference C_GOLD_HOVER as a static-like value
    private static final Color C_GOLD_HOVER_STATIC = new Color(230, 199, 102);
}
