package com.sanctum.view;

import com.sanctum.api.SanctumApiClient;
import com.sanctum.auth.SessionManager;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

/**
 * System Admin (Super Admin) Dashboard
 * Deep-indigo + vibrant accent theme.
 * Pages: Overview · Churches · Users · Financials · Audit Logs · Settings
 */
public class SystemAdminDashboardFrame extends JFrame {

    // ── Palette ───────────────────────────────────────────────────────
  // ── Palette ───────────────────────────────────────────────────────
static final Color C_BG      = new Color(  8,  12,  22);  // deep navy background
static final Color C_SURFACE = new Color( 16,  22,  38);  // surface panels
static final Color C_CARD    = new Color( 24,  31,  52);  // cards
static final Color C_CARD2   = new Color( 32,  41,  68);  // elevated cards
static final Color C_BORDER  = new Color( 52,  65, 100);  // subtle borders

static final Color C_ACCENT  = new Color( 59, 130, 246);  // primary blue
static final Color C_PURPLE  = new Color(139,  92, 246);  // vibrant purple
static final Color C_GOLD    = new Color(245, 158,  11);  // amber gold
static final Color C_GREEN   = new Color( 16, 185, 129);  // emerald green
static final Color C_RED     = new Color(239,  68,  68);  // modern red
static final Color C_ORANGE  = new Color(249, 115,  22);  // orange
static final Color C_CYAN    = new Color(  6, 182, 212);  // cyan
static final Color C_PINK    = new Color(236,  72, 153);  // pink

static final Color C_TEXT    = new Color(241, 245, 249);  // almost white
static final Color C_DIM     = new Color(148, 163, 184);  // muted text
static final Color C_SIDEBAR = new Color( 12,  17,  32);  // sidebar

    // ── Fonts ─────────────────────────────────────────────────────────
    static final Font F_TITLE  = new Font("Segoe UI", Font.BOLD,  28);
    static final Font F_HEAD   = new Font("Segoe UI", Font.BOLD,  16);
    static final Font F_LABEL  = new Font("Segoe UI", Font.BOLD,  13);
    static final Font F_BODY   = new Font("Segoe UI", Font.PLAIN, 12);
    static final Font F_MONO   = new Font("JetBrains Mono", Font.PLAIN, 11);
    static final Font F_MONO_B = new Font("JetBrains Mono", Font.BOLD,  11);
    static final Font F_NUM    = new Font("Segoe UI", Font.BOLD,  24);

    // ── Nav items ─────────────────────────────────────────────────────
    private static final Object[][] NAV = {
        {"\uD83D\uDCCA", "Overview",   C_ACCENT},
        {"\u26EA",       "Churches",   C_CYAN},
        {"\uD83D\uDC65", "Users",      C_PURPLE},
        {"\uD83D\uDCB0", "Financials", C_GOLD},
        {"\uD83D\uDCD3", "Audit Logs", C_ORANGE},
        {"\u2699\uFE0F", "Settings",   C_DIM},
    };

    private JPanel contentArea;
    private String activeMenu = "Overview";
    private JPanel sidebarPanel;

    // ── Stat labels updated by loadOverview() ─────────────────────────
    private JLabel lblChurches, lblPending, lblActive, lblMembers, lblGiving, lblNet;

    // ══════════════════════════════════════════════════════════════════
    public SystemAdminDashboardFrame() {
        configureWindow();
        buildUI();
        loadOverview();
    }

    private void configureWindow() {
        setTitle("Sanctum \u2014 System Administration");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setUndecorated(true);

        // ── Set window icon (shows in taskbar / Alt-Tab) ──────────────
        // Try custom logo first, then fall back to icon.png
        java.awt.Image windowIcon = null;
        String[] iconPaths = {"/images/logo.png", "/images/icon.png", "/images/sanctum-logo.png"};
        for (String path : iconPaths) {
            try {
                java.io.InputStream is = getClass().getResourceAsStream(path);
                if (is != null) {
                    windowIcon = javax.imageio.ImageIO.read(is);
                    is.close();
                    if (windowIcon != null) { System.out.println("Window icon loaded: " + path); break; }
                }
            } catch (Exception ignored) {}
        }
        if (windowIcon != null) {
            // Provide multiple sizes for best quality across DPI settings
            java.util.List<java.awt.Image> icons = new java.util.ArrayList<>();
            for (int sz : new int[]{16, 32, 64, 128}) {
                icons.add(windowIcon.getScaledInstance(sz, sz, java.awt.Image.SCALE_SMOOTH));
            }
            setIconImages(icons);
        }

        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        Rectangle b = gd.getDefaultConfiguration().getBounds();
        setSize(b.width - 60, b.height - 60);
        setLocationRelativeTo(null);
        getContentPane().setBackground(C_BG);
    }

    // ── UI shell ──────────────────────────────────────────────────────
    private void buildUI() {
        setLayout(new BorderLayout());
        JPanel root = bg(C_BG, new BorderLayout());
        root.add(buildTopBar(),  BorderLayout.NORTH);
        sidebarPanel = buildSidebar();
        root.add(sidebarPanel, BorderLayout.WEST);
        contentArea = bg(C_BG, new CardLayout());
        contentArea.add(buildOverview(),   "Overview");
        contentArea.add(buildChurches(),   "Churches");
        contentArea.add(buildUsers(),      "Users");
        contentArea.add(buildFinancials(), "Financials");
        contentArea.add(buildAuditLogs(),  "Audit Logs");
        contentArea.add(buildSettings(),   "Settings");
        root.add(contentArea, BorderLayout.CENTER);
        add(root);
    }

