package com.sanctum.view;

import com.sanctum.security.SessionManager;
import com.sanctum.ui.ModernButton;
import com.sanctum.ui.RoundedPanel;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.awt.Image;
import javax.swing.*;
import java.awt.*;

/**
 * Main application frame with sidebar navigation and dashboard
 */
public class MainFrame extends JFrame {
    private final String authToken;
    private JPanel mainPanel;
    private CardLayout cardLayout;
    private JLabel userLabel;
    private JLabel churchLabel;
    
    public MainFrame(String authToken) {
        this.authToken = authToken;
        setApplicationIcon();
        initializeFrame();
        createComponents();
        layoutComponents();
        setupEventHandlers();
        loadUserData();
    }
    
    private void initializeFrame() {
        setTitle("Sanctum - Church Management System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(1000, 600));
        getContentPane().setBackground(new Color(244, 246, 249));
    }
    
    private void createComponents() {
        // Create card layout for main content
        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);
        
        // Create dashboard panel
        JPanel dashboardPanel = createDashboardPanel();
        mainPanel.add(dashboardPanel, "dashboard");
        
        // Create members panel
        JPanel membersPanel = createMembersPanel();
        mainPanel.add(membersPanel, "members");
        
        // Create donations panel
        JPanel donationsPanel = createDonationsPanel();
        mainPanel.add(donationsPanel, "donations");
        
        // Create expenses panel
        JPanel expensesPanel = createExpensesPanel();
        mainPanel.add(expensesPanel, "expenses");
        
        // Create events panel
        JPanel eventsPanel = createEventsPanel();
        mainPanel.add(eventsPanel, "events");
        
        // Create reports panel
        JPanel reportsPanel = createReportsPanel();
        mainPanel.add(reportsPanel, "reports");
        
        // Create settings panel
        JPanel settingsPanel = createSettingsPanel();
        mainPanel.add(settingsPanel, "settings");
        
        // User info labels
        userLabel = new JLabel("Loading...", SwingConstants.RIGHT);
        userLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        userLabel.setForeground(new Color(55, 65, 81));
        
        churchLabel = new JLabel("Church: Loading...", SwingConstants.RIGHT);
        churchLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        churchLabel.setForeground(new Color(107, 114, 128));
    }
    
