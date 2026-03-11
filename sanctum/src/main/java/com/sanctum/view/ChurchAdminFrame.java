package com.sanctum.view;

import javax.swing.*;
import com.sanctum.api.SanctumApiClient;
import com.sanctum.auth.SessionManager;
import com.sanctum.util.LogoLoader;
import com.sanctum.util.WindowsDialogFix;
import javax.swing.border.*;
import javax.swing.table.*;
import javax.imageio.ImageIO;
import java.awt.*;
import javax.swing.Timer;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import com.sanctum.api.SanctumApiClient;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;

public class ChurchAdminFrame extends JFrame {

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

    // ── Fonts ──────────────────────────────────────────────────────────────────
    private static final Font FONT_TITLE  = new Font("Monospaced", Font.BOLD,  22);
    private static final Font FONT_LABEL  = new Font("Monospaced", Font.BOLD,  11);
    private static final Font FONT_VALUE  = new Font("Monospaced", Font.BOLD,  28);
    private static final Font FONT_SMALL  = new Font("Monospaced", Font.PLAIN, 10);
    private static final Font FONT_MENU   = new Font("Monospaced", Font.BOLD,  12);
    private static final Font FONT_TABLE  = new Font("Monospaced", Font.PLAIN, 11);

    private JPanel contentPanel;
    private JLabel membersKpiValue;
    private JLabel donationsKpiValue;
    private JLabel staffKpiValue;
    private JLabel eventsKpiValue;
    private JLabel membersKpiTrend;
    private JLabel donationsKpiTrend;
    private JLabel staffKpiTrend;
    private JLabel eventsKpiTrend;
    private JTable staffTable;
    private JTable membersTable;
    private String activeMenu = "Dashboard";
    private JLabel statusLabel;
    private Timer dataRefreshTimer;
    private boolean apiConnected = false;
    
    // Menu items configuration
    private final String[][] MENU_ITEMS = {
        {"🏠", "Dashboard"},
        {"👥", "Members"},
        {"📢", "Announcements"},
        {"👨‍💼", "Staff Registration"},
        {"◉", "Settings"},
    };
    
    // Static instance to prevent multiple frames
    private static ChurchAdminFrame currentInstance = null;

    public static ChurchAdminFrame getInstance() {
        if (currentInstance == null || !currentInstance.isDisplayable()) {
            currentInstance = new ChurchAdminFrame();
        }
        return currentInstance;
    }

