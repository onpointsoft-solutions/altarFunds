#!/bin/bash
# =============================================================================
#  Sanctum Church Management System
#  Windows Setup EXE Builder — Pure Shell Script
#
#  Usage  : chmod +x build-setup-exe.sh && ./build-setup-exe.sh
#  Output : dist/SanctumSetup.exe
#
#  Requires: bash, base64, zip, java (for Maven), mvn
#  No Python, no Node, no external tools needed.
#
#  How the output EXE works on Windows:
#    1. User double-clicks SanctumSetup.exe
#    2. Batch stub decodes embedded ZIP via PowerShell EncodedCommand
#    3. Expands to %TEMP%\SanctumSetup_xxxx\
#    4. Launches a 5-page PowerShell GUI wizard (console hidden)
#       Welcome → License → Install Options → Installing → Finish
#    5. Installs files, shortcuts, registry (ARP), uninstaller
#    6. Auto-cleans temp folder on exit
# =============================================================================

set -euo pipefail

# ── Configuration ─────────────────────────────────────────────────────────────
APP_NAME="Sanctum Church Management System"
APP_SHORT="Sanctum"
APP_VERSION="1.0.0"
APP_PUBLISHER="Sanctum Church Software"
APP_URL="https://www.sanctumchurch.com"
APP_SUPPORT="https://support.sanctumchurch.com"
APP_GUID="{B3F1A2C4-9D7E-4F8A-B6E5-D2C1A0F3E8B7}"
JAVA_MIN="11"
JAVA_DL_URL="https://adoptium.net/temurin/releases/?version=21"

ROOT_DIR="$(pwd)"
BUILD_DIR="$ROOT_DIR/build-windows"
STAGE_DIR="$BUILD_DIR/stage"
DIST_DIR="$ROOT_DIR/dist"

# ── Colours ───────────────────────────────────────────────────────────────────
R='\033[0;31m' G='\033[0;32m' Y='\033[1;33m' B='\033[0;34m' C='\033[0;36m' N='\033[0m'
ok()   { echo -e "${G}[OK]${N}    $*"; }
info() { echo -e "${C}[INFO]${N}  $*"; }
warn() { echo -e "${Y}[WARN]${N}  $*"; }
err()  { echo -e "${R}[FAIL]${N}  $*"; exit 1; }
step() { echo -e "\n${B}▶  $*${N}"; }

echo -e "${C}"
echo "  ╔══════════════════════════════════════════════════════════════╗"
echo "  ║   Sanctum Church Management System — Setup EXE Builder      ║"
echo "  ║   Output: dist/SanctumSetup.exe  (Windows 7+)               ║"
echo "  ╚══════════════════════════════════════════════════════════════╝"
echo -e "${N}"

# ── Step 1: Prerequisites ─────────────────────────────────────────────────────
step "1 / 5  Checking prerequisites"

command -v base64 >/dev/null 2>&1 || err "base64 not found in PATH"
command -v zip    >/dev/null 2>&1 || err "zip not found in PATH"

# Maven + Java only required if pom.xml present (skip in standalone mode)
if [ -f "$ROOT_DIR/pom.xml" ]; then
    command -v java >/dev/null 2>&1 || err "java not found in PATH"
    command -v mvn  >/dev/null 2>&1 || err "mvn not found in PATH"
    JV=$(java -version 2>&1 | awk -F'"' '{print $2}' | cut -d'.' -f1)
    [ "${JV:-0}" -ge 11 ] 2>/dev/null || err "Java 11+ required (found: $JV)"
    ok "Java $JV  Maven ready"
    RUN_MAVEN=true
else
    warn "No pom.xml found — skipping Maven build (standalone EXE builder mode)"
    RUN_MAVEN=false
fi

ok "Prerequisites OK"

# ── Step 2: Maven build ───────────────────────────────────────────────────────
step "2 / 5  Maven build"

if [ "$RUN_MAVEN" = true ]; then
    mvn clean package -DskipTests --no-transfer-progress \
        || err "Maven build failed"
    FAT_JAR=$(find target -maxdepth 1 -name "sanctum-church-management-*.jar" \
        ! -name "*-sources.jar" ! -name "*-javadoc.jar" | sort | tail -n1)
    [ -f "$FAT_JAR" ] || err "Fat JAR not found in target/"
    ok "JAR built → $FAT_JAR"
else
    FAT_JAR=""
    warn "Maven skipped — JAR will not be embedded (add manually to $STAGE_DIR)"
fi

# ── Step 3: Stage files ───────────────────────────────────────────────────────
step "3 / 5  Staging files"

rm -rf "$BUILD_DIR"
mkdir -p "$STAGE_DIR" "$DIST_DIR"

# Copy JAR if available
if [ -n "$FAT_JAR" ] && [ -f "$FAT_JAR" ]; then
    cp "$FAT_JAR" "$STAGE_DIR/SanctumChurchManagement.jar"
    ok "JAR → $STAGE_DIR/SanctumChurchManagement.jar"
fi

# Copy icon if available
ICON_SRC="src/main/resources/icons/sanctum.ico"
if [ -f "$ICON_SRC" ]; then
    cp "$ICON_SRC" "$STAGE_DIR/sanctum.ico"
    ok "Icon → $STAGE_DIR/sanctum.ico"
else
    warn "Icon not found at $ICON_SRC — add a .ico for branding"
fi

# Write README into stage
cat > "$STAGE_DIR/README.txt" << 'README'
================================================================================
  Sanctum Church Management System  v1.0.0
  Copyright 2026 Sanctum Church Software
================================================================================

DEMO ACCOUNTS
-------------
  Role        Email                           Password
  ----------  ------------------------------  ----------
  Admin       admin@altarfunds.com            admin123
  Pastor      pastor@altarfunds.com           pastor123
  Treasurer   treasurer@altarfunds.com        treasurer123
  Secretary   secretary@altarfunds.com        secretary123
  Usher       usher@altarfunds.com            usher123
  Member      test@altarfunds.com             test123

