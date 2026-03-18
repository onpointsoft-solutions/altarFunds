; Sanctum Church Management System - Inno Setup Installer
; Professional Windows installer with full Windows integration

#define MyAppName "Sanctum Church Management System"
#define MyAppShort "Sanctum"
#define MyAppVersion "1.0.0"
#define MyAppPublisher "Sanctum Church Software"
#define MyAppURL "https://www.sanctumchurch.com"
#define MyAppSupportURL "https://support.sanctumchurch.com"
#define MyAppExeName "Sanctum.exe"
#define MyAppAssocName "Sanctum Church Management"

[Setup]
; App identification
AppId={{B3F1A2C4-9D7E-4F8A-B6E5-D2C1A0F3E8B7}
AppName={#MyAppName}
AppVersion={#MyAppVersion}
AppVerName={#MyAppName} {#MyAppVersion}
AppPublisher={#MyAppPublisher}
AppPublisherURL={#MyAppURL}
AppSupportURL={#MyAppSupportURL}
AppUpdatesURL={#MyAppURL}/updates
AppCopyright=Copyright © 2026 {#MyAppPublisher}

; Installation paths
DefaultDirName={autopf}\{#MyAppShort}
DefaultGroupName={#MyAppShort}
AllowNoIcons=no
DisableProgramGroupPage=yes

; Permissions and compatibility
PrivilegesRequired=admin
PrivilegesRequiredOverridesAllowed=dialog
MinVersion=6.1sp1  ; Windows 7 SP1 minimum
ArchitecturesAllowed=x86 x64 arm64
ArchitecturesInstallIn64BitMode=x64 arm64

; Output configuration
OutputDir=..\
OutputBaseFilename=SanctumSetup-{#MyAppVersion}
SetupIconFile=..\src\main\resources\images\icon.ico
Compression=lzma2/ultra64
SolidCompression=yes
LZMAUseSeparateProcess=yes
InternalCompressLevel=ultra64

; Wizard appearance
WizardStyle=modern
WizardResizable=no
DisableWelcomePage=no
WizardImageFile=wizard.bmp
WizardSmallImageFile=wizard-small.bmp

; Uninstaller
UninstallDisplayIcon={app}\{#MyAppExeName}
UninstallDisplayName={#MyAppName}
CreateUninstallRegKey=yes
CloseApplications=yes
CloseApplicationsFilter=*.exe

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Tasks]
Name: "desktopicon"; Description: "Create a &desktop shortcut"; GroupDescription: "Shortcuts:"; Flags: checkedonce
Name: "quicklaunchicon"; Description: "Create a &Quick Launch shortcut"; GroupDescription: "Shortcuts:"; OnlyBelowVersion: 6.1; Flags: unchecked
Name: "startmenuicon"; Description: "Create a &Start Menu shortcut"; GroupDescription: "Shortcuts:"; Flags: checkedonce

[Dirs]
Name: "{app}"; Permissions: users-modify
Name: "{app}\data"; Permissions: users-modify
Name: "{app}\logs"; Permissions: users-modify
Name: "{localappdata}\{#MyAppShort}"; Permissions: users-modify

[Files]
; Main application executable
Source: "..\target\Sanctum.exe"; DestDir: "{app}"; Flags: ignoreversion
; Required JAR file (embedded but needed for .exe to work)
Source: "..\target\sanctum-church-management-{#MyAppVersion}.jar"; DestDir: "{app}"; Flags: ignoreversion
; Documentation
Source: "README.txt"; DestDir: "{app}"; Flags: ignoreversion isreadme
Source: "LICENSE.txt"; DestDir: "{app}"; Flags: ignoreversion
; Additional resources (if any)
Source: "resources\*"; DestDir: "{app}\resources"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
; Start Menu
Name: "{group}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"; WorkingDir: "{app}"; Tasks: startmenuicon
Name: "{group}\Uninstall {#MyAppShort}"; Filename: "{uninstallexe}"
; Desktop
Name: "{autodesktop}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"; WorkingDir: "{app}"; Tasks: desktopicon
; Quick Launch (Windows 7 and below)
Name: "{userappdata}\Microsoft\Internet Explorer\Quick Launch\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"; WorkingDir: "{app}"; Tasks: quicklaunchicon

[Registry]
; Application registration
Root: HKA; Subkey: "Software\{#MyAppPublisher}\{#MyAppShort}"; ValueType: string; ValueName: "InstallPath"; ValueData: "{app}"; Flags: uninsdeletekey
Root: HKA; Subkey: "Software\{#MyAppPublisher}\{#MyAppShort}"; ValueType: string; ValueName: "Version"; ValueData: "{#MyAppVersion}"
; Add/Remove Programs enhancement
Root: HKA; Subkey: "Software\Microsoft\Windows\CurrentVersion\Uninstall\{#MyAppSetup}_is1"; ValueType: string; ValueName: "Publisher"; ValueData: "{#MyAppPublisher}"
Root: HKA; Subkey: "Software\Microsoft\Windows\CurrentVersion\Uninstall\{#MyAppSetup}_is1"; ValueType: string; ValueName: "DisplayVersion"; ValueData: "{#MyAppVersion}"
Root: HKA; Subkey: "Software\Microsoft\Windows\CurrentVersion\Uninstall\{#MyAppSetup}_is1"; ValueType: string; ValueName: "URLInfoAbout"; ValueData: "{#MyAppURL}"
Root: HKA; Subkey: "Software\Microsoft\Windows\CurrentVersion\Uninstall\{#MyAppSetup}_is1"; ValueType: string; ValueName: "HelpLink"; ValueData: "{#MyAppSupportURL}"
Root: HKA; Subkey: "Software\Microsoft\Windows\CurrentVersion\Uninstall\{#MyAppSetup}_is1"; ValueType: dword; ValueName: "EstimatedSize"; ValueData: "102400"

[Run]
; Launch application after install
Filename: "{app}\{#MyAppExeName}"; Description: "Launch {#MyAppShort} now"; Flags: nowait postinstall skipifsilent

[UninstallDelete]
; Clean up user data on uninstall (optional - comment out to preserve data)
Type: filesandordirs; Name: "{app}\logs"

[Code]
// Pascal script for additional functionality

// Windows version check
function InitializeSetup(): Boolean;
var
  Version: TWindowsVersion;
begin
  GetWindowsVersionEx(Version);
  if Version.Major < 6 then
  begin
    MsgBox('This application requires Windows 7 SP1 or later.', mbError, MB_OK);
    Result := False;
    Exit;
  end;
  if (Version.Major = 6) and (Version.Minor < 1) then
  begin
    MsgBox('This application requires Windows 7 SP1 or later.', mbError, MB_OK);
    Result := False;
    Exit;
  end;
  Result := True;
end;

// Java detection
function IsJavaInstalled(): Boolean;
var
  ResultCode: Integer;
begin
  Result := False;
  if Exec('java', '-version', '', SW_HIDE, ewWaitUntilTerminated, ResultCode) then
  begin
    Result := (ResultCode = 0);
  end;
end;

// Java installation prompt
function NextButtonClick(CurPageID: Integer): Boolean;
begin
  Result := True;
  if (CurPageID = wpReady) and not IsJavaInstalled() then
  begin
    if MsgBox('Java Runtime Environment 11 or newer is required but was not detected.' + #13#10 + #13#10 +
              'Click Yes to download Java, or No to continue anyway (installation may fail).', 
              mbConfirmation, MB_YESNO) = IDYES then
    begin
      Result := ShellExec('', 'https://adoptium.net/temurin/releases/?version=21', '', '', SW_SHOWNORMAL, ewNoWait, ResultCode);
    end;
  end;
end;
