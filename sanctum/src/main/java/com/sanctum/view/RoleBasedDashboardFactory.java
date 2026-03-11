package com.sanctum.view;

import javax.swing.*;
import com.sanctum.api.SanctumApiClient;
import java.awt.*;
import java.util.List;
import java.util.Map;

/**
 * Factory class to create role-based dashboards
 * Provides specialized dashboard interfaces for different user roles
 */
public class RoleBasedDashboardFactory {
    
    /**
     * Creates a dashboard frame based on user role
     * @param userRole The role of the current user
     * @param authToken JWT authentication token
     * @return A configured dashboard frame
     */
    public static JFrame createRoleBasedDashboard(String userRole, String authToken) {
        if (userRole == null) return new ChurchAdminFrame();
        
        switch (userRole.toLowerCase()) {
            case "denomination_admin":
            case "admin":
                return new ChurchAdminFrame();
            case "pastor":
                return PastorDashboard.createDashboard(authToken);
            case "treasurer":
                return new TreasurerDashboardFrame();
            case "usher":
                return UsherDashboard.createDashboard(authToken);
            case "youth_leader":
                return YouthLeaderDashboard.createDashboard(authToken);
            case "secretary":
                return SecretaryDashboard.createDashboard(authToken);
            default:
                return new ChurchAdminFrame();
        }
    }
}

/**
 * Base dashboard interface for all role-specific dashboards
 */
abstract class BaseDashboard {
    
    /**
     * Creates a dashboard with common styling and structure
     * @param title The dashboard title
     * @param primaryColor The primary color for dashboard
     * @return The created dashboard frame
     */
    protected static JFrame createBaseDashboard(String title, Color primaryColor) {
        JFrame dashboard = new JFrame(title);
        dashboard.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        dashboard.setSize(1400, 900);
        dashboard.setLocationRelativeTo(null);
        
        // Apply deep purple theme to entire dashboard
        dashboard.getContentPane().setBackground(new Color(75, 0, 130)); // Deep purple background
        
        return dashboard;
    }
    
    /**
     * Creates a styled panel with consistent theming
     * @param backgroundColor The background color
     * @return A styled JPanel
     */
    protected static JPanel createStyledPanel(Color backgroundColor) {
        JPanel panel = new JPanel();
        panel.setBackground(backgroundColor);
        panel.setBorder(BorderFactory.createLineBorder(new Color(102, 51, 153), 2)); // Deep purple border
        return panel;
    }
    
    /**
     * Creates a styled button with consistent theming
     * @param text The button text
     * @param backgroundColor The background color
     * @return A styled JButton
     */
    protected static JButton createStyledButton(String text, Color backgroundColor) {
        JButton button = new JButton(text);
        button.setBackground(backgroundColor);
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Monospaced", Font.BOLD, 12));
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(102, 51, 153), 1), // Deep purple border
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        return button;
    }
    
    /**
     * Creates a styled label with consistent theming
     * @param text The label text
     * @return A styled JLabel
     */
    protected static JLabel createStyledLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(Color.WHITE);
        label.setFont(new Font("Monospaced", Font.BOLD, 14));
        return label;
    }
    
    /**
     * Creates a styled table with consistent theming
     * @return A styled JTable
     */
    protected static JTable createStyledTable() {
        JTable table = new JTable();
        table.setBackground(new Color(75, 0, 130)); // Deep purple background
        table.setForeground(Color.WHITE);
        table.setFont(new Font("Monospaced", Font.PLAIN, 11));
        table.getTableHeader().setBackground(new Color(102, 51, 153)); // Deep purple header
        table.getTableHeader().setForeground(Color.WHITE);
        table.setRowHeight(25);
        return table;
    }
    
    /**
     * Loads dashboard data from API
     * @param dashboard The dashboard frame
     * @param authToken Authentication token
     */
    protected static void loadDashboardData(JFrame dashboard, String authToken, JLabel... labels) {
        SanctumApiClient.getDashboardData().thenAccept(data -> {
            SwingUtilities.invokeLater(() -> {
                if (labels.length >= 1 && labels[0] != null) labels[0].setText("Total Members: " + data.getOrDefault("total_members", 0));
                if (labels.length >= 2 && labels[1] != null) labels[1].setText("Active Staff: " + data.getOrDefault("active_staff", 0));
                if (labels.length >= 3 && labels[2] != null) labels[2].setText("Total Donations: $" + data.getOrDefault("total_donations", 0));
                if (labels.length >= 4 && labels[3] != null) labels[3].setText("Upcoming Events: " + data.getOrDefault("events", 0));
            });
        });
    }
}

