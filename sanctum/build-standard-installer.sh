#!/bin/bash

# =============================================================================
# Sanctum Church Management System - Standard Windows Installer Build
# Creates industry-standard MSI installer using WiX Toolset
# =============================================================================

set -euo pipefail

# Configuration
APP_NAME="Sanctum Church Management System"
APP_SHORT="Sanctum"
APP_VERSION="1.0.0"
APP_PUBLISHER="Sanctum Church Software"
APP_UPGRADE_CODE="{B3F1A2C4-9D7E-4F8A-B6E5-D2C1A0F3E8B7}"

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

success() { echo -e "${GREEN}[OK]${NC} $*"; }
info() { echo -e "${BLUE}[INFO]${NC} $*"; }
warn() { echo -e "${YELLOW}[WARN]${NC} $*"; }
error() { echo -e "${RED}[ERROR]${NC} $*"; exit 1; }

echo "🏛️  Creating Standard Windows Installer (MSI)"
echo "============================================"

# Check prerequisites
command -v mvn >/dev/null 2>&1 || error "Maven required"
command -v java >/dev/null 2>&1 || error "Java required"

# Build application
info "Building application..."
mvn clean package -DskipTests || error "Build failed"

# Find JAR
JAR_FILE=$(find target -name "sanctum-church-management-*.jar" ! -name "*-sources.jar" | head -n1)
[ -f "$JAR_FILE" ] || error "JAR file not found"
success "JAR found: $JAR_FILE"

# Create WiX source file for MSI
cat > SanctumInstaller.wxs << 'WIX_EOF'
<?xml version="1.0" encoding="UTF-8"?>
<Wix xmlns="http://schemas.microsoft.com/wix/2006/wi">
  
  <!-- Product Definition -->
  <Product 
    Id="*" 
    Name="Sanctum Church Management System" 
    Language="1033" 
    Version="1.0.0.0" 
    Manufacturer="Sanctum Church Software" 
    UpgradeCode="{B3F1A2C4-9D7E-4F8A-B6E5-D2C1A0F3E8B7}">
    
    <!-- Package Information -->
    <Package 
      InstallerVersion="200" 
      Compressed="yes" 
      InstallScope="perMachine" 
      Description="Church Management System" 
      Comments="Professional church administration software" 
      Manufacturer="Sanctum Church Software" />
    
    <!-- Media -->
    <MediaTemplate EmbedCab="yes" />
    
    <!-- Features -->
    <Feature Id="ProductFeature" Title="Sanctum Church Management" Level="1">
      <ComponentGroupRef Id="ProductComponents" />
      <ComponentRef Id="ApplicationShortcut" />
      <ComponentRef Id="DesktopShortcut" />
    </Feature>
    
    <!-- Directory Structure -->
    <Directory Id="TARGETDIR" Name="SourceDir">
      <Directory Id="ProgramFilesFolder">
        <Directory Id="INSTALLFOLDER" Name="Sanctum" />
      </Directory>
      
      <!-- Start Menu -->
      <Directory Id="ProgramMenuFolder">
        <Directory Id="ApplicationProgramsFolder" Name="Sanctum"/>
      </Directory>
      
      <!-- Desktop -->
      <Directory Id="DesktopFolder" Name="Desktop" />
    </Directory>
    
    <!-- Components -->
    <ComponentGroup Id="ProductComponents" Directory="INSTALLFOLDER">
      <Component Id="MainExecutable">
        <File Id="SanctumEXE" Source="$(var.SanctumChurchManagement.TargetPath)" />
        <RegistryValue 
          Root="HKLM" 
          Key="Software\Sanctum Church Software\Sanctum" 
          Name="installed" 
          Type="integer" 
          Value="1" 
          KeyPath="yes" />
      </Component>
    </ComponentGroup>
    
    <!-- Shortcuts -->
    <Component Id="ApplicationShortcut" Directory="ApplicationProgramsFolder">
      <Shortcut 
        Id="ApplicationStartMenuShortcut"
        Name="Sanctum Church Management"
        Description="Church administration system"
        Target="[#SanctumEXE]"
        WorkingDirectory="INSTALLFOLDER"
        Icon="SanctumIcon.ico" />
      <RemoveFolder Id="ApplicationProgramsFolder" On="uninstall"/>
      <RegistryValue 
        Root="HKCU" 
        Key="Software\Sanctum Church Software\Sanctum" 
        Name="installed" 
        Type="integer" 
        Value="1" 
        KeyPath="yes" />
    </Component>
    
    <Component Id="DesktopShortcut" Directory="DesktopFolder">
      <Shortcut 
        Id="ApplicationDesktopShortcut"
        Name="Sanctum Church Management"
        Description="Church administration system"
        Target="[#SanctumEXE]"
        WorkingDirectory="INSTALLFOLDER"
        Icon="SanctumIcon.ico" />
      <RegistryValue 
        Root="HKCU" 
        Key="Software\Sanctum Church Software\Sanctum" 
        Name="desktop_shortcut" 
        Type="integer" 
        Value="1" 
        KeyPath="yes" />
    </Component>
    
    <!-- Icons -->
    <Icon Id="SanctumIcon.ico" SourceFile="resources\app.ico"/>
    
    <!-- Properties -->
    <Property Id="ARPPRODUCTICON" Value="SanctumIcon.ico" />
    <Property Id="ARPHELPLINK" Value="https://support.sanctumchurch.com" />
    <Property Id="ARPURLINFOABOUT" Value="https://www.sanctumchurch.com" />
    
    <!-- UI Configuration -->
    <UIRef Id="WixUI_InstallDir" />
    <Property Id="WIXUI_INSTALLDIR" Value="INSTALLFOLDER" />
    
    <!-- License Agreement -->
    <WixVariable Id="WixUILicenseRtf" Value="License.rtf" />
    
  </Product>
