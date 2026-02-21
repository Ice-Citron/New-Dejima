# Start the test_device emulator for agent pipeline
# Usage: .\scripts\start-emulator.ps1

$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
$env:PATH = "$env:ANDROID_HOME\platform-tools;$env:ANDROID_HOME\emulator;$env:PATH"

Write-Host "Starting emulator (test_device)..." -ForegroundColor Cyan
Start-Process -FilePath "emulator" -ArgumentList "-avd", "test_device", "-no-window", "-no-audio" -NoNewWindow
Write-Host "Waiting for device (~45s)..." -ForegroundColor Yellow
adb wait-for-device
$booted = adb shell getprop sys.boot_completed 2>$null
while ($booted -ne "1") {
    Start-Sleep -Seconds 5
    $booted = adb shell getprop sys.boot_completed 2>$null
}
Write-Host "Emulator ready." -ForegroundColor Green
adb devices