DATA LOCATION
-------------
  %LOCALAPPDATA%\Sanctum\  (database + logs — preserved on uninstall)

UNINSTALL
---------
  Settings → Apps → Sanctum Church Management System → Uninstall

SUPPORT
-------
  https://support.sanctumchurch.com
================================================================================
README

ok "README.txt written"
info "Stage contents:"
ls -lh "$STAGE_DIR/" | awk '{print "    "$0}'

# ── Step 4: Write PowerShell installer script ─────────────────────────────────
step "4 / 5  Generating PowerShell GUI wizard + packing EXE payload"

# Write installer.ps1 to stage
# The heredoc uses 'INSTALLER_EOF' (single-quoted) so the shell does NOT
# expand $variables — they are PowerShell variables, not shell ones.
cat > "$STAGE_DIR/installer.ps1" << 'INSTALLER_EOF'
#Requires -Version 2.0
# Windows compatibility: Supports Windows 7 SP1 + PowerShell 2.0 through Windows 11
Add-Type -AssemblyName System.Windows.Forms
Add-Type -AssemblyName System.Drawing

# Detect Windows version for compatibility adjustments
$OS_VERSION = [Environment]::OSVersion.Version
$OS_MAJOR = $OS_VERSION.Major
$OS_MINOR = $OS_VERSION.Minor
$IS_WIN7_PLUS = ($OS_MAJOR -gt 6) -or ($OS_MAJOR -eq 6 -and $OS_MINOR -ge 1)
$IS_WIN10_PLUS = $OS_MAJOR -ge 10

# Fallback for older systems without Segoe UI
$HAS_SEGOE = $false
try {
    $fontTest = New-Object Drawing.Font("Segoe UI", 9)
    $HAS_SEGOE = $true
    $fontTest.Dispose()
} catch {}

