# Sanctum Mobile App - Background Notification System

## 📱 Overview

This document outlines the enhanced background notification system for the Sanctum mobile app, ensuring reliable notification delivery even when the app is closed or the device is rebooted.

## 🔧 System Components

### 1. **NotificationService** (FirebaseMessagingService)
- Handles incoming FCM messages
- Routes notifications to appropriate handlers
- Manages notification channels
- Works when app is in foreground/background

### 2. **NotificationWorker** (CoroutineWorker)
- Processes notifications in background
- Shows actual notifications with proper styling
- Handles user relevance filtering
- Supports different notification types and priorities

### 3. **BootReceiver** (BroadcastReceiver)
- Restarts notification services after device reboot
- Re-initializes Firebase messaging
- Schedules periodic sync tasks
- Requests battery optimization whitelist

### 4. **NotificationSyncWorker** (PeriodicWorkRequest)
- Syncs missed notifications every 6 hours
- Refreshes FCM tokens
- Handles offline notification scenarios
- Requires network connectivity

### 5. **Application Class** (MemberApp)
- Initializes Firebase on app startup
- Manages FCM token lifecycle
- Sends tokens to backend server

## 🚀 Features Implemented

### ✅ **Background Notification Handling**
- Notifications work when app is closed
- Proper notification channels for different types
- Sound, vibration, and badge support
- User relevance filtering

### ✅ **Device Reboot Support**
- Automatic service restart after boot
- Package update detection
- Firebase re-initialization

### ✅ **Periodic Synchronization**
- Every 6 hours sync for missed notifications
- Network-aware execution
- Battery-conscious constraints

### ✅ **Battery Optimization**
- Whitelist request preparation
- Power management integration
- User-friendly permission handling

### ✅ **Permission Management**
- POST_NOTIFICATIONS permission (Android 13+)
- WAKE_LOCK for background work
- RECEIVE_BOOT_COMPLETED for boot receiver
- FOREGROUND_SERVICE for background tasks

## 📋 Notification Types

| Type | Priority | Sound | Vibration | Channel |
|------|----------|-------|-----------|---------|
| devotional_new | HIGH | ✓ | ✓ | devotionals |
| devotional_shared | HIGH | ✓ | ✓ | devotionals |
| announcement_posted | HIGH | ✓ | ✓ | announcements |
| church_event | HIGH | ✓ | ✓ | events |
| giving_reminder | DEFAULT | ✓ | - | giving |
| prayer_request | DEFAULT | - | - | general |
| general | DEFAULT | - | - | general |

## 🧪 Testing Scenarios

### **Test 1: App Closed Notifications**
1. Close the app completely
2. Send a test notification from Firebase console
3. Verify notification appears with proper styling
4. Tap notification to open app

### **Test 2: Device Reboot**
1. Send app to background
2. Reboot device
3. Send test notification
4. Verify notification appears after reboot

### **Test 3: App Update**
1. Install app update
2. Send test notification
3. Verify notification handling works

### **Test 4: Battery Optimization**
1. Enable battery optimization for app
2. Send test notification
3. Check if notification appears
4. Test whitelist request functionality

### **Test 5: Network Scenarios**
1. Turn off network
2. Send multiple notifications
3. Turn on network
4. Verify periodic sync recovers missed notifications

## 🔍 Debugging

### **Log Tags to Monitor**
- `NotificationService` - FCM message handling
- `NotificationWorker` - Background processing
- `BootReceiver` - Device reboot handling
- `NotificationSyncWorker` - Periodic sync
- `MemberApp` - Firebase initialization
- `MainActivity` - Permission and setup

### **Common Issues & Solutions**

**Issue: Notifications not appearing when app is closed**
- Check if WorkManager is properly initialized
- Verify battery optimization settings
- Check notification permissions

**Issue: Notifications stop after device reboot**
- Verify BootReceiver is registered in manifest
- Check RECEIVE_BOOT_COMPLETED permission
- Monitor BootReceiver logs

**Issue: Missing notification channels**
- Check createNotificationChannel implementation
- Verify Android O+ channel creation
- Monitor channel creation logs

## 📊 Performance Considerations

### **Battery Usage**
- Periodic sync every 6 hours (configurable)
- Network-aware constraints
- Battery-not-low requirements

### **Network Usage**
- Minimal API calls for sync
- Efficient token refresh
- Background data usage optimization

### **Memory Usage**
- Lightweight WorkManager tasks
- Proper cleanup in workers
- Efficient notification handling

## 🔧 Configuration

### **Sync Interval**
```kotlin
// In MainActivity.kt - schedulePeriodicNotificationSync()
6, java.util.concurrent.TimeUnit.HOURS // Can be adjusted
```

### **Notification Priorities**
```kotlin
// In NotificationWorker.kt - showBackgroundNotification()
when (type) {
    "devotional_new", "announcement_posted", "church_event" -> {
        notificationBuilder.setPriority(NotificationCompat.PRIORITY_HIGH)
        notificationBuilder.setDefaults(NotificationCompat.DEFAULT_ALL)
    }
    // ... other types
}
```

### **Work Constraints**
```kotlin
// In MainActivity.kt - schedulePeriodicNotificationSync()
val constraints = Constraints.Builder()
    .setRequiredNetworkType(NetworkType.CONNECTED)
    .setRequiresBatteryNotLow(true)
    .build()
```

## 🚀 Deployment Checklist

- [ ] Verify all permissions in AndroidManifest.xml
- [ ] Test notification channels on Android O+
- [ ] Validate battery optimization handling
- [ ] Test device reboot scenarios
- [ ] Verify periodic sync functionality
- [ ] Check Firebase configuration
- [ ] Test with different Android versions
- [ ] Verify notification styling and behavior

## 📞 Support

For issues with the notification system:
1. Check logcat with relevant tags
2. Verify Firebase project configuration
3. Test with different notification types
4. Check device notification settings
5. Verify network connectivity

---

**Last Updated**: May 4, 2026  
**Version**: 1.0  
**Compatibility**: Android API 21+
