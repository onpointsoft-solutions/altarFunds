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
        {"🙏", "Prayer"}
    };

    // ─── UI State ─────────────────────────────────────────────────────
    private final CardLayout cardLayout = new CardLayout();
    private JPanel contentArea;
    private JPanel sidebar;
    private String activeMenu = "Overview";
    
    // Grid panel references for data reloading
    private JPanel devotionalsGrid;
    private JPanel membersGrid;

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
        System.out.println("Switching to: " + menu);
        
        // Reload data when switching to specific pages
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

        // Header
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
        
        // KPI cards row - 4 cards
        JPanel kpiRow = new JPanel(new GridLayout(1, 4, 15, 0));
        kpiRow.setOpaque(false);
        kpiRow.setBorder(new EmptyBorder(0, 0, 20, 0));

        JLabel[] kpi = new JLabel[4];
        kpiRow.add(buildKpiCard("Total Members", "0", "Active members", C_GOLD, "👥", kpi, 0));
        kpiRow.add(buildKpiCard("Devotionals", "0", "This week", C_SUCCESS, "📖", kpi, 1));
        kpiRow.add(buildKpiCard("Prayer Requests", "0", "Pending", C_TEXT_MID, "🙏", kpi, 2));
        kpiRow.add(buildKpiCard("Counseling", "0", "Sessions", new Color(100, 150, 255), "💬", kpi, 3));

        lblTotalMembers = kpi[0];
        lblDevotionals = kpi[1];
        lblPrayerRequests = kpi[2];
        lblCounselingSessions = kpi[3];

        // Dashboard content with cards grid
        JPanel contentGrid = new JPanel(new GridLayout(1, 2, 20, 20));
        contentGrid.setOpaque(false);
        
        // Quick Actions Card
        contentGrid.add(createQuickActionsCard());
        
        // Recent Announcements Card
        contentGrid.add(createRecentAnnouncementsCard());

        // Main content area
        JPanel center = new JPanel(new BorderLayout(0, 20));
        center.setOpaque(false);
        center.add(kpiRow, BorderLayout.NORTH);
        center.add(contentGrid, BorderLayout.CENTER);

        main.add(header, BorderLayout.NORTH);
        main.add(center, BorderLayout.CENTER);
        
        // Load data for all cards
        loadDashboardCardsData();
        
        return main;
    }
    
    private JPanel createQuickActionsCard() {
        JPanel card = createCard("Quick Actions", "⚡", C_GOLD);
        JPanel content = new JPanel(new GridLayout(2, 2, 10, 10));
        content.setOpaque(false);
        
        String[][] actions = {
            {"📢", "New Announcement", "announcement"},
            {"📅", "Schedule Event", "event"},
            {"🙏", "Add Devotional", "devotional"},
            {"👥", "Add Member", "member"}
        };
        
        for (String[] action : actions) {
            content.add(createActionButton(action[0], action[1], action[2]));
        }
        
        card.add(content, BorderLayout.CENTER);
        return card;
    }
    
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
        btn.setFont(F_LABEL);
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
            case "announcement":
                showCreateAnnouncementDialog();
                break;
            case "event":
                showCreateEventDialog();
                break;
            case "devotional":
                showCreateDevotionalDialog();
                break;
            case "member":
                showCreateMemberDialog();
                break;
        }
    }
    
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
        
        JTextField titleField = new JTextField(20);
        titleField.setBackground(C_SURFACE);
        titleField.setForeground(C_TEXT);
        titleField.setCaretColor(C_TEXT);
        titleField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(C_BORDER),
            new EmptyBorder(5, 8, 5, 8)
        ));
        
        JTextArea contentArea = new JTextArea(8, 20);
        contentArea.setBackground(C_SURFACE);
        contentArea.setForeground(C_TEXT);
        contentArea.setCaretColor(C_TEXT);
        contentArea.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(C_BORDER),
            new EmptyBorder(5, 8, 5, 8)
        ));
        contentArea.setLineWrap(true);
        contentArea.setWrapStyleWord(true);
        
        JComboBox<String> priorityCombo = new JComboBox<>(new String[]{"Normal", "High", "Urgent"});
        priorityCombo.setBackground(C_SURFACE);
        priorityCombo.setForeground(C_TEXT);
        
        JButton createBtn = new JButton("Create Announcement");
        createBtn.setBackground(C_GOLD);
        createBtn.setForeground(C_TEXT);
        createBtn.setFocusPainted(false);
        createBtn.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        
        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.setBackground(C_DANGER);
        cancelBtn.setForeground(C_TEXT);
        cancelBtn.setFocusPainted(false);
        cancelBtn.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        
        gc.gridx = 0; gc.gridy = 0; gc.anchor = GridBagConstraints.WEST;
        panel.add(new JLabel("Title:") {{ setForeground(C_TEXT); setFont(F_LABEL); }}, gc);
        gc.gridx = 1; gc.gridy = 0; gc.weightx = 1;
        panel.add(titleField, gc);
        
        gc.gridx = 0; gc.gridy = 1; gc.weightx = 0; gc.anchor = GridBagConstraints.NORTHWEST;
        panel.add(new JLabel("Content:") {{ setForeground(C_TEXT); setFont(F_LABEL); }}, gc);
        gc.gridx = 1; gc.gridy = 1; gc.weightx = 1; gc.fill = GridBagConstraints.BOTH;
        panel.add(new JScrollPane(contentArea), gc);
        
        gc.gridx = 0; gc.gridy = 2; gc.weightx = 0; gc.fill = GridBagConstraints.HORIZONTAL; gc.anchor = GridBagConstraints.WEST;
        panel.add(new JLabel("Priority:") {{ setForeground(C_TEXT); setFont(F_LABEL); }}, gc);
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
            String title = titleField.getText().trim();
            String content = contentArea.getText().trim();
            String priority = priorityCombo.getSelectedItem().toString().toLowerCase();
            
            if (title.isEmpty() || content.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Title and content are required.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            SanctumApiClient.createAnnouncement(title, content, priority).thenAccept(success -> {
                SwingUtilities.invokeLater(() -> {
                    if (success) {
                        JOptionPane.showMessageDialog(dialog, "Announcement created successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                        dialog.dispose();
                        // Refresh announcements
                        loadDashboardCardsData();
                    } else {
                        JOptionPane.showMessageDialog(dialog, "Failed to create announcement.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                });
            });
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
        
        JTextField titleField = new JTextField(20);
        titleField.setBackground(C_SURFACE);
        titleField.setForeground(C_TEXT);
        titleField.setCaretColor(C_TEXT);
        titleField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(C_BORDER),
            new EmptyBorder(5, 8, 5, 8)
        ));
        
        JTextField scriptureField = new JTextField(20);
        scriptureField.setBackground(C_SURFACE);
        scriptureField.setForeground(C_TEXT);
        scriptureField.setCaretColor(C_TEXT);
        scriptureField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(C_BORDER),
            new EmptyBorder(5, 8, 5, 8)
        ));
        
        JTextArea contentArea = new JTextArea(10, 20);
        contentArea.setBackground(C_SURFACE);
        contentArea.setForeground(C_TEXT);
        contentArea.setCaretColor(C_TEXT);
        contentArea.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(C_BORDER),
            new EmptyBorder(5, 8, 5, 8)
        ));
        contentArea.setLineWrap(true);
        contentArea.setWrapStyleWord(true);
        
        JButton createBtn = new JButton("Create Devotional");
        createBtn.setBackground(C_GOLD);
        createBtn.setForeground(C_TEXT);
        createBtn.setFocusPainted(false);
        createBtn.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        
        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.setBackground(C_DANGER);
        cancelBtn.setForeground(C_TEXT);
        cancelBtn.setFocusPainted(false);
        cancelBtn.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        
        gc.gridx = 0; gc.gridy = 0; gc.anchor = GridBagConstraints.WEST;
        panel.add(new JLabel("Title:") {{ setForeground(C_TEXT); setFont(F_LABEL); }}, gc);
        gc.gridx = 1; gc.gridy = 0; gc.weightx = 1;
        panel.add(titleField, gc);
        
        gc.gridx = 0; gc.gridy = 1; gc.weightx = 0; gc.anchor = GridBagConstraints.WEST;
        panel.add(new JLabel("Scripture Reference:") {{ setForeground(C_TEXT); setFont(F_LABEL); }}, gc);
        gc.gridx = 1; gc.gridy = 1; gc.weightx = 1;
        panel.add(scriptureField, gc);
        
        gc.gridx = 0; gc.gridy = 2; gc.weightx = 0; gc.anchor = GridBagConstraints.NORTHWEST;
        panel.add(new JLabel("Content:") {{ setForeground(C_TEXT); setFont(F_LABEL); }}, gc);
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
            String title = titleField.getText().trim();
            String scripture = scriptureField.getText().trim();
            String content = contentArea.getText().trim();
            
            if (title.isEmpty() || content.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Title and content are required.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            SanctumApiClient.createDevotional(title, content, scripture).thenAccept(success -> {
                SwingUtilities.invokeLater(() -> {
                    if (success) {
                        JOptionPane.showMessageDialog(dialog, "Devotional created successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                        dialog.dispose();
                        // Refresh devotionals
                        loadData();
                    } else {
                        JOptionPane.showMessageDialog(dialog, "Failed to create devotional.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                });
            });
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
        
        JTextField firstNameField = new JTextField(20);
        firstNameField.setBackground(C_SURFACE);
        firstNameField.setForeground(C_TEXT);
        firstNameField.setCaretColor(C_TEXT);
        firstNameField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(C_BORDER),
            new EmptyBorder(5, 8, 5, 8)
        ));
        
        JTextField lastNameField = new JTextField(20);
        lastNameField.setBackground(C_SURFACE);
        lastNameField.setForeground(C_TEXT);
        lastNameField.setCaretColor(C_TEXT);
        lastNameField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(C_BORDER),
            new EmptyBorder(5, 8, 5, 8)
        ));
        
        JTextField emailField = new JTextField(20);
        emailField.setBackground(C_SURFACE);
        emailField.setForeground(C_TEXT);
        emailField.setCaretColor(C_TEXT);
        emailField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(C_BORDER),
            new EmptyBorder(5, 8, 5, 8)
        ));
        
        JTextField phoneField = new JTextField(20);
        phoneField.setBackground(C_SURFACE);
        phoneField.setForeground(C_TEXT);
        phoneField.setCaretColor(C_TEXT);
        phoneField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(C_BORDER),
            new EmptyBorder(5, 8, 5, 8)
        ));
        
        JComboBox<String> statusCombo = new JComboBox<>(new String[]{"new_member", "active_member", "inactive_member"});
        statusCombo.setBackground(C_SURFACE);
        statusCombo.setForeground(C_TEXT);
        
        JButton createBtn = new JButton("Add Member");
        createBtn.setBackground(C_GOLD);
        createBtn.setForeground(C_TEXT);
        createBtn.setFocusPainted(false);
        createBtn.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        
        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.setBackground(C_DANGER);
        cancelBtn.setForeground(C_TEXT);
        cancelBtn.setFocusPainted(false);
        cancelBtn.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        
        // Form fields
        gc.gridx = 0; gc.gridy = 0; gc.anchor = GridBagConstraints.WEST;
        panel.add(new JLabel("First Name:") {{ setForeground(C_TEXT); setFont(F_LABEL); }}, gc);
        gc.gridx = 1; gc.gridy = 0; gc.weightx = 1;
        panel.add(firstNameField, gc);
        
        gc.gridx = 0; gc.gridy = 1; gc.weightx = 0; gc.anchor = GridBagConstraints.WEST;
        panel.add(new JLabel("Last Name:") {{ setForeground(C_TEXT); setFont(F_LABEL); }}, gc);
        gc.gridx = 1; gc.gridy = 1; gc.weightx = 1;
        panel.add(lastNameField, gc);
        
        gc.gridx = 0; gc.gridy = 2; gc.weightx = 0; gc.anchor = GridBagConstraints.WEST;
        panel.add(new JLabel("Email:") {{ setForeground(C_TEXT); setFont(F_LABEL); }}, gc);
        gc.gridx = 1; gc.gridy = 2; gc.weightx = 1;
        panel.add(emailField, gc);
        
        gc.gridx = 0; gc.gridy = 3; gc.weightx = 0; gc.anchor = GridBagConstraints.WEST;
        panel.add(new JLabel("Phone:") {{ setForeground(C_TEXT); setFont(F_LABEL); }}, gc);
        gc.gridx = 1; gc.gridy = 3; gc.weightx = 1;
        panel.add(phoneField, gc);
        
        gc.gridx = 0; gc.gridy = 4; gc.weightx = 0; gc.fill = GridBagConstraints.HORIZONTAL; gc.anchor = GridBagConstraints.WEST;
        panel.add(new JLabel("Status:") {{ setForeground(C_TEXT); setFont(F_LABEL); }}, gc);
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
            String firstName = firstNameField.getText().trim();
            String lastName = lastNameField.getText().trim();
            String email = emailField.getText().trim();
            String phone = phoneField.getText().trim();
            String status = statusCombo.getSelectedItem().toString();
            
            if (firstName.isEmpty() || email.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "First name and email are required.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            Map<String, Object> memberData = new HashMap<>();
            memberData.put("first_name", firstName);
            memberData.put("last_name", lastName);
            memberData.put("email", email);
            memberData.put("phone_number", phone);
            memberData.put("membership_status", status);
            
            SanctumApiClient.createMember(memberData).thenAccept(success -> {
                SwingUtilities.invokeLater(() -> {
                    if (success) {
                        JOptionPane.showMessageDialog(dialog, "Member added successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                        dialog.dispose();
                        // Refresh members data
                        loadData();
                    } else {
                        JOptionPane.showMessageDialog(dialog, "Failed to add member.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                });
            });
        });
        
        cancelBtn.addActionListener(e -> dialog.dispose());
        
        dialog.setVisible(true);
    }
    
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
            {"🎵", "Worship Teams", "3 active"},
            {"📚", "Bible Studies", "5 groups"},
            {"🎓", "Youth Ministry", "42 members"},
            {"👶", "Children's Church", "28 children"},
            {"🤝", "Outreach Programs", "2 ongoing"},
            {"💰", "This Month Giving", "KES 150K"}
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
        
        JLabel iconLabel = new JLabel(icon);
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 16));
        
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
        
        row.add(left, BorderLayout.WEST);
        row.add(valueLabel, BorderLayout.EAST);
        
        return row;
    }
    
    private JPanel createCard(String title, String icon, Color accentColor) {
        JPanel card = new JPanel(new BorderLayout(0, 10)) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Card background with gradient
                GradientPaint gradient = new GradientPaint(
                    0, 0, C_CARD,
                    0, getHeight(), C_SURFACE
                );
                g2.setPaint(gradient);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                
                // Accent border at top
                g2.setColor(accentColor);
                g2.fillRoundRect(0, 0, getWidth(), 4, 4, 4);
                
                // Side accent
                g2.fillRoundRect(0, 0, 4, getHeight(), 4, 4);
                
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(15, 20, 15, 20));
        
        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        
        JLabel titleLabel = new JLabel(icon + " " + title);
        titleLabel.setFont(F_LABEL);
        titleLabel.setForeground(C_TEXT);
        
        header.add(titleLabel, BorderLayout.WEST);
        card.add(header, BorderLayout.NORTH);
        
        return card;
    }
    
    private void loadDashboardCardsData() {
        // Load announcements for the announcements card
        SanctumApiClient.getAnnouncements().thenAccept(announcements -> {
            SwingUtilities.invokeLater(() -> {
                // Find the announcements content panel
                Component[] components = contentArea.getComponents();
                for (Component c : components) {
                    if (c instanceof JPanel) {
                        JPanel card = findCardByName((JPanel) c, "announcementsContent");
                        if (card != null) {
                            updateAnnouncementsCard(card, announcements);
                        }
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
            for (Map<String, Object> announcement : announcements) {
                if (count >= 3) break; // Show only 3 recent
                String title = announcement.getOrDefault("title", "Untitled").toString();
                String date = announcement.getOrDefault("created_at", "").toString();
                if (date.length() > 10) date = date.substring(0, 10);
                
                JPanel item = new JPanel(new BorderLayout());
                item.setOpaque(false);
                item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
                item.setBorder(new EmptyBorder(5, 0, 5, 0));
                
                JLabel titleLabel = new JLabel("• " + title);
                titleLabel.setFont(F_MONO_SM);
                titleLabel.setForeground(C_TEXT);
                titleLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                
                JLabel dateLabel = new JLabel(date);
                dateLabel.setFont(F_MONO_SM);
                dateLabel.setForeground(C_TEXT_DIM);
                
                item.add(titleLabel, BorderLayout.WEST);
                item.add(dateLabel, BorderLayout.EAST);
                
                content.add(item);
                content.add(Box.createVerticalStrut(5));
                count++;
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
            for (Map<String, Object> event : events) {
                if (count >= 3) break;
                String title = event.getOrDefault("title", "Untitled").toString();
                String date = event.getOrDefault("created_at", "").toString();
                if (date.length() > 10) date = date.substring(0, 10);
                
                JPanel item = new JPanel(new BorderLayout());
                item.setOpaque(false);
                item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
                item.setBorder(new EmptyBorder(5, 0, 5, 0));
                
                JLabel titleLabel = new JLabel("📅 " + title);
                titleLabel.setFont(F_MONO_SM);
                titleLabel.setForeground(C_TEXT);
                
                JLabel dateLabel = new JLabel(date);
                dateLabel.setFont(F_MONO_SM);
                dateLabel.setForeground(C_SUCCESS);
                
                item.add(titleLabel, BorderLayout.WEST);
                item.add(dateLabel, BorderLayout.EAST);
                
                content.add(item);
                content.add(Box.createVerticalStrut(5));
                count++;
            }
        }
        content.revalidate();
        content.repaint();
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
        title.setBorder(new EmptyBorder(0, 0, 20, 0));

        // Add button for creating new devotionals
        JButton addBtn = new JButton("➕ Add Devotional");
        addBtn.setBackground(C_GOLD);
        addBtn.setForeground(C_TEXT);
        addBtn.setFocusPainted(false);
        addBtn.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        addBtn.setFont(F_LABEL);
        addBtn.addActionListener(e -> showCreateDevotionalDialog());
        
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.add(title, BorderLayout.WEST);
        headerPanel.add(addBtn, BorderLayout.EAST);

        // Grid layout for devotionals
        devotionalsGrid = new JPanel(new GridLayout(0, 2, 20, 20)); // 2 columns, dynamic rows
        devotionalsGrid.setOpaque(false);
        
        JScrollPane scroll = new JScrollPane(devotionalsGrid);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(BorderFactory.createEmptyBorder());

        panel.add(headerPanel, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        
        // Load real devotionals data
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

        // Add button for creating new members
        JButton addBtn = new JButton("➕ Add Member");
        addBtn.setBackground(C_GOLD);
        addBtn.setForeground(C_TEXT);
        addBtn.setFocusPainted(false);
        addBtn.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        addBtn.setFont(F_LABEL);
        addBtn.addActionListener(e -> showCreateMemberDialog());
        
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.add(title, BorderLayout.WEST);
        headerPanel.add(addBtn, BorderLayout.EAST);

        // Grid layout for members cards
        membersGrid = new JPanel(new GridLayout(0, 3, 20, 20)); // 3 columns, dynamic rows
        membersGrid.setOpaque(false);
        
        JScrollPane scroll = new JScrollPane(membersGrid);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(BorderFactory.createEmptyBorder());

        panel.add(headerPanel, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        
        // Load real members data
        loadMembersGridData(membersGrid);
        
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
            if (data != null && !data.isEmpty()) {
                // Extract meaningful data from comprehensive dashboard
                int totalMembers = parseSafely(data.getOrDefault("total_members", "0"));
                int devotionalsCount = parseSafely(data.getOrDefault("devotionals_count", "0"));
                int prayerRequests = parseSafely(data.getOrDefault("prayer_requests", "0"));
                int counselingSessions = parseSafely(data.getOrDefault("counseling_sessions", "0"));
                
                // If no data from comprehensive, try members API for member count
                if (totalMembers == 0) {
                    SanctumApiClient.getMembers().thenAccept(members -> SwingUtilities.invokeLater(() -> {
                        lblTotalMembers.setText(String.valueOf(members.size()));
                    }));
                } else {
                    lblTotalMembers.setText(String.valueOf(totalMembers));
                }
                
                // Load devotionals count from devotionals API
                SanctumApiClient.getDevotionals().thenAccept(devotionals -> SwingUtilities.invokeLater(() -> {
                    lblDevotionals.setText(String.valueOf(devotionals.size()));
                }));
                
                // Use announcements as fallback for prayer requests
                SanctumApiClient.getAnnouncements().thenAccept(announcements -> SwingUtilities.invokeLater(() -> {
                    lblPrayerRequests.setText(String.valueOf(announcements.size()));
                }));
                
                // Use staff count as fallback for counseling sessions
                SanctumApiClient.getStaff().thenAccept(staff -> SwingUtilities.invokeLater(() -> {
                    lblCounselingSessions.setText(String.valueOf(staff.size()));
                }));
                
                System.out.println("Pastor dashboard data loaded successfully.");
            } else {
                // Fallback to individual API calls
                loadIndividualKpiData();
            }
        })).exceptionally(ex -> {
            SwingUtilities.invokeLater(() -> {
                System.err.println("Failed to load dashboard data: " + ex.getMessage());
                loadIndividualKpiData(); // Fallback to individual calls
            });
            return null;
        });
    }
    
    private void loadIndividualKpiData() {
        // Load each KPI individually as fallback
        SanctumApiClient.getMembers().thenAccept(members -> SwingUtilities.invokeLater(() -> {
            lblTotalMembers.setText(String.valueOf(members.size()));
        }));
        
        SanctumApiClient.getDevotionals().thenAccept(devotionals -> SwingUtilities.invokeLater(() -> {
            lblDevotionals.setText(String.valueOf(devotionals.size()));
        }));
        
        SanctumApiClient.getAnnouncements().thenAccept(announcements -> SwingUtilities.invokeLater(() -> {
            lblPrayerRequests.setText(String.valueOf(announcements.size()));
        }));
        
        SanctumApiClient.getStaff().thenAccept(staff -> SwingUtilities.invokeLater(() -> {
            lblCounselingSessions.setText(String.valueOf(staff.size()));
        }));
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

    // ─── Enhanced Grid Data Loading Methods ─────────────────────────────────────
    private void loadDevotionalsGridData(JPanel devotionalsGrid) {
        System.out.println("Loading devotionals data...");
        SanctumApiClient.getDevotionals().thenAccept(devotionals -> {
            SwingUtilities.invokeLater(() -> {
                System.out.println("Received " + devotionals.size() + " devotionals");
                devotionalsGrid.removeAll();
                if (devotionals.isEmpty()) {
                    JPanel emptyPanel = createEmptyStatePanel("No devotionals available", "📖", "Click 'Add Devotional' to create your first devotional");
                    devotionalsGrid.add(emptyPanel);
                } else {
                    for (Map<String, Object> devotional : devotionals) {
                        System.out.println("Creating card for devotional: " + devotional.get("title"));
                        JPanel card = createDevotionalGridCard(devotional);
                        devotionalsGrid.add(card);
                    }
                }
                devotionalsGrid.revalidate();
                devotionalsGrid.repaint();
                System.out.println("Devotionals grid updated with " + devotionalsGrid.getComponentCount() + " components");
            });
        }).exceptionally(ex -> {
            System.err.println("Error loading devotionals: " + ex.getMessage());
            SwingUtilities.invokeLater(() -> {
                devotionalsGrid.removeAll();
                JPanel errorPanel = createErrorPanel("Failed to load devotionals", "📖");
                devotionalsGrid.add(errorPanel);
                devotionalsGrid.revalidate();
                devotionalsGrid.repaint();
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
        
        // Header with title and date - add null checks
        String title = devotional.getOrDefault("title", "Untitled") != null ? 
                      devotional.getOrDefault("title", "Untitled").toString() : "Untitled";
        String date = devotional.getOrDefault("date", "") != null ? 
                     devotional.getOrDefault("date", "").toString() : "";
        String author = devotional.getOrDefault("author", "Unknown") != null ? 
                       devotional.getOrDefault("author", "Unknown").toString() : "Unknown";
        String scripture = devotional.getOrDefault("scripture_reference", "") != null ? 
                          devotional.getOrDefault("scripture_reference", "").toString() : "";
        
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(F_LABEL);
        titleLabel.setForeground(C_TEXT);
        
        JLabel dateLabel = new JLabel(date);
        dateLabel.setFont(F_MONO_SM);
        dateLabel.setForeground(C_TEXT_DIM);
        
        headerPanel.add(titleLabel, BorderLayout.CENTER);
        headerPanel.add(dateLabel, BorderLayout.EAST);
        
        // Content panel
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setOpaque(false);
        
        if (!scripture.isEmpty()) {
            JLabel scriptureLabel = new JLabel("📜 " + scripture);
            scriptureLabel.setFont(F_MONO_SM);
            scriptureLabel.setForeground(C_GOLD);
            scriptureLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            contentPanel.add(scriptureLabel);
            contentPanel.add(Box.createVerticalStrut(8));
        }
        
        JLabel authorLabel = new JLabel("✍️ " + author);
        authorLabel.setFont(F_MONO_SM);
        authorLabel.setForeground(C_TEXT_DIM);
        authorLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(authorLabel);
        
        // Reactions section - add null checks
        int reactionsCount = parseSafely(devotional.getOrDefault("reactions_count", "0"));
        String userReaction = devotional.get("user_reaction") != null ? 
                             devotional.get("user_reaction").toString() : "";
        
        JPanel reactionsPanel = createReactionsPanel(devotional, reactionsCount, userReaction);
        reactionsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(Box.createVerticalStrut(12));
        contentPanel.add(reactionsPanel);
        
        // Action buttons
        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actionsPanel.setOpaque(false);
        
        JButton viewBtn = new JButton("👁️ View");
        viewBtn.setBackground(C_SUCCESS);
        viewBtn.setForeground(C_TEXT);
        viewBtn.setFocusPainted(false);
        viewBtn.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        viewBtn.setFont(F_MONO_SM);
        viewBtn.addActionListener(e -> showDevotionalDetails(devotional));
        
        JButton editBtn = new JButton("✏️ Edit");
        editBtn.setBackground(C_GOLD);
        editBtn.setForeground(C_TEXT);
        editBtn.setFocusPainted(false);
        editBtn.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        editBtn.setFont(F_MONO_SM);
        
        actionsPanel.add(viewBtn);
        actionsPanel.add(editBtn);
        
        card.add(headerPanel, BorderLayout.NORTH);
        card.add(contentPanel, BorderLayout.CENTER);
        card.add(actionsPanel, BorderLayout.SOUTH);
        
        // Hover effect
        card.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { card.setBackground(C_SURFACE); card.repaint(); }
            @Override public void mouseExited(MouseEvent e) { card.setBackground(C_CARD); card.repaint(); }
        });
        
        return card;
    }
    
    private JPanel createReactionsPanel(Map<String, Object> devotional, int reactionsCount, String userReaction) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        panel.setOpaque(false);
        
        // Common reactions
        String[] reactions = {"❤️", "🙏", "👍", "🔥", "🎉"};
        
        for (String reaction : reactions) {
            JButton reactionBtn = new JButton(reaction);
            reactionBtn.setBackground(C_SURFACE);
            reactionBtn.setForeground(C_TEXT);
            reactionBtn.setFocusPainted(false);
            reactionBtn.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
            reactionBtn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
            reactionBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            
            // Highlight if user has reacted
            if (reaction.equals(userReaction)) {
                reactionBtn.setBackground(C_GOLD_DIM);
            }
            
            reactionBtn.addActionListener(e -> {
                // Toggle reaction
                if (reactionBtn.getBackground().equals(C_GOLD_DIM)) {
                    reactionBtn.setBackground(C_SURFACE);
                } else {
                    reactionBtn.setBackground(C_GOLD_DIM);
                }
                // Here you would call API to update reaction
                System.out.println("Reaction clicked: " + reaction + " for devotional: " + devotional.get("id"));
            });
            
            panel.add(reactionBtn);
        }
        
        // Reactions count
        JLabel countLabel = new JLabel(reactionsCount + " reactions");
        countLabel.setFont(F_MONO_SM);
        countLabel.setForeground(C_TEXT_DIM);
        panel.add(countLabel);
        
        return panel;
    }
    
    private void loadMembersGridData(JPanel membersGrid) {
        System.out.println("Loading members data...");
        SanctumApiClient.getMembers().thenAccept(members -> {
            SwingUtilities.invokeLater(() -> {
                System.out.println("Received " + members.size() + " members");
                membersGrid.removeAll();
                if (members.isEmpty()) {
                    JPanel emptyPanel = createEmptyStatePanel("No members available", "👥", "Click 'Add Member' to register your first member");
                    membersGrid.add(emptyPanel);
                } else {
                    for (Map<String, Object> member : members) {
                        System.out.println("Creating card for member: " + member.get("first_name"));
                        JPanel card = createMemberGridCard(member);
                        membersGrid.add(card);
                    }
                }
                membersGrid.revalidate();
                membersGrid.repaint();
                System.out.println("Members grid updated with " + membersGrid.getComponentCount() + " components");
            });
        }).exceptionally(ex -> {
            System.err.println("Error loading members: " + ex.getMessage());
            SwingUtilities.invokeLater(() -> {
                membersGrid.removeAll();
                JPanel errorPanel = createErrorPanel("Failed to load members", "👥");
                membersGrid.add(errorPanel);
                membersGrid.revalidate();
                membersGrid.repaint();
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
        
        // Avatar and name - add null checks
        String firstName = member.getOrDefault("first_name", "Unknown") != null ? 
                         member.getOrDefault("first_name", "Unknown").toString() : "Unknown";
        String lastName = member.getOrDefault("last_name", "") != null ? 
                        member.getOrDefault("last_name", "").toString() : "";
        String fullName = lastName.isEmpty() ? firstName : firstName + " " + lastName;
        String initials = (firstName.isEmpty() ? "?" : firstName.substring(0, 1)) + 
                        (lastName.isEmpty() ? "" : lastName.substring(0, 1)).toUpperCase();
        
        JPanel headerPanel = new JPanel(new BorderLayout(8, 0));
        headerPanel.setOpaque(false);
        
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
        avatar.setPreferredSize(new Dimension(50, 50));
        avatar.setOpaque(false);
        
        JLabel avatarLabel = new JLabel(initials);
        avatarLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        avatarLabel.setForeground(C_TEXT);
        avatarLabel.setHorizontalAlignment(SwingConstants.CENTER);
        avatarLabel.setVerticalAlignment(SwingConstants.CENTER);
        avatar.add(avatarLabel, BorderLayout.CENTER);
        
        // Name and status
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setOpaque(false);
        
        JLabel nameLabel = new JLabel(fullName);
        nameLabel.setFont(F_LABEL);
        nameLabel.setForeground(C_TEXT);
        
        String status = member.getOrDefault("membership_status", "new_member").toString();
        JLabel statusLabel = new JLabel("🟢 " + status.replace("_", " ").toUpperCase());
        statusLabel.setFont(F_MONO_SM);
        statusLabel.setForeground(C_SUCCESS);
        
        infoPanel.add(nameLabel);
        infoPanel.add(Box.createVerticalStrut(5));
        infoPanel.add(statusLabel);
        
        headerPanel.add(avatar, BorderLayout.WEST);
        headerPanel.add(infoPanel, BorderLayout.CENTER);
        
        // Contact info - add null checks
        JPanel contactPanel = new JPanel();
        contactPanel.setLayout(new BoxLayout(contactPanel, BoxLayout.Y_AXIS));
        contactPanel.setOpaque(false);
        
        String email = member.getOrDefault("email", "") != null ? 
                      member.getOrDefault("email", "").toString() : "";
        String phone = member.getOrDefault("phone_number", "") != null ? 
                      member.getOrDefault("phone_number", "").toString() : "";
        
        if (!email.isEmpty()) {
            JLabel emailLabel = new JLabel("📧 " + email);
            emailLabel.setFont(F_MONO_SM);
            emailLabel.setForeground(C_TEXT_DIM);
            contactPanel.add(emailLabel);
        }
        
        if (!phone.isEmpty()) {
            JLabel phoneLabel = new JLabel("📱 " + phone);
            phoneLabel.setFont(F_MONO_SM);
            phoneLabel.setForeground(C_TEXT_DIM);
            contactPanel.add(phoneLabel);
        }
        
        // Action buttons
        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actionsPanel.setOpaque(false);
        
        JButton viewBtn = new JButton("👁️ View");
        viewBtn.setBackground(C_SUCCESS);
        viewBtn.setForeground(C_TEXT);
        viewBtn.setFocusPainted(false);
        viewBtn.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        viewBtn.setFont(F_MONO_SM);
        
        JButton contactBtn = new JButton("💬 Contact");
        contactBtn.setBackground(C_GOLD);
        contactBtn.setForeground(C_TEXT);
        contactBtn.setFocusPainted(false);
        contactBtn.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        contactBtn.setFont(F_MONO_SM);
        
        actionsPanel.add(viewBtn);
        actionsPanel.add(contactBtn);
        
        card.add(headerPanel, BorderLayout.NORTH);
        card.add(contactPanel, BorderLayout.CENTER);
        card.add(actionsPanel, BorderLayout.SOUTH);
        
        // Hover effect
        card.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { card.setBackground(C_SURFACE); card.repaint(); }
            @Override public void mouseExited(MouseEvent e) { card.setBackground(C_CARD); card.repaint(); }
        });
        
        return card;
    }

    // ─── Missing Helper Methods ─────────────────────────────────────────────
    private JPanel createEmptyStatePanel(String message, String icon, String subtext) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(40, 20, 40, 20));
        
        JLabel iconLabel = new JLabel(icon);
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 48));
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
        
        panel.add(iconLabel, BorderLayout.NORTH);
        panel.add(textPanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createErrorPanel(String message, String icon) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(40, 20, 40, 20));
        
        JLabel iconLabel = new JLabel(icon);
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 48));
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
        
        panel.add(iconLabel, BorderLayout.NORTH);
        panel.add(textPanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    private void showDevotionalDetails(Map<String, Object> devotional) {
        JDialog dialog = new JDialog(this, "Devotional Details", true);
        dialog.setSize(600, 500);
        dialog.setLocationRelativeTo(this);
        
        JPanel panel = new JPanel(new BorderLayout(0, 20));
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        panel.setBackground(C_CARD);
        
        // Header
        String title = devotional.getOrDefault("title", "Untitled").toString();
        String scripture = devotional.getOrDefault("scripture_reference", "").toString();
        String date = devotional.getOrDefault("created_at", "").toString();
        if (date.length() > 10) date = date.substring(0, 10);
        
        JLabel titleLabel = new JLabel("📖 " + title);
        titleLabel.setFont(F_TITLE);
        titleLabel.setForeground(C_TEXT);
        
        JLabel scriptureLabel = new JLabel("📜 " + scripture);
        scriptureLabel.setFont(F_LABEL);
        scriptureLabel.setForeground(C_GOLD);
        
        JLabel dateLabel = new JLabel("📅 " + date);
        dateLabel.setFont(F_MONO_SM);
        dateLabel.setForeground(C_TEXT_DIM);
        
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setOpaque(false);
        headerPanel.add(titleLabel);
        headerPanel.add(Box.createVerticalStrut(5));
        headerPanel.add(scriptureLabel);
        headerPanel.add(Box.createVerticalStrut(5));
        headerPanel.add(dateLabel);
        
        // Content
        String content = devotional.getOrDefault("content", "No content available.").toString();
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
        
        // Close button
        JButton closeBtn = new JButton("Close");
        closeBtn.setBackground(C_GOLD);
        closeBtn.setForeground(C_TEXT);
        closeBtn.setFocusPainted(false);
        closeBtn.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        closeBtn.addActionListener(e -> dialog.dispose());
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setOpaque(false);
        buttonPanel.add(closeBtn);
        
        panel.add(headerPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        dialog.add(panel);
        dialog.setVisible(true);
    }
    
    // ─── Legacy Data Loading Methods (for other pages) ───────────────────────
    private void loadPrayerRequestsData(JPanel prayerPanel) {
        // Use announcements as a temporary fallback for prayer requests
        SanctumApiClient.getAnnouncements().thenAccept(announcements -> {
            SwingUtilities.invokeLater(() -> {
                prayerPanel.removeAll();
                if (announcements.isEmpty()) {
                    prayerPanel.add(createEmptyStatePanel("No prayer requests available", "🙏", "No prayer requests at this time"));
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
                prayerPanel.add(createErrorPanel("Failed to load prayer requests", "🙏"));
                prayerPanel.revalidate();
                prayerPanel.repaint();
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
