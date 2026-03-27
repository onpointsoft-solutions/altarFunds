package com.sanctum.view;

import com.sanctum.api.SanctumApiClient;
import com.sanctum.util.DialogManager;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDate;
import java.util.*;
import java.util.List;

/**
 * Budget Studio — Sanctum Church Management System
 * Full-page budget management UI matching TreasurerDashboardFrame design language.
 * Includes Add/Edit form panel, data table, and summary cards.
 */
public class BudgetStudio extends JPanel {

    // ─── Sanctum Brand Color System ──────────────────────────────────────────
    private static final Color C_BG          = new Color(14,  46,  42);
    private static final Color C_SURFACE     = new Color(19,  58,  54);
    private static final Color C_CARD        = new Color(28,  47,  44);
    private static final Color C_CARD_HOVER  = new Color(42,  74,  69);
    private static final Color C_BORDER      = new Color(42,  74,  69);
    private static final Color C_BORDER_LT   = new Color(66, 115, 107);
    private static final Color C_GOLD        = new Color(212, 175,  55);
    private static final Color C_GOLD_HOVER  = new Color(230, 199, 102);
    private static final Color C_TEXT        = new Color(255, 255, 255);
    private static final Color C_TEXT_MID    = new Color(207, 207, 207);
    private static final Color C_TEXT_DIM    = new Color(156, 163, 175);
    private static final Color C_SUCCESS     = new Color( 76, 175,  80);
    private static final Color C_WARNING     = new Color(255, 152,   0);
    private static final Color C_ERROR       = new Color(244,  67,  54);

    // ─── Typography ───────────────────────────────────────────────────────────
    private static final Font F_DISPLAY  = new Font("Georgia",    Font.BOLD,  26);
    private static final Font F_HEADING  = new Font("Arial",      Font.BOLD,  13);
    private static final Font F_LABEL    = new Font("Arial",      Font.BOLD,  11);
    private static final Font F_MONO_SM  = new Font("Monospaced", Font.PLAIN, 11);
    private static final Font F_MONO_LG  = new Font("Monospaced", Font.BOLD,  18);
    private static final Font F_BODY     = new Font("Arial",      Font.PLAIN, 12);
    private static final Font F_SMALL    = new Font("Arial",      Font.PLAIN, 11);
    private static final Font F_H1       = new Font("Arial",      Font.BOLD,  24);

    // ─── Form fields (kept as instance vars for easy read/reset) ─────────────
    private JTextField      fName, fAllocated, fSpent, fYear;
    private JComboBox<String> fUser, fPeriod, fMonth;
    private JTextField      fDepartment;
    private JLabel          lblCreatedAt, lblUpdatedAt;

    // ─── Loading state ───────────────────────────────────────────────────────────
    private JProgressBar   loadingProgress;
    private JLabel          loadingLabel;

    // ─── Table ────────────────────────────────────────────────────────────────
    private JTable          budgetTable;
    private DefaultTableModel tableModel;

    // ─── KPI labels ───────────────────────────────────────────────────────────
    private JLabel lblTotalBudgets, lblTotalAllocated, lblTotalSpent, lblTotalRemaining;

    // ─── Parent frame reference (for dialogs) ────────────────────────────────
    private final Frame parentFrame;

    // ─── Editing state ────────────────────────────────────────────────────────
    private int editingRow = -1;   // -1 = adding new; ≥0 = editing that row

    // ─── Sample user list (replace with SanctumApiClient.getUsers()) ─────────
    private static final String[] SAMPLE_USERS = {
        "vincenttres@gmail.com",
        "usher@gmail.com",
        "vincen@gmail.com",
        "vincent@gmail.com",
        "dkituyi@gmail.com",
        "cyrusmiya@gmail.com"
    };

    // =========================================================================
    // Constructor
    // =========================================================================
    public BudgetStudio(Frame parent) {
        this.parentFrame = parent;
        setOpaque(false);
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(24, 28, 28, 28));

        add(buildPageHeader(),   BorderLayout.NORTH);
        add(buildBody(),         BorderLayout.CENTER);

