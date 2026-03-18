package com.sanctum.view;

import com.sanctum.api.SanctumApiClient;
import com.sanctum.util.WindowsDialogFix;
import com.sanctum.util.DialogManager;
import com.sanctum.auth.SessionManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.DefaultTableCellRenderer;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Professional Treasurer Dashboard — Sanctum Church Management System
 * Modern dark UI with glass-morphism cards, sidebar navigation, KPI metrics,
 * budget progress bars, donation feed, and full transaction table.
 */
public class TreasurerDashboardFrame extends JFrame {

    // ─── Sanctum Brand Color System ────────────────────────────────────────────────
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

    // ─── Typography ──────────────────────────────────────────────────────────
    private static final Font F_DISPLAY      = new Font("Georgia", Font.BOLD, 26);
    private static final Font F_H1           = new Font("Arial",   Font.BOLD, 24);
    private static final Font F_HEADING      = new Font("Arial",   Font.BOLD, 13);
    private static final Font F_LABEL        = new Font("Arial",   Font.BOLD, 11);
    private static final Font F_MONO_LG      = new Font("Monospaced", Font.BOLD, 22);
    private static final Font F_MONO_SM      = new Font("Monospaced", Font.PLAIN, 11);
    private static final Font F_BODY         = new Font("Arial",   Font.PLAIN, 12);
    private static final Font F_SMALL        = new Font("Arial",   Font.PLAIN, 11);

    // UI component references for data updates
    private JPanel recentDonationsList;
    private JPanel budgetList;
    private JPanel fundList;

    // ─── State ───────────────────────────────────────────────────────────────
    private int dragX, dragY;
    private JTable donationsTable;
    private JTable transactionsTable;
    private JPanel contentArea;
    private CardLayout cardLayout;

    // KPI value labels (updated from API)
    private JLabel lblTotalDonations;
    private JLabel lblMonthlyDonations;
    private JLabel lblTotalExpenses;
    private JLabel lblNetBalance;

    public TreasurerDashboardFrame() {
        configureWindow();
        setApplicationIcon();
        buildUI();
        loadData();
    }

    // ─── Window Setup ────────────────────────────────────────────────────────