    private JPanel createDashboardPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        
        JLabel label = new JLabel("Dashboard", SwingConstants.CENTER);
        label.setFont(new Font("Segoe UI", Font.BOLD, 24));
        panel.add(label, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createMembersPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        
        JLabel label = new JLabel("Members Management", SwingConstants.CENTER);
        label.setFont(new Font("Segoe UI", Font.BOLD, 24));
        panel.add(label, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createDonationsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        
        JLabel label = new JLabel("Donations Management", SwingConstants.CENTER);
        label.setFont(new Font("Segoe UI", Font.BOLD, 24));
        panel.add(label, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createExpensesPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        
        JLabel label = new JLabel("Expenses Management", SwingConstants.CENTER);
        label.setFont(new Font("Segoe UI", Font.BOLD, 24));
        panel.add(label, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createEventsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        
        JLabel label = new JLabel("Events Management", SwingConstants.CENTER);
        label.setFont(new Font("Segoe UI", Font.BOLD, 24));
        panel.add(label, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createReportsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        
        JLabel label = new JLabel("Reports", SwingConstants.CENTER);
        label.setFont(new Font("Segoe UI", Font.BOLD, 24));
        panel.add(label, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createSettingsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        
        JLabel label = new JLabel("Settings", SwingConstants.CENTER);
        label.setFont(new Font("Segoe UI", Font.BOLD, 24));
        panel.add(label, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createSidebar() {
        RoundedPanel sidebar = new RoundedPanel(15, new Color(10, 25, 70));
        sidebar.setPreferredSize(new Dimension(220, 0));
        sidebar.setLayout(new BorderLayout());
        
        // Logo area
        JPanel logoPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        logoPanel.setOpaque(false);
        
        JLabel logoLabel = new JLabel("⛪ SANCTUM", SwingConstants.CENTER);
        logoLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        logoLabel.setForeground(Color.WHITE);
        
        logoPanel.add(logoLabel);
        
        // Menu buttons
        JPanel menuPanel = new JPanel();
        menuPanel.setOpaque(false);
        menuPanel.setLayout(new BoxLayout(menuPanel, BoxLayout.Y_AXIS));
        menuPanel.setBorder(BorderFactory.createEmptyBorder(20, 15, 20, 15));
        
        String[] menuItems = {"Dashboard", "Members", "Donations", "Expenses", "Events", "Reports", "Settings"};
        String[] icons = {"📊", "👥", "💰", "💳", "📅", "📋", "⚙️"};
        
        for (int i = 0; i < menuItems.length; i++) {
            ModernButton menuButton = createMenuButton(icons[i] + " " + menuItems[i], menuItems[i].toLowerCase());
            menuPanel.add(menuButton);
            menuPanel.add(Box.createVerticalStrut(5));
        }
        
        // Logout button
        ModernButton logoutButton = new ModernButton("🚪 Logout");
        logoutButton.setBackgroundColor(new Color(239, 68, 68));
        logoutButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        menuPanel.add(Box.createVerticalStrut(20));
        menuPanel.add(logoutButton);
        
        // Add components to sidebar
        sidebar.add(logoPanel, BorderLayout.NORTH);
        sidebar.add(menuPanel, BorderLayout.CENTER);
        
        return sidebar;
    }
    
    private ModernButton createMenuButton(String text, String panelName) {
        ModernButton button = new ModernButton(text, new Color(59, 130, 246));
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        button.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        
        button.addActionListener(e -> {
            cardLayout.show(mainPanel, panelName);
        });
        
        return button;
    }
    
    private JPanel createTopBar() {
        RoundedPanel topBar = new RoundedPanel(10, Color.WHITE);
        topBar.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        topBar.setLayout(new BorderLayout());
        
        // Left side - welcome message
        JLabel welcomeLabel = new JLabel("Good Morning! 👋");
        welcomeLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        welcomeLabel.setForeground(new Color(55, 65, 81));
        
        // Right side - user info
        JPanel userInfoPanel = new JPanel(new BorderLayout(10, 5));
        userInfoPanel.setOpaque(false);
        
        userInfoPanel.add(churchLabel, BorderLayout.NORTH);
        userInfoPanel.add(userLabel, BorderLayout.CENTER);
        
        // Profile avatar (placeholder)
        JLabel avatarLabel = new JLabel("👤");
        avatarLabel.setFont(new Font("Segoe UI", Font.PLAIN, 24));
        avatarLabel.setPreferredSize(new Dimension(40, 40));
        avatarLabel.setHorizontalAlignment(SwingConstants.CENTER);
        avatarLabel.setVerticalAlignment(SwingConstants.CENTER);
        avatarLabel.setBackground(new Color(229, 231, 235));
        avatarLabel.setOpaque(true);
        
        // Add avatar to right
        JPanel rightPanel = new JPanel(new BorderLayout(10, 0));
        rightPanel.setOpaque(false);
        rightPanel.add(userInfoPanel, BorderLayout.CENTER);
        rightPanel.add(avatarLabel, BorderLayout.EAST);
        
        topBar.add(welcomeLabel, BorderLayout.WEST);
        topBar.add(rightPanel, BorderLayout.EAST);
        
        return topBar;
    }
    
    private void layoutComponents() {
        setLayout(new BorderLayout());
        
        // Create main container
        JPanel container = new JPanel(new BorderLayout());
        container.setOpaque(false);
        
        // Add top bar
        container.add(createTopBar(), BorderLayout.NORTH);
        container.add(mainPanel, BorderLayout.CENTER);
        
        // Add sidebar
        JPanel sidebarWrapper = new JPanel(new BorderLayout());
        sidebarWrapper.setOpaque(false);
        sidebarWrapper.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 0));
        sidebarWrapper.add(createSidebar(), BorderLayout.CENTER);
        
        // Add main content with padding
        JPanel contentWrapper = new JPanel(new BorderLayout());
        contentWrapper.setOpaque(false);
        contentWrapper.setBorder(BorderFactory.createEmptyBorder(20, 10, 20, 20));
        contentWrapper.add(container, BorderLayout.CENTER);
        
        // Add to frame
        add(sidebarWrapper, BorderLayout.WEST);
        add(contentWrapper, BorderLayout.CENTER);
    }
    
    private void setupEventHandlers() {
        // Window close handler
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                logout();
            }
        });
    }
    
    private void loadUserData() {
        // Load user data in background
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                SessionManager.UserSession session = SessionManager.getSession(authToken);
                if (session != null) {
                    SwingUtilities.invokeLater(() -> {
                        userLabel.setText(session.getUsername());
                        churchLabel.setText("Church ID: " + session.getChurchId());
                    });
                }
                return null;
            }
            
            @Override
            protected void done() {
                // Show dashboard by default
                cardLayout.show(mainPanel, "dashboard");
            }
        };
        worker.execute();
    }
    
    private void logout() {
        int confirm = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to logout?",
            "Confirm Logout",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE
        );
        
        if (confirm == JOptionPane.YES_OPTION) {
            // Invalidate session
            SessionManager.invalidateSession(authToken);
            
            // Show login frame
            SwingUtilities.invokeLater(() -> {
                LoginFrame loginFrame = new LoginFrame();
                loginFrame.setVisible(true);
                dispose();
            });
        }
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
                System.out.println("MainFrame PNG icon loaded successfully - Size: " + iconImage.getWidth(null) + "x" + iconImage.getHeight(null));
            } else {
                System.out.println("MainFrame PNG icon failed to load, trying ICO fallback");
                iconImage = loadIconFromResources("/images/icon.ico");
                
                if (iconImage != null) {
                    setIconImage(iconImage);
                    System.out.println("MainFrame ICO icon loaded successfully as fallback - Size: " + iconImage.getWidth(null) + "x" + iconImage.getHeight(null));
                } else {
                    System.out.println("MainFrame Both PNG and ICO fallback failed - using default icon");
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to set MainFrame application icon: " + e.getMessage());
        }
    }
    
    /**
     * Load icon image from resources using ImageIO for better format support
     */
    private Image loadIconFromResources(String path) {
        try {
            InputStream inputStream = MainFrame.class.getResourceAsStream(path);
            if (inputStream == null) {
                System.out.println("MainFrame Resource not found: " + path);
                return null;
            }
            
            // Use ImageIO to read the image (better for ICO files)
            BufferedImage bufferedImage = ImageIO.read(inputStream);
            inputStream.close();
            
            if (bufferedImage != null) {
                System.out.println("MainFrame Successfully loaded image from " + path + " - Size: " + bufferedImage.getWidth() + "x" + bufferedImage.getHeight());
                return bufferedImage;
            } else {
                System.out.println("MainFrame Failed to read image from " + path);
                return null;
            }
            
        } catch (Exception e) {
            System.err.println("MainFrame Error loading image from " + path + ": " + e.getMessage());
            return null;
        }
    }
}
