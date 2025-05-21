# Meshtastic Android Project Structure

This document provides an overview of the Meshtastic Android project structure and its key components.

## Project Overview
Meshtastic Android is an Android application that implements the Meshtastic protocol, allowing for mesh networking capabilities on Android devices. The application enables users to communicate over long distances using mesh networking technology, with features like text messaging, location sharing, and node management.

## Application Architecture

### Core Components
- `MainActivity.kt` - Main entry point of the application
- `ApplicationModule.kt` - Dependency injection setup
- `NodeInfo.kt` - Node information management
- `DataPacket.kt` - Data packet handling
- `MyNodeInfo.kt` - Local node information management

### Key Features
1. Mesh Networking
   - Node discovery and management
   - Message routing
   - Network topology visualization
   - Channel management

2. Communication
   - Text messaging
   - Location sharing
   - Contact management
   - Channel-based communication

3. Device Management
   - Radio configuration
   - Battery monitoring
   - Debug information
   - Settings management

## User Interface

### Main UI Components
1. Navigation
   - Bottom navigation bar
   - Fragment-based navigation
   - Screen transitions

2. Key Screens
   - `ChannelFragment.kt` - Channel management and messaging
   - `ContactsFragment.kt` - Contact list and management
   - `SettingsFragment.kt` - Application settings
   - `DebugFragment.kt` - Debug information and tools
   - `NodeDetail.kt` - Detailed node information
   - `UsersFragment.kt` - User management
   - `ShareFragment.kt` - Content sharing interface

3. UI Components
   - `NodeItem.kt` - Node list item display
   - `ContactItem.kt` - Contact list item display
   - `BatteryInfo.kt` - Battery status display
   - `LastHeardInfo.kt` - Last communication information
   - `LinkedCoordinates.kt` - Location information display

### UI Resources
1. Layouts (`res/layout/`)
   - Screen layouts
   - Custom views
   - Dialog layouts

2. Drawables (`res/drawable/`)
   - Icons
   - Backgrounds
   - Vector graphics

3. Values (`res/values/`)
   - Colors
   - Styles
   - Themes
   - Strings (with multi-language support)

4. Internationalization
   - Supports multiple languages
   - RTL layout support
   - Localized resources

## Directory Structure

### Root Level
- `app/` - Main application module
- `build/` - Build output directory
- `config/` - Configuration files
- `design/` - Design assets and resources
- `gradle/` - Gradle wrapper files
- `.github/` - GitHub specific files (workflows, templates)
- `.gradle/` - Gradle cache and build files
- `.idea/` - IntelliJ IDEA/Android Studio project files
- `.kotlin/` - Kotlin specific files

### Key Files
- `build.gradle` - Root level build configuration
- `settings.gradle` - Project settings and module definitions
- `gradle.properties` - Gradle configuration properties
- `local.properties` - Local development environment settings
- `gradlew` & `gradlew.bat` - Gradle wrapper scripts
- `NOTES.md` - Development notes and commands
- `debugging-android.md` - Debugging documentation
- `README.md` - Project overview and setup instructions
- `LICENSE` - Project license information

### App Module (`app/`)
- `src/` - Source code directory
  - `main/` - Main application source code
    - `java/com/geeksville/mesh/` - Core application code
      - `ui/` - User interface components
      - `service/` - Background services
      - `repository/` - Data repositories
      - `model/` - Data models
      - `database/` - Local database
      - `util/` - Utility functions
    - `res/` - Resources
      - `layout/` - UI layouts
      - `drawable/` - Graphics and images
      - `values/` - Strings, colors, styles
      - `menu/` - Menu definitions
    - `assets/` - Static assets
    - `proto/` - Protocol buffer definitions
  - `test/` - Unit tests
  - `androidTest/` - Android instrumentation tests
  - `fdroid/` - F-Droid specific configurations
  - `google/` - Google Play Store specific configurations
- `build.gradle` - App module build configuration
- `proguard-rules.pro` - ProGuard rules for code optimization
- `google-services.json` - Google Services configuration
- `schemas/` - Database schemas and migrations

## Build Variants
The project supports multiple build variants:
- Debug builds for development
- Release builds for production
- F-Droid specific builds
- Google Play Store specific builds

## Development Setup
1. Set up Android SDK environment variables
2. Initialize git submodules
3. Clean and build the project using Gradle
4. Generate Protocol Buffers
5. Build and install the APK on a connected device

## Build Commands
- `./gradlew assembleDebug` - Build debug APK
- `./gradlew assembleFdroidDebug` - Build F-Droid debug APK
- `./gradlew assembleFdroidRelease` - Build F-Droid release APK
- `./gradlew generateProto` - Generate Protocol Buffers

## Installation
Debug APKs can be installed using:
- `adb install app/build/outputs/apk/fdroid/debug/app-fdroid-debug.apk`
- `adb install app/build/outputs/apk/google/debug/app-google-debug.apk`

## Environment Variables
Required environment variables for development:
- `ANDROID_HOME`
- `ANDROID_SDK_ROOT`
- `ANDROID_AVD_HOME`
- `ANDROID_NDK_HOME`
- `ANDROID_NDK_ROOT`
- `ANDROID_NDK_PATH`
- `ANDROID_NDK_PLATFORM`

## Satochip Integration

### Overview
The project includes Satochip hardware wallet integration through two main components:
- `satochip-lib/` - Core Satochip library
- `satochip-android/` - Android-specific Satochip implementation

### Integration Steps

1. Add Dependencies
   In your app's `build.gradle`, add:
   ```gradle
   dependencies {
       implementation project(':satochip-lib')
       implementation project(':satochip-android')
   }
   ```

2. Update Settings
   In `settings.gradle`, add:
   ```gradle
   include ':satochip-lib'
   include ':satochip-android'
   ```

3. Implementation
   - Use Satochip for secure key storage
   - Implement hardware wallet authentication
   - Add secure transaction signing
   - Enable hardware wallet backup features

### Key Features
1. Hardware Security
   - Secure key storage
   - Hardware-based authentication
   - Transaction signing
   - Backup and recovery

2. Integration Points
   - User authentication
   - Secure messaging
   - Key management
   - Backup operations

### Usage Guidelines
1. Initialize Satochip
   ```kotlin
   // Initialize Satochip manager
   val satochipManager = SatochipManager(context)
   ```

2. Authentication
   ```kotlin
   // Authenticate user
   satochipManager.authenticate()
   ```

3. Key Management
   ```kotlin
   // Generate or import keys
   satochipManager.generateKey()
   ```

4. Backup Operations
   ```kotlin
   // Backup wallet
   satochipManager.backupWallet()
   ```

### Security Considerations
1. Key Storage
   - Use Satochip for all sensitive key storage
   - Never store private keys in app storage
   - Implement proper backup procedures

2. Authentication
   - Require hardware authentication for sensitive operations
   - Implement proper session management
   - Handle authentication failures gracefully

3. Backup
   - Implement secure backup procedures
   - Provide clear backup instructions to users
   - Handle backup verification

### Error Handling
1. Hardware Issues
   - Handle device not found
   - Manage connection errors
   - Implement retry mechanisms

2. Authentication Failures
   - Handle incorrect PIN
   - Manage timeout scenarios
   - Provide clear error messages

3. Backup Issues
   - Handle backup failures
   - Implement recovery procedures
   - Provide user guidance 