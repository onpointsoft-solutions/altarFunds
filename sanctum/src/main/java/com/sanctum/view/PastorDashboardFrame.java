package com.sanctum.view;

import com.sanctum.api.SanctumApiClient;
import com.sanctum.auth.SessionManager;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.ArrayList;

/**
 * Enhanced Pastor Dashboard with Devotionals Management and Member Viewing
 * Sanctum Brand UI — glass-morphism cards, sidebar navigation, full backend integration
 */
public class PastorDashboardFrame extends JFrame {

    // ─── Sanctum Brand Color System ─────────────────────────────────────────────
    private static final Color C_BG          = new Color(14,  46,  42);   // Deep Emerald Green
    private static final Color C_SURFACE     = new Color(19,  58,  54);   // Dark Green Secondary
    private static final Color C_CARD        = new Color(28,  47,  44);   // Input Background
    private static final Color C_CARD_HOVER  = new Color(42,  74,  69);   // Hover State

    private static final Color C_BORDER      = new Color(42,  74,  69);   // Border Color
    private static final Color C_BORDER_LT   = new Color(66, 115, 107);  // Light Border

    // Accent palette — Gold spectrum
    private static final Color C_GOLD        = new Color(212, 175,  55);  // Gold Accent
    private static final Color C_GOLD_DIM    = new Color(212, 175,  55, 25);
    private static final Color C_GOLD_HOVER  = new Color(230, 199, 102);  // Light Gold Hover
    private static final Color C_GOLD_DIM_HOVER = new Color(230, 199, 102, 25);

    // Text hierarchy
    private static final Color C_TEXT        = new Color(255, 255, 255);  // White Text
    private static final Color C_TEXT_MID    = new Color(207, 207, 207);  // Soft Gray Secondary
    private static final Color C_TEXT_DIM    = new Color(156, 163, 175);  // Dim Text

    // Gradient endpoints for header glow
    private static final Color C_GLOW_TOP    = new Color(212, 175, 55, 60);
    private static final Color C_GLOW_BTM    = new Color(14,  46,  42,   0);

    // ─── Typography ──────────────────────────────────────────────────────────
    private static final Font F_DISPLAY      = new Font("Palatino Linotype", Font.BOLD, 26);
    private static final Font F_H1           = new Font("Palatino Linotype", Font.BOLD, 24);
    private static final Font F_HEADING      = new Font("Verdana",  Font.BOLD, 13);
    private static final Font F_LABEL        = new Font("Verdana",  Font.BOLD, 11);
    private static final Font F_MONO_LG      = new Font("Monospaced", Font.BOLD, 22);
    private static final Font F_MONO_SM      = new Font("Monospaced", Font.PLAIN, 11);
    private static final Font F_BODY         = new Font("Verdana",  Font.PLAIN, 12);
    private static final Font F_SMALL        = new Font("Verdana",  Font.PLAIN, 11);

    // ─── State ───────────────────────────────────────────────────────────────
    private int dragX, dragY;
    private JTable devotionalsTable;
    private JTable membersTable;
    private JPanel contentArea;
    private CardLayout cardLayout;
    private List<Map<String, Object>> currentDevotionals = new ArrayList<>();
    private List<Map<String, Object>> currentMembers = new ArrayList<>();

    // KPI value labels
    private JLabel lblTotalDevotionals;
    private JLabel lblThisMonth;
    private JLabel lblTotalMembers;
    private JLabel lblActiveMembers;

    public PastorDashboardFrame() {
        configureWindow();
        buildUI();
        loadData();
    }

    // ─── Window Setup ────────────────────────────────────────────────────────

    private void configureWindow() {
        setTitle("Sanctum — Pastor Dashboard");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setUndecorated(true);
        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        if (gd.isFullScreenSupported()) {
            gd.setFullScreenWindow(this);
        } else {
            setExtendedState(JFrame.MAXIMIZED_BOTH);
        }
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(C_BG);
        root.setOpaque(true);
        setContentPane(root);

        root.add(buildTopBar(), BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(0, 0));
        body.setOpaque(false);
        body.add(buildSidebar(), BorderLayout.WEST);
        body.add(buildMainArea(), BorderLayout.CENTER);
        root.add(body, BorderLayout.CENTER);
    }

    // ─── Top Bar ─────────────────────────────────────────────────────────────

    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Clean deep purple background
                g2.setColor(C_SURFACE);
                g2.fillRect(0, 0, getWidth(), getHeight());
                // Bottom separator line
                g2.setColor(C_BORDER);
                g2.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        bar.setOpaque(false);
        bar.setBorder(new EmptyBorder(16, 28, 16, 24));
        bar.setPreferredSize(new Dimension(0, 72));

        // Left — Brand
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setOpaque(false);

        // Logo
        JLabel icon;
        try {
            // Try to load logo from resources
            java.net.URL logoUrl = getClass().getResource("/com/sanctum/resources/logo.png");
            if (logoUrl != null) {
                ImageIcon logoIcon = new ImageIcon(logoUrl);
                Image scaledImage = logoIcon.getImage().getScaledInstance(32, 32, Image.SCALE_SMOOTH);
                icon = new JLabel(new ImageIcon(scaledImage));
            } else {
                // Fallback to text logo
                icon = new JLabel("✦");
                icon.setFont(new Font("Arial", Font.PLAIN, 28));
                icon.setForeground(C_GOLD);
            }
        } catch (Exception e) {
            // Fallback to text logo
            icon = new JLabel("✦");
            icon.setFont(new Font("Arial", Font.PLAIN, 28));
            icon.setForeground(C_GOLD);
        }
        left.add(icon);

        JLabel title = new JLabel("Sanctum");
        title.setFont(F_DISPLAY);
        title.setForeground(C_TEXT);

        JLabel divider = new JLabel("  /  ");
        divider.setFont(F_BODY);
        divider.setForeground(C_TEXT_DIM);

