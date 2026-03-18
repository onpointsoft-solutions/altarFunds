package com.sanctum.view;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.awt.Image;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Secretary Dashboard Frame with dark theme matching ChurchAdminFrame
 */
public class SecretaryDashboardFrame extends JFrame {
    
    // Sanctum Brand Color System
    private static final Color C_BG          = new Color(14,  46,  42);   // Deep Emerald Green
    private static final Color C_SURFACE     = new Color(19,  58,  54);   // Dark Green Secondary
    private static final Color C_CARD        = new Color(28,  47,  44);   // Input Background
    private static final Color C_GOLD        = new Color(212, 175,  55);  // Gold Accent
    private static final Color C_GOLD_HOVER  = new Color(230, 199, 102);  // Light Gold Hover
    private static final Color C_GOLD_DIM    = new Color(212, 175,  55, 25);  // Dim Gold
    private static final Color C_TEXT        = new Color(255, 255, 255);  // White Text
    private static final Color C_TEXT_MID    = new Color(207, 207, 207);  // Soft Gray Secondary
    private static final Color C_BORDER      = new Color(42,  74,  69);   // Border Color
    
    // Fonts
    private static final Font FONT_TITLE = new Font("Monospaced", Font.BOLD, 24);
    private static final Font FONT_LABEL = new Font("Monospaced", Font.BOLD, 12);
    private static final Font FONT_VALUE = new Font("Monospaced", Font.BOLD, 16);
    private static final Font FONT_SMALL = new Font("Monospaced", Font.PLAIN, 11);
    
    private int mouseX, mouseY;
    
    public SecretaryDashboardFrame() {
        setApplicationIcon();
        initializeFrame();
        createComponents();
        layoutComponents();
        setupEventHandlers();
    }
    
