# A1 Android environment setup — run before testing the agent pipeline
# Usage: .\scripts\setup-android-env.ps1

$ErrorActionPreference = "Stop"

# 1. Android SDK
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
$env:PATH = "$env:ANDROID_HOME\platform-tools;$env:ANDROID_HOME\emulator;$env:ANDROID_HOME\cmdline-tools\latest\bin;$env:PATH"

Write-Host "ANDROID_HOME = $env:ANDROID_HOME" -ForegroundColor Cyan
Write-Host "adb --version:" -ForegroundColor Cyan
adb --version
Write-Host ""

# 2. List AVDs
Write-Host "Available AVDs:" -ForegroundColor Cyan
emulator -list-avds
Write-Host ""

# 3. Create test_device if missing
$avds = emulator -list-avds 2>$null
if ($avds -notmatch "test_device") {
    Write-Host "Creating AVD 'test_device'..." -ForegroundColor Yellow
    avdmanager create avd -n "test_device" -k "system-images;android-35;google_apis;arm64-v8a" -d "pixel_6"
} else {
    Write-Host "AVD 'test_device' already exists." -ForegroundColor Green
}

# 4. Workspace
$workspace = "$env:TEMP\dejima-workspace"
$reference = "$env:TEMP\hello-dejima"
Write-Host ""
Write-Host "Workspace: $workspace" -ForegroundColor Cyan
Write-Host "Reference: $reference" -ForegroundColor Cyan
Write-Host ""
Write-Host "To start emulator: emulator -avd test_device -no-window -no-audio" -ForegroundColor Yellow
Write-Host "Then wait ~45s and run: adb wait-for-device" -ForegroundColor Yellow