/**
 * Denomination Admin Dashboard - Full church management interface
 */
class DenominationAdminDashboard extends BaseDashboard {
    
    public static JFrame createDashboard(String authToken) {
        JFrame dashboard = createBaseDashboard("Sanctum ADMIN — Control Panel", new Color(75, 0, 130));
        
        JPanel mainPanel = createStyledPanel(new Color(75, 0, 130));
        mainPanel.setLayout(new BorderLayout(10, 10));
        
        // Header
        JLabel headerLabel = createStyledLabel("CHURCH MANAGEMENT SYSTEM");
        headerLabel.setFont(new Font("Monospaced", Font.BOLD, 18));
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        headerPanel.setBackground(new Color(102, 51, 153));
        headerPanel.add(headerLabel);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        
        // Stats Overview
        JPanel statsPanel = createStyledPanel(new Color(75, 0, 130));
        statsPanel.setLayout(new GridLayout(2, 3, 10, 10));
        
        JLabel totalMembersLabel = createStyledLabel("Total Members: loading...");
        JLabel activeStaffLabel = createStyledLabel("Active Staff: loading...");
        JLabel totalDonationsLabel = createStyledLabel("Total Donations: loading...");
        JLabel eventsLabel = createStyledLabel("Upcoming Events: loading...");
        
        statsPanel.add(totalMembersLabel);
        statsPanel.add(activeStaffLabel);
        statsPanel.add(totalDonationsLabel);
        statsPanel.add(eventsLabel);
        
        // Quick Actions
        JPanel actionsPanel = createStyledPanel(new Color(75, 0, 130));
        actionsPanel.setLayout(new GridLayout(1, 4, 10, 10));
        
        JButton membersBtn = createStyledButton("👥 Members", new Color(75, 0, 130));
        JButton staffBtn = createStyledButton("👥 Staff Management", new Color(75, 0, 130));
        JButton announcementsBtn = createStyledButton("📢 Announcements", new Color(75, 0, 130));
        JButton donationsBtn = createStyledButton("💰 Donations", new Color(75, 0, 130));
        JButton eventsBtn = createStyledButton("📅 Events", new Color(75, 0, 130));
        JButton settingsBtn = createStyledButton("⚙️ Settings", new Color(75, 0, 130));
        
        actionsPanel.add(membersBtn);
        actionsPanel.add(staffBtn);
        actionsPanel.add(announcementsBtn);
        actionsPanel.add(donationsBtn);
        actionsPanel.add(eventsBtn);
        actionsPanel.add(settingsBtn);
        
        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(statsPanel, BorderLayout.CENTER);
        mainPanel.add(actionsPanel, BorderLayout.SOUTH);
        
        dashboard.add(mainPanel);
        
        // Load real data
        loadDashboardData(dashboard, authToken, totalMembersLabel, activeStaffLabel, totalDonationsLabel, eventsLabel);
        
        return dashboard;
    }
}

/**
 * Pastor Dashboard - Pastoral care and member management
 */
class PastorDashboard extends BaseDashboard {
    
