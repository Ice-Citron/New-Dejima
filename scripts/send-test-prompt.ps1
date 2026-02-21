# Send the first test prompt to the OpenClaw agent
# Prerequisites: Gateway running (.\scripts\run-gateway.ps1)
# Run from project root: .\scripts\send-test-prompt.ps1

$prompt = "Build me an Android tip calculator app. Use Kotlin and Jetpack Compose. Input field for bill amount, slider for tip percentage (10-30%), show calculated tip and total. Follow the android-app-builder skill and produce a working APK."

Write-Host "Sending prompt to OpenClaw agent..." -ForegroundColor Cyan
Write-Host ""
Write-Host $prompt -ForegroundColor Gray
Write-Host ""
Write-Host "Tip: Use 'openclaw tui' for interactive chat. Skills load from ~/.openclaw/skills/" -ForegroundColor Yellow
Write-Host ""

npx openclaw@latest agent --agent main --message $prompt