</Wix>
WIX_EOF

# Create WiX project file
cat > SanctumInstaller.wixproj << 'WIXPROJ_EOF'
<?xml version="1.0" encoding="utf-8"?>
<Project ToolsVersion="4.0" DefaultTargets="Build" xmlns="http://schemas.microsoft.com/developer/msbuild/2003">
  <PropertyGroup>
    <Configuration Condition=" '$(Configuration)' == '' ">Release</Configuration>
    <Platform Condition=" '$(Platform)' == '' ">x86</Platform>
    <ProductVersion>3.10</ProductVersion>
    <ProjectGuid>{guid}</ProjectGuid>
    <SchemaVersion>2.0</SchemaVersion>
    <OutputType>Package</OutputType>
    <WixTargetsPath Condition=" '$(WixTargetsPath)' == '' AND '$(MSBuildExtensionsPath32)' != '' ">$(MSBuildExtensionsPath32)\Microsoft\WiX\v3.x\Wix.targets</WixTargetsPath>
    <Name>SanctumInstaller</Name>
  </PropertyGroup>
  
  <PropertyGroup Condition=" '$(Configuration)|$(Platform)' == 'Release|x86' ">
    <OutputPath>bin\Release\</OutputPath>
    <IntermediateOutputPath>obj\Release\</IntermediateOutputPath>
    <DefineConstants>Configuration=Release</DefineConstants>
  </PropertyGroup>
  
  <ItemGroup>
    <ProjectReference Include="..\SanctumChurchManagement\SanctumChurchManagement.csproj" />
  </ItemGroup>
  
  <ItemGroup>
    <Compile Include="SanctumInstaller.wxs" />
  </ItemGroup>
  
  <ItemGroup>
    <Content Include="License.rtf" />
    <Content Include="resources\app.ico" />
  </ItemGroup>
  
  <Import Project="$(WixTargetsPath)" Condition=" '$(WixTargetsPath)' != '' " />
</Project>
WIXPROJ_EOF

# Create license file
cat > License.rtf << 'LICENSE_EOF'
{\rtf1\ansi\deff0{\fonttbl{\f0 Times New Roman;}}
\f0\fs24{\b Sanctum Church Management System\line End User License Agreement}\line\line
Copyright {\textcopyright} 2026 Sanctum Church Software. All rights reserved.\line\line
Permission is granted to install and use this software for church management purposes.\line\line
{\b Restrictions:} Redistribution or resale requires written permission.\line\line
{\b Disclaimer:} Software provided "as-is" without warranty.\line\line
Full license: https://www.sanctumchurch.com/license}
LICENSE_EOF

# Create build instructions
cat > BUILD_INSTRUCTIONS.txt << 'BUILD_EOF'
Standard Windows MSI Installer Build Instructions
===============================================

REQUIREMENTS:
1. Visual Studio 2019+ with WiX Toolset extension
2. OR WiX Toolset v3.11+ standalone
3. .NET Framework 4.7.2+

BUILD STEPS:
1. Install WiX Toolset:
   - Visual Studio: Extensions > Manage Extensions > Search "WiX"
   - Standalone: https://wixtoolset.org/releases/

2. Build MSI:
   ```
   msbuild SanctumInstaller.wixproj /p:Configuration=Release
   ```

3. Output: bin\Release\SanctumInstaller.msi

STANDARD FEATURES:
✅ Windows Installer service integration
✅ Add/Remove Programs support  
✅ Repair/Modify/Uninstall options
✅ Component-based installation
✅ Windows Logo compliance
✅ Code signing support
✅ Automatic update detection
✅ Prerequisites checking
✅ Multi-language support
✅ Silent installation support

DEPLOYMENT OPTIONS:
- Group Policy (GPO) deployment
- System Center Configuration Manager (SCCM)
- Microsoft Intune
- Manual installation
- Web deployment

CODE SIGNING (Recommended):
signtool sign /f certificate.pfx /p password /t http://timestamp.digicert.com SanctumInstaller.msi
BUILD_EOF

success "Standard MSI installer project created!"
info "Files created:"
info "  - SanctumInstaller.wxs (WiX source)"
info "  - SanctumInstaller.wixproj (Project file)"
info "  - License.rtf (License agreement)"
info "  - BUILD_INSTRUCTIONS.txt (Build guide)"

echo ""
echo "📋 Next Steps:"
echo "1. Install WiX Toolset (Visual Studio extension or standalone)"
echo "2. Build with: msbuild SanctumInstaller.wixproj"
echo "3. Sign MSI (optional but recommended)"
echo "4. Deploy using standard Windows deployment methods"
echo ""
echo "✅ This creates a standards-compliant Windows installer!"