    public static JFrame createDashboard(String authToken) {
        JFrame dashboard = createBaseDashboard("Sanctum PASTOR — Pastoral Care", new Color(75, 0, 130));
        
        JPanel mainPanel = createStyledPanel(new Color(75, 0, 130));
        mainPanel.setLayout(new BorderLayout(10, 10));
        
        // Header
        JLabel headerLabel = createStyledLabel("PASTORAL MINISTRY");
        headerLabel.setFont(new Font("Monospaced", Font.BOLD, 18));
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        headerPanel.setBackground(new Color(102, 51, 153));
        headerPanel.add(headerLabel);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        
        // Member Management
        JPanel memberPanel = createStyledPanel(new Color(75, 0, 130));
        memberPanel.setLayout(new BorderLayout(10, 10));
        
        JLabel memberHeaderLabel = createStyledLabel("MEMBER MANAGEMENT");
        JPanel memberHeaderPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        memberHeaderPanel.setBackground(new Color(102, 51, 153));
        memberHeaderPanel.add(memberHeaderLabel);
        memberHeaderPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        
        // Member table
        JTable memberTable = createStyledTable();
        String[] memberColumns = {"ID", "Name", "Email", "Phone", "Join Date", "Status"};
        memberTable.setModel(new javax.swing.table.DefaultTableModel(new Object[][]{}, memberColumns));
        
        // Stats Overview (Added for data fetching)
        JPanel statsPanel = createStyledPanel(new Color(75, 0, 130));
        statsPanel.setLayout(new GridLayout(1, 4, 10, 10));
        
        JLabel totalMembersLabel = createStyledLabel("Members: loading...");
        JLabel activeStaffLabel = createStyledLabel("Staff: loading...");
        JLabel totalDonationsLabel = createStyledLabel("Donations: loading...");
        JLabel eventsLabel = createStyledLabel("Events: loading...");
        
        statsPanel.add(totalMembersLabel);
        statsPanel.add(activeStaffLabel);
        statsPanel.add(totalDonationsLabel);
        statsPanel.add(eventsLabel);
        
        // Action buttons
        JPanel memberActionsPanel = createStyledPanel(new Color(75, 0, 130));
        memberActionsPanel.setLayout(new GridLayout(1, 3, 10, 10));
        
        JButton addMemberBtn = createStyledButton("➕ Add Member", new Color(75, 0, 130));
        JButton viewMembersBtn = createStyledButton("👥 View All Members", new Color(75, 0, 130));
        JButton attendanceBtn = createStyledButton("📋 Attendance", new Color(75, 0, 130));
        
        memberActionsPanel.add(addMemberBtn);
        memberActionsPanel.add(viewMembersBtn);
        memberActionsPanel.add(attendanceBtn);
        
        memberPanel.add(memberHeaderPanel, BorderLayout.NORTH);
        memberPanel.add(new JScrollPane(memberTable), BorderLayout.CENTER);
        memberPanel.add(memberActionsPanel, BorderLayout.SOUTH);
        
        // Devotional Management
        JPanel devotionalPanel = createStyledPanel(new Color(75, 0, 130));
        devotionalPanel.setLayout(new BorderLayout(10, 10));
        
        JLabel devotionalHeaderLabel = createStyledLabel("DEVOTIONAL CONTENT");
        JPanel devotionalHeaderPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        devotionalHeaderPanel.setBackground(new Color(102, 51, 153));
        devotionalHeaderPanel.add(devotionalHeaderLabel);
        devotionalHeaderPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        
        JButton addDevotionalBtn = createStyledButton("➕ Add Devotional", new Color(75, 0, 130));
        JButton viewDevotionalsBtn = createStyledButton("📖 View Devotionals", new Color(75, 0, 130));
        
        devotionalPanel.add(devotionalHeaderPanel, BorderLayout.NORTH);
        
        JPanel devotionalActionsPanel = createStyledPanel(new Color(75, 0, 130));
        devotionalActionsPanel.setLayout(new GridLayout(1, 2, 10, 10));
        devotionalActionsPanel.add(addDevotionalBtn);
        devotionalActionsPanel.add(viewDevotionalsBtn);
        
        devotionalPanel.add(devotionalActionsPanel, BorderLayout.CENTER);
        
        mainPanel.add(memberPanel, BorderLayout.CENTER);
        mainPanel.add(devotionalPanel, BorderLayout.EAST);
        
        // Load real data
        loadDashboardData(dashboard, authToken);
        
        return dashboard;
    }
}

/**
 * Treasurer Dashboard - Financial management
 */
class TreasurerDashboard extends BaseDashboard {
    