        JLabel sub = new JLabel("Pastor Dashboard");
        sub.setFont(new Font("Verdana", Font.PLAIN, 14));
        sub.setForeground(C_TEXT_MID);

        left.add(icon);
        left.add(title);
        left.add(divider);
        left.add(sub);

        // Right — User info and Window controls
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        controls.setOpaque(false);

        // User info and logout
        JPanel userInfo = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        userInfo.setOpaque(false);
        
        JLabel userLabel = new JLabel("👤 Pastor");
        userLabel.setFont(new Font("Verdana", Font.PLAIN, 12));
        userLabel.setForeground(C_TEXT_MID);
        
        JButton logoutBtn = new JButton("Logout") {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                if (getModel().isRollover()) {
                    g2.setColor(C_GOLD_DIM_HOVER);
                    g2.fillRect(0, 0, getWidth(), getHeight());
                }
                g2.dispose();
            }
        };
        logoutBtn.setFont(new Font("Verdana", Font.PLAIN, 11));
        logoutBtn.setForeground(C_TEXT_MID);
        logoutBtn.setBackground(C_CARD);
        logoutBtn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(C_BORDER),
            BorderFactory.createEmptyBorder(6, 12, 6, 12)
        ));
        logoutBtn.setFocusPainted(false);
        logoutBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        logoutBtn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { 
                logoutBtn.setForeground(C_GOLD); 
                logoutBtn.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(C_GOLD),
                    BorderFactory.createEmptyBorder(6, 12, 6, 12)
                ));
            }
            public void mouseExited(MouseEvent e) { 
                logoutBtn.setForeground(C_TEXT_MID);
                logoutBtn.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(C_BORDER),
                    BorderFactory.createEmptyBorder(6, 12, 6, 12)
                ));
            }
        });
        logoutBtn.addActionListener(e -> performLogout());
        
        userInfo.add(userLabel);
        userInfo.add(logoutBtn);

        JButton minimize = buildWindowBtn("─");
        JButton close = buildWindowBtn("✕");
        close.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { close.setBackground(C_GOLD_HOVER); }
            public void mouseExited(MouseEvent e) { close.setBackground(C_CARD); }
        });

        minimize.addActionListener(e -> setState(JFrame.ICONIFIED));
        close.addActionListener(e -> {
            dispose();
            new LoginFrame().setVisible(true);
        });

        controls.add(userInfo);
        controls.add(minimize);
        controls.add(close);

        bar.add(left, BorderLayout.WEST);
        bar.add(controls, BorderLayout.EAST);

        // Drag support
        bar.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) { dragX = e.getX(); dragY = e.getY(); }
        });
        bar.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                setLocation(getX() + e.getX() - dragX, getY() + e.getY() - dragY);
            }
        });

        return bar;
    }

    private JButton buildWindowBtn(String text) {
        JButton btn = new JButton(text);
        btn.setPreferredSize(new Dimension(34, 34));
        btn.setBackground(C_CARD);
        btn.setForeground(C_TEXT_MID);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createLineBorder(C_BORDER, 1));
        btn.setFont(F_HEADING);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(C_CARD_HOVER); }
            public void mouseExited(MouseEvent e) { btn.setBackground(C_CARD); }
        });
        return btn;
    }

    // ─── Sidebar ──────────────────────────────────────────────────────────────

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(C_SURFACE);
                g2.fillRect(0, 0, getWidth(), getHeight());
                // Right border
                g2.setColor(C_BORDER);
                g2.drawLine(getWidth() - 1, 0, getWidth() - 1, getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setOpaque(false);
        sidebar.setBorder(new EmptyBorder(32, 14, 28, 14));
        sidebar.setPreferredSize(new Dimension(264, 0));

        // Logo block
        JLabel logo = new JLabel("✦");
        logo.setFont(new Font("Arial", Font.PLAIN, 40));
        logo.setForeground(C_GOLD);
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel brand = new JLabel("SANCTUM");
        brand.setFont(new Font("Verdana", Font.BOLD, 12));
        brand.setForeground(C_GOLD_HOVER);
        brand.setAlignmentX(Component.CENTER_ALIGNMENT);
        brand.setBorder(new EmptyBorder(6, 0, 2, 0));

        JLabel brandSub = new JLabel("Pastor Portal");
        brandSub.setFont(F_SMALL);
        brandSub.setForeground(C_TEXT_DIM);
        brandSub.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Divider
        JPanel divider = new JPanel();
        divider.setBackground(C_BORDER);
        divider.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        divider.setPreferredSize(new Dimension(0, 1));

        // Nav items
        JPanel nav = new JPanel();
        nav.setLayout(new BoxLayout(nav, BoxLayout.Y_AXIS));
        nav.setOpaque(false);
        nav.setBorder(new EmptyBorder(20, 0, 0, 0));

        nav.add(addNavItem("Overview",        "overview"));
        nav.add(Box.createVerticalStrut(3));
        nav.add(addNavItem("Devotionals",     "devotionals"));
        nav.add(Box.createVerticalStrut(3));
        nav.add(addNavItem("Members",         "members"));
        nav.add(Box.createVerticalStrut(3));
        nav.add(addNavItem("Prayer",          "prayer"));
        nav.add(Box.createVerticalStrut(3));
        nav.add(addNavItem("Counseling",      "counseling"));
        nav.add(Box.createVerticalStrut(3));
        nav.add(addNavItem("Events",          "events"));
        nav.add(Box.createVerticalStrut(3));
        nav.add(addNavItem("Settings",         "settings"));

        // Spacer
        nav.add(Box.createVerticalGlue());
        nav.add(Box.createVerticalStrut(24));
        nav.add(addNavItem("Sign Out",        "signout"));

        sidebar.add(logo);
        sidebar.add(brand);
        sidebar.add(brandSub);
        sidebar.add(Box.createVerticalStrut(20));
        sidebar.add(divider);
        sidebar.add(nav);

        return sidebar;
    }

    private JButton addNavItem(String text, String page) {
        JButton btn = new JButton(text) {
            private boolean hovered = false;

            {
                addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
                    public void mouseExited(MouseEvent e) { hovered = false; repaint(); }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (hovered) {
                    // Clean gold hover
                    g2.setColor(C_GOLD_DIM_HOVER);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                    g2.setColor(C_GOLD);
                    g2.fillRoundRect(0, 6, 3, getHeight() - 12, 3, 3);
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        btn.setBackground(new Color(0, 0, 0, 0));
        btn.setForeground(hovered(btn) ? C_TEXT : C_TEXT_MID);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        btn.setFont(F_BODY);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);

        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setForeground(C_TEXT); }
            public void mouseExited(MouseEvent e) { btn.setForeground(C_TEXT_MID); }
        });

        btn.addActionListener(e -> {
            switch (page) {
                case "overview":    showOverviewPage(); break;
                case "devotionals": showDevotionalsPage(); break;
                case "members":     showMembersPage(); break;
                case "prayer":      showPrayerPage(); break;
                case "counseling":  showCounselingPage(); break;
                case "events":      showEventsPage(); break;
                case "settings":    showSettingsPage(); break;
                case "signout": {
                    dispose();
                    new LoginFrame().setVisible(true);
                    break;
                }
            }
        });

        return btn;
    }

    // Helper to avoid compile error — btn hover state managed via anonymous class
    private boolean hovered(JButton btn) { return false; }

    // ─── Main Area ────────────────────────────────────────────────────────────

    private JPanel buildMainArea() {
        JPanel main = new JPanel(new BorderLayout());
        main.setOpaque(false);

        cardLayout = new CardLayout();
        contentArea = new JPanel(cardLayout);
        contentArea.setOpaque(false);

        contentArea.add(buildDashboardContent(), "overview");
        contentArea.add(buildDevotionalsPage(),  "devotionals");
        contentArea.add(buildMembersPage(),      "members");
        contentArea.add(buildPrayerPage(),       "prayer");
        contentArea.add(buildCounselingPage(),   "counseling");
        contentArea.add(buildEventsPage(),       "events");
        contentArea.add(buildSettingsPage(),     "settings");

        JScrollPane scroll = new JScrollPane(contentArea);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getVerticalScrollBar().setUI(new ModernScrollBarUI());
        main.add(scroll, BorderLayout.CENTER);

        return main;
    }

    // ─── Dashboard Content ────────────────────────────────────────────────────

    private JPanel buildDashboardContent() {
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(36, 36, 36, 36));

        // Page header
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(0, 0, 28, 0));
        header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));

        JLabel title = new JLabel("Pastoral Overview");
        title.setFont(F_H1);
        title.setForeground(C_TEXT);

        JLabel subtitle = new JLabel("Manage your ministry and spiritual leadership");
        subtitle.setFont(F_BODY);
        subtitle.setForeground(C_TEXT_MID);
        subtitle.setBorder(new EmptyBorder(6, 0, 0, 0));

        header.add(title,    BorderLayout.NORTH);
        header.add(subtitle, BorderLayout.CENTER);

        content.add(header);
        content.add(buildKpiRow());
        content.add(Box.createVerticalStrut(28));
        content.add(buildQuickActions());
        content.add(Box.createVerticalStrut(28));
        content.add(buildRecentActivities());

        return content;
    }

    private JPanel buildKpiRow() {
        JPanel row = new JPanel(new GridLayout(1, 4, 16, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 118));

        JPanel cardTD = buildKpiCard("Total Devotionals", "0", "All time",     C_GOLD,     "📖");
        JPanel cardMD = buildKpiCard("This Month",        "0", "Devotionals",  C_GOLD_HOVER, "📅");
        JPanel cardTE = buildKpiCard("Total Members",     "0", "Congregation", C_TEXT_MID,   "👥");
        JPanel cardNB = buildKpiCard("Active Members",    "0", "This season",  C_GOLD_DIM,   "✨");

        lblTotalDevotionals = findValueLabel(cardTD);
        lblThisMonth        = findValueLabel(cardMD);
        lblTotalMembers     = findValueLabel(cardTE);
        lblActiveMembers    = findValueLabel(cardNB);

        row.add(cardTD); row.add(cardMD); row.add(cardTE); row.add(cardNB);
        return row;
    }

    private JPanel buildKpiCard(String title, String value, String subtitle, Color accent, String icon) {
        JPanel card = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Card background with subtle purple tint
                g2.setColor(C_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);

                // Clean accent band
                g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 40));
                g2.fillRect(0, 0, getWidth(), (int)(getHeight() * 0.3));

                // Left accent bar
                g2.setColor(accent);
                g2.fillRoundRect(0, 10, 4, getHeight() - 20, 4, 4);

                // Border
                g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 50));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);

                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setBorder(new EmptyBorder(18, 20, 18, 20));
        card.setOpaque(false);

        // Top row: icon + title
        JPanel top = new JPanel(new BorderLayout(8, 0));
        top.setOpaque(false);

        JLabel iconLabel = new JLabel(icon);
        iconLabel.setFont(F_H1);
        iconLabel.setForeground(accent);

        JLabel titleLabel = new JLabel(title.toUpperCase());
        titleLabel.setFont(new Font("Verdana", Font.BOLD, 9));
        titleLabel.setForeground(C_TEXT_DIM);
        titleLabel.setVerticalAlignment(SwingConstants.BOTTOM);

        top.add(iconLabel,  BorderLayout.WEST);
        top.add(titleLabel, BorderLayout.CENTER);

        // Value
        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(F_MONO_LG);
        valueLabel.setForeground(C_TEXT);
        valueLabel.setHorizontalAlignment(SwingConstants.CENTER);

        // Subtitle
        JLabel subtitleLabel = new JLabel(subtitle);
        subtitleLabel.setFont(new Font("Verdana", Font.PLAIN, 10));
        subtitleLabel.setForeground(C_TEXT_DIM);
        subtitleLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setOpaque(false);
        center.add(Box.createVerticalStrut(6));
        center.add(valueLabel);
        center.add(Box.createVerticalStrut(3));
        center.add(subtitleLabel);

        card.add(top,    BorderLayout.NORTH);
        card.add(center, BorderLayout.CENTER);

        return card;
    }

    private JLabel findValueLabel(JPanel card) {
        for (Component c : card.getComponents()) {
            if (c instanceof JPanel) {
                for (Component cc : ((JPanel) c).getComponents()) {
                    if (cc instanceof JLabel && ((JLabel) cc).getFont().equals(F_MONO_LG)) {
                        return (JLabel) cc;
                    }
                }
            }
        }
        return null;
    }

    private JPanel buildQuickActions() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, 230));

        JLabel sectionTitle = new JLabel("QUICK ACTIONS");
        sectionTitle.setFont(new Font("Verdana", Font.BOLD, 10));
        sectionTitle.setForeground(C_TEXT_DIM);
        sectionTitle.setBorder(new EmptyBorder(0, 0, 12, 0));

        JPanel grid = new JPanel(new GridLayout(2, 3, 14, 14));
        grid.setOpaque(false);

        grid.add(buildActionButton("New Devotional",   "Create daily devotional", C_GOLD));
        grid.add(buildActionButton("View Members",     "Manage congregation",     C_GOLD_HOVER));
        grid.add(buildActionButton("Prayer Requests",  "View and intercede",      C_TEXT_MID));
        grid.add(buildActionButton("Counseling",       "Schedule sessions",       C_GOLD_DIM));
        grid.add(buildActionButton("Events",           "Church activities",       C_GOLD));
        grid.add(buildActionButton("Reports",          "Ministry insights",       C_GOLD_HOVER));

        wrapper.add(sectionTitle, BorderLayout.NORTH);
        wrapper.add(grid,         BorderLayout.CENTER);
        return wrapper;
    }

    private JPanel buildActionButton(String title, String description, Color accent) {
        JPanel btn = new JPanel(new BorderLayout()) {
            private boolean hovered = false;

            {
                addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
                    public void mouseExited(MouseEvent e) { hovered = false; repaint(); }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(hovered ? C_CARD_HOVER : C_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                if (hovered) {
                    g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 25));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                    g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 80));
                    g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                } else {
                    g2.setColor(C_BORDER);
                    g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setBorder(new EmptyBorder(16, 16, 16, 16));
        btn.setOpaque(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Verdana", Font.BOLD, 11));
        titleLabel.setForeground(C_TEXT);

        JLabel descLabel = new JLabel(description);
        descLabel.setFont(F_SMALL);
        descLabel.setForeground(C_TEXT_DIM);

        JPanel text = new JPanel();
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.setOpaque(false);
        text.add(titleLabel);
        text.add(Box.createVerticalStrut(3));
        text.add(descLabel);

        btn.add(text, BorderLayout.CENTER);

        return btn;
    }

    private JPanel buildRecentActivities() {
        JPanel panel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(C_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.setColor(C_BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        panel.setBorder(new EmptyBorder(22, 22, 22, 22));
        panel.setOpaque(false);
        panel.setPreferredSize(new Dimension(0, 260));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 260));

        JLabel title = new JLabel("RECENT ACTIVITIES");
        title.setFont(new Font("Verdana", Font.BOLD, 10));
        title.setForeground(C_TEXT_DIM);
        title.setBorder(new EmptyBorder(0, 0, 14, 0));

        JLabel placeholder = new JLabel("Activity feed will appear here…");
        placeholder.setFont(F_BODY);
        placeholder.setForeground(C_TEXT_DIM);
        placeholder.setHorizontalAlignment(SwingConstants.CENTER);

        panel.add(title,       BorderLayout.NORTH);
        panel.add(placeholder, BorderLayout.CENTER);

        return panel;
    }

    // ─── Devotionals Page ─────────────────────────────────────────────────────

    private JPanel buildDevotionalsPage() {
        JPanel page = new JPanel(new BorderLayout());
        page.setOpaque(false);
        page.setBorder(new EmptyBorder(36, 36, 36, 36));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(0, 0, 24, 0));

        JLabel title = new JLabel("📖  Devotionals Management");
        title.setFont(F_H1);
        title.setForeground(C_TEXT);

        JButton createBtn = buildAccentButton("✦ New Devotional", C_GOLD);
        createBtn.addActionListener(e -> openDevotionalCreator());

        header.add(title,     BorderLayout.WEST);
        header.add(createBtn, BorderLayout.EAST);

        String[] cols = {"Date", "Title", "Scripture", "Comments", "Reactions"};
        DefaultTableModel model = new DefaultTableModel(new Object[][]{}, cols) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        devotionalsTable = new JTable(model) {
            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                Component c = super.prepareRenderer(renderer, row, column);
                if (!isRowSelected(row)) {
                    c.setBackground(row % 2 == 0 ? C_CARD : C_CARD_HOVER);
                }
                return c;
            }
        };
        
        // Add click listener for viewing details
        devotionalsTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = devotionalsTable.rowAtPoint(e.getPoint());
                    if (row >= 0) {
                        showDevotionalDetails(row);
                    }
                }
            }
        });
        styleTable(devotionalsTable);

        JScrollPane scroll = new JScrollPane(devotionalsTable);
        scroll.setOpaque(false);
        scroll.getViewport().setBackground(C_CARD);
        scroll.setBorder(BorderFactory.createLineBorder(C_BORDER, 1));
        scroll.getVerticalScrollBar().setUI(new ModernScrollBarUI());

        page.add(header, BorderLayout.NORTH);
        page.add(scroll, BorderLayout.CENTER);

        return page;
    }

    // ─── Members Page ─────────────────────────────────────────────────────────

    private JPanel buildMembersPage() {
        JPanel page = new JPanel(new BorderLayout());
        page.setOpaque(false);
        page.setBorder(new EmptyBorder(36, 36, 36, 36));

        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(0, 0, 24, 0));

        JLabel title = new JLabel("👥  Church Members");
        title.setFont(F_H1);
        title.setForeground(C_TEXT);

        JTextField search = new JTextField();
        search.setBackground(C_CARD);
        search.setForeground(C_TEXT);
        search.setCaretColor(C_GOLD);
        search.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(C_BORDER, 1),
            BorderFactory.createEmptyBorder(8, 14, 8, 14)
        ));
        search.setFont(F_BODY);
        search.setPreferredSize(new Dimension(220, 38));
        search.setText("Search members…");
        search.setForeground(C_TEXT_DIM);
        search.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                if (search.getText().equals("Search members…")) {
                    search.setText("");
                    search.setForeground(C_TEXT);
                }
            }
            public void focusLost(FocusEvent e) {
                if (search.getText().isEmpty()) {
                    search.setText("Search members…");
                    search.setForeground(C_TEXT_DIM);
                }
            }
        });

        header.add(title,  BorderLayout.WEST);
        header.add(search, BorderLayout.EAST);

        String[] cols = {"Name", "Email", "Membership #", "Status", "Date"};
        DefaultTableModel model = new DefaultTableModel(new Object[][]{}, cols) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        membersTable = new JTable(model);
        styleTable(membersTable);

        JScrollPane scroll = new JScrollPane(membersTable);
        scroll.setOpaque(false);
        scroll.getViewport().setBackground(C_CARD);
        scroll.setBorder(BorderFactory.createLineBorder(C_BORDER, 1));
        scroll.getVerticalScrollBar().setUI(new ModernScrollBarUI());

        page.add(header, BorderLayout.NORTH);
        page.add(scroll, BorderLayout.CENTER);

        return page;
    }

    // ─── Placeholder Pages ────────────────────────────────────────────────────

    private JPanel buildPrayerPage()     { return buildPlaceholderPage("🙏  Prayer Ministry",  "Manage prayer requests and intercession",     C_GOLD); }
    private JPanel buildCounselingPage() { return buildPlaceholderPage("💬  Counseling",        "Pastoral counseling and appointments",         C_GOLD_HOVER); }
    private JPanel buildEventsPage()     { return buildPlaceholderPage("📅  Events",            "Church events and activities",                 C_GOLD_DIM); }
    private JPanel buildSettingsPage()   { return buildPlaceholderPage("⚙️  Settings",           "Pastoral preferences and configuration",       C_GOLD); }

    private JPanel buildPlaceholderPage(String title, String subtitle, Color accent) {
        JPanel page = new JPanel(new BorderLayout());
        page.setOpaque(false);
        page.setBorder(new EmptyBorder(36, 36, 36, 36));

        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setOpaque(false);

        JLabel titleLabel = new JLabel(title.trim());
        titleLabel.setFont(F_H1);
        titleLabel.setForeground(C_TEXT);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitleLabel = new JLabel(subtitle);
        subtitleLabel.setFont(F_BODY);
        subtitleLabel.setForeground(C_TEXT_MID);
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton comingSoon = buildAccentButton("Coming Soon", accent);
        comingSoon.setAlignmentX(Component.CENTER_ALIGNMENT);

        center.add(Box.createVerticalGlue());
        center.add(titleLabel);
        center.add(Box.createVerticalStrut(8));
        center.add(subtitleLabel);
        center.add(Box.createVerticalStrut(24));
        center.add(comingSoon);
        center.add(Box.createVerticalGlue());

        page.add(center, BorderLayout.CENTER);
        return page;
    }

    // ─── Shared Accent Button ─────────────────────────────────────────────────

    private JButton buildAccentButton(String text, Color accent) {
        JButton btn = new JButton(text) {
            private boolean hovered = false;

            {
                addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
                    public void mouseExited(MouseEvent e) { hovered = false; repaint(); }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color base = hovered
                    ? new Color(Math.min(accent.getRed() + 20, 255), Math.min(accent.getGreen() + 20, 255), Math.min(accent.getBlue() + 20, 255))
                    : accent;
                g2.setColor(base);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setContentAreaFilled(false);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        btn.setFont(new Font("Verdana", Font.BOLD, 11));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    // ─── Navigation ───────────────────────────────────────────────────────────

    private void showOverviewPage()    { cardLayout.show(contentArea, "overview");    loadData(); }
    private void showDevotionalsPage() { cardLayout.show(contentArea, "devotionals"); loadDevotionalsData(); }
    private void showMembersPage()     { cardLayout.show(contentArea, "members");     loadMembersData(); }
    private void showPrayerPage()      { cardLayout.show(contentArea, "prayer"); }
    private void showCounselingPage()  { cardLayout.show(contentArea, "counseling"); }
    private void showEventsPage()      { cardLayout.show(contentArea, "events"); }
    private void showSettingsPage()    { cardLayout.show(contentArea, "settings"); }

    // ─── Devotional Creator ─────────────────────────────────────────────────────

    private void openDevotionalCreator() {
        JDialog dialog = new JDialog(this, "Create New Devotional", true);
        dialog.setSize(600, 500);
        dialog.setLocationRelativeTo(this);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        JPanel mainPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(C_BG);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        mainPanel.setBorder(new EmptyBorder(24, 24, 24, 24));

        // Header
        JLabel header = new JLabel("✦ Create New Devotional");
        header.setFont(F_H1);
        header.setForeground(C_TEXT);
        header.setBorder(new EmptyBorder(0, 0, 20, 0));

        // Form
        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setOpaque(false);

        // Title field
        JLabel titleLabel = new JLabel("Title:");
        titleLabel.setFont(F_LABEL);
        titleLabel.setForeground(C_TEXT_MID);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JTextField titleField = new JTextField();
        titleField.setBackground(C_CARD);
        titleField.setForeground(C_TEXT);
        titleField.setCaretColor(C_GOLD);
        titleField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(C_BORDER, 1),
            BorderFactory.createEmptyBorder(12, 14, 12, 14)
        ));
        titleField.setFont(F_BODY);
        titleField.setMaximumSize(new Dimension(Integer.MAX_VALUE, titleField.getPreferredSize().height));

        // Scripture field
        JLabel scriptureLabel = new JLabel("Scripture Reference:");
        scriptureLabel.setFont(F_LABEL);
        scriptureLabel.setForeground(C_TEXT_MID);
        scriptureLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        scriptureLabel.setBorder(new EmptyBorder(16, 0, 8, 0));

        JTextField scriptureField = new JTextField();
        scriptureField.setBackground(C_CARD);
        scriptureField.setForeground(C_TEXT);
        scriptureField.setCaretColor(C_GOLD);
        scriptureField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(C_BORDER, 1),
            BorderFactory.createEmptyBorder(12, 14, 12, 14)
        ));
        scriptureField.setFont(F_BODY);
        scriptureField.setMaximumSize(new Dimension(Integer.MAX_VALUE, scriptureField.getPreferredSize().height));

        // Content area
        JLabel contentLabel = new JLabel("Content:");
        contentLabel.setFont(F_LABEL);
        contentLabel.setForeground(C_TEXT_MID);
        contentLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentLabel.setBorder(new EmptyBorder(16, 0, 8, 0));

        JTextArea contentArea = new JTextArea(8, 20);
        contentArea.setBackground(C_CARD);
        contentArea.setForeground(C_TEXT);
        contentArea.setCaretColor(C_GOLD);
        contentArea.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(C_BORDER, 1),
            BorderFactory.createEmptyBorder(12, 14, 12, 14)
        ));
        contentArea.setFont(F_BODY);
        contentArea.setLineWrap(true);
        contentArea.setWrapStyleWord(true);

        JScrollPane contentScroll = new JScrollPane(contentArea);
        contentScroll.setOpaque(false);
        contentScroll.getViewport().setOpaque(false);
        contentScroll.setBorder(null);
        contentScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        buttonPanel.setOpaque(false);

        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.setBackground(C_CARD);
        cancelBtn.setForeground(C_TEXT_MID);
        cancelBtn.setFocusPainted(false);
        cancelBtn.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        cancelBtn.setFont(F_BODY);
        cancelBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JButton createBtn = new JButton("Create Devotional");
        createBtn.setBackground(C_GOLD);
        createBtn.setForeground(Color.WHITE);
        createBtn.setFocusPainted(false);
        createBtn.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        createBtn.setFont(F_BODY);
        createBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        buttonPanel.add(cancelBtn);
        buttonPanel.add(createBtn);

        // Add components to form
        form.add(titleLabel);
        form.add(Box.createVerticalStrut(4));
        form.add(titleField);
        form.add(scriptureLabel);
        form.add(Box.createVerticalStrut(4));
        form.add(scriptureField);
        form.add(contentLabel);
        form.add(Box.createVerticalStrut(4));
        form.add(contentScroll);
        form.add(Box.createVerticalStrut(20));
        form.add(buttonPanel);

        mainPanel.add(header, BorderLayout.NORTH);
        mainPanel.add(form, BorderLayout.CENTER);

        // Button actions
        cancelBtn.addActionListener(e -> dialog.dispose());

        createBtn.addActionListener(e -> {
            String title = titleField.getText().trim();
            String scripture = scriptureField.getText().trim();
            String content = contentArea.getText().trim();

            if (title.isEmpty() || content.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, 
                    "Please fill in both title and content fields.", 
                    "Missing Information", 
                    JOptionPane.WARNING_MESSAGE);
                return;
            }

            createBtn.setEnabled(false);
            createBtn.setText("Creating...");

            SanctumApiClient.createDevotional(title, content, scripture).thenAccept(success -> {
                SwingUtilities.invokeLater(() -> {
                    if (success) {
                        JOptionPane.showMessageDialog(dialog, 
                            "Devotional created successfully!", 
                            "Success", 
                            JOptionPane.INFORMATION_MESSAGE);
                        dialog.dispose();
                        // Refresh devotionals data
                        loadDevotionalsData();
                        loadKpiData();
                    } else {
                        JOptionPane.showMessageDialog(dialog, 
                            "Failed to create devotional. Please try again.", 
                            "Error", 
                            JOptionPane.ERROR_MESSAGE);
                    }
                    createBtn.setEnabled(true);
                    createBtn.setText("Create Devotional");
                });
            });
        });

        dialog.setContentPane(mainPanel);
        dialog.setVisible(true);
    }

    // ─── Devotional Details View ─────────────────────────────────────────────────

    private void showDevotionalDetails(int row) {
        DefaultTableModel model = (DefaultTableModel) devotionalsTable.getModel();
        
        // Store current devotionals data for access
        if (currentDevotionals == null || currentDevotionals.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "No devotional data available. Please refresh the devotionals list.", 
                "No Data", 
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        if (row >= currentDevotionals.size()) {
            return;
        }

        Map<String, Object> devotional = currentDevotionals.get(row);
        
        JDialog dialog = new JDialog(this, "Devotional Details", true);
        dialog.setSize(700, 600);
        dialog.setLocationRelativeTo(this);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        JPanel mainPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(C_BG);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        mainPanel.setBorder(new EmptyBorder(24, 24, 24, 24));

        // Header
        JLabel header = new JLabel("📖 " + devotional.getOrDefault("title", "Untitled"));
        header.setFont(F_H1);
        header.setForeground(C_TEXT);
        header.setBorder(new EmptyBorder(0, 0, 16, 0));

        // Content panel
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setOpaque(false);

        // Scripture
        String scripture = (String) devotional.getOrDefault("scripture_reference", "No scripture reference");
        JLabel scriptureLabel = new JLabel("📜 " + scripture);
        scriptureLabel.setFont(new Font("Verdana", Font.ITALIC, 14));
        scriptureLabel.setForeground(C_GOLD_HOVER);
        scriptureLabel.setBorder(new EmptyBorder(0, 0, 16, 0));
        scriptureLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Date
        String date = devotional.getOrDefault("date", "").toString();
        if (date.length() > 10) date = date.substring(0, 10);
        JLabel dateLabel = new JLabel("📅 Published: " + date);
        dateLabel.setFont(F_SMALL);
        dateLabel.setForeground(C_TEXT_DIM);
        dateLabel.setBorder(new EmptyBorder(0, 0, 20, 0));
        dateLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Content
        String content = (String) devotional.getOrDefault("content", "No content available");
        JTextArea contentArea = new JTextArea(content);
        contentArea.setBackground(C_CARD);
        contentArea.setForeground(C_TEXT);
        contentArea.setEditable(false);
        contentArea.setLineWrap(true);
        contentArea.setWrapStyleWord(true);
        contentArea.setFont(F_BODY);
        contentArea.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(C_BORDER, 1),
            BorderFactory.createEmptyBorder(16, 16, 16, 16)
        ));
        contentArea.setAlignmentX(Component.LEFT_ALIGNMENT);

        JScrollPane contentScroll = new JScrollPane(contentArea);
        contentScroll.setOpaque(false);
        contentScroll.getViewport().setOpaque(false);
        contentScroll.setBorder(null);
        contentScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));

        // Stats
        JPanel statsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 0));
        statsPanel.setOpaque(false);
        statsPanel.setBorder(new EmptyBorder(20, 0, 0, 0));
        statsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        String comments = devotional.getOrDefault("comments_count", "0").toString();
        String reactions = devotional.getOrDefault("reactions_count", "0").toString();

        JLabel commentsLabel = new JLabel("💬 " + comments + " Comments");
        commentsLabel.setFont(F_BODY);
        commentsLabel.setForeground(C_TEXT_MID);

        JLabel reactionsLabel = new JLabel("❤️ " + reactions + " Reactions");
        reactionsLabel.setFont(F_BODY);
        reactionsLabel.setForeground(C_TEXT_MID);

        statsPanel.add(commentsLabel);
        statsPanel.add(reactionsLabel);

        // Close button
        JButton closeBtn = new JButton("Close");
        closeBtn.setBackground(C_GOLD);
        closeBtn.setForeground(Color.WHITE);
        closeBtn.setFocusPainted(false);
        closeBtn.setBorder(BorderFactory.createEmptyBorder(10, 24, 10, 24));
        closeBtn.setFont(F_BODY);
        closeBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        closeBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        closeBtn.addActionListener(e -> dialog.dispose());

        // Add components
        contentPanel.add(scriptureLabel);
        contentPanel.add(dateLabel);
        contentPanel.add(contentScroll);
        contentPanel.add(statsPanel);
        contentPanel.add(Box.createVerticalStrut(24));
        contentPanel.add(closeBtn);

        mainPanel.add(header, BorderLayout.NORTH);
        mainPanel.add(contentPanel, BorderLayout.CENTER);

        dialog.setContentPane(mainPanel);
        dialog.setVisible(true);
    }

    // ─── Data Loading ─────────────────────────────────────────────────────────

    private void loadData() {
        loadDevotionalsData();
        loadMembersData();
        loadKpiData();
    }

    private void loadDevotionalsData() {
        SanctumApiClient.getDevotionals().thenAccept(devotionals -> {
            SwingUtilities.invokeLater(() -> updateDevotionalsTable(devotionals));
        }).exceptionally(ex -> { System.err.println("Devotionals load error: " + ex.getMessage()); return null; });
    }

    private void updateDevotionalsTable(List<Map<String, Object>> devotionals) {
        // Store current devotionals for details view
        currentDevotionals = new ArrayList<>(devotionals);
        
        if (devotionalsTable == null) return;
        DefaultTableModel model = (DefaultTableModel) devotionalsTable.getModel();
        model.setRowCount(0);

        if (devotionals.isEmpty()) {
            model.addRow(new Object[]{"—", "No devotionals available", "—", "—", "—"});
        } else {
            for (Map<String, Object> d : devotionals) {
                String date = d.getOrDefault("date", "—").toString();
                if (date.length() > 10) date = date.substring(0, 10);
                model.addRow(new Object[]{
                    date,
                    d.getOrDefault("title", "—"),
                    d.getOrDefault("scripture_reference", "—"),
                    d.getOrDefault("comments_count", "0"),
                    d.getOrDefault("reactions_count", "0")
                });
            }
        }
    }

    private void loadMembersData() {
        SanctumApiClient.getMembers().thenAccept(members ->
            SwingUtilities.invokeLater(() -> updateMembersTable(members))
        ).exceptionally(ex -> { System.err.println("Members load error: " + ex.getMessage()); return null; });
    }

    private void loadKpiData() {
        // Load members count
        SanctumApiClient.getMembers().thenAccept(members -> {
            SwingUtilities.invokeLater(() -> {
                if (lblTotalMembers != null) {
                    lblTotalMembers.setText(String.valueOf(members.size()));
                }
                // Calculate active members (those with membership status not equal to 'inactive')
                long activeCount = members.stream()
                    .filter(m -> {
                        String status = (String) m.getOrDefault("membership_status", "");
                        return !"inactive".equalsIgnoreCase(status) && !"former".equalsIgnoreCase(status);
                    })
                    .count();
                if (lblActiveMembers != null) {
                    lblActiveMembers.setText(String.valueOf(activeCount));
                }
            });
        }).exceptionally(ex -> {
            System.err.println("KPI members load error: " + ex.getMessage());
            return null;
        });

        // Load devotionals count
        SanctumApiClient.getDevotionals().thenAccept(devotionals -> {
            SwingUtilities.invokeLater(() -> {
                if (lblTotalDevotionals != null) {
                    lblTotalDevotionals.setText(String.valueOf(devotionals.size()));
                }
                
                // Calculate this month's devotionals
                java.time.LocalDateTime now = java.time.LocalDateTime.now();
                int currentMonth = now.getMonthValue();
                int currentYear = now.getYear();
                
                long thisMonthCount = devotionals.stream()
                    .filter(d -> {
                        String dateStr = d.getOrDefault("date", "").toString();
                        if (dateStr.isEmpty()) return false;
                        try {
                            java.time.LocalDate date = java.time.LocalDate.parse(dateStr.substring(0, 10));
                            return date.getMonthValue() == currentMonth && date.getYear() == currentYear;
                        } catch (Exception e) {
                            return false;
                        }
                    })
                    .count();
                
                if (lblThisMonth != null) {
                    lblThisMonth.setText(String.valueOf(thisMonthCount));
                }
            });
        }).exceptionally(ex -> {
            System.err.println("KPI devotionals load error: " + ex.getMessage());
            return null;
        });
    }

    private void updateMembersTable(List<Map<String, Object>> members) {
        // Store current members for details view
        currentMembers = new ArrayList<>(members);
        
        if (membersTable == null) return;
        DefaultTableModel model = (DefaultTableModel) membersTable.getModel();
        model.setRowCount(0);
        if (members.isEmpty()) {
            model.addRow(new Object[]{"—", "No members available", "—", "—", "—"});
        } else {
            for (Map<String, Object> m : members) {
                model.addRow(new Object[]{
                    m.getOrDefault("first_name", "") + " " + m.getOrDefault("last_name", ""),
                    m.getOrDefault("email", ""),
                    m.getOrDefault("membership_number", "—"),
                    m.getOrDefault("membership_status", "—"),
                    m.getOrDefault("membership_date", "—")
                });
            }
        }
    }

    // ─── Table Styling ────────────────────────────────────────────────────────

    private void styleTable(JTable table) {
        table.setFont(F_SMALL);
        table.setForeground(C_TEXT);
        table.setBackground(C_CARD);
        table.setGridColor(new Color(C_BORDER.getRed(), C_BORDER.getGreen(), C_BORDER.getBlue(), 80));
        table.setRowHeight(36);
        table.setShowVerticalLines(false);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setSelectionBackground(C_GOLD_DIM);
        table.setSelectionForeground(C_TEXT);
        table.setFillsViewportHeight(true);

        // Header
        JTableHeader header = table.getTableHeader();
        header.setBackground(new Color(16, 10, 40));
        header.setForeground(C_TEXT_DIM);
        header.setFont(new Font("Verdana", Font.BOLD, 9));
        header.setPreferredSize(new Dimension(0, 36));
        header.setBorder(new MatteBorder(0, 0, 1, 0, C_BORDER));
        ((DefaultTableCellRenderer) header.getDefaultRenderer()).setHorizontalAlignment(SwingConstants.LEFT);

        // Alternating row renderer
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object val,
                    boolean isSelected, boolean hasFocus, int row, int col) {
                super.getTableCellRendererComponent(tbl, val, isSelected, hasFocus, row, col);
                if (!isSelected) {
                    setBackground(row % 2 == 0 ? C_CARD : new Color(26, 16, 58));
                    setForeground(C_TEXT_MID);
                } else {
                    setBackground(new Color(C_GOLD.getRed(), C_GOLD.getGreen(), C_GOLD.getBlue(), 35));
                    setForeground(C_TEXT);
                }
                setBorder(new EmptyBorder(0, 12, 0, 12));
                return this;
            }
        });
    }

    // ─── Scrollbar UI ─────────────────────────────────────────────────────────

    private static class ModernScrollBarUI extends javax.swing.plaf.basic.BasicScrollBarUI {
        @Override
        protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(isDragging ? new Color(130, 80, 220) : new Color(80, 50, 150));
            g2.fillRoundRect(r.x + 2, r.y + 2, r.width - 4, r.height - 4, 6, 6);
            g2.dispose();
        }

        @Override
        protected void paintTrack(Graphics g, JComponent c, Rectangle r) {
            g.setColor(new Color(13, 8, 32));
            g.fillRect(r.x, r.y, r.width, r.height);
        }

        @Override protected JButton createDecreaseButton(int o) { return createZeroBtn(); }
        @Override protected JButton createIncreaseButton(int o) { return createZeroBtn(); }

        private JButton createZeroBtn() {
            JButton btn = new JButton();
            btn.setPreferredSize(new Dimension(0, 0));
            btn.setMinimumSize(new Dimension(0, 0));
            btn.setMaximumSize(new Dimension(0, 0));
            return btn;
        }
    }

    // ─── Logout Functionality ─────────────────────────────────────────────────────

    private void performLogout() {
        // Show confirmation dialog
        int result = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to logout?\n\nThis will clear your session and return to the login screen.",
            "Confirm Logout",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE
        );

        if (result == JOptionPane.YES_OPTION) {
            try {
                // Clear session
                SessionManager sessionManager = SessionManager.getInstance();
                sessionManager.clearSession();
                
                // Show success message briefly
                JOptionPane.showMessageDialog(this, 
                    "Logged out successfully!", 
                    "Logout", 
                    JOptionPane.INFORMATION_MESSAGE);

                // Close current window and open login
                dispose();
                new LoginFrame().setVisible(true);
                
            } catch (Exception e) {
                // Handle any errors during logout
                JOptionPane.showMessageDialog(this,
                    "An error occurred during logout: " + e.getMessage(),
                    "Logout Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // ─── Entry Point ─────────────────────────────────────────────────────────

    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            new PastorDashboardFrame().setVisible(true);
        });
    }
}


