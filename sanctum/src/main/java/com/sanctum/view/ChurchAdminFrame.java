package com.sanctum.view;

import com.sanctum.api.SanctumApiClient;
import com.sanctum.util.WindowsDialogFix;
import com.sanctum.util.DialogManager;
import com.sanctum.util.IconManager;
import com.sanctum.auth.SessionManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Church Admin Dashboard - Modern Management Interface
 * Emerald green theme matching UsherDashboardFrame
 */
public class ChurchAdminFrame extends JFrame {
    
    // ─── Sanctum Brand Color System ───────────────────────────────────
    private static final Color C_BG          = new Color(14, 46, 42);   // Deep Emerald Green
    private static final Color C_SURFACE     = new Color(19, 58, 54);   // Dark Green Secondary
    private static final Color C_CARD        = new Color(28, 47, 44);   // Input Background
    private static final Color C_GOLD        = new Color(212, 175, 55);   // Gold Accent
    private static final Color C_GOLD_HOVER  = new Color(230, 199, 102);  // Light Gold Hover
    private static final Color C_GOLD_DIM    = new Color(212, 175, 55, 25); // Dim Gold
    private static final Color C_TEXT        = new Color(255, 255, 255);  // White Text
    private static final Color C_TEXT_MID    = new Color(207, 207, 207);  // Soft Gray Secondary
    private static final Color C_TEXT_DIM    = new Color(156, 163, 175);  // Dim Text
    private static final Color C_BORDER      = new Color(42, 74, 69);   // Border Color
    private static final Color C_SUCCESS      = new Color(52, 199, 89);
    private static final Color C_DANGER      = new Color(255, 59, 48);

    // ─── Typography ──────────────────────────────────────────────────────────
    private static final Font F_TITLE       = new Font("Segoe UI", Font.BOLD, 28);
    private static final Font F_HEADING     = new Font("Segoe UI", Font.BOLD, 16);
    private static final Font F_LABEL       = new Font("Segoe UI", Font.BOLD, 14);
    private static final Font F_BODY        = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font F_MONO_SM     = new Font("JetBrains Mono", Font.PLAIN, 11);
    private static final Font F_MONO_LG     = new Font("JetBrains Mono",Font.BOLD, 20);

    // ─── Navigation ───────────────────────────────────────────────────
    private static final String[][] MENU_ITEMS = {
        {"🏠", "Dashboard"},
        {"👥", "Members"},
        {"📢", "Announcements"},
        {"👨‍💼", "Staff"},
        {"👤", "Users"},
        {"⚙️", "Settings"}
    };

    // ─── UI State ─────────────────────────────────────────────────────
    private JPanel contentArea;
    private JPanel sidebar;
    private String activeMenu = "Dashboard";

    // ─── Constructor ──────────────────────────────────────────────────
    public ChurchAdminFrame() {
        try {
            configureWindow();
            setApplicationIcon();
            buildUI();
            loadOverviewData();
        } catch (Exception e) {
            System.err.println("Failed to create ChurchAdminFrame: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to create ChurchAdminFrame: " + e.getMessage(), e);
        }
    }

    // ─── Window Setup ─────────────────────────────────────────────────
    private void configureWindow() {
        setTitle("Sanctum — Admin Dashboard");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setUndecorated(true);
        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        Rectangle bounds = gd.getDefaultConfiguration().getBounds();
        setSize(bounds.width - 100, bounds.height - 100);
        setLocationRelativeTo(null);
        getContentPane().setBackground(C_BG);
    }

