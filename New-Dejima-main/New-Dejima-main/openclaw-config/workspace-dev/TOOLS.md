# TOOLS.md - Android Development Environment

## Android SDK
- ANDROID_HOME=/Users/administrator/Library/Android/sdk
- JAVA_HOME=/Users/administrator/Library/Java/JavaVirtualMachines/corretto-17.0.17/Contents/Home
- PATH includes: $JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$ANDROID_HOME/cmdline-tools/latest/bin
- Java 17 (Corretto 17.0.17)
- ADB, emulator, avdmanager available

## CRITICAL: JAVA_HOME
The system default JDK is v25 which is INCOMPATIBLE with Kotlin 2.0.21.
You MUST run this before any gradle command:
```bash
export JAVA_HOME=/Users/administrator/Library/Java/JavaVirtualMachines/corretto-17.0.17/Contents/Home
```
Also set org.gradle.java.home in gradle.properties (see SKILL.md template).

## Connected Device
- Samsung Galaxy A16 (SM-A165F) connected via USB
- Serial: RFGYC34581H
- Android 15 (API 35), 1080x2340 @ 450dpi
- Use: adb -s RFGYC34581H <command>

## CRITICAL: SDK Path Conflict Fix
There is a conflicting ANDROID_SDK_ROOT env var. You MUST:
1. Always create local.properties in the project root with: sdk.dir=/Users/administrator/Library/Android/sdk
2. Before running gradlew, run: unset ANDROID_SDK_ROOT
3. Copy gradlew + gradle/wrapper/ from /tmp/dejima-workspace/tip-calculator/ (already built, has cached deps)

## Workspace
- Build apps in: /tmp/dejima-workspace/
- Gradle wrapper template: /tmp/dejima-workspace/gradle-template/
- Copy gradlew and gradle/wrapper/ from the template into new projects

## Exec Approvals
All Android build commands are pre-approved: gradlew, gradle, adb, emulator, avdmanager, ls, mkdir, chmod, cat, curl, cp, mv, rm, find, grep, sleep, java, sh, bash.
You can execute these without asking for permission.

## Pinned Android Versions (MANDATORY)
- compileSdk = 35, minSdk = 31, targetSdk = 35
- Kotlin = 2.0.21, Compose BOM = 2024.12.01
- Android Gradle Plugin = 8.7.3, Gradle wrapper = 8.11.1
- JVM target = 17
- ALWAYS Jetpack Compose, Material 3, Kotlin only (NEVER Java, NEVER XML layouts)
