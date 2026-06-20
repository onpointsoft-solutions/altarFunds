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
import java.util.HashMap;

/**
 * Pastor Dashboard - Modern Ministry Interface
 * Emerald green theme matching UsherDashboardFrame
 *
 * FIXES APPLIED:
 *  1. getEmojiFont() — probe char changed to SMP surrogate pair (😀) so
 *     Dialog fallback is never mistakenly accepted on Linux / older Windows.
 *  2. withEmojiFont() helper introduced — derives the correct size/style
 *     from any base font while substituting an emoji-capable family.
 *  3. Every JButton / JLabel that carries an emoji prefix now calls
 *     withEmojiFont() instead of F_LABEL or F_MONO_SM directly.
 *  4. buildTopBar() window-control buttons use withEmojiFont().
 *  5. buildDevotionalsPage() / buildMembersPage() "Add" buttons fixed.
 *  6. createDevotionalGridCard() view/edit buttons fixed.
 *  7. createMemberGridCard() view/contact buttons fixed.
 *  8. createActionButton() fixed.
 *  9. Sidebar icon labels: getEmojiFont call kept; verified safe.
 * 10. Dead-code createMinistryStatsCard() / createUpcomingEventsCard()
 *     retained but emoji labels inside them also fixed for consistency.
 */
public class PastorDashboardFrame extends JFrame {

    // ─── Sanctum Brand Color System ───────────────────────────────────
    private static final Color C_BG          = new Color(14, 46, 42);
    private static final Color C_SURFACE     = new Color(19, 58, 54);
    private static final Color C_CARD        = new Color(28, 47, 44);
    private static final Color C_GOLD        = new Color(212, 175, 55);
    private static final Color C_GOLD_HOVER  = new Color(230, 199, 102);
    private static final Color C_GOLD_DIM    = new Color(212, 175, 55, 25);
    private static final Color C_TEXT        = new Color(255, 255, 255);
    private static final Color C_TEXT_MID    = new Color(207, 207, 207);
    private static final Color C_TEXT_DIM    = new Color(156, 163, 175);
    private static final Color C_BORDER      = new Color(42, 74, 69);
    private static final Color C_SUCCESS     = new Color(52, 199, 89);
    private static final Color C_DANGER      = new Color(255, 59, 48);

    // ─── Typography ───────────────────────────────────────────────────
    private static final Font F_TITLE   = new Font("Segoe UI",       Font.BOLD,  28);
    private static final Font F_LABEL   = new Font("Segoe UI",       Font.BOLD,  14);
    private static final Font F_MONO_SM = new Font("JetBrains Mono", Font.PLAIN, 11);
    private static final Font F_MONO_LG = new Font("JetBrains Mono", Font.BOLD,  20);

    // ─── Navigation ───────────────────────────────────────────────────
    private static final String[][] MENU_ITEMS = {
        {"🏠", "Overview"},
        {"📖", "Devotionals"},
        {"👥", "Members"},
        {"🙏", "Announcements"}
    };

    // ─── UI State ─────────────────────────────────────────────────────
    private final CardLayout cardLayout = new CardLayout();
    private JPanel contentArea;
    private JPanel sidebar;
    private String activeMenu = "Overview";

    private JPanel devotionalsGrid;
    private JPanel membersGrid;

    private JLabel lblTotalMembers;
    private JLabel lblDevotionals;
    private JLabel lblAnnouncements;
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

    // ═══════════════════════════════════════════════════════════════════
    //  EMOJI FONT UTILITIES  (FIX 1 + FIX 2)
    // ═══════════════════════════════════════════════════════════════════

    /**
     * FIX 1 — Returns the first available color-emoji font at {@code size}.
     *
     * The original code probed with U+263A (☺), a Basic Multilingual Plane
     * character that many non-emoji fonts can display, causing the Dialog
     * fallback to be returned on Linux / older Windows — which renders emoji
     * as monochrome boxes.
     *
     * We now probe with the surrogate pair "\uD83D\uDE00" (😀, U+1F600), a
     * Supplementary Multilingual Plane character that only a genuine color-
     * emoji font can represent.  canDisplayUpTo() returns -1 when every
     * character in the string can be displayed.
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
     * FIX 2 — Derives an emoji-capable font that matches the size and style
     * of {@code base}.
     *
     * Use this wherever a JButton or JLabel mixes emoji with Latin text and
     * was previously set to F_LABEL or F_MONO_SM directly.
     *
     * Example:
     *   btn.setFont(withEmojiFont(F_MONO_SM));   // was: btn.setFont(F_MONO_SM)
     */
    private static Font withEmojiFont(Font base) {
        Font emoji = getEmojiFont(base.getSize());
        return emoji.deriveFont(base.getStyle(), base.getSize2D());
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
        contentArea.add(buildMainDashboard(),   "overview");
        contentArea.add(buildDevotionalsPage(), "devotionals");
        contentArea.add(buildMembersPage(),     "members");
        contentArea.add(buildAnnouncementsPage(),      "announcements");

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
        icon.setFont(getEmojiFont(16));             // already correct
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
        // FIX 3 — window-control glyphs (─, ✕) are BMP but using withEmojiFont
        // guarantees they render correctly even when the glyph set differs.
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

        Map<String, Object> userData = SanctumApiClient.getCurrentUserData();
        String firstName   = userData != null ? userData.getOrDefault("first_name", "Pastor").toString() : "Pastor";
        String lastName    = userData != null ? userData.getOrDefault("last_name",  "").toString()       : "";
        String displayName = lastName.isEmpty() ? firstName : firstName + " " + lastName;
        String initials    = (firstName.isEmpty() ? "P" : String.valueOf(firstName.charAt(0)))
                           + (lastName.isEmpty()  ? ""  : String.valueOf(lastName.charAt(0)));

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

        // FIX 9 — icon label: getEmojiFont already applied (was correct)
        JLabel iconLabel = new JLabel(icon + "  ");
        iconLabel.setFont(getEmojiFont(14));
        iconLabel.setForeground(text.equals(activeMenu) ? C_BG : C_TEXT_MID);

        JLabel textLabel = new JLabel(text) {
            @Override public Color getForeground() {
                return text.equals(activeMenu) ? C_BG : C_TEXT_MID;
            }
        };
        textLabel.setFont(F_MONO_SM);  // plain text label — no emoji, safe

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
        System.out.println("Switching to: " + menu);

        if (menu.equals("Devotionals") && devotionalsGrid != null) {
            System.out.println("Loading devotionals page data...");
            loadDevotionalsGridData(devotionalsGrid);
        } else if (menu.equals("Members") && membersGrid != null) {
            System.out.println("Loading members page data...");
            loadMembersGridData(membersGrid);
        }

        cardLayout.show(contentArea, menu.toLowerCase());
        if (sidebar != null) sidebar.repaint();
    }

