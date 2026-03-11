package com.sanctum.view;

import com.sanctum.api.SanctumApiClient;
import com.sanctum.auth.SessionManager;
import com.sanctum.util.LogoLoader;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import javax.swing.SwingWorker;

/**
 * Church Setup Frame - Setup church information after admin registration
 */
public class ChurchSetupFrame extends JFrame {
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

    // Wizard steps
    private static final String[] STEP_TITLES = {
        "Church Information",
        "Church Contact", 
        "Pastor Details",
        "Church Statistics"
    };
    
    private static final String[] STEP_DESCRIPTIONS = {
        "Please enter the basic details about your church location.",
        "Provide contact information for the church office.",
        "Enter information about the senior pastor.",
        "Share statistics about your church community."
    };
    
    private int currentStep = 0;
    private JPanel stepIndicatorPanel;
    private JLabel stepTitleLabel;
    private JLabel stepDescriptionLabel;
    private JPanel cardPanel;
    private CardLayout cardLayout;

    // Form components
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

    private JButton nextButton;
    private JButton backButton;
    private JButton finishButton;
    private JLabel statusLabel;
    private JLabel loadingLabel;
    private JProgressBar loadingBar;
    private JPanel loadingPanel;

    private final SessionManager sessionManager;

    public ChurchSetupFrame() {
        this.sessionManager = SessionManager.getInstance();
        initializeFrame();
        createComponents();
        layoutComponents();
        setupEventHandlers();
    }

    private void initializeFrame() {
        setTitle("Church Setup - Sanctum");
        setSize(900, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
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

    private void createComponents() {
        // Create buttons first (needed by panels)
        nextButton = createGlowButton("Next Step →", C_GOLD);
        nextButton.setPreferredSize(new Dimension(150, 45));
        
        backButton = createGlowButton("← Back", C_TEXT_MID);
        backButton.setPreferredSize(new Dimension(120, 45));
        backButton.setEnabled(false);
        
        finishButton = createGlowButton("Complete Setup", C_GOLD);
        finishButton.setPreferredSize(new Dimension(180, 45));
        finishButton.setVisible(false);

        // Status label
        statusLabel = new JLabel("", SwingConstants.CENTER);
        statusLabel.setFont(FONT_SMALL);
        statusLabel.setForeground(C_GOLD);

        // Loading components
        loadingLabel = new JLabel("Setting up your church...", SwingConstants.CENTER);
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

        // Create wizard header
        JPanel wizardHeader = createWizardHeader();
        
        // Create card panel for wizard steps
        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);
        cardPanel.setBackground(C_BG);
        
        // Create all step panels
        createStepPanels();
        
        // Create navigation panel
        JPanel navigationPanel = createNavigationPanel();
        
        // Window controls
        JPanel windowControls = createWindowControls();

        add(windowControls, BorderLayout.NORTH);
        add(mainContainer, BorderLayout.CENTER);

        // Add wizard components to main container
        JPanel wizardContainer = new JPanel(new BorderLayout());
        wizardContainer.setOpaque(false);
        wizardContainer.add(wizardHeader, BorderLayout.NORTH);
        wizardContainer.add(cardPanel, BorderLayout.CENTER);
        wizardContainer.add(navigationPanel, BorderLayout.SOUTH);

        // Side panels for spacing
        JPanel leftPanel = new JPanel();
        leftPanel.setOpaque(false);
        leftPanel.setPreferredSize(new Dimension(50, 0));

        JPanel rightPanel = new JPanel();
        rightPanel.setOpaque(false);
        rightPanel.setPreferredSize(new Dimension(50, 0));

        mainContainer.add(leftPanel, BorderLayout.WEST);
        mainContainer.add(wizardContainer, BorderLayout.CENTER);
        mainContainer.add(rightPanel, BorderLayout.EAST);

        // Initialize first step
        updateStepDisplay();
    }

    private void createStepPanels() {
        // Step 0: Church Information
        JPanel step0Panel = createChurchBasicPanel();
        step0Panel.setBorder(BorderFactory.createEmptyBorder(40, 60, 40, 60));
        cardPanel.add(step0Panel, "step0");
        
        // Step 1: Church Contact
        JPanel step1Panel = createChurchContactPanel();
        step1Panel.setBorder(BorderFactory.createEmptyBorder(40, 60, 40, 60));
        cardPanel.add(step1Panel, "step1");
        
        // Step 2: Pastor Details
        JPanel step2Panel = createPastorPanel();
        step2Panel.setBorder(BorderFactory.createEmptyBorder(40, 60, 40, 60));
        cardPanel.add(step2Panel, "step2");
        
        // Step 3: Church Statistics
        JPanel step3Panel = createChurchStatsPanel();
        step3Panel.setBorder(BorderFactory.createEmptyBorder(40, 60, 40, 60));
        cardPanel.add(step3Panel, "step3");
    }

    private JPanel createWizardHeader() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(C_SURFACE);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(30, 60, 30, 60));
        
