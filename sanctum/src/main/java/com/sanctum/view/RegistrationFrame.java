package com.sanctum.view;

import com.sanctum.api.SanctumApiClient;
import com.sanctum.model.Church;
import com.sanctum.model.User;
import com.sanctum.repository.ChurchRepository;
import com.sanctum.repository.UserRepository;
import com.sanctum.security.PasswordUtil;
import com.sanctum.service.AuthService;
import com.sanctum.ui.ModernButton;
import com.sanctum.ui.RoundedPanel;
import com.sanctum.util.LogoLoader;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.awt.Image;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * Modern registration frame with dark theme matching LoginFrame
 */
public class RegistrationFrame extends JFrame {
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
    // Wizard steps - using these for sidebar labels
    private static final String[] STEP_TITLES = {
        "1. Church Info",
        "2. Contact", 
        "3. Pastor",
        "4. Statistics",
        "5. Admin",
        "6. Review"
    };
    
    private static final String[] STEP_DESCRIPTIONS = {
        "Basic church details",
        "Contact information",
        "Pastor information",
        "Church statistics",
        "Administrator account",
        "Review and submit"
    };
    private JPanel stepIndicatorPanel;
    private JLabel stepTitleLabel;
    private JLabel stepDescriptionLabel;
    private JPanel cardPanel;

    private JTextField churchNameField;
    private JTextField churchAddressField;
    private JTextField churchCityField;
    private JTextField churchCountyField;
    private JTextField churchPhoneField;
    private JTextField churchEmailField;
    private JTextField churchWebsiteField;
    private JTextField churchPastorField;
    private JTextField churchPastorPhoneField;
    private JTextField churchPastorEmailField;
    private JComboBox<String> churchTypeField;
    private JTextField churchMembershipField;
    private JTextField churchAttendanceField;
    
    private JTextField adminUsernameField;
    private JTextField adminFirstNameField;
    private JTextField adminLastNameField;
    private JTextField adminEmailField;
    private JTextField adminPhoneField;
    private JTextField adminTitleField;
    private JPasswordField adminPasswordField;
    private JPasswordField adminConfirmPasswordField;
    
    private JButton registerButton;
    private JButton cancelButton;
    private JLabel statusLabel;
    
    // Sidebar navigation components
    private JPanel sidebar;
    private JPanel contentArea;
    private CardLayout cardLayout;
    private JButton[] stepButtons;
    private int currentStep = 0;
    
    // Content panels for each step
    private JPanel churchBasicPanel;
    private JPanel churchContactPanel;
    private JPanel pastorPanel;
    private JPanel churchStatsPanel;
    private JPanel adminDetailsPanel;
    private JPanel reviewPanel;
    
    // Loading components
    private JProgressBar loadingBar;
    private JLabel loadingLabel;
    private JPanel loadingPanel;
    
    private final ChurchRepository churchRepository;
    private final UserRepository userRepository;
    private final AuthService authService;
    
    private static final Logger logger = Logger.getLogger(RegistrationFrame.class.getName());
    
    private int mouseX, mouseY;
    
    public RegistrationFrame() {
        this.churchRepository = new ChurchRepository();
        this.userRepository = new UserRepository();
        this.authService = new AuthService();
        
        setApplicationIcon();
        initializeFrame();
        createComponents();
        layoutComponents();
        setupEventHandlers();
    }
    