        // Load budgets from API on initialization
        SwingUtilities.invokeLater(this::refreshBudgets);
    }

    // =========================================================================
    // Page Header
    // =========================================================================
    private JPanel buildPageHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(0, 0, 20, 0));

        // Left: title + subtitle
        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));

        JLabel h1 = new JLabel("Budget Studio");
        h1.setFont(F_DISPLAY);
        h1.setForeground(C_TEXT);

        JLabel sub = new JLabel("Create and manage church budget allocations");
        sub.setFont(F_MONO_SM);
        sub.setForeground(C_TEXT_DIM);

        titles.add(h1);
        titles.add(Box.createVerticalStrut(4));
        titles.add(sub);
        header.add(titles, BorderLayout.WEST);

        // Right: action buttons
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        actions.add(buildTopBtn("📤  Export",    C_TEXT_MID));
        actions.add(buildTopBtn("📥  Import",    C_TEXT_MID));
        actions.add(buildTopBtn("🔄  Refresh",   C_TEXT_MID, e -> refreshBudgets()));
        actions.add(buildPrimaryBtn("➕  Add Budget", e -> resetFormForNew()));
        header.add(actions, BorderLayout.EAST);

        return header;
    }

    // =========================================================================
    // Body: two-column layout — form (left) + table (right)
    // =========================================================================
    private JPanel buildBody() {
        JPanel body = new JPanel(new GridBagLayout());
        body.setOpaque(false);

        GridBagConstraints g = new GridBagConstraints();
        g.fill    = GridBagConstraints.BOTH;
        g.weighty = 1.0;

        // KPI row spans full width
        g.gridx = 0; g.gridy = 0; g.gridwidth = 2; g.weightx = 1.0;
        g.insets = new Insets(0, 0, 16, 0);
        body.add(buildKpiRow(), g);

        // Form panel (left, narrower)
        g.gridx = 0; g.gridy = 1; g.gridwidth = 1; g.weightx = 0.38;
        g.insets = new Insets(0, 0, 0, 16);
        body.add(buildFormPanel(), g);

        // Table panel (right, wider)
        g.gridx = 1; g.gridy = 1; g.weightx = 0.62;
        g.insets = new Insets(0, 0, 0, 0);
        body.add(buildTablePanel(), g);

        return body;
    }

    // =========================================================================
    // KPI Row
    // =========================================================================
    private JPanel buildKpiRow() {
        JPanel row = new JPanel(new GridLayout(1, 4, 16, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));

        JPanel c1 = buildKpiCard("Total Budgets",    "0",       "Budget entries",   C_GOLD,       "📋");
        JPanel c2 = buildKpiCard("Total Allocated",  "KES 0",   "Across all funds", C_GOLD_HOVER, "💰");
        JPanel c3 = buildKpiCard("Total Spent",      "KES 0",   "Year to date",     C_WARNING,    "📤");
        JPanel c4 = buildKpiCard("Total Remaining",  "KES 0",   "Available balance",C_SUCCESS,    "🏦");

        lblTotalBudgets   = findKpiLabel(c1);
        lblTotalAllocated = findKpiLabel(c2);
        lblTotalSpent     = findKpiLabel(c3);
        lblTotalRemaining = findKpiLabel(c4);

        row.add(c1); row.add(c2); row.add(c3); row.add(c4);
        return row;
    }

    private JPanel buildKpiCard(String title, String value, String sub, Color accent, String emoji) {
        JPanel card = new JPanel(new GridBagLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(C_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                GradientPaint gp = new GradientPaint(0, 0, accent, getWidth() / 2f, 0,
                    new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 0));
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth(), 3, 3, 3);
                g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 35));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
            }
            @Override public Dimension getPreferredSize() { return new Dimension(0, 100); }
        };

        GridBagConstraints g = new GridBagConstraints();

        JLabel titleLbl = new JLabel(title.toUpperCase());
        titleLbl.setFont(F_MONO_SM); titleLbl.setForeground(C_TEXT_DIM);

        JLabel emojiLbl = new JLabel(emoji);
        try {
            emojiLbl.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 16));
        } catch (Exception e) {
            // Fallback to a standard font if Segoe UI Emoji is not available
            emojiLbl.setFont(new Font("Arial", Font.PLAIN, 16));
        }

        JLabel valueLbl = new JLabel(value);
        valueLbl.setFont(F_MONO_LG);
        valueLbl.setForeground(accent);
        valueLbl.putClientProperty("kpiValue", Boolean.TRUE);

        JLabel subLbl = new JLabel(sub);
        subLbl.setFont(F_SMALL); subLbl.setForeground(C_TEXT_DIM);

        g.gridx = 0; g.gridy = 0; g.anchor = GridBagConstraints.WEST;
        g.insets = new Insets(14, 14, 2, 14);
        card.add(titleLbl, g);

        g.gridx = 1; g.anchor = GridBagConstraints.NORTHEAST;
        g.insets = new Insets(10, 0, 0, 10);
        card.add(emojiLbl, g);

        g.gridx = 0; g.gridy = 1; g.gridwidth = 2; g.anchor = GridBagConstraints.WEST;
        g.insets = new Insets(2, 14, 2, 14);
        card.add(valueLbl, g);

        g.gridy = 2; g.insets = new Insets(1, 14, 12, 14);
        card.add(subLbl, g);

        return card;
    }

    private JLabel findKpiLabel(Container c) {
        for (Component comp : c.getComponents()) {
            if (comp instanceof JLabel) {
                JLabel l = (JLabel) comp;
                if (Boolean.TRUE.equals(l.getClientProperty("kpiValue"))) return l;
            }
            if (comp instanceof Container) {
                JLabel found = findKpiLabel((Container) comp);
                if (found != null) return found;
            }
        }
        return null;
    }

    // =========================================================================
    // Form Panel
    // =========================================================================
    private JPanel buildFormPanel() {
        JPanel wrapper = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(C_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(C_BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
            }
        };
        wrapper.setOpaque(false);

        // ── Card header
        wrapper.add(buildCardHeader("Add Budget", C_GOLD), BorderLayout.NORTH);

        // ── Scrollable form body
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        form.setBorder(new EmptyBorder(8, 20, 20, 20));

        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL;
        g.weightx = 1.0;

        int row = 0;

        // ── Section: Basic Information ────────────────────────────────────────
        addSectionDivider(form, g, row++, "Basic Information", C_GOLD);

        // Name
        fName = styledField("Budget name");
        addFormRow(form, g, row++, "Name", fName);

        // User (dropdown)
        fUser = new JComboBox<>(SAMPLE_USERS);
        styleCombo(fUser);
        addFormRow(form, g, row++, "User", fUser);

        // Department
        fDepartment = styledField("Department or ministry name");
        addFormRow(form, g, row++, "Department", fDepartment);

        // ── Section: Financial Details ────────────────────────────────────────
        addSectionDivider(form, g, row++, "Financial Details", C_GOLD_HOVER);

        // Allocated amount
        fAllocated = styledField("e.g. 500000");
        addFormRow(form, g, row++, "Allocated amount", fAllocated);

        // Spent amount
        fSpent = styledField("e.g. 120000");
        addFormRow(form, g, row++, "Spent amount", fSpent);

        // ── Section: Period ───────────────────────────────────────────────────
        addSectionDivider(form, g, row++, "Period", C_GOLD_HOVER);

        // Period combo
        fPeriod = new JComboBox<>(new String[]{"Monthly", "Quarterly", "Annual"});
        styleCombo(fPeriod);
        addFormRow(form, g, row++, "Period", fPeriod);

        // Year
        fYear = styledField(String.valueOf(LocalDate.now().getYear()));
        addFormRow(form, g, row++, "Year", fYear);

        // Month
        fMonth = new JComboBox<>(new String[]{
            "January","February","March","April","May","June",
            "July","August","September","October","November","December"
        });
        styleCombo(fMonth);
        addFormRow(form, g, row++, "Month", fMonth);

        // ── Section: System Information ───────────────────────────────────────
        addSectionDivider(form, g, row++, "System Information", C_TEXT_DIM);

        lblCreatedAt = buildReadonlyValue("—");
        addFormRow(form, g, row++, "Created at", lblCreatedAt);

        lblUpdatedAt = buildReadonlyValue("—");
        addFormRow(form, g, row++, "Updated at", lblUpdatedAt);

        // ── Buttons ───────────────────────────────────────────────────────────
        g.gridx = 0; g.gridy = row; g.gridwidth = 2;
        g.insets = new Insets(20, 0, 0, 0);
        form.add(buildFormButtons(), g);

        JScrollPane scroll = new JScrollPane(form);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(12);
        scroll.getVerticalScrollBar().setUI(new ModernScrollBarUI());
        wrapper.add(scroll, BorderLayout.CENTER);

        return wrapper;
    }

    private JPanel buildFormButtons() {
        JPanel btns = new JPanel(new GridLayout(1, 2, 10, 0));
        btns.setOpaque(false);

        JButton cancel = buildSecondaryBtn("✕  Cancel");
        JButton save   = buildPrimaryBtn("💾  Save Budget", e -> submitForm());

        cancel.addActionListener(e -> resetFormForNew());
        btns.add(cancel);
        btns.add(save);
        return btns;
    }

    // =========================================================================
    // Table Panel
    // =========================================================================
    private JPanel buildTablePanel() {
        JPanel wrapper = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(C_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(C_BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
            }
        };
        wrapper.setOpaque(false);

        wrapper.add(buildCardHeader("Budget List", C_GOLD_HOVER), BorderLayout.NORTH);

        // Search bar
        JPanel searchBar = new JPanel(new BorderLayout(8, 0));
        searchBar.setOpaque(false);
        searchBar.setBorder(new EmptyBorder(10, 16, 6, 16));
        JTextField searchField = styledField("🔍  Search budgets...");
        searchBar.add(searchField, BorderLayout.CENTER);
        wrapper.add(searchBar, BorderLayout.AFTER_LAST_LINE);

        // Table
        String[] cols = {"Name", "User", "Department", "Period", "Year", "Month",
                         "Allocated (KES)", "Spent (KES)", "Remaining (KES)", "Actions"};

        tableModel = new DefaultTableModel(new Object[][]{}, cols) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        budgetTable = new JTable(tableModel);
        styleTable(budgetTable);

        // Row selection → populate form
        budgetTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) populateFormFromRow(budgetTable.getSelectedRow());
        });

        JScrollPane scroll = new JScrollPane(budgetTable);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(new EmptyBorder(0, 12, 12, 12));
        scroll.getVerticalScrollBar().setUI(new ModernScrollBarUI());
        wrapper.add(scroll, BorderLayout.CENTER);

        // Load sample/real data
        refreshBudgets();

        return wrapper;
    }

    // =========================================================================
    // Card header helper
    // =========================================================================
    private JPanel buildCardHeader(String title, Color accent) {
        JPanel hdr = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setColor(C_BORDER);
                g2.fillRect(0, getHeight() - 1, getWidth(), 1);
            }
        };
        hdr.setOpaque(false);
        hdr.setBorder(new EmptyBorder(13, 16, 13, 16));

        // dot + title
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setOpaque(false);

        JPanel dot = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(accent); g2.fillOval(0, 0, 7, 7);
            }
            @Override public Dimension getPreferredSize() { return new Dimension(8, 8); }
        };
        dot.setOpaque(false);

        JLabel lbl = new JLabel(title);
        lbl.setFont(F_HEADING); lbl.setForeground(C_TEXT);

        left.add(dot); left.add(lbl);
        hdr.add(left, BorderLayout.WEST);

        return hdr;
    }

    // =========================================================================
    // Form helpers
    // =========================================================================
    private void addSectionDivider(JPanel form, GridBagConstraints g, int row, String label, Color accent) {
        g.gridx = 0; g.gridy = row; g.gridwidth = 2;
        g.insets = new Insets(row == 0 ? 4 : 18, 0, 6, 0);

        JPanel divider = new JPanel(new BorderLayout(10, 0)) {
            @Override protected void paintComponent(Graphics g2) {
                Graphics2D gfx = (Graphics2D) g2;
                gfx.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 45));
                gfx.fillRect(0, getHeight() / 2, getWidth(), 1);
            }
        };
        divider.setOpaque(false);

        JLabel lbl = new JLabel(label);
        lbl.setFont(F_MONO_SM); lbl.setForeground(accent);

        JPanel labelWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        labelWrap.setOpaque(false);
        labelWrap.add(lbl);

        divider.add(labelWrap, BorderLayout.WEST);
        form.add(divider, g);
        g.gridwidth = 1;
    }

    private void addFormRow(JPanel form, GridBagConstraints g, int row, String label, Component field) {
        g.gridx = 0; g.gridy = row; g.weightx = 0; g.gridwidth = 1;
        g.insets = new Insets(5, 0, 5, 12);
        JLabel lbl = new JLabel(label);
        lbl.setFont(F_LABEL); lbl.setForeground(C_TEXT_MID);
        lbl.setPreferredSize(new Dimension(110, 30));
        form.add(lbl, g);

        g.gridx = 1; g.weightx = 1.0; g.insets = new Insets(5, 0, 5, 0);
        form.add(field, g);
    }

    private JTextField styledField(String placeholder) {
        JTextField tf = new JTextField() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(C_SURFACE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 7, 7);
                g2.setColor(isFocusOwner() ? C_GOLD : C_BORDER_LT);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 7, 7);
                super.paintComponent(g);
            }
            @Override public Dimension getPreferredSize() { return new Dimension(0, 34); }
        };
        tf.setOpaque(false);
        tf.setBorder(new EmptyBorder(0, 10, 0, 10));
        tf.setFont(F_BODY);
        tf.setForeground(C_TEXT);
        tf.setCaretColor(C_GOLD);
        tf.putClientProperty("placeholder", placeholder);

        // Placeholder
        tf.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) { tf.repaint(); }
            public void focusLost(FocusEvent e)   { tf.repaint(); }
        });

        return tf;
    }

    private void styleCombo(JComboBox<?> cb) {
        cb.setFont(F_BODY);
        cb.setForeground(C_TEXT);
        cb.setBackground(C_SURFACE);
        cb.setPreferredSize(new Dimension(0, 34));
        cb.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(C_BORDER_LT, 1, true),
            new EmptyBorder(0, 8, 0, 8)
        ));
        cb.setOpaque(true);
        cb.setFocusable(false);
        cb.setRenderer(new DefaultListCellRenderer() {
            { setOpaque(true); }
            @Override public Component getListCellRendererComponent(
                    JList<?> list, Object value, int idx, boolean sel, boolean fcs) {
                super.getListCellRendererComponent(list, value, idx, sel, fcs);
                setBackground(sel ? C_CARD_HOVER : C_SURFACE);
                setForeground(C_TEXT);
                setFont(F_BODY);
                setBorder(new EmptyBorder(4, 10, 4, 10));
                return this;
            }
        });
    }

    private JLabel buildReadonlyValue(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(F_MONO_SM);
        lbl.setForeground(C_TEXT_DIM);
        lbl.setPreferredSize(new Dimension(0, 34));
        lbl.setBorder(new EmptyBorder(0, 10, 0, 0));
        return lbl;
    }

    // =========================================================================
    // Table styling
    // =========================================================================
    private void styleTable(JTable table) {
        table.setFont(F_SMALL);
        table.setForeground(C_TEXT_MID);
        table.setBackground(C_CARD);
        table.setGridColor(C_BORDER);
        table.setRowHeight(36);
        table.setShowVerticalLines(false);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setSelectionBackground(new Color(C_GOLD.getRed(), C_GOLD.getGreen(), C_GOLD.getBlue(), 30));
        table.setSelectionForeground(C_TEXT);
        table.setFillsViewportHeight(true);

        JTableHeader header = table.getTableHeader();
        header.setBackground(C_SURFACE);
        header.setForeground(C_TEXT_DIM);
        header.setFont(F_MONO_SM);
        header.setPreferredSize(new Dimension(0, 36));
        header.setBorder(new MatteBorder(0, 0, 1, 0, C_BORDER));
        ((DefaultTableCellRenderer) header.getDefaultRenderer())
            .setHorizontalAlignment(SwingConstants.LEFT);

        int[] widths = {120, 140, 110, 80, 50, 70, 110, 100, 110, 80};
        for (int i = 0; i < Math.min(widths.length, table.getColumnCount()); i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }

        DefaultTableCellRenderer padded = new DefaultTableCellRenderer() {
            { setBorder(new EmptyBorder(0, 12, 0, 12)); }
        };
        // Actions column uses a custom renderer
        table.getColumnModel().getColumn(9).setCellRenderer(new ActionCellRenderer());
        for (int i = 0; i < 9; i++) table.getColumnModel().getColumn(i).setCellRenderer(padded);
    }

    // ─── Inline action buttons renderer ──────────────────────────────────────
    private class ActionCellRenderer extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(
                JTable t, Object v, boolean sel, boolean fcs, int r, int c) {
            JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 4));
            p.setOpaque(false);
            p.setBackground(sel ? new Color(C_GOLD.getRed(), C_GOLD.getGreen(), C_GOLD.getBlue(), 30) : C_CARD);

            JLabel edit = iconBtn("✏️", "Edit");
            JLabel del  = iconBtn("🗑️", "Delete");
            p.add(edit); p.add(del);
            return p;
        }

        private JLabel iconBtn(String emoji, String tip) {
            JLabel lbl = new JLabel(emoji);
            try {
                lbl.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 13));
            } catch (Exception e) {
                // Fallback to a standard font if Segoe UI Emoji is not available
                lbl.setFont(new Font("Arial", Font.PLAIN, 13));
            }
            lbl.setToolTipText(tip);
            lbl.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            return lbl;
        }
    }

    // =========================================================================
    // Button builders
    // =========================================================================
    private JButton buildPrimaryBtn(String text, ActionListener al) {
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
                g2.drawString(text,
                    (getWidth() - fm.stringWidth(text)) / 2,
                    (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
            }
        };
        btn.setPreferredSize(new Dimension(160, 34));
        btn.setContentAreaFilled(false); btn.setBorderPainted(false); btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        if (al != null) btn.addActionListener(al);
        return btn;
    }

    private JButton buildSecondaryBtn(String text) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? C_CARD_HOVER : C_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(getModel().isRollover() ? C_BORDER_LT : C_BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.setFont(F_LABEL);
                g2.setColor(getModel().isRollover() ? C_TEXT : C_TEXT_MID);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(text,
                    (getWidth() - fm.stringWidth(text)) / 2,
                    (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
            }
        };
        btn.setPreferredSize(new Dimension(0, 34));
        btn.setContentAreaFilled(false); btn.setBorderPainted(false); btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JButton buildTopBtn(String text, Color fg) {
        return buildTopBtn(text, fg, null);
    }

    private JButton buildTopBtn(String text, Color fg, ActionListener al) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? C_CARD_HOVER : C_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(getModel().isRollover() ? C_BORDER_LT : C_BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.setFont(F_SMALL);
                g2.setColor(getModel().isRollover() ? C_TEXT : fg);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(text,
                    (getWidth() - fm.stringWidth(text)) / 2,
                    (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
            }
        };
        btn.setPreferredSize(new Dimension(130, 34));
        btn.setContentAreaFilled(false); btn.setBorderPainted(false); btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        if (al != null) btn.addActionListener(al);
        return btn;
    }

    // =========================================================================
    // Data / form logic
    // =========================================================================

    /** Clear form for creating a new entry */
    private void resetFormForNew() {
        editingRow = -1;
        fName.setText("");
        fDepartment.setText("");
        fAllocated.setText("");
        fSpent.setText("");
        fPeriod.setSelectedIndex(0);
        fYear.setText(String.valueOf(LocalDate.now().getYear()));
        fMonth.setSelectedIndex(LocalDate.now().getMonthValue() - 1);
        fUser.setSelectedIndex(0);
        lblCreatedAt.setText("—");
        lblUpdatedAt.setText("—");
        budgetTable.clearSelection();
    }

    /** Populate form fields from a selected table row */
    private void populateFormFromRow(int row) {
        if (row < 0 || row >= tableModel.getRowCount()) return;
        editingRow = row;
        fName.setText(tableModel.getValueAt(row, 0).toString());
        fUser.setSelectedItem(tableModel.getValueAt(row, 1).toString());
        fDepartment.setText(tableModel.getValueAt(row, 2).toString());

        String period = tableModel.getValueAt(row, 3).toString();
        for (int i = 0; i < fPeriod.getItemCount(); i++) {
            if (fPeriod.getItemAt(i).equalsIgnoreCase(period)) { fPeriod.setSelectedIndex(i); break; }
        }
        fYear.setText(tableModel.getValueAt(row, 4).toString());
        String month = tableModel.getValueAt(row, 5).toString();
        for (int i = 0; i < fMonth.getItemCount(); i++) {
            if (fMonth.getItemAt(i).equalsIgnoreCase(month)) { fMonth.setSelectedIndex(i); break; }
        }

        // Strip "KES " prefix for editing
        fAllocated.setText(stripKes(tableModel.getValueAt(row, 6).toString()));
        fSpent.setText(stripKes(tableModel.getValueAt(row, 7).toString()));
        lblCreatedAt.setText("2025-01-15  10:32");
        lblUpdatedAt.setText("2025-04-02  08:14");
    }

    private String stripKes(String v) { return v.replace("KES ", "").replace(",", "").trim(); }

    /** Save or update a budget entry */
    private void submitForm() {
        String name  = fName.getText().trim();
        String dept  = fDepartment.getText().trim();
        String alloc = fAllocated.getText().trim();
        String spent = fSpent.getText().trim();
        String year  = fYear.getText().trim();

        if (name.isEmpty() || dept.isEmpty() || alloc.isEmpty() || year.isEmpty()) {
            DialogManager.showMessageDialog(this,
                "Name, Department, Allocated amount and Year are required.",
                "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        double allocatedVal, spentVal;
        try {
            allocatedVal = Double.parseDouble(alloc.replaceAll("[^\\d.]", ""));
            spentVal     = spent.isEmpty() ? 0 : Double.parseDouble(spent.replaceAll("[^\\d.]", ""));
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this,
                "Please enter valid numeric values for amounts.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        double remaining = allocatedVal - spentVal;
        String user      = fUser.getSelectedItem().toString();
        String period    = fPeriod.getSelectedItem().toString();
        String month     = fMonth.getSelectedItem().toString();
        String now       = LocalDate.now().toString();

        if (editingRow >= 0) {
            // Update existing row
            tableModel.setValueAt(name,               editingRow, 0);
            tableModel.setValueAt(user,               editingRow, 1);
            tableModel.setValueAt(dept,               editingRow, 2);
            tableModel.setValueAt(period,             editingRow, 3);
            tableModel.setValueAt(year,               editingRow, 4);
            tableModel.setValueAt(month,              editingRow, 5);
            tableModel.setValueAt(fmtKes(allocatedVal), editingRow, 6);
            tableModel.setValueAt(fmtKes(spentVal),   editingRow, 7);
            tableModel.setValueAt(fmtKes(remaining),  editingRow, 8);
            tableModel.setValueAt("Edit / Delete",    editingRow, 9);
            lblUpdatedAt.setText(now);
        } else {
            // Add new row
            tableModel.addRow(new Object[]{
                name, user, dept, period, year, month,
                fmtKes(allocatedVal), fmtKes(spentVal), fmtKes(remaining),
                "Edit / Delete"
            });
            lblCreatedAt.setText(now);
            lblUpdatedAt.setText(now);
        }

        updateKpiLabels();

        SanctumApiClient.addBudget(name, dept, allocatedVal, period.toLowerCase(),
            Integer.parseInt(year), fMonth.getSelectedIndex() + 1)
            .thenAccept(ok -> SwingUtilities.invokeLater(() -> { 
                if (!ok) {
                    DialogManager.showMessageDialog(this, "Failed to save budget to server.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }));

        String msg = editingRow >= 0 ? "Budget updated successfully." : "Budget saved successfully.";
        DialogManager.showMessageDialog(this, msg, "Success", JOptionPane.INFORMATION_MESSAGE);
        resetFormForNew();
        
        // Fire property change to notify parent (TreasurerDashboardFrame) to refresh data
        firePropertyChange("budgetSaved", null, Boolean.TRUE);
    }

    /** Load budgets from API (or use table data) */
    private void refreshBudgets() {
        SanctumApiClient.getBudgets().thenAccept(budgets -> SwingUtilities.invokeLater(() -> {
            tableModel.setRowCount(0);
            for (Map<String, Object> b : budgets) {
                double alloc     = parseDouble(b.getOrDefault("allocated_amount", "0").toString());
                double spentAmt  = parseDouble(b.getOrDefault("spent_amount",     "0").toString());
                double remaining = alloc - spentAmt;
                tableModel.addRow(new Object[]{
                    b.getOrDefault("name",       "—"),
                    b.getOrDefault("user_email", "—"),
                    b.getOrDefault("department", "—"),
                    b.getOrDefault("period",     "—"),
                    b.getOrDefault("year",       "—"),
                    b.getOrDefault("month",      "—"),
                    fmtKes(alloc),
                    fmtKes(spentAmt),
                    fmtKes(remaining),
                    "Edit / Delete"
                });
            }
            updateKpiLabels();
        })).exceptionally(ex -> { System.err.println("Budget refresh error: " + ex.getMessage()); return null; });
    }

    private void updateKpiLabels() {
        int total = tableModel.getRowCount();
        double sumAlloc = 0, sumSpent = 0, sumRem = 0;
        for (int r = 0; r < total; r++) {
            sumAlloc += parseKes(tableModel.getValueAt(r, 6).toString());
            sumSpent += parseKes(tableModel.getValueAt(r, 7).toString());
            sumRem   += parseKes(tableModel.getValueAt(r, 8).toString());
        }
        if (lblTotalBudgets   != null) lblTotalBudgets.setText(String.valueOf(total));
        if (lblTotalAllocated != null) lblTotalAllocated.setText("KES " + fmt(String.valueOf(sumAlloc)));
        if (lblTotalSpent     != null) lblTotalSpent.setText("KES " + fmt(String.valueOf(sumSpent)));
        if (lblTotalRemaining != null) lblTotalRemaining.setText("KES " + fmt(String.valueOf(sumRem)));
    }

    // =========================================================================
    // Number / format helpers
    // =========================================================================
    private double parseDouble(String s) {
        try { return Double.parseDouble(s.replaceAll("[^\\d.]", "")); }
        catch (NumberFormatException e) { return 0; }
    }

    private double parseKes(String s) { return parseDouble(s.replace("KES ", "")); }

    private String fmtKes(double val) { return "KES " + fmt(String.valueOf(val)); }

    private String fmt(String raw) {
        try {
            double val = Double.parseDouble(raw.replaceAll("[^\\d.]", ""));
            if (val >= 1_000_000) return String.format("%.2fM", val / 1_000_000);
            if (val >= 1_000)     return String.format("%,.0f", val);
            return String.format("%.2f", val);
        } catch (NumberFormatException e) { return raw; }
    }

    // =========================================================================
    // Modern ScrollBar UI (same as parent frame)
    // =========================================================================
    static class ModernScrollBarUI extends javax.swing.plaf.basic.BasicScrollBarUI {
        @Override protected void configureScrollBarColors() {
            thumbColor      = new Color(C_BORDER_LT.getRed(), C_BORDER_LT.getGreen(), C_BORDER_LT.getBlue(), 140);
            trackColor      = C_SURFACE;
        }
        @Override protected JButton createDecreaseButton(int o) { return zeroBtn(); }
        @Override protected JButton createIncreaseButton(int o) { return zeroBtn(); }
        private JButton zeroBtn() {
            JButton b = new JButton(); b.setPreferredSize(new Dimension(0, 0)); b.setMinimumSize(new Dimension(0, 0)); return b;
        }
        @Override protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(thumbColor);
            g2.fillRoundRect(r.x + 2, r.y + 2, r.width - 4, r.height - 4, 6, 6);
        }
        @Override protected void paintTrack(Graphics g, JComponent c, Rectangle r) {
            g.setColor(trackColor); g.fillRect(r.x, r.y, r.width, r.height);
        }
    }

    // =========================================================================
    // Stand-alone test entry point
    // =========================================================================
public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Budget Studio — Test");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setUndecorated(true);
            frame.setExtendedState(JFrame.MAXIMIZED_BOTH);

            JPanel root = new JPanel(new BorderLayout()) {
                @Override protected void paintComponent(Graphics g) {
                    g.setColor(new Color(14, 46, 42));
                    g.fillRect(0, 0, getWidth(), getHeight());
                }
            };
            root.setOpaque(true);

            JScrollPane scroll = new JScrollPane(new BudgetStudio(frame));
            scroll.setOpaque(false);
            scroll.getViewport().setOpaque(false);
            scroll.setBorder(null);
            root.add(scroll);

            frame.setContentPane(root);
            frame.setVisible(true);
        });
    }
}