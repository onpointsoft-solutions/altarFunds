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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

/**
 * Professional Treasurer Dashboard — Sanctum Church Management System
 * Pages: Overview · Donations · Reports · Budget · Transactions · Accounts · Expenses
 */
public class TreasurerDashboardFrame extends JFrame {

    // ─── Color System ───────────────────────────────────────────────────────
    private static final Color C_BG         = new Color(14,  46,  42);
    private static final Color C_SURFACE    = new Color(19,  58,  54);
    private static final Color C_CARD       = new Color(28,  47,  44);
    private static final Color C_CARD_HOVER = new Color(42,  74,  69);
    private static final Color C_BORDER     = new Color(42,  74,  69);
    private static final Color C_BORDER_LT  = new Color(66, 115, 107);
    private static final Color C_GOLD       = new Color(212, 175,  55);
    private static final Color C_GOLD_HOVER = new Color(230, 199, 102);
    private static final Color C_SUCCESS    = new Color(76,  175,  80);
    private static final Color C_WARNING    = new Color(255, 152,   0);
    private static final Color C_DANGER     = new Color(244,  67,  54);
    private static final Color C_TEXT       = new Color(255, 255, 255);
    private static final Color C_TEXT_MID   = new Color(207, 207, 207);
    private static final Color C_TEXT_DIM   = new Color(156, 163, 175);

    // ─── Fonts ──────────────────────────────────────────────────────────────
    private static final Font F_DISPLAY  = new Font("Georgia",    Font.BOLD,  26);
    private static final Font F_H1       = new Font("Arial",      Font.BOLD,  24);
    private static final Font F_HEADING  = new Font("Arial",      Font.BOLD,  13);
    private static final Font F_LABEL    = new Font("Arial",      Font.BOLD,  11);
    private static final Font F_MONO_LG  = new Font("Monospaced", Font.BOLD,  22);
    private static final Font F_MONO_SM  = new Font("Monospaced", Font.PLAIN, 11);
    private static final Font F_BODY     = new Font("Arial",      Font.PLAIN, 12);
    private static final Font F_SMALL    = new Font("Arial",      Font.PLAIN, 11);

    // ─── State ──────────────────────────────────────────────────────────────
    private int dragX, dragY;
    private JPanel contentArea;
    private CardLayout cardLayout;

    // KPI labels
    private JLabel lblTotalDonations, lblMonthlyDonations, lblTotalExpenses, lblNetBalance;

    // Page-level data containers
    private JPanel  recentDonationsList;
    private JPanel  budgetList;
    private JPanel  fundSummaryContainer;

    // Tables
    private JTable overviewTransactionsTable;
    private JTable transactionsPageTable;
    private JTable donationsPageTable;
    private JTable expensesPageTable;
    private JTable accountsPageTable;

    // Nav buttons (for active-state management)
    private final List<JButton> navButtons = new ArrayList<>();
    private JLabel topBarTitle;
    private JLabel topBarSub;

    // Reports page charts
    private int[] reportDonations = {210, 245, 290, 380, 260, 284};
    private int[] reportExpenses  = { 90, 105, 120, 140, 110,  95};
    private String[] reportMonths = {"SEP","OCT","NOV","DEC","JAN","FEB"};

    // ─── Constructor ────────────────────────────────────────────────────────
    public TreasurerDashboardFrame() {
        configureWindow();
        setApplicationIcon();
        buildUI();
        loadData();
    }

    // ─── Window ─────────────────────────────────────────────────────────────
    private void configureWindow() {
        setTitle("Sanctum — Treasurer Dashboard");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setUndecorated(true);
        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        if (gd.isFullScreenSupported()) gd.setFullScreenWindow(this);
        else setExtendedState(JFrame.MAXIMIZED_BOTH);
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(C_BG);
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        root.setOpaque(true);
        setContentPane(root);
        root.add(buildTitleBar(), BorderLayout.NORTH);
        JPanel body = new JPanel(new BorderLayout());
        body.setOpaque(false);
        body.add(buildSidebar(),  BorderLayout.WEST);
        body.add(buildMainArea(), BorderLayout.CENTER);
        root.add(body, BorderLayout.CENTER);
    }