    // ─── UI Construction ──────────────────────────────────────────────
    private void buildUI() {
        setLayout(new BorderLayout());

        JPanel main = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0, C_BG, getWidth(), getHeight(), C_SURFACE));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        main.setOpaque(false);

        main.add(buildTopBar(), BorderLayout.NORTH);

        sidebar = buildSidebar();
        // Content area with CardLayout for page switching
        contentArea = new JPanel(new CardLayout());
        contentArea.setOpaque(false);
        
        // Add all pages to the card layout
        contentArea.add(buildMainDashboard(), "overview");
        contentArea.add(buildMembersPage(), "members");
        contentArea.add(buildAnnouncementsPage(), "announcements");
        contentArea.add(buildStaffPage(), "staff");
        contentArea.add(buildUsersPage(), "users");
        contentArea.add(buildSettingsPage(), "settings"); // Integrated settings page

        // Create a panel for sidebar and content
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setOpaque(false);
        centerPanel.add(sidebar, BorderLayout.WEST);
        centerPanel.add(contentArea, BorderLayout.CENTER);

        main.add(centerPanel, BorderLayout.CENTER);
        add(main);
    }

    // ─── Top Bar ──────────────────────────────────────────────────────
    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(C_SURFACE);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(C_BORDER);
                g2.fillRect(0, getHeight() - 1, getWidth(), 1);
                g2.dispose();
            }
        };
        bar.setPreferredSize(new Dimension(0, 60));
        bar.setBorder(new EmptyBorder(10, 20, 10, 20));

        // Left — Logo
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setOpaque(false);
        JLabel icon  = new JLabel("⛪");
        icon.setFont(new Font("Arial", Font.PLAIN, 15));
        JLabel title = new JLabel("Sanctum  ·  Admin Dashboard");
        title.setFont(F_MONO_SM);
        title.setForeground(C_TEXT_MID);
        left.add(icon);
        left.add(title);

        // Center — User card
        JPanel center = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        center.setOpaque(false);
        center.add(buildUserInfoCard());

        // Right — Window controls
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        controls.setOpaque(false);
        
        JButton minimizeBtn = new JButton();
        minimizeBtn.setIcon(IconManager.getThemedIcon("settings", IconManager.SIZE_SMALL));
        minimizeBtn.setContentAreaFilled(false);
        minimizeBtn.setBorderPainted(false);
        minimizeBtn.setFocusPainted(false);
        minimizeBtn.setPreferredSize(new Dimension(24, 24));
        minimizeBtn.setToolTipText("Minimize");
        minimizeBtn.addActionListener(e -> setState(JFrame.ICONIFIED));
        
        JButton closeBtn = new JButton();
        closeBtn.setIcon(IconManager.getThemedIcon("error", IconManager.SIZE_SMALL));
        closeBtn.setContentAreaFilled(false);
        closeBtn.setBorderPainted(false);
        closeBtn.setFocusPainted(false);
        closeBtn.setPreferredSize(new Dimension(24, 24));
        closeBtn.setToolTipText("Close");
        closeBtn.addActionListener(e -> performLogout());
        
        controls.add(minimizeBtn);
        controls.add(closeBtn);

        bar.add(left,   BorderLayout.WEST);
        bar.add(center, BorderLayout.CENTER);
        bar.add(controls,  BorderLayout.EAST);
        return bar;
    }

    private JPanel buildUserInfoCard() {
        JPanel card = new JPanel(new BorderLayout(10, 0));
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(5, 15, 5, 15));

        // Fetch real user data
        Map<String, Object> userData = SanctumApiClient.getCurrentUserData();
        String firstName   = userData != null ? userData.getOrDefault("first_name", "Admin").toString() : "Admin";
        String lastName    = userData != null ? userData.getOrDefault("last_name", "").toString()     : "";
        String displayName = lastName.isEmpty() ? firstName : firstName + " " + lastName;
        String initials    = (firstName.isEmpty() ? "A" : String.valueOf(firstName.charAt(0)))
                           + (lastName.isEmpty()  ? ""  : String.valueOf(lastName.charAt(0)));

        // Avatar circle
        JPanel avatar = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(C_GOLD);
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        avatar.setPreferredSize(new Dimension(35, 35));
        avatar.setOpaque(false);

        JLabel initialsLabel = new JLabel(initials.toUpperCase());
        initialsLabel.setFont(F_LABEL);
        initialsLabel.setForeground(C_TEXT);
        initialsLabel.setHorizontalAlignment(SwingConstants.CENTER);
        initialsLabel.setVerticalAlignment(SwingConstants.CENTER);
        avatar.add(initialsLabel, BorderLayout.CENTER);

        // Name + role stack
        JPanel info = new JPanel();
        info.setOpaque(false);
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        
        JLabel name = new JLabel(displayName);
        name.setFont(F_LABEL);
        name.setForeground(C_TEXT);
        JLabel role = new JLabel("Administrator");
        role.setFont(F_MONO_SM);
        role.setForeground(C_TEXT_DIM);
        info.add(name);
        info.add(role);

        card.add(avatar, BorderLayout.WEST);
        card.add(info,   BorderLayout.CENTER);
        return card;
    }

    // ─── Sidebar ──────────────────────────────────────────────────────
    private JPanel buildSidebar() {
        JPanel side = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(C_SURFACE);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(C_BORDER);
                g2.fillRect(getWidth() - 1, 0, 1, getHeight());
                g2.dispose();
            }
        };
        side.setPreferredSize(new Dimension(200, 0));
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setBorder(BorderFactory.createEmptyBorder(16, 0, 16, 0));

        for (String[] item : MENU_ITEMS) {
            side.add(buildMenuItem(item[0], item[1]));
        }
        side.add(Box.createVerticalGlue());

        JLabel ver = new JLabel("  v2.4.1 build 9081");
        ver.setFont(F_MONO_SM);
        ver.setForeground(C_TEXT_MID);
        side.add(ver);
        return side;
    }
    
    private JPanel buildMenuItem(String icon, String text) {
        JPanel item = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(text.equals(activeMenu) ? C_GOLD : C_SURFACE);
                g2.fillRect(0, 0, getWidth(), getHeight());
                if (text.equals(activeMenu)) {
                    g2.setColor(C_GOLD.darker());
                    g2.fillRect(0, 0, 4, getHeight());
                }
                g2.dispose();
            }
        };
        item.setPreferredSize(new Dimension(200, 44));
        item.setMaximumSize(new Dimension(200, 44));
        item.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        item.setBorder(new EmptyBorder(0, 16, 0, 16));

        JLabel iconLabel = new JLabel(icon + "  ");
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
        iconLabel.setForeground(text.equals(activeMenu) ? C_BG : C_TEXT_MID);

        JLabel textLabel = new JLabel(text) {
            @Override public Color getForeground() {
                return text.equals(activeMenu) ? C_BG : C_TEXT_MID;
            }
        };
        textLabel.setFont(F_MONO_SM);

        item.add(iconLabel, BorderLayout.WEST);
        item.add(textLabel, BorderLayout.CENTER);

        item.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e)  { switchContent(text); }
            @Override public void mouseEntered(MouseEvent e)  { item.repaint(); }
            @Override public void mouseExited(MouseEvent e)   { item.repaint(); }
        });

        return item;
    }

    private void switchContent(String menu) {
        activeMenu = menu;
        String cardName = menu.toLowerCase();
        
        // Map "dashboard" to "overview" since that's the actual card name
        if (cardName.equals("dashboard")) {
            cardName = "overview";
        }
        
        if (menu.equals("Settings")) {
            // Use integrated settings page instead of separate dialog
            ((CardLayout) contentArea.getLayout()).show(contentArea, "settings");
        } else {
            ((CardLayout) contentArea.getLayout()).show(contentArea, cardName);
        }
        if (sidebar != null) sidebar.repaint();
    }

    // ─── Main Dashboard Page ──────────────────────────────────────────
    private JPanel buildMainDashboard() {
        JPanel main = new JPanel(new BorderLayout(20, 20));
        main.setOpaque(false);
        main.setBorder(new EmptyBorder(30, 30, 30, 30));

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JPanel logoBlock = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        logoBlock.setOpaque(false);
        JLabel logoLabel = new JLabel("Sanctum");
        logoLabel.setFont(new Font("Georgia", Font.BOLD, 20));
        logoLabel.setForeground(C_TEXT);
        JLabel roleLabel = new JLabel("ADMIN PORTAL");
        roleLabel.setFont(F_MONO_SM);
        roleLabel.setForeground(C_GOLD);
        logoBlock.add(logoLabel);
        logoBlock.add(Box.createHorizontalStrut(10));
        logoBlock.add(roleLabel);
        header.add(logoBlock, BorderLayout.WEST);

        // Main content area with overview cards
        JPanel contentPanel = new JPanel(new GridLayout(1, 2, 20, 20));
        contentPanel.setOpaque(false);
        
        // Announcements Overview Card
        JPanel announcementsCard = createOverviewCard(
            "📢 Announcements",
            "Recent announcements and updates",
            C_GOLD,
            () -> switchContent("announcements")
        );
        
        // Members Overview Card
        JPanel membersCard = createOverviewCard(
            "👥 Members",
            "Member management and overview",
            C_SUCCESS,
            () -> switchContent("members")
        );
        
        contentPanel.add(announcementsCard);
        contentPanel.add(membersCard);
        
        // Quick Actions Panel
        JPanel quickActionsPanel = createQuickActionsPanel();
        
        // Layout organization
        JPanel centerContent = new JPanel(new BorderLayout(0, 20));
        centerContent.setOpaque(false);
        centerContent.add(contentPanel, BorderLayout.CENTER);
        centerContent.add(quickActionsPanel, BorderLayout.SOUTH);
        
        JPanel north = new JPanel(new BorderLayout(0, 20));
        north.setOpaque(false);
        north.add(header, BorderLayout.NORTH);
        north.add(centerContent, BorderLayout.CENTER);

        main.add(north, BorderLayout.CENTER);
        
        // Load overview data
        loadOverviewData();
        
        return main;
    }

    // ─── Overview Cards and Quick Actions ───────────────────────────────────
    private JPanel createOverviewCard(String title, String description, Color accent, Runnable onClick) {
        JPanel card = new JPanel(new BorderLayout(0, 15)) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Gradient background
                GradientPaint gradient = new GradientPaint(
                    0, 0, C_CARD,
                    0, getHeight(), C_SURFACE
                );
                g2.setPaint(gradient);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                
                // Accent border
                g2.setColor(accent);
                g2.setStroke(new BasicStroke(2));
                g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 15, 15);
                
                // Left accent bar
                g2.setColor(accent);
                g2.fillRoundRect(0, 0, 6, getHeight(), 3, 3);
                
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(25, 25, 25, 25));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { onClick.run(); }
            @Override public void mouseEntered(MouseEvent e) { card.repaint(); }
            @Override public void mouseExited(MouseEvent e) { card.repaint(); }
        });
        
        // Header with icon and title
        JPanel headerPanel = new JPanel(new BorderLayout(10, 0));
        headerPanel.setOpaque(false);
        
        JLabel iconLabel = new JLabel(title.split(" ")[0]);
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 32));
        iconLabel.setForeground(accent);
        
        JLabel titleLabel = new JLabel(title.substring(title.indexOf(' ') + 1));
        titleLabel.setFont(F_HEADING);
        titleLabel.setForeground(C_TEXT);
        
        headerPanel.add(iconLabel, BorderLayout.WEST);
        headerPanel.add(titleLabel, BorderLayout.CENTER);
        
        // Description
        JLabel descLabel = new JLabel(description);
        descLabel.setFont(F_LABEL);
        descLabel.setForeground(C_TEXT_MID);
        descLabel.setBorder(new EmptyBorder(10, 0, 0, 0));
        
        // Arrow button
        JButton arrowBtn = new JButton("→");
        arrowBtn.setFont(new Font("Segoe UI", Font.BOLD, 16));
        arrowBtn.setForeground(accent);
        arrowBtn.setContentAreaFilled(false);
        arrowBtn.setBorderPainted(false);
        arrowBtn.setFocusPainted(false);
        arrowBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        arrowBtn.addActionListener(e -> onClick.run());
        
        JPanel arrowPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        arrowPanel.setOpaque(false);
        arrowPanel.add(arrowBtn);
        
        // Stats preview (will be populated by loadOverviewData)
        JPanel statsPanel = new JPanel(new GridLayout(2, 1, 0, 5));
        statsPanel.setOpaque(false);
        statsPanel.setBorder(new EmptyBorder(15, 0, 0, 0));
        
        JLabel countLabel = new JLabel("Loading...");
        countLabel.setFont(F_MONO_LG);
        countLabel.setForeground(C_TEXT);
        
        JLabel detailLabel = new JLabel("Fetching data...");
        detailLabel.setFont(F_MONO_SM);
        detailLabel.setForeground(C_TEXT_DIM);
        
        statsPanel.add(countLabel);
        statsPanel.add(detailLabel);
        
        card.add(headerPanel, BorderLayout.NORTH);
        card.add(descLabel, BorderLayout.CENTER);
        card.add(arrowPanel, BorderLayout.EAST);
        card.add(statsPanel, BorderLayout.SOUTH);
        
        // Store references for data updates
        if (title.contains("Announcements")) {
            card.putClientProperty("countLabel", countLabel);
            card.putClientProperty("detailLabel", detailLabel);
        } else if (title.contains("Members")) {
            card.putClientProperty("countLabel", countLabel);
            card.putClientProperty("detailLabel", detailLabel);
        }
        
        return card;
    }
    
    private JPanel createQuickActionsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(20, 0, 0, 0));
        
        JLabel titleLabel = new JLabel("Quick Actions");
        titleLabel.setFont(F_HEADING);
        titleLabel.setForeground(C_TEXT);
        
        JPanel actionsPanel = new JPanel(new GridLayout(1, 4, 15, 0));
        actionsPanel.setOpaque(false);
        
        // Add Announcement button
        JButton addAnnouncementBtn = createQuickActionButton(
            "📢 Add Announcement",
            C_GOLD,
            () -> showAddAnnouncementDialog()
        );
        
        // Add Staff button
        JButton addStaffBtn = createQuickActionButton(
            "👨‍💼 Add Staff",
            C_SUCCESS,
            () -> showAddStaffDialog()
        );
        
        // View All Announcements button
        JButton viewAnnouncementsBtn = createQuickActionButton(
            "📋 View All",
            C_GOLD_DIM,
            () -> switchContent("announcements")
        );
        
        // View All Members button
        JButton viewMembersBtn = createQuickActionButton(
            "👥 View All",
            C_TEXT_MID,
            () -> switchContent("members")
        );
        
        actionsPanel.add(addAnnouncementBtn);
        actionsPanel.add(addStaffBtn);
        actionsPanel.add(viewAnnouncementsBtn);
        actionsPanel.add(viewMembersBtn);
        
        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(actionsPanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JButton createQuickActionButton(String text, Color color, Runnable onClick) {
        JButton button = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                if (getModel().isRollover()) {
                    g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 50));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                }
                
                g2.setColor(color);
                g2.setStroke(new BasicStroke(1));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                
                g2.dispose();
            }
        };
        
        button.setFont(F_LABEL);
        button.setForeground(color);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setBorder(new EmptyBorder(10, 15, 10, 15));
        button.addActionListener(e -> onClick.run());
        
        return button;
    }
    
    private void loadOverviewData() {
        // Load announcements overview
        SanctumApiClient.getAnnouncements().thenAccept(announcements -> {
            SwingUtilities.invokeLater(() -> {
                int totalAnnouncements = announcements.size();
                int recentCount = (int) announcements.stream()
                    .filter(a -> {
                        // Filter for recent announcements (last 7 days)
                        try {
                            String dateStr = a.get("date_created").toString();
                            LocalDateTime date = LocalDateTime.parse(dateStr.replace("Z", ""));
                            return date.isAfter(LocalDateTime.now().minusDays(7));
                        } catch (Exception e) {
                            return false;
                        }
                    })
                    .count();
                
                // Update announcements card stats
                updateOverviewCardStats("📢 Announcements", totalAnnouncements, recentCount + " recent");
            });
        }).exceptionally(ex -> {
            SwingUtilities.invokeLater(() -> {
                updateOverviewCardStats("📢 Announcements", 0, "Error loading data");
            });
            return null;
        });
        
        // Load members overview
        SanctumApiClient.getMembers().thenAccept(members -> {
            SwingUtilities.invokeLater(() -> {
                int totalMembers = members.size();
                int activeCount = (int) members.stream()
                    .filter(m -> Boolean.TRUE.equals(m.get("is_active")))
                    .count();
                
                // Update members card stats
                updateOverviewCardStats("👥 Members", totalMembers, activeCount + " active");
            });
        }).exceptionally(ex -> {
            SwingUtilities.invokeLater(() -> {
                updateOverviewCardStats("👥 Members", 0, "Error loading data");
            });
            return null;
        });
    }
    
    private void updateOverviewCardStats(String cardIdentifier, int count, String detail) {
        // Find and update the appropriate overview card with safer navigation
        try {
            // Navigate to the overview cards more safely
            if (contentArea != null) {
                Component overviewComponent = null;
                
                // Try to find the main dashboard component
                for (Component comp : contentArea.getComponents()) {
                    if (comp instanceof JPanel) {
                        JPanel mainPanel = (JPanel) comp;
                        // Look for the center content panel
                        for (Component innerComp : mainPanel.getComponents()) {
                            if (innerComp instanceof JPanel) {
                                JPanel centerPanel = (JPanel) innerComp;
                                // Look for the content panel with overview cards
                                for (Component contentComp : centerPanel.getComponents()) {
                                    if (contentComp instanceof JPanel) {
                                        JPanel contentPanel = (JPanel) contentComp;
                                        // This should be our content panel with overview cards
                                        overviewComponent = contentPanel;
                                        break;
                                    }
                                }
                                if (overviewComponent != null) break;
                            }
                        }
                        if (overviewComponent != null) break;
                    }
                }
                
                if (overviewComponent instanceof JPanel) {
                    JPanel contentPanel = (JPanel) overviewComponent;
                    for (Component comp : contentPanel.getComponents()) {
                        if (comp instanceof JPanel) {
                            JPanel card = (JPanel) comp;
                            Object countLabelObj = card.getClientProperty("countLabel");
                            Object detailLabelObj = card.getClientProperty("detailLabel");
                            
                            if (countLabelObj instanceof JLabel && detailLabelObj instanceof JLabel) {
                                JLabel countLabel = (JLabel) countLabelObj;
                                JLabel detailLabel = (JLabel) detailLabelObj;
                                
                                // Check if this is the right card based on its current content
                                if (cardIdentifier.contains("Announcements") && countLabel.getText().contains("Loading")) {
                                    countLabel.setText(String.valueOf(count));
                                    detailLabel.setText(detail);
                                    card.repaint();
                                    break;
                                } else if (cardIdentifier.contains("Members") && !countLabel.getText().equals(String.valueOf(count))) {
                                    countLabel.setText(String.valueOf(count));
                                    detailLabel.setText(detail);
                                    card.repaint();
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error updating overview card stats: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ─── Sub-pages ────────────────────────────────────────────────────
    private JPanel buildMembersPage() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(30, 30, 30, 30));

        JLabel title = new JLabel("Members Management");
        title.setFont(F_TITLE);
        title.setForeground(C_TEXT);
        title.setBorder(new EmptyBorder(0, 0, 30, 0));

        // Members table with real data
        String[] columns = {"ID", "Name", "Email", "Phone", "Join Date", "Status"};
        DefaultTableModel model = new DefaultTableModel(new Object[][]{}, columns) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(model);
        table.setOpaque(false);
        table.getTableHeader().setOpaque(false);
        table.getTableHeader().setBackground(C_SURFACE);
        table.getTableHeader().setForeground(C_TEXT);
        table.getTableHeader().setFont(F_LABEL);
        table.setForeground(C_TEXT);
        table.setBackground(new Color(0, 0, 0, 0));
        table.setRowHeight(30);
        table.setSelectionBackground(C_GOLD_DIM);
        table.setSelectionForeground(C_TEXT);
        table.setGridColor(C_BORDER);
        
        // Custom renderer for better visibility
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (c instanceof JLabel) {
                    JLabel label = (JLabel) c;
                    label.setForeground(C_TEXT);
                    label.setOpaque(false);
                    if (isSelected) {
                        label.setBackground(C_GOLD_DIM);
                        label.setOpaque(true);
                    }
                }
                return c;
            }
        });

        JScrollPane scroll = new JScrollPane(table);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);

        panel.add(title, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        
        // Load real members data
        loadMembersData(model);
        
        return panel;
    }

    private JPanel buildAnnouncementsPage() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setName("announcements");
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(30, 30, 30, 30));

        // Header with add button
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        
        JLabel title = new JLabel("Announcements Management");
        title.setFont(F_TITLE);
        title.setForeground(C_TEXT);
        
        JButton addBtn = createStyledButton("+ Add Announcement", C_GOLD);
        addBtn.addActionListener(e -> showAddAnnouncementDialog());
        
        JPanel headerRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        headerRight.setOpaque(false);
        headerRight.add(addBtn);
        
        header.add(title, BorderLayout.WEST);
        header.add(headerRight, BorderLayout.EAST);
        header.setBorder(new EmptyBorder(0, 0, 20, 0));

        // Announcements grid panel with 3x3 pagination
        JPanel announcementsContainer = new JPanel(new BorderLayout());
        announcementsContainer.setOpaque(false);
        
        JPanel announcementsGrid = new JPanel(new GridLayout(3, 3, 20, 20)); // 3x3 grid
        announcementsGrid.setName("grid");
        announcementsGrid.setOpaque(false);
        announcementsContainer.add(announcementsGrid, BorderLayout.CENTER);
        
        // Pagination controls
        JPanel paginationPanel = createPaginationPanel(announcementsGrid, "announcements");
        announcementsContainer.add(paginationPanel, BorderLayout.SOUTH);
        
        JScrollPane scroll = new JScrollPane(announcementsContainer);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        panel.add(header, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        
        // Load real announcements data with pagination
        loadAnnouncementsGridData(announcementsGrid, paginationPanel);
        
        return panel;
    }

    private JPanel buildStaffPage() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setName("staff");
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(30, 30, 30, 30));

        // Header with add button
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        
        JLabel title = new JLabel("Staff Management");
        title.setFont(F_TITLE);
        title.setForeground(C_TEXT);
        
        JButton addBtn = createStyledButton("+ Add Staff", C_GOLD);
        addBtn.addActionListener(e -> showAddStaffDialog());
        
        JPanel headerRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        headerRight.setOpaque(false);
        headerRight.add(addBtn);
        
        header.add(title, BorderLayout.WEST);
        header.add(headerRight, BorderLayout.EAST);
        header.setBorder(new EmptyBorder(0, 0, 20, 0));

        // Staff grid panel with 3x3 pagination
        JPanel staffContainer = new JPanel(new BorderLayout());
        staffContainer.setOpaque(false);
        
        JPanel staffGrid = new JPanel(new GridLayout(3, 3, 20, 20)); // 3x3 grid
        staffGrid.setName("grid");
        staffGrid.setOpaque(false);
        staffContainer.add(staffGrid, BorderLayout.CENTER);
        
        // Pagination controls
        JPanel paginationPanel = createPaginationPanel(staffGrid, "staff");
        staffContainer.add(paginationPanel, BorderLayout.SOUTH);
        
        JScrollPane scroll = new JScrollPane(staffContainer);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        panel.add(header, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        
        // Load real staff data with pagination
        loadStaffGridData(staffGrid, paginationPanel);
        
        return panel;
    }

    private JPanel buildUsersPage() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(30, 30, 30, 30));

        JLabel title = new JLabel("User Accounts Management");
        title.setFont(F_TITLE);
        title.setForeground(C_TEXT);
        title.setBorder(new EmptyBorder(0, 0, 30, 0));

        // Users table with real data
        String[] columns = {"ID", "Username", "Role", "Email", "Last Login", "Status"};
        DefaultTableModel model = new DefaultTableModel(new Object[][]{}, columns) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(model);
        table.setOpaque(false);
        table.getTableHeader().setOpaque(false);
        table.getTableHeader().setBackground(C_SURFACE);
        table.getTableHeader().setForeground(C_TEXT);
        table.getTableHeader().setFont(F_LABEL);
        table.setForeground(C_TEXT);
        table.setBackground(new Color(0, 0, 0, 0));
        table.setRowHeight(30);
        table.setSelectionBackground(C_GOLD_DIM);
        table.setSelectionForeground(C_TEXT);
        table.setGridColor(C_BORDER);
        
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (c instanceof JLabel) {
                    JLabel label = (JLabel) c;
                    label.setForeground(C_TEXT);
                    label.setOpaque(false);
                    if (isSelected) {
                        label.setBackground(C_GOLD_DIM);
                        label.setOpaque(true);
                    }
                }
                return c;
            }
        });

        JScrollPane scroll = new JScrollPane(table);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);

        panel.add(title, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        
        // Load real users data
        loadUsersData(model);
        
        return panel;
    }

    // ─── Helpers ──────────────────────────────────────────────────────
    private JButton createStyledButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(F_LABEL);
        btn.setForeground(C_TEXT);
        btn.setBackground(bg);
        btn.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        btn.setFocusPainted(false);
        btn.setOpaque(true);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { btn.setBackground(bg.brighter()); }
            @Override public void mouseExited(MouseEvent e)  { btn.setBackground(bg); }
        });
        return btn;
    }

    private void performLogout() {
        int result = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to logout?",
            "Confirm Logout",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE
        );

        if (result == JOptionPane.YES_OPTION) {
            dispose();
            SwingUtilities.invokeLater(() -> {
                try {
                    new LoginFrame().setVisible(true);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(null,
                        "Error returning to login: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
                }
            });
        }
    }

    // ─── Icon Loading ─────────────────────────────────────────────────
    private void setApplicationIcon() {
        Image img = loadIconFromResources("/images/icon.png");
        if (img == null) img = loadIconFromResources("/images/icon.ico");
        if (img != null) setIconImage(img);
    }

    private Image loadIconFromResources(String path) {
        try (InputStream is = ChurchAdminFrame.class.getResourceAsStream(path)) {
            if (is == null) return null;
            BufferedImage bi = ImageIO.read(is);
            if (bi != null) System.out.println("Icon loaded: " + path);
            return bi;
        } catch (Exception e) {
            System.err.println("Icon load failed (" + path + "): " + e.getMessage());
            return null;
        }
    }

    // ─── Data Loading ───────────────────────────────────────────────────
    // Data is now loaded through loadOverviewData() method

    private void loadMembersData(DefaultTableModel model) {
        SanctumApiClient.getMembers().thenAccept(members -> {
            SwingUtilities.invokeLater(() -> {
                model.setRowCount(0);
                if (members.isEmpty()) {
                    model.addRow(new Object[]{"No data", "No members found", "", "", "", ""});
                } else {
                    for (Map<String, Object> member : members) {
                        Object[] row = {
                            member.getOrDefault("id", "N/A"),
                            member.getOrDefault("name", "Unknown"),
                            member.getOrDefault("email", "N/A"),
                            member.getOrDefault("phone", "N/A"),
                            member.getOrDefault("join_date", "N/A"),
                            member.getOrDefault("status", "Unknown")
                        };
                        model.addRow(row);
                    }
                }
            });
        }).exceptionally(ex -> {
            SwingUtilities.invokeLater(() -> {
                model.setRowCount(0);
                model.addRow(new Object[]{"Error", "Failed to load members", "", "", "", ""});
            });
            return null;
        });
    }

    private void loadAnnouncementsGridData(JPanel announcementsGrid, JPanel paginationPanel) {
        SanctumApiClient.getAnnouncements().thenAccept(announcements -> {
            SwingUtilities.invokeLater(() -> {
                // Store data in panel for pagination
                announcementsGrid.putClientProperty("allData", announcements);
                announcementsGrid.putClientProperty("currentPage", 0);
                announcementsGrid.putClientProperty("paginationPanel", paginationPanel);
                
                // Display first page
                displayAnnouncementsPage(announcementsGrid, announcements, 0);
                updatePaginationControls(paginationPanel, announcements.size(), 0, "announcements", announcementsGrid);
            });
        }).exceptionally(ex -> {
            SwingUtilities.invokeLater(() -> {
                announcementsGrid.removeAll();
                announcementsGrid.add(createErrorCard("Failed to load announcements"));
                announcementsGrid.revalidate();
                announcementsGrid.repaint();
            });
            return null;
        });
    }
    
    private void displayAnnouncementsPage(JPanel grid, List<Map<String, Object>> announcements, int page) {
        grid.removeAll();
        int itemsPerPage = 9; // 3x3 grid
        int start = page * itemsPerPage;
        int end = Math.min(start + itemsPerPage, announcements.size());
        
        if (announcements.isEmpty()) {
            grid.add(createNoDataCard("No announcements available", "Click 'Add Announcement' to create one"));
        } else {
            for (int i = start; i < end; i++) {
                Map<String, Object> announcement = announcements.get(i);
                String title = announcement.getOrDefault("title", "Untitled").toString();
                String content = announcement.getOrDefault("content", "No content available").toString();
                String date = announcement.getOrDefault("created_at", "").toString();
                if (date.length() > 10) date = date.substring(0, 10);
                
                grid.add(createCompactAnnouncementCard(title, content, date, announcement));
            }
            // Fill empty slots to maintain grid layout
            for (int i = end - start; i < itemsPerPage; i++) {
                JPanel emptyCard = new JPanel();
                emptyCard.setOpaque(false);
                grid.add(emptyCard);
            }
        }
        grid.revalidate();
        grid.repaint();
    }

    private void loadStaffGridData(JPanel staffGrid, JPanel paginationPanel) {
        SanctumApiClient.getStaff().thenAccept(staff -> {
            SwingUtilities.invokeLater(() -> {
                // Store data in panel for pagination
                staffGrid.putClientProperty("allData", staff);
                staffGrid.putClientProperty("currentPage", 0);
                staffGrid.putClientProperty("paginationPanel", paginationPanel);
                
                // Display first page
                displayStaffPage(staffGrid, staff, 0);
                updatePaginationControls(paginationPanel, staff.size(), 0, "staff", staffGrid);
            });
        }).exceptionally(ex -> {
            SwingUtilities.invokeLater(() -> {
                staffGrid.removeAll();
                staffGrid.add(createErrorCard("Failed to load staff"));
                staffGrid.revalidate();
                staffGrid.repaint();
            });
            return null;
        });
    }
    
    private void displayStaffPage(JPanel grid, List<Map<String, Object>> staff, int page) {
        grid.removeAll();
        int itemsPerPage = 9; // 3x3 grid
        int start = page * itemsPerPage;
        int end = Math.min(start + itemsPerPage, staff.size());
        
        if (staff.isEmpty()) {
            grid.add(createNoDataCard("No staff found", "Click 'Add Staff' to add team members"));
        } else {
            for (int i = start; i < end; i++) {
                Map<String, Object> staffMember = staff.get(i);
                String name = staffMember.getOrDefault("name", "Unknown").toString();
                String role = staffMember.getOrDefault("role", "Unknown").toString();
                String email = staffMember.getOrDefault("email", "N/A").toString();
                String phone = staffMember.getOrDefault("phone", "N/A").toString();
                String status = staffMember.getOrDefault("status", "Unknown").toString();
                
                grid.add(createCompactStaffCard(name, role, email, phone, status, staffMember));
            }
            // Fill empty slots to maintain grid layout
            for (int i = end - start; i < itemsPerPage; i++) {
                JPanel emptyCard = new JPanel();
                emptyCard.setOpaque(false);
                grid.add(emptyCard);
            }
        }
        grid.revalidate();
        grid.repaint();
    }

    // ─── Card Creation Methods ────────────────────────────────────────────
    private JPanel createAnnouncementCard(String title, String content, String date, Map<String, Object> announcementData) {
        JPanel card = new JPanel(new BorderLayout(0, 15));
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(20, 20, 20, 20));
        card.setPreferredSize(new Dimension(320, 280)); // Larger, more attractive size
        
        // Header with image placeholder
        JPanel headerPanel = new JPanel(new BorderLayout(10, 8));
        headerPanel.setOpaque(false);
        
        // Icon placeholder using IconManager with larger size
        JLabel iconLabel = new JLabel();
        iconLabel.setIcon(IconManager.getThemedIcon("announcement", IconManager.SIZE_LARGE));
        iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
        iconLabel.setVerticalAlignment(SwingConstants.CENTER);
        iconLabel.setPreferredSize(new Dimension(80, 80));
        
        // Title and date panel
        JPanel textPanel = new JPanel(new BorderLayout(0, 4));
        textPanel.setOpaque(false);
        
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(F_HEADING); // Larger font for title
        titleLabel.setForeground(C_TEXT);
        titleLabel.setBorder(new EmptyBorder(5, 0, 0, 0));
        
        JLabel dateLabel = new JLabel(date);
        dateLabel.setFont(F_MONO_SM);
        dateLabel.setForeground(C_GOLD); // Gold color for date
        
        textPanel.add(titleLabel, BorderLayout.NORTH);
        textPanel.add(dateLabel, BorderLayout.SOUTH);
        
        headerPanel.add(iconLabel, BorderLayout.WEST);
        headerPanel.add(textPanel, BorderLayout.CENTER);
        
        // Content preview with better formatting
        JLabel contentLabel = new JLabel();
        contentLabel.setFont(F_LABEL);
        contentLabel.setForeground(C_TEXT_MID);
        String preview = content.length() > 120 ? content.substring(0, 120) + "..." : content;
        contentLabel.setText("<html><body style='width: 240px; line-height: 1.4'>" + preview + "</body></html>");
        contentLabel.setBorder(new EmptyBorder(10, 0, 10, 0));
        
        // View more button with better styling
        JButton viewMoreBtn = createStyledButton("View Details", C_GOLD);
        viewMoreBtn.setIcon(IconManager.getThemedIcon("info", IconManager.SIZE_SMALL));
        viewMoreBtn.setHorizontalTextPosition(SwingConstants.LEFT);
        viewMoreBtn.addActionListener(e -> showAnnouncementDetails(announcementData));
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(new EmptyBorder(10, 0, 0, 0));
        buttonPanel.add(viewMoreBtn);
        
        card.add(headerPanel, BorderLayout.NORTH);
        card.add(contentLabel, BorderLayout.CENTER);
        card.add(buttonPanel, BorderLayout.SOUTH);
        
        // Enhanced card background with gradient effect
        card = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Gradient background
                GradientPaint gradient = new GradientPaint(
                    0, 0, C_CARD,
                    0, getHeight(), C_SURFACE
                );
                g2.setPaint(gradient);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                
                // Gold accent border
                g2.setColor(C_GOLD);
                g2.setStroke(new BasicStroke(2));
                g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 15, 15);
                
                // Left accent bar
                g2.setColor(C_GOLD);
                g2.fillRoundRect(0, 0, 6, getHeight(), 3, 3);
                
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(15, 15, 15, 15));
        card.add(headerPanel, BorderLayout.NORTH);
        card.add(contentLabel, BorderLayout.CENTER);
        card.add(buttonPanel, BorderLayout.SOUTH);
        
        return card;
    }
    
    private JPanel createStaffCard(String name, String role, String email, String phone, String status, Map<String, Object> staffData) {
        JPanel card = new JPanel(new BorderLayout(0, 10));
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(15, 15, 15, 15));
        
        // Header with avatar
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        
        // Avatar using IconManager
        JLabel avatarLabel = new JLabel();
        avatarLabel.setIcon(IconManager.getThemedIcon("staff", IconManager.SIZE_MEDIUM));
        avatarLabel.setHorizontalAlignment(SwingConstants.CENTER);
        avatarLabel.setVerticalAlignment(SwingConstants.CENTER);
        avatarLabel.setPreferredSize(new Dimension(60, 60));
        avatarLabel.setOpaque(false);
        
        // Add circular background
        JPanel avatarPanel = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(status.equals("Active") ? C_SUCCESS : C_TEXT_DIM);
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        avatarPanel.setOpaque(false);
        avatarPanel.setPreferredSize(new Dimension(60, 60));
        avatarPanel.add(avatarLabel, BorderLayout.CENTER);
        
        JLabel nameLabel = new JLabel(name);
        nameLabel.setFont(F_LABEL);
        nameLabel.setForeground(C_TEXT);
        
        JLabel roleLabel = new JLabel(role);
        roleLabel.setFont(F_MONO_SM);
        roleLabel.setForeground(C_GOLD);
        
        JPanel textPanel = new JPanel(new BorderLayout(0, 2));
        textPanel.setOpaque(false);
        textPanel.add(nameLabel, BorderLayout.NORTH);
        textPanel.add(roleLabel, BorderLayout.SOUTH);
        
        headerPanel.add(avatarPanel, BorderLayout.WEST);
        headerPanel.add(textPanel, BorderLayout.CENTER);
        
        // Contact info
        JPanel contactPanel = new JPanel(new GridLayout(2, 1, 0, 5));
        contactPanel.setOpaque(false);
        
        JLabel emailLabel = new JLabel();
        emailLabel.setIcon(IconManager.getThemedIcon("email", IconManager.SIZE_SMALL));
        emailLabel.setText(" " + email);
        emailLabel.setFont(F_MONO_SM);
        emailLabel.setForeground(C_TEXT_MID);
        emailLabel.setHorizontalTextPosition(SwingConstants.RIGHT);
        
        JLabel phoneLabel = new JLabel();
        phoneLabel.setIcon(IconManager.getThemedIcon("phone", IconManager.SIZE_SMALL));
        phoneLabel.setText(" " + phone);
        phoneLabel.setFont(F_MONO_SM);
        phoneLabel.setForeground(C_TEXT_MID);
        phoneLabel.setHorizontalTextPosition(SwingConstants.RIGHT);
        
        contactPanel.add(emailLabel);
        contactPanel.add(phoneLabel);
        
        // View more button
        JButton viewMoreBtn = createStyledButton("View Details", C_GOLD);
        viewMoreBtn.addActionListener(e -> showStaffDetails(staffData));
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.add(viewMoreBtn);
        
        card.add(headerPanel, BorderLayout.NORTH);
        card.add(contactPanel, BorderLayout.CENTER);
        card.add(buttonPanel, BorderLayout.SOUTH);
        
        // Card background
        card = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(C_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(status.equals("Active") ? C_SUCCESS : C_TEXT_DIM);
                g2.fillRoundRect(0, 0, 4, getHeight(), 4, 4);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(10, 10, 10, 10));
        card.add(headerPanel, BorderLayout.NORTH);
        card.add(contactPanel, BorderLayout.CENTER);
        card.add(buttonPanel, BorderLayout.SOUTH);
        
        return card;
    }
    
    private JPanel createNoDataCard(String title, String message) {
        JPanel card = new JPanel(new BorderLayout(0, 10));
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(F_LABEL);
        titleLabel.setForeground(C_TEXT_DIM);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        JLabel messageLabel = new JLabel(message);
        messageLabel.setFont(F_MONO_SM);
        messageLabel.setForeground(C_TEXT_DIM);
        messageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        card.add(titleLabel, BorderLayout.NORTH);
        card.add(messageLabel, BorderLayout.CENTER);
        
        // Card background
        card = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(C_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(C_BORDER);
                g2.fillRoundRect(0, 0, 4, getHeight(), 4, 4);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(10, 10, 10, 10));
        card.add(titleLabel, BorderLayout.NORTH);
        card.add(messageLabel, BorderLayout.CENTER);
        
        return card;
    }
    
    private JPanel createErrorCard(String message) {
        JPanel card = new JPanel(new BorderLayout());
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        JLabel errorLabel = new JLabel("❌ " + message);
        errorLabel.setFont(F_LABEL);
        errorLabel.setForeground(C_DANGER);
        errorLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        card.add(errorLabel, BorderLayout.CENTER);
        
        // Card background
        card = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(C_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(C_DANGER);
                g2.fillRoundRect(0, 0, 4, getHeight(), 4, 4);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(10, 10, 10, 10));
        card.add(errorLabel, BorderLayout.CENTER);
        
        return card;
    }
    
    // ─── Pagination Methods ─────────────────────────────────────────────────
    private JPanel createPaginationPanel(JPanel targetGrid, String type) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        panel.setOpaque(false);
        panel.putClientProperty("targetGrid", targetGrid);
        panel.putClientProperty("type", type);
        
        JButton prevBtn = createStyledButton("◀ Previous", C_TEXT_DIM);
        prevBtn.putClientProperty("action", "prev");
        prevBtn.setEnabled(false);
        
        JLabel pageLabel = new JLabel("Page 1 of 1");
        pageLabel.setFont(F_LABEL);
        pageLabel.setForeground(C_TEXT);
        pageLabel.putClientProperty("pageLabel", true);
        
        JButton nextBtn = createStyledButton("Next ▶", C_TEXT_DIM);
        nextBtn.putClientProperty("action", "next");
        nextBtn.setEnabled(false);
        
        prevBtn.addActionListener(e -> navigatePage(targetGrid, -1, type));
        nextBtn.addActionListener(e -> navigatePage(targetGrid, 1, type));
        
        panel.add(prevBtn);
        panel.add(pageLabel);
        panel.add(nextBtn);
        
        return panel;
    }
    
    @SuppressWarnings("unchecked")
    private void navigatePage(JPanel grid, int direction, String type) {
        List<Map<String, Object>> allData = (List<Map<String, Object>>) grid.getClientProperty("allData");
        Integer currentPage = (Integer) grid.getClientProperty("currentPage");
        JPanel paginationPanel = (JPanel) grid.getClientProperty("paginationPanel");
        
        if (allData == null || currentPage == null) return;
        
        int itemsPerPage = 9;
        int totalPages = (int) Math.ceil(allData.size() / (double) itemsPerPage);
        int newPage = currentPage + direction;
        
        if (newPage >= 0 && newPage < totalPages) {
            grid.putClientProperty("currentPage", newPage);
            
            if ("announcements".equals(type)) {
                displayAnnouncementsPage(grid, allData, newPage);
            } else if ("staff".equals(type)) {
                displayStaffPage(grid, allData, newPage);
            }
            
            updatePaginationControls(paginationPanel, allData.size(), newPage, type, grid);
        }
    }
    
    private void updatePaginationControls(JPanel panel, int totalItems, int currentPage, String type, JPanel grid) {
        int itemsPerPage = 9;
        int totalPages = (int) Math.ceil(totalItems / (double) itemsPerPage);
        
        for (Component comp : panel.getComponents()) {
            if (comp instanceof JButton) {
                JButton btn = (JButton) comp;
                String action = (String) btn.getClientProperty("action");
                if ("prev".equals(action)) {
                    btn.setEnabled(currentPage > 0);
                } else if ("next".equals(action)) {
                    btn.setEnabled(currentPage < totalPages - 1);
                }
            } else if (comp instanceof JLabel) {
                JLabel label = (JLabel) comp;
                Object pageLabelProp = label.getClientProperty("pageLabel");
                if (pageLabelProp != null) {
                    if (totalPages == 0) {
                        label.setText("No items");
                    } else {
                        label.setText("Page " + (currentPage + 1) + " of " + totalPages);
                    }
                }
            }
        }
    }
    
    // ─── Compact Card Creation Methods ─────────────────────────────────────
    private JPanel createCompactAnnouncementCard(String title, String content, String date, Map<String, Object> announcementData) {
        JPanel card = new JPanel(new BorderLayout(0, 8));
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(15, 15, 15, 15));
        card.setPreferredSize(new Dimension(220, 200)); // Increased height for button
        
        // Header with icon and title
        JPanel headerPanel = new JPanel(new BorderLayout(8, 0));
        headerPanel.setOpaque(false);
        
        JLabel iconLabel = new JLabel(IconManager.getThemedIcon("announcement", IconManager.SIZE_SMALL));
        iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        JLabel titleLabel = new JLabel(truncateText(title, 20));
        titleLabel.setFont(F_LABEL);
        titleLabel.setForeground(C_TEXT);
        
        headerPanel.add(iconLabel, BorderLayout.WEST);
        headerPanel.add(titleLabel, BorderLayout.CENTER);
        
        // Content
        String truncatedContent = truncateText(content, 60);
        JLabel contentLabel = new JLabel("<html><body style='width: 180px;'><p style='color: #9CA3AF; font-size: 11px;'>" + 
            truncatedContent + "</p></body></html>");
        contentLabel.setFont(F_MONO_SM);
        contentLabel.setVerticalAlignment(SwingConstants.TOP);
        
        // Footer with date and view details button
        JPanel footerPanel = new JPanel(new BorderLayout());
        footerPanel.setOpaque(false);
        JLabel dateLabel = new JLabel("📅 " + date);
        dateLabel.setFont(F_MONO_SM);
        dateLabel.setForeground(C_TEXT_DIM);
        
        JButton viewDetailsBtn = new JButton("View Details");
        viewDetailsBtn.setFont(F_MONO_SM);
        viewDetailsBtn.setForeground(C_GOLD);
        viewDetailsBtn.setBackground(new Color(0, 0, 0, 0));
        viewDetailsBtn.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        viewDetailsBtn.setFocusPainted(false);
        viewDetailsBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        viewDetailsBtn.addActionListener(e -> showAnnouncementDetailsDialog(announcementData));
        
        footerPanel.add(dateLabel, BorderLayout.WEST);
        footerPanel.add(viewDetailsBtn, BorderLayout.EAST);
        
        card.add(headerPanel, BorderLayout.NORTH);
        card.add(contentLabel, BorderLayout.CENTER);
        card.add(footerPanel, BorderLayout.SOUTH);
        
        // Card background with hover effect
        card = new JPanel(new BorderLayout(0, 8)) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Gradient background
                GradientPaint gradient = new GradientPaint(
                    0, 0, C_CARD,
                    0, getHeight(), new Color(C_CARD.getRed(), C_CARD.getGreen(), C_CARD.getBlue(), 200)
                );
                g2.setPaint(gradient);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                
                // Accent border
                g2.setColor(C_GOLD);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 10, 10);
                
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(15, 15, 15, 15));
        card.add(headerPanel, BorderLayout.NORTH);
        card.add(contentLabel, BorderLayout.CENTER);
        card.add(footerPanel, BorderLayout.SOUTH);
        
        return card;
    }
    
    private JPanel createCompactStaffCard(String name, String role, String email, String phone, String status, Map<String, Object> staffData) {
        JPanel card = new JPanel(new BorderLayout(0, 8));
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(15, 15, 15, 15));
        card.setPreferredSize(new Dimension(220, 200)); // Increased height for button
        
        // Header with icon and name
        JPanel headerPanel = new JPanel(new BorderLayout(8, 0));
        headerPanel.setOpaque(false);
        
        JLabel iconLabel = new JLabel(IconManager.getThemedIcon("staff", IconManager.SIZE_SMALL));
        iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        JPanel namePanel = new JPanel(new GridLayout(2, 1, 0, 2));
        namePanel.setOpaque(false);
        
        JLabel nameLabel = new JLabel(truncateText(name, 18));
        nameLabel.setFont(F_LABEL);
        nameLabel.setForeground(C_TEXT);
        
        JLabel roleLabel = new JLabel("💼 " + truncateText(role, 15));
        roleLabel.setFont(F_MONO_SM);
        roleLabel.setForeground(C_GOLD);
        
        namePanel.add(nameLabel);
        namePanel.add(roleLabel);
        
        headerPanel.add(iconLabel, BorderLayout.WEST);
        headerPanel.add(namePanel, BorderLayout.CENTER);
        
        // Contact info
        JPanel contactPanel = new JPanel(new GridLayout(2, 1, 0, 4));
        contactPanel.setOpaque(false);
        
        JLabel emailLabel = new JLabel("📧 " + truncateText(email, 22));
        emailLabel.setFont(F_MONO_SM);
        emailLabel.setForeground(C_TEXT_DIM);
        
        JLabel phoneLabel = new JLabel("📱 " + truncateText(phone, 18));
        phoneLabel.setFont(F_MONO_SM);
        phoneLabel.setForeground(C_TEXT_DIM);
        
        contactPanel.add(emailLabel);
        contactPanel.add(phoneLabel);
        
        // Footer with status and view details button
        JPanel footerPanel = new JPanel(new BorderLayout());
        footerPanel.setOpaque(false);
        
        String statusEmoji = "active".equalsIgnoreCase(status) ? "🟢" : "⚪";
        JLabel statusLabel = new JLabel(statusEmoji + " " + status);
        statusLabel.setFont(F_MONO_SM);
        statusLabel.setForeground("active".equalsIgnoreCase(status) ? C_SUCCESS : C_TEXT_DIM);
        
        JButton viewDetailsBtn = new JButton("View Details");
        viewDetailsBtn.setFont(F_MONO_SM);
        viewDetailsBtn.setForeground(C_GOLD);
        viewDetailsBtn.setBackground(new Color(0, 0, 0, 0));
        viewDetailsBtn.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        viewDetailsBtn.setFocusPainted(false);
        viewDetailsBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        viewDetailsBtn.addActionListener(e -> showStaffDetailsDialog(staffData));
        
        footerPanel.add(statusLabel, BorderLayout.WEST);
        footerPanel.add(viewDetailsBtn, BorderLayout.EAST);
        
        card.add(headerPanel, BorderLayout.NORTH);
        card.add(contactPanel, BorderLayout.CENTER);
        card.add(footerPanel, BorderLayout.SOUTH);
        
        // Card background
        card = new JPanel(new BorderLayout(0, 8)) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Gradient background
                GradientPaint gradient = new GradientPaint(
                    0, 0, C_CARD,
                    0, getHeight(), new Color(C_CARD.getRed(), C_CARD.getGreen(), C_CARD.getBlue(), 200)
                );
                g2.setPaint(gradient);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                
                // Accent border based on status
                Color accentColor = "active".equalsIgnoreCase(status) ? C_SUCCESS : C_GOLD;
                g2.setColor(accentColor);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 10, 10);
                
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(15, 15, 15, 15));
        card.add(headerPanel, BorderLayout.NORTH);
        card.add(contactPanel, BorderLayout.CENTER);
        card.add(footerPanel, BorderLayout.SOUTH);
        
        return card;
    }
    
    private String truncateText(String text, int maxLength) {
        if (text == null || text.isEmpty()) return "N/A";
        return text.length() > maxLength ? text.substring(0, maxLength - 3) + "..." : text;
    }
    
    // ─── View Details Dialog Methods ──────────────────────────────────────
    private void showAnnouncementDetailsDialog(Map<String, Object> announcementData) {
        JDialog dialog = new JDialog(this, "Announcement Details", true);
        dialog.setSize(450, 350);
        dialog.setLocationRelativeTo(this);
        dialog.getContentPane().setBackground(C_BG);
        
        JPanel panel = new JPanel(new BorderLayout(20, 20));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(25, 25, 25, 25));
        
        // Title
        String title = announcementData.getOrDefault("title", "Untitled").toString();
        JLabel titleLabel = new JLabel("📢 " + title);
        titleLabel.setFont(F_TITLE);
        titleLabel.setForeground(C_TEXT);
        
        // Content
        String content = announcementData.getOrDefault("content", "No content available").toString();
        JTextArea contentArea = new JTextArea(content);
        contentArea.setFont(F_BODY);
        contentArea.setForeground(C_TEXT);
        contentArea.setBackground(C_CARD);
        contentArea.setLineWrap(true);
        contentArea.setWrapStyleWord(true);
        contentArea.setEditable(false);
        contentArea.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        // Metadata
        String date = announcementData.getOrDefault("created_at", "").toString();
        String priority = announcementData.getOrDefault("priority", "normal").toString();
        String author = announcementData.getOrDefault("author", "Unknown").toString();
        
        JPanel metaPanel = new JPanel(new GridLayout(3, 1, 5, 5));
        metaPanel.setOpaque(false);
        metaPanel.add(createDetailLabel("📅 Date:", date));
        metaPanel.add(createDetailLabel("🔔 Priority:", priority));
        metaPanel.add(createDetailLabel("👤 Author:", author));
        
        // Close button
        JButton closeBtn = createStyledButton("Close", C_GOLD);
        closeBtn.addActionListener(e -> dialog.dispose());
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setOpaque(false);
        buttonPanel.add(closeBtn);
        
        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(contentArea, BorderLayout.CENTER);
        panel.add(metaPanel, BorderLayout.SOUTH);
        
        dialog.add(panel);
        dialog.setVisible(true);
    }
    
    private void showStaffDetailsDialog(Map<String, Object> staffData) {
        JDialog dialog = new JDialog(this, "Staff Member Details", true);
        dialog.setSize(400, 400);
        dialog.setLocationRelativeTo(this);
        dialog.getContentPane().setBackground(C_BG);
        
        JPanel panel = new JPanel(new BorderLayout(20, 20));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(25, 25, 25, 25));
        
        // Header with name and role
        String name = staffData.getOrDefault("name", "Unknown").toString();
        String role = staffData.getOrDefault("role", "Unknown").toString();
        
        JPanel headerPanel = new JPanel(new GridLayout(2, 1, 5, 5));
        headerPanel.setOpaque(false);
        
        JLabel nameLabel = new JLabel("👤 " + name);
        nameLabel.setFont(F_TITLE);
        nameLabel.setForeground(C_TEXT);
        
        JLabel roleLabel = new JLabel("💼 " + role);
        roleLabel.setFont(F_LABEL);
        roleLabel.setForeground(C_GOLD);
        
        headerPanel.add(nameLabel);
        headerPanel.add(roleLabel);
        
        // Details
        String email = staffData.getOrDefault("email", "N/A").toString();
        String phone = staffData.getOrDefault("phone", "N/A").toString();
        String status = staffData.getOrDefault("status", "Unknown").toString();
        String department = staffData.getOrDefault("department", "N/A").toString();
        String joinDate = staffData.getOrDefault("date_joined", "").toString();
        if (joinDate.length() > 10) joinDate = joinDate.substring(0, 10);
        
        JPanel detailsPanel = new JPanel(new GridLayout(5, 1, 8, 8));
        detailsPanel.setOpaque(false);
        detailsPanel.add(createDetailLabel("📧 Email:", email));
        detailsPanel.add(createDetailLabel("📱 Phone:", phone));
        detailsPanel.add(createDetailLabel("🟢 Status:", status));
        detailsPanel.add(createDetailLabel("🏢 Department:", department));
        detailsPanel.add(createDetailLabel("📅 Join Date:", joinDate));
        
        // Close button
        JButton closeBtn = createStyledButton("Close", C_GOLD);
        closeBtn.addActionListener(e -> dialog.dispose());
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setOpaque(false);
        buttonPanel.add(closeBtn);
        
        panel.add(headerPanel, BorderLayout.NORTH);
        panel.add(detailsPanel, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        dialog.add(panel);
        dialog.setVisible(true);
    }
    
    private JLabel createDetailLabel(String label, String value) {
        JLabel lbl = new JLabel(label + " " + value);
        lbl.setFont(F_LABEL);
        lbl.setForeground(C_TEXT);
        return lbl;
    }
    
    // ─── Concurrent Data Syncing ──────────────────────────────────────────
    private void refreshAnnouncementsData() {
        // Find announcements grid and pagination panel
        Component[] components = contentArea.getComponents();
        for (Component comp : components) {
            if (comp instanceof JPanel) {
                JPanel page = (JPanel) comp;
                if ("announcements".equals(page.getName())) {
                    // Found announcements page, refresh it
                    SwingUtilities.invokeLater(() -> {
                        for (Component child : page.getComponents()) {
                            if (child instanceof JScrollPane) {
                                JScrollPane scroll = (JScrollPane) child;
                                Component viewport = scroll.getViewport().getView();
                                if (viewport instanceof JPanel) {
                                    JPanel container = (JPanel) viewport;
                                    for (Component containerChild : container.getComponents()) {
                                        if (containerChild instanceof JPanel && "grid".equals(containerChild.getName())) {
                                            JPanel grid = (JPanel) containerChild;
                                            JPanel paginationPanel = (JPanel) grid.getClientProperty("paginationPanel");
                                            loadAnnouncementsGridData(grid, paginationPanel);
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    });
                    break;
                }
            }
        }
    }
    
    private void refreshStaffData() {
        // Find staff grid and pagination panel
        Component[] components = contentArea.getComponents();
        for (Component comp : components) {
            if (comp instanceof JPanel) {
                JPanel page = (JPanel) comp;
                if ("staff".equals(page.getName())) {
                    // Found staff page, refresh it
                    SwingUtilities.invokeLater(() -> {
                        for (Component child : page.getComponents()) {
                            if (child instanceof JScrollPane) {
                                JScrollPane scroll = (JScrollPane) child;
                                Component viewport = scroll.getViewport().getView();
                                if (viewport instanceof JPanel) {
                                    JPanel container = (JPanel) viewport;
                                    for (Component containerChild : container.getComponents()) {
                                        if (containerChild instanceof JPanel && "grid".equals(containerChild.getName())) {
                                            JPanel grid = (JPanel) containerChild;
                                            JPanel paginationPanel = (JPanel) grid.getClientProperty("paginationPanel");
                                            loadStaffGridData(grid, paginationPanel);
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    });
                    break;
                }
            }
        }
    }
    
    // ─── Dialog Methods ───────────────────────────────────────────────────
    private void showAddAnnouncementDialog() {
        JDialog dialog = new JDialog(this, "Add Announcement", true);
        dialog.setSize(500, 400);
        dialog.setLocationRelativeTo(this);
        dialog.getContentPane().setBackground(C_BG);
        
        JPanel panel = new JPanel(new BorderLayout(20, 20));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        JLabel title = new JLabel();
        title.setIcon(IconManager.getThemedIcon("announcement", IconManager.SIZE_LARGE));
        title.setText(" Create New Announcement");
        title.setFont(F_TITLE);
        title.setForeground(C_TEXT);
        title.setHorizontalTextPosition(SwingConstants.RIGHT);
        
        JPanel inputPanel = new JPanel(new GridLayout(3, 2, 10, 10));
        inputPanel.setOpaque(false);
        
        inputPanel.add(createLabel("Title:"));
        JTextField titleField = new JTextField();
        titleField.setFont(F_LABEL);
        titleField.setForeground(C_TEXT);
        titleField.setBackground(C_CARD);
        titleField.setCaretColor(C_GOLD);
        inputPanel.add(titleField);
        
        inputPanel.add(createLabel("Content:"));
        JTextArea contentArea = new JTextArea(5, 20);
        contentArea.setFont(F_LABEL);
        contentArea.setForeground(C_TEXT);
        contentArea.setBackground(C_CARD);
        contentArea.setCaretColor(C_GOLD);
        contentArea.setLineWrap(true);
        contentArea.setWrapStyleWord(true);
        JScrollPane contentScroll = new JScrollPane(contentArea);
        contentScroll.setOpaque(false);
        contentScroll.getViewport().setOpaque(false);
        inputPanel.add(contentScroll);
        
        inputPanel.add(createLabel("Priority:"));
        JComboBox<String> priorityCombo = new JComboBox<>(new String[]{"low", "medium", "high"});
        priorityCombo.setFont(F_LABEL);
        priorityCombo.setForeground(C_TEXT);
        priorityCombo.setBackground(C_CARD);
        inputPanel.add(priorityCombo);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setOpaque(false);
        
        JButton saveBtn = createStyledButton("Save Announcement", C_GOLD);
        saveBtn.setIcon(IconManager.getThemedIcon("save", IconManager.SIZE_SMALL));
        saveBtn.setHorizontalTextPosition(SwingConstants.RIGHT);
        JButton cancelBtn = createStyledButton("Cancel", C_DANGER);
        cancelBtn.setIcon(IconManager.getThemedIcon("error", IconManager.SIZE_SMALL));
        cancelBtn.setHorizontalTextPosition(SwingConstants.RIGHT);
        
        saveBtn.addActionListener(e -> {
            String titleText = titleField.getText().trim();
            String contentText = contentArea.getText().trim();
            String priority = priorityCombo.getSelectedItem().toString();
            
            if (!titleText.isEmpty() && !contentText.isEmpty()) {
                // Show loading state
                saveBtn.setEnabled(false);
                saveBtn.setIcon(IconManager.getThemedIcon("loading", IconManager.SIZE_SMALL));
                saveBtn.setText("Saving...");
                
                // Call backend API to add announcement
                System.out.println("Creating announcement with priority: '" + priority + "'"); // Debug line
                SanctumApiClient.createAnnouncement(titleText, contentText, priority)
                    .thenAccept(success -> {
                        SwingUtilities.invokeLater(() -> {
                            if (success) {
                                JOptionPane.showMessageDialog(dialog, "📢 Announcement saved successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                                dialog.dispose();
                                loadOverviewData(); // Refresh dashboard data
                                refreshAnnouncementsData(); // Refresh announcements page concurrently
                            } else {
                                JOptionPane.showMessageDialog(dialog, "Failed to save announcement. Please try again.", "Error", JOptionPane.ERROR_MESSAGE);
                                saveBtn.setEnabled(true);
                                saveBtn.setIcon(IconManager.getThemedIcon("save", IconManager.SIZE_SMALL));
                                saveBtn.setText("Save Announcement");
                            }
                        });
                    })
                    .exceptionally(ex -> {
                        SwingUtilities.invokeLater(() -> {
                            JOptionPane.showMessageDialog(dialog, "Error saving announcement: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                            saveBtn.setEnabled(true);
                            saveBtn.setIcon(IconManager.getThemedIcon("save", IconManager.SIZE_SMALL));
                            saveBtn.setText("Save Announcement");
                        });
                        return null;
                    });
            } else {
                JOptionPane.showMessageDialog(dialog, "Please fill in all required fields", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        
        cancelBtn.addActionListener(e -> dialog.dispose());
        
        buttonPanel.add(saveBtn);
        buttonPanel.add(cancelBtn);
        
        panel.add(title, BorderLayout.NORTH);
        panel.add(inputPanel, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        dialog.add(panel);
        dialog.setVisible(true);
    }
    
    private void showAddStaffDialog() {
        JDialog dialog = new JDialog(this, "Add Staff Member", true);
        dialog.setSize(500, 450);
        dialog.setLocationRelativeTo(this);
        dialog.getContentPane().setBackground(C_BG);
        
        JPanel panel = new JPanel(new BorderLayout(20, 20));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        JLabel title = new JLabel();
        title.setIcon(IconManager.getThemedIcon("staff", IconManager.SIZE_LARGE));
        title.setText(" Add New Staff Member");
        title.setFont(F_TITLE);
        title.setForeground(C_TEXT);
        title.setHorizontalTextPosition(SwingConstants.RIGHT);
        
        JPanel inputPanel = new JPanel(new GridLayout(5, 2, 10, 10));
        inputPanel.setOpaque(false);
        
        inputPanel.add(createLabel("Name:"));
        JTextField nameField = new JTextField();
        nameField.setFont(F_LABEL);
        nameField.setForeground(C_TEXT);
        nameField.setBackground(C_CARD);
        nameField.setCaretColor(C_GOLD);
        inputPanel.add(nameField);
        
        inputPanel.add(createLabel("Role:"));
        JComboBox<String> roleCombo = new JComboBox<>();
        roleCombo.setFont(F_LABEL);
        roleCombo.setForeground(C_TEXT);
        roleCombo.setBackground(C_CARD);
        
        // Load roles from backend
        loadStaffRoles(roleCombo);
        inputPanel.add(roleCombo);
        
        inputPanel.add(createLabel("Email:"));
        JTextField emailField = new JTextField();
        emailField.setFont(F_LABEL);
        emailField.setForeground(C_TEXT);
        emailField.setBackground(C_CARD);
        emailField.setCaretColor(C_GOLD);
        inputPanel.add(emailField);
        
        inputPanel.add(createLabel("Phone:"));
        JTextField phoneField = new JTextField();
        phoneField.setFont(F_LABEL);
        phoneField.setForeground(C_TEXT);
        phoneField.setBackground(C_CARD);
        phoneField.setCaretColor(C_GOLD);
        inputPanel.add(phoneField);
        
        inputPanel.add(createLabel("Department:"));
        JComboBox<String> deptCombo = new JComboBox<>(new String[]{"Pastoral", "Administration", "Music", "Youth", "Outreach", "Facilities"});
        deptCombo.setFont(F_LABEL);
        deptCombo.setForeground(C_TEXT);
        deptCombo.setBackground(C_CARD);
        inputPanel.add(deptCombo);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setOpaque(false);
        
        JButton saveBtn = createStyledButton("Add Staff", C_GOLD);
        saveBtn.setIcon(IconManager.getThemedIcon("staff", IconManager.SIZE_SMALL));
        saveBtn.setHorizontalTextPosition(SwingConstants.RIGHT);
        JButton cancelBtn = createStyledButton("Cancel", C_DANGER);
        cancelBtn.setIcon(IconManager.getThemedIcon("error", IconManager.SIZE_SMALL));
        cancelBtn.setHorizontalTextPosition(SwingConstants.RIGHT);
        
        saveBtn.addActionListener(e -> {
            String name = nameField.getText().trim();
            String role = roleCombo.getSelectedItem().toString();
            String email = emailField.getText().trim();
            String phone = phoneField.getText().trim();
            String department = deptCombo.getSelectedItem().toString();
            
            if (!name.isEmpty() && !role.isEmpty() && !email.isEmpty()) {
                // Show loading state
                saveBtn.setEnabled(false);
                saveBtn.setIcon(IconManager.getThemedIcon("loading", IconManager.SIZE_SMALL));
                saveBtn.setText("Adding...");
                
                // Call backend API to add staff
                SanctumApiClient.createStaff(name, email, phone, role, department, java.time.LocalDate.now().toString())
                    .thenAccept(success -> {
                        SwingUtilities.invokeLater(() -> {
                            if (success) {
                                JOptionPane.showMessageDialog(dialog, "👨‍💼 Staff member added successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                                dialog.dispose();
                                loadOverviewData(); // Refresh dashboard data
                                refreshStaffData(); // Refresh staff page concurrently
                            } else {
                                JOptionPane.showMessageDialog(dialog, "Failed to add staff member. Please try again.", "Error", JOptionPane.ERROR_MESSAGE);
                                saveBtn.setEnabled(true);
                                saveBtn.setIcon(IconManager.getThemedIcon("staff", IconManager.SIZE_SMALL));
                                saveBtn.setText("Add Staff");
                            }
                        });
                    })
                    .exceptionally(ex -> {
                        SwingUtilities.invokeLater(() -> {
                            JOptionPane.showMessageDialog(dialog, "Error adding staff member: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                            saveBtn.setEnabled(true);
                            saveBtn.setIcon(IconManager.getThemedIcon("staff", IconManager.SIZE_SMALL));
                            saveBtn.setText("Add Staff");
                        });
                        return null;
                    });
            } else {
                JOptionPane.showMessageDialog(dialog, "Please fill in all required fields (Name, Role, Email)", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        
        cancelBtn.addActionListener(e -> dialog.dispose());
        
        buttonPanel.add(saveBtn);
        buttonPanel.add(cancelBtn);
        
        panel.add(title, BorderLayout.NORTH);
        panel.add(inputPanel, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        dialog.add(panel);
        dialog.setVisible(true);
    }
    
    private void showAnnouncementDetails(Map<String, Object> announcementData) {
        JDialog dialog = new JDialog(this, "Announcement Details", true);
        dialog.setSize(600, 500);
        dialog.setLocationRelativeTo(this);
        dialog.getContentPane().setBackground(C_BG);
        
        JPanel panel = new JPanel(new BorderLayout(20, 20));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        JLabel title = new JLabel("📢 " + announcementData.getOrDefault("title", "Untitled").toString());
        title.setFont(F_TITLE);
        title.setForeground(C_TEXT);
        
        JTextArea contentArea = new JTextArea(announcementData.getOrDefault("content", "No content available").toString());
        contentArea.setFont(F_LABEL);
        contentArea.setForeground(C_TEXT);
        contentArea.setBackground(C_CARD);
        contentArea.setCaretColor(C_GOLD);
        contentArea.setLineWrap(true);
        contentArea.setWrapStyleWord(true);
        contentArea.setEditable(false);
        JScrollPane contentScroll = new JScrollPane(contentArea);
        contentScroll.setOpaque(false);
        contentScroll.getViewport().setOpaque(false);
        
        JPanel infoPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        infoPanel.setOpaque(false);
        
        infoPanel.add(createLabel("Created:"));
        JLabel createdLabel = new JLabel(announcementData.getOrDefault("created_at", "Unknown").toString());
        createdLabel.setFont(F_MONO_SM);
        createdLabel.setForeground(C_TEXT_MID);
        infoPanel.add(createdLabel);
        
        infoPanel.add(createLabel("Priority:"));
        JLabel priorityLabel = new JLabel(announcementData.getOrDefault("priority", "Normal").toString());
        priorityLabel.setFont(F_MONO_SM);
        priorityLabel.setForeground(C_GOLD);
        infoPanel.add(priorityLabel);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setOpaque(false);
        
        JButton closeBtn = createStyledButton("❌ Close", C_DANGER);
        closeBtn.addActionListener(e -> dialog.dispose());
        
        buttonPanel.add(closeBtn);
        
        panel.add(title, BorderLayout.NORTH);
        panel.add(contentScroll, BorderLayout.CENTER);
        panel.add(infoPanel, BorderLayout.SOUTH);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        dialog.add(panel);
        dialog.setVisible(true);
    }
    
    private void showStaffDetails(Map<String, Object> staffData) {
        JDialog dialog = new JDialog(this, "Staff Details", true);
        dialog.setSize(500, 400);
        dialog.setLocationRelativeTo(this);
        dialog.getContentPane().setBackground(C_BG);
        
        JPanel panel = new JPanel(new BorderLayout(20, 20));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        JLabel title = new JLabel("👨‍💼 " + staffData.getOrDefault("name", "Unknown").toString());
        title.setFont(F_TITLE);
        title.setForeground(C_TEXT);
        
        JPanel infoPanel = new JPanel(new GridLayout(6, 2, 10, 10));
        infoPanel.setOpaque(false);
        
        infoPanel.add(createLabel("Role:"));
        JLabel roleLabel = new JLabel(staffData.getOrDefault("role", "Unknown").toString());
        roleLabel.setFont(F_LABEL);
        roleLabel.setForeground(C_GOLD);
        infoPanel.add(roleLabel);
        
        infoPanel.add(createLabel("Email:"));
        JLabel emailLabel = new JLabel(staffData.getOrDefault("email", "N/A").toString());
        emailLabel.setFont(F_MONO_SM);
        emailLabel.setForeground(C_TEXT_MID);
        infoPanel.add(emailLabel);
        
        infoPanel.add(createLabel("Phone:"));
        JLabel phoneLabel = new JLabel(staffData.getOrDefault("phone", "N/A").toString());
        phoneLabel.setFont(F_MONO_SM);
        phoneLabel.setForeground(C_TEXT_MID);
        infoPanel.add(phoneLabel);
        
        infoPanel.add(createLabel("Department:"));
        JLabel deptLabel = new JLabel(staffData.getOrDefault("department", "Unknown").toString());
        deptLabel.setFont(F_MONO_SM);
        deptLabel.setForeground(C_TEXT_MID);
        infoPanel.add(deptLabel);
        
        infoPanel.add(createLabel("Status:"));
        JLabel statusLabel = new JLabel(staffData.getOrDefault("status", "Unknown").toString());
        statusLabel.setFont(F_MONO_SM);
        statusLabel.setForeground(staffData.getOrDefault("status", "Unknown").toString().equals("Active") ? C_SUCCESS : C_TEXT_DIM);
        infoPanel.add(statusLabel);
        
        infoPanel.add(createLabel("Joined:"));
        JLabel joinedLabel = new JLabel(staffData.getOrDefault("created_at", "Unknown").toString());
        joinedLabel.setFont(F_MONO_SM);
        joinedLabel.setForeground(C_TEXT_DIM);
        infoPanel.add(joinedLabel);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setOpaque(false);
        
        JButton closeBtn = createStyledButton("❌ Close", C_DANGER);
        closeBtn.addActionListener(e -> dialog.dispose());
        
        buttonPanel.add(closeBtn);
        
        panel.add(title, BorderLayout.NORTH);
        panel.add(infoPanel, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        dialog.add(panel);
        dialog.setVisible(true);
    }
    
    // ─── Settings Panel Integration ───────────────────────────────────────
    private JPanel buildSettingsPage() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setName("settings");
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(30, 30, 30, 30));

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        
        JLabel title = new JLabel();
        title.setIcon(IconManager.getThemedIcon("settings", IconManager.SIZE_LARGE));
        title.setText(" Settings");
        title.setFont(F_TITLE);
        title.setForeground(C_TEXT);
        title.setHorizontalTextPosition(SwingConstants.RIGHT);
        
        header.add(title, BorderLayout.WEST);
        header.setBorder(new EmptyBorder(0, 0, 20, 0));

        // Settings content - will be populated with real data
        JPanel settingsContent = new JPanel(new GridLayout(0, 2, 20, 20));
        settingsContent.setOpaque(false);
        settingsContent.setName("settingsContent");
        
        // Loading placeholder
        JLabel loadingLabel = new JLabel("Loading settings...", SwingConstants.CENTER);
        loadingLabel.setFont(F_LABEL);
        loadingLabel.setForeground(C_TEXT_DIM);
        settingsContent.add(loadingLabel);
        
        JScrollPane scroll = new JScrollPane(settingsContent);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);
        
        panel.add(header, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        
        // Load real settings data from backend
        loadSettingsData(settingsContent);
        
        return panel;
    }
    
    private void loadSettingsData(JPanel settingsContent) {
        // Load church details from backend
        SanctumApiClient.getChurchDetails().thenAccept(churchData -> {
            SwingUtilities.invokeLater(() -> {
                settingsContent.removeAll();
                
                // Get current user info from session
                com.sanctum.auth.SessionManager sessionMgr = com.sanctum.auth.SessionManager.getInstance();
                String currentUser = sessionMgr.getCurrentUser();
                String userRole = sessionMgr.getUserRole();
                
                // General Settings Card with real church data
                String churchName = churchData.getOrDefault("name", "N/A").toString();
                String churchCode = churchData.getOrDefault("church_code", "N/A").toString();
                String city = churchData.getOrDefault("city", "N/A").toString();
                String county = churchData.getOrDefault("county", "N/A").toString();
                String churchType = churchData.getOrDefault("church_type_display", "N/A").toString();
                String status = churchData.getOrDefault("status_display", "N/A").toString();
                
                JPanel generalCard = createSettingsCard(
                    "Church Information",
                    "⛪",
                    new String[]{
                        "Name: " + churchName,
                        "Code: " + churchCode,
                        "Type: " + churchType,
                        "Location: " + city + ", " + county,
                        "Status: " + status
                    }
                );
                
                // User Settings Card with real user data
                String userName = currentUser != null ? currentUser : "N/A";
                String userRoleDisplay = userRole != null ? userRole : "N/A";
                
                JPanel userCard = createSettingsCard(
                    "User Profile",
                    "👤",
                    new String[]{
                        "Username: " + userName,
                        "Role: " + userRoleDisplay,
                        "Church: " + churchName,
                        "Session: Active"
                    }
                );
                
                // System Settings Card
                JPanel systemCard = createSettingsCard(
                    "System Information",
                    "💻",
                    new String[]{
                        "Version: 1.0.0",
                        "Database: Connected",
                        "API Status: Active",
                        "Last Sync: " + java.time.LocalDateTime.now().toLocalDate()
                    }
                );
                
                // Contact Settings Card with church contact info
                String phone = churchData.getOrDefault("phone", "N/A").toString();
                String email = churchData.getOrDefault("email", "N/A").toString();
                String website = churchData.getOrDefault("website", "N/A").toString();
                
                JPanel contactCard = createSettingsCard(
                    "Contact Information",
                    "📞",
                    new String[]{
                        "Phone: " + phone,
                        "Email: " + email,
                        "Website: " + website,
                        "Address: " + city + ", " + county
                    }
                );
                
                // Branding/Theme Settings Card with logo and colors
                JPanel brandingCard = createBrandingSettingsCard(churchData);
                
                settingsContent.add(generalCard);
                settingsContent.add(userCard);
                settingsContent.add(systemCard);
                settingsContent.add(contactCard);
                settingsContent.add(brandingCard);
                
                settingsContent.revalidate();
                settingsContent.repaint();
            });
        }).exceptionally(ex -> {
            SwingUtilities.invokeLater(() -> {
                settingsContent.removeAll();
                JLabel errorLabel = new JLabel("Failed to load settings: " + ex.getMessage(), SwingConstants.CENTER);
                errorLabel.setFont(F_LABEL);
                errorLabel.setForeground(C_DANGER);
                settingsContent.add(errorLabel);
                settingsContent.revalidate();
                settingsContent.repaint();
            });
            return null;
        });
    }
    
    private JPanel createSettingsCard(String title, String icon, String[] items) {
        JPanel card = new JPanel(new BorderLayout(0, 15));
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        // Header
        JPanel headerPanel = new JPanel(new BorderLayout(10, 0));
        headerPanel.setOpaque(false);
        
        JLabel iconLabel = new JLabel(icon);
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 24));
        iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
        iconLabel.setPreferredSize(new Dimension(40, 40));
        
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(F_HEADING);
        titleLabel.setForeground(C_TEXT);
        
        headerPanel.add(iconLabel, BorderLayout.WEST);
        headerPanel.add(titleLabel, BorderLayout.CENTER);
        
        // Content
        JPanel contentPanel = new JPanel(new GridLayout(items.length, 1, 0, 8));
        contentPanel.setOpaque(false);
        
        for (String item : items) {
            JLabel itemLabel = new JLabel(item);
            itemLabel.setFont(F_LABEL);
            itemLabel.setForeground(C_TEXT_MID);
            itemLabel.setBorder(new EmptyBorder(5, 10, 5, 10));
            contentPanel.add(itemLabel);
        }
        
        // Card background
        card = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Gradient background
                GradientPaint gradient = new GradientPaint(
                    0, 0, C_CARD,
                    0, getHeight(), C_SURFACE
                );
                g2.setPaint(gradient);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                
                // Border
                g2.setColor(C_BORDER);
                g2.setStroke(new BasicStroke(1));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(10, 10, 10, 10));
        card.add(headerPanel, BorderLayout.NORTH);
        card.add(contentPanel, BorderLayout.CENTER);
        
        return card;
    }
    
    private JPanel createBrandingSettingsCard(Map<String, Object> churchData) {
        // Get church ID for API calls
        final int churchId = churchData.get("id") instanceof Number 
            ? ((Number) churchData.get("id")).intValue() 
            : 0;
        
        // Get current theme colors from church data
        final String currentPrimaryColor = churchData.getOrDefault("primary_color", "#3B82F6").toString();
        final String currentSecondaryColor = churchData.getOrDefault("secondary_color", "#10B981").toString();
        final String currentAccentColor = churchData.getOrDefault("accent_color", "#F59E0B").toString();
        
        JPanel card = new JPanel(new BorderLayout(0, 15));
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        // Header
        JPanel headerPanel = new JPanel(new BorderLayout(10, 0));
        headerPanel.setOpaque(false);
        
        JLabel iconLabel = new JLabel("🎨");
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 24));
        iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
        iconLabel.setPreferredSize(new Dimension(40, 40));
        
        JLabel titleLabel = new JLabel("Church Branding");
        titleLabel.setFont(F_HEADING);
        titleLabel.setForeground(C_TEXT);
        
        headerPanel.add(iconLabel, BorderLayout.WEST);
        headerPanel.add(titleLabel, BorderLayout.CENTER);
        
        // Content panel
        JPanel contentPanel = new JPanel(new GridLayout(0, 1, 0, 12));
        contentPanel.setOpaque(false);
        
        // Logo display/upload section
        String logoUrl = churchData.getOrDefault("logo", "").toString();
        if (logoUrl.isEmpty() || logoUrl.equals("null")) {
            logoUrl = churchData.getOrDefault("logo_url", "").toString();
        }
        
        JPanel logoPanel = new JPanel(new BorderLayout(10, 0));
        logoPanel.setOpaque(false);
        
        JLabel logoPreview = new JLabel("No Logo", SwingConstants.CENTER);
        logoPreview.setFont(F_MONO_SM);
        logoPreview.setForeground(C_TEXT_DIM);
        logoPreview.setPreferredSize(new Dimension(80, 80));
        logoPreview.setBorder(BorderFactory.createLineBorder(C_BORDER, 2));
        
        // Load logo if available
        final String finalLogoUrl = logoUrl;
        if (!logoUrl.isEmpty() && !logoUrl.equals("null")) {
            try {
                ImageIcon logoIcon = new ImageIcon(new java.net.URL(logoUrl));
                Image scaled = logoIcon.getImage().getScaledInstance(80, 80, Image.SCALE_SMOOTH);
                logoPreview.setIcon(new ImageIcon(scaled));
                logoPreview.setText("");
            } catch (Exception e) {
                System.err.println("Failed to load logo: " + e.getMessage());
            }
        }
        
        JButton uploadBtn = createStyledButton("Upload Logo", C_GOLD);
        uploadBtn.setFont(F_MONO_SM);
        uploadBtn.addActionListener(e -> showLogoUploadDialog(churchId, logoPreview));
        
        logoPanel.add(logoPreview, BorderLayout.WEST);
        logoPanel.add(uploadBtn, BorderLayout.CENTER);
        contentPanel.add(logoPanel);
        
        // Theme color selection
        JLabel colorLabel = new JLabel("Primary Theme Color:");
        colorLabel.setFont(F_LABEL);
        colorLabel.setForeground(C_TEXT_MID);
        contentPanel.add(colorLabel);
        
        JPanel colorPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        colorPanel.setOpaque(false);
        
        // Color presets with their hex values
        String[][] colorPresets = {
            {"#3B82F6", "Blue"},
            {"#10B981", "Emerald"},
            {"#F59E0B", "Amber"},
            {"#EF4444", "Red"},
            {"#8B5CF6", "Purple"},
            {"#EC4899", "Pink"},
            {"#6366F1", "Indigo"},
            {"#14B8A6", "Teal"}
        };
        
        // Track selected color button
        final JButton[] selectedColorBtn = {null};
        
        for (String[] preset : colorPresets) {
            final String hexColor = preset[0];
            final String name = preset[1];
            final Color color = Color.decode(hexColor);
            
            JButton colorBtn = new JButton();
            colorBtn.setPreferredSize(new Dimension(40, 40));
            colorBtn.setBackground(color);
            colorBtn.setBorder(BorderFactory.createLineBorder(
                currentPrimaryColor.equalsIgnoreCase(hexColor) ? C_GOLD : C_BORDER, 
                currentPrimaryColor.equalsIgnoreCase(hexColor) ? 3 : 2
            ));
            colorBtn.setFocusPainted(false);
            colorBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            colorBtn.setToolTipText(name + " (" + hexColor + ")");
            
            colorBtn.addActionListener(e -> {
                // Update selection visual
                if (selectedColorBtn[0] != null) {
                    selectedColorBtn[0].setBorder(BorderFactory.createLineBorder(C_BORDER, 2));
                }
                colorBtn.setBorder(BorderFactory.createLineBorder(C_GOLD, 3));
                selectedColorBtn[0] = colorBtn;
                
                // Save to backend
                if (churchId > 0) {
                    SanctumApiClient.updateChurchBranding(churchId, hexColor, null, null)
                        .thenAccept(response -> {
                            SwingUtilities.invokeLater(() -> {
                                if (response.containsKey("success") && (Boolean) response.get("success")) {
                                    JOptionPane.showMessageDialog(this, 
                                        "✅ Theme color updated to " + name + "!",
                                        "Theme Updated", JOptionPane.INFORMATION_MESSAGE);
                                } else {
                                    JOptionPane.showMessageDialog(this, 
                                        "❌ Failed to update theme color.\n" + response.getOrDefault("error", "Unknown error"),
                                        "Error", JOptionPane.ERROR_MESSAGE);
                                }
                            });
                        }).exceptionally(ex -> {
                            SwingUtilities.invokeLater(() -> {
                                JOptionPane.showMessageDialog(this, 
                                    "❌ Error updating theme: " + ex.getMessage(),
                                    "Error", JOptionPane.ERROR_MESSAGE);
                            });
                            return null;
                        });
                }
            });
            
            colorPanel.add(colorBtn);
        }
        
        contentPanel.add(colorPanel);
        
        // Current theme info
        JLabel currentTheme = new JLabel("Current Primary: " + currentPrimaryColor);
        currentTheme.setFont(F_MONO_SM);
        currentTheme.setForeground(C_GOLD);
        contentPanel.add(currentTheme);
        
        // Card background with gradient
        card = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                GradientPaint gradient = new GradientPaint(
                    0, 0, C_CARD,
                    0, getHeight(), C_SURFACE
                );
                g2.setPaint(gradient);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                
                g2.setColor(C_BORDER);
                g2.setStroke(new BasicStroke(1));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(10, 10, 10, 10));
        card.add(headerPanel, BorderLayout.NORTH);
        card.add(contentPanel, BorderLayout.CENTER);
        
        return card;
    }
    
    private void showLogoUploadDialog(int churchId, JLabel logoPreview) {
        if (churchId <= 0) {
            JOptionPane.showMessageDialog(this,
                "❌ Church ID not available. Cannot upload logo.",
                "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select Church Logo");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
            "Image files (PNG, JPG, GIF)", "png", "jpg", "jpeg", "gif"));
        
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            java.io.File selectedFile = fileChooser.getSelectedFile();
            
            // Show confirmation
            int confirm = JOptionPane.showConfirmDialog(this,
                "Upload logo: " + selectedFile.getName() + "?",
                "Confirm Upload", JOptionPane.YES_NO_OPTION);
            
            if (confirm == JOptionPane.YES_OPTION) {
                // Upload to backend with church ID
                SanctumApiClient.uploadChurchLogo(churchId, selectedFile)
                    .thenAccept(response -> {
                        SwingUtilities.invokeLater(() -> {
                            if (response.containsKey("success") && (Boolean) response.get("success")) {
                                JOptionPane.showMessageDialog(this,
                                    "✅ Logo uploaded successfully!",
                                    "Success", JOptionPane.INFORMATION_MESSAGE);
                                // Refresh settings to show new logo
                                refreshSettingsData();
                            } else {
                                JOptionPane.showMessageDialog(this,
                                    "❌ Failed to upload logo:\n" + response.getOrDefault("error", "Unknown error"),
                                    "Error", JOptionPane.ERROR_MESSAGE);
                            }
                        });
                    }).exceptionally(ex -> {
                        SwingUtilities.invokeLater(() -> {
                            JOptionPane.showMessageDialog(this,
                                "❌ Error uploading logo: " + ex.getMessage(),
                                "Error", JOptionPane.ERROR_MESSAGE);
                        });
                        return null;
                    });
            }
        }
    }
    
    private void refreshSettingsData() {
        // Find settings content panel and reload
        Component[] components = contentArea.getComponents();
        for (Component comp : components) {
            if (comp instanceof JPanel && "settings".equals(((JPanel) comp).getName())) {
                JPanel settingsPanel = (JPanel) comp;
                for (Component child : settingsPanel.getComponents()) {
                    if (child instanceof JScrollPane) {
                        JScrollPane scroll = (JScrollPane) child;
                        Component viewport = scroll.getViewport().getView();
                        if (viewport instanceof JPanel && "settingsContent".equals(viewport.getName())) {
                            loadSettingsData((JPanel) viewport);
                            break;
                        }
                    }
                }
                break;
            }
        }
    }
    
    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(F_LABEL);
        label.setForeground(C_TEXT_MID);
        return label;
    }
    
    private void loadStaffRoles(JComboBox<String> roleCombo) {
        // Roles that are properly mapped in the backend API
        String[] roles = {
            "Pastor",
            "Treasurer", 
            "Usher",
            "Music Director",
            "Youth Leader",
            "Admin",
            "Secretary"
        };
        
        for (String role : roles) {
            roleCombo.addItem(role);
        }
        
        // Set default selection
        roleCombo.setSelectedIndex(0);
    }

    private void loadStaffData(DefaultTableModel model) {
        SanctumApiClient.getStaff().thenAccept(staff -> {
            SwingUtilities.invokeLater(() -> {
                model.setRowCount(0);
                if (staff.isEmpty()) {
                    model.addRow(new Object[]{"No data", "No staff found", "", "", "", ""});
                } else {
                    for (Map<String, Object> staffMember : staff) {
                        Object[] row = {
                            staffMember.getOrDefault("id", "N/A"),
                            staffMember.getOrDefault("name", "Unknown"),
                            staffMember.getOrDefault("role", "Unknown"),
                            staffMember.getOrDefault("email", "N/A"),
                            staffMember.getOrDefault("phone", "N/A"),
                            staffMember.getOrDefault("status", "Unknown")
                        };
                        model.addRow(row);
                    }
                }
            });
        }).exceptionally(ex -> {
            SwingUtilities.invokeLater(() -> {
                model.setRowCount(0);
                model.addRow(new Object[]{"Error", "Failed to load staff", "", "", "", ""});
            });
            return null;
        });
    }

    private void loadUsersData(DefaultTableModel model) {
        // Use staff data as a temporary fallback for users
        SanctumApiClient.getStaff().thenAccept(staff -> {
            SwingUtilities.invokeLater(() -> {
                model.setRowCount(0);
                if (staff.isEmpty()) {
                    model.addRow(new Object[]{"No data", "No users found", "", "", "", ""});
                } else {
                    for (Map<String, Object> user : staff) {
                        Object[] row = {
                            user.getOrDefault("id", "N/A"),
                            user.getOrDefault("name", "Unknown").toString().toLowerCase().replace(" ", ""),
                            user.getOrDefault("role", "Unknown"),
                            user.getOrDefault("email", "N/A"),
                            user.getOrDefault("created_at", "Never"),
                            user.getOrDefault("status", "Unknown")
                        };
                        model.addRow(row);
                    }
                }
            });
        }).exceptionally(ex -> {
            SwingUtilities.invokeLater(() -> {
                model.setRowCount(0);
                model.addRow(new Object[]{"Error", "Failed to load users", "", "", "", ""});
            });
            return null;
        });
    }


    // ─── Entry Point ──────────────────────────────────────────────────
    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        SwingUtilities.invokeLater(() -> new ChurchAdminFrame().setVisible(true));
    }
}
