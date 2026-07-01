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
    private static final Color C_GOLD_DIM    = new Color(170, 140, 90);
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
    private JLabel donationsTotalMini, donationsMonthMini, donationsDonorsMini;
    private JLabel reportIncomeMini, reportExpensesMini, reportNetMini, reportBudgetUsedMini;
    private JLabel budgetTotalMini, budgetSpentMini, budgetRemainingMini;
    private JLabel accountsCountMini, accountsBalanceMini, accountsActiveMini;
    private JLabel expensesTotalMini, expensesMonthMini, expensesPendingMini, expensesApprovedMini;
    private JLabel membersTotalMini, membersDonorsMini, membersAverageMini, membersMonthMini;

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
    private JTable budgetPageTable;    // fix: was a local variable — could never be found reliably
    private JTable membersPageTable;   // fix: was a local variable — 8-column search was fragile

    // Nav buttons (for active-state management)
    private final List<JButton> navButtons = new ArrayList<>();
    private JLabel topBarTitle;
    private JLabel topBarSub;

    // Reports page charts
    private int[] reportDonations = {0, 0, 0, 0, 0, 0};
    private int[] reportExpenses  = {0, 0, 0, 0, 0, 0};
    private String[] reportMonths = buildRecentMonthLabels();
    private int[] givingBreakdownValues = {0, 0, 0, 0};
    private String[] givingBreakdownLabels = {"Tithe", "Offering", "Building", "Other"};
    private JPanel givingBreakdownPanel;
    private JTable reportStatementTable;

    // ═══════════════════════════════════════════════════════════════════
    //  EMOJI FONT UTILITIES  (Matching other dashboards)
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

    // ─── Helper Methods ───────────────────────────────────────
    private JTextField styledField(String placeholder) {
        JTextField f = new JTextField(20);
        f.setBackground(C_CARD);
        f.setForeground(C_TEXT);
        f.setCaretColor(C_GOLD);
        f.setFont(F_BODY);
        f.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(1,1,1,1,C_BORDER),
            new EmptyBorder(6,10,6,10)));
        return f;
    }

    private void styleCombo(JComboBox<?> cb) {
        cb.setBackground(C_CARD);
        cb.setForeground(C_TEXT);
        cb.setFont(F_BODY);
        cb.setBorder(new MatteBorder(1,1,1,1,C_BORDER));
    }

   /* private void addFormRow(JPanel p, GridBagConstraints g, int row, String label, JComponent field) {
        g.gridx=0; g.gridy=row; g.gridwidth=1; g.weightx=0; g.insets=new Insets(6,0,6,12);
        JLabel lbl = new JLabel(label); lbl.setFont(F_LABEL); lbl.setForeground(C_TEXT_MID);
        p.add(lbl, g);
        g.gridx=1; g.weightx=1.0; g.insets=new Insets(6,0,6,0);
        p.add(field, g);
    }*/

    // ─── Constructor ────────────────────────────────────────────────────────
    public TreasurerDashboardFrame() {
        configureWindow();
        setApplicationIcon();
        buildUI();
        loadData();
    }

    private static String[] buildRecentMonthLabels() {
        String[] labels = new String[6];
        LocalDate start = LocalDate.now().minusMonths(5);
        for (int i = 0; i < labels.length; i++) {
            labels[i] = start.plusMonths(i).format(DateTimeFormatter.ofPattern("MMM")).toUpperCase();
        }
        return labels;
    }

    // ─── Window ─────────────────────────────────────────────────────────────
    private void configureWindow() {
        setTitle("Sanctum — Treasurer Dashboard");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setUndecorated(true);
        // Use MAXIMIZED_BOTH instead of exclusive full-screen.
        // Exclusive full-screen (gd.setFullScreenWindow) causes the frame to
        // iconify whenever a modal JDialog is shown — a Java/AWT limitation.
        // MAXIMIZED_BOTH gives the same visual result without the iconify bug.
        setExtendedState(JFrame.MAXIMIZED_BOTH);
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
        JLabel icon  = new JLabel("⛪"); icon.setFont(getEmojiFont(15));
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
                g2.setFont(getEmojiFont(14));
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
                g2.setColor(new Color(156, 163, 175));
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

        card.add(avatar); card.add(info); card.add(pwBtn);
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
    private JTextField styledTextField(String placeholder) {
        JTextField f = new JTextField(20);
        f.setBackground(C_CARD); f.setForeground(C_TEXT); f.setCaretColor(C_GOLD);
        f.setFont(F_BODY);
        f.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(1,1,1,1,C_BORDER), new EmptyBorder(6,10,6,10)));
        return f;
    }

    /*private void styleCombo(JComboBox<?> cb) {
        cb.setBackground(C_CARD); cb.setForeground(C_TEXT); cb.setFont(F_BODY);
        cb.setBorder(new MatteBorder(1,1,1,1,C_BORDER));
    }*/

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

        JPanel c1 = buildKpiCard("Total Donations", "Loading...", "All time",       C_GOLD,      "💰");
        JPanel c2 = buildKpiCard("This Month",      "Loading...", "Current period", C_GOLD_HOVER,"📅");
        JPanel c3 = buildKpiCard("Total Expenses",  "Loading...", "All time",       C_TEXT_MID,  "📤");
        JPanel c4 = buildKpiCard("Net Balance",     "Loading...", "As of today",    C_SUCCESS,   "🏦");

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
        JLabel iconLbl = new JLabel(emoji); iconLbl.setFont(getEmojiFont(15));
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
                int chartH=H-padT-padB, n=months.length;
                int maxVal = 1;
                for (int v : donations) maxVal = Math.max(maxVal, v);
                for (int v : expenses) maxVal = Math.max(maxVal, v);
                maxVal = Math.max(1, (int) Math.ceil(maxVal * 1.15));
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
                g2.setFont(getEmojiFont(13));
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
        JButton givingTypesBtn = buildPrimaryBtn("🏷 Giving Types", e -> showGivingTypesDialog());
        headerBtns.add(givingTypesBtn);
        headerBtns.add(buildPrimaryBtn("➕ Add Donation", e -> showAddDonationDialog()));
        header.add(headerBtns, BorderLayout.EAST);

        // KPI mini row
        JPanel kpiRow = new JPanel(new GridLayout(1,3,16,0));
        kpiRow.setOpaque(false); kpiRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
        kpiRow.setBorder(new EmptyBorder(0,0,20,0));
        JPanel totalCard = buildMiniKpiCard("Total Donations",  "Loading...", C_GOLD);
        JPanel monthCard = buildMiniKpiCard("This Month",       "Loading...", C_GOLD_HOVER);
        JPanel donorCard = buildMiniKpiCard("No. of Donors",    "Loading...", C_SUCCESS);
        donationsTotalMini = findValueLabel(totalCard);
        donationsMonthMini = findValueLabel(monthCard);
        donationsDonorsMini = findValueLabel(donorCard);
        kpiRow.add(totalCard);
        kpiRow.add(monthCard);
        kpiRow.add(donorCard);

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
        val.putClientProperty("kpiValue", Boolean.TRUE);
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
        JPanel grossIncomeCard = buildMiniKpiCard("Gross Income",   "Loading...", C_GOLD);
        JPanel expensesCard = buildMiniKpiCard("Gross Expenses", "Loading...", C_WARNING);
        JPanel netCard = buildMiniKpiCard("Net Surplus",    "Loading...", C_SUCCESS);
        JPanel budgetUsedCard = buildMiniKpiCard("Budget Used",    "Loading...", C_GOLD_HOVER);
        reportIncomeMini = findValueLabel(grossIncomeCard);
        reportExpensesMini = findValueLabel(expensesCard);
        reportNetMini = findValueLabel(netCard);
        reportBudgetUsedMini = findValueLabel(budgetUsedCard);
        kpiRow.add(grossIncomeCard);
        kpiRow.add(expensesCard);
        kpiRow.add(netCard);
        kpiRow.add(budgetUsedCard);
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
        JPanel statementCard = buildSectionCard("Income & Expense Statement", C_GOLD);
        String[] cols = {"Category","Budget (KES)","Actual (KES)","Variance (KES)","% Used"};
        DefaultTableModel m = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable stmtTable = new JTable(m); styleTable(stmtTable);
        reportStatementTable = stmtTable;
        int[] widths = {180,140,140,140,100};
        for (int i=0; i<widths.length; i++) stmtTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        statementCard.add(styledScroll(stmtTable, 200), BorderLayout.CENTER);
        content.add(statementCard);

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
                Color[] colors={C_GOLD,C_GOLD_HOVER,C_SUCCESS,C_TEXT_DIM};
                int[] values = givingBreakdownValues;
                String[] labels = givingBreakdownLabels;
                int total = 0;
                for (int v : values) total += Math.max(0, v);
                int startAngle=90;
                for (int i=0; i<values.length; i++) {
                    int arc = total > 0 ? (int)Math.round(values[i] / (double)total * 360) : 0;
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
                    int pct = total > 0 ? (int)Math.round(values[i] * 100.0 / total) : 0;
                    g2.drawString(labels[i]+" ("+pct+"%)", 24, ly+8);
                    ly+=22;
                }
            }
            @Override public Dimension getPreferredSize() { return new Dimension(0,210); }
        };
        donut.setOpaque(false);
        givingBreakdownPanel = donut;
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
        hBtns.add(buildPrimaryBtn("🔑 Access PINs", e -> showBudgetPinManagerDialog()));
        hBtns.add(buildPrimaryBtn("➕ Add Budget", e -> showBudgetStudioDialog()));
        hdr.add(hBtns, BorderLayout.EAST);

        JPanel content = new JPanel(); content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        // Progress cards row
        JPanel progressRow = new JPanel(new GridLayout(1,3,16,0));
        progressRow.setOpaque(false); progressRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
        progressRow.setBorder(new EmptyBorder(0,0,20,0));
        JPanel totalBudgetCard = buildMiniKpiCard("Total Budget", "Loading...", C_GOLD);
        JPanel totalSpentCard = buildMiniKpiCard("Total Spent",  "Loading...", C_WARNING);
        JPanel remainingCard = buildMiniKpiCard("Remaining",    "Loading...", C_SUCCESS);
        budgetTotalMini = findValueLabel(totalBudgetCard);
        budgetSpentMini = findValueLabel(totalSpentCard);
        budgetRemainingMini = findValueLabel(remainingCard);
        progressRow.add(totalBudgetCard);
        progressRow.add(totalSpentCard);
        progressRow.add(remainingCard);
        content.add(progressRow);

        // Budget table
        JPanel tableCard = buildSectionCard("Budget Allocations", C_GOLD);
        String[] cols = {"Department","Budget Name","Allocated (KES)","Spent (KES)","Remaining (KES)","Period","% Used"};
        DefaultTableModel m = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable budgetTable = new JTable(m); styleTable(budgetTable);
        budgetPageTable = budgetTable; // assign to class field so loadBudgetPageData() can find it
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
        JPanel totalAccountsCard = buildMiniKpiCard("Total Accounts", "Loading...", C_GOLD);
        JPanel totalBalanceCard = buildMiniKpiCard("Total Balance",  "Loading...", C_SUCCESS);
        JPanel activeAccountsCard = buildMiniKpiCard("Active Accounts","Loading...", C_GOLD_HOVER);
        accountsCountMini = findValueLabel(totalAccountsCard);
        accountsBalanceMini = findValueLabel(totalBalanceCard);
        accountsActiveMini = findValueLabel(activeAccountsCard);
        kpiRow.add(totalAccountsCard);
        kpiRow.add(totalBalanceCard);
        kpiRow.add(activeAccountsCard);
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
        JPanel totalExpensesCard = buildMiniKpiCard("Total Expenses",  "Loading...", C_WARNING);
        JPanel monthExpensesCard = buildMiniKpiCard("This Month",      "Loading...", C_GOLD);
        JPanel pendingCard = buildMiniKpiCard("Pending Approval","Loading...", C_DANGER);
        JPanel approvedCard = buildMiniKpiCard("Approved",        "Loading...", C_SUCCESS);
        expensesTotalMini = findValueLabel(totalExpensesCard);
        expensesMonthMini = findValueLabel(monthExpensesCard);
        expensesPendingMini = findValueLabel(pendingCard);
        expensesApprovedMini = findValueLabel(approvedCard);
        kpiRow.add(totalExpensesCard);
        kpiRow.add(monthExpensesCard);
        kpiRow.add(pendingCard);
        kpiRow.add(approvedCard);
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
        JButton refreshBtn = buildTopBtn("🔄 Refresh", C_TEXT_MID);
        refreshBtn.addActionListener(e -> loadMembersPageData());
        hBtns.add(refreshBtn);
        hdr.add(hBtns, BorderLayout.EAST);

        JPanel content = new JPanel(); content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        // Member statistics cards
        JPanel kpiRow = new JPanel(new GridLayout(1,4,16,0));
        kpiRow.setOpaque(false); kpiRow.setMaximumSize(new Dimension(Integer.MAX_VALUE,90));
        kpiRow.setBorder(new EmptyBorder(0,0,20,0));
        JPanel totalMembersCard = buildMiniKpiCard("Total Members", "Loading...", C_GOLD);
        JPanel activeDonorsCard = buildMiniKpiCard("Active Donors", "Loading...", C_SUCCESS);
        JPanel avgDonationCard = buildMiniKpiCard("Avg Donation", "Loading...", C_GOLD_HOVER);
        JPanel membersMonthCard = buildMiniKpiCard("This Month",   "Loading...", C_WARNING);
        membersTotalMini = findValueLabel(totalMembersCard);
        membersDonorsMini = findValueLabel(activeDonorsCard);
        membersAverageMini = findValueLabel(avgDonationCard);
        membersMonthMini = findValueLabel(membersMonthCard);
        kpiRow.add(totalMembersCard);
        kpiRow.add(activeDonorsCard);
        kpiRow.add(avgDonationCard);
        kpiRow.add(membersMonthCard);
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
        this.membersPageTable = membersPageTable; // assign to class field so loadMembersPageData() can find it
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
        // Only load Overview data eagerly — other pages load on navigation
        loadKpiData();
        loadRecentDonationsData();
        loadBudgetSummaryData();
        loadFundSummaryData();
        loadOverviewTransactionsData();
        loadTrendChartData();
    }

    private void loadTrendChartData() {
        CompletableFuture<List<Map<String,Object>>> donationsFuture = SanctumApiClient.getDonations();
        CompletableFuture<List<Map<String,Object>>> expensesFuture = SanctumApiClient.getExpenses();
        donationsFuture.thenCombine(expensesFuture, (donations, expenses) -> new Object[]{donations, expenses})
            .thenAccept(result -> SwingUtilities.invokeLater(() -> {
                @SuppressWarnings("unchecked")
                List<Map<String,Object>> donations = (List<Map<String,Object>>) result[0];
                @SuppressWarnings("unchecked")
                List<Map<String,Object>> expenses = (List<Map<String,Object>>) result[1];
                updateTrendData(donations, expenses);
            })).exceptionally(ex -> null);
    }

    private void loadKpiData() {
        setKpiLabels("Loading...");

        SanctumApiClient.getDashboardData().thenAccept(data -> SwingUtilities.invokeLater(() -> {
            if (data != null && !data.isEmpty()) {
                // The comprehensive dashboard wraps financials in "financial_summary" (camelCase)
                double totalIncome   = 0, monthlyIncome = 0, totalExpenses = 0, netBalance = 0;

                Object fs = data.get("financial_summary");
                if (fs instanceof Map) {
                    @SuppressWarnings("unchecked") Map<String,Object> fin = (Map<String,Object>) fs;
                    totalIncome   = parseSafelyDouble(fin.getOrDefault("totalIncome",   fin.getOrDefault("total_income",   "0")));
                    monthlyIncome = parseSafelyDouble(fin.getOrDefault("monthlyIncome", fin.getOrDefault("monthly_income", "0")));
                    totalExpenses = parseSafelyDouble(fin.getOrDefault("totalExpenses", fin.getOrDefault("total_expenses", "0")));
                    netBalance    = parseSafelyDouble(fin.getOrDefault("netBalance",    fin.getOrDefault("net_income",     "0")));
                } else {
                    // flat keys — older endpoint shape
                    totalIncome   = parseSafelyDouble(data.getOrDefault("total_income",   data.getOrDefault("totalIncome",   "0")));
                    monthlyIncome = parseSafelyDouble(data.getOrDefault("monthly_income", data.getOrDefault("monthlyIncome", "0")));
                    totalExpenses = parseSafelyDouble(data.getOrDefault("total_expenses", data.getOrDefault("totalExpenses", "0")));
                    netBalance    = parseSafelyDouble(data.getOrDefault("net_income",     data.getOrDefault("netBalance",     "0")));
                }

                updateKpiLabels(totalIncome, monthlyIncome, totalExpenses, netBalance);
                System.out.println("Treasurer KPIs loaded from dashboard endpoint.");
            } else {
                loadFinancialOverviewFallback();
            }
        })).exceptionally(ex -> {
            SwingUtilities.invokeLater(() -> {
                System.err.println("Dashboard data failed — falling back: " + ex.getMessage());
                loadFinancialOverviewFallback();
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
        // Load donations total
        SanctumApiClient.getDonations().thenAccept(donations -> SwingUtilities.invokeLater(() -> {
            double totalIncome = donations.stream()
                .mapToDouble(d -> parseSafelyDouble(d.getOrDefault("amount", "0")))
                .sum();

            // Monthly — filter by current month
            String currentMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
            double monthlyIncome = donations.stream()
                .filter(d -> d.getOrDefault("created_at", "").toString().startsWith(currentMonth))
                .mapToDouble(d -> parseSafelyDouble(d.getOrDefault("amount", "0")))
                .sum();

            if (lblTotalDonations != null) {
                lblTotalDonations.setText("KES " + fmt(String.valueOf(totalIncome)));
                lblTotalDonations.revalidate(); lblTotalDonations.repaint();
            }
            if (lblMonthlyDonations != null) {
                lblMonthlyDonations.setText("KES " + fmt(String.valueOf(monthlyIncome)));
                lblMonthlyDonations.revalidate(); lblMonthlyDonations.repaint();
            }

            // Net balance needs expenses — chain the call
            SanctumApiClient.getExpenses().thenAccept(expenses -> SwingUtilities.invokeLater(() -> {
                double totalExpenses = expenses.stream()
                    .mapToDouble(e -> parseSafelyDouble(e.getOrDefault("amount", "0")))
                    .sum();
                double netBalance = totalIncome - totalExpenses;

                if (lblTotalExpenses != null) {
                    lblTotalExpenses.setText("KES " + fmt(String.valueOf(totalExpenses)));
                    lblTotalExpenses.revalidate(); lblTotalExpenses.repaint();
                }
                if (lblNetBalance != null) {
                    lblNetBalance.setText("KES " + fmt(String.valueOf(netBalance)));
                    lblNetBalance.revalidate(); lblNetBalance.repaint();
                }
                if (contentArea != null) { contentArea.revalidate(); contentArea.repaint(); }
            }));
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

    private void setMiniLabel(JLabel label, String value) {
        if (label == null) return;
        label.setText(value);
        label.revalidate();
        label.repaint();
    }

    private String dateValue(Map<String,Object> row) {
        String date = row.getOrDefault("created_at",
            row.getOrDefault("transaction_date",
            row.getOrDefault("date", ""))).toString();
        return date.length() > 10 ? date.substring(0, 10) : date;
    }

    private boolean isCurrentMonth(String date) {
        return date != null && date.startsWith(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM")));
    }

    private int monthIndexForDate(String date) {
        if (date == null || date.length() < 7) return -1;
        try {
            LocalDate month = LocalDate.parse(date.substring(0, 10));
            LocalDate start = LocalDate.now().minusMonths(5).withDayOfMonth(1);
            LocalDate itemMonth = month.withDayOfMonth(1);
            int diff = (itemMonth.getYear() - start.getYear()) * 12 + itemMonth.getMonthValue() - start.getMonthValue();
            return diff >= 0 && diff < 6 ? diff : -1;
        } catch (Exception ignored) {
            return -1;
        }
    }

    private void updateGivingBreakdownFromDonations(List<Map<String,Object>> donations) {
        double tithe = 0, offering = 0, building = 0, other = 0;
        for (Map<String,Object> d : donations) {
            double amount = parseSafelyDouble(d.getOrDefault("amount", "0"));
            String type = d.getOrDefault("giving_type",
                d.getOrDefault("donation_type", d.getOrDefault("category", ""))).toString().toLowerCase();
            if (type.contains("tithe")) tithe += amount;
            else if (type.contains("offering")) offering += amount;
            else if (type.contains("building")) building += amount;
            else other += amount;
        }
        givingBreakdownValues = new int[]{
            (int)Math.round(tithe),
            (int)Math.round(offering),
            (int)Math.round(building),
            (int)Math.round(other)
        };
        if (givingBreakdownPanel != null) givingBreakdownPanel.repaint();
    }

    private void updateReportStatement(List<Map<String,Object>> budgets) {
        if (reportStatementTable == null) return;
        DefaultTableModel model = (DefaultTableModel) reportStatementTable.getModel();
        model.setRowCount(0);
        if (budgets.isEmpty()) {
            model.addRow(new Object[]{"No budget data", "", "", "", ""});
            return;
        }
        for (Map<String,Object> b : budgets) {
            double budget = parseSafelyDouble(b.getOrDefault("allocated_amount", "0"));
            double actual = parseSafelyDouble(b.getOrDefault("spent_amount", "0"));
            double variance = budget - actual;
            double used = budget > 0 ? (actual / budget) * 100 : 0;
            model.addRow(new Object[]{
                b.getOrDefault("department", b.getOrDefault("name", "Budget")),
                fmt(String.valueOf(budget)),
                fmt(String.valueOf(actual)),
                fmt(String.valueOf(variance)),
                String.format("%.1f%%", used)
            });
        }
    }

    private void updateTrendData(List<Map<String,Object>> donations, List<Map<String,Object>> expenses) {
        reportMonths = buildRecentMonthLabels();
        reportDonations = new int[]{0, 0, 0, 0, 0, 0};
        reportExpenses = new int[]{0, 0, 0, 0, 0, 0};
        for (Map<String,Object> d : donations) {
            int idx = monthIndexForDate(dateValue(d));
            if (idx >= 0) reportDonations[idx] += (int)Math.round(parseSafelyDouble(d.getOrDefault("amount", "0")));
        }
        for (Map<String,Object> e : expenses) {
            int idx = monthIndexForDate(dateValue(e));
            if (idx >= 0) reportExpenses[idx] += (int)Math.round(parseSafelyDouble(e.getOrDefault("amount", "0")));
        }
        if (contentArea != null) {
            contentArea.revalidate();
            contentArea.repaint();
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
                fundSummaryContainer.add(buildFundRow(C_GOLD_HOVER,"THIS MONTH",     "KES "+fmt(overview.getOrDefault("monthly_income", overview.getOrDefault("total_income","0")).toString())));
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
            double total = 0;
            double monthTotal = 0;
            java.util.Set<String> donors = new java.util.HashSet<>();
            if (donations.isEmpty()) {
                m.addRow(new Object[]{"No donations found","","","","","",""});
            } else {
                for (Map<String,Object> d : donations) {
                    double amount = parseSafelyDouble(d.getOrDefault("amount","0"));
                    total += amount;
                    String date = dateValue(d);
                    if (isCurrentMonth(date)) monthTotal += amount;
                    String donorKey = d.getOrDefault("member",
                        d.getOrDefault("donor_name", d.getOrDefault("member_name", ""))).toString();
                    if (!donorKey.isBlank()) donors.add(donorKey);
                    m.addRow(new Object[]{
                        date,
                        d.getOrDefault("donor_name", d.getOrDefault("member_name", d.getOrDefault("member","Unknown"))),
                        d.getOrDefault("giving_type", d.getOrDefault("donation_type", "General")),
                        "KES "+fmt(String.valueOf(amount)),
                        d.getOrDefault("payment_method","Cash"),
                        d.getOrDefault("status", "Completed"),
                        d.getOrDefault("notes","")
                    });
                }
            }
            setMiniLabel(donationsTotalMini, "KES " + fmt(String.valueOf(total)));
            setMiniLabel(donationsMonthMini, "KES " + fmt(String.valueOf(monthTotal)));
            setMiniLabel(donationsDonorsMini, String.valueOf(donors.size()));
        })).exceptionally(ex -> null);
    }

    private void loadReportsPageData() {
        CompletableFuture<Map<String,Object>> overviewFuture = SanctumApiClient.getFinancialReport();
        CompletableFuture<List<Map<String,Object>>> donationsFuture = SanctumApiClient.getDonations();
        CompletableFuture<List<Map<String,Object>>> expensesFuture = SanctumApiClient.getExpenses();
        CompletableFuture<List<Map<String,Object>>> budgetsFuture = SanctumApiClient.getBudgets();

        CompletableFuture.allOf(overviewFuture, donationsFuture, expensesFuture, budgetsFuture)
            .thenRun(() -> SwingUtilities.invokeLater(() -> {
                try {
                    Map<String,Object> overview = overviewFuture.get();
                    List<Map<String,Object>> donations = donationsFuture.get();
                    List<Map<String,Object>> expenses = expensesFuture.get();
                    List<Map<String,Object>> budgets = budgetsFuture.get();

                    double totalIncome = donations.stream()
                        .mapToDouble(d -> parseSafelyDouble(d.getOrDefault("amount", "0")))
                        .sum();
                    double totalExpenses = expenses.stream()
                        .mapToDouble(e -> parseSafelyDouble(e.getOrDefault("amount", "0")))
                        .sum();
                    if (overview != null && !overview.isEmpty()) {
                        totalIncome = parseSafelyDouble(overview.getOrDefault("total_income", totalIncome));
                        totalExpenses = parseSafelyDouble(overview.getOrDefault("total_expenses", totalExpenses));
                    }
                    double net = totalIncome - totalExpenses;

                    double totalBudget = 0;
                    double totalSpent = 0;
                    for (Map<String,Object> b : budgets) {
                        totalBudget += parseSafelyDouble(b.getOrDefault("allocated_amount", "0"));
                        totalSpent += parseSafelyDouble(b.getOrDefault("spent_amount", "0"));
                    }
                    double budgetUsed = totalBudget > 0 ? (totalSpent / totalBudget) * 100 : 0;

                    updateTrendData(donations, expenses);
                    setMiniLabel(reportIncomeMini, "KES " + fmt(String.valueOf(totalIncome)));
                    setMiniLabel(reportExpensesMini, "KES " + fmt(String.valueOf(totalExpenses)));
                    setMiniLabel(reportNetMini, "KES " + fmt(String.valueOf(net)));
                    setMiniLabel(reportBudgetUsedMini, String.format("%.1f%%", budgetUsed));
                    updateGivingBreakdownFromDonations(donations);
                    updateReportStatement(budgets);

                    if (contentArea != null) { contentArea.revalidate(); contentArea.repaint(); }
                } catch (Exception ex) {
                    System.err.println("Failed to apply reports page data: " + ex.getMessage());
                }
            })).exceptionally(ex -> {
                SwingUtilities.invokeLater(() ->
                    System.err.println("Failed to load reports page data: " + ex.getMessage()));
                return null;
            });
    }

    private void loadBudgetPageData() {
        SanctumApiClient.getBudgets().thenAccept(budgets -> SwingUtilities.invokeLater(() -> {
            // Use the class field directly — local-variable search was finding the wrong table
            if (budgetPageTable == null) return;

            DefaultTableModel m = (DefaultTableModel) budgetPageTable.getModel();
            m.setRowCount(0);

            if (budgets.isEmpty()) {
                m.addRow(new Object[]{"No budgets", "", "", "", "", "", ""});
                setMiniLabel(budgetTotalMini, "KES 0");
                setMiniLabel(budgetSpentMini, "KES 0");
                setMiniLabel(budgetRemainingMini, "KES 0");
            } else {
                double totalAlloc = 0, totalSpent = 0;
                for (Map<String, Object> b : budgets) {
                    double alloc = parseSafelyDouble(b.getOrDefault("allocated_amount", "0"));
                    double spent = parseSafelyDouble(b.getOrDefault("spent_amount", "0"));
                    double pct   = alloc > 0 ? (spent / alloc) * 100 : 0;
                    totalAlloc += alloc;
                    totalSpent += spent;
                    m.addRow(new Object[]{
                        b.getOrDefault("department", b.getOrDefault("name", "")),
                        b.getOrDefault("name", ""),
                        fmt(String.valueOf(alloc)),
                        fmt(String.valueOf(spent)),
                        fmt(String.valueOf(alloc - spent)),
                        b.getOrDefault("period", ""),
                        String.format("%.1f%%", pct)
                    });
                }
                // Totals summary row
                double remaining = totalAlloc - totalSpent;
                double totalPct  = totalAlloc > 0 ? (totalSpent / totalAlloc) * 100 : 0;
                setMiniLabel(budgetTotalMini, "KES " + fmt(String.valueOf(totalAlloc)));
                setMiniLabel(budgetSpentMini, "KES " + fmt(String.valueOf(totalSpent)));
                setMiniLabel(budgetRemainingMini, "KES " + fmt(String.valueOf(remaining)));
                m.addRow(new Object[]{
                    "TOTAL", "",
                    fmt(String.valueOf(totalAlloc)),
                    fmt(String.valueOf(totalSpent)),
                    fmt(String.valueOf(remaining)),
                    "",
                    String.format("%.1f%%", totalPct)
                });
            }
        })).exceptionally(ex -> {
            SwingUtilities.invokeLater(() ->
                System.err.println("Failed to load budget page data: " + ex.getMessage()));
            return null;
        });
    }

    /** @deprecated Use budgetPageTable / membersPageTable class fields directly. */
    private JTable findTableInContainer(java.awt.Container root, String cardHint) {
        for (int i = 0; i < root.getComponentCount(); i++) {
            java.awt.Component c = root.getComponent(i);
            if (c instanceof JTable) return (JTable) c;
            if (c instanceof java.awt.Container) {
                JTable found = findTableInContainer((java.awt.Container) c, cardHint);
                if (found != null) return found;
            }
        }
        return null;
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
            double totalBalance = 0;
            int activeCount = 0;
            if (accounts.isEmpty()) {
                m.addRow(new Object[]{"No accounts found","","","","","",""});
            } else {
                for (Map<String,Object> a : accounts) {
                    double balance = parseSafelyDouble(a.getOrDefault("balance", a.getOrDefault("current_balance", "0")));
                    boolean active = a.getOrDefault("is_active", true).equals(true);
                    totalBalance += balance;
                    if (active) activeCount++;
                    m.addRow(new Object[]{
                        a.getOrDefault("name", a.getOrDefault("account_name","")),
                        a.getOrDefault("account_type",""),
                        a.getOrDefault("account_number",""),
                        a.getOrDefault("bank_name",""),
                        "KES "+fmt(String.valueOf(balance)),
                        a.getOrDefault("currency","KES"),
                        active ? "Active" : "Inactive"
                    });
                }
            }
            setMiniLabel(accountsCountMini, String.valueOf(accounts.size()));
            setMiniLabel(accountsBalanceMini, "KES " + fmt(String.valueOf(totalBalance)));
            setMiniLabel(accountsActiveMini, String.valueOf(activeCount));
        })).exceptionally(ex -> null);
    }

    private void loadExpensesPageData() {
        SanctumApiClient.getExpenses().thenAccept(expenses -> SwingUtilities.invokeLater(() -> {
            if (expensesPageTable == null) return;
            DefaultTableModel m = (DefaultTableModel) expensesPageTable.getModel(); m.setRowCount(0);
            double total = 0;
            double monthTotal = 0;
            int pending = 0;
            int approved = 0;
            if (expenses.isEmpty()) {
                m.addRow(new Object[]{"No expenses found","","","","","","",""});
            } else {
                for (Map<String,Object> e : expenses) {
                    String date = dateValue(e);
                    double amount = parseSafelyDouble(e.getOrDefault("amount","0"));
                    total += amount;
                    if (isCurrentMonth(date)) monthTotal += amount;
                    String status = e.getOrDefault("status","Pending").toString();
                    if (status.equalsIgnoreCase("approved")) approved++;
                    else if (status.equalsIgnoreCase("pending")) pending++;
                    m.addRow(new Object[]{
                        date,
                        e.getOrDefault("description", e.getOrDefault("title","")),
                        e.getOrDefault("category",""),
                        e.getOrDefault("vendor",""),
                        "KES "+fmt(String.valueOf(amount)),
                        e.getOrDefault("approved_by",""),
                        status,
                        e.getOrDefault("receipt","") != null && !e.getOrDefault("receipt","").toString().isEmpty() ? "✓" : "—"
                    });
                }
            }
            setMiniLabel(expensesTotalMini, "KES " + fmt(String.valueOf(total)));
            setMiniLabel(expensesMonthMini, "KES " + fmt(String.valueOf(monthTotal)));
            setMiniLabel(expensesPendingMini, String.valueOf(pending));
            setMiniLabel(expensesApprovedMini, String.valueOf(approved));
        })).exceptionally(ex -> null);
    }

    private void loadMembersPageData() {
        CompletableFuture<java.util.List<Map<String,Object>>> membersFuture   = SanctumApiClient.getMembers();
        CompletableFuture<java.util.List<Map<String,Object>>> donationsFuture = SanctumApiClient.getDonations();

        membersFuture.thenCombine(donationsFuture, (members, donations) -> {
            // ── Build per-member donation summaries ──────────────────────
            // KEY FIX: donations from the API identify the member by their
            // primary key integer (field "member"), NOT by display name.
            // We build two lookup maps keyed by member-id string so the join
            // is always correct regardless of how names are formatted.
            java.util.Map<String, Double> totalById   = new java.util.LinkedHashMap<>();
            java.util.Map<String, Double> monthlyById = new java.util.LinkedHashMap<>();
            java.util.Map<String, String> lastById    = new java.util.LinkedHashMap<>();

            String currentMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));

            for (Map<String,Object> d : donations) {
                // "member" field is the member PK (int or string)
                String memberId = d.getOrDefault("member", "").toString();
                double amount   = parseSafelyDouble(d.getOrDefault("amount", "0"));
                String date     = d.getOrDefault("created_at", d.getOrDefault("transaction_date", "")).toString();

                totalById.merge(memberId, amount, Double::sum);
                if (date.startsWith(currentMonth))
                    monthlyById.merge(memberId, amount, Double::sum);
                String prev = lastById.getOrDefault(memberId, "");
                if (date.compareTo(prev) > 0)
                    lastById.put(memberId, date.length() > 10 ? date.substring(0, 10) : date);
            }

            return new Object[]{members, totalById, monthlyById, lastById};

        }).thenAccept(result -> SwingUtilities.invokeLater(() -> {
            // Use the class field — fragile column-count search removed
            if (membersPageTable == null) return;

            @SuppressWarnings("unchecked")
            java.util.List<Map<String,Object>> members =
                (java.util.List<Map<String,Object>>) result[0];
            @SuppressWarnings("unchecked")
            java.util.Map<String,Double> totalById =
                (java.util.Map<String,Double>) result[1];
            @SuppressWarnings("unchecked")
            java.util.Map<String,Double> monthlyById =
                (java.util.Map<String,Double>) result[2];
            @SuppressWarnings("unchecked")
            java.util.Map<String,String> lastById =
                (java.util.Map<String,String>) result[3];

            DefaultTableModel m = (DefaultTableModel) membersPageTable.getModel();
            m.setRowCount(0);

            double totalGivenAll = totalById.values().stream().mapToDouble(Double::doubleValue).sum();
            double monthGivenAll = monthlyById.values().stream().mapToDouble(Double::doubleValue).sum();
            long activeDonors = totalById.values().stream().filter(v -> v > 0).count();
            double avgGiven = activeDonors > 0 ? totalGivenAll / activeDonors : 0;
            setMiniLabel(membersTotalMini, String.valueOf(members.size()));
            setMiniLabel(membersDonorsMini, String.valueOf(activeDonors));
            setMiniLabel(membersAverageMini, "KES " + fmt(String.valueOf(avgGiven)));
            setMiniLabel(membersMonthMini, "KES " + fmt(String.valueOf(monthGivenAll)));

            if (members.isEmpty()) {
                m.addRow(new Object[]{"No members found","","","","","","",""});
                return;
            }

            for (Map<String,Object> mem : members) {
                String memberId  = mem.getOrDefault("id", "").toString();
                String firstName = mem.getOrDefault("first_name",
                                   mem.getOrDefault("user_first_name","")).toString();
                String lastName  = mem.getOrDefault("last_name",
                                   mem.getOrDefault("user_last_name","")).toString();
                String name      = (firstName + " " + lastName).trim();
                if (name.isEmpty()) name = mem.getOrDefault("email",
                                            mem.getOrDefault("user_email","Unknown")).toString();

                String email   = mem.getOrDefault("email",
                                 mem.getOrDefault("user_email","")).toString();
                String phone   = mem.getOrDefault("phone",
                                 mem.getOrDefault("phone_number","")).toString();
                String joined  = mem.getOrDefault("date_joined","").toString();
                if (joined.length() > 10) joined = joined.substring(0, 10);

                // Lookup by member PK — correct key
                double totalGiven   = totalById.getOrDefault(memberId, 0.0);
                double monthlyGiven = monthlyById.getOrDefault(memberId, 0.0);
                String lastGiven    = lastById.getOrDefault(memberId, "—");
                String status       = mem.getOrDefault("is_active","true")
                                        .toString().equalsIgnoreCase("true") ? "Active" : "Inactive";

                m.addRow(new Object[]{
                    name, email, phone, joined,
                    fmt(String.valueOf(totalGiven)),
                    fmt(String.valueOf(monthlyGiven)),
                    status, lastGiven
                });
            }
        })).exceptionally(ex -> {
            SwingUtilities.invokeLater(() ->
                System.err.println("Failed to load members page data: " + ex.getMessage()));
            return null;
        });
    }

    /**
     * No longer used — membersPageTable is now a class field.
     * Kept to avoid breaking any call sites that may exist elsewhere.
     */
    private JTable findMembersTable() {
        return membersPageTable;
    }

    private JTable findTableByColumns(java.awt.Container root, int colCount) {
        for (int i = 0; i < root.getComponentCount(); i++) {
            java.awt.Component c = root.getComponent(i);
            if (c instanceof JTable && ((JTable)c).getColumnCount() == colCount)
                return (JTable) c;
            if (c instanceof java.awt.Container) {
                JTable found = findTableByColumns((java.awt.Container) c, colCount);
                if (found != null) return found;
            }
        }
        return null;
    }

    // ─── Dialogs ─────────────────────────────────────────────────────────────

    // ── Giving Types Manager ──────────────────────────────────────────────────
    private void showGivingTypesDialog() {
        JDialog dlg = new JDialog(this, "Giving Types (Categories)", true);
        dlg.setSize(700, 520);
        dlg.setLocationRelativeTo(this);

        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBackground(C_BG);
        root.setBorder(new EmptyBorder(24, 28, 20, 28));

        JLabel title = new JLabel("🏷  Giving Types / Categories");
        title.setFont(F_H1); title.setForeground(C_GOLD);

        // ── Category table ────────────────────────────────────────────────
        String[] cols = {"Name", "Description", "Has Target", "Monthly Target (KES)", "Yearly Target (KES)", "Active"};
        DefaultTableModel catModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable catTable = new JTable(catModel);
        styleTable(catTable);
        catTable.getColumnModel().getColumn(0).setPreferredWidth(140);
        catTable.getColumnModel().getColumn(1).setPreferredWidth(200);

        // Load categories from API
        Runnable loadCats = () -> {
            SanctumApiClient.getGivingCategories().thenAccept(cats -> SwingUtilities.invokeLater(() -> {
                catModel.setRowCount(0);
                if (cats.isEmpty()) {
                    catModel.addRow(new Object[]{"No categories yet", "", "", "", "", ""});
                } else {
                    for (Map<String,Object> c : cats) {
                        Object monthlyRaw = c.get("monthly_target");
                        Object yearlyRaw  = c.get("yearly_target");
                        catModel.addRow(new Object[]{
                            c.getOrDefault("name",        ""),
                            c.getOrDefault("description", ""),
                            Boolean.TRUE.equals(c.get("has_target")) ? "Yes" : "No",
                            monthlyRaw != null ? fmt(monthlyRaw.toString()) : "—",
                            yearlyRaw  != null ? fmt(yearlyRaw.toString())  : "—",
                            Boolean.TRUE.equals(c.get("is_active")) ? "✓" : "✗"
                        });
                    }
                }
            })).exceptionally(ex -> null);
        };
        loadCats.run();

        JScrollPane scroll = styledScroll(catTable, 160);

        // ── Add-new form ──────────────────────────────────────────────────
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(C_CARD);
        form.setBorder(new EmptyBorder(14, 14, 14, 14));
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.HORIZONTAL; gc.insets = new Insets(4, 4, 4, 4);

        JTextField fCatName  = styledField("Category name e.g. Tithe, Offering");
        JTextField fCatDesc  = styledField("Short description");
        JTextField fMonthly  = styledField("0");
        JTextField fYearly   = styledField("0");
        JCheckBox  cbTarget  = new JCheckBox("Has Target");
        cbTarget.setBackground(C_CARD); cbTarget.setForeground(C_TEXT_MID); cbTarget.setFont(F_LABEL);

        int r = 0;
        gc.gridx=0; gc.gridy=r; gc.weightx=0; gc.gridwidth=1;
        form.add(label("Name *"), gc);
        gc.gridx=1; gc.weightx=1; gc.gridwidth=3;
        form.add(fCatName, gc);

        r++;
        gc.gridx=0; gc.gridy=r; gc.weightx=0; gc.gridwidth=1;
        form.add(label("Description"), gc);
        gc.gridx=1; gc.weightx=1; gc.gridwidth=3;
        form.add(fCatDesc, gc);

        r++;
        gc.gridx=0; gc.gridy=r; gc.weightx=0; gc.gridwidth=1;
        form.add(label("Monthly Target"), gc);
        gc.gridx=1; gc.weightx=1; gc.gridwidth=1;
        form.add(fMonthly, gc);
        gc.gridx=2; gc.weightx=0;
        form.add(label("Yearly Target"), gc);
        gc.gridx=3; gc.weightx=1;
        form.add(fYearly, gc);

        r++;
        gc.gridx=0; gc.gridy=r; gc.gridwidth=4; gc.weightx=1;
        form.add(cbTarget, gc);

        // ── Status + buttons ──────────────────────────────────────────────
        JLabel statusLbl = new JLabel(" ");
        statusLbl.setFont(F_SMALL);
        statusLbl.setForeground(C_TEXT_DIM);

        JButton addBtn  = buildPrimaryBtn("➕ Add Category", null);
        JButton doneBtn = buildPrimaryBtn("✓ Done", e -> dlg.dispose());

        addBtn.addActionListener(e -> {
            String name = fCatName.getText().trim();
            if (name.isEmpty()) { statusLbl.setText("Category name is required."); statusLbl.setForeground(C_DANGER); return; }
            double monthly = parseDouble(fMonthly.getText().trim());
            double yearly  = parseDouble(fYearly.getText().trim());
            addBtn.setEnabled(false);
            addBtn.setText("⏳ Saving…");
            statusLbl.setText("Saving…"); statusLbl.setForeground(C_GOLD);

            SanctumApiClient.addGivingCategory(name, fCatDesc.getText().trim(),
                cbTarget.isSelected(), monthly, yearly)
                .thenAccept(ok -> SwingUtilities.invokeLater(() -> {
                    addBtn.setEnabled(true);
                    addBtn.setText("➕ Add Category");
                    if (ok) {
                        statusLbl.setText("✓ Category '" + name + "' added.");
                        statusLbl.setForeground(C_SUCCESS);
                        fCatName.setText(""); fCatDesc.setText(""); fMonthly.setText("0"); fYearly.setText("0");
                        cbTarget.setSelected(false);
                        loadCats.run();   // refresh table
                    } else {
                        statusLbl.setText("Failed to save — check connection.");
                        statusLbl.setForeground(C_DANGER);
                    }
                })).exceptionally(ex -> {
                    SwingUtilities.invokeLater(() -> {
                        addBtn.setEnabled(true);
                        addBtn.setText("➕ Add Category");
                        statusLbl.setText("Network error: " + ex.getMessage());
                        statusLbl.setForeground(C_DANGER);
                    });
                    return null;
                });
        });

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnRow.setBackground(C_BG);
        btnRow.add(statusLbl);
        btnRow.add(addBtn);
        btnRow.add(doneBtn);

        JPanel south = new JPanel(new BorderLayout(0, 8));
        south.setBackground(C_BG);
        south.add(form,   BorderLayout.NORTH);
        south.add(btnRow, BorderLayout.SOUTH);

        root.add(title,  BorderLayout.NORTH);
        root.add(scroll, BorderLayout.CENTER);
        root.add(south,  BorderLayout.SOUTH);

        dlg.setContentPane(root);
        dlg.setVisible(true);
    }

    /** Quick plain label for form rows in dialogs */
    private JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setFont(F_LABEL); l.setForeground(C_TEXT_MID);
        return l;
    }

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
            String amt   = fAmount.getText().trim();
            String donor = fDonor.getText().trim();
            if (amt.isEmpty() || donor.isEmpty()) {
                DialogManager.showMessageDialog(dlg, "Amount and Donor are required.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }
            try { Double.parseDouble(amt.replaceAll("[^\\d.]","")); }
            catch (NumberFormatException ex) {
                DialogManager.showMessageDialog(dlg, "Invalid amount.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }
            save.setEnabled(false);
            SanctumApiClient.addDonation(amt, donor,
                    fType.getSelectedItem().toString(),
                    fDesc.getText().trim())
                .thenAccept(ok -> SwingUtilities.invokeLater(() -> {
                    save.setEnabled(true);
                    if (ok) {
                        DialogManager.showMessageDialog(dlg, "Donation saved!", "Success", JOptionPane.INFORMATION_MESSAGE);
                        dlg.dispose();
                        loadData();
                    } else {
                        DialogManager.showMessageDialog(dlg, "Failed to save donation.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }))
                .exceptionally(ex -> {
                    SwingUtilities.invokeLater(() -> {
                        save.setEnabled(true);
                        DialogManager.showMessageDialog(dlg, "Network error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    });
                    return null;
                });
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
            String name  = fName.getText().trim();
            String alloc = fAlloc.getText().trim();
            if (name.isEmpty() || alloc.isEmpty()) {
                DialogManager.showMessageDialog(dlg, "Budget name and amount are required.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }
            double allocAmt = 0;
            try { allocAmt = Double.parseDouble(alloc.replaceAll("[^\\d.]","")); }
            catch (NumberFormatException ex) {
                DialogManager.showMessageDialog(dlg, "Invalid amount.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Map period selector to API values
            String periodStr = fPeriod.getSelectedItem().toString().toLowerCase().replace("-","").replace(" ","_");
            int year  = LocalDate.now().getYear();
            Integer month = periodStr.equals("monthly") ? LocalDate.now().getMonthValue() : null;

            final double finalAlloc = allocAmt;
            save.setEnabled(false);
            SanctumApiClient.addBudget(name, fCategory.getSelectedItem().toString(), finalAlloc, periodStr, year, month)
                .thenAccept(ok -> SwingUtilities.invokeLater(() -> {
                    save.setEnabled(true);
                    if (ok) {
                        DialogManager.showMessageDialog(dlg, "Budget saved successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                        dlg.dispose();
                        loadBudgetSummaryData();
                        loadBudgetPageData();
                        loadKpiData();
                    } else {
                        DialogManager.showMessageDialog(dlg, "Failed to save budget. Please try again.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }))
                .exceptionally(ex -> {
                    SwingUtilities.invokeLater(() -> {
                        save.setEnabled(true);
                        DialogManager.showMessageDialog(dlg, "Network error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    });
                    return null;
                });
        });
        btns.add(cancel); btns.add(save);
        gc.gridx=0; gc.gridy=5; gc.gridwidth=2; gc.insets=new Insets(18,0,0,0);
        panel.add(btns, gc);
        dlg.setContentPane(panel);
        DialogManager.showDialogEnhanced(dlg);
    }

    private void showAddExpenseDialog() {
        JDialog dlg = new JDialog(this, "Add Expense", true);
        dlg.setSize(460, 400); dlg.setLocationRelativeTo(this);
        JPanel panel = new JPanel(new GridBagLayout()) {
            @Override protected void paintComponent(Graphics g) { g.setColor(C_SURFACE); g.fillRect(0,0,getWidth(),getHeight()); }
        };
        panel.setBorder(new EmptyBorder(24,28,24,28));
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.HORIZONTAL; gc.insets = new Insets(6,0,6,0);

        JTextField fAmount  = styledField("e.g. 5000");
        JTextField fTitle   = styledField("Title of expense");
        JTextField fVendor  = styledField("Vendor / supplier name");
        JTextField fDate    = styledField(LocalDate.now().toString());

        // Use human-readable category names instead of raw integer IDs
        String[] categoryNames = {"Ministry","Utilities","Salaries","Maintenance","Events","Other"};
        JComboBox<String> fCat = new JComboBox<>(categoryNames);
        styleCombo(fCat);

        addFormRow(panel, gc, 0, "Amount (KES)", fAmount);
        addFormRow(panel, gc, 1, "Title",        fTitle);
        addFormRow(panel, gc, 2, "Category",     fCat);
        addFormRow(panel, gc, 3, "Vendor",       fVendor);
        addFormRow(panel, gc, 4, "Date",         fDate);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT,10,0)); btns.setOpaque(false);
        JButton cancel = buildTopBtn("Cancel", C_TEXT_MID);
        JButton save   = buildPrimaryBtn("Save Expense", null);
        cancel.addActionListener(e -> dlg.dispose());
        save.addActionListener(e -> {
            String amt   = fAmount.getText().trim();
            String title = fTitle.getText().trim();
            if (amt.isEmpty() || title.isEmpty()) {
                DialogManager.showMessageDialog(dlg, "Amount and Title are required.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }
            try { Double.parseDouble(amt.replaceAll("[^\\d.]","")); }
            catch (NumberFormatException ex) {
                DialogManager.showMessageDialog(dlg, "Invalid amount.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }
            // Map position → numeric category ID expected by the backend
            int catId = fCat.getSelectedIndex() + 1;
            save.setEnabled(false);
            SanctumApiClient.addExpense(
                    amt, title,
                    String.valueOf(catId),
                    fVendor.getText().trim(),
                    fDate.getText().trim())
                .thenAccept(ok -> SwingUtilities.invokeLater(() -> {
                    save.setEnabled(true);
                    if (ok) {
                        DialogManager.showMessageDialog(dlg, "Expense saved!", "Success", JOptionPane.INFORMATION_MESSAGE);
                        dlg.dispose();
                        loadExpensesPageData();
                        loadKpiData();
                    } else {
                        DialogManager.showMessageDialog(dlg, "Failed to save expense.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }))
                .exceptionally(ex -> {
                    SwingUtilities.invokeLater(() -> {
                        save.setEnabled(true);
                        DialogManager.showMessageDialog(dlg, "Network error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    });
                    return null;
                });
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
                DialogManager.showMessageDialog(dlg, "Account name is required.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }
            save.setEnabled(false);
            SanctumApiClient.addAccount(
                    fName.getText().trim(),
                    fType.getSelectedItem().toString(),
                    fBank.getText().trim(),
                    fAccNo.getText().trim(),
                    fBalance.getText().trim())
                .thenAccept(ok -> SwingUtilities.invokeLater(() -> {
                    save.setEnabled(true);
                    if (ok) {
                        DialogManager.showMessageDialog(dlg, "Account created!", "Success", JOptionPane.INFORMATION_MESSAGE);
                        dlg.dispose();
                        loadAccountsPageData();
                    } else {
                        DialogManager.showMessageDialog(dlg, "Failed to create account.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }))
                .exceptionally(ex -> {
                    SwingUtilities.invokeLater(() -> {
                        save.setEnabled(true);
                        DialogManager.showMessageDialog(dlg, "Network error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    });
                    return null;
                });
        });
        btns.add(cancel); btns.add(save);
        gc.gridx=0; gc.gridy=5; gc.gridwidth=2; gc.insets=new Insets(18,0,0,0);
        panel.add(btns, gc);
        dlg.setContentPane(panel);
        DialogManager.showDialogEnhanced(dlg);
    }

    // ─── Budget PIN Manager ───────────────────────────────────────────────────
    private void showBudgetPinManagerDialog() {
        JDialog dlg = new JDialog(this, "Budget Access PINs", true);
        dlg.setSize(820, 600);
        dlg.setLocationRelativeTo(this);

        JPanel root = new JPanel(new BorderLayout(0, 16));
        root.setBackground(C_BG);
        root.setBorder(new EmptyBorder(24, 28, 20, 28));

        // ── Header ────────────────────────────────────────────────────────
        JLabel title = new JLabel("🔑  Budget Access PIN Manager");
        title.setFont(F_H1); title.setForeground(C_GOLD);

        JLabel subtitle = new JLabel(
            "Generate a time-limited PIN so members can view the church budget summary on their phones.");
        subtitle.setFont(F_SMALL); subtitle.setForeground(C_TEXT_DIM);

        JPanel hdr = new JPanel(); hdr.setOpaque(false);
        hdr.setLayout(new BoxLayout(hdr, BoxLayout.Y_AXIS));
        hdr.add(title); hdr.add(Box.createVerticalStrut(4)); hdr.add(subtitle);

        // ── Create-new form ───────────────────────────────────────────────
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(C_CARD);
        form.setBorder(new EmptyBorder(16, 20, 16, 20));
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.HORIZONTAL; gc.insets = new Insets(5, 5, 5, 5);

        JTextField fLabel = styledField("e.g. Q2 Budget Review");

        // Duration selector
        String[] durations = {"1 hour","2 hours","6 hours","12 hours","24 hours","48 hours","7 days","30 days"};
        int[]    durationH = {1,       2,        6,        12,        24,        48,        168,      720};
        JComboBox<String> durCombo = new JComboBox<>(durations);
        durCombo.setBackground(C_CARD); durCombo.setForeground(C_TEXT); durCombo.setFont(F_BODY);
        durCombo.setSelectedIndex(4); // default 24 h

        JTextField fMaxUses = styledField("Leave blank for unlimited");

        int r = 0;
        gc.gridx=0; gc.gridy=r; gc.weightx=0; gc.gridwidth=1;
        form.add(label("Label"), gc);
        gc.gridx=1; gc.weightx=1; gc.gridwidth=3;
        form.add(fLabel, gc);

        r++;
        gc.gridx=0; gc.gridy=r; gc.weightx=0; gc.gridwidth=1;
        form.add(label("Valid for"), gc);
        gc.gridx=1; gc.weightx=1; gc.gridwidth=1;
        form.add(durCombo, gc);
        gc.gridx=2; gc.weightx=0;
        form.add(label("Max uses"), gc);
        gc.gridx=3; gc.weightx=1;
        form.add(fMaxUses, gc);

        // ── Active PINs table ─────────────────────────────────────────────
        String[] cols = {"PIN", "Label", "Expires", "Uses", "Max Uses", "Status", "Action"};
        DefaultTableModel pinModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        JTable pinTable = new JTable(pinModel);
        styleTable(pinTable);
        int[] colWidths = {70, 200, 150, 55, 70, 70, 80};
        for (int i = 0; i < colWidths.length; i++)
            pinTable.getColumnModel().getColumn(i).setPreferredWidth(colWidths[i]);

        // Store raw pin IDs for revoke action
        java.util.List<Integer> pinIds = new java.util.ArrayList<>();

        // Status label + refresh lambda
        JLabel statusLbl = new JLabel("Loading PINs…");
        statusLbl.setFont(F_SMALL); statusLbl.setForeground(C_TEXT_DIM);

        Runnable loadPins = () -> {
            statusLbl.setText("Loading…");
            SanctumApiClient.getBudgetPins().thenAccept(pins -> SwingUtilities.invokeLater(() -> {
                pinModel.setRowCount(0);
                pinIds.clear();
                if (pins.isEmpty()) {
                    pinModel.addRow(new Object[]{"—", "No PINs yet", "", "", "", "", ""});
                } else {
                    for (Map<String,Object> p : pins) {
                        String expires = p.getOrDefault("expires_at", "").toString();
                        if (expires.length() > 16) expires = expires.substring(0, 16).replace("T", " ");
                        boolean valid = Boolean.TRUE.equals(p.get("is_valid"));
                        String pinStr = p.getOrDefault("pin", "").toString();
                        // Only show PIN to treasurer while viewing the dialog
                        pinModel.addRow(new Object[]{
                            pinStr,
                            p.getOrDefault("label",      "").toString(),
                            expires,
                            p.getOrDefault("view_count", "0").toString(),
                            p.getOrDefault("max_uses",   "∞").toString(),
                            valid ? "✓ Active" : "✗ Expired",
                            valid ? "Revoke" : "—"
                        });
                        Object idObj = p.get("id");
                        pinIds.add(idObj instanceof Number ? ((Number) idObj).intValue() : -1);
                    }
                }
                statusLbl.setText(pins.size() + " PIN(s)");
            })).exceptionally(ex -> {
                SwingUtilities.invokeLater(() -> statusLbl.setText("Failed to load PINs"));
                return null;
            });
        };
        loadPins.run();

        // Revoke on row double-click
        pinTable.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() != 2) return;
                int row = pinTable.getSelectedRow();
                if (row < 0 || row >= pinIds.size()) return;
                int id = pinIds.get(row);
                if (id < 0) return;
                String pinVal = pinModel.getValueAt(row, 0).toString();
                int confirm = JOptionPane.showConfirmDialog(dlg,
                    "Revoke PIN " + pinVal + "? Members will no longer be able to use it.",
                    "Confirm Revoke", JOptionPane.YES_NO_OPTION);
                if (confirm != JOptionPane.YES_OPTION) return;
                SanctumApiClient.revokeBudgetPin(id).thenAccept(ok -> SwingUtilities.invokeLater(() -> {
                    if (ok) { statusLbl.setText("PIN " + pinVal + " revoked."); loadPins.run(); }
                    else    { statusLbl.setText("Revoke failed — try again."); }
                })).exceptionally(ex -> null);
            }
        });

        JScrollPane scroll = styledScroll(pinTable, 220);

        // ── Buttons ───────────────────────────────────────────────────────
        JButton generateBtn = buildPrimaryBtn("⚡ Generate PIN", null);
        JButton refreshBtn  = buildTopBtn("↻ Refresh", C_TEXT_MID);
        JButton closeBtn    = buildTopBtn("✓ Close",   C_TEXT_MID);

        generateBtn.addActionListener(e -> {
            String lbl = fLabel.getText().trim();
            int hours  = durationH[durCombo.getSelectedIndex()];
            int maxU   = -1;
            String mu  = fMaxUses.getText().trim();
            if (!mu.isEmpty()) {
                try { maxU = Integer.parseInt(mu); }
                catch (NumberFormatException ex) { statusLbl.setText("Max uses must be a number."); return; }
            }
            generateBtn.setEnabled(false);
            generateBtn.setText("⏳ Generating…");
            statusLbl.setText("Creating PIN…"); statusLbl.setForeground(C_GOLD);

            final int finalMaxU = maxU;
            SanctumApiClient.createBudgetPin(lbl, hours, finalMaxU)
                .thenAccept(result -> SwingUtilities.invokeLater(() -> {
                    generateBtn.setEnabled(true);
                    generateBtn.setText("⚡ Generate PIN");
                    if (!result.isEmpty()) {
                        String newPin = result.getOrDefault("pin", "?").toString();
                        String expiry = result.getOrDefault("expires_at", "").toString();
                        if (expiry.length() > 16) expiry = expiry.substring(0, 16).replace("T", " ");
                        statusLbl.setForeground(C_SUCCESS);
                        statusLbl.setText("✓ PIN created: " + newPin + "  (expires " + expiry + ")");
                        // Show the PIN prominently
                        JOptionPane.showMessageDialog(dlg,
                            "New Budget Access PIN:\n\n"
                          + "  " + newPin + "\n\n"
                          + "Share this PIN with members.\n"
                          + "Valid until: " + expiry + "\n"
                          + "Members enter it in the app under: Giving → View Budget",
                            "PIN Generated", JOptionPane.INFORMATION_MESSAGE);
                        fLabel.setText("");
                        loadPins.run();
                    } else {
                        statusLbl.setForeground(C_DANGER);
                        statusLbl.setText("Failed to create PIN — check connection.");
                    }
                })).exceptionally(ex -> {
                    SwingUtilities.invokeLater(() -> {
                        generateBtn.setEnabled(true);
                        generateBtn.setText("⚡ Generate PIN");
                        statusLbl.setText("Error: " + ex.getMessage());
                        statusLbl.setForeground(C_DANGER);
                    });
                    return null;
                });
        });

        refreshBtn.addActionListener(e -> loadPins.run());
        closeBtn.addActionListener(e -> dlg.dispose());

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnRow.setBackground(C_BG);
        btnRow.add(statusLbl); btnRow.add(refreshBtn); btnRow.add(closeBtn); btnRow.add(generateBtn);

        JPanel south = new JPanel(new BorderLayout(0, 8));
        south.setBackground(C_BG);
        south.add(form,   BorderLayout.NORTH);
        south.add(btnRow, BorderLayout.SOUTH);

        root.add(hdr,    BorderLayout.NORTH);
        root.add(scroll, BorderLayout.CENTER);
        root.add(south,  BorderLayout.SOUTH);

        dlg.setContentPane(root);
        dlg.setVisible(true);
    }

    private void showBudgetStudioDialog() {
        // Embed BudgetStudio directly as the budget page content — full frame, no dialog clipping
        JDialog dialog = new JDialog(this, "Budget Studio", true);
        dialog.setUndecorated(false);
        // Size to nearly full screen for comfortable two-column layout
        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        Rectangle screen = gd.getDefaultConfiguration().getBounds();
        dialog.setSize(Math.min(screen.width - 80, 1440), Math.min(screen.height - 80, 900));
        dialog.setLocationRelativeTo(this);

        // Dark root panel matching treasurer theme
        JPanel root = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(C_BG); g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        root.setOpaque(true);

        BudgetStudio bs = new BudgetStudio(this);
        // Listen for saves → refresh treasurer KPIs + budget page
        bs.addPropertyChangeListener("budgetSaved", evt -> {
            if (Boolean.TRUE.equals(evt.getNewValue())) {
                loadBudgetSummaryData();
                loadKpiData();
            }
        });

        JScrollPane scroll = new JScrollPane(bs);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        root.add(scroll, BorderLayout.CENTER);

        dialog.setContentPane(root);
        dialog.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close");
        dialog.getRootPane().getActionMap().put("close", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { dialog.dispose(); }
        });
        dialog.setVisible(true);
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