    private void initializeFrame() {
        setTitle("Sanctum — Church Registration");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
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
    
    private void createComponents() {
        // Create buttons first (needed by panels)
        registerButton = createGlowButton("CREATE ACCOUNT", C_GOLD);
        registerButton.setPreferredSize(new Dimension(200, 50));
        
        cancelButton = createGlowButton("Cancel", C_TEXT_MID);
        cancelButton.setPreferredSize(new Dimension(120, 45));
        
        statusLabel = new JLabel("", SwingConstants.CENTER);
        statusLabel.setFont(FONT_SMALL);
        statusLabel.setForeground(C_TEXT_DIM);
        
        // Create loading components
        loadingLabel = new JLabel("Creating your church account...", SwingConstants.CENTER);
        loadingLabel.setFont(FONT_LABEL);
        loadingLabel.setForeground(C_GOLD);
        
        loadingBar = new JProgressBar();
        loadingBar.setIndeterminate(true);
        loadingBar.setStringPainted(false);
        loadingBar.setForeground(C_GOLD);
        loadingBar.setBackground(C_SURFACE);
        loadingBar.setBorder(BorderFactory.createEmptyBorder());
        
        loadingPanel = new JPanel(new GridLayout(2, 1, 0, 10));
        loadingPanel.setOpaque(false);
        loadingPanel.add(loadingLabel);
        loadingPanel.add(loadingBar);
        loadingPanel.setVisible(false);
        
        // Create CardLayout for content area
        cardLayout = new CardLayout();
        contentArea = new JPanel(cardLayout);
        contentArea.setOpaque(false);
        
        // Create panels for each step
        churchBasicPanel = createChurchBasicPanel();
        churchContactPanel = createChurchContactPanel();
        pastorPanel = createPastorPanel();
        churchStatsPanel = createChurchStatsPanel();
        adminDetailsPanel = createAdminDetailsPanel();
        reviewPanel = createReviewPanel();
        
        // Add panels to content area
        contentArea.add(churchBasicPanel, "step0");
        contentArea.add(churchContactPanel, "step1");
        contentArea.add(pastorPanel, "step2");
        contentArea.add(churchStatsPanel, "step3");
        contentArea.add(adminDetailsPanel, "step4");
        contentArea.add(reviewPanel, "step5");
        
        // Initialize sidebar
        createSidebar();
    }
    
    private void createSidebar() {
        sidebar = new JPanel(new BorderLayout(0, 15));
        sidebar.setOpaque(false);
        sidebar.setBorder(BorderFactory.createEmptyBorder(20, 15, 20, 15));
        sidebar.setPreferredSize(new Dimension(200, 0));
        
        // Logo at top
        JLabel logoPanel = LogoLoader.createLogoLabel(new Dimension(100, 100));
        logoPanel.setPreferredSize(new Dimension(0, 100));
        
        // Step buttons panel
        JPanel stepsPanel = new JPanel();
        stepsPanel.setLayout(new BoxLayout(stepsPanel, BoxLayout.Y_AXIS));
        stepsPanel.setOpaque(false);
        
        stepButtons = new JButton[6];
        for (int i = 0; i < 6; i++) {
            final int step = i;
            JButton stepBtn = createStepButton(STEP_TITLES[i], STEP_DESCRIPTIONS[i], i == 0);
            stepBtn.addActionListener(e -> navigateToStep(step));
            stepButtons[i] = stepBtn;
            stepsPanel.add(stepBtn);
            stepsPanel.add(Box.createVerticalStrut(8));
        }
        
        // Progress indicator
        JLabel progressLabel = new JLabel("Step 1 of 6", SwingConstants.CENTER);
        progressLabel.setFont(FONT_SMALL);
        progressLabel.setForeground(C_TEXT_DIM);
        progressLabel.setName("progressLabel");
        
        sidebar.add(logoPanel, BorderLayout.NORTH);
        sidebar.add(stepsPanel, BorderLayout.CENTER);
        sidebar.add(progressLabel, BorderLayout.SOUTH);
    }
    
    private JButton createStepButton(String title, String description, boolean active) {
        JButton btn = new JButton() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                boolean isActive = stepButtons != null && this == stepButtons[currentStep];
                boolean isCompleted = stepButtons != null && getStepIndex(this) < currentStep;
                
                // Background
                if (isActive) {
                    g2.setColor(C_GOLD);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                } else if (isCompleted) {
                    g2.setColor(new Color(C_GOLD.getRed(), C_GOLD.getGreen(), C_GOLD.getBlue(), 100));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                } else if (getModel().isRollover()) {
                    g2.setColor(C_CARD_HOVER);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                }
                
                // Text
                g2.setFont(FONT_LABEL);
                if (isActive) {
                    g2.setColor(C_BG);
                } else {
                    g2.setColor(isCompleted ? C_GOLD : C_TEXT_MID);
                }
                
                FontMetrics fm = g2.getFontMetrics();
                int textY = (getHeight() + fm.getAscent() - fm.getDescent()) / 2 - 3;
                g2.drawString(title, 15, textY);
                
                g2.setFont(FONT_SMALL);
                g2.setColor(isActive ? C_BG : C_TEXT_DIM);
                FontMetrics fmSmall = g2.getFontMetrics();
                int descY = textY + fmSmall.getHeight();
                g2.drawString(description, 15, descY);
                
                g2.dispose();
            }
        };
        btn.setPreferredSize(new Dimension(170, 50));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }
    
    private int getStepIndex(JButton btn) {
        for (int i = 0; i < stepButtons.length; i++) {
            if (stepButtons[i] == btn) return i;
        }
        return -1;
    }
    
    private void navigateToStep(int step) {
        if (step >= 0 && step < 6) {
            currentStep = step;
            cardLayout.show(contentArea, "step" + step);
            updateSidebarSelection();
            if (step == 5) {
                updateReviewContent();
            }
        }
    }
    
    private void updateSidebarSelection() {
        for (int i = 0; i < stepButtons.length; i++) {
            stepButtons[i].repaint();
        }
        // Update progress label
        for (Component c : sidebar.getComponents()) {
            if (c instanceof JLabel && "progressLabel".equals(c.getName())) {
                ((JLabel) c).setText("Step " + (currentStep + 1) + " of 6");
            }
        }
    }
    
    private void nextStep() {
        if (currentStep < 5) {
            navigateToStep(currentStep + 1);
        }
    }
    
    private void previousStep() {
        if (currentStep > 0) {
            navigateToStep(currentStep - 1);
        }
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
    
    private JPanel createChurchPanel() {
        JPanel panel = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Dark gradient background
                GradientPaint gradient = new GradientPaint(
                    0, 0, C_SURFACE,
                    getWidth(), getHeight(), new Color(30, 35, 45)
                );
                g2.setPaint(gradient);
                g2.fillRect(0, 0, getWidth(), getHeight());
                
                // Decorative elements
                g2.setColor(new Color(C_GOLD.getRed(), C_GOLD.getGreen(), C_GOLD.getBlue(), 20));
                g2.fillOval(getWidth() - 150, 50, 100, 100);
                g2.fillOval(getWidth() - 100, 200, 60, 60);
                
                g2.dispose();
            }
        };
        panel.setOpaque(false);
        
        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(40, 60, 20, 60));
        
        JLabel titleLabel = new JLabel("Church Information");
        titleLabel.setFont(FONT_TITLE);
        titleLabel.setForeground(Color.WHITE);
        
        JLabel descLabel = new JLabel("Tell us about your church. This information will help us set up your account.");
        descLabel.setFont(FONT_VALUE);
        descLabel.setForeground(Color.WHITE);
        
        headerPanel.add(titleLabel, BorderLayout.NORTH);
        headerPanel.add(Box.createVerticalStrut(8), BorderLayout.CENTER);
        headerPanel.add(descLabel, BorderLayout.SOUTH);
        
        // Form content
        JPanel formPanel = new JPanel();
        formPanel.setOpaque(false);
        formPanel.setBorder(BorderFactory.createEmptyBorder(10, 60, 10, 60));
        
        // Create scrollable form area
        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getViewport().setBackground(C_SURFACE);
        
        // Customize scroll bar appearance
        JScrollBar verticalScrollBar = scrollPane.getVerticalScrollBar();
        verticalScrollBar.setUnitIncrement(16);
        verticalScrollBar.setBlockIncrement(32);
        verticalScrollBar.setPreferredSize(new Dimension(12, 0));
        
        // Create form grid
        JPanel gridPanel = new JPanel(new GridBagLayout()) {
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
        gridPanel.setOpaque(false);
        gridPanel.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // Church fields
        addFormField(gridPanel, gbc, "Church Name", churchNameField = createTextField("Enter church name"), 0, 0);
        addFormField(gridPanel, gbc, "Address Line 1", churchAddressField = createTextField("Enter street address"), 0, 1);
        addFormField(gridPanel, gbc, "City", churchCityField = createTextField("Enter city"), 0, 2);
        addFormField(gridPanel, gbc, "County", churchCountyField = createTextField("Enter county"), 0, 3);
        addFormField(gridPanel, gbc, "Church Phone", churchPhoneField = createTextField("+254 7XX XXX XXX"), 0, 4);
        addFormField(gridPanel, gbc, "Church Email", churchEmailField = createTextField("church@example.com"), 0, 5);
        addFormField(gridPanel, gbc, "Website", churchWebsiteField = createTextField("www.churchwebsite.com (optional)"), 0, 6);
        addFormField(gridPanel, gbc, "Senior Pastor Name", churchPastorField = createTextField("Pastor's full name"), 0, 7);
        addFormField(gridPanel, gbc, "Pastor Phone", churchPastorPhoneField = createTextField("+254 7XX XXX XXX"), 0, 8);
        addFormField(gridPanel, gbc, "Pastor Email", churchPastorEmailField = createTextField("pastor@church.com"), 0, 9);
        addFormField(gridPanel, gbc, "Church Type", churchTypeField = createChurchTypeDropdown(), 0, 10);
        addFormField(gridPanel, gbc, "Membership Count", churchMembershipField = createTextField("Number of members"), 0, 11);
        addFormField(gridPanel, gbc, "Average Attendance", churchAttendanceField = createTextField("Average weekly attendance"), 0, 12);
        
        // Add some spacing at the bottom
        gbc.gridx = 0;
        gbc.gridy = 13;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;
        gridPanel.add(Box.createVerticalGlue(), gbc);
        
        // Set minimum size to ensure scrolling
        gridPanel.setPreferredSize(new Dimension(700, 800));
        gridPanel.setMinimumSize(new Dimension(600, 700));
        
        scrollPane.getViewport().setView(gridPanel);
        formPanel.add(scrollPane, BorderLayout.CENTER);
        
        // Add navigation buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 10));
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(5, 60, 5, 60));
        
        JButton nextButton = new JButton("Next →");
        nextButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        nextButton.setForeground(Color.WHITE);
        nextButton.setBackground(C_GOLD);
        nextButton.setOpaque(true);
        nextButton.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));
        nextButton.setFocusPainted(false);
        nextButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        nextButton.addActionListener(e -> nextStep());
        
        buttonPanel.add(nextButton);
        
        // Create main container with proper layout
        JPanel mainContainer = new JPanel(new BorderLayout());
        mainContainer.setOpaque(false);
        mainContainer.add(headerPanel, BorderLayout.NORTH);
        mainContainer.add(formPanel, BorderLayout.CENTER);
        mainContainer.add(buttonPanel, BorderLayout.SOUTH);
        
        panel.add(mainContainer, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createChurchBasicPanel() {
        JPanel panel = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Dark background with rounded corners
                g2.setColor(C_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                
                // Border
                g2.setColor(C_BORDER);
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 8, 8);
                
                // Highlight for selected tab
                g2.setColor(new Color(C_GOLD.getRed(), C_GOLD.getGreen(), C_GOLD.getBlue(), 30));
                g2.fillRoundRect(1, 1, getWidth()-2, 4, 4, 4);
                
                g2.dispose();
            }
        };
        
        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(40, 60, 20, 60));
        
        JLabel titleLabel = new JLabel("🏛️ CHURCH INFORMATION");
        titleLabel.setFont(FONT_TITLE);
        titleLabel.setForeground(Color.WHITE);
        
        JLabel descLabel = new JLabel("Basic details about your church");
        descLabel.setFont(FONT_VALUE);
        descLabel.setForeground(Color.WHITE);
        
        headerPanel.add(titleLabel, BorderLayout.NORTH);
        headerPanel.add(Box.createVerticalStrut(8), BorderLayout.CENTER);
        headerPanel.add(descLabel, BorderLayout.SOUTH);
        
        // Form panel
        JPanel formPanel = new JPanel(new BorderLayout());
        formPanel.setOpaque(false);
        formPanel.setBorder(BorderFactory.createEmptyBorder(10, 60, 10, 60));
        
        // Create scrollable form area
        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getViewport().setBackground(C_SURFACE);
        
        // Customize scroll bar appearance
        JScrollBar verticalScrollBar = scrollPane.getVerticalScrollBar();
        verticalScrollBar.setUnitIncrement(16);
        verticalScrollBar.setBlockIncrement(32);
        verticalScrollBar.setPreferredSize(new Dimension(12, 0));
        
        // Grid panel for form fields
        JPanel gridPanel = new JPanel(new GridBagLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Dark background with rounded corners
                g2.setColor(C_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                
                // Border
                g2.setColor(C_BORDER);
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 8, 8);
                
                // Highlight for selected tab
                g2.setColor(new Color(C_GOLD.getRed(), C_GOLD.getGreen(), C_GOLD.getBlue(), 30));
                g2.fillRoundRect(1, 1, getWidth()-2, 4, 4, 4);
                
                g2.dispose();
            }
        };
        gridPanel.setOpaque(false);
        gridPanel.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // Basic church fields
        addFormField(gridPanel, gbc, "Church Name", churchNameField = createTextField("Enter church name"), 0, 0);
        addFormField(gridPanel, gbc, "Address Line 1", churchAddressField = createTextField("Enter street address"), 0, 1);
        addFormField(gridPanel, gbc, "City", churchCityField = createTextField("Enter city"), 0, 2);
        addFormField(gridPanel, gbc, "County", churchCountyField = createTextField("Enter county"), 0, 3);
        
        // Add some spacing at the bottom
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;
        gridPanel.add(Box.createVerticalGlue(), gbc);
        
        // Set minimum size to ensure scrolling
        gridPanel.setPreferredSize(new Dimension(700, 400));
        gridPanel.setMinimumSize(new Dimension(600, 300));
        
        scrollPane.getViewport().setView(gridPanel);
        formPanel.add(scrollPane, BorderLayout.CENTER);
        
        // Add navigation buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 10));
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(5, 60, 5, 60));
        
        JButton nextButton = new JButton("Next →");
        nextButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        nextButton.setForeground(Color.WHITE);
        nextButton.setBackground(C_GOLD);
        nextButton.setOpaque(true);
        nextButton.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));
        nextButton.setFocusPainted(false);
        nextButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        nextButton.addActionListener(e -> nextStep());
        
        buttonPanel.add(nextButton);
        
        // Create main container with proper layout
        JPanel mainContainer = new JPanel(new BorderLayout());
        mainContainer.setOpaque(false);
        mainContainer.add(headerPanel, BorderLayout.NORTH);
        mainContainer.add(formPanel, BorderLayout.CENTER);
        mainContainer.add(buttonPanel, BorderLayout.SOUTH);
        
        panel.add(mainContainer, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createChurchContactPanel() {
        JPanel panel = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Dark background with rounded corners
                g2.setColor(C_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                
                // Border
                g2.setColor(C_BORDER);
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 8, 8);
                
                // Highlight for selected tab
                g2.setColor(new Color(C_GOLD.getRed(), C_GOLD.getGreen(), C_GOLD.getBlue(), 30));
                g2.fillRoundRect(1, 1, getWidth()-2, 4, 4, 4);
                
                g2.dispose();
            }
        };
        
        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(40, 60, 20, 60));
        
        JLabel titleLabel = new JLabel("📞 CHURCH CONTACT");
        titleLabel.setFont(FONT_TITLE);
        titleLabel.setForeground(Color.WHITE);
        
        JLabel descLabel = new JLabel("Contact information for your church");
        descLabel.setFont(FONT_VALUE);
        descLabel.setForeground(Color.WHITE);
        
        headerPanel.add(titleLabel, BorderLayout.NORTH);
        headerPanel.add(Box.createVerticalStrut(8), BorderLayout.CENTER);
        headerPanel.add(descLabel, BorderLayout.SOUTH);
        
        // Form panel
        JPanel formPanel = new JPanel(new BorderLayout());
        formPanel.setOpaque(false);
        formPanel.setBorder(BorderFactory.createEmptyBorder(10, 60, 10, 60));
        
        // Create scrollable form area
        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getViewport().setBackground(C_SURFACE);
        
        // Customize scroll bar appearance
        JScrollBar verticalScrollBar = scrollPane.getVerticalScrollBar();
        verticalScrollBar.setUnitIncrement(16);
        verticalScrollBar.setBlockIncrement(32);
        verticalScrollBar.setPreferredSize(new Dimension(12, 0));
        
        // Grid panel for form fields
        JPanel gridPanel = new JPanel(new GridBagLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Dark background with rounded corners
                g2.setColor(C_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                
                // Border
                g2.setColor(C_BORDER);
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 8, 8);
                
                // Highlight for selected tab
                g2.setColor(new Color(C_GOLD.getRed(), C_GOLD.getGreen(), C_GOLD.getBlue(), 30));
                g2.fillRoundRect(1, 1, getWidth()-2, 4, 4, 4);
                
                g2.dispose();
            }
        };
        gridPanel.setOpaque(false);
        gridPanel.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // Church contact fields
        addFormField(gridPanel, gbc, "Church Phone", churchPhoneField = createTextField("+254 7XX XXX XXX"), 0, 0);
        addFormField(gridPanel, gbc, "Church Email", churchEmailField = createTextField("church@example.com"), 0, 1);
        addFormField(gridPanel, gbc, "Website", churchWebsiteField = createTextField("www.churchwebsite.com (optional)"), 0, 2);
        
        // Add some spacing at the bottom
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;
        gridPanel.add(Box.createVerticalGlue(), gbc);
        
        // Set minimum size to ensure scrolling
        gridPanel.setPreferredSize(new Dimension(700, 350));
        gridPanel.setMinimumSize(new Dimension(600, 250));
        
        scrollPane.getViewport().setView(gridPanel);
        formPanel.add(scrollPane, BorderLayout.CENTER);
        
        // Add navigation buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 10));
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(5, 60, 5, 60));
        
        JButton backButton = new JButton("← Back");
        backButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        backButton.setForeground(Color.WHITE);
        backButton.setBackground(new Color(108, 117, 125));
        backButton.setOpaque(true);
        backButton.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));
        backButton.setFocusPainted(false);
        backButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        backButton.addActionListener(e -> previousStep());
        
        JButton nextButton = new JButton("Next →");
        nextButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        nextButton.setForeground(Color.WHITE);
        nextButton.setBackground(C_GOLD);
        nextButton.setOpaque(true);
        nextButton.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));
        nextButton.setFocusPainted(false);
        nextButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        nextButton.addActionListener(e -> nextStep());
        
        buttonPanel.add(backButton);
        buttonPanel.add(nextButton);
        
        // Create main container with proper layout
        JPanel mainContainer = new JPanel(new BorderLayout());
        mainContainer.setOpaque(false);
        mainContainer.add(headerPanel, BorderLayout.NORTH);
        mainContainer.add(formPanel, BorderLayout.CENTER);
        mainContainer.add(buttonPanel, BorderLayout.SOUTH);
        
        panel.add(mainContainer, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createPastorPanel() {
        JPanel panel = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Dark background with rounded corners
                g2.setColor(C_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                
                // Border
                g2.setColor(C_BORDER);
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 8, 8);
                
                // Highlight for selected tab
                g2.setColor(new Color(C_GOLD.getRed(), C_GOLD.getGreen(), C_GOLD.getBlue(), 30));
                g2.fillRoundRect(1, 1, getWidth()-2, 4, 4, 4);
                
                g2.dispose();
            }
        };
        
        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(40, 60, 20, 60));
        
        JLabel titleLabel = new JLabel("👨‍⚕️ PASTOR INFORMATION");
        titleLabel.setFont(FONT_TITLE);
        titleLabel.setForeground(Color.WHITE);
        
        JLabel descLabel = new JLabel("Details about the senior pastor");
        descLabel.setFont(FONT_VALUE);
        descLabel.setForeground(Color.WHITE);
        
        headerPanel.add(titleLabel, BorderLayout.NORTH);
        headerPanel.add(Box.createVerticalStrut(8), BorderLayout.CENTER);
        headerPanel.add(descLabel, BorderLayout.SOUTH);
        
        // Form panel
        JPanel formPanel = new JPanel(new BorderLayout());
        formPanel.setOpaque(false);
        formPanel.setBorder(BorderFactory.createEmptyBorder(10, 60, 10, 60));
        
        // Create scrollable form area
        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getViewport().setBackground(C_SURFACE);
        
        // Customize scroll bar appearance
        JScrollBar verticalScrollBar = scrollPane.getVerticalScrollBar();
        verticalScrollBar.setUnitIncrement(16);
        verticalScrollBar.setBlockIncrement(32);
        verticalScrollBar.setPreferredSize(new Dimension(12, 0));
        
        // Grid panel for form fields
        JPanel gridPanel = new JPanel(new GridBagLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Dark background with rounded corners
                g2.setColor(C_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                
                // Border
                g2.setColor(C_BORDER);
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 8, 8);
                
                // Highlight for selected tab
                g2.setColor(new Color(C_GOLD.getRed(), C_GOLD.getGreen(), C_GOLD.getBlue(), 30));
                g2.fillRoundRect(1, 1, getWidth()-2, 4, 4, 4);
                
                g2.dispose();
            }
        };
        gridPanel.setOpaque(false);
        gridPanel.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // Pastor information fields
        addFormField(gridPanel, gbc, "Senior Pastor Name", churchPastorField = createTextField("Pastor's full name"), 0, 0);
        addFormField(gridPanel, gbc, "Pastor Phone", churchPastorPhoneField = createTextField("+254 7XX XXX XXX"), 0, 1);
        addFormField(gridPanel, gbc, "Pastor Email", churchPastorEmailField = createTextField("pastor@church.com"), 0, 2);
        
        // Add some spacing at the bottom
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;
        gridPanel.add(Box.createVerticalGlue(), gbc);
        
        // Set minimum size to ensure scrolling
        gridPanel.setPreferredSize(new Dimension(700, 350));
        gridPanel.setMinimumSize(new Dimension(600, 250));
        
        scrollPane.getViewport().setView(gridPanel);
        formPanel.add(scrollPane, BorderLayout.CENTER);
        
        // Add navigation buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 10));
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(5, 60, 5, 60));
        
        JButton backButton = new JButton("← Back");
        backButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        backButton.setForeground(Color.WHITE);
        backButton.setBackground(new Color(108, 117, 125));
        backButton.setOpaque(true);
        backButton.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));
        backButton.setFocusPainted(false);
        backButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        backButton.addActionListener(e -> previousStep());
        
        JButton nextButton = new JButton("Next →");
        nextButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        nextButton.setForeground(Color.WHITE);
        nextButton.setBackground(C_GOLD);
        nextButton.setOpaque(true);
        nextButton.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));
        nextButton.setFocusPainted(false);
        nextButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        nextButton.addActionListener(e -> nextStep());
        
        buttonPanel.add(backButton);
        buttonPanel.add(nextButton);
        
        // Create main container with proper layout
        JPanel mainContainer = new JPanel(new BorderLayout());
        mainContainer.setOpaque(false);
        mainContainer.add(headerPanel, BorderLayout.NORTH);
        mainContainer.add(formPanel, BorderLayout.CENTER);
        mainContainer.add(buttonPanel, BorderLayout.SOUTH);
        
        panel.add(mainContainer, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createChurchStatsPanel() {
        JPanel panel = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Dark background with rounded corners
                g2.setColor(C_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                
                // Border
                g2.setColor(C_BORDER);
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 8, 8);
                
                // Highlight for selected tab
                g2.setColor(new Color(C_GOLD.getRed(), C_GOLD.getGreen(), C_GOLD.getBlue(), 30));
                g2.fillRoundRect(1, 1, getWidth()-2, 4, 4, 4);
                
                g2.dispose();
            }
        };
        
        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(40, 60, 20, 60));
        
        JLabel titleLabel = new JLabel("📊 CHURCH STATISTICS");
        titleLabel.setFont(FONT_TITLE);
        titleLabel.setForeground(Color.WHITE);
        
        JLabel descLabel = new JLabel("Church size and type information");
        descLabel.setFont(FONT_VALUE);
        descLabel.setForeground(Color.WHITE);
        
        headerPanel.add(titleLabel, BorderLayout.NORTH);
        headerPanel.add(Box.createVerticalStrut(8), BorderLayout.CENTER);
        headerPanel.add(descLabel, BorderLayout.SOUTH);
        
        // Form panel
        JPanel formPanel = new JPanel(new BorderLayout());
        formPanel.setOpaque(false);
        formPanel.setBorder(BorderFactory.createEmptyBorder(10, 60, 10, 60));
        
        // Create scrollable form area
        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getViewport().setBackground(C_SURFACE);
        
        // Customize scroll bar appearance
        JScrollBar verticalScrollBar = scrollPane.getVerticalScrollBar();
        verticalScrollBar.setUnitIncrement(16);
        verticalScrollBar.setBlockIncrement(32);
        verticalScrollBar.setPreferredSize(new Dimension(12, 0));
        
        // Grid panel for form fields
        JPanel gridPanel = new JPanel(new GridBagLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Dark background with rounded corners
                g2.setColor(C_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                
                // Border
                g2.setColor(C_BORDER);
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 8, 8);
                
                // Highlight for selected tab
                g2.setColor(new Color(C_GOLD.getRed(), C_GOLD.getGreen(), C_GOLD.getBlue(), 30));
                g2.fillRoundRect(1, 1, getWidth()-2, 4, 4, 4);
                
                g2.dispose();
            }
        };
        gridPanel.setOpaque(false);
        gridPanel.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // Church statistics fields
        addFormField(gridPanel, gbc, "Church Type", churchTypeField = createChurchTypeDropdown(), 0, 0);
        addFormField(gridPanel, gbc, "Membership Count", churchMembershipField = createTextField("Number of members"), 0, 1);
        addFormField(gridPanel, gbc, "Average Attendance", churchAttendanceField = createTextField("Average weekly attendance"), 0, 2);
        
        // Add some spacing at the bottom
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;
        gridPanel.add(Box.createVerticalGlue(), gbc);
        
        // Set minimum size to ensure scrolling
        gridPanel.setPreferredSize(new Dimension(700, 300));
        gridPanel.setMinimumSize(new Dimension(600, 200));
        
        scrollPane.getViewport().setView(gridPanel);
        formPanel.add(scrollPane, BorderLayout.CENTER);
        
        // Add navigation buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 10));
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(5, 60, 5, 60));
        
        JButton backButton = new JButton("← Back");
        backButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        backButton.setForeground(Color.WHITE);
        backButton.setBackground(new Color(108, 117, 125));
        backButton.setOpaque(true);
        backButton.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));
        backButton.setFocusPainted(false);
        backButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        backButton.addActionListener(e -> previousStep());
        
        JButton nextButton = new JButton("Next →");
        nextButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        nextButton.setForeground(Color.WHITE);
        nextButton.setBackground(C_GOLD);
        nextButton.setOpaque(true);
        nextButton.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));
        nextButton.setFocusPainted(false);
        nextButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        nextButton.addActionListener(e -> nextStep());
        
        buttonPanel.add(backButton);
        buttonPanel.add(nextButton);
        
        // Create main container with proper layout
        JPanel mainContainer = new JPanel(new BorderLayout());
        mainContainer.setOpaque(false);
        mainContainer.add(headerPanel, BorderLayout.NORTH);
        mainContainer.add(formPanel, BorderLayout.CENTER);
        mainContainer.add(buttonPanel, BorderLayout.SOUTH);
        
        panel.add(mainContainer, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createAdminDetailsPanel() {
        JPanel panel = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Dark gradient background
                GradientPaint gradient = new GradientPaint(
                    0, 0, C_SURFACE,
                    0, getHeight(), new Color(30, 30, 40)
                );
                g2.setPaint(gradient);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                
                // Border
                g2.setColor(C_BORDER);
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 8, 8);
                
                // Highlight for selected tab
                g2.setColor(new Color(C_TEXT_MID.getRed(), C_TEXT_MID.getGreen(), C_TEXT_MID.getBlue(), 30));
                g2.fillRoundRect(1, 1, getWidth()-2, 4, 4, 4);
                
                g2.dispose();
            }
        };
        
        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(40, 60, 20, 60));
        
        JLabel titleLabel = new JLabel("👤 ADMIN ACCOUNT");
        titleLabel.setFont(FONT_TITLE);
        titleLabel.setForeground(Color.WHITE);
        
        JLabel descLabel = new JLabel("Create administrator account");
        descLabel.setFont(FONT_VALUE);
        descLabel.setForeground(Color.WHITE);
        
        headerPanel.add(titleLabel, BorderLayout.NORTH);
        headerPanel.add(Box.createVerticalStrut(8), BorderLayout.CENTER);
        headerPanel.add(descLabel, BorderLayout.SOUTH);
        
        // Form panel
        JPanel formPanel = new JPanel(new BorderLayout());
        formPanel.setOpaque(false);
        formPanel.setBorder(BorderFactory.createEmptyBorder(10, 60, 10, 60));
        
        // Create scrollable form area
        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getViewport().setBackground(C_SURFACE);
        
        // Customize scroll bar appearance
        JScrollBar verticalScrollBar = scrollPane.getVerticalScrollBar();
        verticalScrollBar.setUnitIncrement(16);
        verticalScrollBar.setBlockIncrement(32);
        verticalScrollBar.setPreferredSize(new Dimension(12, 0));
        
        // Grid panel for form fields
        JPanel gridPanel = new JPanel(new GridBagLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Dark background with rounded corners
                g2.setColor(C_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                
                // Border
                g2.setColor(C_BORDER);
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 8, 8);
                
                // Highlight for selected tab
                g2.setColor(new Color(C_TEXT_MID.getRed(), C_TEXT_MID.getGreen(), C_TEXT_MID.getBlue(), 30));
                g2.fillRoundRect(1, 1, getWidth()-2, 4, 4, 4);
                
                g2.dispose();
            }
        };
        gridPanel.setOpaque(false);
        gridPanel.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // Admin fields - single column
        addFormField(gridPanel, gbc, "Username", adminUsernameField = createTextField("Choose username"), 0, 0);
        addFormField(gridPanel, gbc, "First Name", adminFirstNameField = createTextField("Enter your first name"), 0, 1);
        addFormField(gridPanel, gbc, "Last Name", adminLastNameField = createTextField("Enter your last name"), 0, 2);
        addFormField(gridPanel, gbc, "Email Address", adminEmailField = createTextField("your.email@example.com"), 0, 3);
        addFormField(gridPanel, gbc, "Phone Number", adminPhoneField = createTextField("+254 7XX XXX XXX"), 0, 4);
        addFormField(gridPanel, gbc, "Title/Position", adminTitleField = createTextField("e.g., Senior Pastor"), 0, 5);
        addFormField(gridPanel, gbc, "Password", adminPasswordField = createPasswordField("Enter secure password"), 0, 6);
        addFormField(gridPanel, gbc, "Confirm Password", adminConfirmPasswordField = createPasswordField("Re-enter password"), 0, 7);
        
        // Password requirements
        JPanel requirementsPanel = createPasswordRequirementsPanel();
        gbc.gridx = 0;
        gbc.gridy = 8;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 20, 0);
        gridPanel.add(requirementsPanel, gbc);
        
        // Add some spacing at the bottom
        gbc.gridx = 0;
        gbc.gridy = 9;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;
        gbc.insets = new Insets(0, 0, 0, 0);
        gridPanel.add(Box.createVerticalGlue(), gbc);
        
        // Set minimum size to ensure scrolling
        gridPanel.setPreferredSize(new Dimension(700, 700));
        gridPanel.setMinimumSize(new Dimension(600, 600));
        
        scrollPane.getViewport().setView(gridPanel);
        formPanel.add(scrollPane, BorderLayout.CENTER);
        
        // Add navigation buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 10));
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(5, 60, 5, 60));
        
        JButton backButton = new JButton("← Back");
        backButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        backButton.setForeground(Color.WHITE);
        backButton.setBackground(new Color(108, 117, 125));
        backButton.setOpaque(true);
        backButton.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));
        backButton.setFocusPainted(false);
        backButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        backButton.addActionListener(e -> previousStep());
        
        JButton nextButton = new JButton("Next →");
        nextButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        nextButton.setForeground(Color.WHITE);
        nextButton.setBackground(C_TEXT_MID);
        nextButton.setOpaque(true);
        nextButton.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));
        nextButton.setFocusPainted(false);
        nextButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        nextButton.addActionListener(e -> {
            updateReviewContent();
            nextStep();
        });
        
        buttonPanel.add(backButton);
        buttonPanel.add(nextButton);
        
        // Create main container with proper layout
        JPanel mainContainer = new JPanel(new BorderLayout());
        mainContainer.setOpaque(false);
        mainContainer.add(headerPanel, BorderLayout.NORTH);
        mainContainer.add(formPanel, BorderLayout.CENTER);
        mainContainer.add(buttonPanel, BorderLayout.SOUTH);
        
        panel.add(mainContainer, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createAdminPanel() {
        JPanel panel = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Dark gradient background
                GradientPaint gradient = new GradientPaint(
                    0, 0, C_SURFACE,
                    getWidth(), getHeight(), new Color(30, 35, 45)
                );
                g2.setPaint(gradient);
                g2.fillRect(0, 0, getWidth(), getHeight());
                
                // Decorative elements
                g2.setColor(new Color(C_TEXT_MID.getRed(), C_TEXT_MID.getGreen(), C_TEXT_MID.getBlue(), 20));
                g2.fillOval(getWidth() - 150, 50, 100, 100);
                g2.fillOval(getWidth() - 100, 200, 60, 60);
                
                g2.dispose();
            }
        };
        panel.setOpaque(false);
        
        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(40, 60, 20, 60));
        
        JLabel titleLabel = new JLabel("Administrator Account");
        titleLabel.setFont(FONT_TITLE);
        titleLabel.setForeground(Color.WHITE);
        
        JLabel descLabel = new JLabel("Create your administrator account. You'll have full access to manage your church.");
        descLabel.setFont(FONT_VALUE);
        descLabel.setForeground(Color.WHITE);
        
        headerPanel.add(titleLabel, BorderLayout.NORTH);
        headerPanel.add(Box.createVerticalStrut(8), BorderLayout.CENTER);
        headerPanel.add(descLabel, BorderLayout.SOUTH);
        
        // Form content
        JPanel formPanel = new JPanel();
        formPanel.setOpaque(false);
        formPanel.setBorder(BorderFactory.createEmptyBorder(10, 60, 10, 60));
        
        // Create scrollable form area
        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getViewport().setBackground(C_SURFACE);
        
        // Customize scroll bar appearance
        JScrollBar verticalScrollBar = scrollPane.getVerticalScrollBar();
        verticalScrollBar.setUnitIncrement(16);
        verticalScrollBar.setBlockIncrement(32);
        verticalScrollBar.setPreferredSize(new Dimension(12, 0));
        verticalScrollBar.setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            @Override
            protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(C_GOLD);
                g2.fillRoundRect(thumbBounds.x, thumbBounds.y, thumbBounds.width, thumbBounds.height, 6, 6);
                g2.dispose();
            }
            
            @Override
            protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(C_BORDER);
                g2.fillRoundRect(trackBounds.x, trackBounds.y, trackBounds.width, trackBounds.height, 6, 6);
                g2.dispose();
            }
        });
        
        JPanel gridPanel = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
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
        gridPanel.setOpaque(false);
        gridPanel.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gridPanel.setPreferredSize(new Dimension(700, 700));
        gridPanel.setMinimumSize(new Dimension(600, 600));
        
        scrollPane.getViewport().setView(gridPanel);
        formPanel.add(scrollPane, BorderLayout.CENTER);
        
        // Add navigation buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 10));
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(5, 60, 5, 60));
        
        JButton backButton = new JButton("← Back");
        backButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        backButton.setForeground(Color.WHITE);
        backButton.setBackground(new Color(108, 117, 125));
        backButton.setOpaque(true);
        backButton.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));
        backButton.setFocusPainted(false);
        backButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        backButton.addActionListener(e -> previousStep());
        
        JButton nextButton = new JButton("Next →");
        nextButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        nextButton.setForeground(Color.WHITE);
        nextButton.setBackground(C_TEXT_MID);
        nextButton.setOpaque(true);
        nextButton.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));
        nextButton.setFocusPainted(false);
        nextButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        nextButton.addActionListener(e -> {
            updateReviewContent();
            nextStep();
        });
        
        buttonPanel.add(backButton);
        buttonPanel.add(nextButton);
        
        // Create main container with proper layout
        JPanel mainContainer = new JPanel(new BorderLayout());
        mainContainer.setOpaque(false);
        mainContainer.add(headerPanel, BorderLayout.NORTH);
        mainContainer.add(formPanel, BorderLayout.CENTER);
        mainContainer.add(buttonPanel, BorderLayout.SOUTH);
        
        panel.add(mainContainer, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createReviewPanel() {
        JPanel panel = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Dark gradient background
                GradientPaint gradient = new GradientPaint(
                    0, 0, C_SURFACE,
                    getWidth(), getHeight(), new Color(30, 35, 45)
                );
                g2.setPaint(gradient);
                g2.fillRect(0, 0, getWidth(), getHeight());
                
                // Decorative elements
                g2.setColor(new Color(C_GOLD.getRed(), C_GOLD.getGreen(), C_GOLD.getBlue(), 20));
                g2.fillOval(getWidth() - 150, 50, 100, 100);
                g2.fillOval(getWidth() - 100, 200, 60, 60);
                
                g2.dispose();
            }
        };
        panel.setOpaque(false);
        
        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(40, 60, 20, 60));
        
        JLabel titleLabel = new JLabel("Review & Submit");
        titleLabel.setFont(FONT_TITLE);
        titleLabel.setForeground(Color.WHITE);
        
        JLabel descLabel = new JLabel("Review your information before submitting the registration.");
        descLabel.setFont(FONT_VALUE);
        descLabel.setForeground(Color.WHITE);
        
        headerPanel.add(titleLabel, BorderLayout.NORTH);
        headerPanel.add(Box.createVerticalStrut(8), BorderLayout.CENTER);
        headerPanel.add(descLabel, BorderLayout.SOUTH);
        
        // Review content with scrollable area
        JPanel reviewContentPanel = createReviewContentPanel();
        
        panel.add(headerPanel, BorderLayout.NORTH);
        
        // Create a split pane with fixed button area at bottom
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setResizeWeight(1.0); // Give most space to content
        splitPane.setDividerSize(0); // Hide divider
        
        JScrollPane scrollPane = new JScrollPane(reviewContentPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setPreferredSize(new Dimension(600, 400));
        
        // Fixed button panel at bottom
        JPanel fixedButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 30, 25)) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Dark background for button area
                g2.setColor(C_CARD);
                g2.fillRect(0, 0, getWidth(), getHeight());
                
                // Top border
                g2.setColor(C_BORDER);
                g2.fillRect(0, 0, getWidth(), 1);
                
                g2.dispose();
            }
        };
        fixedButtonPanel.setOpaque(false);
        fixedButtonPanel.setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 40));
        fixedButtonPanel.add(registerButton);
        fixedButtonPanel.add(cancelButton);
        fixedButtonPanel.setPreferredSize(new Dimension(0, 80));
        fixedButtonPanel.setMinimumSize(new Dimension(0, 80));
        fixedButtonPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        
        splitPane.setTopComponent(scrollPane);
        splitPane.setBottomComponent(fixedButtonPanel);
        
        panel.add(splitPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createReviewContentPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(C_SURFACE);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Church review
        JPanel churchReviewPanel = new JPanel(new BorderLayout(0, 10)) {
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
        churchReviewPanel.setOpaque(false);
        churchReviewPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        JLabel churchReviewTitle = new JLabel("🏛️ Church Information");
        churchReviewTitle.setFont(FONT_LABEL);
        churchReviewTitle.setForeground(C_GOLD);
        
        JPanel churchDetailsPanel = new JPanel();
        churchDetailsPanel.setBackground(C_CARD);
        churchDetailsPanel.setLayout(new BoxLayout(churchDetailsPanel, BoxLayout.Y_AXIS));
        
        // Admin review
        JPanel adminReviewPanel = new JPanel(new BorderLayout(0, 10)) {
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
                g2.setColor(new Color(C_TEXT_MID.getRed(), C_TEXT_MID.getGreen(), C_TEXT_MID.getBlue(), 30));
                g2.fillRoundRect(1, 1, getWidth()-2, 4, 4, 4);
                
                g2.dispose();
            }
        };
        adminReviewPanel.setOpaque(false);
        adminReviewPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        JLabel adminReviewTitle = new JLabel("👤 Administrator Account");
        adminReviewTitle.setFont(FONT_LABEL);
        adminReviewTitle.setForeground(C_TEXT_MID);
        
        JPanel adminDetailsPanel = new JPanel();
        adminDetailsPanel.setBackground(C_CARD);
        adminDetailsPanel.setLayout(new BoxLayout(adminDetailsPanel, BoxLayout.Y_AXIS));
        
        churchReviewPanel.add(churchReviewTitle, BorderLayout.NORTH);
        churchReviewPanel.add(churchDetailsPanel, BorderLayout.CENTER);
        
        adminReviewPanel.add(adminReviewTitle, BorderLayout.NORTH);
        adminReviewPanel.add(adminDetailsPanel, BorderLayout.CENTER);
        
        panel.add(churchReviewPanel);
        panel.add(Box.createVerticalStrut(20));
        panel.add(adminReviewPanel);
        panel.add(Box.createVerticalStrut(30));
        
        return panel;
    }
    
    private JPanel createPasswordRequirementsPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(C_SURFACE);
        panel.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        panel.setLayout(new BorderLayout());
        
        JLabel requirementsLabel = new JLabel("<html><div style='font-size: 13px; color: #ffffff; line-height: 1.5;'>" +
            "Password must contain at least 8 characters including uppercase, lowercase, number and special character" + "</div></html>");
        requirementsLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        
        panel.add(requirementsLabel, BorderLayout.CENTER);
        return panel;
    }
    
    private JTextField createTextField(String placeholder) {
        JTextField field = new JTextField() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (!hasFocus() && getText().isEmpty()) {
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
        field.setMinimumSize(new Dimension(300, 50));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        field.setPreferredSize(new Dimension(300, 50));
        
        field.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                if (field.getText().isEmpty()) {
                    field.setForeground(C_TEXT);
                }
                field.repaint();
            }
            public void focusLost(FocusEvent e) {
                if (field.getText().isEmpty()) {
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
        field.setMinimumSize(new Dimension(300, 50));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        field.setPreferredSize(new Dimension(300, 50));
        
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
    
    private JComboBox<String> createChurchTypeDropdown() {
        String[] churchTypes = {
            "Main Church",
            "Branch Church", 
            "Church Plant",
            "Chaplaincy",
            "Mission Station",
            "Non-Denominational"
        };
        
        JComboBox<String> dropdown = new JComboBox<>(churchTypes);
        dropdown.setFont(FONT_VALUE);
        dropdown.setForeground(C_TEXT);
        dropdown.setBackground(C_CARD);
        dropdown.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(C_BORDER),
            BorderFactory.createEmptyBorder(12, 15, 12, 15)
        ));
        dropdown.setMinimumSize(new Dimension(300, 40));
        dropdown.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        dropdown.setSelectedIndex(0); // Default to "Main Church"
        
        return dropdown;
    }
    
    private void addFormField(JPanel parent, GridBagConstraints gbc, String labelText, JComponent field, int gridx, int gridy) {
        JLabel label = new JLabel(labelText);
        label.setFont(new Font("Segoe UI", Font.BOLD, 14));
        label.setForeground(Color.WHITE);
        
        gbc.gridx = gridx;
        gbc.gridy = gridy;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = new Insets(0, 0, 8, 20);
        gbc.weightx = 0.0;
        parent.add(label, gbc);
        
        gbc.gridx = gridx + 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 16, 0);
        parent.add(field, gbc);
    }
    
    private void updateReviewContent() {
        // Update church review panel
        JPanel churchDetailsPanel = new JPanel();
        churchDetailsPanel.setBackground(C_CARD);
        churchDetailsPanel.setLayout(new BoxLayout(churchDetailsPanel, BoxLayout.Y_AXIS));
        
        churchDetailsPanel.add(createReviewLabel("Church Name:", churchNameField.getText()));
        churchDetailsPanel.add(createReviewLabel("Address Line 1:", churchAddressField.getText()));
        churchDetailsPanel.add(createReviewLabel("City:", churchCityField.getText()));
        churchDetailsPanel.add(createReviewLabel("County:", churchCountyField.getText()));
        churchDetailsPanel.add(createReviewLabel("Phone:", churchPhoneField.getText()));
        churchDetailsPanel.add(createReviewLabel("Email:", churchEmailField.getText()));
        churchDetailsPanel.add(createReviewLabel("Website:", churchWebsiteField.getText()));
        churchDetailsPanel.add(createReviewLabel("Senior Pastor:", churchPastorField.getText()));
        churchDetailsPanel.add(createReviewLabel("Pastor Phone:", churchPastorPhoneField.getText()));
        churchDetailsPanel.add(createReviewLabel("Pastor Email:", churchPastorEmailField.getText()));
        churchDetailsPanel.add(createReviewLabel("Church Type:", (String) churchTypeField.getSelectedItem()));
        churchDetailsPanel.add(createReviewLabel("Membership Count:", churchMembershipField.getText()));
        churchDetailsPanel.add(createReviewLabel("Average Attendance:", churchAttendanceField.getText()));
        churchDetailsPanel.add(Box.createVerticalStrut(10));
        
        // Update admin review panel
        JPanel adminDetailsPanel = new JPanel();
        adminDetailsPanel.setBackground(C_CARD);
        adminDetailsPanel.setLayout(new BoxLayout(adminDetailsPanel, BoxLayout.Y_AXIS));
        
        adminDetailsPanel.add(createReviewLabel("Username:", adminUsernameField.getText()));
        adminDetailsPanel.add(createReviewLabel("First Name:", adminFirstNameField.getText()));
        adminDetailsPanel.add(createReviewLabel("Last Name:", adminLastNameField.getText()));
        adminDetailsPanel.add(createReviewLabel("Email:", adminEmailField.getText()));
        adminDetailsPanel.add(createReviewLabel("Phone:", adminPhoneField.getText()));
        adminDetailsPanel.add(createReviewLabel("Title:", adminTitleField.getText()));
        adminDetailsPanel.add(createReviewLabel("Password:", "••••••••"));
        adminDetailsPanel.add(Box.createVerticalStrut(10));
        
        // Update the review panel content
        reviewPanel.removeAll();
        
        // Main container with BorderLayout to ensure buttons stay visible
        JPanel mainContainer = new JPanel(new BorderLayout());
        mainContainer.setBackground(C_SURFACE);
        
        // Scrollable content area
        JPanel scrollablePanel = new JPanel();
        scrollablePanel.setBackground(C_SURFACE);
        scrollablePanel.setLayout(new BoxLayout(scrollablePanel, BoxLayout.Y_AXIS));
        scrollablePanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Church review
        RoundedPanel churchReviewPanel = new RoundedPanel(10, C_CARD);
        churchReviewPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        churchReviewPanel.setLayout(new BorderLayout(0, 10));
        
        JLabel churchReviewTitle = new JLabel("🏛️ Church Information");
        churchReviewTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        churchReviewTitle.setForeground(Color.WHITE);
        
        churchReviewPanel.add(churchReviewTitle, BorderLayout.NORTH);
        churchReviewPanel.add(churchDetailsPanel, BorderLayout.CENTER);
        
        // Admin review
        RoundedPanel adminReviewPanel = new RoundedPanel(10, C_CARD);
        adminReviewPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        adminReviewPanel.setLayout(new BorderLayout(0, 10));
        
        JLabel adminReviewTitle = new JLabel("👤 Administrator Account");
        adminReviewTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        adminReviewTitle.setForeground(Color.WHITE);
        
        adminReviewPanel.add(adminReviewTitle, BorderLayout.NORTH);
        adminReviewPanel.add(adminDetailsPanel, BorderLayout.CENTER);
        
        // Add content to scrollable panel
        scrollablePanel.add(churchReviewPanel);
        scrollablePanel.add(Box.createVerticalStrut(20));
        scrollablePanel.add(adminReviewPanel);
        scrollablePanel.add(Box.createVerticalStrut(20)); // Reduced strut to make room for buttons
        
        // Create scroll pane for the content
        JScrollPane scrollPane = new JScrollPane(scrollablePanel);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        
        // Customize scroll bar appearance
        JScrollBar verticalScrollBar = scrollPane.getVerticalScrollBar();
        verticalScrollBar.setUnitIncrement(16);
        verticalScrollBar.setBlockIncrement(32);
        verticalScrollBar.setPreferredSize(new Dimension(12, 0));
        
        // Fixed button panel at bottom
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        buttonPanel.setPreferredSize(new Dimension(0, 70));
        buttonPanel.setMinimumSize(new Dimension(0, 70));
        
        JButton backButton = new JButton("← Back");
        backButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        backButton.setForeground(Color.WHITE);
        backButton.setBackground(new Color(108, 117, 125));
        backButton.setOpaque(true);
        backButton.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));
        backButton.setFocusPainted(false);
        backButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        backButton.addActionListener(e -> previousStep());
        
        buttonPanel.add(backButton);
        buttonPanel.add(cancelButton);
        buttonPanel.add(registerButton);
        
        // Add components to main container
        mainContainer.add(scrollPane, BorderLayout.CENTER);
        mainContainer.add(buttonPanel, BorderLayout.SOUTH);
        
        reviewPanel.add(mainContainer, BorderLayout.CENTER);
        reviewPanel.revalidate();
        reviewPanel.repaint();
    }
    
    private JLabel createReviewLabel(String label, String value) {
        JLabel labelComponent = new JLabel("<html><div style='font-size: 14px; color: #ffffff; margin: 2px 0;'><strong>" + label + "</strong> " + (value != null && !value.isEmpty() ? value : "Not provided") + "</div></html>");
        return labelComponent;
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
        
        // Window controls
        JPanel windowControls = createWindowControls();
        
        // Sidebar navigation
        JPanel sidebarPanel = createSidebarPanel();
        
        // Add components
        add(windowControls, BorderLayout.NORTH);
        add(mainContainer, BorderLayout.CENTER);
        
        // Add sidebar and content to main container
        mainContainer.add(sidebarPanel, BorderLayout.WEST);
        mainContainer.add(contentArea, BorderLayout.CENTER);
    }
    
    private JPanel createSidebarPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 15));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 15, 20, 15));
        panel.setPreferredSize(new Dimension(220, 0));
        
        // Logo at top
        JLabel logoPanel = LogoLoader.createLogoLabel(new Dimension(100, 100));
        logoPanel.setPreferredSize(new Dimension(0, 100));
        
        // Step buttons panel
        JPanel stepsPanel = new JPanel();
        stepsPanel.setLayout(new BoxLayout(stepsPanel, BoxLayout.Y_AXIS));
        stepsPanel.setOpaque(false);
        
        stepButtons = new JButton[6];
        for (int i = 0; i < 6; i++) {
            final int step = i;
            JButton stepBtn = createStepButton(STEP_TITLES[i], STEP_DESCRIPTIONS[i], i == 0);
            stepBtn.addActionListener(e -> navigateToStep(step));
            stepButtons[i] = stepBtn;
            stepsPanel.add(stepBtn);
            stepsPanel.add(Box.createVerticalStrut(8));
        }
        
        // Progress indicator at bottom
        JPanel progressPanel = new JPanel(new BorderLayout());
        progressPanel.setOpaque(false);
        JLabel progressLabel = new JLabel("Step 1 of 6", SwingConstants.CENTER);
        progressLabel.setFont(FONT_SMALL);
        progressLabel.setForeground(C_TEXT_DIM);
        progressLabel.setName("progressLabel");
        progressPanel.add(progressLabel, BorderLayout.CENTER);
        
        panel.add(logoPanel, BorderLayout.NORTH);
        panel.add(stepsPanel, BorderLayout.CENTER);
        panel.add(progressPanel, BorderLayout.SOUTH);
        
        return panel;
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
        JLabel title = new JLabel("  Sanctum Registration");
        title.setFont(FONT_SMALL);
        title.setForeground(C_TEXT_MID);
        
        // Right side - Controls
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        controls.setOpaque(false);
        
        JButton minimizeBtn = new JButton("—") {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Clear background
                g2.setColor(getBackground());
                g2.fillRect(0, 0, getWidth(), getHeight());
                
                // Background on hover
                if (getModel().isRollover()) {
                    g2.setColor(new Color(255, 255, 255, 50));
                    g2.fillRoundRect(2, 2, getWidth()-4, getHeight()-4, 4, 4);
                }
                
                // Draw minimize icon (horizontal line)
                g2.setColor(C_TEXT);
                g2.setStroke(new BasicStroke(2f));
                int lineY = getHeight() / 2;
                g2.drawLine(getWidth()/2 - 6, lineY, getWidth()/2 + 6, lineY);
                
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
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Clear background
                g2.setColor(getBackground());
                g2.fillRect(0, 0, getWidth(), getHeight());
                
                // Background on hover
                if (getModel().isRollover()) {
                    g2.setColor(new Color(255, 100, 100, 100));
                    g2.fillRoundRect(2, 2, getWidth()-4, getHeight()-4, 4, 4);
                }
                
                // Draw X
                g2.setColor(C_TEXT);
                g2.setStroke(new BasicStroke(2f));
                int cx = getWidth() / 2;
                int cy = getHeight() / 2;
                int offset = 5;
                g2.drawLine(cx - offset, cy - offset, cx + offset, cy + offset);
                g2.drawLine(cx + offset, cy - offset, cx - offset, cy + offset);
                
                g2.dispose();
            }
        };
        closeBtn.setContentAreaFilled(false);
        closeBtn.setBorderPainted(false);
        closeBtn.setFocusPainted(false);
        closeBtn.setPreferredSize(new Dimension(40, 30));
        closeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeBtn.addActionListener(e -> {
            this.dispose();
            new com.sanctum.view.LoginFrame().setVisible(true);
        });
        
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
        // Window dragging
        addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent evt) {
                mouseX = evt.getX();
                mouseY = evt.getY();
            }
        });
        
        addMouseMotionListener(new MouseAdapter() {
            public void mouseDragged(MouseEvent evt) {
                int x = evt.getXOnScreen() - mouseX;
                int y = evt.getYOnScreen() - mouseY;
                setLocation(x, y);
            }
        });
        
        // Register button action
        registerButton.addActionListener(e -> attemptRegistration());
        
        // Cancel button action
        cancelButton.addActionListener(e -> {
            this.dispose();
            new com.sanctum.view.LoginFrame().setVisible(true);
        });
        
        // Enter key on password fields
        KeyListener enterKeyListener = new KeyListener() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    attemptRegistration();
                }
            }
            
            @Override
            public void keyReleased(KeyEvent e) {}
            
            @Override
            public void keyTyped(KeyEvent e) {}
        };
        
        adminConfirmPasswordField.addKeyListener(enterKeyListener);
        
        // Clear status on typing
        clearStatusOnTyping();
    }
    
    private void clearStatusOnTyping() {
        // Add listeners to clear status when user starts typing
        java.awt.event.KeyListener clearListener = new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent e) {
                statusLabel.setText("");
            }
        };
        
        churchNameField.addKeyListener(clearListener);
        churchAddressField.addKeyListener(clearListener);
        churchCityField.addKeyListener(clearListener);
        churchCountyField.addKeyListener(clearListener);
        churchPhoneField.addKeyListener(clearListener);
        churchEmailField.addKeyListener(clearListener);
        adminUsernameField.addKeyListener(clearListener);
        adminFirstNameField.addKeyListener(clearListener);
        adminLastNameField.addKeyListener(clearListener);
        adminEmailField.addKeyListener(clearListener);
        adminPasswordField.addKeyListener(clearListener);
        adminConfirmPasswordField.addKeyListener(clearListener);
    }
    
    private void attemptRegistration() {
        // Collect church data
        String churchName = churchNameField.getText().trim();
        String churchAddress = churchAddressField.getText().trim();
        String churchCity = churchCityField.getText().trim();
        String churchCounty = churchCountyField.getText().trim();
        String churchPhone = churchPhoneField.getText().trim();
        String churchEmail = churchEmailField.getText().trim();
        String churchWebsite = churchWebsiteField.getText().trim();
        String churchPastor = churchPastorField.getText().trim();
        String churchPastorPhone = churchPastorPhoneField.getText().trim();
        String churchPastorEmail = churchPastorEmailField.getText().trim();
        // Map display values to backend values
        final String churchTypeDisplay = (String) churchTypeField.getSelectedItem();
        final String churchTypeValue;
        if (churchTypeDisplay.equals("Main Church")) {
            churchTypeValue = "main";
        } else if (churchTypeDisplay.equals("Branch Church")) {
            churchTypeValue = "branch";
        } else if (churchTypeDisplay.equals("Church Plant")) {
            churchTypeValue = "plant";
        } else if (churchTypeDisplay.equals("Chaplaincy")) {
            churchTypeValue = "chaplaincy";
        } else if (churchTypeDisplay.equals("Mission Station")) {
            churchTypeValue = "mission";
        } else if (churchTypeDisplay.equals("Non-Denominational")) {
            churchTypeValue = "main"; // Map non-denominational to main
        } else {
            churchTypeValue = "main"; // default fallback
        }
        String churchMembership = churchMembershipField.getText().trim();
        String churchAttendance = churchAttendanceField.getText().trim();
        
        // Collect admin data
        String adminUsername = adminUsernameField.getText().trim();
        String adminFirstName = adminFirstNameField.getText().trim();
        String adminLastName = adminLastNameField.getText().trim();
        String adminEmail = adminEmailField.getText().trim();
        String adminPhone = adminPhoneField.getText().trim();
        String adminTitle = adminTitleField.getText().trim();
        String adminPassword = new String(adminPasswordField.getPassword());
        String adminConfirmPassword = new String(adminConfirmPasswordField.getPassword());
        
        // Validation - Required Church Fields
        if (churchName.isEmpty()) {
            showStatus("Please enter church name", true);
            navigateToStep(0);
            return;
        }
        
        if (churchAddress.isEmpty()) {
            showStatus("Please enter church address", true);
            navigateToStep(0);
            return;
        }
        
        if (churchCity.isEmpty()) {
            showStatus("Please enter church city", true);
            navigateToStep(0);
            return;
        }
        
        if (churchCounty.isEmpty()) {
            showStatus("Please enter church county", true);
            navigateToStep(0);
            return;
        }
        
        if (churchPhone.isEmpty()) {
            showStatus("Please enter church phone", true);
            navigateToStep(0);
            return;
        }
        
        if (churchEmail.isEmpty()) {
            showStatus("Please enter church email", true);
            navigateToStep(0);
            return;
        }
        
        if (churchPastor.isEmpty()) {
            showStatus("Please enter senior pastor name", true);
            navigateToStep(0);
            return;
        }
        
        // Validation - Required Admin Fields
        if (adminUsername.isEmpty()) {
            showStatus("Please enter username", true);
            navigateToStep(4);
            return;
        }
        
        if (adminFirstName.isEmpty()) {
            showStatus("Please enter your first name", true);
            navigateToStep(4);
            return;
        }
        
        if (adminLastName.isEmpty()) {
            showStatus("Please enter your last name", true);
            navigateToStep(4);
            return;
        }
        
        if (adminEmail.isEmpty()) {
            showStatus("Please enter email address", true);
            navigateToStep(4);
            return;
        }
        
        if (adminPassword.isEmpty()) {
            showStatus("Please enter password", true);
            navigateToStep(4);
            return;
        }
        
        if (!adminPassword.equals(adminConfirmPassword)) {
            showStatus("Passwords do not match", true);
            navigateToStep(4);
            return;
        }
        
        if (!PasswordUtil.isPasswordValid(adminPassword)) {
            showStatus("Password does not meet requirements", true);
            navigateToStep(4);
            return;
        }
        
        // Disable register button during registration
        registerButton.setEnabled(false);
        registerButton.setText("Registering...");
        
        // Perform registration in background
        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                try {
                    // Build registration data according to Django backend format
                    Map<String, Object> registrationData = new HashMap<>();
                    
                    // User data
                    registrationData.put("email", adminEmail);
                    registrationData.put("first_name", adminFirstName);
                    registrationData.put("last_name", adminLastName);
                    registrationData.put("password", adminPassword);
                    registrationData.put("password_confirm", adminConfirmPassword);
                    registrationData.put("phone_number", adminPhone);
                    registrationData.put("role", "denomination_admin");
                    
                    // Church data
                    Map<String, Object> churchData = new HashMap<>();
                    churchData.put("name", churchName);
                    churchData.put("address_line1", churchAddress);
                    churchData.put("city", churchCity);
                    churchData.put("county", churchCounty);
                    churchData.put("phone_number", churchPhone);
                    churchData.put("email", churchEmail);
                    churchData.put("website", churchWebsite.isEmpty() ? null : churchWebsite);
                    churchData.put("senior_pastor_name", churchPastor);
                    churchData.put("senior_pastor_phone", churchPastorPhone.isEmpty() ? null : churchPastorPhone);
                    churchData.put("senior_pastor_email", churchPastorEmail.isEmpty() ? null : churchPastorEmail);
                    churchData.put("church_type", churchTypeValue);
                    churchData.put("membership_count", churchMembership.isEmpty() ? 0 : Integer.parseInt(churchMembership));
                    churchData.put("average_attendance", churchAttendance.isEmpty() ? 0 : Integer.parseInt(churchAttendance));
                    
                    registrationData.put("church_data", churchData);
                    
                    // Send registration request to Django backend
                    System.out.println("=== REGISTRATION DATA BEING SENT ===");
                    System.out.println("Email: " + adminEmail);
                    System.out.println("First Name: " + adminFirstName);
                    System.out.println("Last Name: " + adminLastName);
                    System.out.println("Password: [HIDDEN]");
                    System.out.println("Password Confirm: [HIDDEN]");
                    System.out.println("Phone: " + adminPhone);
                    System.out.println("Role: " + "denomination_admin");
                    System.out.println("Church Data: " + churchData);
                    System.out.println("=== END REGISTRATION DATA ===");
                    
                    // Call static register method directly on the class
                    return SanctumApiClient.register(registrationData).get();
                    
                } catch (Exception e) {
                    logger.severe("Registration failed: " + e.getMessage());
                    throw e;
                }
            }
            
            @Override
            protected void done() {
                try {
                    boolean success = get();
                    
                    if (success) {
                        showStatus("Registration successful! Redirecting to login...", false);
                        
                        // Close registration and open login after delay
                        Timer timer = new Timer(2000, e -> {
                            dispose();
                            // Open login frame
                            LoginFrame loginFrame = new LoginFrame();
                            loginFrame.setVisible(true);
                        });
                        timer.setRepeats(false);
                        timer.start();
                    } else {
                        showStatus("Registration failed. Please try again.", true);
                    }
                } catch (Exception e) {
                    showStatus("Registration error: " + e.getMessage(), true);
                } finally {
                    // Re-enable register button
                    registerButton.setEnabled(true);
                    registerButton.setText("Complete Registration");
                }
            }
        };
        
        worker.execute();
    }
    
    private void showStatus(String message, boolean isError) {
        statusLabel.setText(message);
        statusLabel.setForeground(isError ? C_TEXT_MID : C_GOLD);
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
                System.out.println("RegistrationFrame PNG icon loaded successfully - Size: " + iconImage.getWidth(null) + "x" + iconImage.getHeight(null));
            } else {
                System.out.println("RegistrationFrame PNG icon failed to load, trying ICO fallback");
                iconImage = loadIconFromResources("/images/icon.ico");
                
                if (iconImage != null) {
                    setIconImage(iconImage);
                    System.out.println("RegistrationFrame ICO icon loaded successfully as fallback - Size: " + iconImage.getWidth(null) + "x" + iconImage.getHeight(null));
                } else {
                    System.out.println("RegistrationFrame Both PNG and ICO fallback failed - using default icon");
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to set RegistrationFrame application icon: " + e.getMessage());
        }
    }
    
    /**
     * Load icon image from resources using ImageIO for better format support
     */
    private Image loadIconFromResources(String path) {
        try {
            InputStream inputStream = RegistrationFrame.class.getResourceAsStream(path);
            if (inputStream == null) {
                System.out.println("RegistrationFrame Resource not found: " + path);
                return null;
            }
            
            // Use ImageIO to read the image (better for ICO files)
            BufferedImage bufferedImage = ImageIO.read(inputStream);
            inputStream.close();
            
            if (bufferedImage != null) {
                System.out.println("RegistrationFrame Successfully loaded image from " + path + " - Size: " + bufferedImage.getWidth() + "x" + bufferedImage.getHeight());
                return bufferedImage;
            } else {
                System.out.println("RegistrationFrame Failed to read image from " + path);
                return null;
            }
            
        } catch (Exception e) {
            System.err.println("RegistrationFrame Error loading image from " + path + ": " + e.getMessage());
            return null;
        }
    }
    
}
