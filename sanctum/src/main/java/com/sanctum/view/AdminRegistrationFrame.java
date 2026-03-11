package com.sanctum.view;

import com.sanctum.api.SanctumApiClient;
import com.sanctum.auth.SessionManager;
import com.sanctum.util.LogoLoader;

import javax.swing.*;
import javax.swing.border.*;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import javax.swing.SwingWorker;

/**
 * Admin Registration Frame - Separate admin registration from church setup
 */
public class AdminRegistrationFrame extends JFrame {
    // ─── Sanctum Brand Color System ─────────────────────────────────────────────
    private static final Color C_BG          = new Color(14,  46,  42);   // Deep Emerald Green
    private static final Color C_SURFACE     = new Color(19,  58,  54);   // Dark Green Secondary
    private static final Color C_CARD        = new Color(28,  47,  44);   // Input Background
    private static final Color C_CARD_HOVER  = new Color(42,  74,  69);   // Hover State

    private static final Color C_BORDER      = new Color(42,  74,  69);   // Border Color
    private static final Color C_BORDER_LT   = new Color(66,  115, 107);  // Light Border

    // Accent palette — Gold spectrum
    private static final Color C_GOLD        = new Color(212, 175,  55);  // Gold Accent
    private static final Color C_GOLD_DIM    = new Color(212, 175,  55, 25);
    private static final Color C_GOLD_HOVER  = new Color(230, 199, 102);  // Light Gold Hover
    private static final Color C_GOLD_DIM_HOVER = new Color(230, 199, 102, 25);

    // Text hierarchy
    private static final Color C_TEXT        = new Color(255, 255, 255);  // White Text
    private static final Color C_TEXT_MID    = new Color(207, 207, 207);  // Soft Gray Secondary
    private static final Color C_TEXT_DIM    = new Color(156, 163, 175);  // Dim Text

    // ── Fonts ──────────────────────────────────────────────────────────────
    private static final Font FONT_TITLE  = new Font("Georgia", Font.BOLD, 28);
    private static final Font FONT_LABEL  = new Font("Segoe UI", Font.BOLD, 12);
    private static final Font FONT_VALUE  = new Font("Segoe UI", Font.BOLD, 16);
    private static final Font FONT_SMALL  = new Font("Segoe UI", Font.PLAIN, 11);
    private static final Font FONT_BUTTON = new Font("Segoe UI", Font.BOLD, 14);

    // Form components
    private JTextField firstNameField;
    private JTextField lastNameField;
    private JTextField emailField;
    private JTextField phoneField;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JPasswordField confirmPasswordField;
    private JComboBox<String> roleCombo;
    private JButton registerButton;
    private JLabel statusLabel;
    private JLabel loadingLabel;
    private JProgressBar loadingBar;
    private JPanel loadingPanel;

    private final SessionManager sessionManager;

    public AdminRegistrationFrame() {
        this.sessionManager = SessionManager.getInstance();
        initializeFrame();
        createComponents();
        layoutComponents();
        setupEventHandlers();
    }

