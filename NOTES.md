# Setup Environment
export ANDROID_HOME=/Users/i830671/Library/Android/sdk

# Initialize submodules
git submodule update --init --recursive

# Clean prior to build
rm -rf ~/.gradle/caches/8.14
./gradlew clean --no-daemon

# Generate Protocol Buffers
./gradlew generateProto

# Build with gradle
./gradlew assembleDebug --no-daemon
./gradlew assembleFdroidDebug // What Studio is doing
./gradlew assembleFdroidRelease

# Check for connected device
adb devices -l

# Install APK on connected device
adb install app/build/outputs/apk/fdroid/debug/app-fdroid-debug.apk // What Studio is doing
adb install app/build/outputs/apk/google/debug/app-google-debug.apk

# Uninstall APK from device
adb uninstall com.geeksville.mesh

# Display all gradle tasks
./gradlew tasks

# Other commands
export ANDROID_HOME=/Users/i830671/Library/Android/sdk
export ANDROID_SDK_ROOT=/Users/i830671/Library/Android/sdk
export ANDROID_AVD_HOME=/Users/i830671/Library/Android/avd
export ANDROID_NDK_HOME=/Users/i830671/Library/Android/sdk/ndk-bundle
export ANDROID_NDK_ROOT=/Users/i830671/Library/Android/sdk/ndk-bundle
export ANDROID_NDK_PATH=/Users/i830671/Library/Android/sdk/ndk-bundle
export ANDROID_NDK_PLATFORM=android-21

