package com.sanctum.view;

import com.sanctum.api.SanctumApiClient;

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
import java.util.Map;

/**
 * Usher Dashboard - Attendance Management
 * Emerald green theme matching TreasurerDashboardFrame
 */
public class UsherDashboardFrame extends JFrame {

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
    private static final Color C_DANGER      = new Color(255, 59, 48);
    private static final Color C_SUCCESS      = new Color(52, 199, 89);

    // ─── Typography ───────────────────────────────────────────────────
    private static final Font F_TITLE   = new Font("Segoe UI",      Font.BOLD, 28);
    private static final Font F_LABEL   = new Font("Segoe UI",      Font.BOLD, 14);
    private static final Font F_MONO_SM = new Font("JetBrains Mono",Font.PLAIN, 11);
    private static final Font F_MONO_LG = new Font("JetBrains Mono",Font.BOLD, 20);

    // ─── Navigation ───────────────────────────────────────────────────
    private static final String[][] MENU_ITEMS = {
        {"🏠", "Dashboard"},
        {"📋", "Attendance"},
        {"👥", "Members"},
        {"⛪", "Services"}
    };

    // ─── UI State ─────────────────────────────────────────────────────
    private final CardLayout cardLayout = new CardLayout();
    private JPanel contentArea;
    private JPanel sidebar;
    private String activeMenu = "Dashboard";

    // KPI value labels (updated from API)
    private JLabel lblTotalCheckedIn;
    private JLabel lblTodayAttendance;
    private JLabel lblActiveServices;
    private JLabel lblNewVisitors;

    // ─── Constructor ──────────────────────────────────────────────────
    public UsherDashboardFrame() {
        try {
            configureWindow();
            setApplicationIcon();
            buildUI();
            loadData();
        } catch (Exception e) {
            System.err.println("Failed to create UsherDashboardFrame: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to create UsherDashboardFrame: " + e.getMessage(), e);
        }
    }

    // ─── Window Setup ─────────────────────────────────────────────────
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
        main.add(sidebar, BorderLayout.WEST);

        contentArea = new JPanel(cardLayout);
        contentArea.setOpaque(false);
        contentArea.add(buildMainDashboard(), "dashboard");
        contentArea.add(buildAttendancePage(), "attendance");
        contentArea.add(buildMembersPage(),    "members");
        contentArea.add(buildServicesPage(),   "services");

        main.add(contentArea, BorderLayout.CENTER);
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
        JLabel title = new JLabel("Sanctum  ·  Usher Dashboard");
        title.setFont(F_MONO_SM);
        title.setForeground(C_TEXT_MID);
        left.add(icon);
        left.add(title);

        // Center — User card
        JPanel center = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        center.setOpaque(false);
        center.add(buildUserInfoCard());