    private void initializeFrame() {
        setTitle("Admin Registration - Sanctum");
        setSize(900, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        // Set application icon
        setApplicationIcon();
        
        setUndecorated(true);

        // Full screen setup
        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        if (gd.isFullScreenSupported()) {
            setUndecorated(true);
            gd.setFullScreenWindow(this);
        } else {
            // Fallback to maximized window if full screen not supported
            setExtendedState(JFrame.MAXIMIZED_BOTH);
            setUndecorated(true);
        }

        getContentPane().setBackground(C_BG);
    }
    
    /**
     * Sets the application icon for this window using PNG for better compatibility
     */
    private void setApplicationIcon() {
        try {
            // Try PNG first (better Java compatibility)
            Image iconImage = loadIconFromResources("/images/icon.png");
            
            if (iconImage != null) {
                setIconImage(iconImage);
                System.out.println("AdminRegistrationFrame PNG icon loaded successfully - Size: " + iconImage.getWidth(null) + "x" + iconImage.getHeight(null));
            } else {
                System.out.println("AdminRegistrationFrame PNG icon failed to load, trying ICO fallback");
                iconImage = loadIconFromResources("/images/icon.ico");
                
                if (iconImage != null) {
                    setIconImage(iconImage);
                    System.out.println("AdminRegistrationFrame ICO icon loaded successfully as fallback - Size: " + iconImage.getWidth(null) + "x" + iconImage.getHeight(null));
                } else {
                    System.out.println("AdminRegistrationFrame Both PNG and ICO fallback failed - using default icon");
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to set AdminRegistrationFrame application icon: " + e.getMessage());
        }
    }
    
    /**
     * Load icon image from resources using ImageIO for better format support
     */
    private Image loadIconFromResources(String path) {
        try {
            InputStream inputStream = AdminRegistrationFrame.class.getResourceAsStream(path);
            if (inputStream == null) {
                System.out.println("AdminRegistrationFrame Resource not found: " + path);
                return null;
            }
            
            // Use ImageIO to read the image (better for ICO files)
            BufferedImage bufferedImage = ImageIO.read(inputStream);
            inputStream.close();
            
            if (bufferedImage != null) {
                System.out.println("AdminRegistrationFrame Successfully loaded image from " + path + " - Size: " + bufferedImage.getWidth() + "x" + bufferedImage.getHeight());
                return bufferedImage;
            } else {
                System.out.println("AdminRegistrationFrame Failed to read image from " + path);
                return null;
            }
            
        } catch (Exception e) {
            System.err.println("AdminRegistrationFrame Error loading image from " + path + ": " + e.getMessage());
            return null;
        }
    }

    private void createComponents() {
        // Create buttons first (needed by panels)
        registerButton = createGlowButton("CREATE ADMIN ACCOUNT", C_GOLD);
        registerButton.setPreferredSize(new Dimension(250, 50));

        // Status label
        statusLabel = new JLabel("", SwingConstants.CENTER);
        statusLabel.setFont(FONT_SMALL);
        statusLabel.setForeground(C_GOLD);

        // Loading components
        loadingLabel = new JLabel("Creating admin account...", SwingConstants.CENTER);
        loadingLabel.setFont(FONT_SMALL);
        loadingLabel.setForeground(C_GOLD);

        loadingBar = new JProgressBar();
        loadingBar.setIndeterminate(true);
        loadingBar.setStringPainted(false);
        loadingBar.setForeground(C_GOLD);
        loadingBar.setBackground(C_BORDER);
        loadingBar.setBorderPainted(false);
        loadingBar.setPreferredSize(new Dimension(0, 6));

        loadingPanel = new JPanel(new BorderLayout());
        loadingPanel.setOpaque(false);
        loadingPanel.add(loadingLabel, BorderLayout.NORTH);
        loadingPanel.add(loadingBar, BorderLayout.CENTER);
        loadingPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        loadingPanel.setVisible(false);
    }

    private void layoutComponents() {
        setLayout(new BorderLayout());

        // Main container with dark theme
        JPanel mainContainer = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Dark gradient background
                GradientPaint gradient = new GradientPaint(
                    0, 0, C_BG,
                    getWidth(), getHeight(), C_SURFACE
                );
                g2.setPaint(gradient);
                g2.fillRect(0, 0, getWidth(), getHeight());

                // Decorative elements with gold theme
                g2.setColor(new Color(C_GOLD.getRed(), C_GOLD.getGreen(), C_GOLD.getBlue(), 15));
                g2.fillOval(getWidth() - 200, 100, 150, 150);
                g2.fillOval(getWidth() - 300, 300, 100, 100);

                g2.dispose();
            }
        };
        mainContainer.setOpaque(false);

        // Create registration form
        JPanel registrationPanel = createRegistrationPanel();

        // Window controls
        JPanel windowControls = createWindowControls();

        add(windowControls, BorderLayout.NORTH);
        add(mainContainer, BorderLayout.CENTER);

        // Add registration panel to main container
        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setOpaque(false);
        centerPanel.add(registrationPanel, new GridBagConstraints());

        mainContainer.add(centerPanel, BorderLayout.CENTER);
    }

    private JPanel createRegistrationPanel() {
        JPanel panel = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                g2.setColor(C_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);

                g2.setColor(C_BORDER);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 20, 20);

                // Glow effect
                g2.setColor(new Color(C_GOLD.getRed(), C_GOLD.getGreen(), C_GOLD.getBlue(), 30));
                g2.fillRoundRect(1, 1, getWidth()-2, 4, 4, 4);

                g2.dispose();
            }
        };
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(40, 60, 40, 60));
        panel.setPreferredSize(new Dimension(600, 0));

        // Title
        JLabel titleLabel = new JLabel("Admin Registration", SwingConstants.CENTER);
        titleLabel.setFont(FONT_TITLE);
        titleLabel.setForeground(C_GOLD);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 30, 0));

        // Subtitle
        JLabel subtitleLabel = new JLabel("<html><div style='text-align: center; color: #CFCFCF; font-size: 14px;'>" +
            "Create your administrator account to access Sanctum Church Management System.<br>" +
            "After registration, you'll be prompted to set up your church if needed.</div></html>", SwingConstants.CENTER);
        subtitleLabel.setFont(FONT_SMALL);
        subtitleLabel.setForeground(C_TEXT_MID);
        subtitleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 40, 0));

        // Form panel
        JPanel formPanel = createFormPanel();

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.add(registerButton);

        // Status panel
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setOpaque(false);
        statusPanel.add(statusLabel, BorderLayout.CENTER);
        statusPanel.add(loadingPanel, BorderLayout.SOUTH);
        statusPanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));

        // Main layout
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setOpaque(false);
        mainPanel.add(titleLabel, BorderLayout.NORTH);
        
        // Content panel for subtitle and form
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setOpaque(false);
        contentPanel.add(subtitleLabel, BorderLayout.NORTH);
        contentPanel.add(formPanel, BorderLayout.CENTER);
        
        mainPanel.add(contentPanel, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        mainPanel.add(statusPanel, BorderLayout.SOUTH);

        panel.add(mainPanel, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createFormPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 8, 8, 8);

        // Personal Information Section
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        JLabel personalLabel = new JLabel("Personal Information");
        personalLabel.setFont(FONT_LABEL);
        personalLabel.setForeground(C_GOLD);
        panel.add(personalLabel, gbc);

        gbc.gridy = 1; gbc.gridwidth = 1;
        addFormField(panel, gbc, "First Name", firstNameField = createTextField("Enter your first name"), 0, 1);
        addFormField(panel, gbc, "Last Name", lastNameField = createTextField("Enter your last name"), 1, 1);

        gbc.gridy = 2; gbc.gridwidth = 2;
        addFormField(panel, gbc, "Email Address", emailField = createTextField("your.email@example.com"), 0, 2);
        addFormField(panel, gbc, "Phone Number", phoneField = createTextField("+254 7XX XXX XXX"), 0, 3);

        // Account Information Section
        gbc.gridy = 4; gbc.gridwidth = 2;
        JLabel accountLabel = new JLabel("Account Information");
        accountLabel.setFont(FONT_LABEL);
        accountLabel.setForeground(C_GOLD);
        panel.add(accountLabel, gbc);

        gbc.gridy = 5; gbc.gridwidth = 2;
        addFormField(panel, gbc, "Username", usernameField = createTextField("Choose a username"), 0, 5);

        gbc.gridy = 6; gbc.gridwidth = 1;
        addFormField(panel, gbc, "Password", passwordField = createPasswordField("Enter secure password"), 0, 6);
        addFormField(panel, gbc, "Confirm Password", confirmPasswordField = createPasswordField("Re-enter password"), 1, 6);

        gbc.gridy = 7; gbc.gridwidth = 2;
        addFormField(panel, gbc, "Role", roleCombo = createRoleDropdown(), 0, 7);

        // Password requirements
        JPanel requirementsPanel = createPasswordRequirementsPanel();
        gbc.gridy = 8;
        panel.add(requirementsPanel, gbc);

        // Add spacing at bottom
        gbc.gridy = 9;
        gbc.weighty = 1.0;
        panel.add(Box.createVerticalGlue(), gbc);

        return panel;
    }

    private void addFormField(JPanel panel, GridBagConstraints gbc, String labelText, JComponent field, int x, int y) {
        gbc.gridx = x;
        gbc.gridy = y;
        gbc.weightx = x == 0 ? 0.3 : 0.7;

        JLabel label = new JLabel(labelText);
        label.setFont(FONT_LABEL);
        label.setForeground(C_TEXT_MID);
        panel.add(label, gbc);

        gbc.gridx = x + 1;
        panel.add(field, gbc);
    }

    private JTextField createTextField(String placeholder) {
        JTextField field = new JTextField(placeholder) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (!hasFocus() && getText().equals(placeholder)) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(C_TEXT_DIM);
                    g2.setFont(getFont().deriveFont(Font.ITALIC));
                    FontMetrics fm = g2.getFontMetrics();
                    int textX = 15;
                    int textY = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                    g2.drawString(placeholder, textX, textY);
                    g2.dispose();
                }
            }

            @Override
            protected void paintBorder(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                if (hasFocus()) {
                    g2.setColor(C_GOLD);
                    g2.setStroke(new BasicStroke(2));
                } else {
                    g2.setColor(C_BORDER);
                    g2.setStroke(new BasicStroke(1));
                }

                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 12, 12);
                g2.dispose();
            }
        };

        field.setFont(FONT_VALUE);
        field.setForeground(C_TEXT);
        field.setBackground(C_CARD);
        field.setCaretColor(C_GOLD);
        field.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        field.setMinimumSize(new Dimension(250, 45));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
        field.setPreferredSize(new Dimension(250, 45));

        field.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                if (field.getText().equals(placeholder)) {
                    field.setText("");
                    field.setForeground(C_TEXT);
                }
                field.repaint();
            }
            public void focusLost(FocusEvent e) {
                if (field.getText().isEmpty()) {
                    field.setText(placeholder);
                    field.setForeground(C_TEXT_DIM);
                }
                field.repaint();
            }
        });

        // Initialize with placeholder styling
        if (!placeholder.isEmpty()) {
            field.setForeground(C_TEXT_DIM);
        }

        return field;
    }

    private JPasswordField createPasswordField(String placeholder) {
        JPasswordField field = new JPasswordField(placeholder) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (!hasFocus() && new String(getPassword()).equals(placeholder)) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(C_TEXT_DIM);
                    g2.setFont(getFont().deriveFont(Font.ITALIC));
                    FontMetrics fm = g2.getFontMetrics();
                    int textX = 15;
                    int textY = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                    g2.drawString(placeholder, textX, textY);
                    g2.dispose();
                }
            }

            @Override
            protected void paintBorder(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                if (hasFocus()) {
                    g2.setColor(C_GOLD);
                    g2.setStroke(new BasicStroke(2));
                } else {
                    g2.setColor(C_BORDER);
                    g2.setStroke(new BasicStroke(1));
                }

                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 12, 12);
                g2.dispose();
            }
        };

        field.setFont(FONT_VALUE);
        field.setForeground(C_TEXT);
        field.setBackground(C_CARD);
        field.setCaretColor(C_GOLD);
        field.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        field.setEchoChar('•');
        field.setMinimumSize(new Dimension(250, 45));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
        field.setPreferredSize(new Dimension(250, 45));

        field.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                if (new String(field.getPassword()).equals(placeholder)) {
                    field.setText("");
                    field.setForeground(C_TEXT);
                }
                field.repaint();
            }
            public void focusLost(FocusEvent e) {
                if (new String(field.getPassword()).isEmpty()) {
                    field.setText(placeholder);
                    field.setForeground(C_TEXT_DIM);
                }
                field.repaint();
            }
        });

        // Initialize with placeholder styling
        if (!placeholder.isEmpty()) {
            field.setForeground(C_TEXT_DIM);
        }

        return field;
    }

    private JComboBox<String> createRoleDropdown() {
        String[] roles = {"admin", "pastor", "treasurer", "secretary", "usher"};
        JComboBox<String> combo = new JComboBox<>(roles);
        combo.setFont(FONT_VALUE);
        combo.setForeground(C_TEXT);
        combo.setBackground(C_CARD);
        combo.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(C_BORDER),
            BorderFactory.createEmptyBorder(12, 15, 12, 15)
        ));
        combo.setMinimumSize(new Dimension(250, 45));
        combo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
        combo.setPreferredSize(new Dimension(250, 45));
        return combo;
    }

    private JPanel createPasswordRequirementsPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(C_SURFACE);
        panel.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        panel.setLayout(new BorderLayout());

        JLabel requirementsLabel = new JLabel("<html><div style='font-size: 13px; color: #ffffff; line-height: 1.5;'>" +
            "Password must contain at least 12 characters including uppercase, lowercase, number and special character" + "</div></html>");
        requirementsLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        panel.add(requirementsLabel, BorderLayout.CENTER);
        return panel;
    }

    private JButton createGlowButton(String text, Color color) {
        JButton button = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                // Determine background color based on state
                Color bgColor;
                if (getModel().isPressed()) {
                    bgColor = color.darker();
                } else if (getModel().isRollover()) {
                    bgColor = color.brighter();
                } else {
                    bgColor = color;
                }

                // Draw background
                g2.setColor(bgColor);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);

                // Glow effect
                if (getModel().isRollover()) {
                    g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 50));
                    g2.fillRoundRect(-2, -2, getWidth() + 4, getHeight() + 4, 14, 14);
                }

                // Draw text with shadow for better visibility
                g2.setFont(getFont());
                
                // Text shadow (dark outline for better contrast)
                g2.setColor(new Color(0, 0, 0, 100)); // Semi-transparent black
                FontMetrics fm = g2.getFontMetrics();
                int textWidth = fm.stringWidth(getText());
                int textHeight = fm.getHeight();
                int textX = (getWidth() - textWidth) / 2;
                int textY = (getHeight() - textHeight) / 2 + fm.getAscent();
                
                g2.drawString(getText(), textX + 1, textY + 1); // Shadow
                g2.drawString(getText(), textX - 1, textY - 1); // Shadow
                g2.drawString(getText(), textX + 1, textY - 1); // Shadow
                g2.drawString(getText(), textX - 1, textY + 1); // Shadow

                // Main text (white)
                g2.setColor(Color.WHITE);
                g2.drawString(getText(), textX, textY);

                g2.dispose();
            }

            @Override protected void paintBorder(Graphics g) {
                // No border for modern look
            }
        };

        button.setFont(FONT_BUTTON);
        button.setForeground(Color.WHITE); // Fallback text color
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createEmptyBorder(15, 30, 15, 30));

        return button;
    }

    private JPanel createWindowControls() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 0));

        JButton minimizeBtn = new JButton("−") {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                if (getModel().isRollover()) {
                    g2.setColor(C_GOLD_DIM_HOVER);
                    g2.fillRect(0, 0, getWidth(), getHeight());
                }
                g2.setColor(C_TEXT);
                g2.setFont(FONT_SMALL);
                FontMetrics fm = g2.getFontMetrics();
                int tx = (getWidth() - fm.stringWidth("−")) / 2;
                int ty = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString("−", tx, ty);
                g2.dispose();
            }
        };

        JButton closeBtn = new JButton("✕") {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                if (getModel().isRollover()) {
                    g2.setColor(C_TEXT_MID);
                    g2.fillRect(0, 0, getWidth(), getHeight());
                    g2.setColor(C_TEXT);
                } else {
                    g2.setColor(C_TEXT_MID);
                }
                g2.setFont(FONT_SMALL);
                FontMetrics fm = g2.getFontMetrics();
                int tx = (getWidth() - fm.stringWidth("✕")) / 2;
                int ty = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString("✕", tx, ty);
                g2.dispose();
            }
        };

        // Style buttons
        for (JButton btn : new JButton[]{minimizeBtn, closeBtn}) {
            btn.setContentAreaFilled(false);
            btn.setBorderPainted(false);
            btn.setFocusPainted(false);
            btn.setPreferredSize(new Dimension(40, 30));
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        minimizeBtn.addActionListener(e -> setState(JFrame.ICONIFIED));
        closeBtn.addActionListener(e -> System.exit(0));

        panel.add(minimizeBtn);
        panel.add(closeBtn);

        return panel;
    }

    private void setupEventHandlers() {
        // Window dragging
        addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent evt) {
                mouseX = evt.getX();
                mouseY = evt.getY();
            }
        });

        addMouseMotionListener(new MouseAdapter() {
            public void mouseDragged(MouseEvent evt) {
                setLocation(evt.getXOnScreen() - mouseX, evt.getYOnScreen() - mouseY);
            }
        });

        // Register button action
        registerButton.addActionListener(e -> handleRegistration());

        // Escape key to close
        InputMap im = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        im.put(KeyStroke.getKeyStroke("ESCAPE"), "close");
        getRootPane().getActionMap().put("close", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                dispose();
                new LoginFrame().setVisible(true);
            }
        });
    }

    private int mouseX, mouseY;

    private void handleRegistration() {
        // Validate form
        if (!validateForm()) {
            return;
        }

        // Disable button and show loading
        registerButton.setEnabled(false);
        loadingPanel.setVisible(true);
        statusLabel.setText("");

        // Create registration data
        Map<String, Object> registrationData = new HashMap<>();
        registrationData.put("first_name", firstNameField.getText());
        registrationData.put("last_name", lastNameField.getText());
        registrationData.put("email", emailField.getText());
        registrationData.put("phone_number", phoneField.getText());
        registrationData.put("password", new String(passwordField.getPassword()));
        registrationData.put("password_confirm", new String(confirmPasswordField.getPassword()));
        registrationData.put("role", (String) roleCombo.getSelectedItem());
        
        // Create church_data structure as expected by backend
        Map<String, Object> churchData = new HashMap<>();
        churchData.put("name", "Sanctum Community Church"); // Default church name
        churchData.put("church_type", "denomination");
        churchData.put("phone_number", "+254700000000"); // Default phone
        churchData.put("email", "info@sanctumchurch.com");
        churchData.put("address_line1", "123 Church Street, Nairobi");
        churchData.put("city", "Nairobi");
        churchData.put("county", "Nairobi County");
        churchData.put("senior_pastor_name", firstNameField.getText() + " " + lastNameField.getText());
        churchData.put("senior_pastor_phone", phoneField.getText());
        churchData.put("senior_pastor_email", emailField.getText());
        churchData.put("membership_count", 100);
        churchData.put("average_attendance", 75);
        churchData.put("denomination", "Christian Denomination");
        churchData.put("primary_color", "#0E2E2A");
        churchData.put("secondary_color", "#133A36");
        churchData.put("accent_color", "#D4AF37");
        
        registrationData.put("church_data", churchData);
        
        // Debug: Log the registration data structure
        System.out.println("=== REGISTRATION DATA STRUCTURE ===");
        System.out.println("Total fields: " + registrationData.keySet());
        System.out.println("Church data present: " + registrationData.containsKey("church_data"));
        if (registrationData.containsKey("church_data")) {
            Object churchObj = registrationData.get("church_data");
            System.out.println("Church data type: " + churchObj.getClass().getSimpleName());
            if (churchObj instanceof Map) {
                Map<String, Object> churchMap = (Map<String, Object>) churchObj;
                System.out.println("Church data fields: " + churchMap.keySet());
                System.out.println("Church data values: " + churchMap);
                
                // Validate required church fields
                if (!churchMap.containsKey("name") || churchMap.get("name") == null || churchMap.get("name").toString().trim().isEmpty()) {
                    SwingUtilities.invokeLater(() -> {
                        loadingPanel.setVisible(false);
                        registerButton.setEnabled(true);
                        statusLabel.setText("Error: Church name is required");
                        statusLabel.setForeground(new Color(255, 100, 100));
                    });
                    return;
                }
                
                if (!churchMap.containsKey("senior_pastor_name") || churchMap.get("senior_pastor_name") == null || churchMap.get("senior_pastor_name").toString().trim().isEmpty()) {
                    SwingUtilities.invokeLater(() -> {
                        loadingPanel.setVisible(false);
                        registerButton.setEnabled(true);
                        statusLabel.setText("Error: Senior pastor name is required");
                        statusLabel.setForeground(new Color(255, 100, 100));
                    });
                    return;
                }
                
                if (!churchMap.containsKey("senior_pastor_email") || churchMap.get("senior_pastor_email") == null || churchMap.get("senior_pastor_email").toString().trim().isEmpty()) {
                    SwingUtilities.invokeLater(() -> {
                        loadingPanel.setVisible(false);
                        registerButton.setEnabled(true);
                        statusLabel.setText("Error: Senior pastor email is required");
                        statusLabel.setForeground(new Color(255, 100, 100));
                    });
                    return;
                }
            }
        } else {
            // Error: church data is required for admin registration
            SwingUtilities.invokeLater(() -> {
                loadingPanel.setVisible(false);
                registerButton.setEnabled(true);
                statusLabel.setText("Error: Church information is required for admin registration");
                statusLabel.setForeground(new Color(255, 100, 100)); // Red color for error
            });
            return;
        }

        // Perform registration in background
        SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                return SanctumApiClient.register(registrationData).get();
            }

            @Override
            protected void done() {
                try {
                    boolean success = get();
                    SwingUtilities.invokeLater(() -> {
                        loadingPanel.setVisible(false);
                        registerButton.setEnabled(true);

                        if (success) {
                            statusLabel.setText("Admin account created successfully!");
                            statusLabel.setForeground(C_GOLD);

                            // Show success dialog and transition to login
                            Timer timer = new Timer(2000, e -> {
                                dispose();
                                // After successful admin registration, check if church setup is needed
                                showChurchSetupCheck();
                            });
                            timer.setRepeats(false);
                            timer.start();
                        } else {
                            statusLabel.setText("Registration failed. Please try again.");
                            statusLabel.setForeground(C_TEXT_MID);
                        }
                    });
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> {
                        loadingPanel.setVisible(false);
                        registerButton.setEnabled(true);
                        statusLabel.setText("Registration error: " + ex.getMessage());
                        statusLabel.setForeground(C_TEXT_MID);
                    });
                }
            }
        };

        worker.execute();
    }

    private void showChurchSetupCheck() {
        // Check if admin has associated church
        SanctumApiClient.checkUserChurchAssociation().thenAccept(hasChurch -> {
            SwingUtilities.invokeLater(() -> {
                if (!hasChurch) {
                    // Show church setup dialog
                    new ChurchSetupFrame().setVisible(true);
                } else {
                    // Go directly to login
                    new LoginFrame().setVisible(true);
                }
            });
        });
    }

    private boolean validateForm() {
        // Basic validation
        if (firstNameField.getText().isEmpty() || firstNameField.getText().equals("Enter your first name")) {
            statusLabel.setText("Please enter your first name");
            statusLabel.setForeground(C_TEXT_MID);
            return false;
        }

        if (lastNameField.getText().isEmpty() || lastNameField.getText().equals("Enter your last name")) {
            statusLabel.setText("Please enter your last name");
            statusLabel.setForeground(C_TEXT_MID);
            return false;
        }

        if (emailField.getText().isEmpty() || emailField.getText().equals("your.email@example.com")) {
            statusLabel.setText("Please enter your email address");
            statusLabel.setForeground(C_TEXT_MID);
            return false;
        }

        if (usernameField.getText().isEmpty() || usernameField.getText().equals("Choose a username")) {
            statusLabel.setText("Please enter a username");
            statusLabel.setForeground(C_TEXT_MID);
            return false;
        }

        String password = new String(passwordField.getPassword());
        String confirmPassword = new String(confirmPasswordField.getPassword());

        if (password.isEmpty() || password.equals("Enter secure password")) {
            statusLabel.setText("Please enter a password");
            statusLabel.setForeground(C_TEXT_MID);
            return false;
        }

        if (password.length() < 12) {
            statusLabel.setText("Password must be at least 12 characters long");
            statusLabel.setForeground(C_TEXT_MID);
            return false;
        }

        if (!password.equals(confirmPassword)) {
            statusLabel.setText("Passwords do not match");
            statusLabel.setForeground(C_TEXT_MID);
            return false;
        }

        return true;
    }
}
