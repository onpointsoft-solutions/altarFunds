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
 * Pastor Dashboard - Modern Ministry Interface
 * Emerald green theme matching UsherDashboardFrame
 */
public class PastorDashboardFrame extends JFrame {
    
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

    // ─── Typography ───────────────────────────────────────────────────
    private static final Font F_TITLE   = new Font("Segoe UI",      Font.BOLD, 28);
    private static final Font F_LABEL   = new Font("Segoe UI",      Font.BOLD, 14);
    private static final Font F_MONO_SM = new Font("JetBrains Mono",Font.PLAIN, 11);
    private static final Font F_MONO_LG = new Font("JetBrains Mono",Font.BOLD, 20);

    // ─── Navigation ───────────────────────────────────────────────────
    private static final String[][] MENU_ITEMS = {
        {"🏠", "Overview"},
        {"📖", "Devotionals"},
        {"👥", "Members"},
        {"🙏", "Prayer"},
        {"💬", "Counseling"},
        {"📅", "Events"},
        {"⚙️", "Settings"}
    };

    // ─── UI State ─────────────────────────────────────────────────────
    private final CardLayout cardLayout = new CardLayout();
    private JPanel contentArea;
    private JPanel sidebar;
    private String activeMenu = "Overview";

    // KPI value labels (updated from API)
    private JLabel lblTotalMembers;
    private JLabel lblDevotionals;
    private JLabel lblPrayerRequests;
    private JLabel lblCounselingSessions;

    // ─── Constructor ──────────────────────────────────────────────────
    public PastorDashboardFrame() {
        try {
            configureWindow();
            setApplicationIcon();
            buildUI();
            loadData();
        } catch (Exception e) {
            System.err.println("Failed to create PastorDashboardFrame: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to create PastorDashboardFrame: " + e.getMessage(), e);
        }
    }

    // ─── Window Setup ─────────────────────────────────────────────────
    private void configureWindow() {
        setTitle("Sanctum — Pastor Dashboard");
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
        contentArea.add(buildMainDashboard(), "overview");
        contentArea.add(buildDevotionalsPage(), "devotionals");
        contentArea.add(buildMembersPage(), "members");
        contentArea.add(buildPrayerPage(), "prayer");
        contentArea.add(buildCounselingPage(), "counseling");
        contentArea.add(buildEventsPage(), "events");

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
        JLabel title = new JLabel("Sanctum  ·  Pastor Dashboard");
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
        String firstName   = userData != null ? userData.getOrDefault("first_name", "Pastor").toString() : "Pastor";
        String lastName    = userData != null ? userData.getOrDefault("last_name", "").toString()     : "";
        String displayName = lastName.isEmpty() ? firstName : firstName + " " + lastName;
        String initials    = (firstName.isEmpty() ? "P" : String.valueOf(firstName.charAt(0)))
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
        JLabel role = new JLabel("Pastor");
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
        if (menu.equals("Settings")) {
            // Open settings dialog instead of switching content
            new ChurchSettingsFrame().setVisible(true);
        } else {
            cardLayout.show(contentArea, menu.toLowerCase());
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
        JLabel roleLabel = new JLabel("PASTOR PORTAL");
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

        kpiRow.add(buildKpiCard("Total Members",  "0", "Loading...", C_GOLD,      "👥", kpi, 0));
        kpiRow.add(buildKpiCard("Devotionals",    "0", "Loading...", C_GOLD_HOVER, "📖", kpi, 1));
        kpiRow.add(buildKpiCard("Prayer Requests","0", "Loading...", C_TEXT_MID,   "🙏", kpi, 2));
        kpiRow.add(buildKpiCard("Counseling",     "0", "Loading...", C_GOLD_DIM,   "💬", kpi, 3));

        lblTotalMembers     = kpi[0];
        lblDevotionals      = kpi[1];
        lblPrayerRequests   = kpi[2];
        lblCounselingSessions = kpi[3];

        JPanel north = new JPanel(new BorderLayout(0, 20));
        north.setOpaque(false);
        north.add(header, BorderLayout.NORTH);
        north.add(kpiRow, BorderLayout.CENTER);

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
    private JPanel buildDevotionalsPage() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(30, 30, 30, 30));

        JLabel title = new JLabel("Devotionals Management");
        title.setFont(F_TITLE);
        title.setForeground(C_TEXT);
        title.setBorder(new EmptyBorder(0, 0, 30, 0));

        // Real devotionals panel
        JPanel devotionalsPanel = new JPanel();
        devotionalsPanel.setLayout(new BoxLayout(devotionalsPanel, BoxLayout.Y_AXIS));
        devotionalsPanel.setOpaque(false);

        JScrollPane scroll = new JScrollPane(devotionalsPanel);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);