    public static JFrame createDashboard(String authToken) {
        JFrame dashboard = createBaseDashboard("Sanctum TREASURER — Financial Management", new Color(90, 60, 160));
        
        JPanel mainPanel = createStyledPanel(new Color(90, 60, 160));
        mainPanel.setLayout(new BorderLayout(10, 10));
        
        // Header
        JLabel headerLabel = createStyledLabel("FINANCIAL MANAGEMENT");
        headerLabel.setFont(new Font("Monospaced", Font.BOLD, 18));
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        headerPanel.setBackground(new Color(102, 51, 153));
        headerPanel.add(headerLabel);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        
        // Financial Overview
        JPanel financePanel = createStyledPanel(new Color(75, 0, 130));
        financePanel.setLayout(new GridLayout(2, 2, 10, 10));
        
        JLabel totalDonationsLabel = createStyledLabel("Total Donations: loading...");
        JLabel monthlyGivingLabel = createStyledLabel("Monthly Giving: loading...");
        JLabel memberCountLabel = createStyledLabel("Member Count: loading...");
        JLabel activeStaffLabel = createStyledLabel("Active Staff: loading...");
        
        financePanel.add(totalDonationsLabel);
        financePanel.add(monthlyGivingLabel);
        financePanel.add(memberCountLabel);
        financePanel.add(activeStaffLabel);
        
        // Donation Management
        JPanel donationPanel = createStyledPanel(new Color(75, 0, 130));
        donationPanel.setLayout(new BorderLayout(10, 10));
        
        JLabel donationHeaderLabel = createStyledLabel("DONATION RECORDS");
        JPanel donationHeaderPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        donationHeaderPanel.setBackground(new Color(102, 51, 153));
        donationHeaderPanel.add(donationHeaderLabel);
        donationHeaderPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        
        // Donation table
        JTable donationTable = createStyledTable();
        String[] donationColumns = {"Date", "Member", "Amount", "Type", "Method"};
        donationTable.setModel(new javax.swing.table.DefaultTableModel(new Object[][]{}, donationColumns));
        
        // Action buttons
        JPanel donationActionsPanel = createStyledPanel(new Color(75, 0, 130));
        donationActionsPanel.setLayout(new GridLayout(1, 3, 10, 10));
        
        JButton addDonationBtn = createStyledButton("➕ Record Donation", new Color(75, 0, 130));
        JButton viewDonationsBtn = createStyledButton("📊 View All Donations", new Color(75, 0, 130));
        JButton reportsBtn = createStyledButton("📈 Financial Reports", new Color(75, 0, 130));
        
        return dashboard;
    }
}

/**
 * Usher Dashboard - Member services and event management
 */
class UsherDashboard extends BaseDashboard {
    