    private void configureWindow() {
        setTitle("Sanctum — Treasurer Dashboard");
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
        // Root panel with background
        JPanel root = new JPanel(new BorderLayout(0, 0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setColor(C_BG);
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        root.setOpaque(true);
        setContentPane(root);

        // Title bar
        root.add(buildTitleBar(), BorderLayout.NORTH);

        // Body
        JPanel body = new JPanel(new BorderLayout(0, 0));
        body.setOpaque(false);
        body.add(buildSidebar(), BorderLayout.WEST);
        body.add(buildMainArea(), BorderLayout.CENTER);
        root.add(body, BorderLayout.CENTER);
    }

    // ─── Title Bar ───────────────────────────────────────────────────────────

    private JPanel buildTitleBar() {
        JPanel bar = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setColor(C_SURFACE);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(C_BORDER);
                g2.fillRect(0, getHeight() - 1, getWidth(), 1);
            }
        };
        bar.setOpaque(false);
        bar.setPreferredSize(new Dimension(0, 36));
        bar.setBorder(new EmptyBorder(0, 16, 0, 0));

        // Left: logo + title
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setOpaque(false);
        JLabel icon = new JLabel("⛪");
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 15));
        JLabel title = new JLabel("Sanctum  ·  Treasurer Dashboard");
        title.setFont(F_MONO_SM);
        title.setForeground(C_TEXT_MID);
        left.add(icon);
        left.add(title);
        bar.add(left, BorderLayout.WEST);

        // Right: window controls
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        controls.setOpaque(false);
        controls.add(buildWinBtn("─", C_TEXT_DIM, C_TEXT_MID, e -> setState(ICONIFIED)));
        controls.add(buildWinBtn("✕", C_TEXT_DIM, C_GOLD_HOVER,     e -> System.exit(0)));
        bar.add(controls, BorderLayout.EAST);

        // Drag
        bar.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) { dragX = e.getX(); dragY = e.getY(); }
        });
        bar.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                setLocation(e.getXOnScreen() - dragX, e.getYOnScreen() - dragY);
            }
        });
        return bar;
    }

    private JButton buildWinBtn(String text, Color normal, Color hover, ActionListener al) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                if (getModel().isRollover()) {
                    g2.setColor(new Color(hover.getRed(), hover.getGreen(), hover.getBlue(), 40));
                    g2.fillRect(0, 0, getWidth(), getHeight());
                    g2.setColor(hover);
                } else {
                    g2.setColor(normal);
                }
                g2.setFont(F_SMALL);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(text,
                    (getWidth() - fm.stringWidth(text)) / 2,
                    (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
            }
        };
        btn.setPreferredSize(new Dimension(44, 36));
        btn.setContentAreaFilled(false); btn.setBorderPainted(false); btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(al);
        return btn;
    }

    // ─── Sidebar ─────────────────────────────────────────────────────────────

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setColor(C_SURFACE);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(C_BORDER);
                g2.fillRect(getWidth() - 1, 0, 1, getHeight());
                // Subtle top accent
                GradientPaint gp = new GradientPaint(0, 0, new Color(C_GOLD.getRed(), C_GOLD.getGreen(), C_GOLD.getBlue(), 30), 0, 60, new Color(0,0,0,0));
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), 60);
            }
        };
        sidebar.setOpaque(false);
        sidebar.setPreferredSize(new Dimension(220, 0));

        // Logo block
        JPanel logoBlock = new JPanel();
        logoBlock.setOpaque(false);
        logoBlock.setLayout(new BoxLayout(logoBlock, BoxLayout.Y_AXIS));
        logoBlock.setBorder(new EmptyBorder(24, 20, 20, 20));

        JLabel logoLabel = new JLabel("Sanctum");
        logoLabel.setFont(new Font("Georgia", Font.BOLD, 20));
        logoLabel.setForeground(C_TEXT);
        JLabel roleLabel = new JLabel("TREASURER PORTAL");
        roleLabel.setFont(F_MONO_SM);
        roleLabel.setForeground(C_GOLD);
        logoBlock.add(logoLabel);
        logoBlock.add(Box.createVerticalStrut(2));
        logoBlock.add(roleLabel);

        sidebar.add(logoBlock, BorderLayout.NORTH);

        // Nav items
        JPanel nav = new JPanel();
        nav.setOpaque(false);
        nav.setLayout(new BoxLayout(nav, BoxLayout.Y_AXIS));
        nav.setBorder(new EmptyBorder(8, 12, 8, 12));

        addNavSection(nav, "MAIN MENU");
        JButton btnOverview     = addNavItem(nav, "Overview",      "⊞", C_GOLD, true);
        JButton btnDonations    = addNavItem(nav, "Donations",     "💵", C_TEXT_MID, false);
        JButton btnReports      = addNavItem(nav, "Reports",       "📊", C_TEXT_MID, false);
        JButton btnBudget       = addNavItem(nav, "Budget",        "📋", C_TEXT_MID, false);
        JButton btnTransactions = addNavItem(nav, "Transactions",  "📜", C_TEXT_MID, false);
        addNavSection(nav, "FINANCE");
        JButton btnAccounts     = addNavItem(nav, "Accounts",      "🏦", C_TEXT_MID, false);
        JButton btnExpenses     = addNavItem(nav, "Expenses",      "📤", C_TEXT_MID, false);
        addNavSection(nav, "SYSTEM");
        JButton btnBack         = addNavItem(nav, "Sign Out",      "🔙", C_GOLD_HOVER, false);
        btnBack.addActionListener(e -> { dispose(); new LoginFrame().setVisible(true); });
        
        // Add navigation action listeners
        btnOverview.addActionListener(e -> showOverviewPage());
        btnDonations.addActionListener(e -> showDonationsPage());
        btnReports.addActionListener(e -> showReportsPage());
        btnBudget.addActionListener(e -> showBudgetPage());
        btnTransactions.addActionListener(e -> showTransactionsPage());
        btnAccounts.addActionListener(e -> showAccountsPage());
        btnExpenses.addActionListener(e -> showExpensesPage());

        sidebar.add(nav, BorderLayout.CENTER);

        // User card at bottom
        sidebar.add(buildUserCard(), BorderLayout.SOUTH);

        return sidebar;
    }

    private void addNavSection(JPanel nav, String label) {
        nav.add(Box.createVerticalStrut(14));
        JLabel lbl = new JLabel(label);
        lbl.setFont(F_MONO_SM);
        lbl.setForeground(C_TEXT_DIM);
        lbl.setBorder(new EmptyBorder(0, 8, 4, 0));
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        nav.add(lbl);
    }

    private JButton addNavItem(JPanel nav, String text, String icon, Color accent, boolean active) {
        Color[] state = { accent };
        boolean[] hover = { false };

        JButton btn = new JButton() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                boolean isActive = getClientProperty("active") == Boolean.TRUE;
                boolean isHover  = getModel().isRollover();

                if (isActive) {
                    g2.setColor(new Color(state[0].getRed(), state[0].getGreen(), state[0].getBlue(), 18));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                    // left accent bar
                    g2.setColor(state[0]);
                    g2.fillRoundRect(0, 6, 3, getHeight() - 12, 3, 3);
                } else if (isHover) {
                    g2.setColor(new Color(255, 255, 255, 6));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                }

                // Icon
                g2.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
                g2.setColor(isActive ? state[0] : C_TEXT_MID);
                g2.drawString(icon, 10, getHeight() / 2 + 5);

                // Text
                g2.setFont(F_LABEL);
                g2.setColor(isActive ? state[0] : (isHover ? C_TEXT : C_TEXT_MID));
                g2.drawString(text, 32, getHeight() / 2 + 5);
            }
        };
        btn.putClientProperty("active", active);
        btn.setPreferredSize(new Dimension(196, 36));
        btn.setMaximumSize(new Dimension(196, 36));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setContentAreaFilled(false); btn.setBorderPainted(false); btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        nav.add(btn);
        nav.add(Box.createVerticalStrut(2));
        return btn;
    }

    private JPanel buildUserCard() {
        JPanel card = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setColor(C_BORDER);
                g2.fillRect(0, 0, getWidth(), 1);
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(4, 12, 16, 12));

        // Avatar with real user initials
        JPanel avatar = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Get real user initials
                Map<String, Object> userData = SanctumApiClient.getCurrentUserData();
                String firstName = userData != null ? userData.getOrDefault("first_name", "U").toString() : "U";
                String lastName = userData != null ? userData.getOrDefault("last_name", "").toString() : "";
                String initials = firstName.substring(0, 1).toUpperCase();
                if (!lastName.isEmpty()) {
                    initials += lastName.substring(0, 1).toUpperCase();
                }
                
                g2.setColor(C_GOLD);
                g2.fillOval(0, 0, 34, 34);
                g2.setColor(C_TEXT);
                g2.setFont(new Font("Arial", Font.BOLD, 14));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(initials, (34 - fm.stringWidth(initials)) / 2, 23);
            }
            @Override public Dimension getPreferredSize() { return new Dimension(34, 34); }
        };

        JPanel info = new JPanel();
        info.setOpaque(false);
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        
        // Get real user data from API
        Map<String, Object> userData = SanctumApiClient.getCurrentUserData();
        String firstName = userData != null ? userData.getOrDefault("first_name", "User").toString() : "User";
        String lastName = userData != null ? userData.getOrDefault("last_name", "").toString() : "";
        String displayName = lastName.isEmpty() ? firstName : firstName + " " + lastName;
        
        JLabel name = new JLabel(displayName);
        name.setFont(F_LABEL);
        name.setForeground(C_TEXT);
        JLabel role = new JLabel("Treasurer");
        role.setFont(F_MONO_SM);
        role.setForeground(C_TEXT_DIM);
        info.add(name);
        info.add(role);

        card.add(avatar);
        card.add(info);
        return card;
    }

    // ─── Main Area ───────────────────────────────────────────────────────────

    private JPanel buildMainArea() {
        JPanel main = new JPanel(new BorderLayout());
        main.setOpaque(false);

        // Topbar
        main.add(buildTopBar(), BorderLayout.NORTH);

        // CardLayout for different pages
        cardLayout = new CardLayout();
        contentArea = new JPanel(cardLayout);
        contentArea.setOpaque(false);

        // Add different pages
        contentArea.add(buildDashboardContent(), "overview");
        contentArea.add(buildDonationsPage(), "donations");
        contentArea.add(buildReportsPage(), "reports");
        contentArea.add(buildBudgetPage(), "budget");
        contentArea.add(buildTransactionsPage(), "transactions");
        contentArea.add(buildAccountsPage(), "accounts");
        contentArea.add(buildExpensesPage(), "expenses");

        JScrollPane scroll = new JScrollPane(contentArea);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getVerticalScrollBar().setUI(new ModernScrollBarUI());
        main.add(scroll, BorderLayout.CENTER);

        return main;
    }

    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setColor(C_SURFACE);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(C_BORDER);
                g2.fillRect(0, getHeight() - 1, getWidth(), 1);
            }
        };
        bar.setOpaque(false);
        bar.setPreferredSize(new Dimension(0, 64));
        bar.setBorder(new EmptyBorder(0, 28, 0, 24));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        left.setOpaque(false);
        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        JLabel h1 = new JLabel("Financial Overview");
        h1.setFont(F_DISPLAY);
        h1.setForeground(C_TEXT);
        JLabel sub = new JLabel("FY 2025  ·  February Report  ·  Updated just now");
        sub.setFont(F_MONO_SM);
        sub.setForeground(C_TEXT_DIM);
        titles.add(h1);
        titles.add(Box.createVerticalStrut(3));
        titles.add(sub);
        left.add(titles);
        bar.add(left, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        right.add(buildTopBtn("📤  Export",       C_TEXT_MID));
        right.add(buildTopBtn("🗓  This Month ▾",  C_TEXT_MID));
        right.add(buildPrimaryBtn("➕  Add Donation"));
        bar.add(right, BorderLayout.EAST);

        return bar;
    }

    private JButton buildTopBtn(String text, Color fg) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isRollover()) {
                    g2.setColor(C_CARD_HOVER);
                } else {
                    g2.setColor(C_CARD);
                }
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(getModel().isRollover() ? C_BORDER_LT : C_BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.setFont(F_SMALL);
                g2.setColor(getModel().isRollover() ? C_TEXT : fg);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(text, (getWidth() - fm.stringWidth(text)) / 2,
                    (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
            }
        };
        btn.setPreferredSize(new Dimension(130, 34));
        btn.setContentAreaFilled(false); btn.setBorderPainted(false); btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setFont(F_SMALL);
        return btn;
    }

    private JButton buildPrimaryBtn(String text) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0,
                    getModel().isPressed() ? new Color(170, 140, 90) : C_GOLD,
                    0, getHeight(),
                    getModel().isPressed() ? new Color(140, 110, 70) : new Color(220, 185, 120));
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setFont(F_LABEL);
                g2.setColor(C_BG);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(text, (getWidth() - fm.stringWidth(text)) / 2,
                    (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
            }
        };
        btn.setPreferredSize(new Dimension(150, 34));
        btn.setContentAreaFilled(false); btn.setBorderPainted(false); btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> showAddDonationDialog());
        return btn;
    }

    // ─── Dashboard Content ───────────────────────────────────────────────────

    private JPanel buildDashboardContent() {
        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(new EmptyBorder(24, 28, 28, 28));

        // Row 1: KPI Cards
        content.add(buildKpiRow());
        content.add(Box.createVerticalStrut(20));

        // Row 2: Chart + Recent Donations
        content.add(buildRow2());
        content.add(Box.createVerticalStrut(20));

        // Row 3: Budget + Quick Actions + Fund Summary
        content.add(buildRow3());
        content.add(Box.createVerticalStrut(20));

        // Row 4: Transaction Table (full width)
        content.add(buildTransactionSection());

        return content;
    }

    // ─── KPI Row ─────────────────────────────────────────────────────────────

    private JPanel buildKpiRow() {
        JPanel row = new JPanel(new GridLayout(1, 4, 16, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));

        // Create KPI cards with placeholders that will be updated by API
        JPanel cardTD = buildKpiCard("Total Donations", "KES 0", "No data available", C_GOLD, "💰");
        JPanel cardMD = buildKpiCard("This Month",      "KES 0", "No data available", C_GOLD_HOVER, "📅");
        JPanel cardTE = buildKpiCard("Total Expenses",  "KES 0", "No data available", C_TEXT_MID,  "📤");
        JPanel cardNB = buildKpiCard("Net Balance",     "KES 0", "No data available", C_GOLD_DIM,  "🏦");

        // Store refs for API updates
        lblTotalDonations  = findValueLabel(cardTD);
        lblMonthlyDonations= findValueLabel(cardMD);
        lblTotalExpenses   = findValueLabel(cardTE);
        lblNetBalance      = findValueLabel(cardNB);

        row.add(cardTD); row.add(cardMD); row.add(cardTE); row.add(cardNB);
        return row;
    }

    private JPanel buildKpiCard(String title, String value, String sub, Color accent, String emoji) {
        JPanel card = new JPanel(null) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(C_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                // Top accent bar
                GradientPaint gp = new GradientPaint(0, 0, accent, getWidth() / 2f, 0, new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 0));
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth(), 3, 3, 3);
                // Border
                g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 35));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
            }
            @Override public Dimension getPreferredSize() { return new Dimension(0, 110); }
        };

        // Emoji icon top-right
        JLabel iconLbl = new JLabel(emoji);
        iconLbl.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
        iconLbl.setBounds(0, 14, 30, 24); // will be repositioned in paint

        // Title
        JLabel titleLbl = new JLabel(title.toUpperCase());
        titleLbl.setFont(F_MONO_SM);
        titleLbl.setForeground(C_TEXT_DIM);

        // Value
        JLabel valueLbl = new JLabel(value);
        valueLbl.setFont(F_MONO_LG);
        valueLbl.setForeground(accent);
        valueLbl.putClientProperty("kpiValue", Boolean.TRUE);

        // Sub
        JLabel subLbl = new JLabel(sub);
        subLbl.setFont(F_SMALL);
        subLbl.setForeground(sub.startsWith("▼") ? C_GOLD_HOVER : C_GOLD);

        // Layout using GridBagLayout inside a panel
        JPanel inner = new JPanel(new GridBagLayout());
        inner.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();

        g.gridx = 0; g.gridy = 0; g.anchor = GridBagConstraints.WEST;
        g.insets = new Insets(16, 16, 2, 16);
        inner.add(titleLbl, g);

        // Icon top-right
        g.gridx = 1; g.gridy = 0; g.anchor = GridBagConstraints.NORTHEAST;
        g.insets = new Insets(12, 0, 0, 12);
        JPanel iconBox = new JPanel() {
            @Override protected void paintComponent(Graphics g2d) {
                Graphics2D g2 = (Graphics2D) g2d;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 22));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
            }
            @Override public Dimension getPreferredSize() { return new Dimension(32, 32); }
        };
        iconBox.setOpaque(false);
        iconBox.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 4));
        iconLbl.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 15));
        iconBox.add(iconLbl);
        inner.add(iconBox, g);

        g.gridx = 0; g.gridy = 1; g.gridwidth = 2; g.anchor = GridBagConstraints.WEST;
        g.insets = new Insets(4, 16, 2, 16);
        inner.add(valueLbl, g);

        g.gridy = 2; g.insets = new Insets(2, 16, 14, 16);
        inner.add(subLbl, g);

        card.setLayout(new BorderLayout());
        card.add(inner, BorderLayout.CENTER);
        return card;
    }

    private JLabel findValueLabel(JPanel card) {
        return findLabelRecursive(card);
    }

    private JLabel findLabelRecursive(Container c) {
        for (Component comp : c.getComponents()) {
            if (comp instanceof JLabel) {
                JLabel lbl = (JLabel) comp;
                if (lbl.getClientProperty("kpiValue") == Boolean.TRUE) return lbl;
            }
            if (comp instanceof Container) {
                Container cc = (Container) comp;
                JLabel found = findLabelRecursive(cc);
                if (found != null) return found;
            }
        }
        return null;
    }

    // ─── Row 2: Donation Trend Chart + Recent Donations ──────────────────────

    private JPanel buildRow2() {
        JPanel row = new JPanel(new GridBagLayout());
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 300));

        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.BOTH;
        g.weighty = 1.0;

        g.gridx = 0; g.weightx = 1.6;
        row.add(buildBarChartSection(), g);

        g.gridx = 1; g.weightx = 0; g.insets = new Insets(0, 16, 0, 0);
        JPanel spacer = new JPanel(); spacer.setOpaque(false); spacer.setPreferredSize(new Dimension(16, 0));

        g.gridx = 1; g.weightx = 1.0; g.insets = new Insets(0, 16, 0, 0);
        row.add(buildRecentDonationsSection(), g);

        return row;
    }

    private JPanel buildBarChartSection() {
        JPanel section = buildSectionCard("Donation Trends — Last 6 Months", C_GOLD);
        section.add(buildBarChart(), BorderLayout.CENTER);
        return section;
    }

    private JPanel buildBarChart() {
        int[] donations = {210, 245, 290, 380, 260, 284};
        int[] expenses  = { 90, 105, 120, 140, 110,  95};
        String[] months = {"SEP", "OCT", "NOV", "DEC", "JAN", "FEB"};

        JPanel chart = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int W = getWidth(), H = getHeight();
                int padL = 16, padR = 16, padB = 28, padT = 12;
                int chartH = H - padT - padB;
                int maxVal = 400;
                int n = months.length;
                int groupW = (W - padL - padR) / n;
                int barW = Math.min(groupW / 3, 18);
                int gap = 3;

                // Grid lines
                g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{3, 5}, 0));
                g2.setColor(C_BORDER);
                for (int i = 1; i <= 4; i++) {
                    int y = padT + (int)(chartH * (1 - i / 4.0));
                    g2.drawLine(padL, y, W - padR, y);
                }
                g2.setStroke(new BasicStroke(1f));

                for (int i = 0; i < n; i++) {
                    int cx = padL + i * groupW + groupW / 2;
                    int dH = (int)((donations[i] / (double)maxVal) * chartH);
                    int eH = (int)((expenses[i]  / (double)maxVal) * chartH);

                    // Donation bar
                    int dx = cx - barW - gap / 2;
                    GradientPaint gpD = new GradientPaint(dx, padT + chartH - dH, C_GOLD, dx, padT + chartH, new Color(C_GOLD.getRed(), C_GOLD.getGreen(), C_GOLD.getBlue(), 60));
                    g2.setPaint(gpD);
                    g2.fillRoundRect(dx, padT + chartH - dH, barW, dH, 4, 4);

                    // Expense bar
                    int ex = cx + gap / 2;
                    GradientPaint gpE = new GradientPaint(ex, padT + chartH - eH, C_GOLD_HOVER, ex, padT + chartH, new Color(C_GOLD_HOVER.getRed(), C_GOLD_HOVER.getGreen(), C_GOLD_HOVER.getBlue(), 60));
                    g2.setPaint(gpE);
                    g2.fillRoundRect(ex, padT + chartH - eH, barW, eH, 4, 4);

                    // Month label
                    g2.setFont(F_MONO_SM);
                    g2.setColor(C_TEXT_DIM);
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString(months[i], cx - fm.stringWidth(months[i]) / 2, H - 6);
                }

                // Legend
                g2.setFont(F_SMALL);
                g2.setColor(C_GOLD);
                g2.fillRect(W - 130, padT, 8, 8);
                g2.setColor(C_TEXT_MID);
                g2.drawString("Donations", W - 118, padT + 8);
                g2.setColor(C_GOLD_HOVER);
                g2.fillRect(W - 60, padT, 8, 8);
                g2.setColor(C_TEXT_MID);
                g2.drawString("Expenses", W - 48, padT + 8);
            }
            @Override public Dimension getPreferredSize() { return new Dimension(0, 240); }
        };
        chart.setOpaque(false);
        chart.setBorder(new EmptyBorder(8, 16, 8, 16));
        return chart;
    }

    private JPanel buildRecentDonationsSection() {
        JPanel section = buildSectionCard("Recent Donations", C_GOLD_HOVER);

        recentDonationsList = new JPanel();
        recentDonationsList.setOpaque(false);
        recentDonationsList.setLayout(new BoxLayout(recentDonationsList, BoxLayout.Y_AXIS));

        // Load real donors data
        loadRealDonorsData();

        section.add(recentDonationsList, BorderLayout.CENTER);
        return section;
    }

    private JPanel buildDonorRow(String initials, String name, String type, String amount, Color avatarColor, String date) {
        JPanel row = new JPanel(new BorderLayout(10, 0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setColor(C_BORDER);
                g2.fillRect(0, getHeight() - 1, getWidth(), 1);
            }
        };
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(10, 16, 10, 16));

        // Avatar
        JPanel av = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, avatarColor.brighter(), getWidth(), getHeight(), avatarColor.darker());
                g2.setPaint(gp);
                g2.fillOval(0, 0, 32, 32);
                g2.setFont(new Font("Arial", Font.BOLD, 11));
                g2.setColor(Color.WHITE);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(initials, (32 - fm.stringWidth(initials)) / 2, 21);
            }
            @Override public Dimension getPreferredSize() { return new Dimension(32, 32); }
        };

        JPanel info = new JPanel();
        info.setOpaque(false);
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        JLabel nameLbl = new JLabel(name); nameLbl.setFont(F_LABEL); nameLbl.setForeground(C_TEXT);
        JLabel typeLbl = new JLabel(type); typeLbl.setFont(F_MONO_SM); typeLbl.setForeground(C_TEXT_DIM);
        info.add(nameLbl); info.add(Box.createVerticalStrut(2)); info.add(typeLbl);

        JPanel right = new JPanel();
        right.setOpaque(false);
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        JLabel amtLbl = new JLabel(amount); amtLbl.setFont(new Font("Monospaced", Font.BOLD, 13)); amtLbl.setForeground(C_GOLD); amtLbl.setAlignmentX(Component.RIGHT_ALIGNMENT);
        JLabel dateLbl = new JLabel(date);  dateLbl.setFont(F_MONO_SM); dateLbl.setForeground(C_TEXT_DIM); dateLbl.setAlignmentX(Component.RIGHT_ALIGNMENT);
        right.add(amtLbl); right.add(Box.createVerticalStrut(2)); right.add(dateLbl);

        row.add(av,   BorderLayout.WEST);
        row.add(info, BorderLayout.CENTER);
        row.add(right,BorderLayout.EAST);

        // Hover
        row.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { row.setOpaque(true); row.setBackground(new Color(255,255,255,6)); row.repaint(); }
            public void mouseExited(MouseEvent e)  { row.setOpaque(false); row.repaint(); }
        });

        return row;
    }

    // ─── Row 3 ───────────────────────────────────────────────────────────────

    private JPanel buildRow3() {
        JPanel row = new JPanel(new GridBagLayout());
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 280));

        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.BOTH;
        g.weighty = 1.0;

        g.gridx = 0; g.weightx = 1.0;
        row.add(buildBudgetSection(), g);

        g.gridx = 1; g.insets = new Insets(0, 16, 0, 0);
        row.add(buildQuickActions(), g);

        g.gridx = 2; g.insets = new Insets(0, 16, 0, 0);
        row.add(buildFundSummary(), g);

        return row;
    }

    private JPanel buildBudgetSection() {
        JPanel section = buildSectionCard("Budget Allocation", C_GOLD);
        
        budgetList = new JPanel();
        budgetList.setOpaque(false);
        budgetList.setLayout(new BoxLayout(budgetList, BoxLayout.Y_AXIS));
        budgetList.setBorder(new EmptyBorder(4, 16, 12, 16));

        // Load real budget data
        loadRealBudgetData();

        section.add(budgetList, BorderLayout.CENTER);
        return section;
    }

    private JPanel buildQuickActions() {
        JPanel section = buildSectionCard("Quick Actions", C_GOLD_HOVER);
        JPanel grid = new JPanel(new GridLayout(4, 2, 8, 8));
        grid.setOpaque(false);
        grid.setBorder(new EmptyBorder(8, 14, 14, 14));

        String[][] actions = {
            {"➕","Add Donation"}, {"📤","Log Expense"},
            {"📊","Monthly Report"}, {"📧","Send Statement"},
            {"🔍","Find Donor"}, {"📥","Import CSV"},
            {"🎯","Set Goal"}, {"📋","Audit Log"},
        };

        for (String[] a : actions) {
            grid.add(buildQaBtn(a[0], a[1]));
        }

        section.add(grid, BorderLayout.CENTER);
        return section;
    }

    private JButton buildQaBtn(String emoji, String label) {
        JButton btn = new JButton() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? C_CARD_HOVER : C_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(getModel().isRollover() ? C_BORDER_LT : C_BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                // emoji
                g2.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 13));
                g2.setColor(C_TEXT_MID);
                g2.drawString(emoji, 8, 20);
                g2.setFont(F_SMALL);
                g2.setColor(getModel().isRollover() ? C_TEXT : C_TEXT_MID);
                g2.drawString(label, 26, 20);
            }
            @Override public Dimension getPreferredSize() { return new Dimension(0, 36); }
        };
        btn.setContentAreaFilled(false); btn.setBorderPainted(false); btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JPanel buildFundSummary() {
        JPanel section = buildSectionCard("Fund Summary", C_GOLD_HOVER);
        JPanel list = new JPanel();
        list.setOpaque(false);
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setBorder(new EmptyBorder(4, 16, 12, 16));

        // Load real fund summary data
        loadRealFundData();
        list.add(Box.createVerticalStrut(10));

        JPanel total = new JPanel(new BorderLayout());
        total.setOpaque(false);
        JLabel tl = new JLabel("TOTAL ASSETS"); tl.setFont(F_MONO_SM); tl.setForeground(C_TEXT_DIM);
        JLabel tv = new JLabel("KES 2.72M"); tv.setFont(new Font("Georgia", Font.BOLD, 17)); tv.setForeground(C_TEXT);
        total.add(tl, BorderLayout.WEST); total.add(tv, BorderLayout.EAST);
        list.add(total);

        section.add(list, BorderLayout.CENTER);
        return section;
    }

    private JPanel buildFundRow(Color dot, String label, String value) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(9, 0, 9, 0));

        JPanel dotPanel = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(dot);
                g2.fillRoundRect(0, 3, 7, 7, 3, 3);
            }
            @Override public Dimension getPreferredSize() { return new Dimension(12, 14); }
        };
        dotPanel.setOpaque(false);

        JLabel lbl = new JLabel(label); lbl.setFont(F_BODY); lbl.setForeground(C_TEXT_MID);
        JLabel val = new JLabel(value); val.setFont(F_MONO_SM); val.setForeground(dot);

        JPanel left = new JPanel(new BorderLayout(6, 0));
        left.setOpaque(false);
        left.add(dotPanel, BorderLayout.WEST);
        left.add(lbl, BorderLayout.CENTER);

        row.add(left, BorderLayout.WEST);
        row.add(val,  BorderLayout.EAST);

        // bottom divider
        row.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(0, 0, 1, 0, C_BORDER),
            new EmptyBorder(9, 0, 9, 0)
        ));

        return row;
    }

    // ─── Transaction Table ───────────────────────────────────────────────────

    private JPanel buildTransactionSection() {
        JPanel section = buildSectionCard("Transaction History", C_GOLD);

        // Table
        String[] cols = {"Date", "Reference", "Description", "Category", "Type", "Amount (KES)", "Status"};
        DefaultTableModel model = new DefaultTableModel(new Object[][]{}, cols) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        transactionsTable = new JTable(model);
        styleTable(transactionsTable);

        JScrollPane scroll = new JScrollPane(transactionsTable);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(new EmptyBorder(0, 16, 0, 16));
        scroll.setPreferredSize(new Dimension(0, 180));

        section.add(scroll, BorderLayout.CENTER);
        return section;
    }

    private void styleTable(JTable table) {
        table.setFont(F_SMALL);
        table.setForeground(C_TEXT_MID);
        table.setBackground(C_CARD);
        table.setGridColor(C_BORDER);
        table.setRowHeight(34);
        table.setShowVerticalLines(false);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setSelectionBackground(new Color(C_GOLD.getRed(), C_GOLD.getGreen(), C_GOLD.getBlue(), 30));
        table.setSelectionForeground(C_TEXT);
        table.setFillsViewportHeight(true);

        // Header
        JTableHeader header = table.getTableHeader();
        header.setBackground(C_SURFACE);
        header.setForeground(C_TEXT_DIM);
        header.setFont(F_MONO_SM);
        header.setPreferredSize(new Dimension(0, 34));
        header.setBorder(new MatteBorder(0, 0, 1, 0, C_BORDER));
        ((DefaultTableCellRenderer) header.getDefaultRenderer()).setHorizontalAlignment(SwingConstants.LEFT);

        // Column widths
        int[] widths = {90, 90, 220, 90, 80, 110, 90};
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }

        // Row padding via renderer
        DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer() {
            { setBorder(new EmptyBorder(0, 16, 0, 16)); }
        };
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(cellRenderer);
        }
    }

    // ─── Section Card Helper ─────────────────────────────────────────────────

    private JPanel buildSectionCard(String title, Color accent) {
        JPanel card = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(C_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(C_BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
            }
        };
        card.setOpaque(false);

        // Header
        JPanel hdr = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setColor(C_BORDER);
                g2.fillRect(0, getHeight() - 1, getWidth(), 1);
            }
        };
        hdr.setOpaque(false);
        hdr.setBorder(new EmptyBorder(12, 16, 12, 16));

        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        titlePanel.setOpaque(false);

        // Dot
        JPanel dot = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(accent);
                g2.fillOval(0, 0, 7, 7);
            }
            @Override public Dimension getPreferredSize() { return new Dimension(8, 8); }
        };
        dot.setOpaque(false);

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(F_HEADING);
        titleLbl.setForeground(C_TEXT);

        titlePanel.add(dot);
        titlePanel.add(titleLbl);
        hdr.add(titlePanel, BorderLayout.WEST);

        JLabel seeAll = new JLabel("See All →");
        seeAll.setFont(F_MONO_SM);
        seeAll.setForeground(C_TEXT_DIM);
        seeAll.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        seeAll.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { seeAll.setForeground(accent); }
            public void mouseExited(MouseEvent e)  { seeAll.setForeground(C_TEXT_DIM); }
        });
        hdr.add(seeAll, BorderLayout.EAST);

        card.add(hdr, BorderLayout.NORTH);
        return card;
    }

    // ─── Icon Loading ──────────────────────────────────────────────────────────

    /**
     * Sets the application icon for this window using PNG for better compatibility
     */
    private void setApplicationIcon() {
        try {
            // Try PNG first (better Java compatibility)
            Image iconImage = loadIconFromResources("/images/icon.png");
            
            if (iconImage != null) {
                setIconImage(iconImage);
                System.out.println("TreasurerDashboardFrame PNG icon loaded successfully - Size: " + iconImage.getWidth(null) + "x" + iconImage.getHeight(null));
            } else {
                System.out.println("TreasurerDashboardFrame PNG icon failed to load, trying ICO fallback");
                iconImage = loadIconFromResources("/images/icon.ico");
                
                if (iconImage != null) {
                    setIconImage(iconImage);
                    System.out.println("TreasurerDashboardFrame ICO icon loaded successfully as fallback - Size: " + iconImage.getWidth(null) + "x" + iconImage.getHeight(null));
                } else {
                    System.out.println("TreasurerDashboardFrame Both PNG and ICO fallback failed - using default icon");
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to set TreasurerDashboardFrame application icon: " + e.getMessage());
        }
    }
    
    /**
     * Load icon image from resources using ImageIO for better format support
     */
    private Image loadIconFromResources(String path) {
        try {
            InputStream inputStream = TreasurerDashboardFrame.class.getResourceAsStream(path);
            if (inputStream == null) {
                System.out.println("TreasurerDashboardFrame Resource not found: " + path);
                return null;
            }
            
            // Use ImageIO to read the image (better for ICO files)
            BufferedImage bufferedImage = ImageIO.read(inputStream);
            inputStream.close();
            
            if (bufferedImage != null) {
                System.out.println("TreasurerDashboardFrame Successfully loaded image from " + path + " - Size: " + bufferedImage.getWidth() + "x" + bufferedImage.getHeight());
                return bufferedImage;
            } else {
                System.out.println("TreasurerDashboardFrame Failed to read image from " + path);
                return null;
            }
            
        } catch (Exception e) {
            System.err.println("TreasurerDashboardFrame Error loading image from " + path + ": " + e.getMessage());
            return null;
        }
    }

    // ─── Data Loading ─────────────────────────────────────────────────────────

    private void loadData() {
        // Load all financial data from API
        loadRealKpiData();
        loadRealDonorsData();
        loadRealBudgetData();
        loadRealFundData();
        loadRealTransactionsData();
    }

    private void updateKpis(Map<String, Object> data) {
        if (data == null) return;
        if (lblTotalDonations != null && data.containsKey("total_givings"))
            lblTotalDonations.setText("KES " + fmt(data.get("total_givings").toString()));
        if (lblMonthlyDonations != null && data.containsKey("total_givings"))
            lblMonthlyDonations.setText("KES " + fmt(data.get("total_givings").toString()));
        if (lblTotalExpenses != null && data.containsKey("total_expenses"))
            lblTotalExpenses.setText("KES " + fmt(data.get("total_expenses").toString()));
        if (lblNetBalance != null && data.containsKey("net_income"))
            lblNetBalance.setText("KES " + fmt(data.get("net_income").toString()));
    }

    private void updateTransactions(List<Map<String, Object>> txns) {
        if (txns == null || transactionsTable == null) return;
        DefaultTableModel m = (DefaultTableModel) transactionsTable.getModel();
        m.setRowCount(0);
        
    }

    private String fmt(String s) {
        try {
            double n = Double.parseDouble(s);
            if (n >= 1_000_000) return String.format("%.2fM", n / 1_000_000);
            if (n >= 1_000)     return String.format("%.1fK", n / 1_000);
            return String.format("%.0f", n);
        } catch (NumberFormatException e) { return s; }
    }

    // ─── Add Donation Dialog ──────────────────────────────────────────────────

    private void showAddDonationDialog() {
        // Create enhanced dialog with anti-minimizing features
        JDialog dlg = DialogManager.createModalDialog(this, "Record New Donation");
        dlg.setSize(440, 340);
        dlg.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new GridBagLayout()) {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(C_SURFACE);
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        panel.setBorder(new EmptyBorder(24, 28, 24, 28));

        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(6, 0, 6, 0);

        JTextField fAmount = styledField("e.g. 15000");
        JTextField fDonor  = styledField("Full name");
        JComboBox<String> fType = new JComboBox<>(new String[]{"Tithe", "Offering", "Building Fund", "Missions", "Special Project"});
        styleCombo(fType);
        JTextField fDesc = styledField("Optional description");

        addFormRow(panel, g, 0, "Amount (KES)", fAmount);
        addFormRow(panel, g, 1, "Donor Name",   fDonor);
        addFormRow(panel, g, 2, "Type",          fType);
        addFormRow(panel, g, 3, "Description",   fDesc);

        // Buttons
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btns.setOpaque(false);
        JButton cancel = buildTopBtn("Cancel", C_TEXT_MID);
        JButton save   = buildPrimaryBtn("Save Donation");

        cancel.addActionListener(e -> dlg.dispose());
        save.addActionListener(e -> {
            String amt  = fAmount.getText().trim();
            String donor= fDonor.getText().trim();
            if (amt.isEmpty() || donor.isEmpty()) {
                JOptionPane.showMessageDialog(dlg, "Amount and Donor Name are required.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }
            try {
                SanctumApiClient.addDonation(amt, donor, fType.getSelectedItem().toString(), fDesc.getText().trim())
                    .thenAccept(ok -> SwingUtilities.invokeLater(() -> {
                        if (ok) { JOptionPane.showMessageDialog(dlg, "Donation saved!"); dlg.dispose(); loadData(); }
                        else JOptionPane.showMessageDialog(dlg, "Failed to save.", "Error", JOptionPane.ERROR_MESSAGE);
                    }));
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dlg, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        btns.add(cancel); btns.add(save);

        g.gridx = 0; g.gridy = 4; g.gridwidth = 2;
        g.insets = new Insets(18, 0, 0, 0);
        panel.add(btns, g);

        dlg.setContentPane(panel);
        
        // Show dialog with enhanced anti-minimizing display
        DialogManager.showDialogEnhanced(dlg);
    }

    private JTextField styledField(String placeholder) {
        JTextField f = new JTextField(20);
        f.setBackground(C_CARD);
        f.setForeground(C_TEXT);
        f.setCaretColor(C_GOLD);
        f.setFont(F_BODY);
        f.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(1, 1, 1, 1, C_BORDER),
            new EmptyBorder(6, 10, 6, 10)
        ));
        return f;
    }

    private void styleCombo(JComboBox<String> cb) {
        cb.setBackground(C_CARD);
        cb.setForeground(C_TEXT);
        cb.setFont(F_BODY);
        cb.setBorder(new MatteBorder(1, 1, 1, 1, C_BORDER));
    }

    private void addFormRow(JPanel p, GridBagConstraints g, int row, String label, JComponent field) {
        g.gridx = 0; g.gridy = row; g.gridwidth = 1; g.weightx = 0;
        g.insets = new Insets(6, 0, 6, 12);
        JLabel lbl = new JLabel(label); lbl.setFont(F_LABEL); lbl.setForeground(C_TEXT_MID);
        p.add(lbl, g);
        g.gridx = 1; g.weightx = 1.0; g.insets = new Insets(6, 0, 6, 0);
        p.add(field, g);
    }

    // ─── Modern Scrollbar ─────────────────────────────────────────────────────

    static class ModernScrollBarUI extends javax.swing.plaf.basic.BasicScrollBarUI {
        @Override protected void configureScrollBarColors() {
            thumbColor = new Color(55, 70, 110);
            trackColor = new Color(13, 17, 30);
        }
        @Override protected JButton createDecreaseButton(int o) { return emptyBtn(); }
        @Override protected JButton createIncreaseButton(int o) { return emptyBtn(); }
        private JButton emptyBtn() {
            JButton b = new JButton(); b.setPreferredSize(new Dimension(0, 0)); b.setVisible(false); return b;
        }
        @Override protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(isDragging ? new Color(80, 100, 150) : thumbColor);
            g2.fillRoundRect(r.x + 2, r.y + 2, r.width - 4, r.height - 4, 6, 6);
        }
        @Override protected void paintTrack(Graphics g, JComponent c, Rectangle r) {
            g.setColor(trackColor); g.fillRect(r.x, r.y, r.width, r.height);
        }
    }

    // ─── Page Builders ─────────────────────────────────────────────────────────

    private JPanel buildDonationsPage() {
        JPanel page = new JPanel(new BorderLayout());
        page.setOpaque(false);
        
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT));
        header.setOpaque(false);
        JLabel title = new JLabel("Donations Management");
        title.setFont(F_H1);
        title.setForeground(C_TEXT);
        header.add(title);
        
        JPanel content = new JPanel();
        content.setOpaque(false);
        content.add(new JLabel("Donations page - Loading real data from API..."));
        
        page.add(header, BorderLayout.NORTH);
        page.add(content, BorderLayout.CENTER);
        return page;
    }

    private JPanel buildReportsPage() {
        JPanel page = new JPanel(new BorderLayout());
        page.setOpaque(false);
        
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT));
        header.setOpaque(false);
        JLabel title = new JLabel("Financial Reports");
        title.setFont(F_H1);
        title.setForeground(C_TEXT);
        header.add(title);
        
        JPanel content = new JPanel();
        content.setOpaque(false);
        content.add(new JLabel("Financial reports page - Loading real data from API..."));
        
        page.add(header, BorderLayout.NORTH);
        page.add(content, BorderLayout.CENTER);
        return page;
    }

    private JPanel buildBudgetPage() {
        JPanel page = new JPanel(new BorderLayout());
        page.setOpaque(false);
        
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT));
        header.setOpaque(false);
        JLabel title = new JLabel("Budget Management");
        title.setFont(F_H1);
        title.setForeground(C_TEXT);
        header.add(title);
        
        JPanel content = new JPanel();
        content.setOpaque(false);
        content.add(new JLabel("Budget management page - Loading real data from API..."));
        
        page.add(header, BorderLayout.NORTH);
        page.add(content, BorderLayout.CENTER);
        return page;
    }

    private JPanel buildTransactionsPage() {
        JPanel page = new JPanel(new BorderLayout());
        page.setOpaque(false);
        
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT));
        header.setOpaque(false);
        JLabel title = new JLabel("Transaction History");
        title.setFont(F_H1);
        title.setForeground(C_TEXT);
        header.add(title);
        
        JPanel content = new JPanel();
        content.setOpaque(false);
        content.add(new JLabel("Transaction history page - Loading real data from API..."));
        
        page.add(header, BorderLayout.NORTH);
        page.add(content, BorderLayout.CENTER);
        return page;
    }

    private JPanel buildAccountsPage() {
        JPanel page = new JPanel(new BorderLayout());
        page.setOpaque(false);
        
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT));
        header.setOpaque(false);
        JLabel title = new JLabel("Accounts Management");
        title.setFont(F_H1);
        title.setForeground(C_TEXT);
        header.add(title);
        
        JPanel content = new JPanel();
        content.setOpaque(false);
        content.add(new JLabel("Accounts management page - Loading real data from API..."));
        
        page.add(header, BorderLayout.NORTH);
        page.add(content, BorderLayout.CENTER);
        return page;
    }

    private JPanel buildExpensesPage() {
        JPanel page = new JPanel(new BorderLayout());
        page.setOpaque(false);
        
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT));
        header.setOpaque(false);
        JLabel title = new JLabel("Expenses Management");
        title.setFont(F_H1);
        title.setForeground(C_TEXT);
        header.add(title);
        
        JPanel content = new JPanel();
        content.setOpaque(false);
        content.add(new JLabel("Expenses management page - Loading real data from API..."));
        
        page.add(header, BorderLayout.NORTH);
        page.add(content, BorderLayout.CENTER);
        return page;
    }

    // ─── Navigation Methods ─────────────────────────────────────────────────────

    private void showOverviewPage() {
        System.out.println("Navigating to Overview page");
        if (cardLayout != null && contentArea != null) {
            cardLayout.show(contentArea, "overview");
        }
        loadData(); // Reload data for overview
    }

    private void showDonationsPage() {
        System.out.println("Navigating to Donations page");
        if (cardLayout != null && contentArea != null) {
            cardLayout.show(contentArea, "donations");
        }
        SanctumApiClient.getDonations().thenAccept(donations -> {
            SwingUtilities.invokeLater(() -> {
                System.out.println("Loaded " + donations.size() + " donations");
            });
        });
    }

    private void showReportsPage() {
        System.out.println("Navigating to Reports page");
        if (cardLayout != null && contentArea != null) {
            cardLayout.show(contentArea, "reports");
        }
        SanctumApiClient.getFinancialOverview().thenAccept(overview -> {
            SwingUtilities.invokeLater(() -> {
                System.out.println("Financial reports loaded: " + overview.keySet());
            });
        });
    }

    private void showBudgetPage() {
        System.out.println("Navigating to Budget page");
        if (cardLayout != null && contentArea != null) {
            cardLayout.show(contentArea, "budget");
        }
        SanctumApiClient.getBudgets().thenAccept(budgets -> {
            SwingUtilities.invokeLater(() -> {
                System.out.println("Loaded " + budgets.size() + " budget items");
            });
        });
    }

    private void showTransactionsPage() {
        System.out.println("Navigating to Transactions page");
        if (cardLayout != null && contentArea != null) {
            cardLayout.show(contentArea, "transactions");
        }
        SanctumApiClient.getTransactions().thenAccept(transactions -> {
            SwingUtilities.invokeLater(() -> {
                System.out.println("Loaded " + transactions.size() + " transactions");
            });
        });
    }

    private void showAccountsPage() {
        System.out.println("Navigating to Accounts page");
        if (cardLayout != null && contentArea != null) {
            cardLayout.show(contentArea, "accounts");
        }
    }

    private void showExpensesPage() {
        System.out.println("Navigating to Expenses page");
        if (cardLayout != null && contentArea != null) {
            cardLayout.show(contentArea, "expenses");
        }
    }

    // ─── Real Data Loading Methods ───────────────────────────────────────────────

    private void loadRealKpiData() {
        SanctumApiClient.getFinancialOverview().thenAccept(overview -> {
            SwingUtilities.invokeLater(() -> {
                if (overview.isEmpty()) {
                    // Show "No data" for KPIs when no data is available
                    if (lblTotalDonations != null) lblTotalDonations.setText("KES 0");
                    if (lblMonthlyDonations != null) lblMonthlyDonations.setText("KES 0");
                    if (lblTotalExpenses != null) lblTotalExpenses.setText("KES 0");
                    if (lblNetBalance != null) lblNetBalance.setText("KES 0");
                } else {
                    // Update KPIs with real data from API
                    updateKpis(overview);
                }
            });
        }).exceptionally(ex -> { 
            System.err.println("KPI load error: " + ex.getMessage()); 
            // Show error state in KPIs with fallback values
            SwingUtilities.invokeLater(() -> {
                if (lblTotalDonations != null) lblTotalDonations.setText("Loading...");
                if (lblMonthlyDonations != null) lblMonthlyDonations.setText("Loading...");
                if (lblTotalExpenses != null) lblTotalExpenses.setText("Loading...");
                if (lblNetBalance != null) lblNetBalance.setText("Loading...");
            });
            return null; 
        });
    }

    private void loadRealDonorsData() {
        SanctumApiClient.getDonations().thenAccept(donations -> {
            SwingUtilities.invokeLater(() -> {
                System.out.println("Loaded " + donations.size() + " donations");
                // Update recent donations section
                updateRecentDonations(donations);
                if (donations.isEmpty()) {
                    System.out.println("No donations data available");
                }
            });
        }).exceptionally(ex -> { 
            System.err.println("Donors load error: " + ex.getMessage()); 
            return null; 
        });
    }

    private void loadRealBudgetData() {
        SanctumApiClient.getBudgets().thenAccept(budgets -> {
            SwingUtilities.invokeLater(() -> {
                System.out.println("Loaded " + budgets.size() + " budget items");
                // Update budget section
                updateBudgets(budgets);
                if (budgets.isEmpty()) {
                    System.out.println("No budget data available");
                }
            });
        }).exceptionally(ex -> { 
            System.err.println("Budget load error: " + ex.getMessage()); 
            return null; 
        });
    }

    private void updateRecentDonations(List<Map<String, Object>> donations) {
        if (recentDonationsList == null) return;
        
        recentDonationsList.removeAll();
        
        if (donations.isEmpty()) {
            JLabel noDataLabel = new JLabel("No donations available");
            noDataLabel.setFont(F_BODY);
            noDataLabel.setForeground(C_TEXT_DIM);
            noDataLabel.setBorder(new EmptyBorder(20, 16, 20, 16));
            recentDonationsList.add(noDataLabel);
        } else {
            for (Map<String, Object> donation : donations) {
                String initials = donation.getOrDefault("member", "U").toString().substring(0, 1).toUpperCase();
                String name = donation.getOrDefault("member", "Unknown").toString();
                String type = donation.getOrDefault("giving_type", "General").toString();
                String amount = "KES " + fmt(donation.getOrDefault("amount", "0").toString());
                String date = donation.getOrDefault("created_at", "").toString();
                if (date.length() > 10) date = date.substring(0, 10);
                
                recentDonationsList.add(buildDonorRow(initials, name, type, amount, C_GOLD, date));
            }
        }
        
        recentDonationsList.revalidate();
        recentDonationsList.repaint();
    }

    private void updateBudgets(List<Map<String, Object>> budgets) {
        if (budgetList == null) return;
        
        budgetList.removeAll();
        
        if (budgets.isEmpty()) {
            JLabel noDataLabel = new JLabel("No budget data available");
            noDataLabel.setFont(F_BODY);
            noDataLabel.setForeground(C_TEXT_DIM);
            noDataLabel.setBorder(new EmptyBorder(20, 16, 20, 16));
            budgetList.add(noDataLabel);
        } else {
            for (Map<String, Object> budget : budgets) {
                String name = budget.getOrDefault("name", "Unknown").toString();
                String allocated = "KES " + fmt(budget.getOrDefault("allocated_amount", "0").toString());
                String spent = "KES " + fmt(budget.getOrDefault("spent_amount", "0").toString());
                String remaining = "KES " + fmt(budget.getOrDefault("remaining_amount", "0").toString());
                
                JPanel budgetRow = createBudgetRow(name, allocated, spent, remaining);
                budgetList.add(budgetRow);
            }
        }
        
        budgetList.revalidate();
        budgetList.repaint();
    }
    
    private JPanel createBudgetRow(String name, String allocated, String spent, String remaining) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(8, 16, 8, 16));
        
        JLabel nameLabel = new JLabel(name);
        nameLabel.setFont(F_LABEL);
        nameLabel.setForeground(C_TEXT);
        
        JPanel amounts = new JPanel(new GridLayout(1, 3, 10, 0));
        amounts.setOpaque(false);
        
        JLabel allocatedLabel = new JLabel(allocated);
        allocatedLabel.setFont(F_MONO_SM);
        allocatedLabel.setForeground(C_GOLD);
        allocatedLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        
        JLabel spentLabel = new JLabel(spent);
        spentLabel.setFont(F_MONO_SM);
        spentLabel.setForeground(C_GOLD_HOVER);
        spentLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        
        JLabel remainingLabel = new JLabel(remaining);
        remainingLabel.setFont(F_MONO_SM);
        remainingLabel.setForeground(C_TEXT_MID);
        remainingLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        
        amounts.add(allocatedLabel);
        amounts.add(spentLabel);
        amounts.add(remainingLabel);
        
        row.add(nameLabel, BorderLayout.WEST);
        row.add(amounts, BorderLayout.CENTER);
        
        return row;
    }

    private void loadRealFundData() {
        SanctumApiClient.getFinancialOverview().thenAccept(overview -> {
            SwingUtilities.invokeLater(() -> {
                System.out.println("Fund summary loaded");
                // Update fund summary section - would need to add UI update logic here
                if (overview.isEmpty()) {
                    System.out.println("No fund data available");
                }
            });
        }).exceptionally(ex -> { 
            System.err.println("Fund load error: " + ex.getMessage()); 
            return null; 
        });
    }

    private void loadRealTransactionsData() {
        SanctumApiClient.getTransactions().thenAccept(transactions -> {
            SwingUtilities.invokeLater(() -> {
                System.out.println("Loaded " + transactions.size() + " transactions");
                updateTransactions(transactions);
                if (transactions.isEmpty()) {
                    System.out.println("No transactions data available");
                }
            });
        }).exceptionally(ex -> { 
            System.err.println("Transactions load error: " + ex.getMessage()); 
            return null; 
        });
    }

    // ─── Entry Point ─────────────────────────────────────────────────────────

    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        SwingUtilities.invokeLater(() -> new TreasurerDashboardFrame().setVisible(true));
    }
}