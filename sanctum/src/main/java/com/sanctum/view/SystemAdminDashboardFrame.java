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
    static final Color C_BG      = new Color( 10,  12,  28);  // near-black indigo
    static final Color C_SURFACE = new Color( 18,  22,  50);  // dark indigo
    static final Color C_CARD    = new Color( 26,  32,  70);  // card indigo
    static final Color C_CARD2   = new Color( 32,  40,  88);  // lighter card
    static final Color C_BORDER  = new Color( 48,  58, 108);  // border
    static final Color C_ACCENT  = new Color( 99, 149, 255);  // bright blue
    static final Color C_PURPLE  = new Color(167, 110, 255);  // vivid purple
    static final Color C_GOLD    = new Color(255, 200,  55);  // gold
    static final Color C_GREEN   = new Color( 46, 213, 115);  // vivid green
    static final Color C_RED     = new Color(255,  71,  87);  // vivid red
    static final Color C_ORANGE  = new Color(255, 160,  30);  // orange
    static final Color C_CYAN    = new Color( 28, 210, 214);  // teal
    static final Color C_PINK    = new Color(255, 110, 165);  // pink
    static final Color C_TEXT    = new Color(230, 235, 255);  // near-white
    static final Color C_DIM     = new Color(110, 130, 185);  // dim
    static final Color C_SIDEBAR = new Color( 14,  18,  40);  // sidebar bg

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