    public static JFrame createDashboard(String authToken) {
        JFrame dashboard = createBaseDashboard("Sanctum USHER — Member Services", new Color(75, 0, 130));
        
        JPanel mainPanel = createStyledPanel(new Color(75, 0, 130));
        mainPanel.setLayout(new BorderLayout(10, 10));
        
        // Header
        JLabel headerLabel = createStyledLabel("MEMBER SERVICES");
        headerLabel.setFont(new Font("Monospaced", Font.BOLD, 18));
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        headerPanel.setBackground(new Color(102, 51, 153));
        headerPanel.add(headerLabel);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        
        // Member Directory
        JPanel memberPanel = createStyledPanel(new Color(75, 0, 130));
        memberPanel.setLayout(new BorderLayout(10, 10));
        
        // Stats Overview (Added for data fetching)
        JPanel statsPanel = createStyledPanel(new Color(75, 0, 130));
        statsPanel.setLayout(new GridLayout(1, 4, 10, 10));
        
        JLabel totalMembersLabel = createStyledLabel("Members: loading...");
        JLabel activeStaffLabel = createStyledLabel("Staff: loading...");
        JLabel totalDonationsLabel = createStyledLabel("Donations: loading...");
        JLabel eventsLabel = createStyledLabel("Events: loading...");
        
        statsPanel.add(totalMembersLabel);
        statsPanel.add(activeStaffLabel);
        statsPanel.add(totalDonationsLabel);
        statsPanel.add(eventsLabel);

        JLabel memberHeaderLabel = createStyledLabel("MEMBER DIRECTORY");
        JPanel memberHeaderPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        memberHeaderPanel.setBackground(new Color(102, 51, 153));
        memberHeaderPanel.add(memberHeaderLabel);
        memberHeaderPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        
        // Member table
        JTable memberTable = createStyledTable();
        String[] memberColumns = {"ID", "Name", "Contact", "Join Date", "Status"};
        memberTable.setModel(new javax.swing.table.DefaultTableModel(new Object[][]{}, memberColumns));
        
        // Event Management
        JPanel eventPanel = createStyledPanel(new Color(75, 0, 130));
        eventPanel.setLayout(new BorderLayout(10, 10));
        
        JLabel eventHeaderLabel = createStyledLabel("EVENT MANAGEMENT");
        JPanel eventHeaderPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        eventHeaderPanel.setBackground(new Color(102, 51, 153));
        eventHeaderPanel.add(eventHeaderLabel);
        eventHeaderPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        
        // Event table
        JTable eventTable = createStyledTable();
        String[] eventColumns = {"Date", "Event", "Location", "Attendees"};
        eventTable.setModel(new javax.swing.table.DefaultTableModel(new Object[][]{}, eventColumns));
        
        // Action buttons
        JPanel eventActionsPanel = createStyledPanel(new Color(75, 0, 130));
        eventActionsPanel.setLayout(new GridLayout(1, 3, 10, 10));
        
        JButton addEventBtn = createStyledButton("➕ Create Event", new Color(75, 0, 130));
        JButton viewEventsBtn = createStyledButton("📅 View All Events", new Color(75, 0, 130));
        JButton attendanceBtn = createStyledButton("📋 Attendance Tracking", new Color(75, 0, 130));
        
        eventActionsPanel.add(addEventBtn);
        eventActionsPanel.add(viewEventsBtn);
        eventActionsPanel.add(attendanceBtn);
        
        memberPanel.add(memberHeaderPanel, BorderLayout.NORTH);
        memberPanel.add(new JScrollPane(memberTable), BorderLayout.CENTER);
        eventPanel.add(eventHeaderPanel, BorderLayout.NORTH);
        eventPanel.add(new JScrollPane(eventTable), BorderLayout.CENTER);
        eventPanel.add(eventActionsPanel, BorderLayout.SOUTH);
        
        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(statsPanel, BorderLayout.CENTER); // Reuse center for stats or better grid
        mainPanel.add(memberPanel, BorderLayout.CENTER);
        mainPanel.add(eventPanel, BorderLayout.EAST);
        
        // Load real data
        loadDashboardData(dashboard, authToken, totalMembersLabel, activeStaffLabel, totalDonationsLabel, eventsLabel);
        
        return dashboard;
    }
}

/**
 * Youth Leader Dashboard - Youth ministry management
 */
class YouthLeaderDashboard extends BaseDashboard {
    
