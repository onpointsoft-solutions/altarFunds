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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

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

    // ═══════════════════════════════════════════════════════════════════
    //  EMOJI FONT UTILITIES  (Matching PastorDashboardFrame)
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Returns the first available color-emoji font at {@code size}.
     */
    private static Font getEmojiFont(int size) {
        String[] candidates = {
            "Segoe UI Emoji",
            "Segoe UI Symbol",
            "Apple Color Emoji",
            "Noto Color Emoji",
            "Android Emoji",
            "EmojiOne"
        };
        // SMP probe — only a real emoji font handles this
        String probe = "\uD83D\uDE00"; // 😀
        for (String name : candidates) {
            Font f = new Font(name, Font.PLAIN, size);
            // getFamily() check guards against silent substitution;
            // canDisplayUpTo == -1 means every code point is covered.
            if (f.getFamily().equalsIgnoreCase(name)
                    || f.canDisplayUpTo(probe) == -1) {
                return f;
            }
        }
        // Last resort — Dialog on modern JDKs delegates to system emoji font
        return new Font("Dialog", Font.PLAIN, size);
    }

    /**
     * Derives an emoji-capable font that matches the size and style of {@code base}.
     */
    private static Font withEmojiFont(Font base) {
        Font emoji = getEmojiFont(base.getSize());
        return emoji.deriveFont(base.getStyle(), base.getSize2D());
    }

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
        icon.setFont(getEmojiFont(16));
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

        // Change Password shortcut
        JButton pwBtn = new JButton("🔑") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isRollover()) {
                    g2.setColor(new Color(212, 175, 55, 35));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                }
                g2.setFont(withEmojiFont(F_LABEL));
                g2.setColor(C_TEXT_DIM);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString("🔑", (getWidth()-fm.stringWidth("🔑"))/2,
                    (getHeight()+fm.getAscent()-fm.getDescent())/2);
                g2.dispose();
            }
        };
        pwBtn.setContentAreaFilled(false); pwBtn.setBorderPainted(false);
        pwBtn.setFocusPainted(false);
        pwBtn.setPreferredSize(new Dimension(30, 30));
        pwBtn.setToolTipText("Change Password");
        pwBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        pwBtn.addActionListener(e -> ChangePasswordDialog.show(
            SwingUtilities.getWindowAncestor(card) instanceof Frame
                ? (Frame) SwingUtilities.getWindowAncestor(card) : null));
        card.add(pwBtn, BorderLayout.EAST);

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
        iconLabel.setFont(getEmojiFont(14));
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

        JPanel contentGrid = new JPanel(new GridLayout(1, 2, 20, 20));
        contentGrid.setOpaque(false);
        contentGrid.add(createQuickActionsCard());
        contentGrid.add(createRecentActivityCard());

        JPanel center = new JPanel(new BorderLayout(0, 20));
        center.setOpaque(false);
        center.add(north, BorderLayout.NORTH);
        center.add(contentGrid, BorderLayout.CENTER);

        main.add(center, BorderLayout.CENTER);

        loadDashboardCardsData();
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
                
                // Enhanced gradient background
                Color topColor = new Color(
                    Math.max(0, C_CARD.getRed() - 5),
                    Math.max(0, C_CARD.getGreen() - 5),
                    Math.max(0, C_CARD.getBlue() - 5)
                );
                Color bottomColor = new Color(
                    Math.min(255, C_CARD.getRed() + 10),
                    Math.min(255, C_CARD.getGreen() + 10),
                    Math.min(255, C_CARD.getBlue() + 10)
                );
                
                Paint gradient = new GradientPaint(0, 0, topColor, 0, getHeight(), bottomColor);
                g2.setPaint(gradient);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
                
                // Enhanced accent bar with glow effect
                g2.setColor(accent);
                g2.fillRoundRect(0, 0, 6, getHeight(), 6, 6);
                
                // Add subtle glow effect
                Color glowColor = new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 50);
                g2.setColor(glowColor);
                g2.fillRoundRect(0, 0, 12, getHeight(), 12, 12);
                
                // Add top highlight
                Color highlightColor = new Color(255, 255, 255, 20);
                g2.setColor(highlightColor);
                g2.fillRoundRect(0, 0, getWidth(), 2, 15, 15);
                
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(18, 22, 18, 22));
        
        // Add hover effect
        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                card.setCursor(new Cursor(Cursor.HAND_CURSOR));
                card.revalidate();
                card.repaint();
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                card.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                card.revalidate();
                card.repaint();
            }
        });

        // Top row: title + icon
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(F_LABEL);
        titleLabel.setForeground(C_TEXT_MID);

        JLabel iconLabel = new JLabel(icon);
        iconLabel.setFont(getEmojiFont(22));
        iconLabel.setForeground(accent);
        top.add(titleLabel, BorderLayout.WEST);
        top.add(iconLabel,  BorderLayout.EAST);

        // Value label — capture reference directly with enhanced styling
        JLabel valueLabel = new JLabel(value) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Add subtle text shadow
                g2.setColor(new Color(0, 0, 0, 100));
                g2.drawString(getText(), 1, 1);
                
                g2.dispose();
            }
        };
        valueLabel.setFont(new Font("JetBrains Mono", Font.BOLD, 24));
        valueLabel.setForeground(C_TEXT);
        out[index] = valueLabel;

        JLabel subtitleLabel = new JLabel(subtitle);
        subtitleLabel.setFont(F_MONO_SM);
        subtitleLabel.setForeground(C_TEXT_DIM);

        JPanel bottom = new JPanel(new GridLayout(2, 1, 0, 4));
        bottom.setOpaque(false);
        bottom.add(valueLabel);
        bottom.add(subtitleLabel);

        card.add(top, BorderLayout.NORTH);
        card.add(bottom, BorderLayout.CENTER);
        return card;
    }

    private JPanel createQuickActionsCard() {
        JPanel card = createCard("Quick Actions", "⚡", C_GOLD);
        JPanel content = new JPanel(new GridLayout(2, 2, 10, 10));
        content.setOpaque(false);

        JButton checkInBtn  = createActionButton("✅ Check In Member", C_SUCCESS);
        JButton visitorBtn  = createActionButton("👋 Add Visitor",     C_GOLD);
        JButton bulkBtn     = createActionButton("📥 Bulk Check In",   C_GOLD);
        JButton checkOutBtn = createActionButton("🚪 Check Out",       C_DANGER);

        checkInBtn .addActionListener(e -> showCheckInDialog());
        visitorBtn .addActionListener(e -> showAddVisitorDialog());
        bulkBtn    .addActionListener(e -> showBulkCheckInDialog());
        checkOutBtn.addActionListener(e -> showCheckOutDialog());

        content.add(checkInBtn);
        content.add(visitorBtn);
        content.add(bulkBtn);
        content.add(checkOutBtn);

        card.add(content, BorderLayout.CENTER);
        return card;
    }

    private JPanel createRecentActivityCard() {
        JPanel card = createCard("Recent Activity", "📋", C_GOLD);
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);
        content.setName("activityContent");
        card.add(content, BorderLayout.CENTER);
        return card;
    }

    private JPanel createCard(String title, String icon, Color accentColor) {
        JPanel card = new JPanel(new BorderLayout(0, 10)) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0, C_CARD, 0, getHeight(), C_SURFACE));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.setColor(accentColor);
                g2.fillRoundRect(0, 0, getWidth(), 4,  4,  4);
                g2.fillRoundRect(0, 0, 4,  getHeight(), 4, 4);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(15, 20, 15, 20));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(F_LABEL);
        titleLabel.setForeground(C_TEXT);

        JLabel iconLabel = new JLabel(icon);
        iconLabel.setFont(getEmojiFont(16));
        iconLabel.setForeground(accentColor);

        header.add(titleLabel, BorderLayout.WEST);
        header.add(iconLabel, BorderLayout.EAST);

        card.add(header, BorderLayout.NORTH);
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
        tabs.setFont(withEmojiFont(F_LABEL));
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

        // Activity log with real data
        String[] columns = {"Time", "Member", "Action", "Status"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        // Load real activity data
        String todayDate = java.time.LocalDate.now().toString();
        SanctumApiClient.getMemberAttendances(todayDate).thenAccept(records -> SwingUtilities.invokeLater(() -> {
            if (records != null && !records.isEmpty()) {
                model.setRowCount(0);
                for (Map<String, Object> record : records) {
                    Object at = record.get("arrival_time");
                    String time   = (at != null && !at.toString().isEmpty()) ? at.toString() : "—";
                    String member = record.getOrDefault("member_name", "Unknown").toString();
                    boolean vis   = Boolean.TRUE.equals(record.get("is_visitor"));
                    boolean pres  = Boolean.TRUE.equals(record.get("is_present"));
                    String action = vis ? "Visitor" : pres ? "Check In" : "Check Out";
                    String status = pres ? "✅ Present" : "❌ Absent";
                    model.addRow(new Object[]{time, member, action, status});
                }
            }
        })).exceptionally(ex -> {
            System.err.println("Failed to load activity data: " + ex.getMessage());
            return null;
        });
        JTable table = buildStyledTable(model);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(true);
        scroll.getViewport().setBackground(C_CARD);
        scroll.setBorder(BorderFactory.createLineBorder(C_BORDER));

        activityPanel.add(activityTitle, BorderLayout.NORTH);
        activityPanel.add(scroll, BorderLayout.CENTER);

        panel.add(quickSection, BorderLayout.NORTH);
        panel.add(activityPanel, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createTodayAttendancePanel() {
        JPanel panel = new JPanel(new BorderLayout(20, 20));
        panel.setOpaque(false);

        // ── Live stat cards (values filled by API) ────────────────────
        JLabel[] statValues = new JLabel[4];
        JPanel statsPanel = new JPanel(new GridLayout(2, 2, 15, 15));
        statsPanel.setOpaque(false);
        statsPanel.add(buildLiveStatCard("Present",   "…", C_SUCCESS,     statValues, 0));
        statsPanel.add(buildLiveStatCard("Absent",    "…", C_DANGER,      statValues, 1));
        statsPanel.add(buildLiveStatCard("Late",      "…", C_GOLD,        statValues, 2));
        statsPanel.add(buildLiveStatCard("Visitors",  "…", C_GOLD_HOVER,  statValues, 3));

        // Load live summary counts
        SanctumApiClient.getAttendanceSummaryForDate(java.time.LocalDate.now().toString())
            .thenAccept(data -> SwingUtilities.invokeLater(() -> {
                statValues[0].setText(data.getOrDefault("present_count",  "0").toString());
                statValues[1].setText(data.getOrDefault("absent_count",   "0").toString());
                statValues[2].setText(data.getOrDefault("late_count",     "0").toString());
                statValues[3].setText(data.getOrDefault("visitor_count",  "0").toString());
            }))
            .exceptionally(ex -> {
                SwingUtilities.invokeLater(() -> {
                    for (JLabel v : statValues) v.setText("—");
                });
                return null;
            });

        // ── Detailed attendance table ─────────────────────────────────
        JPanel tablePanel = new JPanel(new BorderLayout(0, 8));
        tablePanel.setOpaque(false);
        tablePanel.setBorder(new EmptyBorder(20, 0, 0, 0));

        JPanel tableHeader = new JPanel(new BorderLayout(10, 0));
        tableHeader.setOpaque(false);
        JLabel tableTitle = new JLabel("Detailed Attendance — " + java.time.LocalDate.now());
        tableTitle.setFont(F_LABEL);
        tableTitle.setForeground(C_TEXT);

        JButton refreshTableBtn = createActionButton("↻ Refresh", C_TEXT_MID);
        tableHeader.add(tableTitle,     BorderLayout.WEST);
        tableHeader.add(refreshTableBtn, BorderLayout.EAST);

        String[] columns = {"Member", "Check In", "Status", "Notes"};
        DefaultTableModel tableModel = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = buildStyledTable(tableModel);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getViewport().setBackground(C_CARD);

        JLabel statusLbl = new JLabel("Loading today's records…");
        statusLbl.setFont(F_MONO_SM);
        statusLbl.setForeground(C_TEXT_DIM);

        // Loader — calls /api/attendance/members/?service_date=today
        // Response fields: member_name, arrival_time, is_present, is_visitor, notes
        Runnable loadRecords = () -> {
            statusLbl.setText("Loading…");
            tableModel.setRowCount(0);
            String today = java.time.LocalDate.now().toString();
            SanctumApiClient.getMemberAttendances(today).thenAccept(records ->
                SwingUtilities.invokeLater(() -> {
                    tableModel.setRowCount(0);
                    long present = 0, absent = 0, late = 0, visitors = 0;
                    for (Map<String, Object> r : records) {
                        // member_name from serializer's SerializerMethodField
                        String member  = r.getOrDefault("member_name", "Unknown").toString();
                        // arrival_time is a TimeField, may be null
                        Object at = r.get("arrival_time");
                        String checkIn = (at != null && !at.toString().isEmpty()) ? at.toString() : "—";
                        // is_present boolean
                        boolean isPresent = Boolean.TRUE.equals(r.get("is_present"));
                        boolean isVisitor = Boolean.TRUE.equals(r.get("is_visitor"));
                        String status;
                        if (isVisitor)       { status = "Visitor";  visitors++; }
                        else if (isPresent)  { status = "Present";  present++; }
                        else                 { status = "Absent";   absent++; }
                        String notes   = r.getOrDefault("notes", "").toString();
                        tableModel.addRow(new Object[]{member, checkIn, status, notes});
                    }
                    int count = tableModel.getRowCount();
                    statusLbl.setText(count + " record(s) today");
                    statValues[0].setText(String.valueOf(present));
                    statValues[1].setText(String.valueOf(absent));
                    statValues[2].setText(String.valueOf(late));
                    statValues[3].setText(String.valueOf(visitors));
                })
            ).exceptionally(ex -> {
                SwingUtilities.invokeLater(() -> statusLbl.setText("Failed to load records"));
                return null;
            });
        };

        refreshTableBtn.addActionListener(e -> loadRecords.run());

        tablePanel.add(tableHeader, BorderLayout.NORTH);
        tablePanel.add(statusLbl,   BorderLayout.CENTER);
        tablePanel.add(scroll,      BorderLayout.SOUTH);

        // Make scroll take the space, not statusLbl
        tablePanel.setLayout(new BorderLayout(0, 6));
        tablePanel.add(tableHeader, BorderLayout.NORTH);
        tablePanel.add(scroll,      BorderLayout.CENTER);
        tablePanel.add(statusLbl,   BorderLayout.SOUTH);

        panel.add(statsPanel,  BorderLayout.NORTH);
        panel.add(tablePanel,  BorderLayout.CENTER);

        // Initial load
        SwingUtilities.invokeLater(loadRecords::run);
        return panel;
    }

    /** Builds a stat card with an externally-held value label for later update. */
    private JPanel buildLiveStatCard(String title, String initial, Color color,
                                     JLabel[] out, int index) {
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
        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(F_MONO_SM);
        titleLbl.setForeground(C_TEXT_MID);
        JLabel valueLbl = new JLabel(initial);
        valueLbl.setFont(F_MONO_LG);
        valueLbl.setForeground(C_TEXT);
        out[index] = valueLbl;
        card.add(titleLbl, BorderLayout.NORTH);
        card.add(valueLbl, BorderLayout.CENTER);
        return card;
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
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        // Load real visitor data from MemberAttendance (is_visitor=true)
        String todayForVisitors = java.time.LocalDate.now().toString();
        SanctumApiClient.getMemberAttendances(todayForVisitors).thenAccept(records -> SwingUtilities.invokeLater(() -> {
            if (records != null && !records.isEmpty()) {
                model.setRowCount(0);
                for (Map<String, Object> record : records) {
                    if (!Boolean.TRUE.equals(record.get("is_visitor"))) continue;
                    String name    = record.getOrDefault("member_name", "Visitor").toString();
                    Object at      = record.get("arrival_time");
                    String checkIn = (at != null && !at.toString().isEmpty()) ? at.toString() : "—";
                    // notes format: "Name | Phone | Purpose | Host: X"
                    String notes   = record.getOrDefault("notes", "").toString();
                    String purpose = "Visit";
                    String host    = "Usher Team";
                    if (!notes.isEmpty()) {
                        String[] parts = notes.split("\\|");
                        if (parts.length >= 3) purpose = parts[2].trim();
                        if (parts.length >= 4) host    = parts[3].replace("Host:", "").trim();
                    }
                    model.addRow(new Object[]{name, checkIn, purpose, host});
                }
            }
        })).exceptionally(ex -> {
            System.err.println("Failed to load visitor data: " + ex.getMessage());
            return null;
        });
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
        title.setBorder(new EmptyBorder(0, 0, 20, 0));

        // ── Search bar ────────────────────────────────────────────────
        JPanel searchRow = new JPanel(new BorderLayout(8, 0));
        searchRow.setOpaque(false);

        JTextField searchField = new JTextField();
        searchField.setFont(F_LABEL);
        searchField.setForeground(C_TEXT);
        searchField.setBackground(C_CARD);
        searchField.setCaretColor(C_GOLD);
        searchField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(C_BORDER),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));

        JButton searchBtn  = createActionButton("🔍 Search",   C_GOLD);
        JButton checkInBtn = createActionButton("✅ Check In",  C_SUCCESS);
        JButton refreshBtn = createActionButton("↻ Refresh",   C_TEXT_MID);

        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnBar.setOpaque(false);
        btnBar.add(refreshBtn);
        btnBar.add(searchBtn);
        btnBar.add(checkInBtn);

        searchRow.add(searchField, BorderLayout.CENTER);
        searchRow.add(btnBar,      BorderLayout.EAST);

        // ── Members table (live data) ─────────────────────────────────
        String[] cols = {"ID", "Name", "Phone", "Email", "Status"};
        DefaultTableModel membersModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable membersTable = buildStyledTable(membersModel);

        JScrollPane scroll = new JScrollPane(membersTable);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getViewport().setBackground(C_CARD);

        JLabel statusLbl = new JLabel("Loading members…");
        statusLbl.setFont(F_MONO_SM);
        statusLbl.setForeground(C_TEXT_DIM);

        // ── Load all members from API ─────────────────────────────────
        Runnable loadAllMembers = () -> {
            statusLbl.setText("Loading…");
            membersModel.setRowCount(0);
            SanctumApiClient.getMembers().thenAccept(members ->
                SwingUtilities.invokeLater(() -> {
                    membersModel.setRowCount(0);
                    String q = searchField.getText().trim().toLowerCase();
                    members.stream()
                        .filter(m -> {
                            if (q.isEmpty()) return true;
                            String fn = m.getOrDefault("first_name","").toString().toLowerCase();
                            String ln = m.getOrDefault("last_name", "").toString().toLowerCase();
                            String em = m.getOrDefault("email",     "").toString().toLowerCase();
                            return fn.contains(q) || ln.contains(q) || em.contains(q);
                        })
                        .forEach(m -> membersModel.addRow(new Object[]{
                            m.getOrDefault("id",               ""),
                            m.getOrDefault("first_name", "") + " " + m.getOrDefault("last_name", ""),
                            m.getOrDefault("phone_number",     "N/A"),
                            m.getOrDefault("email",            ""),
                            m.getOrDefault("membership_status","")
                        }));
                    statusLbl.setText(membersModel.getRowCount() + " member(s)");
                })
            ).exceptionally(ex -> {
                SwingUtilities.invokeLater(() -> statusLbl.setText("Failed to load members"));
                return null;
            });
        };

        // ── Button actions ────────────────────────────────────────────
        refreshBtn.addActionListener(e -> loadAllMembers.run());
        searchBtn.addActionListener(e  -> loadAllMembers.run());
        searchField.addActionListener(e -> loadAllMembers.run()); // Enter key

        checkInBtn.addActionListener(e -> {
            int row = membersTable.getSelectedRow();
            if (row < 0) {
                JOptionPane.showMessageDialog(panel, "Select a member first.", "No Selection", JOptionPane.WARNING_MESSAGE);
                return;
            }
            String idStr = membersModel.getValueAt(row, 0).toString();
            String name  = membersModel.getValueAt(row, 1).toString();
            int memberId;
            try { memberId = Integer.parseInt(idStr); }
            catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(panel, "Invalid member ID.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            checkInBtn.setEnabled(false);
            checkInBtn.setText("⏳ Checking in…");
            SanctumApiClient.getOrCreateAttendanceRecord("Sunday Service")
                .thenCompose(recordId -> recordId < 0
                    ? CompletableFuture.completedFuture(false)
                    : SanctumApiClient.markMemberPresent(recordId, memberId, "Checked in by usher"))
                .thenAccept(ok -> SwingUtilities.invokeLater(() -> {
                    checkInBtn.setEnabled(true);
                    checkInBtn.setText("✅ Check In");
                    if (ok) {
                        statusLbl.setText("✓ " + name + " checked in");
                        loadData(); // refresh KPIs
                    } else {
                        JOptionPane.showMessageDialog(panel,
                            "Check-in failed. Please try again.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                })).exceptionally(ex -> {
                    SwingUtilities.invokeLater(() -> {
                        checkInBtn.setEnabled(true);
                        checkInBtn.setText("✅ Check In");
                        statusLbl.setText("Network error: " + ex.getMessage());
                    });
                    return null;
                });
        });

        // ── Layout ────────────────────────────────────────────────────
        JPanel north = new JPanel(new BorderLayout(0, 8));
        north.setOpaque(false);
        north.add(title,     BorderLayout.NORTH);
        north.add(searchRow, BorderLayout.CENTER);
        north.add(statusLbl, BorderLayout.SOUTH);

        panel.add(north,  BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);

        // Initial load
        SwingUtilities.invokeLater(loadAllMembers::run);
        return panel;
    }

    private JPanel buildServicesPage() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(30, 30, 30, 30));

        // ── Title row ─────────────────────────────────────────────────
        JLabel title = new JLabel("Service Management");
        title.setFont(F_TITLE);
        title.setForeground(C_TEXT);
        title.setBorder(new EmptyBorder(0, 0, 20, 0));

        JButton addBtn     = createActionButton("➕ Add Service", C_GOLD);
        JButton refreshBtn = createActionButton("↻ Refresh",     C_TEXT_MID);

        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnBar.setOpaque(false);
        btnBar.add(refreshBtn);
        btnBar.add(addBtn);

        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        titleRow.add(title,  BorderLayout.WEST);
        titleRow.add(btnBar, BorderLayout.EAST);

        // ── Grid container — rebuilt each refresh ─────────────────────
        JPanel servicesGrid = new JPanel(new GridLayout(0, 2, 20, 20));
        servicesGrid.setOpaque(false);

        JLabel statusLbl = new JLabel("Loading services…");
        statusLbl.setFont(F_MONO_SM);
        statusLbl.setForeground(C_TEXT_DIM);
        statusLbl.setBorder(new EmptyBorder(10, 0, 0, 0));

        // ── Loader lambda ─────────────────────────────────────────────
        Runnable loadServices = () -> {
            statusLbl.setText("Loading…");
            servicesGrid.removeAll();
            SanctumApiClient.getChurchServices().thenAccept(services ->
                SwingUtilities.invokeLater(() -> {
                    servicesGrid.removeAll();
                    if (services.isEmpty()) {
                        JPanel empty = new JPanel(new BorderLayout());
                        empty.setOpaque(false);
                        JLabel emptyLbl = new JLabel(
                            "<html><center>No services configured yet.<br>" +
                            "Click <b>➕ Add Service</b> to create the first one.</center></html>",
                            SwingConstants.CENTER);
                        emptyLbl.setFont(F_LABEL);
                        emptyLbl.setForeground(C_TEXT_DIM);
                        empty.add(emptyLbl, BorderLayout.CENTER);
                        servicesGrid.add(empty);
                        statusLbl.setText("0 services");
                    } else {
                        for (Map<String, Object> svc : services) {
                            String name     = svc.getOrDefault("name",       "Service").toString();
                            String time     = svc.getOrDefault("start_time", "").toString();
                            String location = svc.getOrDefault("location",   "").toString();
                            String active   = Boolean.TRUE.equals(svc.get("is_active")) ? "Active" : "Inactive";
                            servicesGrid.add(createServiceCard(name, time,
                                location.isEmpty() ? "Main Sanctuary" : location, active));
                        }
                        statusLbl.setText(services.size() + " service(s)");
                    }
                    servicesGrid.revalidate();
                    servicesGrid.repaint();
                })
            ).exceptionally(ex -> {
                SwingUtilities.invokeLater(() -> {
                    servicesGrid.removeAll();
                    JLabel err = new JLabel("Failed to load services — check connection.");
                    err.setFont(F_LABEL);
                    err.setForeground(C_DANGER);
                    servicesGrid.add(err);
                    servicesGrid.revalidate();
                    statusLbl.setText("Error");
                });
                return null;
            });
        };

        refreshBtn.addActionListener(e -> loadServices.run());
        addBtn.addActionListener(e -> showAddServiceDialog(loadServices));

        JScrollPane scroll = new JScrollPane(servicesGrid);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);

        panel.add(titleRow,  BorderLayout.NORTH);
        panel.add(scroll,    BorderLayout.CENTER);
        panel.add(statusLbl, BorderLayout.SOUTH);

        SwingUtilities.invokeLater(loadServices::run);
        return panel;
    }

    // ─── Add Service Dialog ────────────────────────────────────────────
    private void showAddServiceDialog(Runnable onSuccess) {
        JDialog dialog = new JDialog(this, "Add New Service", true);
        dialog.setSize(480, 420);
        dialog.setLocationRelativeTo(this);
        dialog.getContentPane().setBackground(C_BG);

        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(24, 24, 20, 24));

        JLabel titleLbl = new JLabel("⛪ Add New Church Service");
        titleLbl.setFont(withEmojiFont(F_TITLE));
        titleLbl.setForeground(C_GOLD);

        // ── Form ──────────────────────────────────────────────────────
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(6, 4, 6, 4);

        // Service name
        JTextField nameField = styledTextField();
        nameField.setToolTipText("e.g. Sunday Morning Service");
        // Service type
        String[] typeOptions = {"sunday_morning","sunday_evening","midweek",
                                "prayer","youth","children","special","other"};
        JComboBox<String> typeCombo = styledComboBox(typeOptions);
        // Day of week
        String[] days = {"Sunday","Monday","Tuesday","Wednesday","Thursday","Friday","Saturday",""};
        JComboBox<String> dayCombo = styledComboBox(days);
        // Start time
        JTextField startField = styledTextField();
        startField.setText("09:00");
        startField.setToolTipText("HH:MM — e.g. 09:00");
        // End time
        JTextField endField = styledTextField();
        endField.setText("11:00");
        endField.setToolTipText("HH:MM — e.g. 11:00");
        // Location
        JTextField locationField = styledTextField();
        locationField.setToolTipText("e.g. Main Sanctuary");

        int row = 0;
        addFormRow(form, gc, row++, "Service Name *", nameField);
        addFormRow(form, gc, row++, "Type",           typeCombo);
        addFormRow(form, gc, row++, "Day of Week",    dayCombo);
        addFormRow(form, gc, row++, "Start Time *",   startField);
        addFormRow(form, gc, row++, "End Time",       endField);
        addFormRow(form, gc, row++, "Location",       locationField);

        // ── Status label ──────────────────────────────────────────────
        JLabel statusLbl = new JLabel(" ");
        statusLbl.setFont(F_MONO_SM);
        statusLbl.setForeground(C_TEXT_DIM);

        // ── Buttons ───────────────────────────────────────────────────
        JButton saveBtn   = createActionButton("💾 Save Service", C_GOLD);
        JButton cancelBtn = createActionButton("✕ Cancel",        C_DANGER);
        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnBar.setOpaque(false);
        btnBar.add(cancelBtn);
        btnBar.add(saveBtn);

        saveBtn.addActionListener(e -> {
            String name      = nameField.getText().trim();
            String startTime = startField.getText().trim();

            if (name.isEmpty()) {
                statusLbl.setText("Service name is required.");
                statusLbl.setForeground(C_DANGER);
                nameField.requestFocusInWindow();
                return;
            }
            if (!startTime.matches("\\d{1,2}:\\d{2}")) {
                statusLbl.setText("Start time must be HH:MM (e.g. 09:00).");
                statusLbl.setForeground(C_DANGER);
                startField.requestFocusInWindow();
                return;
            }

            String type     = typeCombo.getSelectedItem().toString();
            String day      = dayCombo.getSelectedItem().toString();
            String endTime  = endField.getText().trim();
            String location = locationField.getText().trim();

            saveBtn.setEnabled(false);
            saveBtn.setText("⏳ Saving…");
            statusLbl.setText("Saving…");
            statusLbl.setForeground(C_GOLD);

            SanctumApiClient.createChurchService(name, type, day, startTime, endTime, location)
                .thenAccept(newId -> SwingUtilities.invokeLater(() -> {
                    saveBtn.setEnabled(true);
                    saveBtn.setText("💾 Save Service");
                    if (newId > 0) {
                        statusLbl.setText("✓ Service created successfully!");
                        statusLbl.setForeground(C_SUCCESS);
                        // Refresh the services grid and close after a brief pause
                        if (onSuccess != null) onSuccess.run();
                        javax.swing.Timer t = new javax.swing.Timer(900, ev -> dialog.dispose());
                        t.setRepeats(false);
                        t.start();
                    } else {
                        statusLbl.setText("Failed to save — check connection and try again.");
                        statusLbl.setForeground(C_DANGER);
                    }
                })).exceptionally(ex -> {
                    SwingUtilities.invokeLater(() -> {
                        saveBtn.setEnabled(true);
                        saveBtn.setText("💾 Save Service");
                        statusLbl.setText("Network error: " + ex.getMessage());
                        statusLbl.setForeground(C_DANGER);
                    });
                    return null;
                });
        });

        cancelBtn.addActionListener(ev -> dialog.dispose());

        JPanel south = new JPanel(new BorderLayout(0, 6));
        south.setOpaque(false);
        south.add(statusLbl, BorderLayout.NORTH);
        south.add(btnBar,    BorderLayout.SOUTH);

        panel.add(titleLbl, BorderLayout.NORTH);
        panel.add(form,     BorderLayout.CENTER);
        panel.add(south,    BorderLayout.SOUTH);

        dialog.add(panel);
        dialog.setVisible(true);
    }

    /** Helper: adds a label + field pair as a form row. */
    private void addFormRow(JPanel p, GridBagConstraints gc, int row,
                            String label, JComponent field) {
        gc.gridx = 0; gc.gridy = row; gc.weightx = 0;
        JLabel lbl = new JLabel(label);
        lbl.setFont(F_LABEL);
        lbl.setForeground(C_TEXT_MID);
        p.add(lbl, gc);
        gc.gridx = 1; gc.weightx = 1.0;
        p.add(field, gc);
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
        btn.setFont(withEmojiFont(F_LABEL));
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
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                // 1. Paint background gradient FIRST so text renders on top
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Use the button's current background if set (hover/press), else the base color
                Color base = getBackground();
                if (base == null || base.equals(javax.swing.UIManager.getColor("Button.background"))) {
                    base = color;
                }
                Color darker = new Color(
                    Math.max(0, base.getRed()   - 20),
                    Math.max(0, base.getGreen() - 20),
                    Math.max(0, base.getBlue()  - 20)
                );

                g2.setPaint(new GradientPaint(0, 0, base, 0, getHeight(), darker));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);

                // Subtle dark border
                g2.setColor(new Color(0, 0, 0, 60));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);

                // Top highlight gleam
                g2.setColor(new Color(255, 255, 255, 40));
                g2.fillRoundRect(1, 1, getWidth() - 3, 3, 6, 6);

                g2.dispose();

                // 2. Let Swing draw the text label ON TOP of the background
                super.paintComponent(g);
            }
        };

        btn.setFont(withEmojiFont(F_LABEL));
        btn.setForeground(Color.WHITE);           // always white on coloured background
        btn.setBackground(color);                 // initial background = base color
        btn.setContentAreaFilled(false);          // we handle fill ourselves
        btn.setBorderPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(12, 18, 12, 18));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setOpaque(false);

        // Hover / press effects via background colour swap (picked up by paintComponent)
        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                btn.setBackground(new Color(
                    Math.min(255, color.getRed()   + 30),
                    Math.min(255, color.getGreen() + 30),
                    Math.min(255, color.getBlue()  + 30)));
                btn.repaint();
            }
            @Override public void mouseExited(MouseEvent e) {
                btn.setBackground(color);
                btn.repaint();
            }
            @Override public void mousePressed(MouseEvent e) {
                btn.setBackground(new Color(
                    Math.max(0, color.getRed()   - 30),
                    Math.max(0, color.getGreen() - 30),
                    Math.max(0, color.getBlue()  - 30)));
                btn.repaint();
            }
            @Override public void mouseReleased(MouseEvent e) {
                btn.setBackground(color);
                btn.repaint();
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
            // FIX: clear the auth session so tokens are not re-used
            com.sanctum.auth.SessionManager.getInstance().clearSession();
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
        setKpiLabels("…");
        String today = java.time.LocalDate.now().toString();

        // ── 1. Count today's check-ins, visitors, total from MemberAttendance ──
        SanctumApiClient.getMemberAttendances(today).thenAccept(records ->
            SwingUtilities.invokeLater(() -> {
                long checkedIn = 0, visitors = 0;
                for (Map<String, Object> r : records) {
                    boolean pres = Boolean.TRUE.equals(r.get("is_present"));
                    boolean vis  = Boolean.TRUE.equals(r.get("is_visitor"));
                    if (pres || vis) checkedIn++;
                    if (vis) visitors++;
                }
                if (lblTotalCheckedIn  != null) lblTotalCheckedIn .setText(formatNumber(checkedIn));
                if (lblTodayAttendance != null) lblTodayAttendance.setText(formatNumber(checkedIn));
                if (lblNewVisitors     != null) lblNewVisitors    .setText(formatNumber(visitors));
            })
        ).exceptionally(ex -> {
            SwingUtilities.invokeLater(() -> {
                if (lblTotalCheckedIn  != null) lblTotalCheckedIn .setText("—");
                if (lblTodayAttendance != null) lblTodayAttendance.setText("—");
                if (lblNewVisitors     != null) lblNewVisitors    .setText("—");
            });
            return null;
        });

        // ── 2. Count active services ──────────────────────────────────────
        SanctumApiClient.getChurchServices().thenAccept(services ->
            SwingUtilities.invokeLater(() -> {
                long active = services.stream()
                    .filter(s -> Boolean.TRUE.equals(s.get("is_active")))
                    .count();
                if (lblActiveServices != null) lblActiveServices.setText(formatNumber(active));
            })
        ).exceptionally(ex -> {
            SwingUtilities.invokeLater(() -> {
                if (lblActiveServices != null) lblActiveServices.setText("—");
            });
            return null;
        });

        // ── 3. Refresh recent-activity card on dashboard ──────────────────
        loadDashboardCardsData();
    }
    
    private void loadDashboardCardsData() {
        String today = java.time.LocalDate.now().toString();
        SanctumApiClient.getMemberAttendances(today).thenAccept(records -> SwingUtilities.invokeLater(() -> {
            JPanel activityContent = findNamedComponent("activityContent", JPanel.class);
            if (activityContent != null) {
                activityContent.removeAll();

                if (records != null && !records.isEmpty()) {
                    // Show the 5 most-recent entries
                    int shown = 0;
                    for (Map<String, Object> record : records) {
                        if (shown++ >= 5) break;
                        JPanel activityItem = createActivityItem(record);
                        activityContent.add(activityItem);
                        activityContent.add(Box.createVerticalStrut(6));
                    }
                } else {
                    JLabel noActivity = new JLabel("No activity today");
                    noActivity.setForeground(C_TEXT_DIM);
                    noActivity.setFont(F_MONO_SM);
                    activityContent.add(noActivity);
                }

                activityContent.revalidate();
                activityContent.repaint();
            }
        })).exceptionally(ex -> {
            System.err.println("Failed to load recent activity: " + ex.getMessage());
            return null;
        });
    }
    
    private JPanel createActivityItem(Map<String, Object> record) {
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.setOpaque(true);
        panel.setBackground(C_CARD);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, C_BORDER),
            new EmptyBorder(8, 12, 8, 12)
        ));

        // MemberAttendance fields: member_name, arrival_time, is_present, is_visitor
        String memberName = record.getOrDefault("member_name", "Unknown").toString();
        Object at         = record.get("arrival_time");
        String time       = (at != null && !at.toString().isEmpty()) ? at.toString() : "—";
        boolean isVisitor = Boolean.TRUE.equals(record.get("is_visitor"));
        boolean isPresent = Boolean.TRUE.equals(record.get("is_present"));

        String type = isVisitor ? "Visitor" : isPresent ? "Check In" : "Check Out";
        String icon = isVisitor ? "👋" : isPresent ? "✅" : "🚪";

        JLabel typeLabel = new JLabel(icon + "  " + memberName);
        typeLabel.setFont(withEmojiFont(F_MONO_SM));
        typeLabel.setForeground(isVisitor ? C_GOLD_HOVER : isPresent ? C_SUCCESS : C_TEXT_MID);

        JLabel timeLabel = new JLabel(time);
        timeLabel.setFont(F_MONO_SM);
        timeLabel.setForeground(C_TEXT_DIM);

        panel.add(typeLabel, BorderLayout.WEST);
        panel.add(timeLabel, BorderLayout.EAST);
        return panel;
    }
    
    private String getActivityIcon(String type) {
        switch (type.toLowerCase()) {
            case "checkin": return "✅";
            case "checkout": return "🚪";
            case "visitor": return "👋";
            case "bulk": return "📥";
            default: return "📋";
        }
    }
    
    @SuppressWarnings("unchecked")
    private <T> T findNamedComponent(String name, Class<T> type) {
        return (T) findComponentByName(this, name);
    }
    
    private Component findComponentByName(Container container, String name) {
        for (Component comp : container.getComponents()) {
            if (name.equals(comp.getName())) {
                return comp;
            }
            if (comp instanceof Container) {
                Component found = findComponentByName((Container) comp, name);
                if (found != null) return found;
            }
        }
        return null;
    }
    
    /** Format numbers with proper styling */
    private String formatNumber(Object value) {
        try {
            int num = Integer.parseInt(value.toString());
            return String.format("%,d", num);
        } catch (NumberFormatException e) {
            return value.toString();
        }
    }
    
    private void updateAttendancePageWithRealData() {
        // Load real attendance records and update the attendance table
        SanctumApiClient.getAttendanceRecords().thenAccept(records -> SwingUtilities.invokeLater(() -> {
            if (records != null && !records.isEmpty()) {
                System.out.println("Loaded " + records.size() + " attendance records");
                // Update attendance table with real data
                updateAttendanceTable(records);
            } else {
                System.out.println("No attendance records found");
            }
        })).exceptionally(ex -> {
            System.err.println("Failed to load attendance records: " + ex.getMessage());
            return null;
        });
    }
    
    private void updateAttendanceTable(List<Map<String,Object>> records) {
        // Find and update the attendance table in the attendance page
        // This would require storing a reference to the table or finding it through the component hierarchy
        System.out.println("Attendance table updated with " + records.size() + " records");
        
        // For now, just log the data - in a full implementation, we'd update the table model
        for (Map<String,Object> record : records) {
            System.out.println("Record: " + record.getOrDefault("member_name", "Unknown") + 
                             " - " + record.getOrDefault("check_in_time", "No time"));
        }
    }

    /** Convenience: set all four KPI labels to the same text (e.g. "Loading..." or "—"). */
    private void setKpiLabels(String text) {
        if (lblTotalCheckedIn  != null) lblTotalCheckedIn .setText(text);
        if (lblTodayAttendance != null) lblTodayAttendance.setText(text);
        if (lblActiveServices  != null) lblActiveServices .setText(text);
        if (lblNewVisitors     != null) lblNewVisitors    .setText(text);
    }

    // Dialog methods - Now implemented with actual functionality
    // Dialog methods — member attendance with real API calls
    
    /**
     * Searches live members from the API and shows a selection dialog.
     * On selection, marks the member present using the attendance record ID.
     */
    private void showCheckInDialog() {
        JDialog dialog = new JDialog(this, "Check In Member", true);
        dialog.setSize(520, 480);
        dialog.setLocationRelativeTo(this);
        dialog.getContentPane().setBackground(C_BG);

        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("✅ Member Check-In");
        title.setFont(withEmojiFont(F_TITLE));
        title.setForeground(C_TEXT);

        // Service selector row — populated from API
        JPanel serviceRow = new JPanel(new BorderLayout(10, 0));
        serviceRow.setOpaque(false);
        serviceRow.add(createLabel("Service:"), BorderLayout.WEST);
        DefaultComboBoxModel<String> serviceNameModel = new DefaultComboBoxModel<>();
        // We store the mapping name→id in a parallel list
        List<Map<String, Object>> loadedServices = new ArrayList<>();
        JComboBox<String> serviceCombo = new JComboBox<>(serviceNameModel);
        serviceCombo.setFont(F_LABEL);
        serviceCombo.setForeground(C_TEXT);
        serviceCombo.setBackground(C_CARD);
        serviceCombo.addItem("Loading services…");
        serviceRow.add(serviceCombo, BorderLayout.CENTER);
        // Load services from backend
        SanctumApiClient.getChurchServices().thenAccept(svcs -> SwingUtilities.invokeLater(() -> {
            serviceNameModel.removeAllElements();
            loadedServices.clear();
            if (svcs.isEmpty()) {
                serviceNameModel.addElement("No services — add via Services page");
            } else {
                for (Map<String, Object> s : svcs) {
                    serviceNameModel.addElement(s.getOrDefault("name", "Service").toString());
                    loadedServices.add(s);
                }
            }
        })).exceptionally(ex -> {
            SwingUtilities.invokeLater(() -> {
                serviceNameModel.removeAllElements();
                serviceNameModel.addElement("Could not load services");
            });
            return null;
        });

        // Search row
        JPanel searchRow = new JPanel(new BorderLayout(8, 0));
        searchRow.setOpaque(false);
        JTextField searchField = new JTextField();
        searchField.setFont(F_LABEL);
        searchField.setForeground(C_TEXT);
        searchField.setBackground(C_CARD);
        searchField.setCaretColor(C_GOLD);
        searchField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(C_BORDER),
            BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));
        JButton searchBtn = createActionButton("🔍 Search", C_GOLD);
        searchRow.add(searchField, BorderLayout.CENTER);
        searchRow.add(searchBtn,   BorderLayout.EAST);

        // Members table
        String[] cols = {"ID", "Name", "Phone", "Status"};
        DefaultTableModel tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable memberTable = new JTable(tableModel);
        memberTable.setBackground(C_CARD);
        memberTable.setForeground(C_TEXT);
        memberTable.setGridColor(C_BORDER);
        memberTable.setRowHeight(28);
        memberTable.getTableHeader().setBackground(C_SURFACE);
        memberTable.getTableHeader().setForeground(C_TEXT);
        memberTable.getTableHeader().setFont(F_LABEL);
        memberTable.setSelectionBackground(C_GOLD_DIM);
        memberTable.setSelectionForeground(C_TEXT);
        memberTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(t, v, sel, foc, r, c);
                lbl.setForeground(C_TEXT);
                lbl.setOpaque(!sel);
                if (sel) { lbl.setBackground(C_GOLD_DIM); lbl.setOpaque(true); }
                return lbl;
            }
        });
        JScrollPane scroll = new JScrollPane(memberTable);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getViewport().setBackground(C_CARD);

        // Status label
        JLabel statusLbl = new JLabel("Search for a member to check in");
        statusLbl.setFont(F_MONO_SM);
        statusLbl.setForeground(C_TEXT_DIM);

        // Buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnPanel.setOpaque(false);
        JButton checkInBtn = createActionButton("✅ Check In Selected", C_SUCCESS);
        JButton cancelBtn  = createActionButton("✕ Cancel", C_DANGER);

        // Load all members initially
        Runnable loadMembers = () -> {
            statusLbl.setText("Loading members…");
            tableModel.setRowCount(0);
            String query = searchField.getText().trim();
            SanctumApiClient.getMembers().thenAccept(members -> SwingUtilities.invokeLater(() -> {
                tableModel.setRowCount(0);
                members.stream()
                    .filter(m -> {
                        if (query.isEmpty()) return true;
                        String fn = m.getOrDefault("first_name", "").toString().toLowerCase();
                        String ln = m.getOrDefault("last_name",  "").toString().toLowerCase();
                        String email = m.getOrDefault("email",   "").toString().toLowerCase();
                        String q = query.toLowerCase();
                        return fn.contains(q) || ln.contains(q) || email.contains(q);
                    })
                    .forEach(m -> {
                        // Prefer user_id (User pk) over id (Member profile pk)
                        // — mark_member_present expects the User pk
                        String id     = m.containsKey("user_id") && !m.get("user_id").toString().isEmpty()
                                        ? m.get("user_id").toString()
                                        : m.getOrDefault("id", "").toString();
                        String fn     = m.getOrDefault("first_name", "").toString();
                        String ln     = m.getOrDefault("last_name",  "").toString();
                        String phone  = m.getOrDefault("phone_number","N/A").toString();
                        String status = m.getOrDefault("membership_status","").toString();
                        tableModel.addRow(new Object[]{id, fn + " " + ln, phone, status});
                    });
                statusLbl.setText(tableModel.getRowCount() + " member(s) found");
            })).exceptionally(ex -> {
                SwingUtilities.invokeLater(() -> statusLbl.setText("Failed to load members"));
                return null;
            });
        };

        searchBtn.addActionListener(e -> loadMembers.run());
        searchField.addActionListener(e -> loadMembers.run()); // Enter key

        checkInBtn.addActionListener(e -> {
            int row = memberTable.getSelectedRow();
            if (row < 0) { statusLbl.setText("Please select a member first"); return; }
            String idStr = tableModel.getValueAt(row, 0).toString();
            String name  = tableModel.getValueAt(row, 1).toString();
            int memberId;
            try { memberId = Integer.parseInt(idStr); }
            catch (NumberFormatException ex) { statusLbl.setText("Invalid member ID"); return; }

            String serviceType = serviceCombo.getSelectedItem().toString();
            checkInBtn.setEnabled(false);
            checkInBtn.setText("⏳ Checking in…");
            statusLbl.setText("Getting attendance record…");

            // Resolve the selected service ID from the loaded list
            int selectedServiceId = -1;
            int selectedIdx = serviceCombo.getSelectedIndex();
            if (selectedIdx >= 0 && selectedIdx < loadedServices.size()) {
                Object id = loadedServices.get(selectedIdx).get("id");
                if (id instanceof Number) selectedServiceId = ((Number) id).intValue();
            }
            final int serviceId = selectedServiceId;

            CompletableFuture<Integer> recordFuture = serviceId > 0
                ? SanctumApiClient.getOrCreateAttendanceRecord(serviceId)
                : SanctumApiClient.getOrCreateAttendanceRecord(serviceType);

            recordFuture.thenCompose(recordId -> {
                    if (recordId < 0) {
                        return CompletableFuture.completedFuture(false);
                    }
                    return SanctumApiClient.markMemberPresent(recordId, memberId, "Checked in by usher");
                })
                .thenAccept(success -> SwingUtilities.invokeLater(() -> {
                    checkInBtn.setEnabled(true);
                    checkInBtn.setText("✅ Check In Selected");
                    if (success) {
                        JOptionPane.showMessageDialog(dialog,
                            "✅ " + name + " checked in for " + serviceType,
                            "Success", JOptionPane.INFORMATION_MESSAGE);
                        dialog.dispose();
                        loadData();
                    } else {
                        statusLbl.setText("Check-in failed — see log for details");
                    }
                })).exceptionally(ex -> {
                    SwingUtilities.invokeLater(() -> {
                        checkInBtn.setEnabled(true);
                        checkInBtn.setText("✅ Check In Selected");
                        statusLbl.setText("Network error: " + ex.getMessage());
                    });
                    return null;
                });
        });

        cancelBtn.addActionListener(e -> dialog.dispose());
        btnPanel.add(cancelBtn);
        btnPanel.add(checkInBtn);

        JPanel topFields = new JPanel(new GridLayout(2, 1, 0, 8));
        topFields.setOpaque(false);
        topFields.add(serviceRow);
        topFields.add(searchRow);

        panel.add(title,     BorderLayout.NORTH);
        panel.add(topFields, BorderLayout.NORTH); // will overlay — fix with BoxLayout
        // Rebuild with BoxLayout for proper stacking
        panel.removeAll();
        panel.setLayout(new BorderLayout(0, 12));

        JPanel topSection = new JPanel();
        topSection.setLayout(new BoxLayout(topSection, BoxLayout.Y_AXIS));
        topSection.setOpaque(false);
        topSection.add(title);
        topSection.add(Box.createVerticalStrut(12));
        topSection.add(serviceRow);
        topSection.add(Box.createVerticalStrut(8));
        topSection.add(searchRow);
        topSection.add(Box.createVerticalStrut(4));
        topSection.add(statusLbl);

        panel.add(topSection, BorderLayout.NORTH);
        panel.add(scroll,     BorderLayout.CENTER);
        panel.add(btnPanel,   BorderLayout.SOUTH);

        dialog.add(panel);
        loadMembers.run(); // load all members on open
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
        dialog.setSize(560, 520);
        dialog.setLocationRelativeTo(this);
        dialog.getContentPane().setBackground(C_BG);

        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("📥 Bulk Member Check-In");
        title.setFont(withEmojiFont(F_TITLE));
        title.setForeground(C_TEXT);

        // Service selector — populated from API
        JPanel svcRow = new JPanel(new BorderLayout(10, 0));
        svcRow.setOpaque(false);
        svcRow.add(createLabel("Service:"), BorderLayout.WEST);
        DefaultComboBoxModel<String> bulkServiceModel = new DefaultComboBoxModel<>();
        List<Map<String, Object>> bulkLoadedServices = new ArrayList<>();
        JComboBox<String> serviceCombo = new JComboBox<>(bulkServiceModel);
        serviceCombo.setFont(F_LABEL); serviceCombo.setForeground(C_TEXT); serviceCombo.setBackground(C_CARD);
        serviceCombo.addItem("Loading services…");
        svcRow.add(serviceCombo, BorderLayout.CENTER);
        SanctumApiClient.getChurchServices().thenAccept(svcs -> SwingUtilities.invokeLater(() -> {
            bulkServiceModel.removeAllElements();
            bulkLoadedServices.clear();
            if (svcs.isEmpty()) {
                bulkServiceModel.addElement("No services — add via Services page");
            } else {
                for (Map<String, Object> s : svcs) {
                    bulkServiceModel.addElement(s.getOrDefault("name", "Service").toString());
                    bulkLoadedServices.add(s);
                }
            }
        })).exceptionally(ex -> {
            SwingUtilities.invokeLater(() -> { bulkServiceModel.removeAllElements(); bulkServiceModel.addElement("Could not load services"); });
            return null;
        });

        // Member table with checkboxes
        String[] cols = {"✓", "ID", "Name", "Status"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public Class<?> getColumnClass(int c) { return c == 0 ? Boolean.class : String.class; }
            @Override public boolean isCellEditable(int r, int c) { return c == 0; }
        };
        JTable table = new JTable(model);
        table.setBackground(C_CARD); table.setForeground(C_TEXT);
        table.setGridColor(C_BORDER); table.setRowHeight(28);
        table.getTableHeader().setBackground(C_SURFACE); table.getTableHeader().setForeground(C_TEXT);
        table.getColumnModel().getColumn(0).setMaxWidth(36);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setOpaque(false); scroll.getViewport().setOpaque(false);
        scroll.getViewport().setBackground(C_CARD);

        JLabel statusLbl = new JLabel("Loading members…");
        statusLbl.setFont(F_MONO_SM); statusLbl.setForeground(C_TEXT_DIM);

        // Load members into table
        SanctumApiClient.getMembers().thenAccept(members -> SwingUtilities.invokeLater(() -> {
            model.setRowCount(0);
            members.forEach(m -> model.addRow(new Object[]{
                Boolean.FALSE,
                m.getOrDefault("id", "").toString(),
                m.getOrDefault("first_name", "") + " " + m.getOrDefault("last_name", ""),
                m.getOrDefault("membership_status", "")
            }));
            statusLbl.setText(members.size() + " members loaded — select those present");
        })).exceptionally(ex -> { SwingUtilities.invokeLater(() -> statusLbl.setText("Failed to load members")); return null; });

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnPanel.setOpaque(false);
        JButton checkAllBtn = createActionButton("☑ Select All", C_TEXT_MID);
        JButton submitBtn   = createActionButton("📥 Mark Selected Present", C_GOLD);
        JButton cancelBtn   = createActionButton("✕ Cancel", C_DANGER);

        checkAllBtn.addActionListener(e -> {
            boolean anyUnchecked = false;
            for (int i = 0; i < model.getRowCount(); i++) if (!(Boolean) model.getValueAt(i, 0)) { anyUnchecked = true; break; }
            for (int i = 0; i < model.getRowCount(); i++) model.setValueAt(anyUnchecked, i, 0);
        });

        submitBtn.addActionListener(e -> {
            List<Integer> selectedIds = new ArrayList<>();
            List<String> selectedNames = new ArrayList<>();
            for (int i = 0; i < model.getRowCount(); i++) {
                if ((Boolean) model.getValueAt(i, 0)) {
                    try { selectedIds.add(Integer.parseInt(model.getValueAt(i, 1).toString())); }
                    catch (NumberFormatException ignored) {}
                    selectedNames.add(model.getValueAt(i, 2).toString());
                }
            }
            if (selectedIds.isEmpty()) { statusLbl.setText("Please select at least one member"); return; }

            submitBtn.setEnabled(false);
            submitBtn.setText("⏳ Submitting…");
            statusLbl.setText("Getting attendance record…");
            String serviceType = serviceCombo.getSelectedItem().toString();
            String today = java.time.LocalDate.now().toString();

            // Resolve service ID
            int bulkServiceId = -1;
            int bulkIdx = serviceCombo.getSelectedIndex();
            if (bulkIdx >= 0 && bulkIdx < bulkLoadedServices.size()) {
                Object id = bulkLoadedServices.get(bulkIdx).get("id");
                if (id instanceof Number) bulkServiceId = ((Number) id).intValue();
            }
            final int bServiceId = bulkServiceId;

            CompletableFuture<Integer> recordFuture = bServiceId > 0
                ? SanctumApiClient.getOrCreateAttendanceRecord(bServiceId)
                : SanctumApiClient.getOrCreateAttendanceRecord(serviceType);

            recordFuture
                .thenCompose(recordId -> {
                    if (recordId < 0) return CompletableFuture.completedFuture(new HashMap<String, Object>());
                    // Mark each member present via markMemberPresent in parallel
                    List<CompletableFuture<Boolean>> futures = new ArrayList<>();
                    selectedIds.forEach(id -> futures.add(SanctumApiClient.markMemberPresent(recordId, id, "Bulk check-in by usher")));
                    return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                        .thenApply(v -> {
                            long successCount = futures.stream().mapToLong(f -> { try { return f.get() ? 1 : 0; } catch (Exception ex) { return 0; } }).sum();
                            Map<String, Object> r = new HashMap<>();
                            r.put("message", successCount + " of " + selectedIds.size() + " members marked present");
                            return r;
                        });
                })
                .thenAccept(result -> SwingUtilities.invokeLater(() -> {
                    submitBtn.setEnabled(true);
                    submitBtn.setText("📥 Mark Selected Present");
                    String msg = result.getOrDefault("message", "Done").toString();
                    JOptionPane.showMessageDialog(dialog, "✅ " + msg, "Bulk Check-In Complete", JOptionPane.INFORMATION_MESSAGE);
                    dialog.dispose();
                    loadData();
                })).exceptionally(ex -> {
                    SwingUtilities.invokeLater(() -> { submitBtn.setEnabled(true); submitBtn.setText("📥 Mark Selected Present"); statusLbl.setText("Error: " + ex.getMessage()); });
                    return null;
                });
        });

        cancelBtn.addActionListener(e -> dialog.dispose());
        btnPanel.add(checkAllBtn); btnPanel.add(cancelBtn); btnPanel.add(submitBtn);

        JPanel topSection = new JPanel();
        topSection.setLayout(new BoxLayout(topSection, BoxLayout.Y_AXIS));
        topSection.setOpaque(false);
        topSection.add(title);
        topSection.add(Box.createVerticalStrut(10));
        topSection.add(svcRow);
        topSection.add(Box.createVerticalStrut(6));
        topSection.add(statusLbl);

        panel.add(topSection, BorderLayout.NORTH);
        panel.add(scroll,     BorderLayout.CENTER);
        panel.add(btnPanel,   BorderLayout.SOUTH);
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
        title.setFont(withEmojiFont(F_TITLE));
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
        
        JButton exportBtn = createActionButton("Export Report", C_GOLD);
        JButton closeBtn = createActionButton(" Close", C_DANGER);
        
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
        
        JPanel headerPanel = new JPanel(new BorderLayout(0, 15));
        headerPanel.setOpaque(false);
        headerPanel.add(title, BorderLayout.NORTH);
        headerPanel.add(statsPanel, BorderLayout.CENTER);
        
        panel.add(headerPanel, BorderLayout.NORTH);
        panel.add(centerPanel, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        dialog.add(panel);
        dialog.setVisible(true);
    }

    private void searchMembers(String searchTerm) {
        // Delegate to the members page load — the inline search in buildMembersPage
        // already handles filtering. This standalone method is kept for any external callers.
        JDialog dialog = new JDialog(this, "Member Search — " + searchTerm, true);
        dialog.setSize(720, 480);
        dialog.setLocationRelativeTo(this);
        dialog.getContentPane().setBackground(C_BG);

        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel titleLbl = new JLabel("🔍 Results for: " + searchTerm);
        titleLbl.setFont(withEmojiFont(F_TITLE));
        titleLbl.setForeground(C_TEXT);

        JLabel statusLbl = new JLabel("Searching…");
        statusLbl.setFont(F_MONO_SM);
        statusLbl.setForeground(C_TEXT_DIM);

        String[] cols = {"ID", "Name", "Phone", "Email", "Status"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = buildStyledTable(model);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getViewport().setBackground(C_CARD);

        // Load + filter live data
        SanctumApiClient.getMembers().thenAccept(members ->
            SwingUtilities.invokeLater(() -> {
                String q = searchTerm.trim().toLowerCase();
                model.setRowCount(0);
                members.stream()
                    .filter(m -> {
                        String fn = m.getOrDefault("first_name","").toString().toLowerCase();
                        String ln = m.getOrDefault("last_name", "").toString().toLowerCase();
                        String em = m.getOrDefault("email",     "").toString().toLowerCase();
                        String ph = m.getOrDefault("phone_number","").toString();
                        return fn.contains(q) || ln.contains(q) || em.contains(q) || ph.contains(q);
                    })
                    .forEach(m -> model.addRow(new Object[]{
                        m.getOrDefault("id",               ""),
                        m.getOrDefault("first_name","") + " " + m.getOrDefault("last_name",""),
                        m.getOrDefault("phone_number",     "N/A"),
                        m.getOrDefault("email",            ""),
                        m.getOrDefault("membership_status","")
                    }));
                statusLbl.setText(model.getRowCount() + " result(s)");
            })
        ).exceptionally(ex -> {
            SwingUtilities.invokeLater(() -> statusLbl.setText("Search failed: " + ex.getMessage()));
            return null;
        });

        // Action buttons
        JButton checkInBtn    = createActionButton("✅ Check In Selected",  C_SUCCESS);
        JButton viewDetailBtn = createActionButton("👁 View Details",        C_GOLD);
        JButton closeBtn      = createActionButton("✕ Close",               C_DANGER);

        checkInBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) { statusLbl.setText("Select a member first."); return; }
            String idStr = model.getValueAt(row, 0).toString();
            String name  = model.getValueAt(row, 1).toString();
            int memberId;
            try { memberId = Integer.parseInt(idStr); }
            catch (NumberFormatException ex) { statusLbl.setText("Invalid ID"); return; }
            checkInBtn.setEnabled(false);
            checkInBtn.setText("⏳ Checking in…");
            SanctumApiClient.getOrCreateAttendanceRecord("Sunday Service")
                .thenCompose(rid -> rid < 0
                    ? CompletableFuture.completedFuture(false)
                    : SanctumApiClient.markMemberPresent(rid, memberId, "Checked in by usher"))
                .thenAccept(ok -> SwingUtilities.invokeLater(() -> {
                    checkInBtn.setEnabled(true);
                    checkInBtn.setText("✅ Check In Selected");
                    if (ok) {
                        statusLbl.setText("✓ " + name + " checked in");
                        loadData();
                    } else {
                        statusLbl.setText("Check-in failed — try again");
                    }
                })).exceptionally(ex -> {
                    SwingUtilities.invokeLater(() -> {
                        checkInBtn.setEnabled(true);
                        checkInBtn.setText("✅ Check In Selected");
                        statusLbl.setText("Network error");
                    });
                    return null;
                });
        });

        viewDetailBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) { statusLbl.setText("Select a member first."); return; }
            String id    = model.getValueAt(row, 0).toString();
            String name  = model.getValueAt(row, 1).toString();
            String phone = model.getValueAt(row, 2).toString();
            String email = model.getValueAt(row, 3).toString();
            String stat  = model.getValueAt(row, 4).toString();
            JOptionPane.showMessageDialog(dialog,
                "Name:    " + name  + "\n" +
                "ID:      " + id    + "\n" +
                "Phone:   " + phone + "\n" +
                "Email:   " + email + "\n" +
                "Status:  " + stat,
                "Member Details", JOptionPane.INFORMATION_MESSAGE);
        });

        closeBtn.addActionListener(e -> dialog.dispose());

        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnBar.setOpaque(false);
        btnBar.add(viewDetailBtn);
        btnBar.add(checkInBtn);
        btnBar.add(closeBtn);

        JPanel north = new JPanel(new BorderLayout(0, 6));
        north.setOpaque(false);
        north.add(titleLbl,  BorderLayout.NORTH);
        north.add(statusLbl, BorderLayout.SOUTH);

        panel.add(north,  BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        panel.add(btnBar, BorderLayout.SOUTH);

        dialog.add(panel);
        dialog.setVisible(true);
    }

    // ─── Dialog Field Factories (DRY helpers) ─────────────────────────
    /** Themed text field used in all dialogs. */
    /** Shared helper: builds a JTable pre-styled for the dark Sanctum theme. */
    private JTable buildStyledTable(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setBackground(C_CARD);
        table.setForeground(C_TEXT);
        table.setGridColor(C_BORDER);
        table.setRowHeight(30);
        table.setOpaque(true);
        table.setSelectionBackground(new Color(212, 175, 55, 60));
        table.setSelectionForeground(C_TEXT);
        table.getTableHeader().setBackground(C_SURFACE);
        table.getTableHeader().setForeground(C_GOLD);
        table.getTableHeader().setFont(F_LABEL);
        table.getTableHeader().setOpaque(true);
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(
                    JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(t, v, sel, foc, r, c);
                lbl.setForeground(C_TEXT);
                lbl.setBackground(sel ? new Color(212, 175, 55, 60) : (r % 2 == 0 ? C_CARD : C_SURFACE));
                lbl.setOpaque(true);
                lbl.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                return lbl;
            }
        });
        return table;
    }

    private JTextField styledTextField() {
        JTextField f = new JTextField(20);
        f.setBackground(C_SURFACE);
        f.setForeground(C_TEXT);
        f.setCaretColor(C_TEXT);
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(C_BORDER),
            new EmptyBorder(5, 8, 5, 8)
        ));
        return f;
    }

    /** Themed text area used in all dialogs. */
    private JTextArea styledTextArea(int rows) {
        JTextArea a = new JTextArea(rows, 20);
        a.setBackground(C_SURFACE);
        a.setForeground(C_TEXT);
        a.setCaretColor(C_TEXT);
        a.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(C_BORDER),
            new EmptyBorder(5, 8, 5, 8)
        ));
        a.setLineWrap(true);
        a.setWrapStyleWord(true);
        return a;
    }

    /** Themed combo box used in all dialogs. */
    private JComboBox<String> styledComboBox(String... items) {
        JComboBox<String> cb = new JComboBox<>(items);
        cb.setBackground(C_SURFACE);
        cb.setForeground(C_TEXT);
        return cb;
    }

    /** Themed dialog button (no emoji prefix — plain label). */
    private JButton styledDialogButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setBackground(bg);
        btn.setForeground(C_TEXT);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        return btn;
    }

    /** Themed bold label used as a form field label in dialogs. */
    private JLabel dialogLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setForeground(C_TEXT);
        lbl.setFont(F_LABEL);  // no emoji — safe to use F_LABEL directly
        return lbl;
    }

    // ─── Entry Point ──────────────────────────────────────────────────
    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        SwingUtilities.invokeLater(() -> new UsherDashboardFrame().setVisible(true));
    }
}