package com.sanctum.view;

import com.sanctum.api.SanctumApiClient;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;

/**
 * Usher Dashboard - Attendance Management
 * Deep purple theme matching TreasurerDashboardFrame
 */
public class UsherDashboardFrame extends JFrame {
    
    // ─── Sanctum Brand Color System ───────────────────────────────────────────
    private static final Color C_BG = new Color(14,  46,  42);   // Deep Emerald Green
    private static final Color C_SURFACE = new Color(19,  58,  54);   // Dark Green Secondary
    private static final Color C_CARD = new Color(28,  47,  44);   // Input Background
    private static final Color C_GOLD = new Color(212, 175,  55);  // Gold Accent
    private static final Color C_GOLD_HOVER = new Color(230, 199, 102);  // Light Gold Hover
    private static final Color C_GOLD_DIM = new Color(212, 175,  55, 25);  // Dim Gold
    private static final Color C_TEXT = new Color(255, 255, 255);  // White Text
    private static final Color C_TEXT_MID = new Color(207, 207, 207);  // Soft Gray Secondary
    private static final Color C_TEXT_DIM = new Color(156, 163, 175);  // Dim Text
    private static final Color C_BORDER = new Color(42,  74,  69);   // Border Color
    private static final Color C_SUCCESS = new Color(52, 199, 89);
    private static final Color C_DANGER = new Color(255, 59, 48);

    // ─── Typography ───────────────────────────────────────────────────────
    private static final Font F_TITLE = new Font("Segoe UI", Font.BOLD, 28);
    private static final Font F_LABEL = new Font("Segoe UI", Font.BOLD, 14);
    private static final Font F_MONO_SM = new Font("JetBrains Mono", Font.PLAIN, 11);
    private static final Font F_MONO_LG = new Font("JetBrains Mono", Font.BOLD, 20);

    // ─── UI Components ───────────────────────────────────────────────────
    private JPanel contentArea;
    private CardLayout cardLayout;

    // KPI value labels (updated from API)
    private JLabel lblTotalCheckedIn;
    private JLabel lblTodayAttendance;
    private JLabel lblActiveServices;
    private JLabel lblNewVisitors;

    public UsherDashboardFrame() {
        try {
            configureWindow();
            buildUI();
            loadData();
        } catch (Exception e) {
            System.err.println("Failed to create UsherDashboardFrame: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to create UsherDashboardFrame: " + e.getMessage(), e);
        }
    }

    // ─── Window Setup ────────────────────────────────────────────────────────

    private void configureWindow() {
        setTitle("Sanctum — Usher Dashboard");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setUndecorated(true);
        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        Rectangle bounds = gd.getDefaultConfiguration().getBounds();
        setSize(bounds.width - 100, bounds.height - 100);
        setLocationRelativeTo(null);
        getContentPane().setBackground(C_BG);
    }

    // ─── UI Construction ───────────────────────────────────────────────────────