    // ─── Main Dashboard Page ──────────────────────────────────────────
    private JPanel buildMainDashboard() {
        JPanel main = new JPanel(new BorderLayout(0, 20));
        main.setOpaque(false);
        main.setBorder(new EmptyBorder(30, 30, 30, 30));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JPanel logoBlock = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        logoBlock.setOpaque(false);
        JLabel logoLabel = new JLabel("Sanctum");
        logoLabel.setFont(new Font("Georgia", Font.BOLD, 24));
        logoLabel.setForeground(C_TEXT);
        JLabel roleLabel = new JLabel("PASTOR PORTAL");
        roleLabel.setFont(F_MONO_SM);
        roleLabel.setForeground(C_GOLD);
        logoBlock.add(logoLabel);
        logoBlock.add(Box.createHorizontalStrut(12));
        logoBlock.add(roleLabel);
        header.add(logoBlock, BorderLayout.WEST);

        JPanel kpiRow = new JPanel(new GridLayout(1, 4, 15, 0));
        kpiRow.setOpaque(false);
        kpiRow.setBorder(new EmptyBorder(0, 0, 20, 0));

        JLabel[] kpi = new JLabel[4];
        kpiRow.add(buildKpiCard("Total Members",    "0", "Active members", C_GOLD,                    "👥", kpi, 0));
        kpiRow.add(buildKpiCard("Devotionals",      "0", "This week",      C_SUCCESS,                 "📖", kpi, 1));
        kpiRow.add(buildKpiCard("Announcements",  "0", "Pending",        C_TEXT_MID,                "🙏", kpi, 2));
        kpiRow.add(buildKpiCard("Counseling",       "0", "Sessions",       new Color(100, 150, 255),  "💬", kpi, 3));

        lblTotalMembers      = kpi[0];
        lblDevotionals       = kpi[1];
        lblAnnouncements    = kpi[2];
        lblCounselingSessions = kpi[3];

        JPanel contentGrid = new JPanel(new GridLayout(1, 2, 20, 20));
        contentGrid.setOpaque(false);
        contentGrid.add(createQuickActionsCard());
        contentGrid.add(createRecentAnnouncementsCard());

        JPanel center = new JPanel(new BorderLayout(0, 20));
        center.setOpaque(false);
        center.add(kpiRow,       BorderLayout.NORTH);
        center.add(contentGrid,  BorderLayout.CENTER);

        main.add(header, BorderLayout.NORTH);
        main.add(center, BorderLayout.CENTER);

        loadDashboardCardsData();
        return main;
    }

    private JPanel createQuickActionsCard() {
        JPanel card = createCard("Quick Actions", "⚡", C_GOLD);
        JPanel content = new JPanel(new GridLayout(2, 2, 10, 10));
        content.setOpaque(false);

        String[][] actions = {
            {"🙏", "Add Devotional", "devotional"},
        };

        for (String[] action : actions) {
            content.add(createActionButton(action[0], action[1], action[2]));
        }

        card.add(content, BorderLayout.CENTER);
        return card;
    }

