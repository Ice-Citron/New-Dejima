# TOOLS.md - Android Development Environment

## Android SDK (Windows)
- ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk
- PATH includes: %ANDROID_HOME%\platform-tools;%ANDROID_HOME%\emulator;%ANDROID_HOME%\cmdline-tools\latest\bin
- ADB, emulator, avdmanager available

## Android SDK (macOS)
- ANDROID_HOME=/Users/administrator/Library/Android/sdk
- JAVA_HOME=/Users/administrator/Library/Java/JavaVirtualMachines/corretto-17.0.17/Contents/Home
- PATH includes: $JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$ANDROID_HOME/cmdline-tools/latest/bin
- Java 17 (Corretto 17.0.17)

## CRITICAL: JAVA_HOME (macOS only)
The system default JDK is v25 which is INCOMPATIBLE with Kotlin 2.0.21.
You MUST run this before any gradle command on macOS:
```bash
export JAVA_HOME=/Users/administrator/Library/Java/JavaVirtualMachines/corretto-17.0.17/Contents/Home
```

## Connected Device
- Samsung Galaxy A16 (SM-A165F) connected via USB
- Serial: RFGYC34581H
- Android 15 (API 35), 1080x2340 @ 450dpi
- Use: adb -s RFGYC34581H <command>

## Workspace
- Windows: Build apps in %TEMP%\dejima-workspace\
- macOS: Build apps in /tmp/dejima-workspace/

## Exec Approvals
All Android build commands are pre-approved: gradlew, gradle, adb, emulator, avdmanager, ls, mkdir, chmod, cat, curl, cp, mv, rm, find, grep, sleep, java, sh, bash, dir, copy, timeout.
You can execute these without asking for permission.

## Pinned Android Versions (MANDATORY)
- compileSdk = 35, minSdk = 31, targetSdk = 35
- Kotlin = 2.0.21, Compose BOM = 2024.12.01
- Android Gradle Plugin = 8.7.3, Gradle wrapper = 8.11.1
- JVM target = 17
- ALWAYS Jetpack Compose, Material 3, Kotlin only (NEVER Java, NEVER XML layouts)
