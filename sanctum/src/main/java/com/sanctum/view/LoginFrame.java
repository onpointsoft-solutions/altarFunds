package com.sanctum.view;

import com.sanctum.service.AuthService;
import com.sanctum.api.SanctumApiClient;
import com.sanctum.util.LogoLoader;
import com.sanctum.auth.SessionManager;

import javax.swing.*;
import javax.swing.border.*;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.concurrent.CompletableFuture;
import javax.swing.SwingWorker;

/**
 * Modern login frame with dark theme matching ChurchAdminFrame
 */
public class LoginFrame extends JFrame {
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

    // ── Fonts ──────────────────────────────────────────────────────────────────────
    private static final Font FONT_TITLE  = new Font("Monospaced", Font.BOLD,  28);
    private static final Font FONT_LABEL  = new Font("Monospaced", Font.BOLD,  12);
    private static final Font FONT_VALUE  = new Font("Monospaced", Font.BOLD,  16);
    private static final Font FONT_SMALL  = new Font("Monospaced", Font.PLAIN, 11);
    private static final Font FONT_BUTTON = new Font("Monospaced", Font.BOLD,  14);

    private JTextField emailField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JLabel statusLabel;
    private final AuthService authService;
    private final SessionManager sessionManager;
    
    // Loading components
    private JProgressBar loadingBar;
    private JLabel loadingLabel;
    private JPanel loadingPanel;
    
    // Remember me checkbox
    private JCheckBox rememberMeCheckBox;
    
    // Dashboard navigation buttons (no longer used but kept for potential future use)
    // private JButton churchAdminButton;
    // private JButton pastorButton;
    // private JButton secretaryButton;
    // private JButton treasurerButton;
    // private JButton memberButton;
    
    // Window dragging
    private int mouseX, mouseY;
    
    public LoginFrame() {
        this.authService = new AuthService();
        this.sessionManager = SessionManager.getInstance();
        
        // Check if user is already logged in
        if (sessionManager.isLoggedIn()) {
            System.out.println("User already logged in: " + sessionManager.getCurrentUser());
            SwingUtilities.invokeLater(() -> {
                dispose();
                navigateToDashboard(sessionManager.getUserRole());
            });
            return;
        }
        
        initializeFrame();
        createComponents();
        layoutComponents();
        setupEventHandlers();
        setupStatusColorReset();
        setupEscapeKey();
    }
    