    /**
     * FIX 4 — createActionButton(): button now uses withEmojiFont(F_LABEL)
     * so the emoji prefix (e.g. "🙏") renders correctly instead of boxing.
     */
    private JButton createActionButton(String icon, String text, String action) {
        JButton btn = new JButton(icon + " " + text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isPressed()) {
                    g2.setColor(C_SURFACE);
                } else if (getModel().isRollover()) {
                    g2.setColor(C_CARD.brighter());
                } else {
                    g2.setColor(C_CARD);
                }
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        // FIX 4 — was: btn.setFont(F_LABEL)
        btn.setFont(withEmojiFont(F_LABEL));
        btn.setForeground(C_TEXT);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> handleQuickAction(action));
        return btn;
    }

    private void handleQuickAction(String action) {
        switch (action) {
            case "announcement": showCreateAnnouncementDialog(); break;
            case "event":        showCreateEventDialog();        break;
            case "devotional":   showCreateDevotionalDialog();   break;
            case "member":       showCreateMemberDialog();       break;
        }
    }

    // ─── Dialogs ──────────────────────────────────────────────────────
    private void showCreateAnnouncementDialog() {
        JDialog dialog = new JDialog(this, "Create Announcement", true);
        dialog.setSize(500, 400);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        panel.setBackground(C_CARD);
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(5, 5, 5, 5);

        JTextField titleField   = styledTextField();
        JTextArea  contentArea  = styledTextArea(8);
        JComboBox<String> priorityCombo = styledComboBox("Normal", "High", "Urgent");

        JButton createBtn = styledDialogButton("Create Announcement", C_GOLD);
        JButton cancelBtn = styledDialogButton("Cancel", C_DANGER);

        gc.gridx = 0; gc.gridy = 0; gc.anchor = GridBagConstraints.WEST;
        panel.add(dialogLabel("Title:"), gc);
        gc.gridx = 1; gc.gridy = 0; gc.weightx = 1;
        panel.add(titleField, gc);

        gc.gridx = 0; gc.gridy = 1; gc.weightx = 0; gc.anchor = GridBagConstraints.NORTHWEST;
        panel.add(dialogLabel("Content:"), gc);
        gc.gridx = 1; gc.gridy = 1; gc.weightx = 1; gc.fill = GridBagConstraints.BOTH;
        panel.add(new JScrollPane(contentArea), gc);

        gc.gridx = 0; gc.gridy = 2; gc.weightx = 0; gc.fill = GridBagConstraints.HORIZONTAL; gc.anchor = GridBagConstraints.WEST;
        panel.add(dialogLabel("Priority:"), gc);
        gc.gridx = 1; gc.gridy = 2; gc.weightx = 1;
        panel.add(priorityCombo, gc);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.add(cancelBtn);
        buttonPanel.add(createBtn);

        gc.gridx = 0; gc.gridy = 3; gc.gridwidth = 2; gc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(buttonPanel, gc);

        dialog.add(panel);

        createBtn.addActionListener(e -> {
            String t  = titleField.getText().trim();
            String c  = contentArea.getText().trim();
            String p  = priorityCombo.getSelectedItem().toString().toLowerCase();
            if (t.isEmpty() || c.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Title and content are required.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            SanctumApiClient.createAnnouncement(t, c, p).thenAccept(ok -> SwingUtilities.invokeLater(() -> {
                if (ok) { JOptionPane.showMessageDialog(dialog, "Announcement created successfully!", "Success", JOptionPane.INFORMATION_MESSAGE); dialog.dispose(); loadDashboardCardsData(); }
                else    { JOptionPane.showMessageDialog(dialog, "Failed to create announcement.", "Error", JOptionPane.ERROR_MESSAGE); }
            }));
        });
        cancelBtn.addActionListener(e -> dialog.dispose());
        dialog.setVisible(true);
    }

    private void showCreateDevotionalDialog() {
        JDialog dialog = new JDialog(this, "Create Devotional", true);
        dialog.setSize(500, 450);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        panel.setBackground(C_CARD);
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(5, 5, 5, 5);

        JTextField titleField     = styledTextField();
        JTextField scriptureField = styledTextField();
        JTextArea  contentArea    = styledTextArea(10);

        JButton createBtn = styledDialogButton("Create Devotional", C_GOLD);
        JButton cancelBtn = styledDialogButton("Cancel", C_DANGER);

        gc.gridx = 0; gc.gridy = 0; gc.anchor = GridBagConstraints.WEST;
        panel.add(dialogLabel("Title:"), gc);
        gc.gridx = 1; gc.gridy = 0; gc.weightx = 1;
        panel.add(titleField, gc);

        gc.gridx = 0; gc.gridy = 1; gc.weightx = 0; gc.anchor = GridBagConstraints.WEST;
        panel.add(dialogLabel("Scripture Reference:"), gc);
        gc.gridx = 1; gc.gridy = 1; gc.weightx = 1;
        panel.add(scriptureField, gc);

        gc.gridx = 0; gc.gridy = 2; gc.weightx = 0; gc.anchor = GridBagConstraints.NORTHWEST;
        panel.add(dialogLabel("Content:"), gc);
        gc.gridx = 1; gc.gridy = 2; gc.weightx = 1; gc.fill = GridBagConstraints.BOTH;
        panel.add(new JScrollPane(contentArea), gc);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.add(cancelBtn);
        buttonPanel.add(createBtn);

        gc.gridx = 0; gc.gridy = 3; gc.gridwidth = 2; gc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(buttonPanel, gc);

        dialog.add(panel);

        createBtn.addActionListener(e -> {
            String t = titleField.getText().trim();
            String s = scriptureField.getText().trim();
            String c = contentArea.getText().trim();
            if (t.isEmpty() || c.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Title and content are required.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            SanctumApiClient.createDevotional(t, c, s).thenAccept(ok -> SwingUtilities.invokeLater(() -> {
                if (ok) { JOptionPane.showMessageDialog(dialog, "Devotional created successfully!", "Success", JOptionPane.INFORMATION_MESSAGE); dialog.dispose(); loadData(); }
                else    { JOptionPane.showMessageDialog(dialog, "Failed to create devotional.", "Error", JOptionPane.ERROR_MESSAGE); }
            }));
        });
        cancelBtn.addActionListener(e -> dialog.dispose());
        dialog.setVisible(true);
    }

    private void showCreateEventDialog() {
        JOptionPane.showMessageDialog(this, "Event creation coming soon!", "Coming Soon", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showCreateMemberDialog() {
        JDialog dialog = new JDialog(this, "Add New Member", true);
        dialog.setSize(500, 600);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        panel.setBackground(C_CARD);
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(5, 5, 5, 5);

        JTextField firstNameField = styledTextField();
        JTextField lastNameField  = styledTextField();
        JTextField emailField     = styledTextField();
        JTextField phoneField     = styledTextField();
        JComboBox<String> statusCombo = styledComboBox("new_member", "active_member", "inactive_member");

        JButton createBtn = styledDialogButton("Add Member", C_GOLD);
        JButton cancelBtn = styledDialogButton("Cancel", C_DANGER);

        gc.gridx = 0; gc.gridy = 0; gc.anchor = GridBagConstraints.WEST;
        panel.add(dialogLabel("First Name:"), gc);
        gc.gridx = 1; gc.gridy = 0; gc.weightx = 1;
        panel.add(firstNameField, gc);

        gc.gridx = 0; gc.gridy = 1; gc.weightx = 0; gc.anchor = GridBagConstraints.WEST;
        panel.add(dialogLabel("Last Name:"), gc);
        gc.gridx = 1; gc.gridy = 1; gc.weightx = 1;
        panel.add(lastNameField, gc);

        gc.gridx = 0; gc.gridy = 2; gc.weightx = 0; gc.anchor = GridBagConstraints.WEST;
        panel.add(dialogLabel("Email:"), gc);
        gc.gridx = 1; gc.gridy = 2; gc.weightx = 1;
        panel.add(emailField, gc);

        gc.gridx = 0; gc.gridy = 3; gc.weightx = 0; gc.anchor = GridBagConstraints.WEST;
        panel.add(dialogLabel("Phone:"), gc);
        gc.gridx = 1; gc.gridy = 3; gc.weightx = 1;
        panel.add(phoneField, gc);

        gc.gridx = 0; gc.gridy = 4; gc.weightx = 0; gc.fill = GridBagConstraints.HORIZONTAL; gc.anchor = GridBagConstraints.WEST;
        panel.add(dialogLabel("Status:"), gc);
        gc.gridx = 1; gc.gridy = 4; gc.weightx = 1;
        panel.add(statusCombo, gc);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.add(cancelBtn);
        buttonPanel.add(createBtn);

        gc.gridx = 0; gc.gridy = 5; gc.gridwidth = 2; gc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(buttonPanel, gc);

        dialog.add(panel);

        createBtn.addActionListener(e -> {
            String fn    = firstNameField.getText().trim();
            String ln    = lastNameField.getText().trim();
            String email = emailField.getText().trim();
            String phone = phoneField.getText().trim();
            String stat  = statusCombo.getSelectedItem().toString();
            if (fn.isEmpty() || email.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "First name and email are required.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            Map<String, Object> memberData = new HashMap<>();
            memberData.put("first_name",        fn);
            memberData.put("last_name",          ln);
            memberData.put("email",              email);
            memberData.put("phone_number",       phone);
            memberData.put("membership_status",  stat);
            SanctumApiClient.createMember(memberData).thenAccept(ok -> SwingUtilities.invokeLater(() -> {
                if (ok) { JOptionPane.showMessageDialog(dialog, "Member added successfully!", "Success", JOptionPane.INFORMATION_MESSAGE); dialog.dispose(); loadData(); }
                else    { JOptionPane.showMessageDialog(dialog, "Failed to add member.", "Error", JOptionPane.ERROR_MESSAGE); }
            }));
        });
        cancelBtn.addActionListener(e -> dialog.dispose());
        dialog.setVisible(true);
    }

    // ─── Dashboard Cards ──────────────────────────────────────────────
    private JPanel createRecentAnnouncementsCard() {
        JPanel card = createCard("Recent Announcements", "📢", C_GOLD);
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);
        content.setName("announcementsContent");
        card.add(content, BorderLayout.CENTER);
        return card;
    }

    private JPanel createUpcomingEventsCard() {
        JPanel card = createCard("Upcoming Events", "📅", C_SUCCESS);
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);
        content.setName("eventsContent");
        card.add(content, BorderLayout.CENTER);
        return card;
    }

    private JPanel createMinistryStatsCard() {
        JPanel card = createCard("Ministry Overview", "📊", new Color(100, 150, 255));

        JPanel content = new JPanel(new GridLayout(3, 2, 10, 10));
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(10, 10, 10, 10));

        String[][] stats = {
            {"🎵", "Worship Teams",      "3 active"},
            {"📚", "Bible Studies",      "5 groups"},
            {"🎓", "Youth Ministry",     "42 members"},
            {"👶", "Children's Church",  "28 children"},
            {"🤝", "Outreach Programs",  "2 ongoing"},
            {"💰", "This Month Giving",  "KES 150K"}
        };

        for (String[] stat : stats) {
            content.add(createStatRow(stat[0], stat[1], stat[2]));
        }

        card.add(content, BorderLayout.CENTER);
        return card;
    }

    private JPanel createStatRow(String icon, String label, String value) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setOpaque(false);

        // FIX 10 — emoji icon in stat row uses getEmojiFont (was already correct
        // in the original — kept here for clarity and consistency)
        JLabel iconLabel = new JLabel(icon);
        iconLabel.setFont(getEmojiFont(16));

        JLabel nameLabel = new JLabel(label);
        nameLabel.setFont(F_LABEL);
        nameLabel.setForeground(C_TEXT_MID);

        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(F_MONO_SM);
        valueLabel.setForeground(C_GOLD);
        valueLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        left.setOpaque(false);
        left.add(iconLabel);
        left.add(nameLabel);

        row.add(left,       BorderLayout.WEST);
        row.add(valueLabel, BorderLayout.EAST);
        return row;
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

        // Card title uses an emoji icon — give it the emoji font
        JLabel titleLabel = new JLabel(icon + " " + title);
        titleLabel.setFont(withEmojiFont(F_LABEL));   // FIX — was F_LABEL
        titleLabel.setForeground(C_TEXT);

        header.add(titleLabel, BorderLayout.WEST);
        card.add(header, BorderLayout.NORTH);
        return card;
    }

    private void loadDashboardCardsData() {
        SanctumApiClient.getAnnouncements().thenAccept(announcements -> {
            SwingUtilities.invokeLater(() -> {
                Component[] components = contentArea.getComponents();
                for (Component c : components) {
                    if (c instanceof JPanel) {
                        JPanel p = findCardByName((JPanel) c, "announcementsContent");
                        if (p != null) updateAnnouncementsCard(p, announcements);
                    }
                }
            });
        });
    }

    private JPanel findCardByName(JPanel parent, String name) {
        if (name.equals(parent.getName())) return parent;
        for (Component c : parent.getComponents()) {
            if (c instanceof JPanel) {
                if (name.equals(c.getName())) return (JPanel) c;
                JPanel found = findCardByName((JPanel) c, name);
                if (found != null) return found;
            }
        }
        return null;
    }

    private void updateAnnouncementsCard(JPanel content, java.util.List<Map<String, Object>> announcements) {
        content.removeAll();
        if (announcements.isEmpty()) {
            JLabel noData = new JLabel("No recent announcements");
            noData.setFont(F_MONO_SM);
            noData.setForeground(C_TEXT_DIM);
            noData.setAlignmentX(Component.LEFT_ALIGNMENT);
            content.add(noData);
        } else {
            int count = 0;
            for (Map<String, Object> a : announcements) {
                if (count++ >= 3) break;
                String t = a.getOrDefault("title", "Untitled").toString();
                String d = a.getOrDefault("created_at", "").toString();
                if (d.length() > 10) d = d.substring(0, 10);

                JPanel item = new JPanel(new BorderLayout());
                item.setOpaque(false);
                item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
                item.setBorder(new EmptyBorder(5, 0, 5, 0));

                JLabel titleLabel = new JLabel("• " + t);
                titleLabel.setFont(F_MONO_SM);
                titleLabel.setForeground(C_TEXT);
                titleLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

                JLabel dateLabel = new JLabel(d);
                dateLabel.setFont(F_MONO_SM);
                dateLabel.setForeground(C_TEXT_DIM);

                item.add(titleLabel, BorderLayout.WEST);
                item.add(dateLabel,  BorderLayout.EAST);
                content.add(item);
                content.add(Box.createVerticalStrut(5));
            }
        }
        content.revalidate();
        content.repaint();
    }

    private void updateEventsCard(JPanel content, java.util.List<Map<String, Object>> events) {
        content.removeAll();
        if (events.isEmpty()) {
            JLabel noData = new JLabel("No upcoming events");
            noData.setFont(F_MONO_SM);
            noData.setForeground(C_TEXT_DIM);
            noData.setAlignmentX(Component.LEFT_ALIGNMENT);
            content.add(noData);
        } else {
            int count = 0;
            for (Map<String, Object> ev : events) {
                if (count++ >= 3) break;
                String t = ev.getOrDefault("title", "Untitled").toString();
                String d = ev.getOrDefault("created_at", "").toString();
                if (d.length() > 10) d = d.substring(0, 10);

                JPanel item = new JPanel(new BorderLayout());
                item.setOpaque(false);
                item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
                item.setBorder(new EmptyBorder(5, 0, 5, 0));

                // FIX — "📅" emoji prefix; use emoji font
                JLabel titleLabel = new JLabel("📅 " + t);
                titleLabel.setFont(withEmojiFont(F_MONO_SM));
                titleLabel.setForeground(C_TEXT);

                JLabel dateLabel = new JLabel(d);
                dateLabel.setFont(F_MONO_SM);
                dateLabel.setForeground(C_SUCCESS);

                item.add(titleLabel, BorderLayout.WEST);
                item.add(dateLabel,  BorderLayout.EAST);
                content.add(item);
                content.add(Box.createVerticalStrut(5));
            }
        }
        content.revalidate();
        content.repaint();
    }

    // ─── KPI Card ─────────────────────────────────────────────────────
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

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(F_LABEL);
        titleLabel.setForeground(C_TEXT_MID);

        // KPI icon — already using getEmojiFont (correct in original)
        JLabel iconLabel = new JLabel(icon);
        iconLabel.setFont(getEmojiFont(20));
        iconLabel.setForeground(accent);
        top.add(titleLabel, BorderLayout.WEST);
        top.add(iconLabel,  BorderLayout.EAST);

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

        card.add(top,    BorderLayout.NORTH);
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
        title.setBorder(new EmptyBorder(0, 0, 20, 0));

        // FIX 5 — "➕" prefix — was F_LABEL, now withEmojiFont
        JButton addBtn = styledDialogButton("➕ Add Devotional", C_GOLD);
        addBtn.setFont(withEmojiFont(F_LABEL));
        addBtn.addActionListener(e -> showCreateDevotionalDialog());

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.add(title,  BorderLayout.WEST);
        headerPanel.add(addBtn, BorderLayout.EAST);

        devotionalsGrid = new JPanel(new GridLayout(0, 2, 20, 20));
        devotionalsGrid.setOpaque(false);

        JScrollPane scroll = new JScrollPane(devotionalsGrid);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(BorderFactory.createEmptyBorder());

        panel.add(headerPanel, BorderLayout.NORTH);
        panel.add(scroll,      BorderLayout.CENTER);

        loadDevotionalsGridData(devotionalsGrid);
        return panel;
    }

    private JPanel buildMembersPage() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(30, 30, 30, 30));

        JLabel title = new JLabel("Members Directory");
        title.setFont(F_TITLE);
        title.setForeground(C_TEXT);
        title.setBorder(new EmptyBorder(0, 0, 20, 0));

        // FIX 5 — "➕" prefix — was F_LABEL, now withEmojiFont
        JButton addBtn = styledDialogButton("➕ Add Member", C_GOLD);
        addBtn.setFont(withEmojiFont(F_LABEL));
        addBtn.addActionListener(e -> showCreateMemberDialog());

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.add(title,  BorderLayout.WEST);
        headerPanel.add(addBtn, BorderLayout.EAST);

        membersGrid = new JPanel(new GridLayout(0, 3, 20, 20));
        membersGrid.setOpaque(false);

        JScrollPane scroll = new JScrollPane(membersGrid);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(BorderFactory.createEmptyBorder());

        panel.add(headerPanel, BorderLayout.NORTH);
        panel.add(scroll,      BorderLayout.CENTER);

        loadMembersGridData(membersGrid);
        return panel;
    }

    private JPanel buildAnnouncementsPage() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(30, 30, 30, 30));

        JLabel title = new JLabel("Announcements");
        title.setFont(F_TITLE);
        title.setForeground(C_TEXT);
        title.setBorder(new EmptyBorder(0, 0, 30, 0));

        JPanel AnnouncementsPanel = new JPanel();
        AnnouncementsPanel.setLayout(new BoxLayout(AnnouncementsPanel, BoxLayout.Y_AXIS));
        AnnouncementsPanel.setOpaque(false);

        JScrollPane scroll = new JScrollPane(AnnouncementsPanel);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);

        panel.add(title,  BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);

        loadAnnouncementsData(AnnouncementsPanel);
        return panel;
    }

    // ─── Devotionals Grid ─────────────────────────────────────────────
    private void loadDevotionalsGridData(JPanel grid) {
        System.out.println("Loading devotionals data...");
        SanctumApiClient.getDevotionals().thenAccept(devotionals -> {
            SwingUtilities.invokeLater(() -> {
                System.out.println("Received " + devotionals.size() + " devotionals");
                grid.removeAll();
                if (devotionals.isEmpty()) {
                    grid.add(createEmptyStatePanel("No devotionals available", "📖",
                             "Click 'Add Devotional' to create your first devotional"));
                } else {
                    for (Map<String, Object> d : devotionals) {
                        grid.add(createDevotionalGridCard(d));
                    }
                }
                grid.revalidate();
                grid.repaint();
            });
        }).exceptionally(ex -> {
            System.err.println("Error loading devotionals: " + ex.getMessage());
            SwingUtilities.invokeLater(() -> {
                grid.removeAll();
                grid.add(createErrorPanel("Failed to load devotionals", "📖"));
                grid.revalidate();
                grid.repaint();
            });
            return null;
        });
    }

    private JPanel createDevotionalGridCard(Map<String, Object> devotional) {
        JPanel card = new JPanel(new BorderLayout(0, 12));
        card.setOpaque(true);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(C_BORDER, 1),
            new EmptyBorder(20, 20, 20, 20)
        ));
        card.setBackground(C_CARD);
        card.setPreferredSize(new Dimension(300, 200));
        card.setMaximumSize(new Dimension(300, 200));

        String title     = safeStr(devotional, "title",              "Untitled");
        String date      = safeStr(devotional, "date",               "");
        String author    = safeStr(devotional, "author",             "Unknown");
        String scripture = safeStr(devotional, "scripture_reference","");

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(F_LABEL);
        titleLabel.setForeground(C_TEXT);

        JLabel dateLabel = new JLabel(date);
        dateLabel.setFont(F_MONO_SM);
        dateLabel.setForeground(C_TEXT_DIM);

        headerPanel.add(titleLabel, BorderLayout.CENTER);
        headerPanel.add(dateLabel,  BorderLayout.EAST);

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setOpaque(false);

        if (!scripture.isEmpty()) {
            // FIX — "📜" emoji prefix
            JLabel scriptureLabel = new JLabel("📜 " + scripture);
            scriptureLabel.setFont(withEmojiFont(F_MONO_SM));
            scriptureLabel.setForeground(C_GOLD);
            scriptureLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            contentPanel.add(scriptureLabel);
            contentPanel.add(Box.createVerticalStrut(8));
        }

        // FIX — "✍️" emoji prefix
        JLabel authorLabel = new JLabel("✍️ " + author);
        authorLabel.setFont(withEmojiFont(F_MONO_SM));
        authorLabel.setForeground(C_TEXT_DIM);
        authorLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(authorLabel);

        int    reactionsCount = parseSafely(devotional.getOrDefault("reactions_count", "0"));
        String userReaction   = devotional.get("user_reaction") != null
                                ? devotional.get("user_reaction").toString() : "";

        JPanel reactionsPanel = createReactionsPanel(devotional, reactionsCount, userReaction);
        reactionsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(Box.createVerticalStrut(12));
        contentPanel.add(reactionsPanel);

        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actionsPanel.setOpaque(false);

        // FIX 6 — view/edit buttons: withEmojiFont so "👁️" and "✏️" show
        JButton viewBtn = new JButton("👁️ View");
        viewBtn.setBackground(C_SUCCESS);
        viewBtn.setForeground(C_TEXT);
        viewBtn.setFocusPainted(false);
        viewBtn.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        viewBtn.setFont(withEmojiFont(F_MONO_SM));   // FIX — was F_MONO_SM
        viewBtn.addActionListener(e -> showDevotionalDetails(devotional));

        JButton editBtn = new JButton("✏️ Edit");
        editBtn.setBackground(C_GOLD);
        editBtn.setForeground(C_TEXT);
        editBtn.setFocusPainted(false);
        editBtn.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        editBtn.setFont(withEmojiFont(F_MONO_SM));   // FIX — was F_MONO_SM

        actionsPanel.add(viewBtn);
        actionsPanel.add(editBtn);

        card.add(headerPanel,   BorderLayout.NORTH);
        card.add(contentPanel,  BorderLayout.CENTER);
        card.add(actionsPanel,  BorderLayout.SOUTH);

        card.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { card.setBackground(C_SURFACE); card.repaint(); }
            @Override public void mouseExited(MouseEvent e)  { card.setBackground(C_CARD);    card.repaint(); }
        });

        return card;
    }

    private JPanel createReactionsPanel(Map<String, Object> devotional, int reactionsCount, String userReaction) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        panel.setOpaque(false);

        String[] reactions = {"❤️", "🙏", "👍", "🔥", "🎉"};

        for (String reaction : reactions) {
            JButton reactionBtn = new JButton(reaction);
            reactionBtn.setBackground(C_SURFACE);
            reactionBtn.setForeground(C_TEXT);
            reactionBtn.setFocusPainted(false);
            reactionBtn.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
            // Reaction buttons already used getEmojiFont(14) — correct
            reactionBtn.setFont(getEmojiFont(14));
            reactionBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            if (reaction.equals(userReaction)) {
                reactionBtn.setBackground(C_GOLD_DIM);
            }

            reactionBtn.addActionListener(e -> {
                if (reactionBtn.getBackground().equals(C_GOLD_DIM)) {
                    reactionBtn.setBackground(C_SURFACE);
                } else {
                    reactionBtn.setBackground(C_GOLD_DIM);
                }
                System.out.println("Reaction clicked: " + reaction + " for devotional: " + devotional.get("id"));
            });

            panel.add(reactionBtn);
        }

        JLabel countLabel = new JLabel(reactionsCount + " reactions");
        countLabel.setFont(F_MONO_SM);
        countLabel.setForeground(C_TEXT_DIM);
        panel.add(countLabel);

        return panel;
    }

    // ─── Members Grid ─────────────────────────────────────────────────
    private void loadMembersGridData(JPanel grid) {
        System.out.println("Loading members data...");
        SanctumApiClient.getMembers().thenAccept(members -> {
            SwingUtilities.invokeLater(() -> {
                System.out.println("Received " + members.size() + " members");
                grid.removeAll();
                if (members.isEmpty()) {
                    grid.add(createEmptyStatePanel("No members available", "👥",
                             "Click 'Add Member' to register your first member"));
                } else {
                    for (Map<String, Object> m : members) {
                        grid.add(createMemberGridCard(m));
                    }
                }
                grid.revalidate();
                grid.repaint();
            });
        }).exceptionally(ex -> {
            System.err.println("Error loading members: " + ex.getMessage());
            SwingUtilities.invokeLater(() -> {
                grid.removeAll();
                grid.add(createErrorPanel("Failed to load members", "👥"));
                grid.revalidate();
                grid.repaint();
            });
            return null;
        });
    }

    private JPanel createMemberGridCard(Map<String, Object> member) {
        JPanel card = new JPanel(new BorderLayout(0, 12));
        card.setOpaque(true);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(C_BORDER, 1),
            new EmptyBorder(20, 20, 20, 20)
        ));
        card.setBackground(C_CARD);
        card.setPreferredSize(new Dimension(280, 180));
        card.setMaximumSize(new Dimension(280, 180));

        String firstName = safeStr(member, "first_name", "Unknown");
        String lastName  = safeStr(member, "last_name",  "");
        String fullName  = lastName.isEmpty() ? firstName : firstName + " " + lastName;
        String initials  = (firstName.isEmpty() ? "?" : firstName.substring(0, 1))
                         + (lastName.isEmpty()  ? ""  : lastName.substring(0, 1).toUpperCase());

        JPanel headerPanel = new JPanel(new BorderLayout(8, 0));
        headerPanel.setOpaque(false);

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
        avatar.setPreferredSize(new Dimension(50, 50));
        avatar.setOpaque(false);

        JLabel avatarLabel = new JLabel(initials);
        avatarLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        avatarLabel.setForeground(C_TEXT);
        avatarLabel.setHorizontalAlignment(SwingConstants.CENTER);
        avatarLabel.setVerticalAlignment(SwingConstants.CENTER);
        avatar.add(avatarLabel, BorderLayout.CENTER);

        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setOpaque(false);

        JLabel nameLabel = new JLabel(fullName);
        nameLabel.setFont(F_LABEL);
        nameLabel.setForeground(C_TEXT);

        String status = safeStr(member, "membership_status", "new_member");

        // FIX — "🟢" emoji prefix in status label
        JLabel statusLabel = new JLabel("🟢 " + status.replace("_", " ").toUpperCase());
        statusLabel.setFont(withEmojiFont(F_MONO_SM));   // FIX — was F_MONO_SM
        statusLabel.setForeground(C_SUCCESS);

        infoPanel.add(nameLabel);
        infoPanel.add(Box.createVerticalStrut(5));
        infoPanel.add(statusLabel);

        headerPanel.add(avatar,    BorderLayout.WEST);
        headerPanel.add(infoPanel, BorderLayout.CENTER);

        JPanel contactPanel = new JPanel();
        contactPanel.setLayout(new BoxLayout(contactPanel, BoxLayout.Y_AXIS));
        contactPanel.setOpaque(false);

        String email = safeStr(member, "email",        "");
        String phone = safeStr(member, "phone_number", "");

        if (!email.isEmpty()) {
            // FIX — "📧" emoji prefix
            JLabel emailLabel = new JLabel("📧 " + email);
            emailLabel.setFont(withEmojiFont(F_MONO_SM));  // FIX — was F_MONO_SM
            emailLabel.setForeground(C_TEXT_DIM);
            contactPanel.add(emailLabel);
        }

        if (!phone.isEmpty()) {
            // FIX — "📱" emoji prefix
            JLabel phoneLabel = new JLabel("📱 " + phone);
            phoneLabel.setFont(withEmojiFont(F_MONO_SM));  // FIX — was F_MONO_SM
            phoneLabel.setForeground(C_TEXT_DIM);
            contactPanel.add(phoneLabel);
        }

        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actionsPanel.setOpaque(false);

        // FIX 7 — view/contact buttons: withEmojiFont so "👁️" and "💬" show
        JButton viewBtn = new JButton("👁️ View");
        viewBtn.setBackground(C_SUCCESS);
        viewBtn.setForeground(C_TEXT);
        viewBtn.setFocusPainted(false);
        viewBtn.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        viewBtn.setFont(withEmojiFont(F_MONO_SM));   // FIX — was F_MONO_SM

        JButton contactBtn = new JButton("💬 Contact");
        contactBtn.setBackground(C_GOLD);
        contactBtn.setForeground(C_TEXT);
        contactBtn.setFocusPainted(false);
        contactBtn.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        contactBtn.setFont(withEmojiFont(F_MONO_SM)); // FIX — was F_MONO_SM

        actionsPanel.add(viewBtn);
        actionsPanel.add(contactBtn);

        card.add(headerPanel,   BorderLayout.NORTH);
        card.add(contactPanel,  BorderLayout.CENTER);
        card.add(actionsPanel,  BorderLayout.SOUTH);

        card.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { card.setBackground(C_SURFACE); card.repaint(); }
            @Override public void mouseExited(MouseEvent e)  { card.setBackground(C_CARD);    card.repaint(); }
        });

        return card;
    }

    // ─── State Panels ─────────────────────────────────────────────────
    private JPanel createEmptyStatePanel(String message, String icon, String subtext) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(40, 20, 40, 20));

        // Large decorative emoji — must use getEmojiFont
        JLabel iconLabel = new JLabel(icon);
        iconLabel.setFont(getEmojiFont(48));
        iconLabel.setForeground(C_TEXT_DIM);
        iconLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JLabel messageLabel = new JLabel(message);
        messageLabel.setFont(F_LABEL);
        messageLabel.setForeground(C_TEXT_DIM);
        messageLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JLabel subtextLabel = new JLabel(subtext);
        subtextLabel.setFont(F_MONO_SM);
        subtextLabel.setForeground(C_TEXT_DIM);
        subtextLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);
        textPanel.add(Box.createVerticalStrut(20));
        textPanel.add(messageLabel);
        textPanel.add(Box.createVerticalStrut(10));
        textPanel.add(subtextLabel);

        panel.add(iconLabel,  BorderLayout.NORTH);
        panel.add(textPanel,  BorderLayout.CENTER);
        return panel;
    }

    private JPanel createErrorPanel(String message, String icon) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(40, 20, 40, 20));

        JLabel iconLabel = new JLabel(icon);
        iconLabel.setFont(getEmojiFont(48));
        iconLabel.setForeground(C_DANGER);
        iconLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JLabel messageLabel = new JLabel(message);
        messageLabel.setFont(F_LABEL);
        messageLabel.setForeground(C_DANGER);
        messageLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);
        textPanel.add(Box.createVerticalStrut(20));
        textPanel.add(messageLabel);

        panel.add(iconLabel,  BorderLayout.NORTH);
        panel.add(textPanel,  BorderLayout.CENTER);
        return panel;
    }

    // ─── Devotional Detail Dialog ─────────────────────────────────────
    private void showDevotionalDetails(Map<String, Object> devotional) {
        JDialog dialog = new JDialog(this, "Devotional Details", true);
        dialog.setSize(600, 500);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new BorderLayout(0, 20));
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        panel.setBackground(C_CARD);

        String dTitle    = safeStr(devotional, "title",              "Untitled");
        String scripture = safeStr(devotional, "scripture_reference","");
        String date      = safeStr(devotional, "created_at",         "");
        if (date.length() > 10) date = date.substring(0, 10);

        // FIX — emoji in detail header labels
        JLabel titleLabel = new JLabel("📖 " + dTitle);
        titleLabel.setFont(withEmojiFont(F_TITLE));
        titleLabel.setForeground(C_TEXT);

        JLabel scriptureLabel = new JLabel("📜 " + scripture);
        scriptureLabel.setFont(withEmojiFont(F_LABEL));
        scriptureLabel.setForeground(C_GOLD);

        JLabel dateLabel = new JLabel("📅 " + date);
        dateLabel.setFont(withEmojiFont(F_MONO_SM));
        dateLabel.setForeground(C_TEXT_DIM);

        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setOpaque(false);
        headerPanel.add(titleLabel);
        headerPanel.add(Box.createVerticalStrut(5));
        headerPanel.add(scriptureLabel);
        headerPanel.add(Box.createVerticalStrut(5));
        headerPanel.add(dateLabel);

        String content = safeStr(devotional, "content", "No content available.");
        JTextArea contentArea = new JTextArea(content);
        contentArea.setBackground(C_SURFACE);
        contentArea.setForeground(C_TEXT);
        contentArea.setCaretColor(C_TEXT);
        contentArea.setFont(F_LABEL);
        contentArea.setLineWrap(true);
        contentArea.setWrapStyleWord(true);
        contentArea.setEditable(false);
        contentArea.setBorder(new EmptyBorder(10, 10, 10, 10));

        JScrollPane scrollPane = new JScrollPane(contentArea);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(BorderFactory.createLineBorder(C_BORDER));

        JButton closeBtn = styledDialogButton("Close", C_GOLD);
        closeBtn.addActionListener(e -> dialog.dispose());

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setOpaque(false);
        buttonPanel.add(closeBtn);

        panel.add(headerPanel, BorderLayout.NORTH);
        panel.add(scrollPane,  BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        dialog.add(panel);
        dialog.setVisible(true);
    }

    // ─── Announcements ──────────────────────────────────────────────
    private void loadAnnouncementsData(JPanel AnnouncementsPanel) {
        SanctumApiClient.getAnnouncements().thenAccept(announcements -> {
            SwingUtilities.invokeLater(() -> {
                AnnouncementsPanel.removeAll();
                if (announcements.isEmpty()) {
                    AnnouncementsPanel.add(createEmptyStatePanel("No Announcements available", "🙏",
                                   "No Announcements at this time"));
                } else {
                    for (Map<String, Object> Announcements : announcements) {
                        JPanel item = new JPanel(new BorderLayout());
                        item.setOpaque(false);
                        item.setBorder(new EmptyBorder(10, 20, 10, 20));

                        String request = safeStr(Announcements, "title", "Untitled");
                        String date    = safeStr(Announcements, "created_at", "");
                        if (date.length() > 10) date = date.substring(0, 10);

                        // FIX — "🙏" emoji prefix
                        JLabel label = new JLabel("🙏 " + request + (date.isEmpty() ? "" : " (" + date + ")"));
                        label.setFont(withEmojiFont(F_LABEL));  // FIX — was F_LABEL
                        label.setForeground(C_TEXT_MID);

                        item.add(label, BorderLayout.CENTER);
                        AnnouncementsPanel.add(item);
                        AnnouncementsPanel.add(Box.createVerticalStrut(5));
                    }
                }
                AnnouncementsPanel.revalidate();
                AnnouncementsPanel.repaint();
            });
        }).exceptionally(ex -> {
            SwingUtilities.invokeLater(() -> {
                AnnouncementsPanel.removeAll();
                AnnouncementsPanel.add(createErrorPanel("Failed to load Announcements", "🙏"));
                AnnouncementsPanel.revalidate();
                AnnouncementsPanel.repaint();
            });
            return null;
        });
    }

    // ─── Helpers ──────────────────────────────────────────────────────

    /**
     * FIX 3 — createStyledButton now uses withEmojiFont(F_LABEL) so that
     * any symbol in the button text (─, ✕, or an emoji) renders correctly.
     */
    private JButton createStyledButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(withEmojiFont(F_LABEL));   // FIX — was F_LABEL
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

    // ─── Dialog Field Factories (DRY helpers) ─────────────────────────
    /** Themed text field used in all dialogs. */
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

    /**
     * Null-safe map string getter — avoids repeated
     * getOrDefault(...).toString() + null-check patterns.
     */
    private String safeStr(Map<String, Object> map, String key, String defaultVal) {
        Object v = map.get(key);
        return (v != null) ? v.toString() : defaultVal;
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
        setKpiLabels("Loading...");

        SanctumApiClient.getDashboardData().thenAccept(data -> SwingUtilities.invokeLater(() -> {
            if (data != null && !data.isEmpty()) {
                int totalMembers      = parseSafely(data.getOrDefault("total_members",       "0"));
                int devotionalsCount  = parseSafely(data.getOrDefault("devotionals_count",   "0"));
                int Announcements    = parseSafely(data.getOrDefault("Announcements_requests",     "0"));
                int counselingSess    = parseSafely(data.getOrDefault("counseling_sessions", "0"));

                if (totalMembers == 0) {
                    SanctumApiClient.getMembers().thenAccept(members ->
                        SwingUtilities.invokeLater(() -> lblTotalMembers.setText(String.valueOf(members.size()))));
                } else {
                    lblTotalMembers.setText(String.valueOf(totalMembers));
                }

                SanctumApiClient.getDevotionals().thenAccept(devotionals ->
                    SwingUtilities.invokeLater(() -> lblDevotionals.setText(String.valueOf(devotionals.size()))));

                SanctumApiClient.getAnnouncements().thenAccept(announcements ->
                    SwingUtilities.invokeLater(() -> lblAnnouncements.setText(String.valueOf(announcements.size()))));

                SanctumApiClient.getStaff().thenAccept(staff ->
                    SwingUtilities.invokeLater(() -> lblCounselingSessions.setText(String.valueOf(staff.size()))));

                System.out.println("Pastor dashboard data loaded successfully.");
            } else {
                loadIndividualKpiData();
            }
        })).exceptionally(ex -> {
            SwingUtilities.invokeLater(() -> {
                System.err.println("Failed to load dashboard data: " + ex.getMessage());
                loadIndividualKpiData();
            });
            return null;
        });
    }

    private void loadIndividualKpiData() {
        SanctumApiClient.getMembers().thenAccept(members ->
            SwingUtilities.invokeLater(() -> lblTotalMembers.setText(String.valueOf(members.size()))));

        SanctumApiClient.getDevotionals().thenAccept(devotionals ->
            SwingUtilities.invokeLater(() -> lblDevotionals.setText(String.valueOf(devotionals.size()))));

        SanctumApiClient.getAnnouncements().thenAccept(announcements ->
            SwingUtilities.invokeLater(() -> lblAnnouncements.setText(String.valueOf(announcements.size()))));

        SanctumApiClient.getStaff().thenAccept(staff ->
            SwingUtilities.invokeLater(() -> lblCounselingSessions.setText(String.valueOf(staff.size()))));
    }

    private int parseSafely(Object value) {
        try {
            if (value instanceof Number) return ((Number) value).intValue();
            if (value instanceof String) return Integer.parseInt(((String) value).replaceAll("[^0-9-]", ""));
            return 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private void setKpiLabels(String text) {
        if (lblTotalMembers      != null) lblTotalMembers.setText(text);
        if (lblDevotionals       != null) lblDevotionals.setText(text);
        if (lblAnnouncements    != null) lblAnnouncements.setText(text);
        if (lblCounselingSessions != null) lblCounselingSessions.setText(text);
    }

    // ─── Entry Point ──────────────────────────────────────────────────
    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        SwingUtilities.invokeLater(() -> new PastorDashboardFrame().setVisible(true));
    }
}