    private void buildUI() {
        try {
            setLayout(new BorderLayout());

            // Main container
            JPanel main = new JPanel(new BorderLayout()) {
                @Override protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    GradientPaint gp = new GradientPaint(0, 0, C_BG, getWidth(), getHeight(), C_SURFACE);
                    g2.setPaint(gp);
                    g2.fillRect(0, 0, getWidth(), getHeight());
                    g2.dispose();
                }
            };
            main.setOpaque(false);

            // Topbar
            main.add(buildTopBar(), BorderLayout.NORTH);

            // CardLayout for different pages
            cardLayout = new CardLayout();
            contentArea = new JPanel(cardLayout);
            contentArea.setOpaque(false);

            // Add main dashboard page
            contentArea.add(buildMainDashboard(), "dashboard");
            contentArea.add(buildAttendancePage(), "attendance");
            contentArea.add(buildMembersPage(), "members");
            contentArea.add(buildServicesPage(), "services");

            main.add(contentArea, BorderLayout.CENTER);
            add(main);
        } catch (Exception e) {
            System.err.println("Failed to build UI: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to build UI: " + e.getMessage(), e);
        }
    }

    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setColor(C_SURFACE);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(C_BORDER);
                g2.fillRect(0, getHeight() - 1, getWidth(), 1);
                g2.dispose();
            }
        };
        bar.setPreferredSize(new Dimension(0, 60));
        bar.setBorder(new EmptyBorder(10, 20, 10, 20));

        // Left side - Logo and title
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setOpaque(false);
        JLabel icon = new JLabel("⛪");
        icon.setFont(new Font("Arial", Font.PLAIN, 15));
        JLabel title = new JLabel("Sanctum  ·  Usher Dashboard");
        title.setFont(F_MONO_SM);
        title.setForeground(C_TEXT_MID);
        left.add(icon);
        left.add(title);

        // Center - User info
        JPanel center = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        center.setOpaque(false);
        center.add(buildUserInfoCard());

        // Right side - Actions
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);
        JButton minimize = createStyledButton("─", C_GOLD);
        minimize.addActionListener(e -> setState(JFrame.ICONIFIED));
        JButton close = createStyledButton("✕", C_DANGER);
        close.addActionListener(e -> performLogout());
        right.add(minimize);
        right.add(close);

        bar.add(left, BorderLayout.WEST);
        bar.add(center, BorderLayout.CENTER);
        bar.add(right, BorderLayout.EAST);

        return bar;
    }

    private JPanel buildUserInfoCard() {
        JPanel card = new JPanel(new BorderLayout(10, 0));
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(5, 15, 5, 15));

        // Avatar with initials
        JPanel avatar = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(C_GOLD);
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        avatar.setPreferredSize(new Dimension(35, 35));
        avatar.setOpaque(false);

        // Get real user data from API
        Map<String, Object> userData = SanctumApiClient.getCurrentUserData();
        String firstName = userData != null ? userData.getOrDefault("first_name", "User").toString() : "User";
        String lastName = userData != null ? userData.getOrDefault("last_name", "").toString() : "";
        String displayName = lastName.isEmpty() ? firstName : firstName + " " + lastName;
        String initials = (firstName.length() > 0 ? firstName.substring(0, 1) : "U") + 
                         (lastName.length() > 0 ? lastName.substring(0, 1) : "");

        JLabel initialsLabel = new JLabel(initials.toUpperCase());
        initialsLabel.setFont(F_LABEL);
        initialsLabel.setForeground(C_TEXT);
        initialsLabel.setHorizontalAlignment(SwingConstants.CENTER);
        initialsLabel.setVerticalAlignment(SwingConstants.CENTER);
        avatar.add(initialsLabel, BorderLayout.CENTER);

        // User info
        JPanel info = new JPanel();
        info.setOpaque(false);
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        
        JLabel name = new JLabel(displayName);
        name.setFont(F_LABEL);
        name.setForeground(C_TEXT);
        JLabel role = new JLabel("Usher");
        role.setFont(F_MONO_SM);
        role.setForeground(C_TEXT_DIM);
        info.add(name);
        info.add(role);

        card.add(avatar, BorderLayout.WEST);
        card.add(info, BorderLayout.CENTER);

        return card;
    }

    private JPanel buildMainDashboard() {
        try {
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
            JLabel roleLabel = new JLabel("USHER PORTAL");
            roleLabel.setFont(F_MONO_SM);
            roleLabel.setForeground(C_GOLD);
            logoBlock.add(logoLabel);
            logoBlock.add(Box.createHorizontalStrut(10));
            logoBlock.add(roleLabel);

            header.add(logoBlock, BorderLayout.WEST);
            main.add(header, BorderLayout.NORTH);

            // KPI Cards
            JPanel kpiRow = new JPanel(new GridLayout(1, 4, 15, 0));
            kpiRow.setOpaque(false);
            kpiRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));

            // Create KPI cards with placeholders that will be updated by API
            JPanel cardTD = buildKpiCard("Total Checked In", "0", "Loading...", C_GOLD, "✅");
            JPanel cardMD = buildKpiCard("Today's Attendance", "0", "Loading...", C_GOLD_HOVER, "👥");
            JPanel cardTE = buildKpiCard("Active Services", "0", "Loading...", C_TEXT_MID, "⛪");
            JPanel cardNB = buildKpiCard("New Visitors", "0", "Loading...", C_GOLD_DIM, "👋");

            // Store refs for API updates
            lblTotalCheckedIn = findValueLabel(cardTD);
            lblTodayAttendance = findValueLabel(cardMD);
            lblActiveServices = findValueLabel(cardTE);
            lblNewVisitors = findValueLabel(cardNB);

            kpiRow.add(cardTD);
            kpiRow.add(cardMD);
            kpiRow.add(cardTE);
            kpiRow.add(cardNB);

            main.add(kpiRow, BorderLayout.NORTH);

            // Main content area with sidebar
            JPanel contentWrapper = new JPanel(new BorderLayout(20, 0));
            contentWrapper.setOpaque(false);

            // Sidebar navigation
            contentWrapper.add(buildSidebar(), BorderLayout.WEST);

            // Content area
            JPanel content = new JPanel(new BorderLayout(20, 20));
            content.setOpaque(false);

            // Quick actions and recent attendance
            JPanel topRow = new JPanel(new GridLayout(1, 2, 20, 0));
            topRow.setOpaque(false);
            topRow.add(buildQuickActions());
            topRow.add(buildRecentAttendanceSection());

            content.add(topRow, BorderLayout.NORTH);
            content.add(buildAttendanceOverview(), BorderLayout.CENTER);

            contentWrapper.add(content, BorderLayout.CENTER);
            main.add(contentWrapper, BorderLayout.CENTER);
            return main;
        } catch (Exception e) {
            System.err.println("Failed to build main dashboard: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to build main dashboard: " + e.getMessage(), e);
        }
    }

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, C_SURFACE, getWidth(), getHeight(), C_CARD);
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose();
            }
        };
        sidebar.setOpaque(false);
        sidebar.setPreferredSize(new Dimension(200, 0));
        sidebar.setBorder(new EmptyBorder(20, 15, 20, 15));

        // Logo
        JLabel logo = new JLabel("⛪", SwingConstants.CENTER);
        logo.setFont(new Font("Arial", Font.PLAIN, 24));
        logo.setForeground(C_GOLD);
        logo.setBorder(new EmptyBorder(0, 0, 20, 0));

        // Navigation menu
        String[] menuItems = {
            "📊 Dashboard",
            "✅ Attendance",
            "👥 Members", 
            "⛪ Services",
            "🔐 Logout"
        };

        JPanel menu = new JPanel(new GridLayout(menuItems.length, 1, 0, 8));
        menu.setOpaque(false);

        for (String item : menuItems) {
            JButton btn = createSidebarButton(item);
            btn.addActionListener(e -> handleNavigation(item));
            menu.add(btn);
        }

        sidebar.add(logo, BorderLayout.NORTH);
        sidebar.add(menu, BorderLayout.CENTER);

        return sidebar;
    }

    private JPanel buildKpiCard(String title, String value, String subtitle, Color accent, String icon) {
        JPanel card = new JPanel(new BorderLayout(10, 10)) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(C_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(accent);
                g2.fillRoundRect(0, 0, 4, getHeight(), 12, 12);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(15, 20, 15, 20));

        // Top section
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(F_LABEL);
        titleLabel.setForeground(C_TEXT_MID);

        JLabel iconLabel = new JLabel(icon);
        iconLabel.setFont(new Font("Arial", Font.PLAIN, 20));
        iconLabel.setForeground(accent);

        top.add(titleLabel, BorderLayout.WEST);
        top.add(iconLabel, BorderLayout.EAST);

        // Value
        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(F_MONO_LG);
        valueLabel.setForeground(C_TEXT);

        // Subtitle
        JLabel subtitleLabel = new JLabel(subtitle);
        subtitleLabel.setFont(F_MONO_SM);
        subtitleLabel.setForeground(C_TEXT_DIM);

        JPanel bottom = new JPanel(new GridLayout(2, 1));
        bottom.setOpaque(false);
        bottom.add(valueLabel);
        bottom.add(subtitleLabel);

        card.add(top, BorderLayout.NORTH);
        card.add(bottom, BorderLayout.CENTER);

        return card;
    }

    private JPanel buildQuickActions() {
        JPanel panel = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(C_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose();
            }
        };
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("Quick Actions");
        title.setFont(F_LABEL);
        title.setForeground(C_TEXT);
        title.setBorder(new EmptyBorder(0, 0, 15, 0));

        JPanel actions = new JPanel(new GridLayout(2, 2, 10, 10));
        actions.setOpaque(false);

        JButton checkIn = createActionButton("✅ Check In Member", C_SUCCESS);
        checkIn.addActionListener(e -> showCheckInDialog());

        JButton markAbsent = createActionButton("❌ Mark Absent", C_DANGER);
        markAbsent.addActionListener(e -> showMarkAbsentDialog());

        JButton addVisitor = createActionButton("👋 Add Visitor", C_GOLD);
        addVisitor.addActionListener(e -> showAddVisitorDialog());

        JButton viewStats = createActionButton("📊 View Stats", C_GOLD_HOVER);
        viewStats.addActionListener(e -> cardLayout.show(contentArea, "attendance"));

        actions.add(checkIn);
        actions.add(markAbsent);
        actions.add(addVisitor);
        actions.add(viewStats);

        panel.add(title, BorderLayout.NORTH);
        panel.add(actions, BorderLayout.CENTER);

        return panel;
    }

    private JPanel buildRecentAttendanceSection() {
        JPanel panel = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(C_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose();
            }
        };
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("Recent Check-ins");
        title.setFont(F_LABEL);
        title.setForeground(C_TEXT);
        title.setBorder(new EmptyBorder(0, 0, 15, 0));

        // Simple table for recent attendance
        String[] columns = {"Name", "Time", "Status"};
        Object[][] data = {
            {"John Doe", "9:15 AM", "Present"},
            {"Jane Smith", "9:30 AM", "Present"},
            {"Mike Johnson", "9:45 AM", "Late"}
        };

        DefaultTableModel model = new DefaultTableModel(data, columns);
        JTable table = new JTable(model);
        table.setOpaque(false);
        table.getTableHeader().setOpaque(false);
        table.getTableHeader().setBackground(C_SURFACE);
        table.getTableHeader().setForeground(C_TEXT);
        table.setForeground(C_TEXT);
        table.setRowHeight(30);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);

        panel.add(title, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    private JPanel buildAttendanceOverview() {
        JPanel panel = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(C_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose();
            }
        };
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("Today's Attendance Overview");
        title.setFont(F_LABEL);
        title.setForeground(C_TEXT);
        title.setBorder(new EmptyBorder(0, 0, 15, 0));

        // Simple attendance chart placeholder
        JLabel chartPlaceholder = new JLabel("📊 Attendance Chart", SwingConstants.CENTER);
        chartPlaceholder.setFont(F_MONO_LG);
        chartPlaceholder.setForeground(C_TEXT_MID);
        chartPlaceholder.setBorder(new EmptyBorder(50, 20, 50, 20));

        panel.add(title, BorderLayout.NORTH);
        panel.add(chartPlaceholder, BorderLayout.CENTER);

        return panel;
    }

    private JPanel buildAttendancePage() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(30, 30, 30, 30));

        JLabel title = new JLabel("Attendance Management");
        title.setFont(F_TITLE);
        title.setForeground(C_TEXT);
        title.setBorder(new EmptyBorder(0, 0, 30, 0));

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.add(new JLabel("📊 Detailed attendance tracking coming soon..."));

        panel.add(title, BorderLayout.NORTH);
        panel.add(content, BorderLayout.CENTER);

        return panel;
    }

    private JPanel buildMembersPage() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(30, 30, 30, 30));

        JLabel title = new JLabel("Members Directory");
        title.setFont(F_TITLE);
        title.setForeground(C_TEXT);
        title.setBorder(new EmptyBorder(0, 0, 30, 0));

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.add(new JLabel("👥 Members directory coming soon..."));

        panel.add(title, BorderLayout.NORTH);
        panel.add(content, BorderLayout.CENTER);

        return panel;
    }

    private JPanel buildServicesPage() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(30, 30, 30, 30));

        JLabel title = new JLabel("Service Management");
        title.setFont(F_TITLE);
        title.setForeground(C_TEXT);
        title.setBorder(new EmptyBorder(0, 0, 30, 0));

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.add(new JLabel("⛪ Service management coming soon..."));

        panel.add(title, BorderLayout.NORTH);
        panel.add(content, BorderLayout.CENTER);

        return panel;
    }

    // ─── Helper Methods ─────────────────────────────────────────────────────

    private JButton createStyledButton(String text, Color color) {
        JButton btn = new JButton(text);
        btn.setFont(F_MONO_SM);
        btn.setForeground(color);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                btn.setForeground(btn.getForeground().brighter());
            }
            public void mouseExited(MouseEvent e) {
                btn.setForeground(color);
            }
        });
        return btn;
    }

    private JButton createSidebarButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(F_MONO_SM);
        btn.setForeground(C_TEXT_MID);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(12, 15, 12, 15));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(C_GOLD);
                btn.setOpaque(true);
                btn.setForeground(C_TEXT);
            }
            public void mouseExited(MouseEvent e) {
                btn.setOpaque(false);
                btn.setForeground(C_TEXT_MID);
            }
        });
        return btn;
    }

    private JButton createActionButton(String text, Color color) {
        JButton btn = new JButton(text);
        btn.setFont(F_MONO_SM);
        btn.setForeground(C_TEXT);
        btn.setBackground(color);
        btn.setOpaque(true);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(15, 10, 15, 10));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(color.brighter());
            }
            public void mouseExited(MouseEvent e) {
                btn.setBackground(color);
            }
        });
        return btn;
    }

    private JLabel findValueLabel(JPanel card) {
        for (Component comp : card.getComponents()) {
            if (comp instanceof JPanel) {
                JPanel panel = (JPanel) comp;
                for (Component subComp : panel.getComponents()) {
                    if (subComp instanceof JLabel) {
                        JLabel label = (JLabel) subComp;
                        if (label.getFont().equals(F_MONO_LG)) {
                            return label;
                        }
                    }
                }
            }
        }
        return null;
    }

    // ─── Navigation & Actions ─────────────────────────────────────────────────

    private void handleNavigation(String item) {
        switch (item) {
            case "📊 Dashboard":
                cardLayout.show(contentArea, "dashboard");
                break;
            case "✅ Attendance":
                cardLayout.show(contentArea, "attendance");
                break;
            case "👥 Members":
                cardLayout.show(contentArea, "members");
                break;
            case "⛪ Services":
                cardLayout.show(contentArea, "services");
                break;
            case "🔐 Logout":
                performLogout();
                break;
        }
    }

    private void performLogout() {
        // Clear authentication and return to login
        SanctumApiClient.setAuthToken(null);
        SanctumApiClient.setCurrentUserData(null);
        dispose();
        SwingUtilities.invokeLater(() -> {
            LoginFrame login = new LoginFrame();
            login.setVisible(true);
        });
    }

    private void showCheckInDialog() {
        JOptionPane.showMessageDialog(this, 
            "✅ Member Check-In\n\nFeature coming soon!", 
            "Check In Member", 
            JOptionPane.INFORMATION_MESSAGE);
    }

    private void showMarkAbsentDialog() {
        JOptionPane.showMessageDialog(this, 
            "❌ Mark Member Absent\n\nFeature coming soon!", 
            "Mark Absent", 
            JOptionPane.INFORMATION_MESSAGE);
    }

    private void showAddVisitorDialog() {
        JOptionPane.showMessageDialog(this, 
            "👋 Add New Visitor\n\nFeature coming soon!", 
            "Add Visitor", 
            JOptionPane.INFORMATION_MESSAGE);
    }

    // ─── Data Loading ────────────────────────────────────────────────────────

    private void loadData() {
        try {
            // For now, use placeholder data until getAttendanceData API is implemented
            // TODO: Replace with actual API call when available
            Map<String, Object> attendanceData = new HashMap<>();
            attendanceData.put("total_checked_in", "45");
            attendanceData.put("today_attendance", "38");
            attendanceData.put("active_services", "2");
            attendanceData.put("new_visitors", "5");
            
            // Update KPI cards with data
            if (lblTotalCheckedIn != null) {
                lblTotalCheckedIn.setText(String.valueOf(attendanceData.getOrDefault("total_checked_in", "0")));
            }
            if (lblTodayAttendance != null) {
                lblTodayAttendance.setText(String.valueOf(attendanceData.getOrDefault("today_attendance", "0")));
            }
            if (lblActiveServices != null) {
                lblActiveServices.setText(String.valueOf(attendanceData.getOrDefault("active_services", "0")));
            }
            if (lblNewVisitors != null) {
                lblNewVisitors.setText(String.valueOf(attendanceData.getOrDefault("new_visitors", "0")));
            }
        } catch (Exception e) {
            System.err.println("Failed to load attendance data: " + e.getMessage());
            // Keep default values if loading fails
        }
    }
}