    public static JFrame createDashboard(String authToken) {
        JFrame dashboard = createBaseDashboard("Sanctum YOUTH LEADER — Youth Ministry", new Color(75, 0, 130));
        
        JPanel mainPanel = createStyledPanel(new Color(75, 0, 130));
        mainPanel.setLayout(new BorderLayout(10, 10));
        
        // Header
        JLabel headerLabel = createStyledLabel("YOUTH MINISTRY");
        headerLabel.setFont(new Font("Monospaced", Font.BOLD, 18));
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        headerPanel.setBackground(new Color(102, 51, 153));
        headerPanel.add(headerLabel);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        
        // Youth Management
        JPanel youthPanel = createStyledPanel(new Color(75, 0, 130));
        youthPanel.setLayout(new BorderLayout(10, 10));
        
        // Stats Overview (Added for data fetching)
        JPanel statsPanel = createStyledPanel(new Color(75, 0, 130));
        statsPanel.setLayout(new GridLayout(1, 4, 10, 10));
        
        JLabel totalMembersLabel = createStyledLabel("Members: loading...");
        JLabel activeStaffLabel = createStyledLabel("Staff: loading...");
        JLabel totalDonationsLabel = createStyledLabel("Donations: loading...");
        JLabel eventsLabel = createStyledLabel("Events: loading...");
        
        statsPanel.add(totalMembersLabel);
        statsPanel.add(activeStaffLabel);
        statsPanel.add(totalDonationsLabel);
        statsPanel.add(eventsLabel);

        JLabel youthHeaderLabel = createStyledLabel("YOUTH MANAGEMENT");
        JPanel youthHeaderPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        youthHeaderPanel.setBackground(new Color(102, 51, 153));
        youthHeaderPanel.add(youthHeaderLabel);
        youthHeaderPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        
        // Youth Activities table
        JTable youthTable = createStyledTable();
        String[] youthColumns = {"Activity", "Date", "Participants", "Status"};
        youthTable.setModel(new javax.swing.table.DefaultTableModel(new Object[][]{}, youthColumns));
        
        // Action buttons
        JPanel youthActionsPanel = createStyledPanel(new Color(75, 0, 130));
        youthActionsPanel.setLayout(new GridLayout(1, 3, 10, 10));
        
        JButton addActivityBtn = createStyledButton("➕ Add Activity", new Color(75, 0, 130));
        JButton viewActivitiesBtn = createStyledButton("🎯 View Activities", new Color(75, 0, 130));
        JButton attendanceBtn = createStyledButton("📊 Attendance Reports", new Color(75, 0, 130));
        
        youthActionsPanel.add(addActivityBtn);
        youthActionsPanel.add(viewActivitiesBtn);
        youthActionsPanel.add(attendanceBtn);
        
        youthPanel.add(youthHeaderPanel, BorderLayout.NORTH);
        youthPanel.add(new JScrollPane(youthTable), BorderLayout.CENTER);
        youthPanel.add(youthActionsPanel, BorderLayout.SOUTH);
        
        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(statsPanel, BorderLayout.CENTER);
        mainPanel.add(youthPanel, BorderLayout.SOUTH);
        
        dashboard.add(mainPanel);
        
        // Load real data
        loadDashboardData(dashboard, authToken, totalMembersLabel, activeStaffLabel, totalDonationsLabel, eventsLabel);
        
        return dashboard;
    }
}

/**
 * Secretary Dashboard - Church administration and records
 */
class SecretaryDashboard extends BaseDashboard {
    public static JFrame createDashboard(String authToken) {
        JFrame dashboard = createBaseDashboard("Sanctum SECRETARY — Church Administration", new Color(75, 0, 130));
        
        JPanel mainPanel = createStyledPanel(new Color(75, 0, 130));
        mainPanel.setLayout(new BorderLayout(10, 10));
        
        // Header
        JLabel headerLabel = createStyledLabel("CHURCH SECRETARIAT");
        headerLabel.setFont(new Font("Monospaced", Font.BOLD, 18));
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        headerPanel.setBackground(new Color(102, 51, 153));
        headerPanel.add(headerLabel);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        
        // Stats Overview
        JPanel statsPanel = createStyledPanel(new Color(75, 0, 130));
        statsPanel.setLayout(new GridLayout(2, 2, 10, 10));
        
        JLabel totalMembersLabel = createStyledLabel("Total Members: loading...");
        JLabel activeStaffLabel = createStyledLabel("Active Staff: loading...");
        JLabel totalDonationsLabel = createStyledLabel("Total Donations: loading...");
        JLabel eventsLabel = createStyledLabel("Upcoming Events: loading...");
        
        statsPanel.add(totalMembersLabel);
        statsPanel.add(activeStaffLabel);
        statsPanel.add(totalDonationsLabel);
        statsPanel.add(eventsLabel);
        
        // Quick Actions
        JPanel actionsPanel = createStyledPanel(new Color(75, 0, 130));
        actionsPanel.setLayout(new GridLayout(1, 4, 10, 10));
        
        actionsPanel.add(createStyledButton("👥 Members", new Color(75, 0, 130)));
        actionsPanel.add(createStyledButton("📢 Announcements", new Color(75, 0, 130)));
        actionsPanel.add(createStyledButton("📅 Events", new Color(75, 0, 130)));
        actionsPanel.add(createStyledButton("📋 Attendance", new Color(75, 0, 130)));
        
        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(statsPanel, BorderLayout.CENTER);
        mainPanel.add(actionsPanel, BorderLayout.SOUTH);
        
        dashboard.add(mainPanel);
        
        // Load real data
        loadDashboardData(dashboard, authToken, totalMembersLabel, activeStaffLabel, totalDonationsLabel, eventsLabel);
        
        return dashboard;
    }
}