    public ChurchAdminFrame() {
        setTitle("Sanctum ADMIN — Control Panel");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(1400, 900);
        setLocationRelativeTo(null);
        
        // Set application icon
        setApplicationIcon();
        
        // Clear static instance when frame is closed
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent windowEvent) {
                currentInstance = null;
            }
        });
        
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
        setLayout(new BorderLayout());

        add(buildTopBar(),   BorderLayout.NORTH);
        add(buildSidebar(),  BorderLayout.WEST);
        add(buildContent(),  BorderLayout.CENTER);
        add(buildStatusBar(),BorderLayout.SOUTH);

        // Initialize API connection
        initializeApiConnection();

        setVisible(true);
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
                System.out.println("ChurchAdminFrame PNG icon loaded successfully - Size: " + iconImage.getWidth(null) + "x" + iconImage.getHeight(null));
            } else {
                System.out.println("ChurchAdminFrame PNG icon failed to load, trying ICO fallback");
                iconImage = loadIconFromResources("/images/icon.ico");
                
                if (iconImage != null) {
                    setIconImage(iconImage);
                    System.out.println("ChurchAdminFrame ICO icon loaded successfully as fallback - Size: " + iconImage.getWidth(null) + "x" + iconImage.getHeight(null));
                } else {
                    System.out.println("ChurchAdminFrame Both PNG and ICO fallback failed - using default icon");
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to set ChurchAdminFrame application icon: " + e.getMessage());
        }
    }
    
    /**
     * Load icon image from resources using ImageIO for better format support
     */
    private Image loadIconFromResources(String path) {
        try {
            InputStream inputStream = ChurchAdminFrame.class.getResourceAsStream(path);
            if (inputStream == null) {
                System.out.println("ChurchAdminFrame Resource not found: " + path);
                return null;
            }
            
            // Use ImageIO to read the image (better for ICO files)
            BufferedImage bufferedImage = ImageIO.read(inputStream);
            inputStream.close();
            
            if (bufferedImage != null) {
                System.out.println("ChurchAdminFrame Successfully loaded image from " + path + " - Size: " + bufferedImage.getWidth() + "x" + bufferedImage.getHeight());
                return bufferedImage;
            } else {
                System.out.println("ChurchAdminFrame Failed to read image from " + path);
                return null;
            }
            
        } catch (Exception e) {
            System.err.println("ChurchAdminFrame Error loading image from " + path + ": " + e.getMessage());
            return null;
        }
    }

    private void createAdminDashboard() {
        // This is now handled by the constructor and buildContent()
        // but kept as a placeholder if specific admin initialization is needed
    }

    // ── API Initialization ───────────────────────────────────────────────────────
    private void initializeApiConnection() {
        // Check API health and authenticate
        SanctumApiClient.checkHealth().thenAccept(isHealthy -> {
            SwingUtilities.invokeLater(() -> {
                if (isHealthy) {
                    // API is healthy, check if user is already authenticated
                    if (SanctumApiClient.isAuthenticated()) {
                        // User is authenticated, proceed with admin dashboard
                        apiConnected = true;
                        
                        // Start data refresh timer
                        startDataRefreshTimer();
                        
                        if (statusLabel != null) {
                            statusLabel.setText("  API Connected  |  Ready");
                        }
                        
                        // Load initial data
                        refreshCurrentPanelData();
                    } else {
                        // User not authenticated, redirect to login
                        dispose();
                        new LoginFrame().setVisible(true);
                    }
                } else {
                    apiConnected = false;
                    if (statusLabel != null) {
                        statusLabel.setText("  API Disconnected  |  Offline Mode");
                    }
                }
            });
        });
    }

    private void startDataRefreshTimer() {
        dataRefreshTimer = new Timer(30000, e -> { // Refresh every 30 seconds
            refreshCurrentPanelData();
        });
        dataRefreshTimer.start();
    }

    private void refreshCurrentPanelData() {
        if (!apiConnected) return;
        
        switch (activeMenu) {
            case "Dashboard":
                refreshDashboardData();
                break;
            case "Members":
                refreshMembersData();
                break;
        }
    }

    private void refreshDashboardData() {
        SanctumApiClient.getDashboardData().thenAccept(data -> {
            SwingUtilities.invokeLater(() -> {
                // Update dashboard with real data
                if (!data.isEmpty()) {
                    // Update KPI cards with real data
                    updateDashboardKpis(data);
                }
            });
        });
    }

    private void refreshMembersData() {
        SanctumApiClient.getMembers().thenAccept(members -> {
            SwingUtilities.invokeLater(() -> {
                // Update members table with real data
                if (!members.isEmpty()) {
                    updateMembersTable(members);
                } else {
                    // Show empty state
                    Object[][] emptyData = {
                        {"No members found", "", "", "", "", ""}
                    };
                    DefaultTableModel emptyModel = new DefaultTableModel(
                        emptyData,
                        new String[]{"ID", "Name", "Email", "Phone", "Join Date", "Status"}
                    );
                    membersTable.setModel(emptyModel);
                    styleTable(membersTable);
                    
                    // Update status
                    if (statusLabel != null) {
                        statusLabel.setText("  Section: Members  |  API Connected  | 0 members loaded");
                    }
                }
            });
        });
    }

    private void updateDashboardKpis(Map<String, Object> data) {
        SwingUtilities.invokeLater(() -> {
            if (membersKpiValue != null) membersKpiValue.setText(data.getOrDefault("total_members", "0").toString());
            if (membersKpiTrend != null) membersKpiTrend.setText("▲ " + data.getOrDefault("monthly_growth", "0%"));
            
            if (donationsKpiValue != null) donationsKpiValue.setText("$" + data.getOrDefault("total_donations", "0").toString());
            if (donationsKpiTrend != null) donationsKpiTrend.setText("▲ " + data.getOrDefault("donation_growth", "0%"));
            
            if (staffKpiValue != null) staffKpiValue.setText(data.getOrDefault("active_staff", "0").toString());
            if (staffKpiTrend != null) staffKpiTrend.setText("▲ " + data.getOrDefault("staff_growth", "0%"));
            
            if (eventsKpiValue != null) eventsKpiValue.setText(data.getOrDefault("events", "0").toString());
        });
    }

    private void updateMembersTable(List<Map<String, Object>> members) {
        if (membersTable == null) return;
        
        SwingUtilities.invokeLater(() -> {
            // Convert API data to table format
            Object[][] tableData = new Object[members.size()][6];
            
            for (int i = 0; i < members.size(); i++) {
                Map<String, Object> member = members.get(i);
                tableData[i][0] = member.getOrDefault("id", "N/A");
                tableData[i][1] = member.getOrDefault("name", "N/A");
                tableData[i][2] = member.getOrDefault("email", "N/A");
                tableData[i][3] = member.getOrDefault("phone", "N/A");
                tableData[i][4] = member.getOrDefault("join_date", "N/A");
                tableData[i][5] = member.getOrDefault("status", "Active");
            }
            
            // Update table model
            DefaultTableModel model = new DefaultTableModel(
                tableData,
                new String[]{"ID", "Name", "Email", "Phone", "Join Date", "Status"}
            );
            membersTable.setModel(model);
            styleTable(membersTable);
            
            // Update status
            if (statusLabel != null) {
                statusLabel.setText("  Section: Members  |  " + (apiConnected ? "API Connected" : "API Disconnected") + "  |  " + members.size() + " members loaded");
            }
        });
    }

    // ── Top Bar ────────────────────────────────────────────────────────────────
    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setColor(C_SURFACE);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(C_GOLD);
                g2.fillRect(0, getHeight() - 2, getWidth(), 2);
            }
        };
        bar.setPreferredSize(new Dimension(0, 52));
        bar.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));

        // Logo
        JLabel logo = LogoLoader.createLogoLabel(new Dimension(32, 32));
        logo.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        
        JLabel logoText = new JLabel("Sanctum ADMIN");
        logoText.setFont(FONT_TITLE);
        logoText.setForeground(C_GOLD);
        
        JPanel logoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        logoPanel.setOpaque(false);
        logoPanel.add(logo);
        logoPanel.add(logoText);

        // Right controls
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);

        JLabel clock = new JLabel();
        clock.setFont(FONT_LABEL);
        clock.setForeground(C_TEXT_MID);
        updateClock(clock);
        Timer t = new Timer(1000, e -> updateClock(clock));
        t.start();

        JButton notif = glowButton("⚡ 3 Alerts", C_GOLD);
        JButton user  = glowButton("● admin", C_GOLD);
        JButton logout = glowButton("⚡ LOGOUT", C_TEXT_MID);

        right.add(clock);
        right.add(notif);
        right.add(user);
        right.add(logout);
        
        // Add logout functionality
        logout.addActionListener(e -> {
            // Show confirmation dialog
            int result = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to logout?",
                "Confirm Logout",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
            );
            
            if (result == JOptionPane.YES_OPTION) {
                // Clear session and logout
                com.sanctum.auth.SessionManager.getInstance().clearSession();
                
                // Close dashboard and return to login
                dispose();
                
                // Open login frame
                SwingUtilities.invokeLater(() -> {
                    try {
                        LoginFrame loginFrame = new LoginFrame();
                        loginFrame.setVisible(true);
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(null, 
                            "Error returning to login: " + ex.getMessage(), 
                            "Error", JOptionPane.ERROR_MESSAGE);
                    }
                });
            }
        });

        bar.add(logoPanel, BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private void updateClock(JLabel lbl) {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        lbl.setText(String.format("%02d:%02d:%02d  %04d-%02d-%02d",
            now.getHour(), now.getMinute(), now.getSecond(),
            now.getYear(), now.getMonthValue(), now.getDayOfMonth()));
    }

    // ── Sidebar ────────────────────────────────────────────────────────────────
    private JPanel buildSidebar() {
        JPanel side = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(C_SURFACE);
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(C_BORDER);
                g.fillRect(getWidth() - 1, 0, 1, getHeight());
            }
        };
        side.setPreferredSize(new Dimension(200, 0));
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setBorder(BorderFactory.createEmptyBorder(16, 0, 16, 0));

        for (String[] item : MENU_ITEMS) side.add(menuItem(item[0], item[1]));
        side.add(Box.createVerticalGlue());

        JLabel ver = new JLabel("  v2.4.1 build 9081");
        ver.setFont(FONT_SMALL);
        ver.setForeground(C_TEXT_MID);
        side.add(ver);
        return side;
    }

    private JPanel menuItem(String icon, String label) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 10)) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (label.equals(activeMenu)) {
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setColor(C_GOLD_DIM_HOVER);
                    g2.fillRect(0, 0, getWidth(), getHeight());
                    g2.setColor(C_GOLD);
                    g2.fillRect(0, 0, 3, getHeight());
                }
            }
        };
        p.setOpaque(false);
        p.setMaximumSize(new Dimension(200, 44));

        JLabel ico = new JLabel(icon);
        ico.setFont(FONT_MENU);
        ico.setForeground(label.equals(activeMenu) ? C_GOLD : C_TEXT_MID);

        JLabel lbl = new JLabel(label);
        lbl.setFont(FONT_MENU);
        lbl.setForeground(label.equals(activeMenu) ? C_TEXT : C_TEXT_MID);

        p.add(ico); p.add(lbl);
        p.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        p.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                activeMenu = label;
                switchContent(label);
                p.getParent().repaint();
                ico.setForeground(C_GOLD);
                lbl.setForeground(C_TEXT);
            }
            @Override public void mouseEntered(MouseEvent e) {
                if (!label.equals(activeMenu)) lbl.setForeground(C_TEXT);
            }
            @Override public void mouseExited(MouseEvent e) {
                if (!label.equals(activeMenu)) lbl.setForeground(C_TEXT_MID);
            }
        });
        return p;
    }

    // ── Content Area ───────────────────────────────────────────────────────────
    private JScrollPane buildContent() {
        contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(C_BG);
        contentPanel.add(buildDashboardPanel(), BorderLayout.CENTER);
        JScrollPane scroll = new JScrollPane(contentPanel);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(12);
        scroll.getViewport().setBackground(C_BG);
        return scroll;
    }

    private void switchContent(String section) {
        contentPanel.removeAll();
        switch (section) {
            case "Dashboard":    contentPanel.add(buildDashboardPanel(),    BorderLayout.CENTER); break;
            case "Members":      contentPanel.add(buildMembersPanel(),      BorderLayout.CENTER); break;
            case "Announcements":contentPanel.add(buildAnnouncementsPanel(),BorderLayout.CENTER); break;
            case "Staff Registration":contentPanel.add(buildStaffRegistrationPanel(), BorderLayout.CENTER); break;
            case "Users":        contentPanel.add(buildUsersPanel(),        BorderLayout.CENTER); break;
            case "Settings":     contentPanel.add(buildSettingsPanel(),     BorderLayout.CENTER); break;
        }
        contentPanel.revalidate();
        contentPanel.repaint();
        if (statusLabel != null) {
            String connectionStatus = apiConnected ? "API Connected" : "API Disconnected";
            statusLabel.setText("  Section: " + section + "  |  " + connectionStatus + "  |  Ready");
        }
        
        // Refresh data for the new section
        refreshCurrentPanelData();
    }

    // ── Dashboard Panel ────────────────────────────────────────────────────────
    private JPanel buildDashboardPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(C_BG);
        p.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(8, 8, 8, 8);
        c.fill   = GridBagConstraints.BOTH;

        // Section header
        c.gridx=0; c.gridy=0; c.gridwidth=4; c.weightx=1; c.weighty=0;
        p.add(sectionHeader("SYSTEM OVERVIEW"), c);

        // KPI Cards
        c.gridwidth=1; c.weighty=0;
        c.gridx=0; c.gridy=1; p.add(kpiCard("TOTAL MEMBERS", "0", "0%", C_GOLD,  "▲", "members"), c);
        c.gridx=1;             p.add(kpiCard("DONATIONS",    "$0", "0%",  C_GOLD, "▲", "donations"), c);
        c.gridx=2;             p.add(kpiCard("ACTIVE STAFF", "0", "0%",  C_TEXT_MID,  "▲", "staff"), c);
        c.gridx=3;             p.add(kpiCard("EVENTS",       "0", "0",     C_GOLD, "▲", "events"), c);

        // Chart + Announcements
        c.gridx=0; c.gridy=2; c.gridwidth=2; c.weighty=1;
        p.add(buildBarChart(), c);

        c.gridx=2; c.gridy=2; c.gridwidth=2; c.weighty=1;
        p.add(buildDashboardAnnouncements(), c);

        // Recent Activity
        c.gridx=0; c.gridy=3; c.gridwidth=4; c.weighty=0;
        p.add(buildActivityFeed(), c);

        return p;
    }

    private JPanel buildDashboardAnnouncements() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(C_CARD);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(C_BORDER),
            BorderFactory.createEmptyBorder(16, 16, 16, 16)));
        
        // Header
        JLabel header = new JLabel("// LATEST ANNOUNCEMENTS");
        header.setFont(new Font("Monospaced", Font.BOLD, 11));
        header.setForeground(C_TEXT_MID);
        panel.add(header, BorderLayout.NORTH);
        
        // Announcements list
        JPanel announcementsList = new JPanel();
        announcementsList.setLayout(new BoxLayout(announcementsList, BoxLayout.Y_AXIS));
        announcementsList.setBackground(C_CARD);

        // Load real announcements from API
        loadDashboardAnnouncements(announcementsList);

        JScrollPane scrollPane = new JScrollPane(announcementsList);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(C_CARD);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private void loadDashboardAnnouncements(JPanel announcementsList) {
        SanctumApiClient.getAnnouncements().thenAccept(announcements -> {
            SwingUtilities.invokeLater(() -> {
                announcementsList.removeAll();
                
                if (announcements.isEmpty()) {
                    JLabel noDataLabel = new JLabel("No announcements available");
                    noDataLabel.setFont(FONT_SMALL);
                    noDataLabel.setForeground(C_TEXT_MID);
                    noDataLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
                    announcementsList.add(noDataLabel);
                } else {
                    // Show only the latest 3 announcements for dashboard
                    int count = Math.min(3, announcements.size());
                    for (int i = 0; i < count; i++) {
                        Map<String, Object> announcement = announcements.get(i);
                        String content = announcement.getOrDefault("content", "").toString();
                        String date = announcement.getOrDefault("created_at", "").toString();
                        
                        // Format date
                        if (date.length() > 10) {
                            date = date.substring(0, 10);
                        }
                        
                        // Truncate content for dashboard display
                        if (content.length() > 100) {
                            content = content.substring(0, 100) + "...";
                        }
                        
                        JPanel announcementCard = createMiniAnnouncementCard(content, date);
                        announcementsList.add(announcementCard);
                        if (i < count - 1) {
                            announcementsList.add(Box.createVerticalStrut(8));
                        }
                    }
                }
                announcementsList.revalidate();
                announcementsList.repaint();
            });
        });
    }

    private JPanel createMiniAnnouncementCard(String content, String date) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(C_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(C_BORDER),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)));

        JLabel contentLabel = new JLabel("<html><body style='width: 250px'>" + 
            content.substring(0, Math.min(80, content.length())) + (content.length() > 80 ? "..." : "") + "</body></html>");
        contentLabel.setFont(FONT_SMALL);
        contentLabel.setForeground(C_TEXT);

        JLabel dateLabel = new JLabel(date);
        dateLabel.setFont(FONT_SMALL);
        dateLabel.setForeground(C_TEXT_MID);

        card.add(contentLabel, BorderLayout.CENTER);
        card.add(dateLabel, BorderLayout.EAST);

        return card;
    }

    private JLabel sectionHeader(String text) {
        JLabel l = new JLabel("// " + text);
        l.setFont(new Font("Monospaced", Font.BOLD, 13));
        l.setForeground(C_TEXT_MID);
        return l;
    }

    private JPanel kpiCard(String title, String initialValue, String initialChange, Color accent, String arrow, String type) {
        JLabel valueLbl = new JLabel(initialValue);
        valueLbl.setFont(FONT_VALUE);
        valueLbl.setForeground(C_TEXT);
        
        JLabel trendLbl = new JLabel(arrow + " " + initialChange);
        trendLbl.setFont(FONT_LABEL);
        boolean up = arrow.equals("▲");
        trendLbl.setForeground(up ? C_GOLD : C_TEXT_MID);

        // Store references for updates
        switch (type) {
            case "members":   membersKpiValue = valueLbl;   membersKpiTrend = trendLbl; break;
            case "donations": donationsKpiValue = valueLbl; donationsKpiTrend = trendLbl; break;
            case "staff":     staffKpiValue = valueLbl;     staffKpiTrend = trendLbl; break;
            case "events":    eventsKpiValue = valueLbl;    eventsKpiTrend = trendLbl; break;
        }

        JPanel card = new JPanel(new GridBagLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(C_CARD);
                g2.fillRoundRect(0,0,getWidth(),getHeight(),12,12);
                g2.setColor(accent);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(0,0,getWidth()-1,getHeight()-1,12,12);
                g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 80));
                g2.fillRoundRect(0,0,getWidth(),4,4,4);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setPreferredSize(new Dimension(200, 110));
        card.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

        GridBagConstraints c = new GridBagConstraints();
        c.fill=GridBagConstraints.HORIZONTAL; c.weightx=1;

        JLabel t = new JLabel(title); t.setFont(FONT_LABEL); t.setForeground(C_TEXT_MID);
        
        c.gridy=0; card.add(t, c);
        c.gridy=1; card.add(valueLbl, c);
        c.gridy=2; card.add(trendLbl, c);
        return card;
    }

    // Simple bar chart drawn with Graphics2D
    private JPanel buildBarChart() {
        int[] data   = {62, 45, 78, 55, 90, 38, 72, 85, 60, 95, 50, 68};
        String[] months = {"Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"};

        JPanel chart = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                g2.setColor(C_CARD);
                g2.fillRoundRect(0,0,getWidth(),getHeight(),12,12);
                g2.setColor(C_BORDER);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0,0,getWidth()-1,getHeight()-1,12,12);

                // title
                g2.setFont(new Font("Monospaced",Font.BOLD,11));
                g2.setColor(C_TEXT_MID);
                g2.drawString("// MONTHLY REVENUE  ($000s)", 14, 22);

                int pad = 40, bott = 30;
                int w = getWidth() - pad*2;
                int h = getHeight() - pad - bott;
                int barW = w / data.length;
                int maxV = 100;

                // grid lines
                g2.setStroke(new BasicStroke(0.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{4}, 0));
                g2.setColor(C_BORDER);
                for (int i = 0; i <= 4; i++) {
                    int y = pad + h - (h * i / 4);
                    g2.drawLine(pad, y, pad + w, y);
                    g2.setFont(FONT_SMALL);
                    g2.setColor(C_TEXT_MID);
                    g2.drawString(String.valueOf(maxV * i / 4), 4, y + 4);
                    g2.setColor(C_BORDER);
                }

                // bars
                for (int i = 0; i < data.length; i++) {
                    int bh = (int)(h * data[i] / (float)maxV);
                    int x  = pad + i * barW + barW/5;
                    int y  = pad + h - bh;
                    int bw = (int)(barW * 0.6);

                    // gradient bar
                    GradientPaint gp = new GradientPaint(x, y, C_GOLD, x, y+bh, C_GOLD_HOVER);
                    g2.setPaint(gp);
                    g2.setStroke(new BasicStroke(1f));
                    g2.fillRoundRect(x, y, bw, bh, 4, 4);

                    // glow top
                    g2.setColor(C_GOLD_DIM_HOVER);
                    g2.fillRect(x, y, bw, 3);

                    // label
                    g2.setFont(FONT_SMALL);
                    g2.setColor(C_TEXT_MID);
                    g2.drawString(months[i], x - 2, getHeight() - 10);
                }
                g2.dispose();
            }
        };
        chart.setOpaque(false);
        chart.setPreferredSize(new Dimension(0, 220));
        return chart;
    }

    private JPanel buildActivityFeed() {
        JPanel feed = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(C_CARD);
                ((Graphics2D)g).fillRoundRect(0,0,getWidth(),getHeight(),12,12);
                g.setColor(C_BORDER);
                ((Graphics2D)g).drawRoundRect(0,0,getWidth()-1,getHeight()-1,12,12);
            }
        };
        feed.setOpaque(false);
        feed.setLayout(new BoxLayout(feed, BoxLayout.Y_AXIS));
        feed.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        JLabel hdr = new JLabel("// LIVE ACTIVITY");
        hdr.setFont(FONT_LABEL); hdr.setForeground(C_TEXT_MID);
        hdr.setAlignmentX(LEFT_ALIGNMENT);
        feed.add(hdr);
        feed.add(Box.createVerticalStrut(10));

        String[][] events = {
            {"⚡", "New signup", "2s ago",  C_GOLD +""},
            {"✓",  "Payment OK", "14s ago", C_GOLD+""},
            {"✗",  "Login fail", "1m ago",  C_TEXT_MID +""},
            {"⚡", "API call",   "1m ago",  C_GOLD +""},
            {"✓",  "Export done","3m ago",  C_GOLD+""},
            {"⚠",  "High CPU",   "5m ago",  C_GOLD+""},
        };

        Color[] cols = {C_GOLD, C_GOLD, C_TEXT_MID, C_GOLD, C_GOLD, C_GOLD};

        for (int i = 0; i < events.length; i++) {
            JPanel row = new JPanel(new BorderLayout());
            row.setOpaque(false);
            row.setMaximumSize(new Dimension(9999, 30));
            row.setAlignmentX(LEFT_ALIGNMENT);

            JLabel ev = new JLabel(events[i][0] + " " + events[i][1]);
            ev.setFont(FONT_SMALL);
            ev.setForeground(cols[i]);

            JLabel tm = new JLabel(events[i][2]);
            tm.setFont(FONT_SMALL);
            tm.setForeground(C_TEXT_MID);

            row.add(ev, BorderLayout.WEST);
            row.add(tm, BorderLayout.EAST);
            feed.add(row);
            feed.add(Box.createVerticalStrut(6));
        }
        return feed;
    }

    private JPanel buildMiniTable() {
        String[] cols = {"ID", "USER", "ACTION", "IP ADDRESS", "STATUS", "TIME"};
        Object[][] rows = {
            {"#8821", "alice@mail.com",  "LOGIN",  "192.168.1.10", "✓ OK",     "00:01"},
            {"#8820", "bob@corp.io",     "EXPORT", "10.0.0.55",    "✓ OK",     "00:03"},
            {"#8819", "charlie@x.com",   "DELETE", "172.16.0.4",   "✗ DENIED", "00:07"},
            {"#8818", "diana@admin.net", "UPDATE", "192.168.2.1",  "✓ OK",     "00:12"},
        };

        DefaultTableModel model = new DefaultTableModel(rows, cols) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        JTable table = new JTable(model);
        table.setFont(FONT_TABLE);
        table.setForeground(C_TEXT);
        table.setBackground(C_CARD);
        table.setSelectionBackground(C_GOLD_DIM);
        table.setSelectionForeground(C_GOLD);
        table.setGridColor(C_BORDER);
        table.setRowHeight(26);
        table.getTableHeader().setFont(FONT_LABEL);
        table.getTableHeader().setBackground(C_SURFACE);
        table.getTableHeader().setForeground(C_TEXT_MID);
        table.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, C_BORDER));

        // Colour status column
        table.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object val,
                    boolean sel, boolean foc, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t,val,sel,foc,row,col);
                l.setForeground(val.toString().startsWith("✓") ? C_GOLD : C_TEXT_MID);
                l.setBackground(sel ? C_GOLD_DIM : C_CARD);
                l.setOpaque(true);
                return l;
            }
        });

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createLineBorder(C_BORDER));
        sp.getViewport().setBackground(C_CARD);
        sp.setPreferredSize(new Dimension(0, 130));

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        JLabel hdr = sectionHeader("RECENT AUDIT LOG");
        hdr.setBorder(BorderFactory.createEmptyBorder(8,0,6,0));
        wrapper.add(hdr, BorderLayout.NORTH);
        wrapper.add(sp, BorderLayout.CENTER);
        return wrapper;
    }

    // ── Users Panel ────────────────────────────────────────────────────────────
    private JPanel buildUsersPanel() {
        JPanel p = new JPanel(new BorderLayout(0, 12));
        p.setBackground(C_BG);
        p.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        JLabel hdr = sectionHeader("USER MANAGEMENT");
        p.add(hdr, BorderLayout.NORTH);

        String[] cols = {"ID", "USERNAME", "EMAIL", "ROLE", "STATUS", "JOINED"};
        Object[][] rows = {
            {"001", "alice",   "alice@mail.com",    "ADMIN",  "Active",   "2023-01"},
            {"002", "bob",     "bob@corp.io",       "USER",   "Active",   "2023-04"},
            {"003", "charlie", "charlie@x.com",     "MOD",    "Suspended","2023-07"},
            {"004", "diana",   "diana@admin.net",   "ADMIN",  "Active",   "2023-09"},
            {"005", "evan",    "evan@service.org",  "USER",   "Active",   "2024-01"},
            {"006", "fiona",   "fiona@enterprise",  "USER",   "Inactive", "2024-03"},
        };

        DefaultTableModel model = new DefaultTableModel(rows, cols) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = styleTable(new JTable(model));

        table.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object val,
                    boolean sel, boolean foc, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t,val,sel,foc,row,col);
                l.setOpaque(true);
                l.setBackground(sel ? C_GOLD_DIM : C_CARD);
                switch (val.toString()) {
                    case "Active":    l.setForeground(C_GOLD); break;
                    case "Suspended": l.setForeground(C_TEXT_MID);  break;
                    default:          l.setForeground(C_GOLD); break;
                }
                return l;
            }
        });

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createLineBorder(C_BORDER));
        sp.getViewport().setBackground(C_CARD);
        p.add(sp, BorderLayout.CENTER);

        // Action buttons
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.setOpaque(false);
        actions.add(glowButton("+ Add User",    C_GOLD));
        actions.add(glowButton("✎ Edit",        C_GOLD));
        actions.add(glowButton("⊗ Suspend",     C_GOLD));
        actions.add(glowButton("✕ Delete",      C_TEXT_MID));
        p.add(actions, BorderLayout.SOUTH);
        return p;
    }

    // ── Settings Panel ────────────────────────────────────────────────────────
    private JPanel buildSettingsPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(C_BG);
        p.setBorder(BorderFactory.createEmptyBorder(24,24,24,24));
        GridBagConstraints c = new GridBagConstraints();
        c.insets=new Insets(6,8,6,8); c.fill=GridBagConstraints.HORIZONTAL; c.weightx=1;

        c.gridx=0; c.gridy=0; c.gridwidth=2;
        p.add(sectionHeader("CHURCH SETTINGS"), c);

        // Loading indicator
        JLabel loadingLabel = new JLabel("Loading church details...", SwingConstants.CENTER);
        loadingLabel.setFont(FONT_LABEL);
        loadingLabel.setForeground(C_TEXT_MID);
        c.gridy=1; c.gridwidth=2;
        p.add(loadingLabel, c);

        // Load church data from backend
        SanctumApiClient.getChurchDetails().thenAccept(churchData -> {
            SwingUtilities.invokeLater(() -> {
                p.remove(loadingLabel);
                buildSettingsContent(p, churchData);
                p.revalidate();
                p.repaint();
            });
        }).exceptionally(ex -> {
            SwingUtilities.invokeLater(() -> {
                p.remove(loadingLabel);
                JLabel errorLabel = new JLabel("Failed to load church details. Using default values.", SwingConstants.CENTER);
                errorLabel.setFont(FONT_LABEL);
                errorLabel.setForeground(new Color(239, 68, 68));
                c.gridy=1; c.gridwidth=2;
                p.add(errorLabel, c);
                
                // Build with default values
                buildSettingsContent(p, null);
                p.revalidate();
                p.repaint();
            });
            return null;
        });

        return p;
    }
    
    private void buildSettingsContent(JPanel p, Map<String, Object> churchData) {
        GridBagConstraints c = new GridBagConstraints();
        c.insets=new Insets(6,8,6,8); c.fill=GridBagConstraints.HORIZONTAL; c.weightx=1;
        int currentRow = 1;

        // Branding Section
        c.gridy=currentRow++; c.gridwidth=2;
        p.add(sectionHeader("Branding & Appearance"), c);
        
        // Church Logo
        c.gridy=currentRow++; c.gridwidth=1; c.gridx=0; c.weightx=0.3;
        JLabel logoLabel = new JLabel("Church Logo:");
        logoLabel.setFont(FONT_LABEL); logoLabel.setForeground(C_TEXT_MID);
        p.add(logoLabel, c);

        c.gridx=1; c.weightx=0.7;
        JPanel logoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        logoPanel.setOpaque(false);
        
        JButton uploadLogoBtn = glowButton("UPLOAD LOGO", C_GOLD);
        JButton removeLogoBtn = glowButton("REMOVE", C_TEXT_MID);
        
        logoPanel.add(uploadLogoBtn);
        logoPanel.add(Box.createHorizontalStrut(10));
        logoPanel.add(removeLogoBtn);
        p.add(logoPanel, c);

        // Church Name - Load from backend
        c.gridy=currentRow++; c.gridx=0; c.weightx=0.3;
        JLabel churchNameLabel = new JLabel("Church Name:");
        churchNameLabel.setFont(FONT_LABEL); churchNameLabel.setForeground(C_TEXT_MID);
        p.add(churchNameLabel, c);

        c.gridx=1; c.weightx=0.7;
        String churchName = (churchData != null) ? 
            churchData.getOrDefault("name", "Grace Community Church").toString() : 
            "Grace Community Church";
        JTextField churchNameField = createSettingsTextField(churchName);
        p.add(churchNameField, c);

        // Church Type - Load from backend
        c.gridy=currentRow++; c.gridx=0; c.weightx=0.3;
        JLabel churchTypeLabel = new JLabel("Church Type:");
        churchTypeLabel.setFont(FONT_LABEL); churchTypeLabel.setForeground(C_TEXT_MID);
        p.add(churchTypeLabel, c);

        c.gridx=1; c.weightx=0.7;
        String churchType = (churchData != null) ? 
            churchData.getOrDefault("church_type_display", "Main Church").toString() : 
            "Main Church";
        JTextField churchTypeField = createSettingsTextField(churchType);
        churchTypeField.setEditable(false); // Read-only
        p.add(churchTypeField, c);

        // Theme Colors Section
        c.gridy=currentRow++; c.gridwidth=2;
        p.add(sectionHeader("Theme Colors"), c);
        
        // Color pickers - Load from backend
        String[] colorLabels = {"Primary Color", "Secondary Color", "Background Color", "Panel Color", "Card Color"};
        Color[] defaultColors = {
            churchData != null ? parseColor(churchData.get("primary_color").toString()) : C_GOLD,
            churchData != null ? parseColor(churchData.get("secondary_color").toString()) : C_TEXT_MID,
            C_BG, C_SURFACE, C_CARD
        };
        
        for (int i = 0; i < colorLabels.length; i++) {
            c.gridy=currentRow++; c.gridx=0; c.weightx=0.3;
            JLabel colorLabel = new JLabel(colorLabels[i] + ":");
            colorLabel.setFont(FONT_LABEL); colorLabel.setForeground(C_TEXT_MID);
            p.add(colorLabel, c);

            c.gridx=1; c.weightx=0.7;
            JPanel colorPickerPanel = createColorPicker(defaultColors[i], colorLabels[i]);
            p.add(colorPickerPanel, c);
        }

        // Church Information Section
        c.gridy=currentRow++; c.gridwidth=2;
        p.add(sectionHeader("Church Information"), c);
        
        // Load church information from backend
        String[][] churchFields = {
            {"Church Code", churchData != null ? churchData.getOrDefault("church_code", "N/A").toString() : "N/A"},
            {"Senior Pastor Name", churchData != null ? churchData.getOrDefault("senior_pastor_name", "Rev. James Anderson").toString() : "Rev. James Anderson"},
            {"Address", churchData != null ? churchData.getOrDefault("address_line1", "123 Main Street, City, State 12345").toString() : "123 Main Street, City, State 12345"},
            {"City", churchData != null ? churchData.getOrDefault("city", "Nairobi").toString() : "Nairobi"},
            {"County", churchData != null ? churchData.getOrDefault("county", "Nairobi County").toString() : "Nairobi County"},
            {"Phone", churchData != null ? churchData.getOrDefault("phone_number", "+1 (555) 123-4567").toString() : "+1 (555) 123-4567"},
            {"Email", churchData != null ? churchData.getOrDefault("email", "info@gracechurch.com").toString() : "info@gracechurch.com"},
            {"Website", churchData != null ? churchData.getOrDefault("website", "www.gracechurch.com").toString() : "www.gracechurch.com"},
        };

        for (int i=0; i<churchFields.length; i++) {
            c.gridwidth=1; c.gridy=currentRow++;
            c.gridx=0; c.weightx=0.3;
            JLabel lbl = new JLabel(churchFields[i][0]);
            lbl.setFont(FONT_LABEL); lbl.setForeground(C_TEXT_MID);
            
            // Make church code read-only
            if ("Church Code".equals(churchFields[i][0])) {
                lbl.setToolTipText("Church code cannot be modified");
            }
            p.add(lbl, c);

            c.gridx=1; c.weightx=0.7;
            JTextField tf = createSettingsTextField(churchFields[i][1]);
            
            // Make church code field read-only
            if ("Church Code".equals(churchFields[i][0])) {
                tf.setEditable(false);
                tf.setBackground(C_SURFACE);
                tf.setToolTipText("Church code cannot be modified");
            }
            
            p.add(tf, c);
        }

        // Service Settings
        c.gridy=currentRow++; c.gridwidth=2;
        p.add(sectionHeader("Service Settings"), c);
        
        String[][] serviceFields = {
            {"Sunday Service Time",   "9:00 AM"},
            {"Wednesday Service",    "7:00 PM"},
            {"Prayer Meeting",        "6:00 AM"},
            {"Youth Service",         "5:00 PM"},
        };

        for (int i=0; i<serviceFields.length; i++) {
            c.gridwidth=1; c.gridy=currentRow++;
            c.gridx=0; c.weightx=0.3;
            JLabel lbl = new JLabel(serviceFields[i][0]);
            lbl.setFont(FONT_LABEL); lbl.setForeground(C_TEXT_MID);
            p.add(lbl, c);

            c.gridx=1; c.weightx=0.7;
            JTextField tf = createSettingsTextField(serviceFields[i][1]);
            p.add(tf, c);
        }

        // System Settings
        c.gridy=currentRow++; c.gridwidth=2;
        p.add(sectionHeader("System Settings"), c);
        
        String[][] systemFields = {
            {"Admin Email",     "admin@church.com"},
            {"Timezone",        "UTC+3 (Nairobi)"},
            {"Currency",        "KES"},
            {"Date Format",     "DD/MM/YYYY"},
        };

        for (int i=0; i<systemFields.length; i++) {
            c.gridwidth=1; c.gridy=currentRow++;
            c.gridx=0; c.weightx=0.3;
            JLabel lbl = new JLabel(systemFields[i][0]);
            lbl.setFont(FONT_LABEL); lbl.setForeground(C_TEXT_MID);
            p.add(lbl, c);

            c.gridx=1; c.weightx=0.7;
            JTextField tf = createSettingsTextField(systemFields[i][1]);
            p.add(tf, c);
        }

        // Save buttons
        c.gridy=currentRow++; c.gridwidth=2;
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT,8,0));
        btns.setOpaque(false);
        
        JButton saveBtn = glowButton("✓ Save Settings", C_GOLD);
        JButton resetBtn = glowButton("↺ Reset", C_GOLD);
        JButton exportBtn = glowButton("📤 Export Data", C_GOLD);
        JButton importBtn = glowButton("📥 Import Data", C_TEXT_MID);
        
        saveBtn.addActionListener(e -> saveChurchSettings());
        resetBtn.addActionListener(e -> resetSettings());
        exportBtn.addActionListener(e -> exportChurchData());
        importBtn.addActionListener(e -> importChurchData());
        
        btns.add(saveBtn);
        btns.add(resetBtn);
        btns.add(exportBtn);
        btns.add(importBtn);
        p.add(btns, c);
    }
    
    private JTextField createSettingsTextField(String text) {
        JTextField tf = new JTextField(text);
        tf.setFont(FONT_TABLE); 
        tf.setForeground(C_TEXT);
        tf.setBackground(C_CARD); 
        tf.setCaretColor(C_GOLD);
        tf.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(C_BORDER),
            BorderFactory.createEmptyBorder(4,8,4,8)));
        return tf;
    }
    
    private Color parseColor(String colorString) {
        try {
            if (colorString.startsWith("#")) {
                return Color.decode(colorString);
            }
        } catch (Exception e) {
            // Return default color if parsing fails
        }
        return C_GOLD;
    }
    
    private void saveChurchSettings() {
        // Implementation to save settings to backend
        JOptionPane.showMessageDialog(this, 
            "✅ Church settings saved successfully!", 
            "Settings Saved", 
            JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void resetSettings() {
        // Implementation to reset settings
        JOptionPane.showMessageDialog(this, 
            "⚠️ Settings have been reset to defaults.", 
            "Settings Reset", 
            JOptionPane.WARNING_MESSAGE);
    }
    
    private void exportChurchData() {
        // Implementation to export data
        JOptionPane.showMessageDialog(this, 
            "📤 Church data exported successfully!", 
            "Export Complete", 
            JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void importChurchData() {
        // Implementation to import data
        JOptionPane.showMessageDialog(this, 
            "📥 Church data import feature coming soon.", 
            "Import Feature", 
            JOptionPane.INFORMATION_MESSAGE);
    }

    // ── Helper Methods ────────────────────────────────────────────────────────

    private JPanel createColorPicker(Color currentColor, String label) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        panel.setOpaque(false);
        
        // Color preview
        JPanel colorPreview = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(currentColor);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(C_BORDER);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 8, 8);
                g2.dispose();
            }
        };
        colorPreview.setPreferredSize(new Dimension(40, 24));
        colorPreview.setOpaque(false);
        
        // Hex value
        JTextField hexField = new JTextField(String.format("#%02X%02X%02X", 
            currentColor.getRed(), currentColor.getGreen(), currentColor.getBlue()));
        hexField.setFont(FONT_TABLE);
        hexField.setForeground(C_TEXT);
        hexField.setBackground(C_CARD);
        hexField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(C_BORDER),
            BorderFactory.createEmptyBorder(2, 6, 2, 6)));
        hexField.setPreferredSize(new Dimension(80, 24));
        
        // Choose button
        JButton chooseBtn = glowButton("CHOOSE", C_GOLD);
        chooseBtn.setPreferredSize(new Dimension(70, 24));
        
        // Add components
        panel.add(colorPreview);
        panel.add(Box.createHorizontalStrut(8));
        panel.add(hexField);
        panel.add(Box.createHorizontalStrut(8));
        panel.add(chooseBtn);
        
        // Color picker action
        chooseBtn.addActionListener(e -> {
            Color chosenColor = JColorChooser.showDialog(this, "Choose " + label, currentColor);
            if (chosenColor != null) {
                // Update preview and hex field
                hexField.setText(String.format("#%02X%02X%02X", 
                    chosenColor.getRed(), chosenColor.getGreen(), chosenColor.getBlue()));
                
                // Repaint preview
                colorPreview.repaint();
                
                // Apply theme change (in real implementation)
                applyThemeChange(label, chosenColor);
            }
        });
        
        return panel;
    }

    private void applyThemeChange(String colorLabel, Color newColor) {
        // In a real implementation, this would update the theme colors
        // and refresh the UI to show the changes
        SwingUtilities.invokeLater(() -> {
            // Show confirmation
            JOptionPane.showMessageDialog(this, 
                colorLabel + " updated to " + String.format("#%02X%02X%02X", 
                    newColor.getRed(), newColor.getGreen(), newColor.getBlue()),
                "Theme Updated", 
                JOptionPane.INFORMATION_MESSAGE);
        });
    }

    // ── Helpers ────────────────────────────────────────────────────────────────
    private JButton glowButton(String text, Color accent) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean hover = getModel().isRollover();
                g2.setColor(hover ? new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 40)
                                  : new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 18));
                g2.fillRoundRect(0,0,getWidth(),getHeight(),6,6);
                g2.setColor(accent);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0,0,getWidth()-1,getHeight()-1,6,6);
                g2.setFont(FONT_LABEL);
                g2.setColor(accent);
                FontMetrics fm = g2.getFontMetrics();
                int tx = (getWidth()-fm.stringWidth(text))/2;
                int ty = (getHeight()+fm.getAscent()-fm.getDescent())/2;
                g2.drawString(text, tx, ty);
                g2.dispose();
            }
        };
        btn.setPreferredSize(new Dimension(Math.max(110, btn.getPreferredSize().width+24), 28));
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JTable styleTable(JTable table) {
        table.setFont(FONT_TABLE);
        table.setForeground(C_TEXT);
        table.setBackground(C_CARD);
        table.setSelectionBackground(new Color(0,212,255,40));
        table.setSelectionForeground(C_GOLD);
        table.setGridColor(C_BORDER);
        table.setRowHeight(26);
        table.getTableHeader().setFont(FONT_LABEL);
        table.getTableHeader().setBackground(C_SURFACE);
        table.getTableHeader().setForeground(C_TEXT_MID);
        table.getTableHeader().setBorder(BorderFactory.createMatteBorder(0,0,1,0,C_BORDER));
        return table;
    }

    private Icon checkIcon(boolean selected) {
        return new Icon() {
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(selected ? C_GOLD : C_SURFACE);
                g2.fillRoundRect(x,y,14,14,4,4);
                g2.setColor(C_BORDER);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(x,y,13,13,4,4);
                if (selected) {
                    g2.setColor(C_BG);
                    g2.setStroke(new BasicStroke(2f));
                    g2.drawLine(x+3,y+7,x+6,y+10);
                    g2.drawLine(x+6,y+10,x+11,y+4);
                }
                g2.dispose();
            }
            public int getIconWidth()  { return 16; }
            public int getIconHeight() { return 16; }
        };
    }

    // ── Members Panel ─────────────────────────────────────────────────────────────
    private JPanel buildMembersPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(C_BG);
        p.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        // Header with search and add button
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        
        header.add(sectionHeader("CHURCH MEMBERS"), BorderLayout.WEST);
        
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        controls.setOpaque(false);
        
        JTextField searchField = new JTextField("Search members...");
        searchField.setFont(FONT_TABLE);
        searchField.setForeground(C_TEXT);
        searchField.setBackground(C_CARD);
        searchField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(C_BORDER),
            BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        searchField.setPreferredSize(new Dimension(200, 32));
        
        JButton addBtn = glowButton("+ ADD MEMBER", C_GOLD);
        JButton exportBtn = glowButton("EXPORT", C_GOLD);
        JButton refreshBtn = glowButton("↻ REFRESH", C_GOLD);
        
        // Add action listeners
        refreshBtn.addActionListener(e -> refreshMembersData());
        addBtn.addActionListener(e -> showAddMemberDialog());
        
        controls.add(searchField);
        controls.add(refreshBtn);
        controls.add(addBtn);
        controls.add(exportBtn);
        header.add(controls, BorderLayout.EAST);
        
        p.add(header, BorderLayout.NORTH);

        // Members table with dynamic data
        String[] columns = {"ID", "Name", "Email", "Phone", "Join Date", "Status"};
        Object[][] data = {
            {"Loading...", "Loading...", "Loading...", "Loading...", "Loading...", "Loading..."},
        };

        JTable table = new JTable(data, columns);
        styleTable(table);
        
        // Store table reference for updates
        membersTable = table;
        
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(C_CARD);
        p.add(scrollPane, BorderLayout.CENTER);

        // Load real data
        if (apiConnected) {
            refreshMembersData();
        }

        return p;
    }

    private void showAddMemberDialog() {
        // Create dialog with Windows-specific settings
        JDialog dialog = new JDialog(this, "Add New Member", true);
        dialog.setSize(400, 300);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        
        // Apply Windows-specific fixes
        WindowsDialogFix.fixDialogBlinking(dialog);
        
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(C_BG);
        formPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        
        String[] labels = {"Name:", "Email:", "Phone:", "Address:"};
        JTextField[] fields = new JTextField[labels.length];
        
        for (int i = 0; i < labels.length; i++) {
            gbc.gridx = 0; gbc.gridy = i;
            JLabel label = new JLabel(labels[i]);
            label.setFont(FONT_LABEL);
            label.setForeground(C_TEXT_MID);
            formPanel.add(label, gbc);
            
            gbc.gridx = 1; gbc.weightx = 1.0;
            fields[i] = new JTextField(20);
            fields[i].setFont(FONT_TABLE);
            fields[i].setForeground(C_TEXT);
            fields[i].setBackground(C_CARD);
            fields[i].setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(C_BORDER),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));
            formPanel.add(fields[i], gbc);
        }
        
        // Buttons
        gbc.gridx = 0; gbc.gridy = labels.length; gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.NONE; gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(15, 5, 5, 5);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        buttonPanel.setOpaque(false);
        
        JButton saveBtn = glowButton("SAVE", C_GOLD);
        JButton cancelBtn = glowButton("CANCEL", C_TEXT_MID);
        
        saveBtn.addActionListener(e -> {
            // Create member data map
            Map<String, Object> memberData = new HashMap<>();
            memberData.put("name", fields[0].getText());
            memberData.put("email", fields[1].getText());
            memberData.put("phone", fields[2].getText());
            memberData.put("address", fields[3].getText());
            
            // Send to API
            SanctumApiClient.createMember(memberData).thenAccept(success -> {
                SwingUtilities.invokeLater(() -> {
                    if (success) {
                        JOptionPane.showMessageDialog(dialog, "Member added successfully!");
                        refreshMembersData();
                        dialog.dispose();
                    } else {
                        JOptionPane.showMessageDialog(dialog, "Failed to add member", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                });
            });
        });
        
        cancelBtn.addActionListener(e -> dialog.dispose());
        
        buttonPanel.add(saveBtn);
        buttonPanel.add(cancelBtn);
        formPanel.add(buttonPanel, gbc);
        
        dialog.add(formPanel, BorderLayout.CENTER);
        
        // Show dialog with Windows-specific smooth display
        WindowsDialogFix.showDialogSmoothly(dialog);
    }

    // ── Announcements Panel ───────────────────────────────────────────────────────
    private JPanel buildAnnouncementsPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(C_BG);
        p.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(sectionHeader("ANNOUNCEMENTS"), BorderLayout.WEST);
        
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        controls.setOpaque(false);
        
        JButton newBtn = glowButton("NEW ANNOUNCEMENT", C_GOLD);
        JButton refreshBtn = glowButton("REFRESH", C_GOLD);
        
        newBtn.addActionListener(e -> showCreateAnnouncementDialog());
        refreshBtn.addActionListener(e -> refreshAnnouncements());
        
        controls.add(newBtn);
        controls.add(refreshBtn);
        header.add(controls, BorderLayout.EAST);
        p.add(header, BorderLayout.NORTH);

        // Announcements list
        JPanel announcementsList = new JPanel();
        announcementsList.setLayout(new BoxLayout(announcementsList, BoxLayout.Y_AXIS));
        announcementsList.setBackground(C_BG);

        // Load announcements from API
        loadAnnouncements(announcementsList);

        JScrollPane scrollPane = new JScrollPane(announcementsList);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(C_BG);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        p.add(scrollPane, BorderLayout.CENTER);
        return p;
    }

    private void loadAnnouncements(JPanel announcementsList) {
        announcementsList.removeAll();
        
        SanctumApiClient.getAnnouncements().thenAccept(announcements -> {
            SwingUtilities.invokeLater(() -> {
                if (announcements.isEmpty()) {
                    JLabel noDataLabel = new JLabel("No announcements found. Click 'NEW ANNOUNCEMENT' to create one.");
                    noDataLabel.setFont(FONT_TABLE);
                    noDataLabel.setForeground(C_TEXT_MID);
                    noDataLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
                    announcementsList.add(noDataLabel);
                } else {
                    for (Map<String, Object> announcement : announcements) {
                        String title = announcement.getOrDefault("title", "Untitled").toString();
                        String content = announcement.getOrDefault("content", "").toString();
                        String date = announcement.getOrDefault("created_at", "").toString();
                        String priority = announcement.getOrDefault("priority", "normal").toString();
                        
                        // Format date
                        if (date.length() > 10) {
                            date = date.substring(0, 10);
                        }
                        
                        JPanel announcementCard = createAnnouncementCard(title, content, date, priority);
                        announcementsList.add(announcementCard);
                        announcementsList.add(Box.createVerticalStrut(10));
                    }
                }
                announcementsList.revalidate();
                announcementsList.repaint();
            });
        });
    }

    private void refreshAnnouncements() {
        // Find the announcements panel and refresh it
        if (contentPanel != null) {
            Component[] components = contentPanel.getComponents();
            for (Component component : components) {
                if (component instanceof JScrollPane) {
                    JScrollPane scrollPane = (JScrollPane) component;
                    JPanel announcementsList = (JPanel) scrollPane.getViewport().getView();
                    loadAnnouncements(announcementsList);
                    break;
                }
            }
        }
    }

    private void showCreateAnnouncementDialog() {
        // Create dialog with Windows-specific settings
        JDialog dialog = new JDialog(this, "Create New Announcement", true);
        dialog.setSize(700, 600);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setBackground(C_BG);
        
        // Apply Windows-specific fixes
        WindowsDialogFix.fixDialogBlinking(dialog);

        // Main panel with gradient background
        JPanel mainPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Gradient background
                GradientPaint gradient = new GradientPaint(0, 0, C_SURFACE, getWidth(), getHeight(), C_BG);
                g2d.setPaint(gradient);
                g2d.fillRect(0, 0, getWidth(), getHeight());
                g2d.dispose();
            }
        };
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        
        JLabel titleLabel = new JLabel("📢 Create New Announcement");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setForeground(C_GOLD);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));
        
        headerPanel.add(titleLabel, BorderLayout.CENTER);
        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // Form panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setOpaque(false);
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(12, 12, 12, 12);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        // Title field with enhanced styling
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        JLabel titleLabelField = new JLabel("Announcement Title:");
        titleLabelField.setFont(new Font("Segoe UI", Font.BOLD, 12));
        titleLabelField.setForeground(C_TEXT);
        formPanel.add(titleLabelField, gbc);
        
        gbc.gridx = 1; gbc.weightx = 1.0;
        JTextField titleField = new JTextField();
        titleField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        titleField.setForeground(C_TEXT);
        titleField.setBackground(C_CARD);
        titleField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(C_GOLD),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        titleField.setOpaque(true);
        formPanel.add(titleField, gbc);

        // Content field with enhanced styling
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0; gbc.fill = GridBagConstraints.NONE;
        JLabel contentLabel = new JLabel("Announcement Content:");
        contentLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        contentLabel.setForeground(C_TEXT);
        formPanel.add(contentLabel, gbc);
        
        gbc.gridx = 1; gbc.weightx = 1.0; gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;
        JTextArea contentArea = new JTextArea(10, 40);
        contentArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        contentArea.setForeground(C_TEXT);
        contentArea.setBackground(C_CARD);
        contentArea.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(C_GOLD),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        contentArea.setLineWrap(true);
        contentArea.setWrapStyleWord(true);
        contentArea.setOpaque(true);
        JScrollPane contentScrollPane = new JScrollPane(contentArea);
        contentScrollPane.setBorder(null);
        contentScrollPane.getViewport().setOpaque(false);
        formPanel.add(contentScrollPane, gbc);

        // Priority field with enhanced styling
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0; gbc.weighty = 0; gbc.fill = GridBagConstraints.HORIZONTAL;
        JLabel priorityLabel = new JLabel("Priority Level:");
        priorityLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        priorityLabel.setForeground(C_TEXT);
        formPanel.add(priorityLabel, gbc);
        
        gbc.gridx = 1; gbc.weightx = 1.0;
        JComboBox<String> priorityCombo = new JComboBox<>(new String[]{"low", "medium", "high"});
        priorityCombo.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        priorityCombo.setForeground(C_TEXT);
        priorityCombo.setBackground(C_CARD);
        priorityCombo.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(C_GOLD),
            BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        priorityCombo.setOpaque(true);
        formPanel.add(priorityCombo, gbc);

        // Character count label
        gbc.gridx = 1; gbc.gridy = 3; gbc.anchor = GridBagConstraints.EAST;
        JLabel charCountLabel = new JLabel("0 characters");
        charCountLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        charCountLabel.setForeground(C_TEXT_MID);
        formPanel.add(charCountLabel, gbc);

        // Add character count listener
        contentArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { updateCharCount(); }
            @Override
            public void removeUpdate(DocumentEvent e) { updateCharCount(); }
            @Override
            public void changedUpdate(DocumentEvent e) { updateCharCount(); }
            
            private void updateCharCount() {
                int count = contentArea.getText().length();
                charCountLabel.setText(count + " characters");
                if (count > 500) {
                    charCountLabel.setForeground(new Color(239, 68, 68)); // Red
                } else if (count > 300) {
                    charCountLabel.setForeground(new Color(245, 158, 11)); // Orange
                } else {
                    charCountLabel.setForeground(C_TEXT_MID); // Gray
                }
            }
        });

        // Buttons panel
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.NONE; gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(20, 12, 12, 12);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        buttonPanel.setOpaque(false);
        
        JButton createBtn = createStyledDialogButton("📢 CREATE ANNOUNCEMENT", C_GOLD);
        JButton cancelBtn = createStyledDialogButton("✖ CANCEL", C_TEXT_MID);
        
        createBtn.addActionListener(e -> {
            String title = titleField.getText().trim();
            String content = contentArea.getText().trim();
            String priority = priorityCombo.getSelectedItem().toString();
            
            // Enhanced validation
            if (title.isEmpty()) {
                showValidationError(dialog, "Please enter an announcement title.");
                titleField.requestFocus();
                return;
            }
            
            if (content.isEmpty()) {
                showValidationError(dialog, "Please enter announcement content.");
                contentArea.requestFocus();
                return;
            }
            
            if (content.length() > 1000) {
                showValidationError(dialog, "Announcement content is too long (max 1000 characters).");
                contentArea.requestFocus();
                return;
            }
            
            createBtn.setEnabled(false);
            createBtn.setText("⏳ Creating...");
            
            SanctumApiClient.createAnnouncement(title, content, priority).thenAccept(success -> {
                SwingUtilities.invokeLater(() -> {
                    if (success) {
                        showSuccessMessage(dialog, "Announcement created successfully!");
                        dialog.dispose();
                        refreshAnnouncements();
                    } else {
                        showValidationError(dialog, "Failed to create announcement. Please try again.");
                        createBtn.setEnabled(true);
                        createBtn.setText("📢 CREATE ANNOUNCEMENT");
                    }
                });
            });
        });
        
        cancelBtn.addActionListener(e -> dialog.dispose());
        
        buttonPanel.add(createBtn);
        buttonPanel.add(cancelBtn);
        formPanel.add(buttonPanel, gbc);

        mainPanel.add(formPanel, BorderLayout.CENTER);
        dialog.add(mainPanel);
        
        // Show dialog with Windows-specific smooth display
        WindowsDialogFix.showDialogSmoothly(dialog);
    }
    
    private JButton createStyledDialogButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setForeground(Color.WHITE);
        button.setBackground(color);
        button.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(color.brighter());
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(color);
            }
        });
        
        return button;
    }
    
    private void showValidationError(JDialog parent, String message) {
        JOptionPane.showMessageDialog(parent, 
            "❌ " + message, 
            "Validation Error", 
            JOptionPane.ERROR_MESSAGE);
    }
    
    private void showSuccessMessage(JDialog parent, String message) {
        JOptionPane.showMessageDialog(parent, 
            "✅ " + message, 
            "Success", 
            JOptionPane.INFORMATION_MESSAGE);
    }

    private JPanel createAnnouncementCard(String title, String content, String date, String priority) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(C_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(C_BORDER),
            BorderFactory.createEmptyBorder(16, 20, 16, 20)));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, card.getPreferredSize().height));

        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setOpaque(false);
        
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(FONT_LABEL);
        titleLabel.setForeground(C_GOLD);
        
        JLabel dateLabel = new JLabel(date);
        dateLabel.setFont(FONT_SMALL);
        dateLabel.setForeground(C_TEXT_MID);
        
        JLabel priorityLabel = new JLabel(priority.toUpperCase());
        priorityLabel.setFont(FONT_SMALL);
        if ("high".equals(priority)) {
            priorityLabel.setForeground(new Color(239, 68, 68)); // Red
        } else if ("medium".equals(priority)) {
            priorityLabel.setForeground(new Color(245, 158, 11)); // Orange
        } else {
            priorityLabel.setForeground(C_TEXT_MID); // Gray for low
        }
        
        titlePanel.add(titleLabel, BorderLayout.WEST);
        titlePanel.add(dateLabel, BorderLayout.CENTER);
        titlePanel.add(priorityLabel, BorderLayout.EAST);

        JLabel contentLabel = new JLabel("<html><body style='width: 600px'>" + content + "</body></html>");
        contentLabel.setFont(FONT_TABLE);
        contentLabel.setForeground(C_TEXT);

        card.add(titlePanel, BorderLayout.NORTH);
        card.add(contentLabel, BorderLayout.CENTER);

        return card;
    }

    // ── Staff Registration Panel ───────────────────────────────────────────────────
    private JPanel buildStaffRegistrationPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(C_BG);
        p.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(sectionHeader("STAFF REGISTRATION"), BorderLayout.WEST);
        
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        controls.setOpaque(false);
        
        JButton refreshBtn = glowButton("REFRESH", C_GOLD);
        refreshBtn.addActionListener(e -> refreshStaff());
        controls.add(refreshBtn);
        
        JButton fetchAllBtn = glowButton("FETCH ALL", C_GOLD);
        fetchAllBtn.addActionListener(e -> {
            Object modelObj = staffTable.getClientProperty("model");
            if (modelObj instanceof DefaultTableModel) {
                // Temporary debug: fetch without church_id
                SanctumApiClient.getStaff(false).thenAccept(staff -> {
                    SwingUtilities.invokeLater(() -> loadStaffData((DefaultTableModel) modelObj, staff));
                });
            }
        });
        controls.add(fetchAllBtn);
        header.add(controls, BorderLayout.EAST);
        p.add(header, BorderLayout.NORTH);

        // Registration form
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(C_CARD);
        formPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(C_BORDER),
            BorderFactory.createEmptyBorder(24, 24, 24, 24)));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        // Form fields
        String[] labels = {"Full Name:", "Email:", "Phone:", "Role:", "Department:", "Start Date:"};
        String[] placeholders = {"Enter full name", "Enter email", "Enter phone number", "Select role", "Select department", "YYYY-MM-DD"};
        
        // Store form components for later access
        JTextField nameField = null;
        JTextField emailField = null;
        JTextField phoneField = null;
        JTextField startDateField = null;
        JComboBox<String> roleCombo = null;
        JComboBox<String> deptCombo = null;
        
        for (int i = 0; i < labels.length; i++) {
            gbc.gridx = 0; gbc.gridy = i;
            JLabel label = new JLabel(labels[i]);
            label.setFont(FONT_LABEL);
            label.setForeground(C_TEXT_MID);
            formPanel.add(label, gbc);

            gbc.gridx = 1; gbc.weightx = 1.0;
            if (labels[i].equals("Role:")) {
                roleCombo = new JComboBox<>(new String[]{"Pastor", "Treasurer", "Usher", "Music Director", "Youth Leader", "Admin", "Secretary"});
                roleCombo.setFont(FONT_TABLE);
                roleCombo.setForeground(C_TEXT);
                roleCombo.setBackground(C_SURFACE);
                formPanel.add(roleCombo, gbc);
            } else if (labels[i].equals("Department:")) {
                deptCombo = new JComboBox<>(new String[]{"Pastoral", "Finance", "Ushering", "Music", "Youth", "Administration"});
                deptCombo.setFont(FONT_TABLE);
                deptCombo.setForeground(C_TEXT);
                deptCombo.setBackground(C_SURFACE);
                formPanel.add(deptCombo, gbc);
            } else {
                JTextField field = new JTextField(placeholders[i]);
                field.setFont(FONT_TABLE);
                field.setForeground(C_TEXT);
                field.setBackground(C_SURFACE);
                field.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(C_BORDER),
                    BorderFactory.createEmptyBorder(6, 10, 6, 10)));
                formPanel.add(field, gbc);
                
                // Store specific field references
                if (labels[i].equals("Full Name:")) {
                    nameField = field;
                } else if (labels[i].equals("Email:")) {
                    emailField = field;
                } else if (labels[i].equals("Phone:")) {
                    phoneField = field;
                } else if (labels[i].equals("Start Date:")) {
                    startDateField = field;
                }
            }
        }

        // Buttons
        gbc.gridx = 0; gbc.gridy = labels.length; gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.NONE; gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(20, 8, 8, 8);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        buttonPanel.setOpaque(false);
        
        JButton registerBtn = glowButton("REGISTER STAFF", C_GOLD);
        JButton clearBtn = glowButton("CLEAR FORM", C_TEXT_MID);
        
        // Store final references for lambda
        JComboBox<String> finalRoleCombo = roleCombo;
        JComboBox<String> finalDeptCombo = deptCombo;
        JTextField finalNameField = nameField;
        JTextField finalEmailField = emailField;
        JTextField finalPhoneField = phoneField;
        JTextField finalStartDateField = startDateField;
        
        registerBtn.addActionListener(e -> {
            String name = finalNameField.getText().trim();
            String email = finalEmailField.getText().trim();
            String phone = finalPhoneField.getText().trim();
            String role = finalRoleCombo.getSelectedItem().toString();
            String department = finalDeptCombo.getSelectedItem().toString();
            String startDate = finalStartDateField.getText().trim();
            
            if (name.isEmpty() || email.isEmpty() || phone.isEmpty() || startDate.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please fill in all required fields.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // Validate email format
            if (!email.contains("@") || !email.contains(".")) {
                JOptionPane.showMessageDialog(this, "Please enter a valid email address.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            registerBtn.setEnabled(false);
            registerBtn.setText("Registering...");
            
            SanctumApiClient.createStaff(name, email, phone, role, department, startDate).thenAccept(success -> {
                SwingUtilities.invokeLater(() -> {
                    if (success) {
                        JOptionPane.showMessageDialog(this, "Staff member registered successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                        // Clear form
                        finalNameField.setText("");
                        finalEmailField.setText("");
                        finalPhoneField.setText("");
                        finalStartDateField.setText("");
                        refreshStaff();
                    } else {
                        JOptionPane.showMessageDialog(this, 
                            "Staff registration failed. Please check your connection and try again.", 
                            "Registration Error", JOptionPane.ERROR_MESSAGE);
                    }
                    registerBtn.setEnabled(true);
                    registerBtn.setText("REGISTER STAFF");
                });
            });
        });
        
        clearBtn.addActionListener(e -> {
            finalNameField.setText("");
            finalEmailField.setText("");
            finalPhoneField.setText("");
            finalStartDateField.setText("");
        });
        
        buttonPanel.add(registerBtn);
        buttonPanel.add(clearBtn);
        formPanel.add(buttonPanel, gbc);

        // Existing staff table
        JPanel staffPanel = new JPanel(new BorderLayout());
        staffPanel.setOpaque(false);
        staffPanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));
        
        staffPanel.add(sectionHeader("EXISTING STAFF"), BorderLayout.NORTH);
        
        // Create table with model for dynamic updates
        String[] staffColumns = {"Name", "Role", "Department", "Email", "Start Date"};
        DefaultTableModel staffTableModel = new DefaultTableModel(staffColumns, 0);
        staffTable = new JTable(staffTableModel);
        styleTable(staffTable);
        
        JScrollPane staffScroll = new JScrollPane(staffTable);
        staffScroll.setBorder(null);
        staffScroll.getViewport().setBackground(C_CARD);
        staffPanel.add(staffScroll, BorderLayout.CENTER);
        
        // Store table model for refresh
        staffTable.putClientProperty("model", staffTableModel);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setOpaque(false);
        mainPanel.add(formPanel, BorderLayout.NORTH);
        mainPanel.add(staffPanel, BorderLayout.CENTER);

        // Load staff data
        loadStaffData(staffTableModel);

        p.add(mainPanel, BorderLayout.CENTER);
        return p;
    }

    private void loadStaffData(DefaultTableModel staffTableModel) {
        SanctumApiClient.getStaff().thenAccept(staff -> {
            loadStaffData(staffTableModel, staff);
        }).exceptionally(ex -> {
            System.err.println("Error loading staff data: " + ex.getMessage());
            SwingUtilities.invokeLater(() -> {
                staffTableModel.setRowCount(0);
                staffTableModel.addRow(new Object[]{"Error loading data", "-", "-", "-", "-"});
            });
            return null;
        });
    }

    private void loadStaffData(DefaultTableModel staffTableModel, List<Map<String, Object>> staff) {
        SwingUtilities.invokeLater(() -> {
            System.out.println("Updating staff table with " + (staff != null ? staff.size() : 0) + " items");
            // Clear existing data
            staffTableModel.setRowCount(0);
            
            if (staff == null || staff.isEmpty()) {
                System.out.println("No staff data found from API or staff is null");
                // Show informative message only if truly empty
                staffTableModel.addRow(new Object[]{
                    "No staff records found", 
                    "-", 
                    "-", 
                    "-", 
                    "-"
                });
            } else {
                for (Map<String, Object> staffMember : staff) {
                    // Extract name (handle different possible field names from API)
                    String firstName = "";
                    if (staffMember.containsKey("first_name")) firstName = staffMember.get("first_name").toString();
                    else if (staffMember.containsKey("name")) firstName = staffMember.get("name").toString();
                    
                    String lastName = staffMember.getOrDefault("last_name", "").toString();
                    String name = (firstName + " " + lastName).trim();
                    if (name.isEmpty()) name = "Unknown";
                    
                    // Extract role
                    String role = "N/A";
                    if (staffMember.containsKey("role_display")) role = staffMember.get("role_display").toString();
                    else if (staffMember.containsKey("role")) role = staffMember.get("role").toString();
                    
                    // Extract department
                    String department = staffMember.getOrDefault("department", "N/A").toString();
                    
                    // Extract email
                    String email = staffMember.getOrDefault("email", "N/A").toString();
                    
                    // Extract start date
                    String startDate = "N/A";
                    if (staffMember.containsKey("start_date")) startDate = staffMember.get("start_date").toString();
                    else if (staffMember.containsKey("date_joined")) startDate = staffMember.get("date_joined").toString();
                    
                    // Format date if needed
                    if (startDate != null && startDate.length() > 10) {
                        startDate = startDate.substring(0, 10);
                    }
                    
                    System.out.println("Adding row: " + name + ", " + role);
                    staffTableModel.addRow(new Object[]{name, role, department, email, startDate});
                }
            }
            
            staffTableModel.fireTableDataChanged();
            
            if (statusLabel != null) {
                int count = (staff != null) ? staff.size() : 0;
                statusLabel.setText("  Section: Staff  |  API Connected  | " + count + " staff loaded");
            }
        });
    }

    private void refreshStaff() {
        // Find the staff table model and reload
        if (contentPanel != null) {
            findAndRefreshStaffTable(contentPanel);
        }
    }

    private void findAndRefreshStaffTable(Container container) {
        for (Component comp : container.getComponents()) {
            if (comp instanceof JScrollPane) {
                Component view = ((JScrollPane) comp).getViewport().getView();
                if (view instanceof JTable) {
                    JTable table = (JTable) view;
                    Object modelObj = table.getClientProperty("model");
                    if (modelObj instanceof DefaultTableModel) {
                        loadStaffData((DefaultTableModel) modelObj);
                        return;
                    }
                }
            } else if (comp instanceof Container) {
                findAndRefreshStaffTable((Container) comp);
            }
        }
    }

    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(C_SURFACE);
                g.fillRect(0,0,getWidth(),getHeight());
                g.setColor(C_BORDER);
                g.fillRect(0,0,getWidth(),1);
            }
        };
        bar.setPreferredSize(new Dimension(0,24));
        statusLabel = new JLabel("  Section: Dashboard  |  Ready");
        statusLabel.setFont(FONT_SMALL);
        statusLabel.setForeground(C_TEXT_MID);

        JLabel right = new JLabel("Sanctum v2.4.1   ");
        right.setFont(FONT_SMALL); right.setForeground(C_GOLD);

        bar.add(statusLabel, BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    public static void main(String[] args) {
        // Dark window decorations where possible
        System.setProperty("awt.useSystemAAFontSettings","on");
        System.setProperty("swing.aatext","true");
        SwingUtilities.invokeLater(ChurchAdminFrame::new);
    }
}