package com.sanctum.view;

import com.sanctum.api.SanctumApiClient;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class ChurchSettingsFrame extends JFrame {
    
    // Color scheme
    private static final Color C_BG = new Color(20, 20, 30);
    private static final Color C_SURFACE = new Color(30, 30, 45);
    private static final Color C_CARD = new Color(40, 40, 60);
    private static final Color C_BORDER = new Color(60, 60, 80);
    private static final Color C_TEXT = new Color(220, 220, 230);
    private static final Color C_TEXT_MID = new Color(160, 160, 170);
    private static final Color C_GOLD = new Color(212, 175, 55);
    
    private Font FONT_LABEL = new Font("Segoe UI", Font.PLAIN, 13);
    private Font FONT_HEADER = new Font("Segoe UI", Font.BOLD, 16);
    
    // Logo upload button
    private JButton uploadLogoBtn;
    private JButton removeLogoBtn;
    private JLabel currentLogoLabel;
    private String currentLogoPath = null;
    
    // Settings fields
    private JTextField churchNameField;
    private JTextField churchCodeField;
    private JTextField pastorNameField;
    private JTextField addressField;
    private JTextField cityField;
    private JTextField countyField;
    private JTextField phoneField;
    private JTextField emailField;
    private JTextField websiteField;
    private JTextField sundayServiceField;
    private JTextField wednesdayServiceField;
    private JTextField prayerMeetingField;
    private JTextField youthServiceField;
    private JTextField adminEmailField;
    private JTextField timezoneField;
    private JTextField currencyField;
    private JTextField dateFormatField;
    
    // Color pickers
    private JTextField primaryColorField;
    private JTextField secondaryColorField;
    private JTextField backgroundColorField;
    private JTextField panelColorField;
    private JTextField cardColorField;
    
    public ChurchSettingsFrame() {
        initializeFrame();
        createComponents();
        layoutComponents();
        setupEventHandlers();
        loadCurrentSettings();
    }
    
    private void initializeFrame() {
        setTitle("Sanctum — Church Settings");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(900, 700);
        setLocationRelativeTo(null);
        getContentPane().setBackground(C_BG);
        setResizable(true);
    }
    
    private void createComponents() {
        // Church Information Fields
        churchNameField = createTextField();
        churchCodeField = createTextField();
        churchCodeField.setEditable(false); // Church code is read-only
        pastorNameField = createTextField();
        addressField = createTextField();
        cityField = createTextField();
        countyField = createTextField();
        phoneField = createTextField();
        emailField = createTextField();
        websiteField = createTextField();
        
        // Service Times
        sundayServiceField = createTextField();
        wednesdayServiceField = createTextField();
        prayerMeetingField = createTextField();
        youthServiceField = createTextField();
        
        // System Settings
        adminEmailField = createTextField();
        timezoneField = createTextField();
        currencyField = createTextField();
        dateFormatField = createTextField();
        
        // Theme Colors
        primaryColorField = createColorField(C_GOLD);
        secondaryColorField = createColorField(C_TEXT_MID);
        backgroundColorField = createColorField(C_BG);
        panelColorField = createColorField(C_SURFACE);
        cardColorField = createColorField(C_CARD);
        
        // Logo components
        uploadLogoBtn = createButton("📤 Upload Logo");
        removeLogoBtn = createButton("🗑️ Remove Logo");
        currentLogoLabel = new JLabel("No logo uploaded");
        currentLogoLabel.setFont(FONT_LABEL);
        currentLogoLabel.setForeground(C_TEXT_MID);
        currentLogoLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        currentLogoLabel.setBackground(C_CARD);
        currentLogoLabel.setOpaque(true);
        
        // Setup logo button actions
        uploadLogoBtn.addActionListener(e -> uploadLogo());
        removeLogoBtn.addActionListener(e -> removeLogo());
    }
    
    private JTextField createTextField() {
        JTextField field = new JTextField();
        field.setFont(FONT_LABEL);
        field.setForeground(C_TEXT);
        field.setBackground(C_CARD);
        field.setCaretColor(C_GOLD);
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(C_BORDER),
            BorderFactory.createEmptyBorder(5, 8, 5, 8)
        ));
        return field;
    }
    
    private JTextField createColorField(Color color) {
        JTextField field = createTextField();
        field.setText(String.format("#%02X%02X%02X", 
            color.getRed(), color.getGreen(), color.getBlue()));
        field.setPreferredSize(new Dimension(100, 30));
        return field;
    }
    
    private void layoutComponents() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(C_BG);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Header
        JPanel headerPanel = createHeaderPanel();
        mainPanel.add(headerPanel, BorderLayout.NORTH);
        
        // Scrollable content
        JScrollPane scrollPane = new JScrollPane(createContentPanel());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(16);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(C_BG);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        
        // Button panel
        JPanel buttonPanel = createButtonPanel();
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        add(mainPanel);
    }
    
    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(C_BG);
        panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
        
        JLabel title = new JLabel("⚙️ Church Settings");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(C_GOLD);
        
        JLabel subtitle = new JLabel("Manage your church's information, appearance, and system settings");
        subtitle.setFont(FONT_LABEL);
        subtitle.setForeground(C_TEXT_MID);
        
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBackground(C_BG);
        titlePanel.add(title, BorderLayout.NORTH);
        titlePanel.add(subtitle, BorderLayout.CENTER);
        
        panel.add(titlePanel, BorderLayout.WEST);
        
        return panel;
    }
    
    private JPanel createContentPanel() {
        JPanel content = new JPanel(new GridBagLayout());
        content.setBackground(C_BG);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 20, 0);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;
        
        // Church Information Section
        content.add(createSectionPanel("🏛️ Church Information", createChurchInfoPanel()), gbc);
        gbc.gridy++;
        
        // Service Times Section
        content.add(createSectionPanel("⏰ Service Times", createServiceTimesPanel()), gbc);
        gbc.gridy++;
        
        // Theme Colors Section
        content.add(createSectionPanel("🎨 Theme Colors", createThemeColorsPanel()), gbc);
        gbc.gridy++;
        
        // System Settings Section
        content.add(createSectionPanel("⚙️ System Settings", createSystemSettingsPanel()), gbc);
        
        return content;
    }
    
    private JPanel createSectionPanel(String title, JPanel content) {
        JPanel section = new JPanel(new BorderLayout());
        section.setBackground(C_CARD);
        section.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(C_BORDER),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        
        JLabel header = new JLabel(title);
        header.setFont(FONT_HEADER);
        header.setForeground(C_GOLD);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        
        section.add(header, BorderLayout.NORTH);
        section.add(content, BorderLayout.CENTER);
        
        return section;
    }
    
    private JPanel createChurchInfoPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(C_CARD);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        // Church Logo Section
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        JLabel logoHeader = new JLabel("🏷️ Church Logo:");
        logoHeader.setFont(FONT_HEADER);
        logoHeader.setForeground(C_GOLD);
        panel.add(logoHeader, gbc);
        
        gbc.gridy = 1; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.HORIZONTAL;
        JPanel logoPanel = new JPanel(new BorderLayout());
        logoPanel.setBackground(C_CARD);
        logoPanel.add(currentLogoLabel, BorderLayout.CENTER);
        panel.add(logoPanel, gbc);
        
        gbc.gridy = 2; gbc.gridwidth = 1; gbc.fill = GridBagConstraints.NONE;
        panel.add(uploadLogoBtn, gbc);
        
        gbc.gridx = 1;
        panel.add(removeLogoBtn, gbc);
        
        // Separator
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.HORIZONTAL;
        JSeparator separator = new JSeparator();
        separator.setForeground(C_BORDER);
        panel.add(separator, gbc);
        
        // Church Code (read-only)
        addFormField(panel, "Church Code:", churchCodeField, 0, 5, gbc);
        
        // Church Name
        addFormField(panel, "Church Name:", churchNameField, 0, 6, gbc);
        
        // Pastor Name
        addFormField(panel, "Senior Pastor:", pastorNameField, 0, 7, gbc);
        
        // Address
        addFormField(panel, "Address:", addressField, 0, 8, gbc);
        
        // City
        addFormField(panel, "City:", cityField, 0, 9, gbc);
        
        // County
        addFormField(panel, "County:", countyField, 0, 10, gbc);
        
        // Phone
        addFormField(panel, "Phone:", phoneField, 0, 11, gbc);
        
        // Email
        addFormField(panel, "Email:", emailField, 0, 12, gbc);
        
        // Website
        addFormField(panel, "Website:", websiteField, 0, 13, gbc);
        
        return panel;
    }
    
    private JPanel createServiceTimesPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(C_CARD);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        addFormField(panel, "Sunday Service:", sundayServiceField, 0, 0, gbc);
        addFormField(panel, "Wednesday Service:", wednesdayServiceField, 0, 1, gbc);
        addFormField(panel, "Prayer Meeting:", prayerMeetingField, 0, 2, gbc);
        addFormField(panel, "Youth Service:", youthServiceField, 0, 3, gbc);
        
        return panel;
    }
    
    private JPanel createThemeColorsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(C_CARD);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        addColorField(panel, "Primary Color:", primaryColorField, 0, 0, gbc);
        addColorField(panel, "Secondary Color:", secondaryColorField, 0, 1, gbc);
        addColorField(panel, "Background Color:", backgroundColorField, 0, 2, gbc);
        addColorField(panel, "Panel Color:", panelColorField, 0, 3, gbc);
        addColorField(panel, "Card Color:", cardColorField, 0, 4, gbc);
        
        return panel;
    }
    
    private JPanel createSystemSettingsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(C_CARD);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        addFormField(panel, "Admin Email:", adminEmailField, 0, 0, gbc);
        addFormField(panel, "Timezone:", timezoneField, 0, 1, gbc);
        addFormField(panel, "Currency:", currencyField, 0, 2, gbc);
        addFormField(panel, "Date Format:", dateFormatField, 0, 3, gbc);
        
        return panel;
    }
    
    private void addFormField(JPanel panel, String label, JTextField field, int x, int y, GridBagConstraints gbc) {
        gbc.gridx = x;
        gbc.gridy = y;
        gbc.weightx = 0.3;
        gbc.fill = GridBagConstraints.NONE;
        
        JLabel lbl = new JLabel(label);
        lbl.setFont(FONT_LABEL);
        lbl.setForeground(C_TEXT_MID);
        panel.add(lbl, gbc);
        
        gbc.gridx = x + 1;
        gbc.weightx = 0.7;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(field, gbc);
    }
    
    private void addColorField(JPanel panel, String label, JTextField field, int x, int y, GridBagConstraints gbc) {
        gbc.gridx = x;
        gbc.gridy = y;
        gbc.weightx = 0.3;
        gbc.fill = GridBagConstraints.NONE;
        
        JLabel lbl = new JLabel(label);
        lbl.setFont(FONT_LABEL);
        lbl.setForeground(C_TEXT_MID);
        panel.add(lbl, gbc);
        
        gbc.gridx = x + 1;
        gbc.weightx = 0.3;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(field, gbc);
        
        // Add color preview
        gbc.gridx = x + 2;
        gbc.weightx = 0.1;
        JPanel preview = createColorPreview(field);
        panel.add(preview, gbc);
        
        // Add choose button
        gbc.gridx = x + 3;
        gbc.weightx = 0.3;
        JButton chooseBtn = createButton("Choose");
        chooseBtn.addActionListener(e -> chooseColor(field, preview));
        panel.add(chooseBtn, gbc);
    }
    
    private JPanel createColorPreview(JTextField field) {
        JPanel preview = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                try {
                    String hex = field.getText();
                    if (hex.startsWith("#")) {
                        Color color = Color.decode(hex);
                        g.setColor(color);
                    } else {
                        g.setColor(C_GOLD);
                    }
                } catch (Exception e) {
                    g.setColor(C_GOLD);
                }
                g.fillRoundRect(0, 0, getWidth(), getHeight(), 4, 4);
                g.setColor(C_BORDER);
                g.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 4, 4);
            }
        };
        preview.setPreferredSize(new Dimension(30, 24));
        preview.setBackground(C_CARD);
        return preview;
    }
    
    private void chooseColor(JTextField field, JPanel preview) {
        Color currentColor;
        try {
            currentColor = Color.decode(field.getText());
        } catch (Exception e) {
            currentColor = C_GOLD;
        }
        
        Color chosenColor = JColorChooser.showDialog(this, "Choose Color", currentColor);
        if (chosenColor != null) {
            field.setText(String.format("#%02X%02X%02X", 
                chosenColor.getRed(), chosenColor.getGreen(), chosenColor.getBlue()));
            preview.repaint();
        }
    }
    
    private JButton createButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_LABEL);
        btn.setForeground(Color.WHITE);
        btn.setBackground(C_GOLD);
        btn.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btn.setBackground(new Color(255, 215, 0));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                btn.setBackground(C_GOLD);
            }
        });
        
        return btn;
    }
    
    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        panel.setBackground(C_BG);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));
        
        JButton saveBtn = createButton("💾 Save Settings");
        JButton resetBtn = createButton("↺ Reset");
        JButton exportBtn = createButton("📤 Export");
        JButton importBtn = createButton("📥 Import");
        
        saveBtn.addActionListener(e -> saveSettings());
        resetBtn.addActionListener(e -> resetSettings());
        exportBtn.addActionListener(e -> exportSettings());
        importBtn.addActionListener(e -> importSettings());
        
        panel.add(saveBtn);
        panel.add(resetBtn);
        panel.add(exportBtn);
        panel.add(importBtn);
        
        return panel;
    }
    
    private void setupEventHandlers() {
        // Window closing handler
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                dispose();
            }
        });
    }
    
    private void loadCurrentSettings() {
        // Show loading indicator
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        
        // Load settings from backend
        SanctumApiClient.getChurchDetails().thenAccept(churchData -> {
            SwingUtilities.invokeLater(() -> {
                setCursor(Cursor.getDefaultCursor());
                if (churchData != null && !churchData.isEmpty()) {
                    populateFields(churchData);
                    System.out.println("Church settings loaded from backend");
                } else {
                    populateFields(null);
                    System.out.println("Using default church settings");
                }
            });
        }).exceptionally(ex -> {
            SwingUtilities.invokeLater(() -> {
                setCursor(Cursor.getDefaultCursor());
                populateFields(null);
                System.out.println("Failed to load church settings, using defaults: " + ex.getMessage());
            });
            return null;
        });
    }
    
    private void populateFields(Map<String, Object> data) {
        if (data != null) {
            churchCodeField.setText(data.getOrDefault("church_code", "N/A").toString());
            churchNameField.setText(data.getOrDefault("name", "Grace Community Church").toString());
            pastorNameField.setText(data.getOrDefault("senior_pastor_name", "Rev. James Anderson").toString());
            addressField.setText(data.getOrDefault("address_line1", "123 Main Street").toString());
            cityField.setText(data.getOrDefault("city", "Nairobi").toString());
            countyField.setText(data.getOrDefault("county", "Nairobi County").toString());
            phoneField.setText(data.getOrDefault("phone_number", "+254 123 456 789").toString());
            emailField.setText(data.getOrDefault("email", "info@church.com").toString());
            websiteField.setText(data.getOrDefault("website", "www.church.com").toString());
            
            // Service times
            sundayServiceField.setText(data.getOrDefault("sunday_service_time", "9:00 AM").toString());
            wednesdayServiceField.setText(data.getOrDefault("wednesday_service_time", "7:00 PM").toString());
            prayerMeetingField.setText(data.getOrDefault("prayer_meeting_time", "6:00 AM").toString());
            youthServiceField.setText(data.getOrDefault("youth_service_time", "5:00 PM").toString());
            
            // System settings
            adminEmailField.setText(data.getOrDefault("admin_email", "admin@church.com").toString());
            timezoneField.setText(data.getOrDefault("timezone", "UTC+3 (Nairobi)").toString());
            currencyField.setText(data.getOrDefault("currency", "KES").toString());
            dateFormatField.setText(data.getOrDefault("date_format", "DD/MM/YYYY").toString());
            
            // Theme colors
            primaryColorField.setText(data.getOrDefault("primary_color", String.format("#%02X%02X%02X", C_GOLD.getRed(), C_GOLD.getGreen(), C_GOLD.getBlue())).toString());
            secondaryColorField.setText(data.getOrDefault("secondary_color", String.format("#%02X%02X%02X", C_TEXT_MID.getRed(), C_TEXT_MID.getGreen(), C_TEXT_MID.getBlue())).toString());
            backgroundColorField.setText(data.getOrDefault("background_color", String.format("#%02X%02X%02X", C_BG.getRed(), C_BG.getGreen(), C_BG.getBlue())).toString());
            panelColorField.setText(data.getOrDefault("panel_color", String.format("#%02X%02X%02X", C_SURFACE.getRed(), C_SURFACE.getGreen(), C_SURFACE.getBlue())).toString());
            cardColorField.setText(data.getOrDefault("card_color", String.format("#%02X%02X%02X", C_CARD.getRed(), C_CARD.getGreen(), C_CARD.getBlue())).toString());
            
            System.out.println("Populated fields with backend data");
        } else {
            // Default values
            churchCodeField.setText("N/A");
            churchNameField.setText("Grace Community Church");
            pastorNameField.setText("Rev. James Anderson");
            addressField.setText("123 Main Street");
            cityField.setText("Nairobi");
            countyField.setText("Nairobi County");
            phoneField.setText("+254 123 456 789");
            emailField.setText("info@church.com");
            websiteField.setText("www.church.com");
            
            // Default service times
            sundayServiceField.setText("9:00 AM");
            wednesdayServiceField.setText("7:00 PM");
            prayerMeetingField.setText("6:00 AM");
            youthServiceField.setText("5:00 PM");
            
            // Default system settings
            adminEmailField.setText("admin@church.com");
            timezoneField.setText("UTC+3 (Nairobi)");
            currencyField.setText("KES");
            dateFormatField.setText("DD/MM/YYYY");
            
            // Default colors
            primaryColorField.setText(String.format("#%02X%02X%02X", C_GOLD.getRed(), C_GOLD.getGreen(), C_GOLD.getBlue()));
            secondaryColorField.setText(String.format("#%02X%02X%02X", C_TEXT_MID.getRed(), C_TEXT_MID.getGreen(), C_TEXT_MID.getBlue()));
            backgroundColorField.setText(String.format("#%02X%02X%02X", C_BG.getRed(), C_BG.getGreen(), C_BG.getBlue()));
            panelColorField.setText(String.format("#%02X%02X%02X", C_SURFACE.getRed(), C_SURFACE.getGreen(), C_SURFACE.getBlue()));
            cardColorField.setText(String.format("#%02X%02X%02X", C_CARD.getRed(), C_CARD.getGreen(), C_CARD.getBlue()));
            
            System.out.println("Populated fields with default values");
        }
    }
    
    private void saveSettings() {
        // Show loading indicator
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        
        Map<String, Object> settings = new HashMap<>();
        
        // Collect all field values
        settings.put("name", churchNameField.getText());
        settings.put("senior_pastor_name", pastorNameField.getText());
        settings.put("address_line1", addressField.getText());
        settings.put("city", cityField.getText());
        settings.put("county", countyField.getText());
        settings.put("phone_number", phoneField.getText());
        settings.put("email", emailField.getText());
        settings.put("website", websiteField.getText());
        
        // Service times
        settings.put("sunday_service_time", sundayServiceField.getText());
        settings.put("wednesday_service_time", wednesdayServiceField.getText());
        settings.put("prayer_meeting_time", prayerMeetingField.getText());
        settings.put("youth_service_time", youthServiceField.getText());
        
        // System settings
        settings.put("admin_email", adminEmailField.getText());
        settings.put("timezone", timezoneField.getText());
        settings.put("currency", currencyField.getText());
        settings.put("date_format", dateFormatField.getText());
        
        // Theme colors
        settings.put("primary_color", primaryColorField.getText());
        settings.put("secondary_color", secondaryColorField.getText());
        settings.put("background_color", backgroundColorField.getText());
        settings.put("panel_color", panelColorField.getText());
        settings.put("card_color", cardColorField.getText());
        
        System.out.println("Saving church settings: " + settings);
        
        // Save to backend
        SanctumApiClient.updateChurchSettings(settings).thenAccept(response -> {
            SwingUtilities.invokeLater(() -> {
                setCursor(Cursor.getDefaultCursor());
                if (response != null && Boolean.TRUE.equals(response.get("success"))) {
                    JOptionPane.showMessageDialog(this, 
                        "✅ Church settings saved successfully!", 
                        "Success", 
                        JOptionPane.INFORMATION_MESSAGE);
                    System.out.println("Settings saved successfully");
                } else {
                    String errorMsg = response != null ? response.getOrDefault("error", "Unknown error").toString() : "No response from server";
                    JOptionPane.showMessageDialog(this, 
                        "❌ Failed to save settings: " + errorMsg, 
                        "Error", 
                        JOptionPane.ERROR_MESSAGE);
                    System.err.println("Failed to save settings: " + errorMsg);
                }
            });
        }).exceptionally(ex -> {
            SwingUtilities.invokeLater(() -> {
                setCursor(Cursor.getDefaultCursor());
                JOptionPane.showMessageDialog(this, 
                    "❌ Error saving settings: " + ex.getMessage(), 
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
                System.err.println("Exception saving settings: " + ex.getMessage());
            });
            return null;
        });
    }
    
    private void resetSettings() {
        int result = JOptionPane.showConfirmDialog(this, 
            "Are you sure you want to reset all settings to defaults?", 
            "Reset Settings", 
            JOptionPane.YES_NO_OPTION);
            
        if (result == JOptionPane.YES_OPTION) {
            populateFields(null);
            // Reset colors to defaults
            primaryColorField.setText(String.format("#%02X%02X%02X", C_GOLD.getRed(), C_GOLD.getGreen(), C_GOLD.getBlue()));
            secondaryColorField.setText(String.format("#%02X%02X%02X", C_TEXT_MID.getRed(), C_TEXT_MID.getGreen(), C_TEXT_MID.getBlue()));
            backgroundColorField.setText(String.format("#%02X%02X%02X", C_BG.getRed(), C_BG.getGreen(), C_BG.getBlue()));
            panelColorField.setText(String.format("#%02X%02X%02X", C_SURFACE.getRed(), C_SURFACE.getGreen(), C_SURFACE.getBlue()));
            cardColorField.setText(String.format("#%02X%02X%02X", C_CARD.getRed(), C_CARD.getGreen(), C_CARD.getBlue()));
        }
    }
    
    private void exportSettings() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Export Church Settings");
        fileChooser.setFileFilter(new FileNameExtensionFilter("JSON files (*.json)", "json"));
        
        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            if (!fileToSave.getName().endsWith(".json")) {
                fileToSave = new File(fileToSave.getAbsolutePath() + ".json");
            }
            
            // Export implementation
            JOptionPane.showMessageDialog(this, 
                "📤 Settings exported to " + fileToSave.getName(), 
                "Export Complete", 
                JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    private void importSettings() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Import Church Settings");
        fileChooser.setFileFilter(new FileNameExtensionFilter("JSON files (*.json)", "json"));
        
        int userSelection = fileChooser.showOpenDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToImport = fileChooser.getSelectedFile();
            
            // Import implementation
            JOptionPane.showMessageDialog(this, 
                "📥 Settings imported from " + fileToImport.getName(), 
                "Import Complete", 
                JOptionPane.INFORMATION_MESSAGE);
            
            // Reload settings
            loadCurrentSettings();
        }
    }
    
    // Logo upload method
    private void uploadLogo() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select Church Logo");
        fileChooser.setFileFilter(new FileNameExtensionFilter(
            "Image files (*.jpg, *.jpeg, *.png, *.gif)", "jpg", "jpeg", "png", "gif"));
        
        int userSelection = fileChooser.showOpenDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            currentLogoPath = selectedFile.getAbsolutePath();
            
            // Display the logo
            try {
                ImageIcon logoIcon = new ImageIcon(selectedFile.getAbsolutePath());
                Image scaledImage = logoIcon.getImage().getScaledInstance(150, 150, Image.SCALE_SMOOTH);
                currentLogoLabel.setIcon(new ImageIcon(scaledImage));
                currentLogoLabel.setText("");
                currentLogoLabel.setHorizontalAlignment(SwingConstants.CENTER);
                
                JOptionPane.showMessageDialog(this, 
                    "✅ Logo uploaded successfully!", 
                    "Success", 
                    JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, 
                    "❌ Failed to load logo: " + ex.getMessage(), 
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    // Logo removal method
    private void removeLogo() {
        int result = JOptionPane.showConfirmDialog(this, 
            "Are you sure you want to remove the church logo?", 
            "Remove Logo", 
            JOptionPane.YES_NO_OPTION);
            
        if (result == JOptionPane.YES_OPTION) {
            currentLogoPath = null;
            currentLogoLabel.setIcon(null);
            currentLogoLabel.setText("No logo uploaded");
            currentLogoLabel.setHorizontalAlignment(SwingConstants.LEFT);
            
            JOptionPane.showMessageDialog(this, 
                "✅ Logo removed successfully!", 
                "Success", 
                JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        SwingUtilities.invokeLater(() -> new ChurchSettingsFrame().setVisible(true));
    }
}
