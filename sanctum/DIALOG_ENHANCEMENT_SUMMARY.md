# Enhanced Dialog Management for Sanctum

## Overview
Created an enhanced dialog management system to prevent dialog minimizing and improve user experience across the Sanctum Church Management desktop application.

## Key Improvements

### 1. DialogManager Utility Class
- **Location**: `src/main/java/com/sanctum/util/DialogManager.java`
- **Purpose**: Centralized dialog creation with anti-minimizing features

### 2. Enhanced Dialog Features
- **Anti-Minimizing**: Window state listeners that restore dialogs from minimized state
- **Focus Management**: Automatic focus restoration and front positioning
- **Windows-Specific Fixes**: Enhanced behavior for Windows OS
- **Smooth Display**: Timed display to prevent blinking and focus issues

### 3. Updated Components
- **TreasurerDashboardFrame**: Updated donation dialog creation
- **PastorDashboardFrame**: Updated devotional dialogs
- **Import Changes**: Added DialogManager imports

## Technical Implementation

### Window State Management
```java
// Prevent minimizing by monitoring window state changes
@Override
public void windowStateChanged(WindowEvent e) {
    if (IS_WINDOWS && e.getNewState() == Frame.ICONIFIED) {
        SwingUtilities.invokeLater(() -> {
            dialog.setExtendedState(Frame.NORMAL);
            dialog.toFront();
            dialog.requestFocus();
        });
    }
}
```

### Focus Handling
```java
// Enhanced focus management on activation/deactivation
@Override
public void windowActivated(WindowEvent e) {
    dialog.toFront();
    dialog.requestFocus();
}
```

### Windows-Specific Enhancements
```java
// Apply Windows-specific properties
dialog.setFocusableWindowState(false);
dialog.setType(Window.Type.NORMAL);
dialog.setAlwaysOnTop(true); // Temporary during initialization
```

## Benefits
1. **Prevents Unwanted Minimizing**: Dialogs stay visible and accessible
2. **Improved User Experience**: No more accidental dialog hiding
3. **Cross-Platform Compatibility**: Works on Windows, Mac, and Linux
4. **Professional Behavior**: Consistent dialog management across the application

## Usage Example
```java
// Before (basic)
JDialog dialog = new JDialog(this, "Title", true);
dialog.setVisible(true);

// After (enhanced)
JDialog dialog = DialogManager.createModalDialog(this, "Title");
DialogManager.showDialogEnhanced(dialog);
```

## Files Modified
1. `DialogManager.java` - New utility class
2. `TreasurerDashboardFrame.java` - Updated donation dialog
3. `PastorDashboardFrame.java` - Updated devotional dialogs

## Testing Recommendations
1. Test dialog creation on different operating systems
2. Verify focus management when switching between applications
3. Test minimizing behavior to ensure prevention works
4. Validate that dialogs remain responsive during use

This enhancement significantly improves the professional behavior of the Sanctum desktop application by ensuring dialogs remain accessible and properly managed.