        // Title
        JLabel titleLabel = new JLabel("Church Setup");
        titleLabel.setFont(FONT_TITLE);
        titleLabel.setForeground(C_TEXT);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // Step indicator panel
        stepIndicatorPanel = createStepIndicatorPanel();
        
        // Step title and description
        stepTitleLabel = new JLabel(STEP_TITLES[0]);
        stepTitleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        stepTitleLabel.setForeground(C_GOLD);
        
        stepDescriptionLabel = new JLabel(STEP_DESCRIPTIONS[0]);
        stepDescriptionLabel.setFont(FONT_SMALL);
        stepDescriptionLabel.setForeground(C_TEXT_MID);
        
        // Layout
        JPanel titlePanel = new JPanel();
        titlePanel.setOpaque(false);
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        titlePanel.add(titleLabel);
        titlePanel.add(Box.createVerticalStrut(20));
        titlePanel.add(stepIndicatorPanel);
        titlePanel.add(Box.createVerticalStrut(15));
        titlePanel.add(stepTitleLabel);
        titlePanel.add(stepDescriptionLabel);
        
        headerPanel.add(titlePanel, BorderLayout.CENTER);
        
        return headerPanel;
    }

    private JPanel createStepIndicatorPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 4, 10, 0));
        panel.setOpaque(false);
        
        for (int i = 0; i < STEP_TITLES.length; i++) {
            JPanel stepPanel = createStepIndicator(i);
            panel.add(stepPanel);
        }
        
        return panel;
    }

    private JPanel createStepIndicator(int stepIndex) {
        JPanel stepPanel = new JPanel(new BorderLayout());
        stepPanel.setOpaque(false);
        
        // Step circle
        JPanel circlePanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                int size = Math.min(getWidth(), getHeight());
                int x = (getWidth() - size) / 2;
                int y = (getHeight() - size) / 2;
                
                if (stepIndex < currentStep) {
                    // Completed step
                    g2.setColor(C_GOLD);
                    g2.fillOval(x, y, size, size);
                    g2.setColor(C_TEXT);
                    g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
                    String check = "✓";
                    FontMetrics fm = g2.getFontMetrics();
                    int checkX = x + (size - fm.stringWidth(check)) / 2;
                    int checkY = y + (size - fm.getHeight()) / 2 + fm.getAscent();
                    g2.drawString(check, checkX, checkY);
                } else if (stepIndex == currentStep) {
                    // Current step
                    g2.setColor(C_GOLD);
                    g2.fillOval(x, y, size, size);
                    g2.setColor(C_BG);
                    g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
                    String stepNum = String.valueOf(stepIndex + 1);
                    FontMetrics fm = g2.getFontMetrics();
                    int numX = x + (size - fm.stringWidth(stepNum)) / 2;
                    int numY = y + (size - fm.getHeight()) / 2 + fm.getAscent();
                    g2.drawString(stepNum, numX, numY);
                } else {
                    // Future step
                    g2.setColor(C_BORDER);
                    g2.fillOval(x, y, size, size);
                    g2.setColor(C_TEXT_DIM);
                    g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
                    String stepNum = String.valueOf(stepIndex + 1);
                    FontMetrics fm = g2.getFontMetrics();
                    int numX = x + (size - fm.stringWidth(stepNum)) / 2;
                    int numY = y + (size - fm.getHeight()) / 2 + fm.getAscent();
                    g2.drawString(stepNum, numX, numY);
                }
                
                g2.dispose();
            }
        };
        circlePanel.setPreferredSize(new Dimension(40, 40));
        
        // Step label
        JLabel stepLabel = new JLabel("Step " + (stepIndex + 1));
        stepLabel.setFont(FONT_SMALL);
        stepLabel.setForeground(stepIndex <= currentStep ? C_TEXT_MID : C_TEXT_DIM);
        stepLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        stepPanel.add(circlePanel, BorderLayout.CENTER);
        stepPanel.add(stepLabel, BorderLayout.SOUTH);
        
        return stepPanel;
    }

    private void updateStepDisplay() {
        stepTitleLabel.setText(STEP_TITLES[currentStep]);
        stepDescriptionLabel.setText(STEP_DESCRIPTIONS[currentStep]);
        
        // Refresh step indicators
        stepIndicatorPanel.removeAll();
        for (int i = 0; i < STEP_TITLES.length; i++) {
            stepIndicatorPanel.add(createStepIndicator(i));
        }
        stepIndicatorPanel.revalidate();
        stepIndicatorPanel.repaint();
        
        // Show current step
        cardLayout.show(cardPanel, "step" + currentStep);
        
        // Update navigation buttons
        backButton.setEnabled(currentStep > 0);
        nextButton.setVisible(currentStep < STEP_TITLES.length - 1);
        finishButton.setVisible(currentStep == STEP_TITLES.length - 1);
    }

    private JPanel createNavigationPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(C_SURFACE);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 60, 30, 60));
        
        // Progress bar
        JProgressBar progressBar = new JProgressBar(0, STEP_TITLES.length - 1);
        progressBar.setValue(currentStep);
        progressBar.setForeground(C_GOLD);
        progressBar.setBackground(C_BORDER);
        progressBar.setBorderPainted(false);
        progressBar.setStringPainted(true);
        progressBar.setString("Step " + (currentStep + 1) + " of " + STEP_TITLES.length);
        
        // Navigation buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        buttonPanel.setOpaque(false);
        
        buttonPanel.add(backButton);
        buttonPanel.add(nextButton);
        buttonPanel.add(finishButton);
        
        // Status panel
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setOpaque(false);
        statusPanel.add(statusLabel, BorderLayout.CENTER);
        statusPanel.add(loadingPanel, BorderLayout.SOUTH);
        
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setOpaque(false);
        bottomPanel.add(progressBar, BorderLayout.CENTER);
        bottomPanel.add(buttonPanel, BorderLayout.EAST);
        
        panel.add(bottomPanel, BorderLayout.CENTER);
        panel.add(statusPanel, BorderLayout.SOUTH);
        
        return panel;
    }

    private JPanel createChurchBasicPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 8, 8, 8);

        // Church Name
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        addFormField(panel, gbc, "Church Name", churchNameField = createTextField("Enter your church name"), 0, 0);

        // Address
        gbc.gridy = 1;
        addFormField(panel, gbc, "Address Line 1", churchAddressField = createTextField("Enter street address"), 0, 1);

        // City and County
        gbc.gridy = 2; gbc.gridwidth = 1;
        addFormField(panel, gbc, "City", churchCityField = createTextField("Enter city"), 0, 2);
        addFormField(panel, gbc, "County", churchCountyField = createTextField("Enter county"), 1, 2);

        // Add spacing at bottom
        gbc.gridy = 3;
        gbc.weighty = 1.0;
        panel.add(Box.createVerticalGlue(), gbc);

        return panel;
    }

    private JPanel createChurchContactPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 8, 8, 8);

        // Church Phone
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        addFormField(panel, gbc, "Church Phone", churchPhoneField = createTextField("+254 7XX XXX XXX"), 0, 0);

        // Church Email
        gbc.gridy = 1;
        addFormField(panel, gbc, "Church Email", churchEmailField = createTextField("church@example.com"), 0, 1);

        // Website
        gbc.gridy = 2;
        addFormField(panel, gbc, "Website", churchWebsiteField = createTextField("www.churchwebsite.com (optional)"), 0, 2);

        // Add spacing at bottom
        gbc.gridy = 3;
        gbc.weighty = 1.0;
        panel.add(Box.createVerticalGlue(), gbc);

        return panel;
    }

    private JPanel createPastorPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 8, 8, 8);

        // Pastor Name
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        addFormField(panel, gbc, "Senior Pastor Name", churchPastorField = createTextField("Pastor's full name"), 0, 0);

        // Pastor Contact
        gbc.gridy = 1; gbc.gridwidth = 1;
        addFormField(panel, gbc, "Pastor Phone", churchPastorPhoneField = createTextField("+254 7XX XXX XXX"), 0, 1);
        addFormField(panel, gbc, "Pastor Email", churchPastorEmailField = createTextField("pastor@church.com"), 1, 1);

        // Add spacing at bottom
        gbc.gridy = 2;
        gbc.weighty = 1.0;
        panel.add(Box.createVerticalGlue(), gbc);

        return panel;
    }

    private JPanel createChurchStatsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 8, 8, 8);

        // Church Type and Membership
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 1;
        addFormField(panel, gbc, "Church Type", churchTypeField = createChurchTypeDropdown(), 0, 0);
        addFormField(panel, gbc, "Membership Count", churchMembershipField = createTextField("Number of members"), 1, 0);

        // Average Attendance
        gbc.gridy = 1; gbc.gridwidth = 2;
        addFormField(panel, gbc, "Average Attendance", churchAttendanceField = createTextField("Average weekly attendance"), 0, 1);

        // Add spacing at bottom
        gbc.gridy = 2;
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

    private JComboBox<String> createChurchTypeDropdown() {
        String[] churchTypes = {
            "Main Church",
            "Branch Church", 
            "Church Plant",
            "Chaplaincy",
            "Fellowship",
            "Non-denominational",
            "Denominational",
            "Other"
        };
        JComboBox<String> combo = new JComboBox<>(churchTypes);
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

    private JButton createGlowButton(String text, Color color) {
        JButton button = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                if (getModel().isPressed()) {
                    g2.setColor(color.darker());
                } else if (getModel().isRollover()) {
                    g2.setColor(color.brighter());
                } else {
                    g2.setColor(color);
                }

                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);

                // Glow effect
                if (getModel().isRollover()) {
                    g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 50));
                    g2.fillRoundRect(-2, -2, getWidth() + 4, getHeight() + 4, 14, 14);
                }

                g2.dispose();
            }

            @Override protected void paintBorder(Graphics g) {
                // No border for modern look
            }
        };

        button.setFont(FONT_BUTTON);
        button.setForeground(Color.WHITE);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createEmptyBorder(15, 25, 15, 25));

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

        // Navigation buttons
        backButton.addActionListener(e -> {
            if (currentStep > 0) {
                currentStep--;
                updateStepDisplay();
            }
        });

        nextButton.addActionListener(e -> {
            if (currentStep < STEP_TITLES.length - 1) {
                currentStep++;
                updateStepDisplay();
            }
        });

        finishButton.addActionListener(e -> handleChurchSetup());

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

    private void handleChurchSetup() {
        // Validate form
        if (!validateForm()) {
            return;
        }

        // Disable buttons and show loading
        backButton.setEnabled(false);
        nextButton.setEnabled(false);
        finishButton.setEnabled(false);
        loadingPanel.setVisible(true);
        statusLabel.setText("");

        // Create church data
        Map<String, Object> churchData = new HashMap<>();
        churchData.put("name", churchNameField.getText());
        churchData.put("address", churchAddressField.getText());
        churchData.put("city", churchCityField.getText());
        churchData.put("county", churchCountyField.getText());
        churchData.put("phone", churchPhoneField.getText());
        churchData.put("email", churchEmailField.getText());
        churchData.put("website", churchWebsiteField.getText());
        churchData.put("pastor_name", churchPastorField.getText());
        churchData.put("pastor_phone", churchPastorPhoneField.getText());
        churchData.put("pastor_email", churchPastorEmailField.getText());
        churchData.put("church_type", (String) churchTypeField.getSelectedItem());
        churchData.put("membership_count", churchMembershipField.getText());
        churchData.put("average_attendance", churchAttendanceField.getText());

        // Perform church setup in background
        SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                return SanctumApiClient.createChurch(churchData).get();
            }

            @Override
            protected void done() {
                try {
                    boolean success = get();
                    SwingUtilities.invokeLater(() -> {
                        loadingPanel.setVisible(false);
                        backButton.setEnabled(true);
                        nextButton.setEnabled(true);
                        finishButton.setEnabled(true);

                        if (success) {
                            statusLabel.setText("Church setup completed successfully!");
                            statusLabel.setForeground(C_GOLD);

                            // Show success dialog and transition to login
                            Timer timer = new Timer(2000, e -> {
                                dispose();
                                new LoginFrame().setVisible(true);
                            });
                            timer.setRepeats(false);
                            timer.start();
                        } else {
                            statusLabel.setText("Church setup failed. Please try again.");
                            statusLabel.setForeground(C_TEXT_MID);
                        }
                    });
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> {
                        loadingPanel.setVisible(false);
                        backButton.setEnabled(true);
                        nextButton.setEnabled(true);
                        finishButton.setEnabled(true);
                        statusLabel.setText("Setup error: " + ex.getMessage());
                        statusLabel.setForeground(C_TEXT_MID);
                    });
                }
            }
        };

        worker.execute();
    }

    private boolean validateForm() {
        // Basic validation for required fields
        if (churchNameField.getText().isEmpty() || churchNameField.getText().equals("Enter your church name")) {
            statusLabel.setText("Please enter your church name");
            statusLabel.setForeground(C_TEXT_MID);
            return false;
        }

        if (churchAddressField.getText().isEmpty() || churchAddressField.getText().equals("Enter street address")) {
            statusLabel.setText("Please enter your church address");
            statusLabel.setForeground(C_TEXT_MID);
            return false;
        }

        return true;
    }
}