        panel.add(title, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        
        // Load real devotionals data
        loadDevotionalsData(devotionalsPanel);
        
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
        loadPastorMembersData(model);
        
        return panel;
    }

    private JPanel buildPrayerPage() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(30, 30, 30, 30));

        JLabel title = new JLabel("Prayer Requests");
        title.setFont(F_TITLE);
        title.setForeground(C_TEXT);
        title.setBorder(new EmptyBorder(0, 0, 30, 0));

        // Real prayer requests panel
        JPanel prayerPanel = new JPanel();
        prayerPanel.setLayout(new BoxLayout(prayerPanel, BoxLayout.Y_AXIS));
        prayerPanel.setOpaque(false);

        JScrollPane scroll = new JScrollPane(prayerPanel);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);

        panel.add(title, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        
        // Load real prayer requests data
        loadPrayerRequestsData(prayerPanel);
        
        return panel;
    }

    private JPanel buildCounselingPage() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(30, 30, 30, 30));

        JLabel title = new JLabel("Counseling Sessions");
        title.setFont(F_TITLE);
        title.setForeground(C_TEXT);
        title.setBorder(new EmptyBorder(0, 0, 30, 0));

        // Real counseling sessions panel
        JPanel counselingPanel = new JPanel();
        counselingPanel.setLayout(new BoxLayout(counselingPanel, BoxLayout.Y_AXIS));
        counselingPanel.setOpaque(false);

        JScrollPane scroll = new JScrollPane(counselingPanel);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);

        panel.add(title, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        
        // Load real counseling sessions data
        loadCounselingSessionsData(counselingPanel);
        
        return panel;
    }

    private JPanel buildEventsPage() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(30, 30, 30, 30));

        JLabel title = new JLabel("Upcoming Events");
        title.setFont(F_TITLE);
        title.setForeground(C_TEXT);
        title.setBorder(new EmptyBorder(0, 0, 30, 0));

        // Real events panel
        JPanel eventsPanel = new JPanel();
        eventsPanel.setLayout(new BoxLayout(eventsPanel, BoxLayout.Y_AXIS));
        eventsPanel.setOpaque(false);

        JScrollPane scroll = new JScrollPane(eventsPanel);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);

        panel.add(title, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        
        // Load real events data
        loadEventsData(eventsPanel);
        
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
        try (InputStream is = PastorDashboardFrame.class.getResourceAsStream(path)) {
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

        SanctumApiClient.getDashboardData().thenAccept(data -> SwingUtilities.invokeLater(() -> {
            if (data != null) {
                lblTotalMembers.setText(String.valueOf(data.getOrDefault("total_members", "0")));
                lblDevotionals.setText(String.valueOf(data.getOrDefault("devotionals_count", "0")));
                lblPrayerRequests.setText(String.valueOf(data.getOrDefault("prayer_requests", "0")));
                lblCounselingSessions.setText(String.valueOf(data.getOrDefault("counseling_sessions", "0")));
                System.out.println("Pastor dashboard data loaded successfully.");
            } else {
                setKpiLabels("—");
            }
        })).exceptionally(ex -> {
            SwingUtilities.invokeLater(() -> {
                System.err.println("Failed to load dashboard data: " + ex.getMessage());
                setKpiLabels("—");
            });
            return null;
        });
    }

    // ─── Data Loading Methods ─────────────────────────────────────────────
    private void loadDevotionalsData(JPanel devotionalsPanel) {
        SanctumApiClient.getDevotionals().thenAccept(devotionals -> {
            SwingUtilities.invokeLater(() -> {
                devotionalsPanel.removeAll();
                if (devotionals.isEmpty()) {
                    JLabel noDataLabel = new JLabel("No devotionals available");
                    noDataLabel.setFont(F_LABEL);
                    noDataLabel.setForeground(C_TEXT_DIM);
                    noDataLabel.setBorder(new EmptyBorder(20, 20, 20, 20));
                    devotionalsPanel.add(noDataLabel);
                } else {
                    for (Map<String, Object> devotional : devotionals) {
                        JPanel item = new JPanel(new BorderLayout());
                        item.setOpaque(false);
                        item.setBorder(new EmptyBorder(10, 20, 10, 20));
                        
                        String title = devotional.getOrDefault("title", "Untitled").toString();
                        String date = devotional.getOrDefault("created_at", "").toString();
                        if (date.length() > 10) date = date.substring(0, 10);
                        
                        JLabel label = new JLabel("📖 " + title + (date.isEmpty() ? "" : " - " + date));
                        label.setFont(F_LABEL);
                        label.setForeground(C_TEXT_MID);
                        
                        item.add(label, BorderLayout.CENTER);
                        devotionalsPanel.add(item);
                        devotionalsPanel.add(Box.createVerticalStrut(5));
                    }
                }
                devotionalsPanel.revalidate();
                devotionalsPanel.repaint();
            });
        }).exceptionally(ex -> {
            SwingUtilities.invokeLater(() -> {
                devotionalsPanel.removeAll();
                JLabel errorLabel = new JLabel("Failed to load devotionals");
                errorLabel.setFont(F_LABEL);
                errorLabel.setForeground(C_DANGER);
                errorLabel.setBorder(new EmptyBorder(20, 20, 20, 20));
                devotionalsPanel.add(errorLabel);
                devotionalsPanel.revalidate();
                devotionalsPanel.repaint();
            });
            return null;
        });
    }

    private void loadPastorMembersData(DefaultTableModel model) {
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

    private void loadPrayerRequestsData(JPanel prayerPanel) {
        // Use announcements as a temporary fallback for prayer requests
        SanctumApiClient.getAnnouncements().thenAccept(announcements -> {
            SwingUtilities.invokeLater(() -> {
                prayerPanel.removeAll();
                if (announcements.isEmpty()) {
                    JLabel noDataLabel = new JLabel("No prayer requests available");
                    noDataLabel.setFont(F_LABEL);
                    noDataLabel.setForeground(C_TEXT_DIM);
                    noDataLabel.setBorder(new EmptyBorder(20, 20, 20, 20));
                    prayerPanel.add(noDataLabel);
                } else {
                    for (Map<String, Object> prayer : announcements) {
                        JPanel item = new JPanel(new BorderLayout());
                        item.setOpaque(false);
                        item.setBorder(new EmptyBorder(10, 20, 10, 20));
                        
                        String request = prayer.getOrDefault("title", "Untitled").toString();
                        String date = prayer.getOrDefault("created_at", "").toString();
                        if (date.length() > 10) date = date.substring(0, 10);
                        
                        JLabel label = new JLabel("🙏 " + request + (date.isEmpty() ? "" : " (" + date + ")"));
                        label.setFont(F_LABEL);
                        label.setForeground(C_TEXT_MID);
                        
                        item.add(label, BorderLayout.CENTER);
                        prayerPanel.add(item);
                        prayerPanel.add(Box.createVerticalStrut(5));
                    }
                }
                prayerPanel.revalidate();
                prayerPanel.repaint();
            });
        }).exceptionally(ex -> {
            SwingUtilities.invokeLater(() -> {
                prayerPanel.removeAll();
                JLabel errorLabel = new JLabel("Failed to load prayer requests");
                errorLabel.setFont(F_LABEL);
                errorLabel.setForeground(C_DANGER);
                errorLabel.setBorder(new EmptyBorder(20, 20, 20, 20));
                prayerPanel.add(errorLabel);
                prayerPanel.revalidate();
                prayerPanel.repaint();
            });
            return null;
        });
    }

    private void loadCounselingSessionsData(JPanel counselingPanel) {
        // Use staff data as a temporary fallback for counseling sessions
        SanctumApiClient.getStaff().thenAccept(staff -> {
            SwingUtilities.invokeLater(() -> {
                counselingPanel.removeAll();
                if (staff.isEmpty()) {
                    JLabel noDataLabel = new JLabel("No counseling sessions scheduled");
                    noDataLabel.setFont(F_LABEL);
                    noDataLabel.setForeground(C_TEXT_DIM);
                    noDataLabel.setBorder(new EmptyBorder(20, 20, 20, 20));
                    counselingPanel.add(noDataLabel);
                } else {
                    for (Map<String, Object> session : staff) {
                        JPanel item = new JPanel(new BorderLayout());
                        item.setOpaque(false);
                        item.setBorder(new EmptyBorder(10, 20, 10, 20));
                        
                        String type = session.getOrDefault("role", "Unknown").toString();
                        String client = session.getOrDefault("name", "Unknown").toString();
                        
                        JLabel label = new JLabel("💬 " + type + " - " + client);
                        label.setFont(F_LABEL);
                        label.setForeground(C_TEXT_MID);
                        
                        item.add(label, BorderLayout.CENTER);
                        counselingPanel.add(item);
                        counselingPanel.add(Box.createVerticalStrut(5));
                    }
                }
                counselingPanel.revalidate();
                counselingPanel.repaint();
            });
        }).exceptionally(ex -> {
            SwingUtilities.invokeLater(() -> {
                counselingPanel.removeAll();
                JLabel errorLabel = new JLabel("Failed to load counseling sessions");
                errorLabel.setFont(F_LABEL);
                errorLabel.setForeground(C_DANGER);
                errorLabel.setBorder(new EmptyBorder(20, 20, 20, 20));
                counselingPanel.add(errorLabel);
                counselingPanel.revalidate();
                counselingPanel.repaint();
            });
            return null;
        });
    }

    private void loadEventsData(JPanel eventsPanel) {
        // Use announcements as a temporary fallback for events
        SanctumApiClient.getAnnouncements().thenAccept(events -> {
            SwingUtilities.invokeLater(() -> {
                eventsPanel.removeAll();
                if (events.isEmpty()) {
                    JLabel noDataLabel = new JLabel("No upcoming events");
                    noDataLabel.setFont(F_LABEL);
                    noDataLabel.setForeground(C_TEXT_DIM);
                    noDataLabel.setBorder(new EmptyBorder(20, 20, 20, 20));
                    eventsPanel.add(noDataLabel);
                } else {
                    for (Map<String, Object> event : events) {
                        JPanel item = new JPanel(new BorderLayout());
                        item.setOpaque(false);
                        item.setBorder(new EmptyBorder(10, 20, 10, 20));
                        
                        String title = event.getOrDefault("title", "Untitled").toString();
                        String date = event.getOrDefault("created_at", "").toString();
                        if (date.length() > 10) date = date.substring(0, 10);
                        
                        JLabel label = new JLabel("📅 " + title + (date.isEmpty() ? "" : " - " + date));
                        label.setFont(F_LABEL);
                        label.setForeground(C_TEXT_MID);
                        
                        item.add(label, BorderLayout.CENTER);
                        eventsPanel.add(item);
                        eventsPanel.add(Box.createVerticalStrut(5));
                    }
                }
                eventsPanel.revalidate();
                eventsPanel.repaint();
            });
        }).exceptionally(ex -> {
            SwingUtilities.invokeLater(() -> {
                eventsPanel.removeAll();
                JLabel errorLabel = new JLabel("Failed to load events");
                errorLabel.setFont(F_LABEL);
                errorLabel.setForeground(C_DANGER);
                errorLabel.setBorder(new EmptyBorder(20, 20, 20, 20));
                eventsPanel.add(errorLabel);
                eventsPanel.revalidate();
                eventsPanel.repaint();
            });
            return null;
        });
    }

    /** Convenience: set all four KPI labels to the same text (e.g. "Loading..." or "—"). */
    private void setKpiLabels(String text) {
        if (lblTotalMembers     != null) lblTotalMembers.setText(text);
        if (lblDevotionals      != null) lblDevotionals.setText(text);
        if (lblPrayerRequests   != null) lblPrayerRequests.setText(text);
        if (lblCounselingSessions != null) lblCounselingSessions.setText(text);
    }

    // ─── Entry Point ──────────────────────────────────────────────────
    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        SwingUtilities.invokeLater(() -> new PastorDashboardFrame().setVisible(true));
    }
}
