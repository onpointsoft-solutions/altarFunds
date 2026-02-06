# AltarFunds Member App

A modern Android mobile application for church members to manage their giving, view announcements, read devotionals, and manage their profile.

## Features

### 🔐 Authentication
- Login with email and password
- User registration
- Password recovery
- Secure token-based authentication

### ⛪ Church Management
- Search and browse churches
- View church details
- Join a church

### 💰 Giving/Donations
- Make donations (Tithes, Offerings, Special Offerings)
- M-Pesa integration for mobile payments
- View donation history
- Track giving statistics

### 📢 Announcements
- View church announcements
- Priority-based notifications
- Filter by target audience

### 📖 Devotionals
- Daily devotionals
- Scripture references
- Browse devotional history

### 👤 Profile Management
- View and edit profile
- Change password
- Update contact information
- View church membership details

## Tech Stack

- **Language**: Kotlin
- **UI**: XML Layouts with Material Design 3
- **Networking**: Retrofit 2 + OkHttp
- **Architecture**: MVVM (Model-View-ViewModel)
- **Async**: Kotlin Coroutines
- **Data Storage**: DataStore (for preferences)
- **Image Loading**: Glide
- **Navigation**: Android Navigation Component

## Project Structure

```
member-app/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/altarfunds/member/
│   │   │   │   ├── api/              # Retrofit API services
│   │   │   │   ├── models/           # Data models
│   │   │   │   ├── ui/               # Activities and Fragments
│   │   │   │   │   ├── auth/         # Login, Register screens
│   │   │   │   │   ├── church/       # Church search and join
│   │   │   │   │   ├── giving/       # Donation screens
│   │   │   │   │   ├── announcements/# Announcements list
│   │   │   │   │   ├── devotionals/  # Devotionals list
│   │   │   │   │   └── profile/      # Profile and settings
│   │   │   │   ├── adapters/         # RecyclerView adapters
│   │   │   │   ├── utils/            # Utility classes
│   │   │   │   └── MemberApp.kt      # Application class
│   │   │   └── res/
│   │   │       ├── layout/           # XML layouts
│   │   │       ├── drawable/         # Images and icons
│   │   │       ├── values/           # Strings, colors, themes
│   │   │       └── navigation/       # Navigation graphs
│   └── build.gradle
├── build.gradle
└── settings.gradle
```

## API Integration

The app connects to the AltarFunds Django backend API:
- Base URL: `http://altarfunds.pythonanywhere.com/api/`
- Authentication: JWT Bearer tokens
- All API calls use Retrofit with Kotlin Coroutines

## Setup Instructions

1. **Clone the repository**
   ```bash
   cd member-app
   ```

2. **Open in Android Studio**
   - Open Android Studio
   - Select "Open an Existing Project"
   - Navigate to the `member-app` folder

3. **Sync Gradle**
   - Android Studio will automatically sync Gradle dependencies
   - Wait for the sync to complete

4. **Run the app**
   - Connect an Android device or start an emulator
   - Click the "Run" button in Android Studio
   - Select your device/emulator

## Requirements

- Android Studio Hedgehog or later
- Android SDK 24 or higher (Android 7.0+)
- Kotlin 1.9.20
- Gradle 8.2.0

## Dependencies

Key dependencies include:
- Retrofit 2.9.0 - REST API client
- Material Design 3 - Modern UI components
- Kotlin Coroutines - Asynchronous programming
- DataStore - Preferences storage
- Glide - Image loading
- Navigation Component - Screen navigation

## Building for Production

To build a release APK:

```bash
./gradlew assembleRelease
```

The APK will be generated in `app/build/outputs/apk/release/`

## Contributing

This is a church management system. For contributions or issues, please contact the development team.

## License

Copyright © 2026 AltarFunds. All rights reserved.