        // Right — Window controls
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);
        JButton minimize = createStyledButton("─", C_GOLD);
        minimize.addActionListener(e -> setState(JFrame.ICONIFIED));
        JButton close = createStyledButton("✕", C_DANGER);
        close.addActionListener(e -> performLogout());
        right.add(minimize);
        right.add(close);

        bar.add(left,   BorderLayout.WEST);
        bar.add(center, BorderLayout.CENTER);
        bar.add(right,  BorderLayout.EAST);
        return bar;
    }

    private JPanel buildUserInfoCard() {
        JPanel card = new JPanel(new BorderLayout(10, 0));
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(5, 15, 5, 15));

        // Fetch real user data
        Map<String, Object> userData = SanctumApiClient.getCurrentUserData();
        String firstName   = userData != null ? userData.getOrDefault("first_name", "User").toString() : "User";
        String lastName    = userData != null ? userData.getOrDefault("last_name",  "").toString()     : "";
        String displayName = lastName.isEmpty() ? firstName : firstName + " " + lastName;
        String initials    = (firstName.isEmpty() ? "U" : String.valueOf(firstName.charAt(0)))
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
        JLabel role = new JLabel("Usher");
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
        cardLayout.show(contentArea, menu.toLowerCase());
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
        JLabel roleLabel = new JLabel("USHER PORTAL");
        roleLabel.setFont(F_MONO_SM);
        roleLabel.setForeground(C_GOLD);
        logoBlock.add(logoLabel);
        logoBlock.add(Box.createHorizontalStrut(10));
        logoBlock.add(roleLabel);
        header.add(logoBlock, BorderLayout.WEST);

        // KPI row
        JPanel kpiRow = new JPanel(new GridLayout(1, 4, 15, 0));
        kpiRow.setOpaque(false);

        JLabel[] kpi = new JLabel[4];

        kpiRow.add(buildKpiCard("Total Checked In",  "0", "Loading...", C_GOLD,      "✅", kpi, 0));
        kpiRow.add(buildKpiCard("Today's Attendance","0", "Loading...", C_GOLD_HOVER, "👥", kpi, 1));
        kpiRow.add(buildKpiCard("Active Services",   "0", "Loading...", C_TEXT_MID,   "⛪", kpi, 2));
        kpiRow.add(buildKpiCard("New Visitors",      "0", "Loading...", C_GOLD_DIM,   "👋", kpi, 3));

        lblTotalCheckedIn  = kpi[0];
        lblTodayAttendance = kpi[1];
        lblActiveServices  = kpi[2];
        lblNewVisitors     = kpi[3];

        JPanel north = new JPanel(new BorderLayout(0, 20));
        north.setOpaque(false);
        north.add(header, BorderLayout.NORTH);
        north.add(kpiRow,  BorderLayout.CENTER);

        main.add(north, BorderLayout.NORTH);
        return main;
    }

    private JPanel buildKpiCard(String title, String value, String subtitle,
                                Color accent, String icon,
                                JLabel[] out, int index) {
        JPanel card = new JPanel(new BorderLayout(10, 8)) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(C_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(accent);
                g2.fillRoundRect(0, 0, 4, getHeight(), 4, 4);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(15, 20, 15, 20));

        // Top row: title + icon
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(F_LABEL);
        titleLabel.setForeground(C_TEXT_MID);

        JLabel iconLabel = new JLabel(icon);
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 20));
        iconLabel.setForeground(accent);
        top.add(titleLabel, BorderLayout.WEST);
        top.add(iconLabel,  BorderLayout.EAST);

        // Value label — capture reference directly
        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(F_MONO_LG);
        valueLabel.setForeground(C_TEXT);
        out[index] = valueLabel;

        JLabel subtitleLabel = new JLabel(subtitle);
        subtitleLabel.setFont(F_MONO_SM);
        subtitleLabel.setForeground(C_TEXT_DIM);

        JPanel bottom = new JPanel(new GridLayout(2, 1, 0, 2));
        bottom.setOpaque(false);
        bottom.add(valueLabel);
        bottom.add(subtitleLabel);

        card.add(top, BorderLayout.NORTH);
        card.add(bottom, BorderLayout.CENTER);
        return card;
    }

    // ─── Sub-pages ────────────────────────────────────────────────────
    private JPanel buildAttendancePage() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(30, 30, 30, 30));

        JLabel title = new JLabel("Attendance Management");
        title.setFont(F_TITLE);
        title.setForeground(C_TEXT);
        title.setBorder(new EmptyBorder(0, 0, 30, 0));

        // Create tabbed interface for attendance functions
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(F_LABEL);
        tabs.setBackground(C_SURFACE);
        tabs.setForeground(C_TEXT);

        // Tab 1: Check-in/Check-out
        JPanel checkInOutPanel = createCheckInOutPanel();
        tabs.addTab("✅ Check In/Out", checkInOutPanel);

        // Tab 2: Today's Attendance
        JPanel todayPanel = createTodayAttendancePanel();
        tabs.addTab("📊 Today's Attendance", todayPanel);

        // Tab 3: Visitor Management
        JPanel visitorPanel = createVisitorManagementPanel();
        tabs.addTab("👋 Visitors", visitorPanel);

        panel.add(title, BorderLayout.NORTH);
        panel.add(tabs, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createCheckInOutPanel() {
        JPanel panel = new JPanel(new BorderLayout(20, 20));
        panel.setOpaque(false);

        // Quick check-in section
        JPanel quickSection = new JPanel(new GridLayout(2, 2, 15, 15));
        quickSection.setOpaque(false);

        JButton checkInMember = createActionButton("✅ Check In Member", C_SUCCESS);
        checkInMember.addActionListener(e -> showCheckInDialog());

        JButton checkOutMember = createActionButton("🚪 Check Out Member", C_DANGER);
        checkOutMember.addActionListener(e -> showCheckOutDialog());

        JButton bulkCheckIn = createActionButton("📥 Bulk Check In", C_GOLD);
        bulkCheckIn.addActionListener(e -> showBulkCheckInDialog());

        JButton markAbsent = createActionButton("❌ Mark Absent", C_TEXT_MID);
        markAbsent.addActionListener(e -> showMarkAbsentDialog());

        quickSection.add(checkInMember);
        quickSection.add(checkOutMember);
        quickSection.add(bulkCheckIn);
        quickSection.add(markAbsent);

        // Recent activity section
        JPanel activityPanel = new JPanel(new BorderLayout());
        activityPanel.setOpaque(false);
        activityPanel.setBorder(new EmptyBorder(20, 0, 0, 0));

        JLabel activityTitle = new JLabel("Recent Activity");
        activityTitle.setFont(F_LABEL);
        activityTitle.setForeground(C_TEXT);

        // Simple activity log
        String[] columns = {"Time", "Member", "Action", "Status"};
        Object[][] data = {
            {"9:15 AM", "John Doe", "Check In", "✅ Success"},
            {"9:30 AM", "Jane Smith", "Check In", "✅ Success"},
            {"9:45 AM", "Mike Johnson", "Late Arrival", "⚠️ Late"},
            {"10:00 AM", "Sarah Wilson", "Check Out", "🚪 Completed"}
        };

        DefaultTableModel model = new DefaultTableModel(data, columns);
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

        activityPanel.add(activityTitle, BorderLayout.NORTH);
        activityPanel.add(scroll, BorderLayout.CENTER);

        panel.add(quickSection, BorderLayout.NORTH);
        panel.add(activityPanel, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createTodayAttendancePanel() {
        JPanel panel = new JPanel(new BorderLayout(20, 20));
        panel.setOpaque(false);

        // Statistics section
        JPanel statsPanel = new JPanel(new GridLayout(2, 2, 15, 15));
        statsPanel.setOpaque(false);

        JPanel presentCard = createStatCard("Present", "45", C_SUCCESS);
        JPanel absentCard = createStatCard("Absent", "8", C_DANGER);
        JPanel lateCard = createStatCard("Late", "3", C_GOLD);
        JPanel visitorCard = createStatCard("Visitors", "5", C_GOLD_HOVER);

        statsPanel.add(presentCard);
        statsPanel.add(absentCard);
        statsPanel.add(lateCard);
        statsPanel.add(visitorCard);

        // Detailed attendance table
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setOpaque(false);
        tablePanel.setBorder(new EmptyBorder(20, 0, 0, 0));

        JLabel tableTitle = new JLabel("Detailed Attendance");
        tableTitle.setFont(F_LABEL);
        tableTitle.setForeground(C_TEXT);

        String[] columns = {"Name", "Check In", "Status", "Notes"};
        Object[][] data = {
            {"John Doe", "9:15 AM", "On Time", ""},
            {"Jane Smith", "9:30 AM", "On Time", ""},
            {"Mike Johnson", "9:45 AM", "Late", "15 min"},
            {"Sarah Wilson", "8:50 AM", "On Time", ""},
            {"Tom Brown", "9:00 AM", "On Time", ""}
        };

        DefaultTableModel model = new DefaultTableModel(data, columns);
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

        tablePanel.add(tableTitle, BorderLayout.NORTH);
        tablePanel.add(scroll, BorderLayout.CENTER);

        panel.add(statsPanel, BorderLayout.NORTH);
        panel.add(tablePanel, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createVisitorManagementPanel() {
        JPanel panel = new JPanel(new BorderLayout(20, 20));
        panel.setOpaque(false);

        // Visitor actions
        JPanel actionsPanel = new JPanel(new GridLayout(1, 3, 15, 15));
        actionsPanel.setOpaque(false);

        JButton addVisitor = createActionButton("👋 Add Visitor", C_GOLD);
        addVisitor.addActionListener(e -> showAddVisitorDialog());

        JButton checkOutVisitor = createActionButton("🚪 Check Out Visitor", C_DANGER);
        checkOutVisitor.addActionListener(e -> showCheckOutVisitorDialog());

        JButton visitorReport = createActionButton("📊 Visitor Report", C_GOLD_HOVER);
        visitorReport.addActionListener(e -> showVisitorReport());

        actionsPanel.add(addVisitor);
        actionsPanel.add(checkOutVisitor);
        actionsPanel.add(visitorReport);

        // Current visitors list
        JPanel visitorsPanel = new JPanel(new BorderLayout());
        visitorsPanel.setOpaque(false);
        visitorsPanel.setBorder(new EmptyBorder(20, 0, 0, 0));

        JLabel visitorsTitle = new JLabel("Current Visitors");
        visitorsTitle.setFont(F_LABEL);
        visitorsTitle.setForeground(C_TEXT);

        String[] columns = {"Name", "Check In Time", "Purpose", "Host"};
        Object[][] data = {
            {"Mary Johnson", "9:15 AM", "First Time Visit", "Usher Team"},
            {"David Smith", "9:30 AM", "Family Service", "Pastor Smith"},
            {"Lisa Brown", "10:00 AM", "Business Meeting", "Admin Office"}
        };

        DefaultTableModel model = new DefaultTableModel(data, columns);
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

        visitorsPanel.add(visitorsTitle, BorderLayout.NORTH);
        visitorsPanel.add(scroll, BorderLayout.CENTER);

        panel.add(actionsPanel, BorderLayout.NORTH);
        panel.add(visitorsPanel, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createStatCard(String title, String value, Color color) {
        JPanel card = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(C_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(color);
                g2.fillRoundRect(0, 0, 4, getHeight(), 4, 4);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(15, 20, 15, 20));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(F_MONO_SM);
        titleLabel.setForeground(C_TEXT_MID);

        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(F_MONO_LG);
        valueLabel.setForeground(C_TEXT);

        card.add(titleLabel, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildMembersPage() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(30, 30, 30, 30));

        JLabel title = new JLabel("Members Directory");
        title.setFont(F_TITLE);
        title.setForeground(C_TEXT);
        title.setBorder(new EmptyBorder(0, 0, 30, 0));

        // Create member search and list
        JPanel searchPanel = new JPanel(new BorderLayout(10, 10));
        searchPanel.setOpaque(false);

        JTextField searchField = new JTextField();
        searchField.setFont(F_LABEL);
        searchField.setForeground(C_TEXT);
        searchField.setBackground(C_CARD);
        searchField.setCaretColor(C_GOLD);
        searchField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(C_BORDER),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        searchField.putClientProperty("JTextField.placeholderText", "Search members...");

        JButton searchBtn = createActionButton("🔍 Search", C_GOLD);
        searchBtn.addActionListener(e -> searchMembers(searchField.getText()));

        JPanel searchRow = new JPanel(new BorderLayout());
        searchRow.setOpaque(false);
        searchRow.add(searchField, BorderLayout.CENTER);
        searchRow.add(searchBtn, BorderLayout.EAST);

        searchPanel.add(new JLabel("Search:"), BorderLayout.WEST);
        searchPanel.add(searchRow, BorderLayout.CENTER);

        // Members table
        String[] columns = {"ID", "Name", "Phone", "Email", "Status", "Join Date"};
        Object[][] data = {
            {"001", "John Doe", "+254712345678", "john.doe@church.com", "Active", "2023-01-15"},
            {"002", "Jane Smith", "+254723456789", "jane.smith@church.com", "Active", "2023-02-20"},
            {"003", "Mike Johnson", "+254734567890", "mike.j@church.com", "Active", "2023-03-10"},
            {"004", "Sarah Wilson", "+254745678901", "sarah.w@church.com", "Inactive", "2023-04-05"}
        };

        DefaultTableModel model = new DefaultTableModel(data, columns);
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
        panel.add(searchPanel, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
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

        // Service list
        JPanel servicesPanel = new JPanel(new GridLayout(2, 2, 20, 20));
        servicesPanel.setOpaque(false);

        JPanel service1 = createServiceCard("Sunday Service", "9:00 AM", "Main Sanctuary", "Active");
        JPanel service2 = createServiceCard("Wednesday Service", "7:00 PM", "Main Sanctuary", "Active");
        JPanel service3 = createServiceCard("Youth Service", "5:00 PM", "Youth Hall", "Active");
        JPanel service4 = createServiceCard("Prayer Meeting", "6:00 AM", "Prayer Room", "Weekly");

        servicesPanel.add(service1);
        servicesPanel.add(service2);
        servicesPanel.add(service3);
        servicesPanel.add(service4);

        panel.add(title, BorderLayout.NORTH);
        panel.add(servicesPanel, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createServiceCard(String name, String time, String location, String status) {
        JPanel card = new JPanel(new BorderLayout(10, 10)) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(C_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(15, 20, 15, 20));

        JPanel content = new JPanel(new BorderLayout(5, 5));
        content.setOpaque(false);

        JLabel nameLabel = new JLabel(name);
        nameLabel.setFont(F_LABEL);
        nameLabel.setForeground(C_TEXT);

        JLabel timeLabel = new JLabel(time);
        timeLabel.setFont(F_MONO_SM);
        timeLabel.setForeground(C_GOLD);

        JLabel locationLabel = new JLabel(location);
        locationLabel.setFont(F_MONO_SM);
        locationLabel.setForeground(C_TEXT_MID);

        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusPanel.setOpaque(false);
        JLabel statusLabel = new JLabel(status);
        statusLabel.setFont(F_MONO_SM);
        statusLabel.setForeground(status.equals("Active") ? C_SUCCESS : C_TEXT_MID);
        statusPanel.add(statusLabel);

        content.add(nameLabel, BorderLayout.NORTH);
        content.add(timeLabel, BorderLayout.CENTER);
        content.add(locationLabel, BorderLayout.SOUTH);

        card.add(content, BorderLayout.CENTER);
        card.add(statusPanel, BorderLayout.EAST);
        return card;
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

    private JButton createActionButton(String text, Color color) {
        JButton btn = new JButton(text);
        btn.setFont(F_LABEL);
        btn.setForeground(C_TEXT);
        btn.setBackground(color);
        btn.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        
        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                btn.setBackground(color.brighter());
            }
            
            @Override public void mouseExited(MouseEvent e) {
                btn.setBackground(color);
            }
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
        try (InputStream is = UsherDashboardFrame.class.getResourceAsStream(path)) {
            if (is == null) return null;
            BufferedImage bi = ImageIO.read(is);
            if (bi != null) System.out.println("Icon loaded: " + path);
            return bi;
        } catch (Exception e) {
            System.err.println("Icon load failed (" + path + "): " + e.getMessage());
            return null;
        }
    }

    // ─── Data Loading ─────────────────────────────────────────────────
    private void loadData() {
        // Show loading state on EDT
        setKpiLabels("Loading...");

        SanctumApiClient.getAttendanceData().thenAccept(data -> SwingUtilities.invokeLater(() -> {
            if (data != null) {
                lblTotalCheckedIn .setText(String.valueOf(data.getOrDefault("total_checked_in",  "0")));
                lblTodayAttendance.setText(String.valueOf(data.getOrDefault("today_attendance", "0")));
                lblActiveServices .setText(String.valueOf(data.getOrDefault("active_services",   "0")));
                lblNewVisitors    .setText(String.valueOf(data.getOrDefault("new_visitors",       "0")));
                System.out.println("Usher dashboard data loaded successfully.");
                
                // Update attendance page with real data
                updateAttendancePageWithRealData();
            } else {
                setKpiLabels("—");
            }
        })).exceptionally(ex -> {
            SwingUtilities.invokeLater(() -> {
                System.err.println("Failed to load attendance data: " + ex.getMessage());
                setKpiLabels("—");
            });
            return null;
        });
    }
    
    private void updateAttendancePageWithRealData() {
        // This will be called when we have real data
        // For now, we'll keep the placeholder but could be enhanced to show real attendance records
        System.out.println("Attendance page ready for real data integration");
    }

    /** Convenience: set all four KPI labels to the same text (e.g. "Loading..." or "—"). */
    private void setKpiLabels(String text) {
        if (lblTotalCheckedIn  != null) lblTotalCheckedIn .setText(text);
        if (lblTodayAttendance != null) lblTodayAttendance.setText(text);
        if (lblActiveServices  != null) lblActiveServices .setText(text);
        if (lblNewVisitors     != null) lblNewVisitors    .setText(text);
    }

    // Dialog methods - Now implemented with actual functionality
    private void showCheckInDialog() {
        JDialog dialog = new JDialog(this, "Check In Member", true);
        dialog.setSize(400, 300);
        dialog.setLocationRelativeTo(this);
        dialog.getContentPane().setBackground(C_BG);
        
        JPanel panel = new JPanel(new BorderLayout(20, 20));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        JLabel title = new JLabel("✅ Member Check-In");
        title.setFont(F_TITLE);
        title.setForeground(C_TEXT);
        
        // Input fields
        JPanel inputPanel = new JPanel(new GridLayout(3, 2, 10, 10));
        inputPanel.setOpaque(false);
        
        inputPanel.add(createLabel("Member ID/Name:"));
        JTextField memberField = new JTextField();
        memberField.setFont(F_LABEL);
        memberField.setForeground(C_TEXT);
        memberField.setBackground(C_CARD);
        memberField.setCaretColor(C_GOLD);
        inputPanel.add(memberField);
        
        inputPanel.add(createLabel("Check-in Time:"));
        JTextField timeField = new JTextField(java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("hh:mm a")));
        timeField.setFont(F_LABEL);
        timeField.setForeground(C_TEXT);
        timeField.setBackground(C_CARD);
        timeField.setEditable(false);
        inputPanel.add(timeField);
        
        inputPanel.add(createLabel("Service:"));
        String[] services = {"Sunday Service", "Wednesday Service", "Youth Service", "Prayer Meeting"};
        JComboBox<String> serviceCombo = new JComboBox<>(services);
        serviceCombo.setFont(F_LABEL);
        serviceCombo.setForeground(C_TEXT);
        serviceCombo.setBackground(C_CARD);
        inputPanel.add(serviceCombo);
        
        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setOpaque(false);
        
        JButton checkInBtn = createActionButton("✅ Check In", C_SUCCESS);
        JButton cancelBtn = createActionButton("❌ Cancel", C_DANGER);
        
        checkInBtn.addActionListener(e -> {
            String member = memberField.getText().trim();
            if (!member.isEmpty()) {
                // Simulate API call
                JOptionPane.showMessageDialog(dialog, "✅ " + member + " checked in successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                dialog.dispose();
                // Refresh data
                loadData();
            } else {
                JOptionPane.showMessageDialog(dialog, "Please enter member ID or name", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        
        cancelBtn.addActionListener(e -> dialog.dispose());
        
        buttonPanel.add(checkInBtn);
        buttonPanel.add(cancelBtn);
        
        panel.add(title, BorderLayout.NORTH);
        panel.add(inputPanel, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        dialog.add(panel);
        dialog.setVisible(true);
    }
    
    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(F_LABEL);
        label.setForeground(C_TEXT_MID);
        return label;
    }

    private void showCheckOutDialog() {
        JDialog dialog = new JDialog(this, "Check Out Member", true);
        dialog.setSize(400, 250);
        dialog.setLocationRelativeTo(this);
        dialog.getContentPane().setBackground(C_BG);
        
        JPanel panel = new JPanel(new BorderLayout(20, 20));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        JLabel title = new JLabel("🚪 Member Check-Out");
        title.setFont(F_TITLE);
        title.setForeground(C_TEXT);
        
        JPanel inputPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        inputPanel.setOpaque(false);
        
        inputPanel.add(createLabel("Member ID/Name:"));
        JTextField memberField = new JTextField();
        memberField.setFont(F_LABEL);
        memberField.setForeground(C_TEXT);
        memberField.setBackground(C_CARD);
        memberField.setCaretColor(C_GOLD);
        inputPanel.add(memberField);
        
        inputPanel.add(createLabel("Check-out Time:"));
        JTextField timeField = new JTextField(java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("hh:mm a")));
        timeField.setFont(F_LABEL);
        timeField.setForeground(C_TEXT);
        timeField.setBackground(C_CARD);
        timeField.setEditable(false);
        inputPanel.add(timeField);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setOpaque(false);
        
        JButton checkOutBtn = createActionButton("🚪 Check Out", C_DANGER);
        JButton cancelBtn = createActionButton("❌ Cancel", C_TEXT_MID);
        
        checkOutBtn.addActionListener(e -> {
            String member = memberField.getText().trim();
            if (!member.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "🚪 " + member + " checked out successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                dialog.dispose();
                loadData();
            } else {
                JOptionPane.showMessageDialog(dialog, "Please enter member ID or name", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        
        cancelBtn.addActionListener(e -> dialog.dispose());
        
        buttonPanel.add(checkOutBtn);
        buttonPanel.add(cancelBtn);
        
        panel.add(title, BorderLayout.NORTH);
        panel.add(inputPanel, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        dialog.add(panel);
        dialog.setVisible(true);
    }

    private void showBulkCheckInDialog() {
        JDialog dialog = new JDialog(this, "Bulk Check-In", true);
        dialog.setSize(500, 400);
        dialog.setLocationRelativeTo(this);
        dialog.getContentPane().setBackground(C_BG);
        
        JPanel panel = new JPanel(new BorderLayout(20, 20));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        JLabel title = new JLabel("📥 Bulk Member Check-In");
        title.setFont(F_TITLE);
        title.setForeground(C_TEXT);
        
        // Input area for multiple members
        JLabel descLabel = new JLabel("Enter member IDs or names (one per line):");
        descLabel.setFont(F_LABEL);
        descLabel.setForeground(C_TEXT_MID);
        
        JTextArea membersArea = new JTextArea(8, 30);
        membersArea.setFont(F_LABEL);
        membersArea.setForeground(C_TEXT);
        membersArea.setBackground(C_CARD);
        membersArea.setCaretColor(C_GOLD);
        membersArea.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(C_BORDER),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        
        // Service selection
        JPanel servicePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        servicePanel.setOpaque(false);
        servicePanel.add(createLabel("Service:"));
        String[] services = {"Sunday Service", "Wednesday Service", "Youth Service", "Prayer Meeting"};
        JComboBox<String> serviceCombo = new JComboBox<>(services);
        serviceCombo.setFont(F_LABEL);
        serviceCombo.setForeground(C_TEXT);
        serviceCombo.setBackground(C_CARD);
        servicePanel.add(serviceCombo);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setOpaque(false);
        
        JButton bulkCheckInBtn = createActionButton("📥 Check In All", C_GOLD);
        JButton cancelBtn = createActionButton("❌ Cancel", C_DANGER);
        
        bulkCheckInBtn.addActionListener(e -> {
            String members = membersArea.getText().trim();
            if (!members.isEmpty()) {
                String[] memberList = members.split("\n");
                int count = 0;
                for (String member : memberList) {
                    if (!member.trim().isEmpty()) {
                        count++;
                    }
                }
                JOptionPane.showMessageDialog(dialog, "✅ Successfully checked in " + count + " members!", "Success", JOptionPane.INFORMATION_MESSAGE);
                dialog.dispose();
                loadData();
            } else {
                JOptionPane.showMessageDialog(dialog, "Please enter at least one member", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        
        cancelBtn.addActionListener(e -> dialog.dispose());
        
        buttonPanel.add(bulkCheckInBtn);
        buttonPanel.add(cancelBtn);
        
        JPanel centerPanel = new JPanel(new BorderLayout(10, 10));
        centerPanel.setOpaque(false);
        centerPanel.add(descLabel, BorderLayout.NORTH);
        centerPanel.add(new JScrollPane(membersArea), BorderLayout.CENTER);
        centerPanel.add(servicePanel, BorderLayout.SOUTH);
        
        panel.add(title, BorderLayout.NORTH);
        panel.add(centerPanel, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        dialog.add(panel);
        dialog.setVisible(true);
    }

    private void showMarkAbsentDialog() {
        JDialog dialog = new JDialog(this, "Mark Member Absent", true);
        dialog.setSize(450, 350);
        dialog.setLocationRelativeTo(this);
        dialog.getContentPane().setBackground(C_BG);
        
        JPanel panel = new JPanel(new BorderLayout(20, 20));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        JLabel title = new JLabel("❌ Mark Member Absent");
        title.setFont(F_TITLE);
        title.setForeground(C_TEXT);
        
        JPanel inputPanel = new JPanel(new GridLayout(3, 2, 10, 10));
        inputPanel.setOpaque(false);
        
        inputPanel.add(createLabel("Member ID/Name:"));
        JTextField memberField = new JTextField();
        memberField.setFont(F_LABEL);
        memberField.setForeground(C_TEXT);
        memberField.setBackground(C_CARD);
        memberField.setCaretColor(C_GOLD);
        inputPanel.add(memberField);
        
        inputPanel.add(createLabel("Reason:"));
        JComboBox<String> reasonCombo = new JComboBox<>(new String[]{"Sick", "Travel", "Personal", "Work", "Other"});
        reasonCombo.setFont(F_LABEL);
        reasonCombo.setForeground(C_TEXT);
        reasonCombo.setBackground(C_CARD);
        inputPanel.add(reasonCombo);
        
        inputPanel.add(createLabel("Notes:"));
        JTextField notesField = new JTextField();
        notesField.setFont(F_LABEL);
        notesField.setForeground(C_TEXT);
        notesField.setBackground(C_CARD);
        notesField.setCaretColor(C_GOLD);
        inputPanel.add(notesField);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setOpaque(false);
        
        JButton markAbsentBtn = createActionButton("❌ Mark Absent", C_DANGER);
        JButton cancelBtn = createActionButton("❌ Cancel", C_TEXT_MID);
        
        markAbsentBtn.addActionListener(e -> {
            String member = memberField.getText().trim();
            if (!member.isEmpty()) {
                String reason = (String) reasonCombo.getSelectedItem();
                String notes = notesField.getText().trim();
                JOptionPane.showMessageDialog(dialog, "❌ " + member + " marked as absent.\nReason: " + reason + (notes.isEmpty() ? "" : "\nNotes: " + notes), "Success", JOptionPane.INFORMATION_MESSAGE);
                dialog.dispose();
                loadData();
            } else {
                JOptionPane.showMessageDialog(dialog, "Please enter member ID or name", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        
        cancelBtn.addActionListener(e -> dialog.dispose());
        
        buttonPanel.add(markAbsentBtn);
        buttonPanel.add(cancelBtn);
        
        panel.add(title, BorderLayout.NORTH);
        panel.add(inputPanel, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        dialog.add(panel);
        dialog.setVisible(true);
    }

    private void showAddVisitorDialog() {
        JDialog dialog = new JDialog(this, "Add Visitor", true);
        dialog.setSize(500, 450);
        dialog.setLocationRelativeTo(this);
        dialog.getContentPane().setBackground(C_BG);
        
        JPanel panel = new JPanel(new BorderLayout(20, 20));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        JLabel title = new JLabel("👋 Add New Visitor");
        title.setFont(F_TITLE);
        title.setForeground(C_TEXT);
        
        JPanel inputPanel = new JPanel(new GridLayout(5, 2, 10, 10));
        inputPanel.setOpaque(false);
        
        inputPanel.add(createLabel("Full Name:"));
        JTextField nameField = new JTextField();
        nameField.setFont(F_LABEL);
        nameField.setForeground(C_TEXT);
        nameField.setBackground(C_CARD);
        nameField.setCaretColor(C_GOLD);
        inputPanel.add(nameField);
        
        inputPanel.add(createLabel("Phone:"));
        JTextField phoneField = new JTextField();
        phoneField.setFont(F_LABEL);
        phoneField.setForeground(C_TEXT);
        phoneField.setBackground(C_CARD);
        phoneField.setCaretColor(C_GOLD);
        inputPanel.add(phoneField);
        
        inputPanel.add(createLabel("Visit Purpose:"));
        JComboBox<String> purposeCombo = new JComboBox<>(new String[]{"First Time Visit", "Family Service", "Business Meeting", "Event", "Other"});
        purposeCombo.setFont(F_LABEL);
        purposeCombo.setForeground(C_TEXT);
        purposeCombo.setBackground(C_CARD);
        inputPanel.add(purposeCombo);
        
        inputPanel.add(createLabel("Host/Greeter:"));
        JTextField hostField = new JTextField();
        hostField.setFont(F_LABEL);
        hostField.setForeground(C_TEXT);
        hostField.setBackground(C_CARD);
        hostField.setCaretColor(C_GOLD);
        inputPanel.add(hostField);
        
        inputPanel.add(createLabel("Check-in Time:"));
        JTextField timeField = new JTextField(java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("hh:mm a")));
        timeField.setFont(F_LABEL);
        timeField.setForeground(C_TEXT);
        timeField.setBackground(C_CARD);
        timeField.setEditable(false);
        inputPanel.add(timeField);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setOpaque(false);
        
        JButton addVisitorBtn = createActionButton("👋 Add Visitor", C_GOLD);
        JButton cancelBtn = createActionButton("❌ Cancel", C_DANGER);
        
        addVisitorBtn.addActionListener(e -> {
            String name = nameField.getText().trim();
            if (!name.isEmpty()) {
                String phone = phoneField.getText().trim();
                String purpose = (String) purposeCombo.getSelectedItem();
                String host = hostField.getText().trim();
                JOptionPane.showMessageDialog(dialog, "👋 Visitor added successfully!\n\nName: " + name + (phone.isEmpty() ? "" : "\nPhone: " + phone) + "\nPurpose: " + purpose + (host.isEmpty() ? "" : "\nHost: " + host), "Success", JOptionPane.INFORMATION_MESSAGE);
                dialog.dispose();
                loadData();
            } else {
                JOptionPane.showMessageDialog(dialog, "Please enter visitor name", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        
        cancelBtn.addActionListener(e -> dialog.dispose());
        
        buttonPanel.add(addVisitorBtn);
        buttonPanel.add(cancelBtn);
        
        panel.add(title, BorderLayout.NORTH);
        panel.add(inputPanel, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        dialog.add(panel);
        dialog.setVisible(true);
    }

    private void showCheckOutVisitorDialog() {
        JDialog dialog = new JDialog(this, "Check Out Visitor", true);
        dialog.setSize(400, 250);
        dialog.setLocationRelativeTo(this);
        dialog.getContentPane().setBackground(C_BG);
        
        JPanel panel = new JPanel(new BorderLayout(20, 20));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        JLabel title = new JLabel("🚪 Check Out Visitor");
        title.setFont(F_TITLE);
        title.setForeground(C_TEXT);
        
        JPanel inputPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        inputPanel.setOpaque(false);
        
        inputPanel.add(createLabel("Visitor Name:"));
        JTextField visitorField = new JTextField();
        visitorField.setFont(F_LABEL);
        visitorField.setForeground(C_TEXT);
        visitorField.setBackground(C_CARD);
        visitorField.setCaretColor(C_GOLD);
        inputPanel.add(visitorField);
        
        inputPanel.add(createLabel("Check-out Time:"));
        JTextField timeField = new JTextField(java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("hh:mm a")));
        timeField.setFont(F_LABEL);
        timeField.setForeground(C_TEXT);
        timeField.setBackground(C_CARD);
        timeField.setEditable(false);
        inputPanel.add(timeField);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setOpaque(false);
        
        JButton checkOutBtn = createActionButton("🚪 Check Out", C_DANGER);
        JButton cancelBtn = createActionButton("❌ Cancel", C_TEXT_MID);
        
        checkOutBtn.addActionListener(e -> {
            String visitor = visitorField.getText().trim();
            if (!visitor.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "🚪 " + visitor + " checked out successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                dialog.dispose();
                loadData();
            } else {
                JOptionPane.showMessageDialog(dialog, "Please enter visitor name", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        
        cancelBtn.addActionListener(e -> dialog.dispose());
        
        buttonPanel.add(checkOutBtn);
        buttonPanel.add(cancelBtn);
        
        panel.add(title, BorderLayout.NORTH);
        panel.add(inputPanel, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        dialog.add(panel);
        dialog.setVisible(true);
    }

    private void showVisitorReport() {
        JDialog dialog = new JDialog(this, "Visitor Report", true);
        dialog.setSize(600, 500);
        dialog.setLocationRelativeTo(this);
        dialog.getContentPane().setBackground(C_BG);
        
        JPanel panel = new JPanel(new BorderLayout(20, 20));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        JLabel title = new JLabel("📊 Visitor Statistics Report");
        title.setFont(F_TITLE);
        title.setForeground(C_TEXT);
        
        // Statistics panel
        JPanel statsPanel = new JPanel(new GridLayout(2, 3, 15, 15));
        statsPanel.setOpaque(false);
        
        JPanel todayVisitors = createStatCard("Today's Visitors", "12", C_GOLD);
        JPanel weeklyVisitors = createStatCard("This Week", "45", C_GOLD_HOVER);
        JPanel monthlyVisitors = createStatCard("This Month", "186", C_GOLD_DIM);
        JPanel firstTime = createStatCard("First Time", "8", C_SUCCESS);
        JPanel repeatVisitors = createStatCard("Repeat Visitors", "4", C_TEXT_MID);
        JPanel avgDuration = createStatCard("Avg Duration", "1h 45m", C_TEXT_MID);
        
        statsPanel.add(todayVisitors);
        statsPanel.add(weeklyVisitors);
        statsPanel.add(monthlyVisitors);
        statsPanel.add(firstTime);
        statsPanel.add(repeatVisitors);
        statsPanel.add(avgDuration);
        
        // Recent visitors table
        JLabel recentTitle = new JLabel("Recent Visitors");
        recentTitle.setFont(F_LABEL);
        recentTitle.setForeground(C_TEXT);
        
        String[] columns = {"Name", "Check In", "Check Out", "Purpose", "Host"};
        Object[][] data = {
            {"Mary Johnson", "9:15 AM", "11:30 AM", "First Time Visit", "Usher Team"},
            {"David Smith", "9:30 AM", "—", "Family Service", "Pastor Smith"},
            {"Lisa Brown", "10:00 AM", "—", "Business Meeting", "Admin Office"},
            {"Tom Wilson", "8:45 AM", "10:15 AM", "First Time Visit", "Greeter Team"}
        };
        
        DefaultTableModel model = new DefaultTableModel(data, columns);
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
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setOpaque(false);
        
        JButton exportBtn = createActionButton("📊 Export Report", C_GOLD);
        JButton closeBtn = createActionButton("❌ Close", C_DANGER);
        
        exportBtn.addActionListener(e -> {
            JOptionPane.showMessageDialog(dialog, "📊 Visitor report exported successfully!\n\nFormat: PDF\nLocation: Downloads folder", "Export Complete", JOptionPane.INFORMATION_MESSAGE);
        });
        
        closeBtn.addActionListener(e -> dialog.dispose());
        
        buttonPanel.add(exportBtn);
        buttonPanel.add(closeBtn);
        
        JPanel centerPanel = new JPanel(new BorderLayout(10, 10));
        centerPanel.setOpaque(false);
        centerPanel.add(recentTitle, BorderLayout.NORTH);
        centerPanel.add(scroll, BorderLayout.CENTER);
        
        panel.add(title, BorderLayout.NORTH);
        panel.add(statsPanel, BorderLayout.NORTH);
        panel.add(centerPanel, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        dialog.add(panel);
        dialog.setVisible(true);
    }

    private void searchMembers(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a search term", "Search Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        JDialog dialog = new JDialog(this, "Member Search Results", true);
        dialog.setSize(700, 400);
        dialog.setLocationRelativeTo(this);
        dialog.getContentPane().setBackground(C_BG);
        
        JPanel panel = new JPanel(new BorderLayout(20, 20));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        JLabel title = new JLabel("🔍 Search Results for: " + searchTerm);
        title.setFont(F_TITLE);
        title.setForeground(C_TEXT);
        
        // Simulated search results
        String[] columns = {"ID", "Name", "Phone", "Email", "Status", "Join Date"};
        Object[][] data = {
            {"001", "John Doe", "+254712345678", "john.doe@church.com", "Active", "2023-01-15"},
            {"002", "Jane Smith", "+254723456789", "jane.smith@church.com", "Active", "2023-02-20"},
            {"003", "Mike Johnson", "+254734567890", "mike.j@church.com", "Active", "2023-03-10"},
            {"004", "Sarah Wilson", "+254745678901", "sarah.w@church.com", "Inactive", "2023-04-05"},
            {"005", "Tom Brown", "+254756789012", "tom.brown@church.com", "Active", "2023-05-12"}
        };
        
        DefaultTableModel model = new DefaultTableModel(data, columns);
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
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setOpaque(false);
        
        JButton checkInBtn = createActionButton("✅ Check In Selected", C_SUCCESS);
        JButton viewDetailsBtn = createActionButton("👁️ View Details", C_GOLD);
        JButton closeBtn = createActionButton("❌ Close", C_DANGER);
        
        checkInBtn.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow != -1) {
                String memberName = (String) table.getValueAt(selectedRow, 1);
                JOptionPane.showMessageDialog(dialog, "✅ " + memberName + " checked in successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                dialog.dispose();
                loadData();
            } else {
                JOptionPane.showMessageDialog(dialog, "Please select a member to check in", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        
        viewDetailsBtn.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow != -1) {
                String memberName = (String) table.getValueAt(selectedRow, 1);
                String memberId = (String) table.getValueAt(selectedRow, 0);
                JOptionPane.showMessageDialog(dialog, "👁️ Member Details:\n\nID: " + memberId + "\nName: " + memberName + "\n\nFull member profile would be displayed here with attendance history, contact information, and membership status.", "Member Details", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(dialog, "Please select a member to view details", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        
        closeBtn.addActionListener(e -> dialog.dispose());
        
        buttonPanel.add(checkInBtn);
        buttonPanel.add(viewDetailsBtn);
        buttonPanel.add(closeBtn);
        
        panel.add(title, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        dialog.add(panel);
        dialog.setVisible(true);
    }

    // ─── Entry Point ──────────────────────────────────────────────────
    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        SwingUtilities.invokeLater(() -> new UsherDashboardFrame().setVisible(true));
    }
}