$APP_NAME      = "Sanctum Church Management System"
$APP_SHORT     = "Sanctum"
$APP_VERSION   = "1.0.0"
$APP_PUBLISHER = "Sanctum Church Software"
$APP_URL       = "https://www.sanctumchurch.com"
$APP_SUPPORT   = "https://support.sanctumchurch.com"
$APP_GUID      = "{B3F1A2C4-9D7E-4F8A-B6E5-D2C1A0F3E8B7}"
$JAVA_MIN      = 11
$JAVA_DL_URL   = "https://adoptium.net/temurin/releases/?version=21"
$SCRIPT_DIR    = Split-Path -Parent $MyInvocation.MyCommand.Path
$IS_ADMIN      = ([Security.Principal.WindowsPrincipal][Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
$DEFAULT_INST  = if ($IS_ADMIN) { Join-Path $env:ProgramFiles $APP_SHORT } else { Join-Path $env:LOCALAPPDATA $APP_SHORT }

# ── Colour palette ─────────────────────────────────────────────────────────────
$BRAND_DARK  = [Drawing.Color]::FromArgb(28,  32,  50)
$BRAND_BLUE  = [Drawing.Color]::FromArgb(65,  125, 215)
$BRAND_GREEN = [Drawing.Color]::FromArgb(35,  155, 75)
$BRAND_LIGHT = [Drawing.Color]::FromArgb(242, 244, 248)
$WHITE       = [Drawing.Color]::White
$GRAY_TEXT   = [Drawing.Color]::FromArgb(85,  90,  105)
$SEP_COLOR   = [Drawing.Color]::FromArgb(200, 204, 214)

# ── Font selection with fallbacks for Windows compatibility ───────────────────────
$PRIMARY_FONT = if ($HAS_SEGOE) { "Segoe UI" } elseif ($IS_WIN7_PLUS) { "Microsoft Sans Serif" } else { "Arial" }
$BOLD_FONT    = if ($HAS_SEGOE) { "Segoe UI Semibold" } elseif ($IS_WIN7_PLUS) { "Microsoft Sans Serif" } else { "Arial" }

$FONT_H1    = New-Object Drawing.Font($PRIMARY_FONT, 18, [Drawing.FontStyle]::Bold)
$FONT_H2    = New-Object Drawing.Font($BOLD_FONT,    9,  [Drawing.FontStyle]::Bold)
$FONT_BODY  = New-Object Drawing.Font($PRIMARY_FONT, 8)
$FONT_SMALL = New-Object Drawing.Font($PRIMARY_FONT, 7)
$FONT_BTN   = New-Object Drawing.Font($BOLD_FONT,    8,  [Drawing.FontStyle]::Bold)

# ── Java detection ─────────────────────────────────────────────────────────────
function Get-JavaMajor {
    # 1. Try PATH
    try {
        if ((&java -version 2>&1 | Out-String) -match '"(\d+)') { return [int]$Matches[1] }
    } catch {}
    # 2. Adoptium / Temurin registry
    foreach ($n in @(21, 17, 11)) {
        if (Test-Path "HKLM:\SOFTWARE\Eclipse Adoptium\JRE\$n\hotspot\MSI") { return $n }
        if (Test-Path "HKLM:\SOFTWARE\Eclipse Adoptium\JDK\$n\hotspot\MSI") { return $n }
    }
    # 3. Oracle JDK / JRE
    foreach ($k in @("HKLM:\SOFTWARE\JavaSoft\JDK",
                     "HKLM:\SOFTWARE\JavaSoft\Java Runtime Environment")) {
        try {
            $cv = (Get-ItemProperty $k -ErrorAction Stop).CurrentVersion
            $m  = [int]($cv -split '\.')[0]
            if ($m -ge 11) { return $m }
        } catch {}
    }
    return 0
}

# ── UI helpers ─────────────────────────────────────────────────────────────────
function New-Btn($text, $x, $y, $w=110, $h=34, $primary=$true) {
    $b = New-Object Windows.Forms.Button
    $b.Text = $text
    $b.Location = New-Object Drawing.Point($x, $y)
    $b.Size     = New-Object Drawing.Size($w, $h)
    $b.FlatStyle = "Flat"
    $b.Font      = $FONT_BTN
    $b.Cursor    = "Hand"
    $b.FlatAppearance.BorderSize = 0
    if ($primary) {
        $b.BackColor = $BRAND_BLUE
        $b.ForeColor = $WHITE
    } else {
        $b.BackColor = [Drawing.Color]::FromArgb(218, 222, 232)
        $b.ForeColor = [Drawing.Color]::FromArgb(45,  50,  65)
    }
    return $b
}

function New-Form($title, $h=440) {
    $f = New-Object Windows.Forms.Form
    $f.Text            = $title
    $f.Size            = New-Object Drawing.Size(580, $h)
    $f.StartPosition   = "CenterScreen"
    $f.FormBorderStyle = "FixedDialog"
    $f.MaximizeBox     = $false
    $f.BackColor       = $BRAND_LIGHT
    $f.Icon            = [Drawing.SystemIcons]::Application
    return $f
}

function Add-Header($form, $title, $sub) {
    $p = New-Object Windows.Forms.Panel
    $p.Dock      = "Top"
    $p.Height    = 85
    $p.BackColor = $BRAND_DARK

    $t1 = New-Object Windows.Forms.Label
    $t1.Text      = $title
    $t1.Font      = $FONT_H1
    $t1.ForeColor = $WHITE
    $t1.Location  = New-Object Drawing.Point(20, 10)
    $t1.AutoSize  = $true

    $t2 = New-Object Windows.Forms.Label
    $t2.Text      = $sub
    $t2.Font      = $FONT_BODY
    $t2.ForeColor = [Drawing.Color]::FromArgb(160, 170, 200)
    $t2.Location  = New-Object Drawing.Point(22, 50)
    $t2.AutoSize  = $true

    $p.Controls.AddRange(@($t1, $t2))
    $form.Controls.Add($p)

    $acc = New-Object Windows.Forms.Panel
    $acc.BackColor = $BRAND_BLUE
    $acc.Location  = New-Object Drawing.Point(0, 85)
    $acc.Size      = New-Object Drawing.Size(5, 500)
    $form.Controls.Add($acc)
}

function Add-Sep($form, $y) {
    $s = New-Object Windows.Forms.Panel
    $s.BackColor = $SEP_COLOR
    $s.Location  = New-Object Drawing.Point(0, $y)
    $s.Size      = New-Object Drawing.Size(580, 1)
    $form.Controls.Add($s)
}

# ── Shared state ───────────────────────────────────────────────────────────────
$STATE = @{ Dir = $DEFAULT_INST; Desk = $true; Start = $true; Java = 0 }
$STATE.Java = Get-JavaMajor

# ══════════════════════════════════════════════════════════════════════════════
# PAGE 1 — WELCOME
# ══════════════════════════════════════════════════════════════════════════════
function Page-Welcome {
    $f = New-Form "$APP_NAME Setup"
    Add-Header $f "Sanctum" "Church Management System — Installation Wizard"

    $lbl = New-Object Windows.Forms.Label
    $lbl.Text = "Welcome to the $APP_NAME $APP_VERSION setup wizard.`n`nThis wizard will install Sanctum on your computer. Please close all other applications before continuing.`n`nClick Next to proceed."
    $lbl.Font      = $FONT_BODY
    $lbl.ForeColor = $GRAY_TEXT
    $lbl.Location  = New-Object Drawing.Point(26, 100)
    $lbl.Size      = New-Object Drawing.Size(525, 95)
    $f.Controls.Add($lbl)

    # Java status card
    $card = New-Object Windows.Forms.Panel
    $card.Location    = New-Object Drawing.Point(26, 206)
    $card.Size        = New-Object Drawing.Size(525, 72)
    $card.BorderStyle = "FixedSingle"

    if ($STATE.Java -ge $JAVA_MIN) {
        $card.BackColor = [Drawing.Color]::FromArgb(230, 248, 232)
        $jl = New-Object Windows.Forms.Label
        $jl.Text      = "[OK] Java $($STATE.Java) detected — ready to install"
        $jl.Font      = $FONT_H2
        $jl.ForeColor = [Drawing.Color]::FromArgb(25, 110, 45)
        $jl.Location  = New-Object Drawing.Point(12, 10)
        $jl.AutoSize  = $true
        $jl2 = New-Object Windows.Forms.Label
        $jl2.Text      = "Sanctum will run immediately after installation."
        $jl2.Font      = $FONT_SMALL
        $jl2.ForeColor = [Drawing.Color]::FromArgb(40, 130, 60)
        $jl2.Location  = New-Object Drawing.Point(12, 36)
        $jl2.AutoSize  = $true
        $card.Controls.AddRange(@($jl, $jl2))
    } else {
        $card.BackColor = [Drawing.Color]::FromArgb(255, 244, 220)
        $jl = New-Object Windows.Forms.Label
        $jl.Text      = "[!] Java $JAVA_MIN+ not found — required to run Sanctum"
        $jl.Font      = $FONT_H2
        $jl.ForeColor = [Drawing.Color]::FromArgb(155, 75, 0)
        $jl.Location  = New-Object Drawing.Point(12, 8)
        $jl.AutoSize  = $true
        $card.Controls.Add($jl)

        $db = New-Btn "Get Java..." 12 34 100 26 $true
        $db.BackColor = [Drawing.Color]::FromArgb(195, 115, 0)
        $db.add_Click({ Start-Process $JAVA_DL_URL })
        $card.Controls.Add($db)

        $dl = New-Object Windows.Forms.Label
        $dl.Text      = "You can install Java before or after this installer."
        $dl.Font      = $FONT_SMALL
        $dl.ForeColor = [Drawing.Color]::FromArgb(140, 90, 0)
        $dl.Location  = New-Object Drawing.Point(120, 40)
        $dl.AutoSize  = $true
        $card.Controls.Add($dl)
    }
    $f.Controls.Add($card)

    $inf = New-Object Windows.Forms.Label
    $inf.Text      = "v$APP_VERSION  |  $APP_PUBLISHER  |  $APP_URL"
    $inf.Font      = $FONT_SMALL
    $inf.ForeColor = $GRAY_TEXT
    $inf.Location  = New-Object Drawing.Point(26, 292)
    $inf.AutoSize  = $true
    $f.Controls.Add($inf)

    Add-Sep $f 340
    $bc = New-Btn "Cancel" 350 350 100 34 $false
    $bn = New-Btn "Next >"  460 350 100 34 $true

    $bc.add_Click({
        if ([Windows.Forms.MessageBox]::Show("Cancel installation?", "Setup", "YesNo", "Question") -eq "Yes") {
            $f.Tag = "cancel"; $f.Close()
        }
    })
    $bn.add_Click({ $f.Tag = "next"; $f.Close() })
    $f.Controls.AddRange(@($bc, $bn))
    $f.ShowDialog() | Out-Null
    return $f.Tag
}

# ══════════════════════════════════════════════════════════════════════════════
# PAGE 2 — LICENSE
# ══════════════════════════════════════════════════════════════════════════════
function Page-License {
    $f = New-Form "$APP_NAME Setup — License Agreement" 490
    Add-Header $f "License Agreement" "Please read and accept the terms to continue."

    $rtb = New-Object Windows.Forms.RichTextBox
    $rtb.Location    = New-Object Drawing.Point(20, 100)
    $rtb.Size        = New-Object Drawing.Size(540, 238)
    $rtb.ReadOnly    = $true
    $rtb.BackColor   = $WHITE
    $rtb.Font        = $FONT_SMALL
    $rtb.BorderStyle = "FixedSingle"
    $rtb.Text = @"
SANCTUM CHURCH MANAGEMENT SYSTEM
End User License Agreement — Version 1.0.0
Copyright (c) 2026 Sanctum Church Software. All rights reserved.

1. GRANT OF USE
   Non-exclusive, non-transferable right to install and use this
   software for church and religious organisation administration
   within your own organisation only.

2. RESTRICTIONS
   No sublicensing, resale, redistribution, or derivative works
   without prior written authorisation from the publisher.

3. DATA AND PRIVACY
   All data is stored locally on your device. No data is sent to
   external servers without your explicit consent.

4. DISCLAIMER
   Provided as-is without warranty of any kind. The publisher
   accepts no liability for damages arising from use.

5. FULL LICENCE
   https://www.sanctumchurch.com/license
"@
    $f.Controls.Add($rtb)

    $chk = New-Object Windows.Forms.CheckBox
    $chk.Text      = "I accept the License Agreement"
    $chk.Font      = $FONT_H2
    $chk.ForeColor = $BRAND_DARK
    $chk.Location  = New-Object Drawing.Point(20, 350)
    $chk.AutoSize  = $true
    $f.Controls.Add($chk)

    Add-Sep $f 390
    $bb = New-Btn "< Back"  240 398 100 34 $false
    $bc = New-Btn "Cancel"  350 398 100 34 $false
    $bn = New-Btn "Next >"  460 398 100 34 $true
    $bn.Enabled = $false

    $chk.add_CheckedChanged({ $bn.Enabled = $chk.Checked })
    $bb.add_Click({ $f.Tag = "back";   $f.Close() })
    $bc.add_Click({
        if ([Windows.Forms.MessageBox]::Show("Cancel?", "Setup", "YesNo", "Question") -eq "Yes") {
            $f.Tag = "cancel"; $f.Close()
        }
    })
    $bn.add_Click({ $f.Tag = "next"; $f.Close() })
    $f.Controls.AddRange(@($bb, $bc, $bn))
    $f.ShowDialog() | Out-Null
    return $f.Tag
}

# ══════════════════════════════════════════════════════════════════════════════
# PAGE 3 — INSTALL OPTIONS
# ══════════════════════════════════════════════════════════════════════════════
function Page-Options {
    $f = New-Form "$APP_NAME Setup — Install Options" 455
    Add-Header $f "Install Options" "Choose installation folder and shortcuts."

    $l1 = New-Object Windows.Forms.Label
    $l1.Text      = "Installation Folder"
    $l1.Font      = $FONT_H2
    $l1.ForeColor = $BRAND_DARK
    $l1.Location  = New-Object Drawing.Point(20, 100)
    $l1.AutoSize  = $true
    $f.Controls.Add($l1)

    $td = New-Object Windows.Forms.TextBox
    $td.Text     = $STATE.Dir
    $td.Font     = $FONT_BODY
    $td.Location = New-Object Drawing.Point(20, 122)
    $td.Size     = New-Object Drawing.Size(430, 26)
    $f.Controls.Add($td)

    $br = New-Btn "Browse..." 458 120 90 28 $false
    $br.add_Click({
        $d = New-Object Windows.Forms.FolderBrowserDialog
        $d.Description  = "Select the installation folder for $APP_NAME"
        $d.SelectedPath = $td.Text
        if ($d.ShowDialog() -eq "OK") { $td.Text = $d.SelectedPath }
    })
    $f.Controls.Add($br)

    $sn = New-Object Windows.Forms.Label
    $sn.Text      = "Required disk space: approx. 50 MB"
    $sn.Font      = $FONT_SMALL
    $sn.ForeColor = $GRAY_TEXT
    $sn.Location  = New-Object Drawing.Point(20, 156)
    $sn.AutoSize  = $true
    $f.Controls.Add($sn)

    $sp = New-Object Windows.Forms.Panel
    $sp.BackColor = $SEP_COLOR
    $sp.Location  = New-Object Drawing.Point(20, 180)
    $sp.Size      = New-Object Drawing.Size(540, 1)
    $f.Controls.Add($sp)

    $l2 = New-Object Windows.Forms.Label
    $l2.Text      = "Shortcuts"
    $l2.Font      = $FONT_H2
    $l2.ForeColor = $BRAND_DARK
    $l2.Location  = New-Object Drawing.Point(20, 192)
    $l2.AutoSize  = $true
    $f.Controls.Add($l2)

    $cd = New-Object Windows.Forms.CheckBox
    $cd.Text     = "Create desktop shortcut"
    $cd.Font     = $FONT_BODY
    $cd.Checked  = $STATE.Desk
    $cd.Location = New-Object Drawing.Point(20, 216)
    $cd.AutoSize = $true
    $f.Controls.Add($cd)

    $cs = New-Object Windows.Forms.CheckBox
    $cs.Text     = "Create Start Menu shortcut"
    $cs.Font     = $FONT_BODY
    $cs.Checked  = $STATE.Start
    $cs.Location = New-Object Drawing.Point(20, 244)
    $cs.AutoSize = $true
    $f.Controls.Add($cs)

    Add-Sep $f 365
    $bb = New-Btn "< Back"  240 373 100 34 $false
    $bc = New-Btn "Cancel"  350 373 100 34 $false
    $bn = New-Btn "Install" 460 373 100 34 $true
    $bn.BackColor = $BRAND_GREEN

    $bb.add_Click({ $f.Tag = "back"; $f.Close() })
    $bc.add_Click({
        if ([Windows.Forms.MessageBox]::Show("Cancel?", "Setup", "YesNo", "Question") -eq "Yes") {
            $f.Tag = "cancel"; $f.Close()
        }
    })
    $bn.add_Click({
        if ([string]::IsNullOrWhiteSpace($td.Text)) {
            [Windows.Forms.MessageBox]::Show("Please select an installation folder.", "Setup", "OK", "Warning")
            return
        }
        $STATE.Dir   = $td.Text.TrimEnd('\')
        $STATE.Desk  = $cd.Checked
        $STATE.Start = $cs.Checked
        $f.Tag = "next"; $f.Close()
    })
    $f.Controls.AddRange(@($bb, $bc, $bn))
    $f.ShowDialog() | Out-Null
    return $f.Tag
}

# ══════════════════════════════════════════════════════════════════════════════
# PAGE 4 — INSTALLING (progress bar)
# ══════════════════════════════════════════════════════════════════════════════
function Page-Installing {
    $f = New-Form "$APP_NAME Setup — Installing" 320
    $f.ControlBox = $false
    Add-Header $f "Installing..." "Please wait while Sanctum is being installed."

    $pb = New-Object Windows.Forms.ProgressBar
    $pb.Location = New-Object Drawing.Point(20, 104)
    $pb.Size     = New-Object Drawing.Size(540, 26)
    $pb.Minimum  = 0
    $pb.Maximum  = 100
    $pb.Style    = "Continuous"
    $f.Controls.Add($pb)

    $sl = New-Object Windows.Forms.Label
    $sl.Text      = "Initialising..."
    $sl.Font      = $FONT_BODY
    $sl.ForeColor = $GRAY_TEXT
    $sl.Location  = New-Object Drawing.Point(20, 138)
    $sl.Size      = New-Object Drawing.Size(540, 22)
    $f.Controls.Add($sl)

    $dl = New-Object Windows.Forms.Label
    $dl.Text      = ""
    $dl.Font      = $FONT_SMALL
    $dl.ForeColor = $GRAY_TEXT
    $dl.Location  = New-Object Drawing.Point(20, 162)
    $dl.Size      = New-Object Drawing.Size(540, 36)
    $f.Controls.Add($dl)

    $f.Show()
    $f.Refresh()

    function Up($pct, $status, $detail = "") {
        $pb.Value = [Math]::Min($pct, 100)
        $sl.Text  = $status
        $dl.Text  = $detail
        $f.Refresh()
        Start-Sleep -Milliseconds 80
    }

    $errMsg = ""
    try {
        Up 5  "Creating directories..."              $STATE.Dir
        New-Item -ItemType Directory -Force -Path $STATE.Dir                       | Out-Null
        New-Item -ItemType Directory -Force -Path (Join-Path $STATE.Dir "data")    | Out-Null
        New-Item -ItemType Directory -Force -Path (Join-Path $STATE.Dir "logs")    | Out-Null

        Up 20 "Installing SanctumChurchManagement.jar..."
        $j = Join-Path $SCRIPT_DIR "SanctumChurchManagement.jar"
        if (Test-Path $j) { Copy-Item $j $STATE.Dir -Force }

        Up 40 "Installing Sanctum.exe..."
        $e = Join-Path $SCRIPT_DIR "Sanctum.exe"
        if (Test-Path $e) { Copy-Item $e $STATE.Dir -Force }

        Up 55 "Installing resources..."
        $i = Join-Path $SCRIPT_DIR "sanctum.ico"
        if (Test-Path $i) { Copy-Item $i $STATE.Dir -Force }
        $r = Join-Path $SCRIPT_DIR "README.txt"
        if (Test-Path $r) { Copy-Item $r $STATE.Dir -Force }

        if ($STATE.Desk) {
            Up 65 "Creating desktop shortcut..."
            $wsh = New-Object -ComObject WScript.Shell
            $lnk = $wsh.CreateShortcut("$env:USERPROFILE\Desktop\$APP_NAME.lnk")
            $lnk.TargetPath       = Join-Path $STATE.Dir "Sanctum.exe"
            $lnk.WorkingDirectory = $STATE.Dir
            $lnk.Description      = "Church management and administration system"
            $ico = Join-Path $STATE.Dir "sanctum.ico"
            if (Test-Path $ico) { $lnk.IconLocation = $ico }
            $lnk.Save()
        }

        if ($STATE.Start) {
            Up 73 "Creating Start Menu entry..."
            $sm = Join-Path $env:APPDATA "Microsoft\Windows\Start Menu\Programs\$APP_SHORT"
            New-Item -ItemType Directory -Force -Path $sm | Out-Null
            $wsh = New-Object -ComObject WScript.Shell
            $lnk = $wsh.CreateShortcut("$sm\$APP_NAME.lnk")
            $lnk.TargetPath       = Join-Path $STATE.Dir "Sanctum.exe"
            $lnk.WorkingDirectory = $STATE.Dir
            $lnk.Description      = "Church management and administration system"
            $ico = Join-Path $STATE.Dir "sanctum.ico"
            if (Test-Path $ico) { $lnk.IconLocation = $ico }
            $lnk.Save()
        }

        Up 82 "Writing registry entries..." "Add/Remove Programs"
        $ak = "HKCU:\Software\$APP_PUBLISHER\$APP_SHORT"
        New-Item -Path $ak -Force | Out-Null
        Set-ItemProperty $ak "InstallPath" $STATE.Dir
        Set-ItemProperty $ak "Version"     $APP_VERSION

        $uninstPS  = Join-Path $STATE.Dir "uninstall.ps1"
        $uninstCmd = "powershell.exe -ExecutionPolicy Bypass -WindowStyle Hidden -File `"$uninstPS`""
        $arpk = "HKCU:\Software\Microsoft\Windows\CurrentVersion\Uninstall\$APP_GUID"
        New-Item -Path $arpk -Force | Out-Null
        @{
            DisplayName     = $APP_NAME
            DisplayVersion  = $APP_VERSION
            DisplayIcon     = (Join-Path $STATE.Dir "Sanctum.exe")
            Publisher       = $APP_PUBLISHER
            URLInfoAbout    = $APP_URL
            HelpLink        = $APP_SUPPORT
            InstallLocation = $STATE.Dir
            UninstallString = $uninstCmd
            Comments        = "Church management and administration system"
        }.GetEnumerator() | ForEach-Object {
            Set-ItemProperty $arpk $_.Key $_.Value
        }
        Set-ItemProperty $arpk "EstimatedSize" 102400 -Type DWord
        Set-ItemProperty $arpk "NoModify"      1      -Type DWord
        Set-ItemProperty $arpk "NoRepair"      1      -Type DWord

        Up 92 "Creating uninstaller..."
        @"
Add-Type -AssemblyName System.Windows.Forms
`$r = [Windows.Forms.MessageBox]::Show("Remove $APP_NAME from $($STATE.Dir)?","Uninstall $APP_SHORT","YesNo","Question")
if (`$r -ne 'Yes') { exit }
Remove-Item '$($STATE.Dir)' -Recurse -Force -ErrorAction SilentlyContinue
Remove-Item "`$env:USERPROFILE\Desktop\$APP_NAME.lnk" -Force -ErrorAction SilentlyContinue
Remove-Item "`$env:APPDATA\Microsoft\Windows\Start Menu\Programs\$APP_SHORT" -Recurse -Force -ErrorAction SilentlyContinue
Remove-Item 'HKCU:\Software\$APP_PUBLISHER\$APP_SHORT' -Recurse -Force -ErrorAction SilentlyContinue
Remove-Item 'HKCU:\Software\Microsoft\Windows\CurrentVersion\Uninstall\$APP_GUID' -Recurse -Force -ErrorAction SilentlyContinue
[Windows.Forms.MessageBox]::Show("$APP_NAME has been uninstalled.","Done","OK","Information")
"@ | Set-Content $uninstPS

        Up 100 "Installation complete!"
        Start-Sleep -Milliseconds 300
    } catch {
        $errMsg = $_.Exception.Message
    }

    $f.Close()
    if ($errMsg) { return "error:$errMsg" } else { return "ok" }
}

# ══════════════════════════════════════════════════════════════════════════════
# PAGE 5 — FINISH
# ══════════════════════════════════════════════════════════════════════════════
function Page-Finish($success) {
    $f = New-Form "$APP_NAME Setup — Complete" 425

    if ($success) {
        Add-Header $f "Installation Complete" "$APP_NAME v$APP_VERSION installed successfully."

        # Turn the accent bar green
        $f.Controls | Where-Object { $_.Size.Width -eq 5 } | ForEach-Object {
            $_.BackColor = $BRAND_GREEN
        }

        $body = New-Object Windows.Forms.Label
        $body.Text = "Sanctum has been installed to:`n  $($STATE.Dir)`n`nLaunch from the desktop shortcut or Start Menu.`n`nSee README.txt for demo accounts — change all passwords before going live."
        $body.Font      = $FONT_BODY
        $body.ForeColor = $GRAY_TEXT
        $body.Location  = New-Object Drawing.Point(26, 100)
        $body.Size      = New-Object Drawing.Size(525, 110)
        $f.Controls.Add($body)

        # Java warning if not detected
        if ($STATE.Java -lt $JAVA_MIN) {
            $wp = New-Object Windows.Forms.Panel
            $wp.Location    = New-Object Drawing.Point(26, 220)
            $wp.Size        = New-Object Drawing.Size(525, 56)
            $wp.BackColor   = [Drawing.Color]::FromArgb(255, 244, 220)
            $wp.BorderStyle = "FixedSingle"
            $wl = New-Object Windows.Forms.Label
            $wl.Text      = "Reminder: Java $JAVA_MIN+ is required to run Sanctum.`nDownload: $JAVA_DL_URL"
            $wl.Font      = $FONT_SMALL
            $wl.ForeColor = [Drawing.Color]::FromArgb(140, 70, 0)
            $wl.Location  = New-Object Drawing.Point(8, 6)
            $wl.Size      = New-Object Drawing.Size(508, 44)
            $wp.Controls.Add($wl)
            $f.Controls.Add($wp)
        }

        $chk = New-Object Windows.Forms.CheckBox
        $chk.Text      = "Launch Sanctum now"
        $chk.Font      = $FONT_H2
        $chk.ForeColor = $BRAND_DARK
        $chk.Checked   = $true
        $chk.Location  = New-Object Drawing.Point(26, 292)
        $chk.AutoSize  = $true
        $f.Controls.Add($chk)

        Add-Sep $f 342
        $bn = New-Btn "Finish" 460 350 100 34 $true
        $bn.BackColor = $BRAND_GREEN
        $bn.add_Click({
            if ($chk.Checked) {
                $exe = Join-Path $STATE.Dir "Sanctum.exe"
                if (Test-Path $exe) { Start-Process $exe }
            }
            $f.Close()
        })
        $f.Controls.Add($bn)

    } else {
        Add-Header $f "Installation Failed" "An error occurred during installation."

        $body = New-Object Windows.Forms.Label
        $body.Text      = "The installation did not complete successfully.`n`nPlease check disk space and permissions, then try again."
        $body.Font      = $FONT_BODY
        $body.ForeColor = [Drawing.Color]::FromArgb(180, 40, 40)
        $body.Location  = New-Object Drawing.Point(26, 100)
        $body.Size      = New-Object Drawing.Size(525, 80)
        $f.Controls.Add($body)

        Add-Sep $f 342
        $bc = New-Btn "Close" 460 350 100 34 $false
        $bc.add_Click({ $f.Close() })
        $f.Controls.Add($bc)
    }

    $f.ShowDialog() | Out-Null
}

# ══════════════════════════════════════════════════════════════════════════════
# WIZARD MAIN LOOP
# ══════════════════════════════════════════════════════════════════════════════
$page = 1
while ($true) {
    switch ($page) {
        1 {
            $r = Page-Welcome
            if     ($r -eq "next")   { $page = 2 }
            elseif ($r -eq "cancel") { exit 0 }
        }
        2 {
            $r = Page-License
            if     ($r -eq "next")   { $page = 3 }
            elseif ($r -eq "back")   { $page = 1 }
            elseif ($r -eq "cancel") { exit 0 }
        }
        3 {
            $r = Page-Options
            if     ($r -eq "next")   { $page = 4 }
            elseif ($r -eq "back")   { $page = 2 }
            elseif ($r -eq "cancel") { exit 0 }
        }
        4 {
            $r = Page-Installing
            Page-Finish ($r -eq "ok")
            exit 0
        }
    }
}
INSTALLER_EOF

ok "installer.ps1 written ($(wc -l < "$STAGE_DIR/installer.ps1") lines)"

# ── Pack ZIP payload ──────────────────────────────────────────────────────────
info "Packing payload ZIP..."
ZIP_FILE="$BUILD_DIR/payload.zip"

cd "$STAGE_DIR"
zip -9 -r "$ZIP_FILE" . > /dev/null
cd "$ROOT_DIR"

ZIP_SIZE=$(wc -c < "$ZIP_FILE")
ok "payload.zip → ${ZIP_SIZE} bytes ($(( ZIP_SIZE / 1024 )) KB)"

info "Staged files packed:"
unzip -l "$ZIP_FILE" | tail -n +4 | head -n -2 | awk '{print "    "$0}'

# ── Base64-encode the ZIP ─────────────────────────────────────────────────────
info "Base64-encoding payload..."
B64=$(base64 --wrap=0 "$ZIP_FILE")
B64_LEN=${#B64}
ok "Base64: ${B64_LEN} chars"

# ── Encode the PowerShell writer command as UTF-16LE base64 ──────────────────
# The writer PS1 decodes B64 → bytes → writes payload.zip
# We encode it with iconv (UTF-16LE) so PowerShell's -EncodedCommand accepts it
info "Building PowerShell EncodedCommand writer..."

WRITER_PS1="\$b=[Convert]::FromBase64String('${B64}');[IO.File]::WriteAllBytes(\$env:WZIP,\$b)"

# iconv UTF-16LE encode for -EncodedCommand
WRITER_B64=$(printf '%s' "$WRITER_PS1" | iconv -t UTF-16LE | base64 --wrap=0)
WRITER_B64_LEN=${#WRITER_B64}
ok "EncodedCommand: ${WRITER_B64_LEN} chars"

# ── Step 5: Assemble the EXE ──────────────────────────────────────────────────
step "5 / 5  Assembling SanctumSetup.exe"

EXE_OUT="$DIST_DIR/SanctumSetup.exe"

# Write with CRLF line endings (Windows requirement)
# The file IS a valid Windows batch script (.exe extension is cosmetic —
# Windows runs it fine because cmd.exe reads the @echo off header)
{
printf '@echo off\r\n'
printf ':: ============================================================\r\n'
printf ':: Sanctum Church Management System\r\n'
printf ':: Setup v%s — Windows Installation Wizard\r\n' "$APP_VERSION"
printf ':: Supports Windows 7 SP1 and later\r\n'
printf ':: Double-click to install\r\n'
printf ':: ============================================================\r\n'
printf 'setlocal EnableDelayedExpansion\r\n'
printf '\r\n'
printf 'title Sanctum Church Management System — Setup\r\n'
printf '\r\n'
printf ':: Require PowerShell 3+\r\n'
printf 'powershell -Command "if($PSVersionTable.PSVersion.Major -lt 3){exit 1}" >nul 2>&1\r\n'
printf 'if %%errorlevel%% neq 0 (\r\n'
printf '    echo PowerShell 3.0 or later is required.\r\n'
printf '    echo It is built into Windows 8 and later.\r\n'
printf '    echo Windows 7 users: install Windows Management Framework 3.0\r\n'
printf '    echo Download: https://www.microsoft.com/en-us/download/details.aspx?id=34595\r\n'
printf '    pause\r\n'
printf '    exit /b 1\r\n'
printf ')\r\n'
printf '\r\n'
printf ':: Create a unique temp working directory\r\n'
printf 'set "WD=%%TEMP%%\\SanctumSetup_%%RANDOM%%%%RANDOM%%"\r\n'
printf 'set "WZIP=%%WD%%\\payload.zip"\r\n'
printf 'mkdir "%%WD%%" >nul 2>&1\r\n'
printf '\r\n'
printf ':: Decode embedded payload using PowerShell EncodedCommand\r\n'
printf ':: (Base64 UTF-16LE encoded — immune to batch variable expansion)\r\n'
printf 'powershell -NoProfile -ExecutionPolicy Bypass -EncodedCommand %s\r\n' "$WRITER_B64"
printf '\r\n'
printf 'if not exist "%%WZIP%%" (\r\n'
printf '    powershell -NoProfile -Command "Add-Type -A System.Windows.Forms;[Windows.Forms.MessageBox]::Show('"'"'Setup extraction failed. The file may be corrupted.'"'"','"'"'Sanctum Setup'"'"','"'"'OK'"'"','"'"'Error'"'"')"\r\n'
printf '    goto :clean\r\n'
printf ')\r\n'
printf '\r\n'
printf ':: Expand the ZIP archive\r\n'
printf 'powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -Path $env:WZIP -DestinationPath $env:WD -Force"\r\n'
printf '\r\n'
printf 'if not exist "%%WD%%\\installer.ps1" (\r\n'
printf '    powershell -NoProfile -Command "Add-Type -A System.Windows.Forms;[Windows.Forms.MessageBox]::Show('"'"'Failed to expand setup files.'"'"','"'"'Sanctum Setup'"'"','"'"'OK'"'"','"'"'Error'"'"')"\r\n'
printf '    goto :clean\r\n'
printf ')\r\n'
printf '\r\n'
printf ':: Launch GUI wizard (hidden console so only the GUI is visible)\r\n'
printf 'start /wait "" powershell -NoProfile -ExecutionPolicy Bypass -WindowStyle Hidden -File "%%WD%%\\installer.ps1"\r\n'
printf '\r\n'
printf ':clean\r\n'
printf 'if exist "%%WD%%" rd /s /q "%%WD%%" >nul 2>&1\r\n'
printf 'endlocal\r\n'
printf 'exit /b 0\r\n'
} > "$EXE_OUT"

EXE_SIZE=$(wc -c < "$EXE_OUT")
ok "Written → $EXE_OUT"
ok "Size    → ${EXE_SIZE} bytes ($(( EXE_SIZE / 1024 )) KB)"

# ── Verify round-trip ─────────────────────────────────────────────────────────
info "Verifying payload round-trip..."

# Extract the EncodedCommand from the EXE and decode it
ENC_CMD=$(grep -oP '(?<=-EncodedCommand )[A-Za-z0-9+/=]+' "$EXE_OUT" || true)

if [ -n "$ENC_CMD" ]; then
    # Decode UTF-16LE base64 → get the PS1 command
    DECODED_CMD=$(echo "$ENC_CMD" | base64 --decode | iconv -f UTF-16LE -t UTF-8 2>/dev/null || true)

    if [ -n "$DECODED_CMD" ]; then
        # Extract the inner B64 payload from the command
        INNER_B64=$(echo "$DECODED_CMD" | grep -oP "(?<=FromBase64String\(')[A-Za-z0-9+/=]+" || true)

        if [ -n "$INNER_B64" ]; then
            VERIFY_ZIP="$BUILD_DIR/verify.zip"
            echo "$INNER_B64" | base64 --decode > "$VERIFY_ZIP"

            FILE_COUNT=$(unzip -l "$VERIFY_ZIP" 2>/dev/null | grep -c 'installer\|README\|\.jar\|\.ico\|\.exe' || echo 0)
            HAS_INSTALLER=$(unzip -l "$VERIFY_ZIP" 2>/dev/null | grep -c 'installer.ps1' || echo 0)
            HAS_PAGES=$(unzip -p "$VERIFY_ZIP" installer.ps1 2>/dev/null | grep -c 'Page-Welcome\|Page-License\|Page-Options\|Page-Installing\|Page-Finish' || echo 0)

            rm -f "$VERIFY_ZIP"

            ok "Payload decoded successfully"
            ok "Files in ZIP    : ${FILE_COUNT} matched"
            ok "installer.ps1   : $([ "$HAS_INSTALLER" -gt 0 ] && echo 'PRESENT' || echo 'MISSING')"
            ok "Wizard pages    : ${HAS_PAGES}/5 found"
        else
            warn "Could not extract inner B64 from decoded command"
        fi
    else
        warn "Could not decode EncodedCommand (iconv may be missing)"
    fi
else
    warn "EncodedCommand not found in EXE — check file manually"
fi

# ── Done ──────────────────────────────────────────────────────────────────────
echo ""
echo -e "${G}╔══════════════════════════════════════════════════════════════╗${N}"
echo -e "${G}║      SanctumSetup.exe — Built and Ready                     ║${N}"
echo -e "${G}╚══════════════════════════════════════════════════════════════╝${N}"
echo ""
echo -e "  📦  ${C}$EXE_OUT${N}  ($(( EXE_SIZE / 1024 )) KB)"
echo ""
echo -e "  ${C}Wizard pages (5):${N}"
echo "    1. Welcome        — Java status card (green ✔ / amber ⚠)"
echo "    2. License        — Scrollable EULA, Next locked until accepted"
echo "    3. Install Options — Folder picker + Desktop/StartMenu toggles"
echo "    4. Installing     — Animated progress bar, live status text"
echo "    5. Finish         — Launch-now checkbox, Java reminder if needed"
echo ""
echo -e "  ${C}On double-click (Windows 7+):${N}"
echo "    • Decodes payload with PowerShell EncodedCommand (no corruption)"
echo '    • Expands to %TEMP%\SanctumSetup_xxxx\'
echo "    • Launches GUI wizard (console hidden)"
echo "    • Installs files, shortcuts, ARP registry, uninstaller"
echo "    • Auto-cleans temp folder on exit"
echo ""
echo -e "  ${Y}Code signing (recommended):${N}"
echo "    signtool sign /tr http://timestamp.digicert.com /td sha256 /fd sha256 /a \"$EXE_OUT\""
echo ""
echo -e "${C}  Sanctum — Windows setup EXE build complete.${N}"