    private void initializeFrame() {
        setTitle("Sanctum — Secretary Dashboard");
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
        // Components will be created in layoutComponents
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
                
                g2.dispose();
            }
        };
        mainContainer.setOpaque(false);
        
        // Window controls
        JPanel windowControls = createWindowControls();
        
        // Side navigation
        JPanel sideNav = createSideNavigation();
        
        // Content panel
        JPanel contentPanel = createContentPanel();
        
        add(windowControls, BorderLayout.NORTH);
        mainContainer.add(sideNav, BorderLayout.WEST);
        mainContainer.add(contentPanel, BorderLayout.CENTER);
        add(mainContainer, BorderLayout.CENTER);
    }
    
    private JPanel createContentPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Header
        JPanel headerPanel = createHeaderPanel();
        panel.add(headerPanel, BorderLayout.NORTH);
        
        // Main content
        JPanel mainContent = createMainContent();
        panel.add(mainContent, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 30, 20));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(10, 10, 10, 10);
        
        // Title
        gbc.gridx = 0; gbc.gridy = 0;
        JLabel titleLabel = new JLabel("📝 SECRETARY DASHBOARD");
        titleLabel.setFont(FONT_TITLE);
        titleLabel.setForeground(C_GOLD);
        panel.add(titleLabel, gbc);
        
        // Subtitle
        gbc.gridy = 1;
        JLabel subtitleLabel = new JLabel("Administrative Management & Communication");
        subtitleLabel.setFont(FONT_LABEL);
        subtitleLabel.setForeground(C_TEXT_MID);
        panel.add(subtitleLabel, gbc);
        
        // Quick stats
        gbc.gridx = 1; gbc.gridy = 0; gbc.gridheight = 2;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.weightx = 1.0;
        
        JPanel statsPanel = new JPanel(new GridLayout(1, 3, 20, 0));
        statsPanel.setOpaque(false);
        
        statsPanel.add(createStatCard("Total Members", "1,247", C_GOLD));
        statsPanel.add(createStatCard("Pending Tasks", "23", C_GOLD_HOVER));
        statsPanel.add(createStatCard("This Week", "8 Events", C_TEXT_MID));
        
        panel.add(statsPanel, gbc);
        
        return panel;
    }
    
    private JPanel createStatCard(String title, String value, Color accent) {
        JPanel card = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                g2.setColor(C_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                
                g2.setColor(accent);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 8, 8);
                
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        card.setPreferredSize(new Dimension(180, 80));
        
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(FONT_SMALL);
        titleLabel.setForeground(C_TEXT_MID);
        
        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(FONT_VALUE);
        valueLabel.setForeground(accent);
        
        card.add(titleLabel, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        
        return card;
    }
    
    private JPanel createMainContent() {
        JPanel panel = new JPanel(new GridLayout(2, 2, 20, 20));
        panel.setOpaque(false);
        
        // Member Management
        panel.add(createSectionCard("👥 Member Management", createMemberContent(), C_GOLD));
        
        // Communication
        panel.add(createSectionCard("📧 Communication", createCommunicationContent(), C_GOLD_HOVER));
        
        // Event Planning
        panel.add(createSectionCard("📅 Event Planning", createEventContent(), C_TEXT_MID));
        
        // Document Management
        panel.add(createSectionCard("📄 Document Management", createDocumentContent(), C_GOLD));
        
        return panel;
    }
    
    private JPanel createSectionCard(String title, JPanel content, Color accent) {
        JPanel card = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                g2.setColor(C_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                
                g2.setColor(accent);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 12, 12);
                
                // Glow effect
                g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 20));
                g2.fillRoundRect(1, 1, getWidth()-2, 4, 4, 4);
                
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(FONT_LABEL);
        titleLabel.setForeground(accent);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));
        
        card.add(titleLabel, BorderLayout.NORTH);
        card.add(content, BorderLayout.CENTER);
        
        return card;
    }
    
    private JPanel createMemberContent() {
        JPanel panel = new JPanel(new GridLayout(4, 1, 10, 10));
        panel.setOpaque(false);
        
        String[] memberTasks = {"New Member Registrations: 5", "Membership Renewals: 12", "Contact Updates: 8", "Attendance Records: 156"};
        
        for (String task : memberTasks) {
            JPanel itemPanel = new JPanel(new BorderLayout());
            itemPanel.setOpaque(false);
            
            JLabel taskLabel = new JLabel(task);
            taskLabel.setFont(FONT_SMALL);
            taskLabel.setForeground(C_TEXT);
            
            JButton actionBtn = new JButton("Manage") {
                @Override protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2 = (Graphics2D) g.create();
                    if (getModel().isRollover()) {
                        g2.setColor(C_GOLD_DIM);
                        g2.fillRect(0, 0, getWidth(), getHeight());
                    }
                    g2.dispose();
                }
            };
            actionBtn.setContentAreaFilled(false);
            actionBtn.setBorderPainted(false);
            actionBtn.setFocusPainted(false);
            actionBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            actionBtn.setFont(FONT_SMALL);
            actionBtn.setForeground(C_GOLD);
            actionBtn.setPreferredSize(new Dimension(60, 25));
            
            itemPanel.add(taskLabel, BorderLayout.WEST);
            itemPanel.add(actionBtn, BorderLayout.EAST);
            
            panel.add(itemPanel);
        }
        
        return panel;
    }
    
    private JPanel createCommunicationContent() {
        JPanel panel = new JPanel(new GridLayout(4, 1, 10, 10));
        panel.setOpaque(false);
        
        String[] communications = {"Weekly Newsletter", "Prayer Requests", "Announcements", "Email Campaigns"};
        
        for (String comm : communications) {
            JButton commBtn = new JButton(comm) {
                @Override protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2 = (Graphics2D) g.create();
                    if (getModel().isRollover()) {
                        g2.setColor(C_GOLD_DIM);
                        g2.fillRect(0, 0, getWidth(), getHeight());
                    }
                    g2.dispose();
                }
            };
            commBtn.setContentAreaFilled(false);
            commBtn.setBorderPainted(false);
            commBtn.setFocusPainted(false);
            commBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            commBtn.setFont(FONT_SMALL);
            commBtn.setForeground(C_TEXT);
            commBtn.setHorizontalAlignment(SwingConstants.LEFT);
            
            panel.add(commBtn);
        }
        
        return panel;
    }
    
    private JPanel createEventContent() {
        JPanel panel = new JPanel(new GridLayout(4, 1, 10, 10));
        panel.setOpaque(false);
        
        String[] events = {
            "Sunday Service - This Week",
            "Bible Study - Wednesday",
            "Youth Fellowship - Friday",
            "Community Outreach - Saturday"
        };
        
        for (String event : events) {
            JPanel itemPanel = new JPanel(new BorderLayout());
            itemPanel.setOpaque(false);
            
            JLabel eventLabel = new JLabel(event);
            eventLabel.setFont(FONT_SMALL);
            eventLabel.setForeground(C_TEXT);
            
            JLabel statusLabel = new JLabel("Active");
            statusLabel.setFont(FONT_SMALL);
            statusLabel.setForeground(C_GOLD_HOVER);
            
            itemPanel.add(eventLabel, BorderLayout.WEST);
            itemPanel.add(statusLabel, BorderLayout.EAST);
            
            panel.add(itemPanel);
        }
        
        return panel;
    }
    
    private JPanel createDocumentContent() {
        JPanel panel = new JPanel(new GridLayout(4, 1, 10, 10));
        panel.setOpaque(false);
        
        String[] documents = {
            "Meeting Minutes: 12 files",
            "Reports: 8 files", 
            "Forms: 15 files",
            "Archives: 234 files"
        };
        
        for (String doc : documents) {
            JPanel itemPanel = new JPanel(new BorderLayout());
            itemPanel.setOpaque(false);
            
            JLabel docLabel = new JLabel(doc);
            docLabel.setFont(FONT_SMALL);
            docLabel.setForeground(C_TEXT);
            
            JButton viewBtn = new JButton("View") {
                @Override protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2 = (Graphics2D) g.create();
                    if (getModel().isRollover()) {
                        g2.setColor(C_GOLD_DIM);
                        g2.fillRect(0, 0, getWidth(), getHeight());
                    }
                    g2.dispose();
                }
            };
            viewBtn.setContentAreaFilled(false);
            viewBtn.setBorderPainted(false);
            viewBtn.setFocusPainted(false);
            viewBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            viewBtn.setFont(FONT_SMALL);
            viewBtn.setForeground(C_GOLD);
            viewBtn.setPreferredSize(new Dimension(50, 25));
            
            itemPanel.add(docLabel, BorderLayout.WEST);
            itemPanel.add(viewBtn, BorderLayout.EAST);
            
            panel.add(itemPanel);
        }
        
        return panel;
    }
    
    private JPanel createSideNavigation() {
        JPanel sideNav = new JPanel(new BorderLayout()) {
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
                g2.fillOval(getWidth() - 50, 50, 40, 40);
                g2.fillOval(getWidth() - 30, 150, 20, 20);
                
                g2.dispose();
            }
        };
        sideNav.setOpaque(false);
        sideNav.setBorder(BorderFactory.createEmptyBorder(40, 20, 40, 40));
        sideNav.setPreferredSize(new Dimension(250, 0));
        
        // Title
        JLabel titleLabel = new JLabel("// NAVIGATION", SwingConstants.CENTER);
        titleLabel.setFont(FONT_LABEL);
        titleLabel.setForeground(C_GOLD);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 30, 0));
        
        // Navigation buttons
        JPanel navContainer = new JPanel(new GridLayout(6, 1, 0, 15));
        navContainer.setOpaque(false);
        
        // Navigation buttons
        JButton homeBtn = createNavButton("🏠 HOME", C_GOLD);
        JButton membersBtn = createNavButton("👥 MEMBERS", C_GOLD);
        JButton communicationBtn = createNavButton("📧 COMMUNICATION", C_GOLD_HOVER);
        JButton eventsBtn = createNavButton("📅 EVENTS", C_TEXT_MID);
        JButton documentsBtn = createNavButton("📄 DOCUMENTS", C_GOLD);
        JButton backBtn = createNavButton("🔙 BACK TO LOGIN", C_GOLD_HOVER);
        
        // Add action listeners
        homeBtn.addActionListener(e -> {
            dispose();
            new LoginFrame().setVisible(true);
        });
        
        backBtn.addActionListener(e -> {
            dispose();
            new LoginFrame().setVisible(true);
        });
        
        navContainer.add(homeBtn);
        navContainer.add(membersBtn);
        navContainer.add(communicationBtn);
        navContainer.add(eventsBtn);
        navContainer.add(documentsBtn);
        navContainer.add(backBtn);
        
        // Add components to side nav
        sideNav.add(titleLabel, BorderLayout.NORTH);
        sideNav.add(navContainer, BorderLayout.CENTER);
        
        return sideNav;
    }
    
    private JButton createNavButton(String text, Color accent) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
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
                g2.setFont(FONT_SMALL);
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
        
        JLabel title = new JLabel("  Sanctum Secretary Dashboard");
        title.setFont(FONT_SMALL);
        title.setForeground(C_TEXT_MID);
        
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
                    g2.setColor(C_GOLD_HOVER);
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
        // Event handlers setup
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
                System.out.println("SecretaryDashboardFrame PNG icon loaded successfully - Size: " + iconImage.getWidth(null) + "x" + iconImage.getHeight(null));
            } else {
                System.out.println("SecretaryDashboardFrame PNG icon failed to load, trying ICO fallback");
                iconImage = loadIconFromResources("/images/icon.ico");
                
                if (iconImage != null) {
                    setIconImage(iconImage);
                    System.out.println("SecretaryDashboardFrame ICO icon loaded successfully as fallback - Size: " + iconImage.getWidth(null) + "x" + iconImage.getHeight(null));
                } else {
                    System.out.println("SecretaryDashboardFrame Both PNG and ICO fallback failed - using default icon");
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to set SecretaryDashboardFrame application icon: " + e.getMessage());
        }
    }
    
    /**
     * Load icon image from resources using ImageIO for better format support
     */
    private Image loadIconFromResources(String path) {
        try {
            InputStream inputStream = SecretaryDashboardFrame.class.getResourceAsStream(path);
            if (inputStream == null) {
                System.out.println("SecretaryDashboardFrame Resource not found: " + path);
                return null;
            }
            
            // Use ImageIO to read the image (better for ICO files)
            BufferedImage bufferedImage = ImageIO.read(inputStream);
            inputStream.close();
            
            if (bufferedImage != null) {
                System.out.println("SecretaryDashboardFrame Successfully loaded image from " + path + " - Size: " + bufferedImage.getWidth() + "x" + bufferedImage.getHeight());
                return bufferedImage;
            } else {
                System.out.println("SecretaryDashboardFrame Failed to read image from " + path);
                return null;
            }
            
        } catch (Exception e) {
            System.err.println("SecretaryDashboardFrame Error loading image from " + path + ": " + e.getMessage());
            return null;
        }
    }
    
    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        
        SwingUtilities.invokeLater(() -> {
            SecretaryDashboardFrame frame = new SecretaryDashboardFrame();
            frame.setVisible(true);
        });
    }
}