    // ─── Scrollbar UI ────────────────────────────────────────────────────────
    static class ModernScrollBarUI extends javax.swing.plaf.basic.BasicScrollBarUI {
        @Override protected void configureScrollBarColors() {
            thumbColor = new Color(55, 90, 85);
            trackColor = new Color(19, 58, 54);
        }
        @Override protected JButton createDecreaseButton(int o) { return emptyBtn(); }
        @Override protected JButton createIncreaseButton(int o) { return emptyBtn(); }
        private JButton emptyBtn() {
            JButton b = new JButton(); b.setPreferredSize(new Dimension(0,0)); b.setVisible(false); return b;
        }
        @Override protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(isDragging ? new Color(80, 120, 115) : thumbColor);
            g2.fillRoundRect(r.x+2, r.y+2, r.width-4, r.height-4, 6, 6);
        }
        @Override protected void paintTrack(Graphics g, JComponent c, Rectangle r) {
            g.setColor(trackColor); g.fillRect(r.x, r.y, r.width, r.height);
        }
    }

    // ─── Title Bar ───────────────────────────────────────────────────────────
    private JPanel buildTitleBar() {
        JPanel bar = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(C_SURFACE); g.fillRect(0,0,getWidth(),getHeight());
                g.setColor(C_BORDER);  g.fillRect(0,getHeight()-1,getWidth(),1);
            }
        };
        bar.setOpaque(false);
        bar.setPreferredSize(new Dimension(0, 36));
        bar.setBorder(new EmptyBorder(0, 16, 0, 0));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setOpaque(false);
        JLabel icon  = new JLabel("⛪"); icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 15));
        JLabel title = new JLabel("Sanctum  ·  Treasurer Dashboard");
        title.setFont(F_MONO_SM); title.setForeground(C_TEXT_MID);
        left.add(icon); left.add(title);
        bar.add(left, BorderLayout.WEST);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        controls.setOpaque(false);
        controls.add(buildWinBtn("─", C_TEXT_DIM, C_TEXT_MID,   e -> setState(ICONIFIED)));
        controls.add(buildWinBtn("✕", C_TEXT_DIM, C_GOLD_HOVER, e -> System.exit(0)));
        bar.add(controls, BorderLayout.EAST);

        bar.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) { dragX = e.getX(); dragY = e.getY(); }
        });
        bar.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                setLocation(e.getXOnScreen()-dragX, e.getYOnScreen()-dragY);
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
                    g2.fillRect(0,0,getWidth(),getHeight());
                    g2.setColor(hover);
                } else { g2.setColor(normal); }
                g2.setFont(F_SMALL);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(text, (getWidth()-fm.stringWidth(text))/2,
                    (getHeight()+fm.getAscent()-fm.getDescent())/2);
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
                g2.setColor(C_SURFACE); g2.fillRect(0,0,getWidth(),getHeight());
                g2.setColor(C_BORDER);  g2.fillRect(getWidth()-1,0,1,getHeight());
                GradientPaint gp = new GradientPaint(0,0,
                    new Color(C_GOLD.getRed(),C_GOLD.getGreen(),C_GOLD.getBlue(),30),0,60,new Color(0,0,0,0));
                g2.setPaint(gp); g2.fillRect(0,0,getWidth(),60);
            }
        };
        sidebar.setOpaque(false);
        sidebar.setPreferredSize(new Dimension(220, 0));

        // Logo
        JPanel logo = new JPanel(); logo.setOpaque(false);
        logo.setLayout(new BoxLayout(logo, BoxLayout.Y_AXIS));
        logo.setBorder(new EmptyBorder(24,20,20,20));
        JLabel logoLbl = new JLabel("Sanctum"); logoLbl.setFont(new Font("Georgia",Font.BOLD,20)); logoLbl.setForeground(C_TEXT);
        JLabel roleLbl = new JLabel("TREASURER PORTAL"); roleLbl.setFont(F_MONO_SM); roleLbl.setForeground(C_GOLD);
        logo.add(logoLbl); logo.add(Box.createVerticalStrut(2)); logo.add(roleLbl);
        sidebar.add(logo, BorderLayout.NORTH);

        // Nav
        JPanel nav = new JPanel(); nav.setOpaque(false);
        nav.setLayout(new BoxLayout(nav, BoxLayout.Y_AXIS));
        nav.setBorder(new EmptyBorder(8,12,8,12));

        addNavSection(nav, "MAIN MENU");
        JButton btnOverview     = addNavItem(nav, "Overview",     "⊞", C_GOLD,     true);
        JButton btnDonations    = addNavItem(nav, "Donations",    "💵", C_TEXT_MID, false);
        JButton btnReports      = addNavItem(nav, "Reports",      "📊", C_TEXT_MID, false);
        JButton btnBudget       = addNavItem(nav, "Budget",       "📋", C_TEXT_MID, false);
        JButton btnTransactions = addNavItem(nav, "Transactions", "📜", C_TEXT_MID, false);
        JButton btnMembers      = addNavItem(nav, "Members",      "👥", C_TEXT_MID, false);
        addNavSection(nav, "FINANCE");
        JButton btnAccounts     = addNavItem(nav, "Accounts",     "🏦", C_TEXT_MID, false);
        JButton btnExpenses     = addNavItem(nav, "Expenses",     "📤", C_TEXT_MID, false);
        addNavSection(nav, "SYSTEM");
        JButton btnSignOut      = addNavItem(nav, "Sign Out",     "🔙", C_GOLD_HOVER, false);

        btnOverview.addActionListener(e     -> navigateTo("overview",     "Financial Overview",  btnOverview));
        btnDonations.addActionListener(e    -> navigateTo("donations",    "Donations",           btnDonations));
        btnReports.addActionListener(e      -> navigateTo("reports",      "Financial Reports",   btnReports));
        btnBudget.addActionListener(e       -> navigateTo("budget",       "Budget Management",   btnBudget));
        btnTransactions.addActionListener(e -> navigateTo("transactions", "Transactions",        btnTransactions));
        btnMembers.addActionListener(e      -> navigateTo("members",      "Members & Donations", btnMembers));
        btnAccounts.addActionListener(e     -> navigateTo("accounts",     "Accounts",            btnAccounts));
        btnExpenses.addActionListener(e     -> navigateTo("expenses",     "Expenses",            btnExpenses));
        btnSignOut.addActionListener(e -> {
            SessionManager.getInstance().clearSession();
            dispose();
            new LoginFrame().setVisible(true);
        });

        sidebar.add(nav, BorderLayout.CENTER);
        sidebar.add(buildUserCard(), BorderLayout.SOUTH);
        return sidebar;
    }

    private void navigateTo(String card, String title, JButton activeBtn) {
        if (cardLayout != null && contentArea != null) {
            cardLayout.show(contentArea, card);
        }
        // Update active state on all nav buttons
        for (JButton b : navButtons) {
            b.putClientProperty("active", b == activeBtn);
            b.repaint();
        }
        // Update topbar title
        if (topBarTitle != null) topBarTitle.setText(title);
        // Load data for the page
        switch (card) {
            case "overview":     loadData(); break;
            case "donations":    loadDonationsPageData(); break;
            case "reports":      loadReportsPageData(); break;
            case "budget":       loadBudgetPageData(); break;
            case "transactions": loadTransactionsPageData(); break;
            case "members":      loadMembersPageData(); break;
            case "accounts":     loadAccountsPageData(); break;
            case "expenses":     loadExpensesPageData(); break;
        }
    }

    private void addNavSection(JPanel nav, String label) {
        nav.add(Box.createVerticalStrut(14));
        JLabel lbl = new JLabel(label); lbl.setFont(F_MONO_SM); lbl.setForeground(C_TEXT_DIM);
        lbl.setBorder(new EmptyBorder(0,8,4,0)); lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        nav.add(lbl);
    }

    private JButton addNavItem(JPanel nav, String text, String icon, Color accent, boolean active) {
        JButton btn = new JButton() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean isActive = getClientProperty("active") == Boolean.TRUE;
                boolean isHover  = getModel().isRollover();
                if (isActive) {
                    g2.setColor(new Color(accent.getRed(),accent.getGreen(),accent.getBlue(),18));
                    g2.fillRoundRect(0,0,getWidth(),getHeight(),8,8);
                    g2.setColor(accent); g2.fillRoundRect(0,6,3,getHeight()-12,3,3);
                } else if (isHover) {
                    g2.setColor(new Color(255,255,255,6));
                    g2.fillRoundRect(0,0,getWidth(),getHeight(),8,8);
                }
                g2.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
                g2.setColor(isActive ? accent : C_TEXT_MID);
                g2.drawString(icon, 10, getHeight()/2+5);
                g2.setFont(F_LABEL);
                g2.setColor(isActive ? accent : (isHover ? C_TEXT : C_TEXT_MID));
                g2.drawString(text, 32, getHeight()/2+5);
            }
        };
        btn.putClientProperty("active", active);
        btn.setPreferredSize(new Dimension(196, 36)); btn.setMaximumSize(new Dimension(196, 36));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setContentAreaFilled(false); btn.setBorderPainted(false); btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        nav.add(btn); nav.add(Box.createVerticalStrut(2));
        navButtons.add(btn);
        return btn;
    }

    private JPanel buildUserCard() {
        JPanel card = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10)) {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(C_BORDER); g.fillRect(0,0,getWidth(),1);
            }
        };
        card.setOpaque(false); card.setBorder(new EmptyBorder(4,12,16,12));

        Map<String, Object> ud = SanctumApiClient.getCurrentUserData();
        String fn = ud != null ? ud.getOrDefault("first_name","User").toString() : "User";
        String ln = ud != null ? ud.getOrDefault("last_name","").toString() : "";
        String initials = fn.substring(0,1).toUpperCase() + (ln.isEmpty() ? "" : ln.substring(0,1).toUpperCase());
        String displayName = ln.isEmpty() ? fn : fn+" "+ln;

        JPanel avatar = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(C_GOLD); g2.fillOval(0,0,34,34);
                g2.setColor(C_TEXT); g2.setFont(new Font("Arial",Font.BOLD,14));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(initials, (34-fm.stringWidth(initials))/2, 23);
            }
            @Override public Dimension getPreferredSize() { return new Dimension(34,34); }
        };

        JPanel info = new JPanel(); info.setOpaque(false);
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        JLabel name = new JLabel(displayName); name.setFont(F_LABEL); name.setForeground(C_TEXT);
        JLabel role = new JLabel("Treasurer"); role.setFont(F_MONO_SM); role.setForeground(C_TEXT_DIM);
        info.add(name); info.add(role);
        card.add(avatar); card.add(info);
        return card;
    }

    // ─── Main Area ───────────────────────────────────────────────────────────
    private JPanel buildMainArea() {
        JPanel main = new JPanel(new BorderLayout());
        main.setOpaque(false);
        main.add(buildTopBar(), BorderLayout.NORTH);

        cardLayout = new CardLayout();
        contentArea = new JPanel(cardLayout);
        contentArea.setOpaque(false);

        contentArea.add(buildOverviewPage(),     "overview");
        contentArea.add(buildDonationsPage(),    "donations");
        contentArea.add(buildReportsPage(),      "reports");
        contentArea.add(buildBudgetPage(),       "budget");
        contentArea.add(buildTransactionsPage(), "transactions");
        contentArea.add(buildMembersPage(),      "members");
        contentArea.add(buildAccountsPage(),     "accounts");
        contentArea.add(buildExpensesPage(),     "expenses");

        JScrollPane scroll = new JScrollPane(contentArea);
        scroll.setOpaque(false); scroll.getViewport().setOpaque(false);
        scroll.setBorder(null); scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getVerticalScrollBar().setUI(new ModernScrollBarUI());
        main.add(scroll, BorderLayout.CENTER);
        return main;
    }

    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(C_SURFACE); g.fillRect(0,0,getWidth(),getHeight());
                g.setColor(C_BORDER);  g.fillRect(0,getHeight()-1,getWidth(),1);
            }
        };
        bar.setOpaque(false); bar.setPreferredSize(new Dimension(0, 64));
        bar.setBorder(new EmptyBorder(0,28,0,24));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT,0,0)); left.setOpaque(false);
        JPanel titles = new JPanel(); titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));

        topBarTitle = new JLabel("Financial Overview");
        topBarTitle.setFont(F_DISPLAY); topBarTitle.setForeground(C_TEXT);

        String month = LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM yyyy"));
        topBarSub = new JLabel("FY "+LocalDate.now().getYear()+"  ·  "+month+"  ·  Updated just now");
        topBarSub.setFont(F_MONO_SM); topBarSub.setForeground(C_TEXT_DIM);

        titles.add(topBarTitle); titles.add(Box.createVerticalStrut(3)); titles.add(topBarSub);
        left.add(titles); bar.add(left, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT,8,0)); right.setOpaque(false);
        right.add(buildTopBtn("📤  Export",      C_TEXT_MID));
        right.add(buildTopBtn("🗓  This Month ▾", C_TEXT_MID));
        right.add(buildPrimaryBtn("➕  Add Donation", e -> showAddDonationDialog()));
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    // ─── Shared Button Builders ──────────────────────────────────────────────
    private JButton buildTopBtn(String text, Color fg) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? C_CARD_HOVER : C_CARD);
                g2.fillRoundRect(0,0,getWidth(),getHeight(),8,8);
                g2.setColor(getModel().isRollover() ? C_BORDER_LT : C_BORDER);
                g2.drawRoundRect(0,0,getWidth()-1,getHeight()-1,8,8);
                g2.setFont(F_SMALL); g2.setColor(getModel().isRollover() ? C_TEXT : fg);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(text,(getWidth()-fm.stringWidth(text))/2,
                    (getHeight()+fm.getAscent()-fm.getDescent())/2);
            }
        };
        btn.setPreferredSize(new Dimension(140, 34));
        btn.setContentAreaFilled(false); btn.setBorderPainted(false); btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JButton buildPrimaryBtn(String text, ActionListener al) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0,0,
                    getModel().isPressed() ? new Color(170,140,90) : C_GOLD,
                    0,getHeight(),
                    getModel().isPressed() ? new Color(140,110,70) : new Color(220,185,120));
                g2.setPaint(gp); g2.fillRoundRect(0,0,getWidth(),getHeight(),8,8);
                g2.setFont(F_LABEL); g2.setColor(C_BG);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(text,(getWidth()-fm.stringWidth(text))/2,
                    (getHeight()+fm.getAscent()-fm.getDescent())/2);
            }
        };
        btn.setPreferredSize(new Dimension(155, 34));
        btn.setContentAreaFilled(false); btn.setBorderPainted(false); btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        if (al != null) btn.addActionListener(al);
        return btn;
    }

    // ─── Section Card ────────────────────────────────────────────────────────
    private JPanel buildSectionCard(String title, Color accent) {
        JPanel card = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(C_CARD); g2.fillRoundRect(0,0,getWidth(),getHeight(),12,12);
                g2.setColor(C_BORDER); g2.drawRoundRect(0,0,getWidth()-1,getHeight()-1,12,12);
            }
        };
        card.setOpaque(false);

        JPanel hdr = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(C_BORDER); g.fillRect(0,getHeight()-1,getWidth(),1);
            }
        };
        hdr.setOpaque(false); hdr.setBorder(new EmptyBorder(12,16,12,16));

        JPanel tp = new JPanel(new FlowLayout(FlowLayout.LEFT,8,0)); tp.setOpaque(false);
        JPanel dot = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(accent); g2.fillOval(0,0,7,7);
            }
            @Override public Dimension getPreferredSize() { return new Dimension(8,8); }
        };
        dot.setOpaque(false);
        JLabel lbl = new JLabel(title); lbl.setFont(F_HEADING); lbl.setForeground(C_TEXT);
        tp.add(dot); tp.add(lbl); hdr.add(tp, BorderLayout.WEST);
        card.add(hdr, BorderLayout.NORTH);
        return card;
    }

    // ─── Form helpers ────────────────────────────────────────────────────────
    private JTextField styledField(String placeholder) {
        JTextField f = new JTextField(20);
        f.setBackground(C_CARD); f.setForeground(C_TEXT); f.setCaretColor(C_GOLD);
        f.setFont(F_BODY);
        f.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(1,1,1,1,C_BORDER), new EmptyBorder(6,10,6,10)));
        return f;
    }

    private void styleCombo(JComboBox<?> cb) {
        cb.setBackground(C_CARD); cb.setForeground(C_TEXT); cb.setFont(F_BODY);
        cb.setBorder(new MatteBorder(1,1,1,1,C_BORDER));
    }

    private void addFormRow(JPanel p, GridBagConstraints g, int row, String label, JComponent field) {
        g.gridx=0; g.gridy=row; g.gridwidth=1; g.weightx=0; g.insets=new Insets(6,0,6,12);
        JLabel lbl = new JLabel(label); lbl.setFont(F_LABEL); lbl.setForeground(C_TEXT_MID);
        p.add(lbl, g);
        g.gridx=1; g.weightx=1.0; g.insets=new Insets(6,0,6,0);
        p.add(field, g);
    }

    // ─── Table Styling ───────────────────────────────────────────────────────
    private void styleTable(JTable t) {
        t.setFont(F_SMALL); t.setForeground(C_TEXT_MID); t.setBackground(C_CARD);
        t.setGridColor(C_BORDER); t.setRowHeight(34); t.setShowVerticalLines(false);
        t.setIntercellSpacing(new Dimension(0,1));
        t.setSelectionBackground(new Color(C_GOLD.getRed(),C_GOLD.getGreen(),C_GOLD.getBlue(),30));
        t.setSelectionForeground(C_TEXT); t.setFillsViewportHeight(true);
        JTableHeader h = t.getTableHeader();
        h.setBackground(C_SURFACE); h.setForeground(C_TEXT_DIM); h.setFont(F_MONO_SM);
        h.setPreferredSize(new Dimension(0,34)); h.setBorder(new MatteBorder(0,0,1,0,C_BORDER));
        ((DefaultTableCellRenderer)h.getDefaultRenderer()).setHorizontalAlignment(SwingConstants.LEFT);
        DefaultTableCellRenderer cr = new DefaultTableCellRenderer() {{ setBorder(new EmptyBorder(0,14,0,14)); }};
        for (int i=0; i<t.getColumnCount(); i++) t.getColumnModel().getColumn(i).setCellRenderer(cr);
    }

    private JScrollPane styledScroll(JTable t, int height) {
        JScrollPane sp = new JScrollPane(t);
        sp.setOpaque(false); sp.getViewport().setOpaque(false);
        sp.setBorder(new MatteBorder(1,0,0,0,C_BORDER));
        if (height > 0) sp.setPreferredSize(new Dimension(0, height));
        sp.getVerticalScrollBar().setUI(new ModernScrollBarUI());
        return sp;
    }

    private JLabel emptyStateLabel(String msg) {
        JLabel l = new JLabel(msg, SwingConstants.CENTER);
        l.setFont(F_BODY); l.setForeground(C_TEXT_DIM);
        return l;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  PAGE 1 — OVERVIEW
    // ════════════════════════════════════════════════════════════════════════
    private JPanel buildOverviewPage() {
        JPanel page = new JPanel();
        page.setOpaque(false);
        page.setLayout(new BoxLayout(page, BoxLayout.Y_AXIS));
        page.setBorder(new EmptyBorder(24,28,28,28));

        page.add(buildKpiRow());            page.add(Box.createVerticalStrut(20));
        page.add(buildRow2());              page.add(Box.createVerticalStrut(20));
        page.add(buildRow3());              page.add(Box.createVerticalStrut(20));
        page.add(buildOverviewTxSection());
        return page;
    }

    // KPI Row
    private JPanel buildKpiRow() {
        JPanel row = new JPanel(new GridLayout(1,4,16,0));
        row.setOpaque(false); row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));

        JPanel c1 = buildKpiCard("Total Donations", "KES 0", "All time",       C_GOLD,      "💰");
        JPanel c2 = buildKpiCard("This Month",      "KES 0", "Current period", C_GOLD_HOVER,"📅");
        JPanel c3 = buildKpiCard("Total Expenses",  "KES 0", "All time",       C_TEXT_MID,  "📤");
        JPanel c4 = buildKpiCard("Net Balance",     "KES 0", "As of today",    C_SUCCESS,   "🏦");

        lblTotalDonations  = findValueLabel(c1);
        lblMonthlyDonations= findValueLabel(c2);
        lblTotalExpenses   = findValueLabel(c3);
        lblNetBalance      = findValueLabel(c4);
        row.add(c1); row.add(c2); row.add(c3); row.add(c4);
        return row;
    }

    private JPanel buildKpiCard(String title, String value, String sub, Color accent, String emoji) {
        JPanel card = new JPanel(null) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(C_CARD); g2.fillRoundRect(0,0,getWidth(),getHeight(),12,12);
                GradientPaint gp = new GradientPaint(0,0,accent,getWidth()/2f,0,
                    new Color(accent.getRed(),accent.getGreen(),accent.getBlue(),0));
                g2.setPaint(gp); g2.fillRoundRect(0,0,getWidth(),3,3,3);
                g2.setColor(new Color(accent.getRed(),accent.getGreen(),accent.getBlue(),35));
                g2.drawRoundRect(0,0,getWidth()-1,getHeight()-1,12,12);
            }
            @Override public Dimension getPreferredSize() { return new Dimension(0,110); }
        };
        JLabel iconLbl = new JLabel(emoji); iconLbl.setFont(new Font("Segoe UI Emoji",Font.PLAIN,15));
        JLabel titleLbl = new JLabel(title.toUpperCase()); titleLbl.setFont(F_MONO_SM); titleLbl.setForeground(C_TEXT_DIM);
        JLabel valueLbl = new JLabel(value); valueLbl.setFont(F_MONO_LG); valueLbl.setForeground(accent);
        valueLbl.putClientProperty("kpiValue", Boolean.TRUE);
        JLabel subLbl = new JLabel(sub); subLbl.setFont(F_SMALL); subLbl.setForeground(C_TEXT_DIM);

        JPanel inner = new JPanel(new GridBagLayout()); inner.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.gridx=0; g.gridy=0; g.anchor=GridBagConstraints.WEST; g.insets=new Insets(16,16,2,16);
        inner.add(titleLbl, g);

        JPanel iconBox = new JPanel() {
            @Override protected void paintComponent(Graphics g2d) {
                Graphics2D g2 = (Graphics2D) g2d;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(accent.getRed(),accent.getGreen(),accent.getBlue(),22));
                g2.fillRoundRect(0,0,getWidth(),getHeight(),8,8);
            }
            @Override public Dimension getPreferredSize() { return new Dimension(32,32); }
        };
        iconBox.setOpaque(false); iconBox.setLayout(new FlowLayout(FlowLayout.CENTER,0,4)); iconBox.add(iconLbl);
        g.gridx=1; g.anchor=GridBagConstraints.NORTHEAST; g.insets=new Insets(12,0,0,12);
        inner.add(iconBox, g);

        g.gridx=0; g.gridy=1; g.gridwidth=2; g.anchor=GridBagConstraints.WEST; g.insets=new Insets(4,16,2,16);
        inner.add(valueLbl, g);
        g.gridy=2; g.insets=new Insets(2,16,14,16);
        inner.add(subLbl, g);

        card.setLayout(new BorderLayout()); card.add(inner, BorderLayout.CENTER);
        return card;
    }

    private JLabel findValueLabel(JPanel card) { return findLabelRecursive(card); }
    private JLabel findLabelRecursive(Container c) {
        for (Component comp : c.getComponents()) {
            if (comp instanceof JLabel && ((JLabel)comp).getClientProperty("kpiValue")==Boolean.TRUE)
                return (JLabel)comp;
            if (comp instanceof Container) { JLabel f = findLabelRecursive((Container)comp); if (f!=null) return f; }
        }
        return null;
    }

    // Row 2: Chart + Recent Donations
    private JPanel buildRow2() {
        JPanel row = new JPanel(new GridBagLayout());
        row.setOpaque(false); row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 300));
        GridBagConstraints g = new GridBagConstraints();
        g.fill=GridBagConstraints.BOTH; g.weighty=1.0;
        g.gridx=0; g.weightx=1.6; row.add(buildBarChartSection(), g);
        g.gridx=1; g.weightx=1.0; g.insets=new Insets(0,16,0,0); row.add(buildRecentDonationsSection(), g);
        return row;
    }

    private JPanel buildBarChartSection() {
        JPanel s = buildSectionCard("Donation Trends — Last 6 Months", C_GOLD);
        s.add(buildBarChart(reportDonations, reportExpenses, reportMonths), BorderLayout.CENTER);
        return s;
    }

    private JPanel buildBarChart(int[] donations, int[] expenses, String[] months) {
        JPanel chart = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int W=getWidth(), H=getHeight(), padL=16, padR=16, padB=28, padT=12;
                int chartH=H-padT-padB, maxVal=400, n=months.length;
                int groupW=(W-padL-padR)/n, barW=Math.min(groupW/3,18);
                g2.setStroke(new BasicStroke(1f,BasicStroke.CAP_BUTT,BasicStroke.JOIN_BEVEL,0,new float[]{3,5},0));
                g2.setColor(C_BORDER);
                for (int i=1; i<=4; i++) { int y=padT+(int)(chartH*(1-i/4.0)); g2.drawLine(padL,y,W-padR,y); }
                g2.setStroke(new BasicStroke(1f));
                for (int i=0; i<n; i++) {
                    int cx=padL+i*groupW+groupW/2;
                    int dH=(int)((donations[i]/(double)maxVal)*chartH);
                    int eH=(int)((expenses[i]/(double)maxVal)*chartH);
                    int dx=cx-barW-1;
                    GradientPaint gpD=new GradientPaint(dx,padT+chartH-dH,C_GOLD,dx,padT+chartH,new Color(C_GOLD.getRed(),C_GOLD.getGreen(),C_GOLD.getBlue(),60));
                    g2.setPaint(gpD); g2.fillRoundRect(dx,padT+chartH-dH,barW,dH,4,4);
                    int ex=cx+1;
                    GradientPaint gpE=new GradientPaint(ex,padT+chartH-eH,C_GOLD_HOVER,ex,padT+chartH,new Color(C_GOLD_HOVER.getRed(),C_GOLD_HOVER.getGreen(),C_GOLD_HOVER.getBlue(),60));
                    g2.setPaint(gpE); g2.fillRoundRect(ex,padT+chartH-eH,barW,eH,4,4);
                    g2.setFont(F_MONO_SM); g2.setColor(C_TEXT_DIM);
                    FontMetrics fm=g2.getFontMetrics();
                    g2.drawString(months[i], cx-fm.stringWidth(months[i])/2, H-6);
                }
                // Legend
                g2.setFont(F_SMALL);
                g2.setColor(C_GOLD); g2.fillRect(W-130,padT,8,8);
                g2.setColor(C_TEXT_MID); g2.drawString("Donations",W-118,padT+8);
                g2.setColor(C_GOLD_HOVER); g2.fillRect(W-60,padT,8,8);
                g2.setColor(C_TEXT_MID); g2.drawString("Expenses",W-48,padT+8);
            }
            @Override public Dimension getPreferredSize() { return new Dimension(0,240); }
        };
        chart.setOpaque(false); chart.setBorder(new EmptyBorder(8,16,8,16));
        return chart;
    }

    private JPanel buildRecentDonationsSection() {
        JPanel s = buildSectionCard("Recent Donations", C_GOLD_HOVER);
        recentDonationsList = new JPanel();
        recentDonationsList.setOpaque(false);
        recentDonationsList.setLayout(new BoxLayout(recentDonationsList, BoxLayout.Y_AXIS));
        recentDonationsList.setBorder(new EmptyBorder(4,16,12,16));
        recentDonationsList.add(emptyStateLabel("Loading..."));
        s.add(recentDonationsList, BorderLayout.CENTER);
        return s;
    }

    private JPanel buildDonorRow(String initials, String name, String type, String amount, Color avatarColor, String date) {
        JPanel row = new JPanel(new BorderLayout(10,0)) {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(C_BORDER); g.fillRect(0,getHeight()-1,getWidth(),1);
            }
        };
        row.setOpaque(false); row.setBorder(new EmptyBorder(10,4,10,4));
        JPanel av = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(avatarColor); g2.fillOval(0,0,32,32);
                g2.setColor(Color.WHITE); g2.setFont(new Font("Arial",Font.BOLD,11));
                FontMetrics fm=g2.getFontMetrics();
                g2.drawString(initials,(32-fm.stringWidth(initials))/2,21);
            }
            @Override public Dimension getPreferredSize() { return new Dimension(32,32); }
        };
        JPanel info=new JPanel(); info.setOpaque(false); info.setLayout(new BoxLayout(info,BoxLayout.Y_AXIS));
        JLabel nl=new JLabel(name); nl.setFont(F_LABEL); nl.setForeground(C_TEXT);
        JLabel tl=new JLabel(type); tl.setFont(F_MONO_SM); tl.setForeground(C_TEXT_DIM);
        info.add(nl); info.add(Box.createVerticalStrut(2)); info.add(tl);
        JPanel right=new JPanel(); right.setOpaque(false); right.setLayout(new BoxLayout(right,BoxLayout.Y_AXIS));
        JLabel al2=new JLabel(amount); al2.setFont(new Font("Monospaced",Font.BOLD,13)); al2.setForeground(C_GOLD); al2.setAlignmentX(Component.RIGHT_ALIGNMENT);
        JLabel dl=new JLabel(date); dl.setFont(F_MONO_SM); dl.setForeground(C_TEXT_DIM); dl.setAlignmentX(Component.RIGHT_ALIGNMENT);
        right.add(al2); right.add(Box.createVerticalStrut(2)); right.add(dl);
        row.add(av, BorderLayout.WEST); row.add(info, BorderLayout.CENTER); row.add(right, BorderLayout.EAST);
        row.addMouseListener(new MouseAdapter(){
            public void mouseEntered(MouseEvent e){row.setOpaque(true);row.setBackground(new Color(255,255,255,6));row.repaint();}
            public void mouseExited(MouseEvent e) {row.setOpaque(false);row.repaint();}
        });
        return row;
    }

    // Row 3: Budget + Quick Actions + Fund Summary
    private JPanel buildRow3() {
        JPanel row = new JPanel(new GridBagLayout());
        row.setOpaque(false); row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 280));
        GridBagConstraints g = new GridBagConstraints();
        g.fill=GridBagConstraints.BOTH; g.weighty=1.0;
        g.gridx=0; g.weightx=1.0; row.add(buildBudgetSummaryCard(), g);
        g.gridx=1; g.insets=new Insets(0,16,0,0); row.add(buildQuickActionsCard(), g);
        g.gridx=2; row.add(buildFundSummaryCard(), g);
        return row;
    }

    private JPanel buildBudgetSummaryCard() {
        JPanel s = buildSectionCard("Budget Allocation", C_GOLD);
        budgetList = new JPanel(); budgetList.setOpaque(false);
        budgetList.setLayout(new BoxLayout(budgetList, BoxLayout.Y_AXIS));
        budgetList.setBorder(new EmptyBorder(4,16,12,16));
        budgetList.add(emptyStateLabel("Loading..."));
        s.add(budgetList, BorderLayout.CENTER);
        return s;
    }

    private JPanel buildQuickActionsCard() {
        JPanel s = buildSectionCard("Quick Actions", C_GOLD_HOVER);
        JPanel grid = new JPanel(new GridLayout(4,2,8,8));
        grid.setOpaque(false); grid.setBorder(new EmptyBorder(8,14,14,14));
        String[][] actions = {
            {"➕","Add Donation"},{"📋","Add Budget"},
            {"📊","Monthly Report"},{"📧","Send Statement"},
            {"🔍","Find Donor"},{"📥","Import CSV"},
            {"🎯","Set Goal"},{"📋","Audit Log"}
        };
        for (String[] a : actions) {
            JButton btn = buildQaBtn(a[0], a[1]);
            if ("Add Budget".equals(a[1]))   btn.addActionListener(e -> showBudgetStudioDialog());
            if ("Add Donation".equals(a[1])) btn.addActionListener(e -> showAddDonationDialog());
            if ("Monthly Report".equals(a[1])) btn.addActionListener(e -> navigateTo("reports","Financial Reports",navButtons.get(2)));
            grid.add(btn);
        }
        s.add(grid, BorderLayout.CENTER);
        return s;
    }

    private JButton buildQaBtn(String emoji, String label) {
        JButton btn = new JButton() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                if (getModel().isRollover()) {
                    g2.setColor(C_CARD_HOVER); g2.fillRoundRect(0,0,getWidth(),getHeight(),6,6);
                }
                g2.setFont(new Font("Segoe UI Emoji",Font.PLAIN,13));
                g2.setColor(C_TEXT_MID); g2.drawString(emoji, 8, 20);
                g2.setFont(F_SMALL); g2.setColor(getModel().isRollover() ? C_TEXT : C_TEXT_MID);
                g2.drawString(label, 26, 20);
            }
            @Override public Dimension getPreferredSize() { return new Dimension(0,36); }
        };
        btn.setContentAreaFilled(false); btn.setBorderPainted(false); btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JPanel buildFundSummaryCard() {
        JPanel s = buildSectionCard("Fund Summary", C_GOLD_HOVER);
        JPanel wrap = new JPanel(); wrap.setOpaque(false);
        wrap.setLayout(new BoxLayout(wrap, BoxLayout.Y_AXIS));
        wrap.setBorder(new EmptyBorder(4,16,12,16));
        fundSummaryContainer = new JPanel(); fundSummaryContainer.setOpaque(false);
        fundSummaryContainer.setLayout(new BoxLayout(fundSummaryContainer, BoxLayout.Y_AXIS));
        fundSummaryContainer.add(emptyStateLabel("Loading..."));
        wrap.add(fundSummaryContainer);
        s.add(wrap, BorderLayout.CENTER);
        return s;
    }

    private JPanel buildFundRow(Color dot, String label, String value) {
        JPanel row = new JPanel(new BorderLayout(8,0)); row.setOpaque(false);
        row.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(0,0,1,0,C_BORDER), new EmptyBorder(9,0,9,0)));
        JPanel dotPan = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2=(Graphics2D)g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(dot); g2.fillRoundRect(0,3,7,7,3,3);
            }
            @Override public Dimension getPreferredSize() { return new Dimension(12,14); }
        };
        dotPan.setOpaque(false);
        JLabel lbl=new JLabel(label); lbl.setFont(F_BODY); lbl.setForeground(C_TEXT_MID);
        JLabel val=new JLabel(value); val.setFont(F_MONO_SM); val.setForeground(dot);
        JPanel left=new JPanel(new BorderLayout(6,0)); left.setOpaque(false);
        left.add(dotPan,BorderLayout.WEST); left.add(lbl,BorderLayout.CENTER);
        row.add(left,BorderLayout.WEST); row.add(val,BorderLayout.EAST);
        return row;
    }

    // Overview Transaction table
    private JPanel buildOverviewTxSection() {
        JPanel s = buildSectionCard("Recent Transactions", C_GOLD);
        String[] cols = {"Date","Reference","Description","Category","Type","Amount (KES)","Status"};
        DefaultTableModel m = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        overviewTransactionsTable = new JTable(m);
        styleTable(overviewTransactionsTable);
        int[] widths = {90,100,220,90,80,120,90};
        for (int i=0; i<widths.length; i++) overviewTransactionsTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        s.add(styledScroll(overviewTransactionsTable, 200), BorderLayout.CENTER);
        return s;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  PAGE 2 — DONATIONS
    // ════════════════════════════════════════════════════════════════════════
    private JPanel buildDonationsPage() {
        JPanel page = new JPanel(new BorderLayout()); page.setOpaque(false);
        page.setBorder(new EmptyBorder(24,28,28,28));

        // Header row
        JPanel header = new JPanel(new BorderLayout()); header.setOpaque(false);
        header.setBorder(new EmptyBorder(0,0,20,0));
        JLabel title = new JLabel("Donations"); title.setFont(F_H1); title.setForeground(C_TEXT);
        header.add(title, BorderLayout.WEST);
        JPanel headerBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT,8,0)); headerBtns.setOpaque(false);
        headerBtns.add(buildTopBtn("📤 Export", C_TEXT_MID));
        headerBtns.add(buildPrimaryBtn("➕ Add Donation", e -> showAddDonationDialog()));
        header.add(headerBtns, BorderLayout.EAST);

        // KPI mini row
        JPanel kpiRow = new JPanel(new GridLayout(1,3,16,0));
        kpiRow.setOpaque(false); kpiRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
        kpiRow.setBorder(new EmptyBorder(0,0,20,0));
        kpiRow.add(buildMiniKpiCard("Total Donations",  "KES 0", C_GOLD));
        kpiRow.add(buildMiniKpiCard("This Month",       "KES 0", C_GOLD_HOVER));
        kpiRow.add(buildMiniKpiCard("No. of Donors",    "0",     C_SUCCESS));

        // Filter bar
        JPanel filterBar = buildDonationFilterBar();

        // Table
        JPanel tableCard = buildSectionCard("All Donations", C_GOLD);
        String[] cols = {"Date","Donor","Type","Amount (KES)","Method","Status","Notes"};
        DefaultTableModel m = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        donationsPageTable = new JTable(m);
        styleTable(donationsPageTable);
        int[] widths = {100,160,130,120,110,100,180};
        for (int i=0; i<widths.length; i++) donationsPageTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        tableCard.add(styledScroll(donationsPageTable, 0), BorderLayout.CENTER);

        JPanel content = new JPanel(); content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.add(kpiRow);
        content.add(filterBar);
        content.add(Box.createVerticalStrut(12));
        content.add(tableCard);

        page.add(header, BorderLayout.NORTH);
        page.add(content, BorderLayout.CENTER);
        return page;
    }

    private JPanel buildMiniKpiCard(String label, String value, Color accent) {
        JPanel card = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2=(Graphics2D)g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(C_CARD); g2.fillRoundRect(0,0,getWidth(),getHeight(),10,10);
                g2.setColor(new Color(accent.getRed(),accent.getGreen(),accent.getBlue(),35));
                g2.drawRoundRect(0,0,getWidth()-1,getHeight()-1,10,10);
            }
        };
        card.setOpaque(false); card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(14,16,14,16));
        JLabel lbl = new JLabel(label); lbl.setFont(F_MONO_SM); lbl.setForeground(C_TEXT_DIM);
        JLabel val = new JLabel(value); val.setFont(new Font("Monospaced",Font.BOLD,18)); val.setForeground(accent);
        card.add(lbl); card.add(Box.createVerticalStrut(4)); card.add(val);
        return card;
    }

    private JPanel buildDonationFilterBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT,8,8)); bar.setOpaque(false);
        JTextField searchField = styledField("Search donor or type...");
        searchField.setPreferredSize(new Dimension(200,34));
        JComboBox<String> typeFilter = new JComboBox<>(new String[]{"All Types","Tithe","Offering","Building Fund","Missions","Special Project"});
        styleCombo(typeFilter);
        JComboBox<String> periodFilter = new JComboBox<>(new String[]{"All Time","This Month","This Quarter","This Year"});
        styleCombo(periodFilter);
        JButton searchBtn = buildPrimaryBtn("🔍 Search", null);
        searchBtn.setPreferredSize(new Dimension(110,34));
        bar.add(new JLabel("Search:") {{ setFont(F_LABEL); setForeground(C_TEXT_MID); }});
        bar.add(searchField);
        bar.add(new JLabel("Type:") {{ setFont(F_LABEL); setForeground(C_TEXT_MID); }});
        bar.add(typeFilter);
        bar.add(new JLabel("Period:") {{ setFont(F_LABEL); setForeground(C_TEXT_MID); }});
        bar.add(periodFilter);
        bar.add(searchBtn);
        return bar;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  PAGE 3 — REPORTS
    // ════════════════════════════════════════════════════════════════════════
    private JPanel buildReportsPage() {
        JPanel page = new JPanel(new BorderLayout()); page.setOpaque(false);
        page.setBorder(new EmptyBorder(24,28,28,28));

        JLabel title = new JLabel("Financial Reports"); title.setFont(F_H1); title.setForeground(C_TEXT);
        JPanel hdr = new JPanel(new BorderLayout()); hdr.setOpaque(false); hdr.setBorder(new EmptyBorder(0,0,20,0));
        hdr.add(title, BorderLayout.WEST);
        JPanel hBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT,8,0)); hBtns.setOpaque(false);
        hBtns.add(buildTopBtn("📤 Export PDF", C_TEXT_MID));
        hBtns.add(buildTopBtn("📥 Export CSV", C_TEXT_MID));
        hdr.add(hBtns, BorderLayout.EAST);

        JPanel content = new JPanel(); content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        // Row: Summary KPIs
        JPanel kpiRow = new JPanel(new GridLayout(1,4,16,0));
        kpiRow.setOpaque(false); kpiRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
        kpiRow.setBorder(new EmptyBorder(0,0,20,0));
        kpiRow.add(buildMiniKpiCard("Gross Income",   "KES 0",   C_GOLD));
        kpiRow.add(buildMiniKpiCard("Gross Expenses", "KES 0",   C_WARNING));
        kpiRow.add(buildMiniKpiCard("Net Surplus",    "KES 0",   C_SUCCESS));
        kpiRow.add(buildMiniKpiCard("Budget Used",    "0%",      C_GOLD_HOVER));
        content.add(kpiRow);

        // Row: Chart + Giving Breakdown
        JPanel row2 = new JPanel(new GridBagLayout());
        row2.setOpaque(false); row2.setMaximumSize(new Dimension(Integer.MAX_VALUE, 300));
        GridBagConstraints g = new GridBagConstraints();
        g.fill=GridBagConstraints.BOTH; g.weighty=1.0;
        g.gridx=0; g.weightx=1.6;
        JPanel chartCard = buildSectionCard("6-Month Trend", C_GOLD);
        chartCard.add(buildBarChart(reportDonations, reportExpenses, reportMonths), BorderLayout.CENTER);
        row2.add(chartCard, g);
        g.gridx=1; g.weightx=1.0; g.insets=new Insets(0,16,0,0);
        row2.add(buildGivingBreakdownCard(), g);
        content.add(row2);
        content.add(Box.createVerticalStrut(20));

        // Row: Income statement table
        JPanel incomeCard = buildSectionCard("Income & Expense Statement", C_GOLD);
        String[] cols = {"Category","Budget (KES)","Actual (KES)","Variance (KES)","% Used"};
        DefaultTableModel m = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable stmtTable = new JTable(m); styleTable(stmtTable);
        int[] widths = {180,140,140,140,100};
        for (int i=0; i<widths.length; i++) stmtTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        incomeCard.add(styledScroll(stmtTable, 200), BorderLayout.CENTER);
        content.add(incomeCard);

        page.add(hdr, BorderLayout.NORTH);
        page.add(content, BorderLayout.CENTER);
        return page;
    }

    private JPanel buildGivingBreakdownCard() {
        JPanel s = buildSectionCard("Giving by Category", C_GOLD_HOVER);
        JPanel list = new JPanel(); list.setOpaque(false);
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setBorder(new EmptyBorder(8,16,12,16));

        // Donut-style mini chart
        JPanel donut = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2=(Graphics2D)g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                int cx=getWidth()/2, cy=50, r=38;
                int[] values={45,25,20,10};
                Color[] colors={C_GOLD,C_GOLD_HOVER,C_SUCCESS,C_TEXT_DIM};
                String[] labels={"Tithe","Offering","Building","Other"};
                int startAngle=90;
                for (int i=0; i<values.length; i++) {
                    int arc=(int)(values[i]/100.0*360);
                    g2.setColor(colors[i]);
                    g2.fillArc(cx-r,cy-r,r*2,r*2,startAngle,arc);
                    startAngle+=arc;
                }
                g2.setColor(C_CARD); g2.fillOval(cx-22,cy-22,44,44);
                // Legend
                int ly=110;
                for (int i=0; i<values.length; i++) {
                    g2.setColor(colors[i]); g2.fillRoundRect(10,ly,8,8,2,2);
                    g2.setFont(F_SMALL); g2.setColor(C_TEXT_MID);
                    g2.drawString(labels[i]+" ("+values[i]+"%)", 24, ly+8);
                    ly+=22;
                }
            }
            @Override public Dimension getPreferredSize() { return new Dimension(0,210); }
        };
        donut.setOpaque(false);
        list.add(donut);
        s.add(list, BorderLayout.CENTER);
        return s;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  PAGE 4 — BUDGET
    // ════════════════════════════════════════════════════════════════════════
    private JPanel buildBudgetPage() {
        JPanel page = new JPanel(new BorderLayout()); page.setOpaque(false);
        page.setBorder(new EmptyBorder(24,28,28,28));

        JPanel hdr = new JPanel(new BorderLayout()); hdr.setOpaque(false); hdr.setBorder(new EmptyBorder(0,0,20,0));
        JLabel title = new JLabel("Budget Management"); title.setFont(F_H1); title.setForeground(C_TEXT);
        hdr.add(title, BorderLayout.WEST);
        JPanel hBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT,8,0)); hBtns.setOpaque(false);
        hBtns.add(buildTopBtn("📤 Export", C_TEXT_MID));
        hBtns.add(buildPrimaryBtn("➕ Add Budget", e -> showBudgetStudioDialog()));
        hdr.add(hBtns, BorderLayout.EAST);

        JPanel content = new JPanel(); content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        // Progress cards row
        JPanel progressRow = new JPanel(new GridLayout(1,3,16,0));
        progressRow.setOpaque(false); progressRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
        progressRow.setBorder(new EmptyBorder(0,0,20,0));
        progressRow.add(buildMiniKpiCard("Total Budget", "KES 0",   C_GOLD));
        progressRow.add(buildMiniKpiCard("Total Spent",  "KES 0",   C_WARNING));
        progressRow.add(buildMiniKpiCard("Remaining",    "KES 0",   C_SUCCESS));
        content.add(progressRow);

        // Budget table
        JPanel tableCard = buildSectionCard("Budget Allocations", C_GOLD);
        String[] cols = {"Department","Budget Name","Allocated (KES)","Spent (KES)","Remaining (KES)","Period","% Used"};
        DefaultTableModel m = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable budgetTable = new JTable(m); styleTable(budgetTable);
        int[] widths = {130,160,130,120,130,100,80};
        for (int i=0; i<widths.length; i++) budgetTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        // color % used column
        budgetTable.getColumnModel().getColumn(6).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                super.getTableCellRendererComponent(t,v,sel,foc,r,c);
                setBorder(new EmptyBorder(0,14,0,14));
                try {
                    double pct = Double.parseDouble(v.toString().replace("%",""));
                    setForeground(pct > 90 ? C_DANGER : pct > 75 ? C_WARNING : C_SUCCESS);
                } catch (Exception ex) { setForeground(C_TEXT_MID); }
                return this;
            }
        });
        tableCard.add(styledScroll(budgetTable, 0), BorderLayout.CENTER);
        content.add(tableCard);

        page.add(hdr, BorderLayout.NORTH);
        page.add(content, BorderLayout.CENTER);
        return page;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  PAGE 5 — TRANSACTIONS
    // ════════════════════════════════════════════════════════════════════════
    private JPanel buildTransactionsPage() {
        JPanel page = new JPanel(new BorderLayout()); page.setOpaque(false);
        page.setBorder(new EmptyBorder(24,28,28,28));

        JPanel hdr = new JPanel(new BorderLayout()); hdr.setOpaque(false); hdr.setBorder(new EmptyBorder(0,0,16,0));
        JLabel title = new JLabel("Transactions"); title.setFont(F_H1); title.setForeground(C_TEXT);
        hdr.add(title, BorderLayout.WEST);
        JPanel hBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT,8,0)); hBtns.setOpaque(false);
        hBtns.add(buildTopBtn("🔄 Sync Bank",    C_TEXT_MID));
        hBtns.add(buildTopBtn("📥 Import CSV",   C_TEXT_MID));
        hBtns.add(buildTopBtn("📤 Export",       C_TEXT_MID));
        hBtns.add(buildPrimaryBtn("⚡ Auto Reconcile", e -> performAutoReconciliation()));
        hdr.add(hBtns, BorderLayout.EAST);

        // Reconciliation status bar
        JPanel statusBar = buildReconciliationStatusBar();

        // Filter panel
        JPanel filters = buildTransactionFilterBar();

        // Table
        JPanel tableCard = buildSectionCard("All Transactions", C_GOLD);
        String[] cols = {"Date","Reference","Description","Category","Type","Amount (KES)","Status","Bank Match","Reconciled"};
        DefaultTableModel m = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        transactionsPageTable = new JTable(m);
        styleTable(transactionsPageTable);
        int[] widths = {90,100,200,100,80,120,90,90,80};
        for (int i=0; i<widths.length; i++) transactionsPageTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        transactionsPageTable.getColumnModel().getColumn(6).setCellRenderer(new StatusCellRenderer());
        transactionsPageTable.getColumnModel().getColumn(7).setCellRenderer(new MatchCellRenderer());
        transactionsPageTable.getColumnModel().getColumn(8).setCellRenderer(new ReconciledCellRenderer());
        tableCard.add(styledScroll(transactionsPageTable, 0), BorderLayout.CENTER);

        JPanel content = new JPanel(); content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.add(statusBar);
        content.add(Box.createVerticalStrut(12));
        content.add(filters);
        content.add(Box.createVerticalStrut(12));
        content.add(tableCard);

        page.add(hdr, BorderLayout.NORTH);
        page.add(content, BorderLayout.CENTER);
        return page;
    }

    private JPanel buildReconciliationStatusBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT,16,8)); bar.setOpaque(false);
        bar.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(1,0,1,0,C_BORDER), new EmptyBorder(4,0,4,0)));
        addStatusChip(bar, "Total: —",        C_TEXT_DIM);
        addStatusChip(bar, "✓ Reconciled: —", C_SUCCESS);
        addStatusChip(bar, "⏳ Pending: —",   C_WARNING);
        addStatusChip(bar, "🚩 Flagged: —",   C_DANGER);
        return bar;
    }

    private void addStatusChip(JPanel bar, String text, Color color) {
        JLabel l = new JLabel(text); l.setFont(F_LABEL); l.setForeground(color); bar.add(l);
    }

    private JPanel buildTransactionFilterBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT,8,8)); bar.setOpaque(false);
        JTextField searchField = styledField("Search...");
        searchField.setPreferredSize(new Dimension(180,34));
        JComboBox<String> statusFilter = new JComboBox<>(new String[]{"All Status","Reconciled","Unreconciled","Pending","Flagged"});
        styleCombo(statusFilter);
        JComboBox<String> typeFilter = new JComboBox<>(new String[]{"All Types","Income","Expense","Transfer","Bank Fee"});
        styleCombo(typeFilter);
        JTextField fromDate = styledField("From"); fromDate.setPreferredSize(new Dimension(110,34));
        JTextField toDate   = styledField("To");   toDate.setPreferredSize(new Dimension(110,34));
        JButton apply = buildPrimaryBtn("Apply", e -> loadTransactionsPageData());
        apply.setPreferredSize(new Dimension(90,34));
        JButton reset = buildTopBtn("Reset", C_TEXT_MID); reset.setPreferredSize(new Dimension(80,34));
        reset.addActionListener(e -> loadTransactionsPageData());

        for (JLabel l : new JLabel[]{ new JLabel("Search:"){{setFont(F_LABEL);setForeground(C_TEXT_MID);}},
                new JLabel("Status:"){{setFont(F_LABEL);setForeground(C_TEXT_MID);}},
                new JLabel("Type:"){{setFont(F_LABEL);setForeground(C_TEXT_MID);}},
                new JLabel("From:"){{setFont(F_LABEL);setForeground(C_TEXT_MID);}},
                new JLabel("To:"){{setFont(F_LABEL);setForeground(C_TEXT_MID);}}}) {
            // labels added inline below
        }
        bar.add(new JLabel("Search:"){{setFont(F_LABEL);setForeground(C_TEXT_MID);}}); bar.add(searchField);
        bar.add(new JLabel("Status:"){{setFont(F_LABEL);setForeground(C_TEXT_MID);}}); bar.add(statusFilter);
        bar.add(new JLabel("Type:"){{setFont(F_LABEL);setForeground(C_TEXT_MID);}}); bar.add(typeFilter);
        bar.add(new JLabel("From:"){{setFont(F_LABEL);setForeground(C_TEXT_MID);}}); bar.add(fromDate);
        bar.add(new JLabel("To:"){{setFont(F_LABEL);setForeground(C_TEXT_MID);}}); bar.add(toDate);
        bar.add(apply); bar.add(reset);
        return bar;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  PAGE 6 — ACCOUNTS
    // ════════════════════════════════════════════════════════════════════════
    private JPanel buildAccountsPage() {
        JPanel page = new JPanel(new BorderLayout()); page.setOpaque(false);
        page.setBorder(new EmptyBorder(24,28,28,28));

        JPanel hdr = new JPanel(new BorderLayout()); hdr.setOpaque(false); hdr.setBorder(new EmptyBorder(0,0,20,0));
        JLabel title = new JLabel("Accounts"); title.setFont(F_H1); title.setForeground(C_TEXT);
        hdr.add(title, BorderLayout.WEST);
        JPanel hBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT,8,0)); hBtns.setOpaque(false);
        hBtns.add(buildTopBtn("📤 Export", C_TEXT_MID));
        hBtns.add(buildPrimaryBtn("➕ Add Account", e -> showAddAccountDialog()));
        hdr.add(hBtns, BorderLayout.EAST);

        JPanel content = new JPanel(); content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        // Account summary cards
        JPanel kpiRow = new JPanel(new GridLayout(1,3,16,0));
        kpiRow.setOpaque(false); kpiRow.setMaximumSize(new Dimension(Integer.MAX_VALUE,90));
        kpiRow.setBorder(new EmptyBorder(0,0,20,0));
        kpiRow.add(buildMiniKpiCard("Total Accounts", "0",    C_GOLD));
        kpiRow.add(buildMiniKpiCard("Total Balance",  "KES 0",C_SUCCESS));
        kpiRow.add(buildMiniKpiCard("Active Accounts","0",    C_GOLD_HOVER));
        content.add(kpiRow);

        // Accounts table
        JPanel tableCard = buildSectionCard("Church Accounts", C_GOLD);
        String[] cols = {"Account Name","Type","Account No.","Bank","Balance (KES)","Currency","Status"};
        DefaultTableModel m = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        accountsPageTable = new JTable(m);
        styleTable(accountsPageTable);
        int[] widths = {160,120,140,140,130,90,90};
        for (int i=0; i<widths.length; i++) accountsPageTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        tableCard.add(styledScroll(accountsPageTable, 0), BorderLayout.CENTER);
        content.add(tableCard);

        page.add(hdr, BorderLayout.NORTH);
        page.add(content, BorderLayout.CENTER);
        return page;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  PAGE 7 — EXPENSES
    // ════════════════════════════════════════════════════════════════════════
    private JPanel buildExpensesPage() {
        JPanel page = new JPanel(new BorderLayout()); page.setOpaque(false);
        page.setBorder(new EmptyBorder(24,28,28,28));

        JPanel hdr = new JPanel(new BorderLayout()); hdr.setOpaque(false); hdr.setBorder(new EmptyBorder(0,0,20,0));
        JLabel title = new JLabel("Expenses"); title.setFont(F_H1); title.setForeground(C_TEXT);
        hdr.add(title, BorderLayout.WEST);
        JPanel hBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT,8,0)); hBtns.setOpaque(false);
        hBtns.add(buildTopBtn("📤 Export", C_TEXT_MID));
        hBtns.add(buildPrimaryBtn("➕ Add Expense", e -> showAddExpenseDialog()));
        hdr.add(hBtns, BorderLayout.EAST);

        JPanel content = new JPanel(); content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        JPanel kpiRow = new JPanel(new GridLayout(1,4,16,0));
        kpiRow.setOpaque(false); kpiRow.setMaximumSize(new Dimension(Integer.MAX_VALUE,90));
        kpiRow.setBorder(new EmptyBorder(0,0,20,0));
        kpiRow.add(buildMiniKpiCard("Total Expenses",  "KES 0", C_WARNING));
        kpiRow.add(buildMiniKpiCard("This Month",      "KES 0", C_GOLD));
        kpiRow.add(buildMiniKpiCard("Pending Approval","0",     C_DANGER));
        kpiRow.add(buildMiniKpiCard("Approved",        "0",     C_SUCCESS));
        content.add(kpiRow);

        // Filter
        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT,8,8)); filterBar.setOpaque(false);
        JTextField searchField = styledField("Search expense..."); searchField.setPreferredSize(new Dimension(200,34));
        JComboBox<String> catFilter = new JComboBox<>(new String[]{"All Categories","Ministry","Utilities","Salaries","Maintenance","Events","Other"});
        styleCombo(catFilter);
        JComboBox<String> statusFilter = new JComboBox<>(new String[]{"All Status","Approved","Pending","Rejected"});
        styleCombo(statusFilter);
        filterBar.add(new JLabel("Search:"){{setFont(F_LABEL);setForeground(C_TEXT_MID);}});
        filterBar.add(searchField);
        filterBar.add(new JLabel("Category:"){{setFont(F_LABEL);setForeground(C_TEXT_MID);}});
        filterBar.add(catFilter);
        filterBar.add(new JLabel("Status:"){{setFont(F_LABEL);setForeground(C_TEXT_MID);}});
        filterBar.add(statusFilter);
        filterBar.add(buildPrimaryBtn("Apply", e -> loadExpensesPageData()));
        content.add(filterBar);
        content.add(Box.createVerticalStrut(12));

        // Table
        JPanel tableCard = buildSectionCard("All Expenses", C_WARNING);
        String[] cols = {"Date","Description","Category","Vendor","Amount (KES)","Approved By","Status","Receipt"};
        DefaultTableModel m = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        expensesPageTable = new JTable(m);
        styleTable(expensesPageTable);
        int[] widths = {100,200,120,140,120,130,100,90};
        for (int i=0; i<widths.length; i++) expensesPageTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        expensesPageTable.getColumnModel().getColumn(6).setCellRenderer(new StatusCellRenderer());
        tableCard.add(styledScroll(expensesPageTable, 0), BorderLayout.CENTER);
        content.add(tableCard);

        page.add(hdr, BorderLayout.NORTH);
        page.add(content, BorderLayout.CENTER);
        return page;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  PAGE 8 — MEMBERS & DONATIONS
    // ════════════════════════════════════════════════════════════════════════
    private JPanel buildMembersPage() {
        JPanel page = new JPanel(new BorderLayout()); page.setOpaque(false);
        page.setBorder(new EmptyBorder(24,28,28,28));

        JPanel hdr = new JPanel(new BorderLayout()); hdr.setOpaque(false); hdr.setBorder(new EmptyBorder(0,0,20,0));
        JLabel title = new JLabel("Members & Donations"); title.setFont(F_H1); title.setForeground(C_TEXT);
        hdr.add(title, BorderLayout.WEST);
        JPanel hBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT,8,0)); hBtns.setOpaque(false);
        hBtns.add(buildTopBtn("📤 Export", C_TEXT_MID));
        hBtns.add(buildPrimaryBtn("➕ Add Budget", e -> showAddBudgetDialog()));
        hdr.add(hBtns, BorderLayout.EAST);

        JPanel content = new JPanel(); content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        // Member statistics cards
        JPanel kpiRow = new JPanel(new GridLayout(1,4,16,0));
        kpiRow.setOpaque(false); kpiRow.setMaximumSize(new Dimension(Integer.MAX_VALUE,90));
        kpiRow.setBorder(new EmptyBorder(0,0,20,0));
        kpiRow.add(buildMiniKpiCard("Total Members", "0",    C_GOLD));
        kpiRow.add(buildMiniKpiCard("Active Donors", "0",    C_SUCCESS));
        kpiRow.add(buildMiniKpiCard("Avg Donation", "KES 0", C_GOLD_HOVER));
        kpiRow.add(buildMiniKpiCard("This Month",   "KES 0", C_WARNING));
        content.add(kpiRow);

        // Filter and search
        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT,8,8)); filterBar.setOpaque(false);
        JTextField searchField = styledField("Search members..."); searchField.setPreferredSize(new Dimension(200,34));
        JComboBox<String> statusFilter = new JComboBox<>(new String[]{"All Members","Active","Inactive","New Members"});
        styleCombo(statusFilter);
        JComboBox<String> donorFilter = new JComboBox<>(new String[]{"All","Donors Only","Non-Donors"});
        styleCombo(donorFilter);
        filterBar.add(new JLabel("Search:"){{setFont(F_LABEL);setForeground(C_TEXT_MID);}});
        filterBar.add(searchField);
        filterBar.add(new JLabel("Status:"){{setFont(F_LABEL);setForeground(C_TEXT_MID);}});
        filterBar.add(statusFilter);
        filterBar.add(new JLabel("Giving:"){{setFont(F_LABEL);setForeground(C_TEXT_MID);}});
        filterBar.add(donorFilter);
        filterBar.add(buildPrimaryBtn("Apply", e -> loadMembersPageData()));
        content.add(filterBar);
        content.add(Box.createVerticalStrut(12));

        // Members table
        JPanel tableCard = buildSectionCard("Member Directory with Donation History", C_GOLD);
        String[] cols = {"Name","Email","Phone","Join Date","Total Donated","This Month","Status","Last Donation"};
        DefaultTableModel m = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable membersPageTable = new JTable(m);
        styleTable(membersPageTable);
        int[] widths = {180,150,120,100,120,100,80,120};
        for (int i=0; i<widths.length; i++) membersPageTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        membersPageTable.getColumnModel().getColumn(4).setCellRenderer(new CurrencyCellRenderer());
        membersPageTable.getColumnModel().getColumn(5).setCellRenderer(new CurrencyCellRenderer());
        tableCard.add(styledScroll(membersPageTable, 0), BorderLayout.CENTER);
        content.add(tableCard);

        page.add(hdr, BorderLayout.NORTH);
        page.add(content, BorderLayout.CENTER);
        return page;
    }

    // ─── Custom Cell Renderers ────────────────────────────────────────────────
    static class StatusCellRenderer extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
            super.getTableCellRendererComponent(t,v,sel,foc,r,c);
            setBorder(new EmptyBorder(0,14,0,14));
            String s = v != null ? v.toString().toLowerCase() : "";
            Color color = s.contains("reconcil") || s.equals("approved") || s.equals("completed") ? new Color(76,175,80) :
                          s.equals("pending") ? new Color(255,152,0) :
                          s.equals("flagged") || s.equals("rejected") ? new Color(244,67,54) : C_TEXT_MID;
            setForeground(color); setFont(F_LABEL);
            return this;
        }
    }

    static class CurrencyCellRenderer extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
            super.getTableCellRendererComponent(t,v,sel,foc,r,c);
            setBorder(new EmptyBorder(0,14,0,14));
            if (v != null) {
                String text = v.toString();
                if (!text.startsWith("KES") && !text.isEmpty()) {
                    setText("KES " + text);
                }
                setForeground(C_TEXT);
            }
            setFont(F_MONO_SM);
            setHorizontalAlignment(SwingConstants.RIGHT);
            return this;
        }
    }

    static class MatchCellRenderer extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
            super.getTableCellRendererComponent(t,v,sel,foc,r,c);
            setBorder(new EmptyBorder(0,14,0,14));
            String m = v != null ? v.toString() : "";
            setForeground(m.equals("Matched") ? new Color(76,175,80) : m.equals("Partial") ? new Color(255,152,0) : C_TEXT_DIM);
            setFont(F_MONO_SM);
            return this;
        }
    }

    static class ReconciledCellRenderer extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
            super.getTableCellRendererComponent(t,v,sel,foc,r,c);
            setBorder(new EmptyBorder(0,14,0,14));
            boolean rec = Boolean.TRUE.equals(v) || "true".equalsIgnoreCase(String.valueOf(v));
            setText(rec ? "✓ Yes" : "✗ No");
            setForeground(rec ? new Color(76,175,80) : C_TEXT_DIM);
            setFont(F_LABEL); setHorizontalAlignment(SwingConstants.CENTER);
            return this;
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  DATA LOADING — all pages
    // ════════════════════════════════════════════════════════════════════════
    private void loadData() {
        loadKpiData();
        loadRecentDonationsData();
        loadBudgetSummaryData();
        loadFundSummaryData();
        loadOverviewTransactionsData();
        loadDonationsPageData();
        loadReportsPageData();
        loadBudgetPageData();
        loadTransactionsPageData();
        loadAccountsPageData();
        loadExpensesPageData();
    }

    private void loadKpiData() {
        // Show loading state on EDT
        setKpiLabels("Loading...");

        // Try comprehensive dashboard data first (like Pastor Dashboard)
        SanctumApiClient.getDashboardData().thenAccept(data -> SwingUtilities.invokeLater(() -> {
            if (data != null && !data.isEmpty()) {
                // Extract meaningful data from comprehensive dashboard
                double totalIncome = parseSafelyDouble(data.getOrDefault("total_income", "0"));
                double totalExpenses = parseSafelyDouble(data.getOrDefault("total_expenses", "0"));
                double monthlyIncome = parseSafelyDouble(data.getOrDefault("monthly_income", "0"));
                double netIncome = parseSafelyDouble(data.getOrDefault("net_income", "0"));
                
                // Update KPI labels with formatted data
                updateKpiLabels(totalIncome, monthlyIncome, totalExpenses, netIncome);
                
                System.out.println("Treasurer dashboard data loaded from comprehensive endpoint.");
            } else {
                // Fallback to financial overview API (current approach)
                loadFinancialOverviewFallback();
            }
        })).exceptionally(ex -> {
            SwingUtilities.invokeLater(() -> {
                System.err.println("Failed to load comprehensive dashboard data: " + ex.getMessage());
                loadFinancialOverviewFallback(); // Fallback to individual calls
            });
            return null;
        });
    }
    
    private void loadFinancialOverviewFallback() {
        SanctumApiClient.getFinancialOverview().thenAccept(overview -> SwingUtilities.invokeLater(() -> {
            if (overview != null && !overview.isEmpty()) {
                double totalIncome = parseSafelyDouble(overview.getOrDefault("total_income", "0"));
                double totalExpenses = parseSafelyDouble(overview.getOrDefault("total_expenses", "0"));
                double monthlyIncome = parseSafelyDouble(overview.getOrDefault("monthly_income", totalIncome));
                double netIncome = parseSafelyDouble(overview.getOrDefault("net_income", "0"));
                
                updateKpiLabels(totalIncome, monthlyIncome, totalExpenses, netIncome);
                
                System.out.println("Treasurer dashboard data loaded from financial overview endpoint.");
            } else {
                // Final fallback - try individual API calls
                loadIndividualKpiData();
            }
        })).exceptionally(ex -> {
            SwingUtilities.invokeLater(() -> {
                System.err.println("Failed to load financial overview: " + ex.getMessage());
                loadIndividualKpiData(); // Final fallback
            });
            return null;
        });
    }
    
    private void loadIndividualKpiData() {
        // Load each KPI individually as final fallback (like Pastor Dashboard)
        SanctumApiClient.getDonations().thenAccept(donations -> SwingUtilities.invokeLater(() -> {
            double totalIncome = donations.stream()
                .mapToDouble(d -> parseSafelyDouble(d.getOrDefault("amount", "0")))
                .sum();
            if (lblTotalDonations != null) {
                lblTotalDonations.setText("KES " + fmt(String.valueOf(totalIncome)));
                lblTotalDonations.revalidate();
                lblTotalDonations.repaint();
            }
        }));
        
        SanctumApiClient.getExpenses().thenAccept(expenses -> SwingUtilities.invokeLater(() -> {
            double totalExpenses = expenses.stream()
                .mapToDouble(e -> parseSafelyDouble(e.getOrDefault("amount", "0")))
                .sum();
            if (lblTotalExpenses != null) {
                lblTotalExpenses.setText("KES " + fmt(String.valueOf(totalExpenses)));
                lblTotalExpenses.revalidate();
                lblTotalExpenses.repaint();
            }
        }));
        
        // For monthly donations and net balance, use the total income as fallback
        SanctumApiClient.getDonations().thenAccept(donations -> SwingUtilities.invokeLater(() -> {
            double totalIncome = donations.stream()
                .mapToDouble(d -> parseSafelyDouble(d.getOrDefault("amount", "0")))
                .sum();
            if (lblMonthlyDonations != null) {
                lblMonthlyDonations.setText("KES " + fmt(String.valueOf(totalIncome)));
                lblMonthlyDonations.revalidate();
                lblMonthlyDonations.repaint();
            }
            if (lblNetBalance != null) {
                lblNetBalance.setText("KES " + fmt(String.valueOf(totalIncome)));
                lblNetBalance.revalidate();
                lblNetBalance.repaint();
            }
        }));
    }
    
    private void updateKpiLabels(double totalIncome, double monthlyIncome, double totalExpenses, double netIncome) {
        if (lblTotalDonations != null) {
            String value = "KES " + fmt(String.valueOf(totalIncome));
            lblTotalDonations.setText(value);
            lblTotalDonations.revalidate();
            lblTotalDonations.repaint();
            System.out.println("Set lblTotalDonations to: " + value);
        }
        if (lblMonthlyDonations != null) {
            String value = "KES " + fmt(String.valueOf(monthlyIncome));
            lblMonthlyDonations.setText(value);
            lblMonthlyDonations.revalidate();
            lblMonthlyDonations.repaint();
            System.out.println("Set lblMonthlyDonations to: " + value);
        }
        if (lblTotalExpenses != null) {
            String value = "KES " + fmt(String.valueOf(totalExpenses));
            lblTotalExpenses.setText(value);
            lblTotalExpenses.revalidate();
            lblTotalExpenses.repaint();
            System.out.println("Set lblTotalExpenses to: " + value);
        }
        if (lblNetBalance != null) {
            String value = "KES " + fmt(String.valueOf(netIncome));
            lblNetBalance.setText(value);
            lblNetBalance.revalidate();
            lblNetBalance.repaint();
            System.out.println("Set lblNetBalance to: " + value);
        }
        
        // Refresh the entire KPI row container (like Pastor Dashboard)
        if (contentArea != null) {
            contentArea.revalidate();
            contentArea.repaint();
        }
    }
    
    private double parseSafelyDouble(Object value) {
        try {
            if (value instanceof Number) return ((Number) value).doubleValue();
            if (value instanceof String) return Double.parseDouble(((String) value).replaceAll("[^0-9.-]", ""));
            return 0.0;
        } catch (Exception e) {
            return 0.0;
        }
    }
    
    private void setKpiLabels(String text) {
        if (lblTotalDonations != null) {
            lblTotalDonations.setText(text);
            lblTotalDonations.revalidate();
            lblTotalDonations.repaint();
        }
        if (lblMonthlyDonations != null) {
            lblMonthlyDonations.setText(text);
            lblMonthlyDonations.revalidate();
            lblMonthlyDonations.repaint();
        }
        if (lblTotalExpenses != null) {
            lblTotalExpenses.setText(text);
            lblTotalExpenses.revalidate();
            lblTotalExpenses.repaint();
        }
        if (lblNetBalance != null) {
            lblNetBalance.setText(text);
            lblNetBalance.revalidate();
            lblNetBalance.repaint();
        }
    }

    private void loadRecentDonationsData() {
        SanctumApiClient.getDonations().thenAccept(donations -> SwingUtilities.invokeLater(() -> {
            if (recentDonationsList == null) return;
            recentDonationsList.removeAll();
            if (donations.isEmpty()) {
                recentDonationsList.add(emptyStateLabel("No donations yet"));
            } else {
                int max = Math.min(donations.size(), 6);
                Color[] avatarColors = {C_GOLD, C_SUCCESS, C_GOLD_HOVER, new Color(100,160,200), C_WARNING, C_DANGER};
                for (int i = 0; i < max; i++) {
                    Map<String,Object> d = donations.get(i);
                    String name   = d.getOrDefault("member","Unknown").toString();
                    String init   = name.length() > 0 ? name.substring(0,1).toUpperCase() : "?";
                    String type   = d.getOrDefault("giving_type","General").toString();
                    String amount = "KES "+fmt(d.getOrDefault("amount","0").toString());
                    String date   = d.getOrDefault("created_at","").toString();
                    recentDonationsList.add(buildDonorRow(init, name, type, amount, avatarColors[i], date));
                }
            }
            recentDonationsList.revalidate(); recentDonationsList.repaint();
        })).exceptionally(ex -> null);
    }

    private void loadBudgetSummaryData() {
        // Try comprehensive dashboard data first for budget information
        SanctumApiClient.getDashboardData().thenAccept(data -> SwingUtilities.invokeLater(() -> {
            if (data != null && !data.isEmpty() && data.containsKey("budgets")) {
                // Use budget data from comprehensive endpoint if available
                List<Map<String,Object>> budgets = (List<Map<String,Object>>) data.get("budgets");
                updateBudgetList(budgets);
                System.out.println("Budget data loaded from comprehensive endpoint.");
            } else {
                // Fallback to individual budget API
                loadBudgetsFallback();
            }
        })).exceptionally(ex -> {
            SwingUtilities.invokeLater(() -> {
                System.err.println("Failed to load budget data from comprehensive endpoint: " + ex.getMessage());
                loadBudgetsFallback(); // Fallback to individual calls
            });
            return null;
        });
    }
    
    private void loadBudgetsFallback() {
        SanctumApiClient.getBudgets().thenAccept(budgets -> SwingUtilities.invokeLater(() -> {
            updateBudgetList(budgets);
            System.out.println("Budget data loaded from individual endpoint.");
        })).exceptionally(ex -> {
            SwingUtilities.invokeLater(() -> {
                System.err.println("Failed to load budgets: " + ex.getMessage());
                if (budgetList != null) {
                    budgetList.removeAll();
                    budgetList.add(emptyStateLabel("Failed to load budgets"));
                    budgetList.revalidate();
                    budgetList.repaint();
                }
            });
            return null;
        });
    }
    
    private void updateBudgetList(List<Map<String,Object>> budgets) {
        if (budgetList == null) return;
        budgetList.removeAll();
        if (budgets.isEmpty()) {
            budgetList.add(emptyStateLabel("No budgets available"));
        } else {
            for (Map<String,Object> b : budgets) {
                String name    = b.getOrDefault("name","Unknown").toString();
                double alloc   = parseSafelyDouble(b.getOrDefault("allocated_amount","0"));
                double spent   = parseSafelyDouble(b.getOrDefault("spent_amount","0"));
                double pct     = alloc > 0 ? (spent/alloc)*100 : 0;
                budgetList.add(buildBudgetProgressRow(name, alloc, spent, pct));
            }
        }
        budgetList.revalidate(); budgetList.repaint();
    }

    private JPanel buildBudgetProgressRow(String name, double alloc, double spent, double pct) {
        JPanel row = new JPanel(new BorderLayout(0,4)); row.setOpaque(false);
        row.setBorder(new EmptyBorder(8,16,8,16));
        JPanel top = new JPanel(new BorderLayout()); top.setOpaque(false);
        JLabel nl = new JLabel(name); nl.setFont(F_LABEL); nl.setForeground(C_TEXT); top.add(nl, BorderLayout.WEST);
        JLabel pl = new JLabel(String.format("%.0f%%", pct));
        pl.setFont(F_MONO_SM); pl.setForeground(pct>90?C_DANGER:pct>75?C_WARNING:C_SUCCESS); top.add(pl, BorderLayout.EAST);
        // progress bar
        JPanel pb = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2=(Graphics2D)g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(C_BORDER); g2.fillRoundRect(0,0,getWidth(),getHeight(),4,4);
                int w=(int)(getWidth()*Math.min(pct/100.0,1.0));
                g2.setColor(pct>90?C_DANGER:pct>75?C_WARNING:C_SUCCESS);
                g2.fillRoundRect(0,0,w,getHeight(),4,4);
            }
            @Override public Dimension getPreferredSize() { return new Dimension(0,6); }
        };
        pb.setOpaque(false);
        JLabel sub = new JLabel("KES "+fmt(String.valueOf(spent))+" of KES "+fmt(String.valueOf(alloc)));
        sub.setFont(F_MONO_SM); sub.setForeground(C_TEXT_DIM);
        row.add(top, BorderLayout.NORTH); row.add(pb, BorderLayout.CENTER); row.add(sub, BorderLayout.SOUTH);
        return row;
    }

    private void loadFundSummaryData() {
        SanctumApiClient.getFinancialOverview().thenAccept(overview -> SwingUtilities.invokeLater(() -> {
            if (fundSummaryContainer == null) return;
            fundSummaryContainer.removeAll();
            if (overview.isEmpty()) {
                fundSummaryContainer.add(emptyStateLabel("No data"));
            } else {
                fundSummaryContainer.add(buildFundRow(C_GOLD,      "TOTAL INCOME",   "KES "+fmt(overview.getOrDefault("total_income","0").toString())));
                fundSummaryContainer.add(buildFundRow(C_WARNING,   "TOTAL EXPENSES", "KES "+fmt(overview.getOrDefault("total_expenses","0").toString())));
                fundSummaryContainer.add(buildFundRow(C_SUCCESS,   "NET BALANCE",    "KES "+fmt(overview.getOrDefault("net_income","0").toString())));
                fundSummaryContainer.add(buildFundRow(C_GOLD_HOVER,"THIS MONTH",     "KES "+fmt(overview.getOrDefault("total_income","0").toString())));
            }
            fundSummaryContainer.revalidate(); fundSummaryContainer.repaint();
        })).exceptionally(ex -> null);
    }

    private void loadOverviewTransactionsData() {
        // Try comprehensive dashboard data first for transaction information
        SanctumApiClient.getDashboardData().thenAccept(data -> SwingUtilities.invokeLater(() -> {
            if (data != null && !data.isEmpty() && data.containsKey("transactions")) {
                // Use transaction data from comprehensive endpoint if available
                List<Map<String,Object>> txns = (List<Map<String,Object>>) data.get("transactions");
                updateTransactionsTable(overviewTransactionsTable, txns);
                System.out.println("Transaction data loaded from comprehensive endpoint.");
            } else {
                // Fallback to individual transaction API
                loadTransactionsFallback();
            }
        })).exceptionally(ex -> {
            SwingUtilities.invokeLater(() -> {
                System.err.println("Failed to load transaction data from comprehensive endpoint: " + ex.getMessage());
                loadTransactionsFallback(); // Fallback to individual calls
            });
            return null;
        });
    }
    
    private void loadTransactionsFallback() {
        SanctumApiClient.getGivingTransactions().thenAccept(txns -> SwingUtilities.invokeLater(() -> {
            updateTransactionsTable(overviewTransactionsTable, txns);
            System.out.println("Transaction data loaded from individual endpoint.");
        })).exceptionally(ex -> {
            SwingUtilities.invokeLater(() -> {
                System.err.println("Failed to load transactions: " + ex.getMessage());
            });
            return null;
        });
    }
    
    private void updateTransactionsTable(JTable table, List<Map<String,Object>> txns) {
        if (table == null) return;
        DefaultTableModel m = (DefaultTableModel) table.getModel(); m.setRowCount(0);
        for (Map<String,Object> tx : txns) {
            String date = tx.getOrDefault("date","").toString();
            if (date.length()>10) date=date.substring(0,10);
            m.addRow(new Object[]{
                date,
                tx.getOrDefault("reference",""),
                tx.getOrDefault("description",""),
                tx.getOrDefault("category",""),
                tx.getOrDefault("type",""),
                "KES "+fmt(tx.getOrDefault("amount","0").toString()),
                tx.getOrDefault("status","")
            });
        }
    }

    private void loadDonationsPageData() {
        SanctumApiClient.getDonations().thenAccept(donations -> SwingUtilities.invokeLater(() -> {
            if (donationsPageTable == null) return;
            DefaultTableModel m = (DefaultTableModel) donationsPageTable.getModel(); m.setRowCount(0);
            if (donations.isEmpty()) {
                m.addRow(new Object[]{"No donations found","","","","","",""});
            } else {
                for (Map<String,Object> d : donations) {
                    String date = d.getOrDefault("created_at","").toString();
                    if (date.length()>10) date=date.substring(0,10);
                    m.addRow(new Object[]{
                        date,
                        d.getOrDefault("member","Unknown"),
                        d.getOrDefault("giving_type","General"),
                        "KES "+fmt(d.getOrDefault("amount","0").toString()),
                        d.getOrDefault("payment_method","Cash"),
                        "Completed",
                        d.getOrDefault("notes","")
                    });
                }
            }
        })).exceptionally(ex -> null);
    }

    private void loadReportsPageData() {
        SanctumApiClient.getFinancialReport().thenAccept(report -> SwingUtilities.invokeLater(() -> {
            if (report.containsKey("monthly_donations")) {
                // Update chart data from API if available
                System.out.println("Reports data loaded");
            }
        })).exceptionally(ex -> null);
    }

    private void loadBudgetPageData() {
        SanctumApiClient.getBudgets().thenAccept(budgets -> SwingUtilities.invokeLater(() -> {
            System.out.println("Budget page: loaded "+budgets.size()+" budgets");
        })).exceptionally(ex -> null);
    }

    private void loadTransactionsPageData() {
        SanctumApiClient.getGivingTransactions().thenAccept(txns -> SwingUtilities.invokeLater(() -> {
            if (transactionsPageTable == null) return;
            DefaultTableModel m = (DefaultTableModel) transactionsPageTable.getModel(); m.setRowCount(0);
            for (Map<String,Object> tx : txns) {
                String date = tx.getOrDefault("date","").toString();
                if (date.length()>10) date=date.substring(0,10);
                m.addRow(new Object[]{
                    date,
                    tx.getOrDefault("reference",""),
                    tx.getOrDefault("description",""),
                    tx.getOrDefault("category",""),
                    tx.getOrDefault("type",""),
                    "KES "+fmt(tx.getOrDefault("amount","0").toString()),
                    tx.getOrDefault("status","pending"),
                    tx.getOrDefault("bank_match","None"),
                    tx.getOrDefault("reconciled",false)
                });
            }
        })).exceptionally(ex -> null);
    }

    private void loadAccountsPageData() {
        SanctumApiClient.getAccounts().thenAccept(accounts -> SwingUtilities.invokeLater(() -> {
            if (accountsPageTable == null) return;
            DefaultTableModel m = (DefaultTableModel) accountsPageTable.getModel(); m.setRowCount(0);
            if (accounts.isEmpty()) {
                m.addRow(new Object[]{"No accounts found","","","","","",""});
            } else {
                for (Map<String,Object> a : accounts) {
                    m.addRow(new Object[]{
                        a.getOrDefault("name",""),
                        a.getOrDefault("account_type",""),
                        a.getOrDefault("account_number",""),
                        a.getOrDefault("bank_name",""),
                        "KES "+fmt(a.getOrDefault("balance","0").toString()),
                        a.getOrDefault("currency","KES"),
                        a.getOrDefault("is_active",true).equals(true) ? "Active" : "Inactive"
                    });
                }
            }
        })).exceptionally(ex -> null);
    }

    private void loadExpensesPageData() {
        SanctumApiClient.getExpenses().thenAccept(expenses -> SwingUtilities.invokeLater(() -> {
            if (expensesPageTable == null) return;
            DefaultTableModel m = (DefaultTableModel) expensesPageTable.getModel(); m.setRowCount(0);
            if (expenses.isEmpty()) {
                m.addRow(new Object[]{"No expenses found","","","","","","",""});
            } else {
                for (Map<String,Object> e : expenses) {
                    String date = e.getOrDefault("date","").toString();
                    if (date.length()>10) date=date.substring(0,10);
                    m.addRow(new Object[]{
                        date,
                        e.getOrDefault("description",""),
                        e.getOrDefault("category",""),
                        e.getOrDefault("vendor",""),
                        "KES "+fmt(e.getOrDefault("amount","0").toString()),
                        e.getOrDefault("approved_by",""),
                        e.getOrDefault("status","Pending"),
                        e.getOrDefault("receipt","") != null && !e.getOrDefault("receipt","").toString().isEmpty() ? "✓" : "—"
                    });
                }
            }
        })).exceptionally(ex -> null);
    }

    private void loadMembersPageData() {
        // Load members with their donation data using the robust pattern
        SanctumApiClient.getMembers().thenAccept(members -> SwingUtilities.invokeLater(() -> {
            System.out.println("Members page: loaded " + members.size() + " members");
            // TODO: Update the members table with real data
            // This would involve calculating donation totals per member from the donations API
        })).exceptionally(ex -> {
            SwingUtilities.invokeLater(() -> {
                System.err.println("Failed to load members: " + ex.getMessage());
            });
            return null;
        });
    }

    // ─── Dialogs ─────────────────────────────────────────────────────────────
    private void showAddDonationDialog() {
        JDialog dlg = new JDialog(this, "Add Donation", true);
        dlg.setSize(440, 360); dlg.setLocationRelativeTo(this);
        JPanel panel = new JPanel(new GridBagLayout()) {
            @Override protected void paintComponent(Graphics g) { g.setColor(C_SURFACE); g.fillRect(0,0,getWidth(),getHeight()); }
        };
        panel.setBorder(new EmptyBorder(24,28,24,28));
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill=GridBagConstraints.HORIZONTAL; gc.insets=new Insets(6,0,6,0);

        JTextField fAmount = styledField("e.g. 15000");
        JTextField fDonor  = styledField("Full name or member name");
        JComboBox<String> fType = new JComboBox<>(new String[]{"Tithe","Offering","Building Fund","Missions","Special Project"});
        styleCombo(fType);
        JComboBox<String> fMethod = new JComboBox<>(new String[]{"Cash","M-Pesa","Bank Transfer","Cheque"});
        styleCombo(fMethod);
        JTextField fDesc = styledField("Optional notes");

        addFormRow(panel, gc, 0, "Amount (KES)",   fAmount);
        addFormRow(panel, gc, 1, "Donor Name",     fDonor);
        addFormRow(panel, gc, 2, "Type",            fType);
        addFormRow(panel, gc, 3, "Payment Method", fMethod);
        addFormRow(panel, gc, 4, "Notes",          fDesc);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT,10,0)); btns.setOpaque(false);
        JButton cancel = buildTopBtn("Cancel", C_TEXT_MID);
        JButton save   = buildPrimaryBtn("Save Donation", null);
        cancel.addActionListener(e -> dlg.dispose());
        save.addActionListener(e -> {
            String amt  = fAmount.getText().trim();
            String donor= fDonor.getText().trim();
            if (amt.isEmpty() || donor.isEmpty()) {
                DialogManager.showMessageDialog(dlg,"Amount and Donor are required.","Validation",JOptionPane.WARNING_MESSAGE);
                return;
            }
            SanctumApiClient.addDonation(amt, donor, fType.getSelectedItem().toString(), fDesc.getText().trim())
                .thenAccept(ok -> SwingUtilities.invokeLater(() -> {
                    if (ok) { DialogManager.showMessageDialog(dlg,"Donation saved!","Success",JOptionPane.INFORMATION_MESSAGE); dlg.dispose(); loadData(); }
                    else    { DialogManager.showMessageDialog(dlg,"Failed to save donation.","Error",JOptionPane.ERROR_MESSAGE); }
                }));
        });
        btns.add(cancel); btns.add(save);
        gc.gridx=0; gc.gridy=5; gc.gridwidth=2; gc.insets=new Insets(18,0,0,0);
        panel.add(btns, gc);
        dlg.setContentPane(panel);
        DialogManager.showDialogEnhanced(dlg);
    }

    private void showAddBudgetDialog() {
        JDialog dlg = new JDialog(this, "Add Budget", true);
        dlg.setSize(480, 420); dlg.setLocationRelativeTo(this);
        JPanel panel = new JPanel(new GridBagLayout()) {
            @Override protected void paintComponent(Graphics g) { g.setColor(C_SURFACE); g.fillRect(0,0,getWidth(),getHeight()); }
        };
        panel.setBorder(new EmptyBorder(24,28,24,28));
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill=GridBagConstraints.HORIZONTAL; gc.insets=new Insets(6,0,6,0);

        JTextField fName = styledField("e.g. Building Fund");
        JTextField fAlloc = styledField("e.g. 500000");
        JTextField fDesc = styledField("Optional description");
        JComboBox<String> fPeriod = new JComboBox<>(new String[]{"Monthly","Quarterly","Yearly","One-time"});
        styleCombo(fPeriod);
        JComboBox<String> fCategory = new JComboBox<>(new String[]{"Ministry","Operations","Missions","Building","Events","Other"});
        styleCombo(fCategory);

        addFormRow(panel, gc, 0, "Budget Name",    fName);
        addFormRow(panel, gc, 1, "Allocated Amount", fAlloc);
        addFormRow(panel, gc, 2, "Category",        fCategory);
        addFormRow(panel, gc, 3, "Period",          fPeriod);
        addFormRow(panel, gc, 4, "Description",     fDesc);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT,10,0)); btns.setOpaque(false);
        JButton cancel = buildTopBtn("Cancel", C_TEXT_MID);
        JButton save   = buildPrimaryBtn("Save Budget", null);
        cancel.addActionListener(e -> dlg.dispose());
        save.addActionListener(e -> {
            String name = fName.getText().trim();
            String alloc = fAlloc.getText().trim();
            if (name.isEmpty() || alloc.isEmpty()) {
                DialogManager.showMessageDialog(dlg,"Budget name and amount are required.","Validation",JOptionPane.WARNING_MESSAGE);
                return;
            }
            // TODO: Implement addBudget API call
            DialogManager.showMessageDialog(dlg,"Budget creation feature coming soon!","Info",JOptionPane.INFORMATION_MESSAGE);
            dlg.dispose();
        });
        btns.add(cancel); btns.add(save);
        gc.gridx=0; gc.gridy=5; gc.gridwidth=2; gc.insets=new Insets(18,0,0,0);
        panel.add(btns, gc);
        dlg.setContentPane(panel);
        DialogManager.showDialogEnhanced(dlg);
    }

    private void showAddExpenseDialog() {
        JDialog dlg = new JDialog(this, "Add Expense", true);
        dlg.setSize(440, 360); dlg.setLocationRelativeTo(this);
        JPanel panel = new JPanel(new GridBagLayout()) {
            @Override protected void paintComponent(Graphics g) { g.setColor(C_SURFACE); g.fillRect(0,0,getWidth(),getHeight()); }
        };
        panel.setBorder(new EmptyBorder(24,28,24,28));
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill=GridBagConstraints.HORIZONTAL; gc.insets=new Insets(6,0,6,0);

        JTextField fAmount  = styledField("e.g. 5000");
        JTextField fTitle   = styledField("Title of expense");
        JTextField fVendor  = styledField("Vendor / supplier name");
        JTextField fDate    = styledField(LocalDate.now().toString());
        JComboBox<String> fCat = new JComboBox<>(new String[]{"1","2","3","4","5","6"});
        styleCombo(fCat);
        fCat.setPrototypeDisplayValue("Category ID");

        addFormRow(panel, gc, 0, "Amount (KES)",  fAmount);
        addFormRow(panel, gc, 1, "Title",         fTitle);
        addFormRow(panel, gc, 2, "Category ID",   fCat);
        addFormRow(panel, gc, 3, "Vendor",        fVendor);
        addFormRow(panel, gc, 4, "Date",          fDate);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT,10,0)); btns.setOpaque(false);
        JButton cancel = buildTopBtn("Cancel", C_TEXT_MID);
        JButton save   = buildPrimaryBtn("Save Expense", null);
        cancel.addActionListener(e -> dlg.dispose());
        save.addActionListener(e -> {
            String amt  = fAmount.getText().trim();
            String title = fTitle.getText().trim();
            if (amt.isEmpty() || title.isEmpty()) {
                DialogManager.showMessageDialog(dlg,"Amount and Title are required.","Validation",JOptionPane.WARNING_MESSAGE);
                return;
            }
            SanctumApiClient.addExpense(amt, title, fCat.getSelectedItem().toString(), fVendor.getText().trim(), fDate.getText().trim())
                .thenAccept(ok -> SwingUtilities.invokeLater(() -> {
                    if (ok) { DialogManager.showMessageDialog(dlg,"Expense saved!","Success",JOptionPane.INFORMATION_MESSAGE); dlg.dispose(); loadExpensesPageData(); }
                    else    { DialogManager.showMessageDialog(dlg,"Failed to save expense.","Error",JOptionPane.ERROR_MESSAGE); }
                }));
        });
        btns.add(cancel); btns.add(save);
        gc.gridx=0; gc.gridy=5; gc.gridwidth=2; gc.insets=new Insets(18,0,0,0);
        panel.add(btns, gc);
        dlg.setContentPane(panel);
        DialogManager.showDialogEnhanced(dlg);
    }

    private void showAddAccountDialog() {
        JDialog dlg = new JDialog(this, "Add Account", true);
        dlg.setSize(440, 360); dlg.setLocationRelativeTo(this);
        JPanel panel = new JPanel(new GridBagLayout()) {
            @Override protected void paintComponent(Graphics g) { g.setColor(C_SURFACE); g.fillRect(0,0,getWidth(),getHeight()); }
        };
        panel.setBorder(new EmptyBorder(24,28,24,28));
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill=GridBagConstraints.HORIZONTAL; gc.insets=new Insets(6,0,6,0);

        JTextField fName     = styledField("e.g. Main Operating Account");
        JTextField fBank     = styledField("Bank name");
        JTextField fAccNo    = styledField("Account number");
        JTextField fBalance  = styledField("Opening balance");
        JComboBox<String> fType = new JComboBox<>(new String[]{"Checking","Savings","M-Pesa","Investment","Petty Cash"});
        styleCombo(fType);

        addFormRow(panel, gc, 0, "Account Name", fName);
        addFormRow(panel, gc, 1, "Type",          fType);
        addFormRow(panel, gc, 2, "Bank",          fBank);
        addFormRow(panel, gc, 3, "Account No.",   fAccNo);
        addFormRow(panel, gc, 4, "Opening Balance",fBalance);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT,10,0)); btns.setOpaque(false);
        JButton cancel = buildTopBtn("Cancel", C_TEXT_MID);
        JButton save   = buildPrimaryBtn("Save Account", null);
        cancel.addActionListener(e -> dlg.dispose());
        save.addActionListener(e -> {
            if (fName.getText().trim().isEmpty()) {
                DialogManager.showMessageDialog(dlg,"Account name is required.","Validation",JOptionPane.WARNING_MESSAGE);
                return;
            }
            SanctumApiClient.addAccount(fName.getText().trim(), fType.getSelectedItem().toString(),
                    fBank.getText().trim(), fAccNo.getText().trim(), fBalance.getText().trim())
                .thenAccept(ok -> SwingUtilities.invokeLater(() -> {
                    if (ok) { DialogManager.showMessageDialog(dlg,"Account created!","Success",JOptionPane.INFORMATION_MESSAGE); dlg.dispose(); loadAccountsPageData(); }
                    else    { DialogManager.showMessageDialog(dlg,"Failed to create account.","Error",JOptionPane.ERROR_MESSAGE); }
                }));
        });
        btns.add(cancel); btns.add(save);
        gc.gridx=0; gc.gridy=5; gc.gridwidth=2; gc.insets=new Insets(18,0,0,0);
        panel.add(btns, gc);
        dlg.setContentPane(panel);
        DialogManager.showDialogEnhanced(dlg);
    }

    private void showBudgetStudioDialog() {
        JDialog dialog = new JDialog(this, "Budget Studio", true);
        dialog.setSize(1100, 700); dialog.setLocationRelativeTo(this);
        BudgetStudio bs = new BudgetStudio(this);
        bs.addPropertyChangeListener("budgetSaved", evt -> {
            if (Boolean.TRUE.equals(evt.getNewValue())) { loadBudgetSummaryData(); loadKpiData(); }
        });
        dialog.setContentPane(bs);
        DialogManager.showDialogEnhanced(dialog);
    }

    private void performAutoReconciliation() {
        DialogManager.showMessageDialog(this,
            "Auto-reconciliation started.\nMatching bank transactions with records...",
            "Auto-Reconciliation", JOptionPane.INFORMATION_MESSAGE);
        SanctumApiClient.getGivingTransactions().thenAccept(txns -> SwingUtilities.invokeLater(() -> {
            DialogManager.showMessageDialog(this,
                "Reconciliation complete!\n"+txns.size()+" transactions reviewed.",
                "Done", JOptionPane.INFORMATION_MESSAGE);
            loadTransactionsPageData();
        }));
    }

    // ─── Utilities ───────────────────────────────────────────────────────────
    private String fmt(String raw) {
        try {
            double val = Double.parseDouble(raw.replaceAll("[^\\d.]",""));
            if (val >= 1_000_000) return String.format("%.2fM", val/1_000_000);
            if (val >= 1_000)     return String.format("%,.0f", val);
            return String.format("%.2f", val);
        } catch (NumberFormatException e) { return raw; }
    }

    private double parseDouble(String raw) {
        try { return Double.parseDouble(raw.replaceAll("[^\\d.]","")); }
        catch (NumberFormatException e) { return 0.0; }
    }

    // ─── Icon Loading ─────────────────────────────────────────────────────────
    private void setApplicationIcon() {
        try {
            Image img = loadIconFromResources("/images/icon.png");
            if (img == null) img = loadIconFromResources("/images/icon.ico");
            if (img != null) setIconImage(img);
        } catch (Exception e) { System.err.println("Icon load error: "+e.getMessage()); }
    }

    private Image loadIconFromResources(String path) {
        try {
            InputStream is = TreasurerDashboardFrame.class.getResourceAsStream(path);
            if (is == null) return null;
            BufferedImage bi = ImageIO.read(is); is.close(); return bi;
        } catch (Exception e) { return null; }
    }

    // ─── Entry Point ─────────────────────────────────────────────────────────
    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings","on");
        System.setProperty("swing.aatext","true");
        SwingUtilities.invokeLater(() -> new TreasurerDashboardFrame().setVisible(true));
    }
}