    private void initializeFrame() {
        setTitle("Sanctum — Church Management System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        // Set application icon
        setApplicationIcon();
        
        // Set to full screen
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
                System.out.println("LoginFrame PNG icon loaded successfully - Size: " + iconImage.getWidth(null) + "x" + iconImage.getHeight(null));
            } else {
                System.out.println("LoginFrame PNG icon failed to load, trying ICO fallback");
                iconImage = loadIconFromResources("/images/icon.ico");
                
                if (iconImage != null) {
                    setIconImage(iconImage);
                    System.out.println("LoginFrame ICO icon loaded successfully as fallback - Size: " + iconImage.getWidth(null) + "x" + iconImage.getHeight(null));
                } else {
                    System.out.println("LoginFrame Both PNG and ICO fallback failed - using default icon");
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to set LoginFrame application icon: " + e.getMessage());
        }
    }
    
    /**
     * Load icon image from resources using ImageIO for better format support
     */
    private Image loadIconFromResources(String path) {
        try {
            InputStream inputStream = LoginFrame.class.getResourceAsStream(path);
            if (inputStream == null) {
                System.out.println("LoginFrame Resource not found: " + path);
                return null;
            }
            
            // Use ImageIO to read the image (better for ICO files)
            BufferedImage bufferedImage = ImageIO.read(inputStream);
            inputStream.close();
            
            if (bufferedImage != null) {
                System.out.println("LoginFrame Successfully loaded image from " + path + " - Size: " + bufferedImage.getWidth() + "x" + bufferedImage.getHeight());
                return bufferedImage;
            } else {
                System.out.println("LoginFrame Failed to read image from " + path);
                return null;
            }
            
        } catch (Exception e) {
            System.err.println("LoginFrame Error loading image from " + path + ": " + e.getMessage());
            return null;
        }
    }
    
    private void createComponents() {
        // Email field
        emailField = new JTextField("Email");
        emailField.setFont(FONT_VALUE);
        emailField.setForeground(C_TEXT);
        emailField.setBackground(C_CARD);
        emailField.setCaretColor(C_GOLD);
        emailField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(C_BORDER),
            BorderFactory.createEmptyBorder(12, 15, 12, 15)
        ));
        emailField.setMinimumSize(new Dimension(400, 40));
        emailField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        emailField.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                if (emailField.getText().equals("Email")) {
                    emailField.setText("");
                }
                emailField.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(C_GOLD, 2),
                    BorderFactory.createEmptyBorder(11, 14, 11, 14)
                ));
            }
            public void focusLost(FocusEvent e) {
                if (emailField.getText().isEmpty()) {
                    emailField.setText("Email");
                }
                emailField.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(C_BORDER),
                    BorderFactory.createEmptyBorder(12, 15, 12, 15)
                ));
            }
        });
        
        // Password field
        passwordField = new JPasswordField("Password");
        passwordField.setFont(FONT_VALUE);
        passwordField.setForeground(C_TEXT);
        passwordField.setBackground(C_CARD);
        passwordField.setCaretColor(C_GOLD);
        passwordField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(C_BORDER),
            BorderFactory.createEmptyBorder(12, 15, 12, 15)
        ));
        passwordField.setMinimumSize(new Dimension(400, 40));
        passwordField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        passwordField.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                if (String.valueOf(passwordField.getPassword()).equals("Password")) {
                    passwordField.setText("");
                }
            }
            public void focusLost(FocusEvent e) {
                if (String.valueOf(passwordField.getPassword()).isEmpty()) {
                    passwordField.setText("Password");
                }
            }
        });
        
        // Login button
        loginButton = createGlowButton("SIGN IN", C_GOLD);
        loginButton.setPreferredSize(new Dimension(0, 50));
        
        // Status label
        statusLabel = new JLabel("", SwingConstants.CENTER);
        statusLabel.setFont(FONT_SMALL);
        statusLabel.setForeground(C_TEXT_MID);
        
        // Loading components
        loadingLabel = new JLabel("Authenticating...", SwingConstants.CENTER);
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
        
        // Note: Dashboard buttons removed for cleaner login interface
    }
    
    private JButton createGlowButton(String text, Color accent) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean hover = getModel().isRollover();
                boolean pressed = getModel().isPressed();
                
                // Background
                if (pressed) {
                    g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 60));
                } else if (hover) {
                    g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 40));
                } else {
                    g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 20));
                }
                g2.fillRoundRect(0,0,getWidth(),getHeight(),8,8);
                
                // Border
                g2.setColor(accent);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(0,0,getWidth()-1,getHeight()-1,8,8);
                
                // Glow effect
                if (hover) {
                    g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 30));
                    g2.fillRoundRect(2,2,getWidth()-3,getHeight()-3,8,8);
                }
                
                // Text
                g2.setFont(FONT_BUTTON);
                g2.setColor(accent);
                FontMetrics fm = g2.getFontMetrics();
                int tx = (getWidth()-fm.stringWidth(text))/2;
                int ty = (getHeight()+fm.getAscent()-fm.getDescent())/2;
                g2.drawString(text, tx, ty);
                g2.dispose();
            }
        };
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(0, 40));
        return btn;
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
                
                // Decorative elements
                g2.setColor(new Color(C_GOLD.getRed(), C_GOLD.getGreen(), C_GOLD.getBlue(), 20));
                g2.fillOval(getWidth() - 200, 100, 150, 150);
                g2.fillOval(getWidth() - 300, 300, 100, 100);
                
                g2.dispose();
            }
        };
        mainContainer.setOpaque(false);
        
        // Center panel - Login form
        JPanel centerPanel = createCenterPanel();
        
        // Window controls
        JPanel windowControls = createWindowControls();
        
        add(windowControls, BorderLayout.NORTH);
        add(mainContainer, BorderLayout.CENTER);
        
        // Left side panel (empty for spacing)
        JPanel leftPanel = new JPanel();
        leftPanel.setOpaque(false);
        leftPanel.setPreferredSize(new Dimension(150, 0));
        
        // Right side panel (empty for spacing)  
        JPanel rightPanel = new JPanel();
        rightPanel.setOpaque(false);
        rightPanel.setPreferredSize(new Dimension(150, 0));
        
        mainContainer.add(leftPanel, BorderLayout.WEST);
        mainContainer.add(centerPanel, BorderLayout.CENTER);
        mainContainer.add(rightPanel, BorderLayout.EAST);
    }
    
    private JPanel createCenterPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setOpaque(false);
        
        // Left side - App Info Panel
        JPanel appInfoPanel = createAppInfoPanel();
        
        // Right side - Login Form Panel
        JPanel loginFormPanel = createLoginFormPanel();
        
        // Add panels with flexible sizing
        mainPanel.add(appInfoPanel, BorderLayout.WEST);
        mainPanel.add(loginFormPanel, BorderLayout.CENTER);
        
        return mainPanel;
    }
    
    private JPanel createAppInfoPanel() {
        JPanel panel = new JPanel(new GridBagLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Gradient background
                GradientPaint gradient = new GradientPaint(0, 0, C_SURFACE, getWidth(), getHeight(), new Color(30, 35, 45));
                g2.setPaint(gradient);
                g2.fillRect(0, 0, getWidth(), getHeight());
                
                // Decorative elements
                g2.setColor(new Color(C_GOLD.getRed(), C_GOLD.getGreen(), C_GOLD.getBlue(), 20));
                g2.fillOval(getWidth() - 100, 50, 80, 80);
                g2.fillOval(getWidth() - 60, 150, 40, 40);
                
                g2.dispose();
            }
        };
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(60, 80, 60, 40));
        panel.setPreferredSize(new Dimension(400, 0));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(20, 0, 20, 0);
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // App title
        gbc.gridx = 0; gbc.gridy = 0;
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setOpaque(false);
        
        // Logo placeholder
        JLabel logoPanel = LogoLoader.createLogoLabel(new Dimension(120, 120));
        
        JLabel appTitle = new JLabel("Sanctum", SwingConstants.CENTER);
        appTitle.setFont(FONT_TITLE);
        appTitle.setForeground(C_GOLD);
        
        titlePanel.add(logoPanel, BorderLayout.NORTH);
        titlePanel.add(appTitle, BorderLayout.CENTER);
        panel.add(titlePanel, gbc);
        
        // App subtitle
        gbc.gridy = 1;
        JLabel appSubtitle = new JLabel("Church Management System");
        appSubtitle.setFont(FONT_LABEL);
        appSubtitle.setForeground(C_TEXT_MID);
        panel.add(appSubtitle, gbc);
        
        // Version info
        gbc.gridy = 2;
        gbc.insets = new Insets(40, 0, 10, 0);
        JLabel versionLabel = new JLabel("Version 1.0.0");
        versionLabel.setFont(FONT_SMALL);
        versionLabel.setForeground(C_TEXT_MID);
        panel.add(versionLabel, gbc);
        
        // Features section
        gbc.gridy = 3;
        gbc.insets = new Insets(30, 0, 15, 0);
        JLabel featuresTitle = new JLabel("// FEATURES");
        featuresTitle.setFont(FONT_LABEL);
        featuresTitle.setForeground(C_TEXT_MID);
        panel.add(featuresTitle, gbc);
        
        // Features list
        String[] features = {
            "• Member Management",
            "• Donation Tracking", 
            "• Event Planning",
            "• Staff Administration",
            "• Report Generation"
        };
        
        gbc.insets = new Insets(5, 10, 5, 0);
        for (int i = 0; i < features.length; i++) {
            gbc.gridy = 4 + i;
            JLabel featureLabel = new JLabel(features[i]);
            featureLabel.setFont(FONT_SMALL);
            featureLabel.setForeground(C_TEXT);
            panel.add(featureLabel, gbc);
        }
        
        // Footer info
        gbc.gridy = 9;
        gbc.insets = new Insets(40, 0, 0, 0);
        JLabel footerLabel = new JLabel("© 2026 Sanctum Solutions");
        footerLabel.setFont(FONT_SMALL);
        footerLabel.setForeground(C_TEXT_MID);
        panel.add(footerLabel, gbc);
        
        return panel;
    }
    
    private JPanel createLoginFormPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(15, 15, 15, 15);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.CENTER;
        
        // Login card
        JPanel loginCard = new JPanel(new GridBagLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                g2.setColor(C_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                
                g2.setColor(C_BORDER);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 12, 12);
                
                // Glow effect
                g2.setColor(new Color(C_GOLD.getRed(), C_GOLD.getGreen(), C_GOLD.getBlue(), 30));
                g2.fillRoundRect(1, 1, getWidth()-2, 4, 4, 4);
                
                g2.dispose();
            }
        };
        loginCard.setOpaque(false);
        loginCard.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));
        
        GridBagConstraints cardGbc = new GridBagConstraints();
        cardGbc.insets = new Insets(8, 8, 8, 8);
        cardGbc.fill = GridBagConstraints.HORIZONTAL;
        cardGbc.weightx = 1.0;
        cardGbc.anchor = GridBagConstraints.CENTER;
        
        // Login title
        cardGbc.gridx = 0; cardGbc.gridy = 0; cardGbc.gridwidth = 2;
        JLabel loginTitle = new JLabel("LOGIN", SwingConstants.CENTER);
        loginTitle.setFont(new Font("Monospaced", Font.BOLD, 18));
        loginTitle.setForeground(C_GOLD);
        loginCard.add(loginTitle, cardGbc);
        
        // Spacer
        cardGbc.gridy = 1;
        cardGbc.insets = new Insets(10, 8, 10, 8);
        loginCard.add(Box.createVerticalStrut(5), cardGbc);
        
        // Email field
        cardGbc.gridy = 2;
        cardGbc.insets = new Insets(8, 8, 3, 8);
        JLabel emailLabel = new JLabel("EMAIL");
        emailLabel.setFont(FONT_SMALL);
        emailLabel.setForeground(C_TEXT_MID);
        loginCard.add(emailLabel, cardGbc);
        
        cardGbc.gridy = 3;
        cardGbc.insets = new Insets(0, 8, 8, 8);
        loginCard.add(emailField, cardGbc);
        
        // Password field
        cardGbc.gridy = 4;
        cardGbc.insets = new Insets(8, 8, 3, 8);
        JLabel passwordLabel = new JLabel("Password:");
        passwordLabel.setFont(FONT_SMALL);
        passwordLabel.setForeground(C_TEXT_MID);
        loginCard.add(passwordLabel, cardGbc);
        
        cardGbc.gridy = 5;
        cardGbc.insets = new Insets(0, 8, 8, 8);
        loginCard.add(passwordField, cardGbc);
        
        // Remember me checkbox
        cardGbc.gridy = 6;
        rememberMeCheckBox = new JCheckBox("Remember me");
        rememberMeCheckBox.setFont(FONT_SMALL);
        rememberMeCheckBox.setForeground(C_TEXT_MID);
        rememberMeCheckBox.setOpaque(false);
        rememberMeCheckBox.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        loginCard.add(rememberMeCheckBox, cardGbc);
        
        // Login button
        cardGbc.gridy = 7;
        cardGbc.insets = new Insets(5, 8, 8, 8);
        loginCard.add(loginButton, cardGbc);
        
        // Status label
        cardGbc.gridy = 8;
        cardGbc.insets = new Insets(3, 8, 8, 8);
        loginCard.add(statusLabel, cardGbc);
        
        // Loading panel (initially hidden)
        cardGbc.gridy = 9;
        cardGbc.insets = new Insets(5, 8, 8, 8);
        loginCard.add(loadingPanel, cardGbc);
        loadingPanel.setVisible(false);
        
        // Registration link
        cardGbc.gridy = 10;
        cardGbc.insets = new Insets(15, 8, 8, 8);
        JPanel registerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        registerPanel.setOpaque(false);
        
        JLabel registerLabel = new JLabel("Don't have an account? ");
        registerLabel.setFont(FONT_SMALL);
        registerLabel.setForeground(C_TEXT_MID);
        
        JButton registerButton = new JButton("Register") {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                if (getModel().isRollover()) {
                    g2.setColor(new Color(C_GOLD.getRed(), C_GOLD.getGreen(), C_GOLD.getBlue(), 30));
                    g2.fillRect(0, 0, getWidth(), getHeight());
                }
                g2.setColor(C_GOLD);
                g2.setFont(FONT_SMALL);
                FontMetrics fm = g2.getFontMetrics();
                int tx = (getWidth() - fm.stringWidth("Register")) / 2;
                int ty = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString("Register", tx, ty);
                g2.dispose();
            }
        };
        registerButton.setContentAreaFilled(false);
        registerButton.setBorderPainted(false);
        registerButton.setFocusPainted(false);
        registerButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        registerButton.setFont(FONT_SMALL);
        registerButton.setForeground(C_GOLD);
        registerButton.addActionListener(e -> {
            dispose();
            new RegistrationFrame().setVisible(true);
        });
        
        registerPanel.add(registerLabel);
        registerPanel.add(registerButton);
        loginCard.add(registerPanel, cardGbc);
        
        // Add login card to panel
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(loginCard, gbc);
        
        return panel;
    }
    
    private JPanel createLogoPanel() {
        JPanel logoPanel = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Draw custom logo (can be replaced with image)
                int size = Math.min(getWidth(), getHeight()) - 20;
                int x = (getWidth() - size) / 2;
                int y = (getHeight() - size) / 2;
                
                // Draw church building icon
                g2.setColor(C_GOLD);
                
                // Base of church
                g2.fillRect(x + size/4, y + size*2/3, size/2, size/3);
                
                // Roof
                int[] roofX = {x, x + size/2, x + size};
                int[] roofY = {y + size*2/3, y + size/3, y + size*2/3};
                g2.fillPolygon(roofX, roofY, 3);
                
                // Cross
                g2.fillRect(x + size/2 - 2, y + size/4, 4, size/3);
                g2.fillRect(x + size/2 - size/8, y + size/3 - 2, size/4, 4);
                
                // Door
                g2.setColor(C_BG);
                g2.fillRect(x + size/2 - 8, y + size*3/4, 16, size/4);
                
                // Windows
                g2.fillRect(x + size/4 + 4, y + size/2 + 4, 8, 8);
                g2.fillRect(x + 3*size/4 - 12, y + size/2 + 4, 8, 8);
                
                // Glow effect
                g2.setColor(new Color(C_GOLD.getRed(), C_GOLD.getGreen(), C_GOLD.getBlue(), 30));
                g2.fillRoundRect(x - 5, y - 5, size + 10, size + 10, 15, 15);
                
                g2.dispose();
            }
        };
        logoPanel.setOpaque(false);
        logoPanel.setPreferredSize(new Dimension(120, 120));
        logoPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
        
        // Add tooltip for logo replacement instructions
        logoPanel.setToolTipText("Logo placeholder - replace with custom image in createLogoPanel() method");
        
        return logoPanel;
    }
    
    private JPanel createWindowControls() {
        JPanel panel = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(C_SURFACE);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(C_BORDER);
                g2.fillRect(0, getHeight() - 1, getWidth(), 1);
                g2.dispose();
            }
        };
        panel.setPreferredSize(new Dimension(0, 30));
        panel.setOpaque(false);
        
        // Left side - Title
        JLabel title = new JLabel("  Sanctum Admin");
        title.setFont(FONT_SMALL);
        title.setForeground(C_TEXT_MID);
        
        // Right side - Controls
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        controls.setOpaque(false);
        
        JButton minimizeBtn = new JButton("─") {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                if (getModel().isRollover()) {
                    g2.setColor(new Color(255, 255, 255, 30));
                    g2.fillRect(0, 0, getWidth(), getHeight());
                }
                g2.setColor(C_TEXT_MID);
                g2.setFont(FONT_SMALL);
                FontMetrics fm = g2.getFontMetrics();
                int tx = (getWidth() - fm.stringWidth("─")) / 2;
                int ty = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString("─", tx, ty);
                g2.dispose();
            }
        };
        minimizeBtn.setContentAreaFilled(false);
        minimizeBtn.setBorderPainted(false);
        minimizeBtn.setFocusPainted(false);
        minimizeBtn.setPreferredSize(new Dimension(40, 30));
        minimizeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        minimizeBtn.addActionListener(e -> setState(JFrame.ICONIFIED));
        
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
        closeBtn.setContentAreaFilled(false);
        closeBtn.setBorderPainted(false);
        closeBtn.setFocusPainted(false);
        closeBtn.setPreferredSize(new Dimension(40, 30));
        closeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeBtn.addActionListener(e -> System.exit(0));
        
        controls.add(minimizeBtn);
        controls.add(closeBtn);
        
        panel.add(title, BorderLayout.WEST);
        panel.add(controls, BorderLayout.EAST);
        
        // Window dragging
        panel.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                mouseX = e.getX();
                mouseY = e.getY();
            }
        });
        
        panel.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                int x = e.getXOnScreen() - mouseX;
                int y = e.getYOnScreen() - mouseY;
                setLocation(x, y);
            }
        });
        
        return panel;
    }
    
    private void setupEventHandlers() {
        loginButton.addActionListener(e -> {
            System.out.println("=== LOGIN BUTTON CLICKED ===");
            String email = emailField.getText();
            String password = String.valueOf(passwordField.getPassword());

            System.out.println("UI Email input: " + email);
            System.out.println("Password provided: " + (password != null && !password.isEmpty() ? "Yes" : "No"));

            if (email.equals("Email") || email.isEmpty()) {
                System.out.println("LOGIN FAILED: Empty email field");
                statusLabel.setText("✗ Please enter email");
                statusLabel.setForeground(new Color(244, 67, 54)); // Red color for error
                return;
            }

            if (password.equals("Password") || password.isEmpty()) {
                System.out.println("LOGIN FAILED: Empty password field");
                statusLabel.setText("✗ Please enter password");
                statusLabel.setForeground(new Color(244, 67, 54)); // Red color for error
                return;
            }

            System.out.println("=== STARTING AUTHENTICATION PROCESS ===");
            loginButton.setEnabled(false);
            emailField.setEnabled(false);
            passwordField.setEnabled(false);
            rememberMeCheckBox.setEnabled(false);
            statusLabel.setText("");
            loadingPanel.setVisible(true);

            // Use SessionManager for login with proper async handling
            sessionManager.login(email, password, rememberMeCheckBox.isSelected())
                .thenAccept(success -> {
                    SwingUtilities.invokeLater(() -> {
                        System.out.println("=== AUTHENTICATION RESULT RECEIVED ===");
                        System.out.println("Success: " + success);
                        
                        loadingPanel.setVisible(false);
                        loginButton.setEnabled(true);
                        emailField.setEnabled(true);
                        passwordField.setEnabled(true);
                        rememberMeCheckBox.setEnabled(true);

                        if (success) {
                            String userRole = sessionManager.getUserRole();
                            System.out.println("=== LOGIN SUCCESSFUL ===");
                            System.out.println("User Role: " + userRole);
                            
                            // Show success message with better styling
                            statusLabel.setText("✓ Login successful! Redirecting...");
                            statusLabel.setForeground(new Color(76, 175, 80)); // Green color for success
                            
                            // Add a small delay before redirecting for better UX
                            Timer redirectTimer = new Timer(1500, evt -> {
                                SwingUtilities.invokeLater(() -> {
                                    dispose();
                                    navigateToDashboard(userRole);
                                });
                            });
                            redirectTimer.setRepeats(false);
                            redirectTimer.start();
                        } else {
                            System.out.println("=== LOGIN FAILED ===");
                            statusLabel.setText("✗ Invalid credentials");
                            statusLabel.setForeground(new Color(244, 67, 54)); // Red color for error
                        }
                    });
                })
                .exceptionally(ex -> {
                    SwingUtilities.invokeLater(() -> {
                        System.out.println("=== LOGIN EXCEPTION ===");
                        System.err.println("Login exception: " + ex.getMessage());
                        ex.printStackTrace();
                        
                        loadingPanel.setVisible(false);
                        loginButton.setEnabled(true);
                        emailField.setEnabled(true);
                        passwordField.setEnabled(true);
                        rememberMeCheckBox.setEnabled(true);
                        statusLabel.setText("Login failed: " + ex.getMessage());
                    });
                    return null;
                });
        });
        
        // Enter key support
        KeyListener enterKeyListener = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    loginButton.doClick();
                }
            }
        };
        
        emailField.addKeyListener(enterKeyListener);
        passwordField.addKeyListener(enterKeyListener);
        
        // Escape key to close - add to root pane
        JRootPane rootPane = getRootPane();
        rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "escape");
        rootPane.getActionMap().put("escape", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });
    }
    
    private void setupStatusColorReset() {
        // Add listeners to reset status color when user starts typing
        java.awt.event.KeyListener colorResetListener = new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent e) {
                statusLabel.setForeground(C_TEXT_MID); // Reset to default color
            }
        };
        
        emailField.addKeyListener(colorResetListener);
        passwordField.addKeyListener(colorResetListener);
    }
    
    private void navigateToDashboard(String userRole) {
        System.out.println("=== NAVIGATING TO DASHBOARD ===");
        System.out.println("User Role: " + userRole);
        
        // Show loading indicator (check for null first)
        if (loadingPanel != null) {
            loadingPanel.setVisible(true);
        }
        if (loginButton != null) {
            loginButton.setEnabled(false);
        }
        
        // Use SwingWorker for async dashboard loading
        SwingWorker<JFrame, Void> worker = new SwingWorker<>() {
            @Override
            protected JFrame doInBackground() throws Exception {
                switch (userRole.toLowerCase()) {
                    case "admin":
                    case "denomination_admin":
                        return new ChurchAdminFrame();
                    case "pastor":
                        return new PastorDashboardFrame();
                    case "treasurer":
                        // Optimize treasurer loading with pre-initialization
                        return new TreasurerDashboardFrame();
                    case "usher":
                        return new UsherDashboardFrame();
                    case "youth_leader":
                        return YouthLeaderDashboard.createDashboard(SanctumApiClient.getAuthToken());
                    case "secretary":
                        return SecretaryDashboard.createDashboard(SanctumApiClient.getAuthToken());
                    case "music_director":
                        // MusicDirectorDashboard not implemented yet
                        return new PastorDashboardFrame(); // fallback
                    default:
                        return new PastorDashboardFrame(); // fallback
                }
            }
            
            @Override
            protected void done() {
                try {
                    JFrame dashboard = get();
                    if (dashboard != null) {
                        // Hide loading and show dashboard
                        SwingUtilities.invokeLater(() -> {
                            if (loadingPanel != null) {
                                loadingPanel.setVisible(false);
                            }
                            dispose();
                            dashboard.setVisible(true);
                        });
                    }
                } catch (Exception e) {
                    SwingUtilities.invokeLater(() -> {
                        if (loadingPanel != null) {
                            loadingPanel.setVisible(false);
                        }
                        if (loginButton != null) {
                            loginButton.setEnabled(true);
                        }
                        statusLabel.setText("Dashboard loading failed. Please try again.");
                        statusLabel.setForeground(C_TEXT_MID);
                    });
                }
            }
        };
        
        worker.execute();
    }
    
    // Escape key to close - add to root pane
    private void setupEscapeKey() {
        JRootPane rootPane = getRootPane();
        rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "escape");
        rootPane.getActionMap().put("escape", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });
    }
}