    // ── Top bar ───────────────────────────────────────────────────────
    private JPanel buildTopBar() {
        JPanel bar = bg(C_SURFACE, new BorderLayout());
        bar.setPreferredSize(new Dimension(0, 54));
        bar.setBorder(new EmptyBorder(8, 20, 8, 20));

        // Left: logo + brand
        JPanel left = bg(C_SURFACE, new FlowLayout(FlowLayout.LEFT, 10, 0));

        // Use shared LogoLoader — reads logo.png (or fallback) from resources/images/
        JLabel logoLabel = com.sanctum.util.LogoLoader.createLogoLabel(new Dimension(34, 34));
        left.add(logoLabel);

        JLabel brand = new JLabel("Sanctum  \u00B7  System Administration");
        brand.setFont(F_LABEL); brand.setForeground(C_ACCENT);
        left.add(brand);

        // Right: user info + controls
        JPanel right = bg(C_SURFACE, new FlowLayout(FlowLayout.RIGHT, 10, 2));
        Map<String,Object> ud = SanctumApiClient.getCurrentUserData();
        String name = ud != null ? ud.getOrDefault("first_name","Admin") + " " + ud.getOrDefault("last_name","") : "Admin";
        JLabel uLbl = new JLabel("\uD83D\uDEE1\uFE0F  " + name.trim() + "  |  System Admin");
        uLbl.setFont(F_MONO); uLbl.setForeground(C_DIM);

        JButton pwBtn   = topCtrl("\uD83D\uDD11", "Change password", C_GOLD);
        JButton minBtn  = topCtrl("\u2014", "Minimise", C_DIM);
        JButton xBtn    = topCtrl("\u00D7", "Logout", C_RED);
        pwBtn.addActionListener(e -> ChangePasswordDialog.show(this));
        minBtn.addActionListener(e -> setState(JFrame.ICONIFIED));
        xBtn.addActionListener(e -> {
            int ok = JOptionPane.showConfirmDialog(this,"Logout from Sanctum?","Logout",JOptionPane.YES_NO_OPTION);
            if(ok==JOptionPane.YES_OPTION){
                SanctumApiClient.setAuthToken(null);
                SessionManager.getInstance().clearSession();
                dispose();
                new LoginFrame().setVisible(true);
            }
        });
        right.add(uLbl); right.add(pwBtn); right.add(minBtn); right.add(xBtn);
        bar.add(left, BorderLayout.WEST); bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private JButton topCtrl(String text, String tip, Color fg) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
        b.setForeground(fg); b.setToolTipText(tip);
        b.setContentAreaFilled(false); b.setBorderPainted(false); b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new MouseAdapter(){
            @Override public void mouseEntered(MouseEvent e){ b.setForeground(Color.WHITE); }
            @Override public void mouseExited (MouseEvent e){ b.setForeground(fg); }
        });
        return b;
    }

    // ── Sidebar ───────────────────────────────────────────────────────
    private JPanel buildSidebar() {
        JPanel side = new JPanel(){
            @Override protected void paintComponent(Graphics g){
                super.paintComponent(g);
                Graphics2D g2=(Graphics2D)g.create();
                g2.setColor(C_SIDEBAR); g2.fillRect(0,0,getWidth(),getHeight());
                g2.setColor(C_BORDER);  g2.fillRect(getWidth()-1,0,1,getHeight());
                g2.dispose();
            }
        };
        side.setPreferredSize(new Dimension(192,0));
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setBorder(new EmptyBorder(20,0,20,0));
        for(Object[] item : NAV) side.add(navItem((String)item[0],(String)item[1],(Color)item[2]));
        side.add(Box.createVerticalGlue());
        JLabel ver = new JLabel("  Sanctum v1.0"); ver.setFont(F_MONO); ver.setForeground(C_BORDER);
        side.add(ver);
        return side;
    }

    private JPanel navItem(String icon, String label, Color accent) {
        JPanel item = new JPanel(new BorderLayout()){
            @Override protected void paintComponent(Graphics g){
                super.paintComponent(g);
                Graphics2D g2=(Graphics2D)g.create();
                boolean active = label.equals(activeMenu);
                g2.setColor(active ? new Color(accent.getRed(),accent.getGreen(),accent.getBlue(),40) : C_SIDEBAR);
                g2.fillRect(0,0,getWidth(),getHeight());
                if(active){ g2.setColor(accent); g2.fillRect(0,0,3,getHeight()); }
                g2.dispose();
            }
        };
        item.setPreferredSize(new Dimension(192,44));
        item.setMaximumSize(new Dimension(192,44));
        item.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        item.setBorder(new EmptyBorder(0,14,0,14));
        boolean active = label.equals(activeMenu);
        JLabel ic = new JLabel(icon+"  "); ic.setFont(new Font("Segoe UI Emoji",Font.PLAIN,14));
        ic.setForeground(active ? accent : C_DIM);
        JLabel tx = new JLabel(label); tx.setFont(F_BODY);
        tx.setForeground(active ? C_TEXT : C_DIM);
        item.add(ic, BorderLayout.WEST); item.add(tx, BorderLayout.CENTER);
        item.addMouseListener(new MouseAdapter(){
            @Override public void mouseClicked(MouseEvent e){ switchPage(label); }
            @Override public void mouseEntered(MouseEvent e){ item.repaint(); }
            @Override public void mouseExited (MouseEvent e){ item.repaint(); }
        });
        return item;
    }

    private void switchPage(String page){
        activeMenu = page;
        ((CardLayout)contentArea.getLayout()).show(contentArea, page);
        // Rebuild sidebar to refresh highlights
        Container parent = sidebarPanel.getParent();
        parent.remove(sidebarPanel);
        sidebarPanel = buildSidebar();
        parent.add(sidebarPanel, BorderLayout.WEST);
        parent.revalidate(); parent.repaint();
    }

    // ══════════════════════════════════════════════════════════════════
    //  PAGE: OVERVIEW
    // ══════════════════════════════════════════════════════════════════
    private JPanel buildOverview() {
        JPanel page = scrollPage();
        JPanel inner = innerOf(page);

        inner.add(pageHeader("\uD83D\uDCCA  System Overview", "Platform-wide live statistics"));
        inner.add(vgap(20));

        // Stat cards
        JPanel row = new JPanel(new GridLayout(1,6,14,0));
        row.setOpaque(false); row.setMaximumSize(new Dimension(Integer.MAX_VALUE,110));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        lblChurches = statCard(row,"\u26EA","Total Churches",  "...",  C_ACCENT);
        lblPending  = statCard(row,"\u23F3","Pending",         "...",  C_ORANGE);
        lblActive   = statCard(row,"\u2705","Active Churches", "...",  C_GREEN);
        lblMembers  = statCard(row,"\uD83D\uDC65","Members",   "...",  C_CYAN);
        lblGiving   = statCard(row,"\uD83D\uDCB5","Givings/Mo","KES…", C_GOLD);
        lblNet      = statCard(row,"\uD83D\uDCC8","Net Income", "KES…", C_PURPLE);
        inner.add(row);
        inner.add(vgap(28));

        // Pending churches
        inner.add(pageHeader("\u23F3  Pending Church Approvals","Churches awaiting your review"));
        inner.add(vgap(8));
        inner.add(buildPendingTable());
        inner.add(vgap(24));

        // Recent audit
        inner.add(pageHeader("\uD83D\uDCD3  Recent Audit Activity","Last 8 system events"));
        inner.add(vgap(8));
        inner.add(buildAuditTable(8));
        return page;
    }

    // ══════════════════════════════════════════════════════════════════
    //  PAGE: CHURCHES
    // ══════════════════════════════════════════════════════════════════
    private JPanel buildChurches() {
        JPanel page = scrollPage();
        JPanel inner = innerOf(page);
        inner.add(pageHeader("\u26EA  Church Management","Approve, reject or inspect all churches"));
        inner.add(vgap(16));

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(F_LABEL); tabs.setBackground(C_CARD); tabs.setForeground(C_TEXT);
        tabs.setAlignmentX(Component.LEFT_ALIGNMENT);
        tabs.setMaximumSize(new Dimension(Integer.MAX_VALUE, 600));
        tabs.addTab("\u23F3  Pending", buildPendingTable());
        tabs.addTab("\uD83C\uDFDB\uFE0F  All Churches", buildAllChurchesTable());
        inner.add(tabs);
        return page;
    }

    private JPanel buildPendingTable() {
        String[] cols = {"ID","Name","Code","City","Pastor","Registered","Approve / Reject"};
        DefaultTableModel m = new DefaultTableModel(cols,0){
            @Override public boolean isCellEditable(int r,int c){return c==6;}
        };
        JTable tbl = styledTable(m);
        tbl.getColumnModel().getColumn(6).setCellRenderer(new ApproveRenderer());
        tbl.getColumnModel().getColumn(6).setCellEditor(new ApproveEditor(new JCheckBox(),tbl,m));
        tbl.setRowHeight(38);

        JPanel wrap = cardPanel(BorderLayout.class.cast(null));
        wrap.setLayout(new BorderLayout(0,4));
        JLabel lbl = dim("  Loading pending…");
        wrap.add(lbl, BorderLayout.NORTH);
        JScrollPane sp = styledScroll(tbl, 240); wrap.add(sp, BorderLayout.CENTER);

        SanctumApiClient.getPendingChurches().thenAccept(list -> SwingUtilities.invokeLater(() -> {
            lbl.setText("  " + list.size() + " church(es) pending approval");
            m.setRowCount(0);
            for(Map<String,Object> c : list){
                String reg=s(c,"created_at",""); if(reg.length()>10) reg=reg.substring(0,10);
                m.addRow(new Object[]{s(c,"id",""),s(c,"name",""),s(c,"church_code",""),
                    s(c,"city",""),s(c,"senior_pastor_name",""),reg,"ACTIONS"});
            }
        })).exceptionally(ex -> { SwingUtilities.invokeLater(()->lbl.setText("  Load failed")); return null; });
        return wrap;
    }

    private JPanel buildAllChurchesTable() {
        String[] cols = {"ID","Name","Code","Type","City","Status","Members"};
        DefaultTableModel m = new DefaultTableModel(cols,0){
            @Override public boolean isCellEditable(int r,int c){return false;}
        };
        JTable tbl = styledTable(m);
        JPanel wrap = cardPanel(BorderLayout.class.cast(null));
        wrap.setLayout(new BorderLayout(0,4));
        JLabel lbl = dim("  Loading churches…"); wrap.add(lbl, BorderLayout.NORTH);
        wrap.add(styledScroll(tbl, 420), BorderLayout.CENTER);

        SanctumApiClient.getAllChurches().thenAccept(list -> SwingUtilities.invokeLater(() -> {
            lbl.setText("  "+list.size()+" churches total"); m.setRowCount(0);
            for(Map<String,Object> c : list)
                m.addRow(new Object[]{s(c,"id",""),s(c,"name",""),s(c,"church_code",""),
                    s(c,"church_type_display",""),s(c,"city",""),s(c,"status_display",""),s(c,"member_count","0")});
        })).exceptionally(ex -> { SwingUtilities.invokeLater(()->lbl.setText("  Load failed")); return null; });
        return wrap;
    }

    // ══════════════════════════════════════════════════════════════════
    //  PAGE: USERS
    // ══════════════════════════════════════════════════════════════════
    private JPanel buildUsers() {
        JPanel page = scrollPage();
        JPanel inner = innerOf(page);
        inner.add(pageHeader("\uD83D\uDC65  All System Users","Every registered user with their role and church"));
        inner.add(vgap(12));

        // Role breakdown bar
        JPanel roleRow = new JPanel(new GridLayout(1,6,12,0));
        roleRow.setOpaque(false); roleRow.setMaximumSize(new Dimension(Integer.MAX_VALUE,80));
        roleRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel rMember  = miniCard(roleRow,"Member",  "…",C_ACCENT);
        JLabel rPastor  = miniCard(roleRow,"Pastor",  "…",C_PURPLE);
        JLabel rTreasur = miniCard(roleRow,"Treasurer","…",C_GOLD);
        JLabel rUsher   = miniCard(roleRow,"Usher",   "…",C_CYAN);
        JLabel rDenomAdmin=miniCard(roleRow,"Denom Admin","…",C_ORANGE);
        JLabel rSysAdmin=miniCard(roleRow,"Sys Admin","…",C_RED);
        inner.add(roleRow); inner.add(vgap(14));

        // Search bar
        JTextField search = new JTextField(); search.setFont(F_MONO);
        search.setForeground(C_TEXT); search.setBackground(C_CARD);
        search.setCaretColor(C_ACCENT);
        search.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(C_BORDER), new EmptyBorder(6,10,6,10)));
        search.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        search.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel sLbl = new JLabel("  \uD83D\uDD0D  Search by name or email: ");
        sLbl.setFont(F_BODY); sLbl.setForeground(C_DIM); sLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        inner.add(sLbl); inner.add(search); inner.add(vgap(10));

        // Table
        String[] cols={"ID","Email","Name","Role","Church","Active","Joined"};
        DefaultTableModel model = new DefaultTableModel(cols,0){
            @Override public boolean isCellEditable(int r,int c){return false;}
        };
        JTable tbl = styledTable(model);
        // Colour roles
        tbl.setDefaultRenderer(Object.class, new DefaultTableCellRenderer(){
            @Override public Component getTableCellRendererComponent(JTable t,Object v,
                    boolean sel,boolean foc,int r,int c){
                super.getTableCellRendererComponent(t,v,sel,foc,r,c);
                setBackground(sel ? C_BORDER : (r%2==0 ? C_CARD : C_CARD2));
                setForeground(C_TEXT);
                if(c==3 && v!=null){
                    switch(v.toString().toLowerCase()){
                        case "system_admin": case "admin": setForeground(C_RED); break;
                        case "pastor":        setForeground(C_PURPLE); break;
                        case "treasurer":     setForeground(C_GOLD); break;
                        case "usher":         setForeground(C_CYAN); break;
                        case "denomination_admin": setForeground(C_ORANGE); break;
                        default: setForeground(C_ACCENT);
                    }
                }
                setBorder(new EmptyBorder(4,8,4,8));
                return this;
            }
        });

        JPanel wrap = cardPanel(BorderLayout.class.cast(null));
        wrap.setLayout(new BorderLayout(0,4));
        JLabel lbl = dim("  Loading users…"); wrap.add(lbl, BorderLayout.NORTH);
        wrap.add(styledScroll(tbl, 480), BorderLayout.CENTER);
        wrap.setAlignmentX(Component.LEFT_ALIGNMENT);
        inner.add(wrap);

        // Load via new system endpoint + wire search
        final List<Map<String,Object>>[] cache = new List[]{null};
        SanctumApiClient.getSystemUsers("", "", 500).thenAccept(resp -> SwingUtilities.invokeLater(() -> {
            Object results = resp.get("results");
            @SuppressWarnings("unchecked")
            List<Map<String,Object>> users = (results instanceof List)
                ? (List<Map<String,Object>>) results : new java.util.ArrayList<>();
            cache[0] = users;
            Object total = resp.get("total");
            lbl.setText("  " + (total != null ? total : users.size()) + " users  (loaded " + users.size() + ")");

            // Role summary from server
            Object rs = resp.get("role_summary");
            if (rs instanceof Map) {
                @SuppressWarnings("unchecked") Map<String,Object> rm = (Map<String,Object>) rs;
                rMember  .setText(s2(rm,"member",            "0"));
                rPastor  .setText(s2(rm,"pastor",            "0"));
                rTreasur .setText(s2(rm,"treasurer",         "0"));
                rUsher   .setText(s2(rm,"usher",             "0"));
                rDenomAdmin.setText(s2(rm,"denomination_admin","0"));
                int sa = n(rm,"system_admin") + n(rm,"admin");
                rSysAdmin.setText(String.valueOf(sa));
            } else {
                // Fallback: count from list
                Map<String,Integer> cnt = new LinkedHashMap<>();
                for (Map<String,Object> u : users) cnt.merge(s(u,"role","member"),1,Integer::sum);
                rMember  .setText(String.valueOf(cnt.getOrDefault("member",0)));
                rPastor  .setText(String.valueOf(cnt.getOrDefault("pastor",0)));
                rTreasur .setText(String.valueOf(cnt.getOrDefault("treasurer",0)));
                rUsher   .setText(String.valueOf(cnt.getOrDefault("usher",0)));
                rDenomAdmin.setText(String.valueOf(cnt.getOrDefault("denomination_admin",0)));
                rSysAdmin.setText(String.valueOf(cnt.getOrDefault("system_admin",0)+cnt.getOrDefault("admin",0)));
            }
            populateUsers(model, users, "");
        })).exceptionally(ex -> {
            SwingUtilities.invokeLater(() -> lbl.setText("  Load failed: " + ex.getMessage()));
            return null;
        });

        search.getDocument().addDocumentListener(new javax.swing.event.DocumentListener(){
            void upd(){ if(cache[0]!=null) SwingUtilities.invokeLater(()->populateUsers(model,cache[0],search.getText())); }
            public void insertUpdate(javax.swing.event.DocumentEvent e){upd();}
            public void removeUpdate(javax.swing.event.DocumentEvent e){upd();}
            public void changedUpdate(javax.swing.event.DocumentEvent e){upd();}
        });
        return page;
    }

    private void populateUsers(DefaultTableModel m, List<Map<String,Object>> users, String q){
        m.setRowCount(0);
        String lq = q.toLowerCase().trim();
        for(Map<String,Object> u : users){
            String email = s(u,"email",""), fn=s(u,"first_name",""), ln=s(u,"last_name","");
            String name  = (fn+" "+ln).trim();
            if(!lq.isEmpty() && !email.toLowerCase().contains(lq) && !name.toLowerCase().contains(lq)) continue;
            String joined=s(u,"date_joined",""); if(joined.length()>10) joined=joined.substring(0,10);
            m.addRow(new Object[]{s(u,"id",""),email,name,s(u,"role",""),
                s(u,"church_name","—"),Boolean.TRUE.equals(u.get("is_active"))?"Yes":"No",joined});
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  PAGE: FINANCIALS  (uses /api/system/financials/ + /api/system/giving/)
    // ══════════════════════════════════════════════════════════════════
    private JPanel buildFinancials() {
        JPanel page = scrollPage();
        JPanel inner = innerOf(page);
        inner.add(pageHeader("\uD83D\uDCB0  Platform Financials","Giving summary, expenses and net income"));
        inner.add(vgap(20));

        // KPI stat cards row
        JPanel kpiRow = new JPanel(new GridLayout(1,5,14,0));
        kpiRow.setOpaque(false); kpiRow.setMaximumSize(new Dimension(Integer.MAX_VALUE,110));
        kpiRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel kAllTime  = statCard(kpiRow,"\uD83C\uDF1F","All-Time Giving",    "KES …", C_PURPLE);
        JLabel kGiving   = statCard(kpiRow,"\uD83D\uDCB5","Givings/Month",       "KES …", C_GOLD);
        JLabel kExpenses = statCard(kpiRow,"\uD83D\uDCB8","Expenses/Month",      "KES …", C_RED);
        JLabel kNet      = statCard(kpiRow,"\uD83D\uDCC8","Net Income/Month",    "KES …", C_GREEN);
        JLabel kCount    = statCard(kpiRow,"\uD83D\uDCCB","Transactions",        "…",     C_CYAN);
        inner.add(kpiRow); inner.add(vgap(24));

        // Tabs: Top Churches | By Category | All Transactions
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(F_LABEL); tabs.setBackground(C_CARD); tabs.setForeground(C_TEXT);
        tabs.setAlignmentX(Component.LEFT_ALIGNMENT);
        tabs.setMaximumSize(new Dimension(Integer.MAX_VALUE, 520));

        // Tab 1 — Top churches
        String[] topCols = {"Church","Giving (KES)","Transactions"};
        DefaultTableModel topModel = new DefaultTableModel(topCols,0){
            @Override public boolean isCellEditable(int r,int c){return false;}
        };
        JTable topTbl = styledTable(topModel);
        JPanel topWrap = cardPanel(null); topWrap.setLayout(new BorderLayout());
        topWrap.add(styledScroll(topTbl, 400), BorderLayout.CENTER);
        tabs.addTab("\uD83C\uDFC6  Top Churches", topWrap);

        // Tab 2 — By category
        String[] catCols = {"Category","Giving (KES)","Transactions"};
        DefaultTableModel catModel = new DefaultTableModel(catCols,0){
            @Override public boolean isCellEditable(int r,int c){return false;}
        };
        JTable catTbl = styledTable(catModel);
        JPanel catWrap = cardPanel(null); catWrap.setLayout(new BorderLayout());
        catWrap.add(styledScroll(catTbl, 400), BorderLayout.CENTER);
        tabs.addTab("\uD83C\uDFF7\uFE0F  By Category", catWrap);

        // Tab 3 — Recent transactions
        String[] txCols = {"Date","Church","Member","Category","Amount (KES)","Method","Status"};
        DefaultTableModel txModel = new DefaultTableModel(txCols,0){
            @Override public boolean isCellEditable(int r,int c){return false;}
        };
        JTable txTbl = styledTable(txModel);
        txTbl.setDefaultRenderer(Object.class, new DefaultTableCellRenderer(){
            @Override public Component getTableCellRendererComponent(JTable t,Object v,
                    boolean sel,boolean foc,int r,int c){
                super.getTableCellRendererComponent(t,v,sel,foc,r,c);
                setBackground(sel ? C_BORDER : (r%2==0 ? C_CARD : C_CARD2));
                setForeground(C_TEXT);
                if(c==6 && v!=null){
                    setForeground("completed".equalsIgnoreCase(v.toString()) ? C_GREEN : C_ORANGE);
                    setFont(F_MONO_B);
                } else { setFont(F_MONO); }
                setBorder(new EmptyBorder(4,8,4,8)); return this;
            }
        });
        JPanel txWrap = cardPanel(null); txWrap.setLayout(new BorderLayout());
        txWrap.add(styledScroll(txTbl, 400), BorderLayout.CENTER);
        tabs.addTab("\uD83D\uDCCB  Transactions", txWrap);

        inner.add(tabs);

        // Load from /api/system/financials/
        SanctumApiClient.getSystemFinancials().thenAccept(data -> SwingUtilities.invokeLater(()->{
            Object at = data.get("all_time");
            if(at instanceof Map){
                @SuppressWarnings("unchecked") Map<String,Object> m=(Map<String,Object>)at;
                kAllTime.setText("KES "+fmt(m.getOrDefault("total_giving",0)));
            }
            Object tm = data.get("this_month");
            if(tm instanceof Map){
                @SuppressWarnings("unchecked") Map<String,Object> m=(Map<String,Object>)tm;
                kGiving  .setText("KES "+fmt(m.getOrDefault("total_giving",0)));
                kExpenses.setText("KES "+fmt(m.getOrDefault("total_expenses",0)));
                kNet     .setText("KES "+fmt(m.getOrDefault("net_income",0)));
                kCount   .setText(s2(m,"transaction_count","0"));
            }
            // Top churches
            Object tc = data.get("top_churches");
            if(tc instanceof List){
                @SuppressWarnings("unchecked") List<Map<String,Object>> list=(List<Map<String,Object>>)tc;
                topModel.setRowCount(0);
                for(Map<String,Object> c : list)
                    topModel.addRow(new Object[]{s2(c,"church__name",""),fmt(c.getOrDefault("total",0)),s2(c,"count","0")});
            }
            // By category
            Object bc = data.get("by_category");
            if(bc instanceof List){
                @SuppressWarnings("unchecked") List<Map<String,Object>> list=(List<Map<String,Object>>)bc;
                catModel.setRowCount(0);
                for(Map<String,Object> c : list)
                    catModel.addRow(new Object[]{s2(c,"category__name",""),fmt(c.getOrDefault("total",0)),s2(c,"count","0")});
            }
            // Recent transactions
            Object rt = data.get("recent_transactions");
            if(rt instanceof List){
                @SuppressWarnings("unchecked") List<Map<String,Object>> list=(List<Map<String,Object>>)rt;
                txModel.setRowCount(0);
                for(Map<String,Object> tx : list)
                    txModel.addRow(new Object[]{s2(tx,"date",""),s2(tx,"church",""),s2(tx,"member",""),
                        s2(tx,"category",""),fmt(tx.getOrDefault("amount",0)),s2(tx,"payment_method",""),s2(tx,"status","")});
                tabs.setTitleAt(2, "\uD83D\uDCCB  Transactions (" + list.size() + ")");
            }
        })).exceptionally(ex -> null);
        return page;
    }

    // ══════════════════════════════════════════════════════════════════
    //  PAGE: AUDIT LOGS
    // ══════════════════════════════════════════════════════════════════
    private JPanel buildAuditLogs() {
        JPanel page = scrollPage();
        JPanel inner = innerOf(page);
        inner.add(pageHeader("\uD83D\uDCD3  Audit Logs","All system actions with user and timestamp"));
        inner.add(vgap(10));
        JPanel ap = buildAuditTable(200);
        ap.setAlignmentX(Component.LEFT_ALIGNMENT);
        inner.add(ap);
        return page;
    }

    private JPanel buildAuditTable(int max) {
        String[] cols={"Timestamp","User","Action","Details","IP"};
        DefaultTableModel m = new DefaultTableModel(cols,0){
            @Override public boolean isCellEditable(int r,int c){return false;}
        };
        JTable tbl = styledTable(m);
        tbl.getColumnModel().getColumn(3).setPreferredWidth(320);
        JPanel wrap = cardPanel(BorderLayout.class.cast(null));
        wrap.setLayout(new BorderLayout(0,4));
        JLabel lbl = dim("  Loading audit log…"); wrap.add(lbl,BorderLayout.NORTH);
        wrap.add(styledScroll(tbl, max>20?480:200), BorderLayout.CENTER);

        SanctumApiClient.getAuditLogs(max).thenAccept(logs -> SwingUtilities.invokeLater(()->{
            lbl.setText("  "+logs.size()+" entries"); m.setRowCount(0);
            for(Map<String,Object> log : logs){
                String ts=s(log,"created_at",""); if(ts.length()>19) ts=ts.substring(0,19).replace("T"," ");
                String det=s(log,"details",""); if(det.length()>80) det=det.substring(0,80)+"…";
                m.addRow(new Object[]{ts,s(log,"user_email","system"),s(log,"action",""),det,s(log,"ip_address","")});
            }
        })).exceptionally(ex -> { SwingUtilities.invokeLater(()->lbl.setText("  Load failed: "+ex.getMessage())); return null; });
        return wrap;
    }

    // ══════════════════════════════════════════════════════════════════
    //  PAGE: SETTINGS
    // ══════════════════════════════════════════════════════════════════
    private JPanel buildSettings() {
        JPanel page = scrollPage();
        JPanel inner = innerOf(page);
        inner.add(pageHeader("\u2699\uFE0F  System Settings","Platform configuration"));
        inner.add(vgap(16));
        JPanel card = cardPanel(BorderLayout.class.cast(null));
        card.setLayout(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets=new Insets(8,12,8,12); gc.anchor=GridBagConstraints.WEST;
        String[][] rows={{"Platform","Sanctum Church Management"},{"Backend","https://backend.sanctum.co.ke"},
            {"Version","1.0.0"},{"Environment","Production"}};
        for(int i=0;i<rows.length;i++){
            gc.gridx=0;gc.gridy=i;gc.weightx=0;
            JLabel k=new JLabel(rows[i][0]+":"); k.setFont(F_LABEL); k.setForeground(C_DIM); card.add(k,gc);
            gc.gridx=1;gc.weightx=1;
            JLabel v=new JLabel(rows[i][1]); v.setFont(F_MONO); v.setForeground(C_TEXT); card.add(v,gc);
        }
        gc.gridx=0;gc.gridy=rows.length;gc.gridwidth=2;gc.anchor=GridBagConstraints.CENTER;
        JButton cpBtn = actionBtn("Change Password", C_GOLD);
        cpBtn.addActionListener(e -> ChangePasswordDialog.show(this));
        card.add(cpBtn,gc);
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        inner.add(card);
        return page;
    }

    // ══════════════════════════════════════════════════════════════════
    //  DATA LOADING
    // ══════════════════════════════════════════════════════════════════
    private void loadOverview() {
        SanctumApiClient.getSystemOverview().thenAccept(data -> SwingUtilities.invokeLater(()->{
            Object ch=data.get("churches");
            if(ch instanceof Map){
                @SuppressWarnings("unchecked") Map<String,Object> m=(Map<String,Object>)ch;
                if(lblChurches!=null) lblChurches.setText(s(m,"total","0"));
                if(lblPending !=null) lblPending .setText(s(m,"pending","0"));
                if(lblActive  !=null) lblActive  .setText(s(m,"active","0"));
            }
            Object mem=data.get("members");
            if(mem instanceof Map){
                @SuppressWarnings("unchecked") Map<String,Object> m=(Map<String,Object>)mem;
                if(lblMembers!=null) lblMembers.setText(s(m,"total","0"));
            }
            Object fin=data.get("financials");
            if(fin instanceof Map){
                @SuppressWarnings("unchecked") Map<String,Object> m=(Map<String,Object>)fin;
                if(lblGiving!=null) lblGiving.setText("KES "+fmt(m.getOrDefault("total_givings_this_month",0)));
                if(lblNet   !=null) lblNet   .setText("KES "+fmt(m.getOrDefault("net_income",0)));
            }
        })).exceptionally(ex -> null);
    }

    // ══════════════════════════════════════════════════════════════════
    //  APPROVE/REJECT BUTTON CELL
    // ══════════════════════════════════════════════════════════════════
    private class ApproveRenderer implements TableCellRenderer {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER,6,3));
        ApproveRenderer(){ p.setBackground(C_CARD); p.add(actionBtn("Approve",C_GREEN)); p.add(actionBtn("Reject",C_RED)); }
        @Override public Component getTableCellRendererComponent(JTable t,Object v,boolean s,boolean f,int r,int c){return p;}
    }

    private class ApproveEditor extends DefaultCellEditor {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER,6,3));
        JButton approveBtn = actionBtn("Approve",C_GREEN);
        JButton rejectBtn  = actionBtn("Reject", C_RED);
        int row; JTable tbl; DefaultTableModel mod;

        ApproveEditor(JCheckBox cb, JTable t, DefaultTableModel m){
            super(cb); tbl=t; mod=m; p.setBackground(C_CARD); p.add(approveBtn); p.add(rejectBtn);
            setClickCountToStart(1);
            approveBtn.addActionListener(e -> {
                fireEditingStopped();
                Object idObj=mod.getValueAt(row,0); if(idObj==null) return;
                int id=Integer.parseInt(idObj.toString());
                String name=mod.getValueAt(row,1).toString();
                int ok=JOptionPane.showConfirmDialog(SystemAdminDashboardFrame.this,
                    "Approve \""+name+"\"?","Confirm",JOptionPane.YES_NO_OPTION);
                if(ok==JOptionPane.YES_OPTION){
                    SanctumApiClient.approveChurch(id).thenAccept(success -> SwingUtilities.invokeLater(()->{
                        if(success){ mod.removeRow(row); loadOverview();
                            JOptionPane.showMessageDialog(SystemAdminDashboardFrame.this,"\u2705 Approved: "+name);
                        } else {
                            JOptionPane.showMessageDialog(SystemAdminDashboardFrame.this,"\u274C Approval failed",
                                "Error",JOptionPane.ERROR_MESSAGE);
                        }
                    }));
                }
            });
            rejectBtn.addActionListener(e -> {
                fireEditingStopped();
                Object idObj=mod.getValueAt(row,0); if(idObj==null) return;
                int id=Integer.parseInt(idObj.toString());
                String name=mod.getValueAt(row,1).toString();
                String reason=JOptionPane.showInputDialog(SystemAdminDashboardFrame.this,
                    "Reason for rejecting \""+name+"\":","Reject",JOptionPane.WARNING_MESSAGE);
                if(reason!=null && !reason.trim().isEmpty()){
                    SanctumApiClient.rejectChurch(id,reason.trim()).thenAccept(success -> SwingUtilities.invokeLater(()->{
                        if(success){ mod.removeRow(row); loadOverview();
                            JOptionPane.showMessageDialog(SystemAdminDashboardFrame.this,"\u26D4 Rejected: "+name);
                        }
                    }));
                }
            });
        }
        @Override public Component getTableCellEditorComponent(JTable t,Object v,boolean s,int r,int c){ row=r; return p; }
        @Override public Object getCellEditorValue(){ return "ACTIONS"; }
    }

    // ══════════════════════════════════════════════════════════════════
    //  UI HELPERS
    // ══════════════════════════════════════════════════════════════════
    private JPanel scrollPage(){
        JPanel page = new JPanel(new BorderLayout()); page.setBackground(C_BG);
        JPanel inner = new JPanel(); inner.setBackground(C_BG);
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        inner.setBorder(new EmptyBorder(28,32,32,32));
        JScrollPane sp = new JScrollPane(inner);
        sp.setBorder(null); sp.getViewport().setBackground(C_BG);
        page.add(sp, BorderLayout.CENTER);
        return page;
    }

    private JPanel innerOf(JPanel page){ return (JPanel)((JScrollPane)page.getComponent(0)).getViewport().getView(); }

    private JPanel bg(Color c, LayoutManager lm){ JPanel p=new JPanel(lm); p.setBackground(c); return p; }

    private JPanel cardPanel(Object unused){
        JPanel c=new JPanel(); c.setBackground(C_CARD);
        c.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(C_BORDER), new EmptyBorder(12,12,12,12)));
        return c;
    }
    
    private JPanel pageHeader(String title, String sub){
        JPanel p=new JPanel(); p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS)); p.setOpaque(false);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel t=new JLabel(title); t.setFont(F_HEAD); t.setForeground(C_TEXT); t.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(t);
        if(!sub.isEmpty()){ JLabel s=new JLabel(sub); s.setFont(F_MONO); s.setForeground(C_DIM); s.setAlignmentX(Component.LEFT_ALIGNMENT); p.add(s); }
        return p;
    }

    private Component vgap(int h){ return Box.createVerticalStrut(h); }
    private JLabel dim(String t){ JLabel l=new JLabel(t); l.setFont(F_MONO); l.setForeground(C_DIM); return l; }

    private JLabel statCard(JPanel parent, String icon, String label, String init, Color accent){
        JPanel card = new JPanel(new BorderLayout(0,6)){
            @Override protected void paintComponent(Graphics g){
                super.paintComponent(g);
                Graphics2D g2=(Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(C_CARD); g2.fillRoundRect(0,0,getWidth(),getHeight(),12,12);
                g2.setColor(new Color(accent.getRed(),accent.getGreen(),accent.getBlue(),60));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(1,1,getWidth()-3,getHeight()-3,11,11);
                g2.setColor(accent); g2.fillRoundRect(0,0,4,getHeight(),4,4);
                g2.dispose();
            }
        };
        card.setOpaque(false); card.setBorder(new EmptyBorder(14,16,14,14));
        JPanel top=new JPanel(new FlowLayout(FlowLayout.LEFT,5,0)); top.setOpaque(false);
        JLabel ic=new JLabel(icon); ic.setFont(new Font("Segoe UI Emoji",Font.PLAIN,18));
        JLabel lb=new JLabel(label); lb.setFont(F_MONO); lb.setForeground(C_DIM);
        top.add(ic); top.add(lb);
        JLabel val=new JLabel(init); val.setFont(F_NUM); val.setForeground(accent);
        card.add(top, BorderLayout.NORTH); card.add(val, BorderLayout.CENTER);
        parent.add(card);
        return val;
    }

    private JLabel miniCard(JPanel parent, String label, String init, Color accent){
        JPanel card=new JPanel(new BorderLayout(0,2)); card.setBackground(C_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0,3,0,0,accent), new EmptyBorder(8,10,8,10)));
        JLabel lb=new JLabel(label); lb.setFont(F_MONO); lb.setForeground(C_DIM);
        JLabel val=new JLabel(init); val.setFont(new Font("Segoe UI",Font.BOLD,18)); val.setForeground(accent);
        card.add(lb,BorderLayout.NORTH); card.add(val,BorderLayout.CENTER);
        parent.add(card);
        return val;
    }

    private JTable styledTable(DefaultTableModel m){
        JTable t=new JTable(m);
        t.setBackground(C_CARD); t.setForeground(C_TEXT); t.setFont(F_MONO);
        t.setGridColor(C_BORDER); t.setRowHeight(32);
        t.setShowGrid(true); t.setIntercellSpacing(new Dimension(1,1));
        t.getTableHeader().setBackground(C_SURFACE); t.getTableHeader().setForeground(C_DIM);
        t.getTableHeader().setFont(F_MONO_B);
        t.setSelectionBackground(new Color(C_ACCENT.getRed(),C_ACCENT.getGreen(),C_ACCENT.getBlue(),60));
        t.setSelectionForeground(Color.WHITE);
        return t;
    }

    private JScrollPane styledScroll(JTable tbl, int h){
        JScrollPane sp=new JScrollPane(tbl);
        sp.setPreferredSize(new Dimension(0,h)); sp.getViewport().setBackground(C_CARD); sp.setBorder(null);
        return sp;
    }

    private JButton actionBtn(String text, Color bg){
        JButton b=new JButton(text); b.setBackground(bg); b.setForeground(Color.WHITE);
        b.setFont(F_MONO_B); b.setFocusPainted(false); b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(bg.darker()), new EmptyBorder(4,10,4,10)));
        return b;
    }

    private String s(Map<String,Object> m,String k,String def){ Object v=m.get(k); return (v==null||"null".equals(v.toString()))?def:v.toString(); }
    private String s2(Map<String,Object> m,String k,String def){ return s(m,k,def); }
    private int    n(Map<String,Object> m,String k){ Object v=m.get(k); if(v instanceof Number) return ((Number)v).intValue(); try{return Integer.parseInt(v.toString());}catch(Exception e){return 0;} }
    private String fmt(Object v){ try{ return String.format("%,.0f",Double.parseDouble(v.toString())); } catch(Exception e){ return v.toString(); } }
}
