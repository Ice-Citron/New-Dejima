# Send workout timer prompt to the OpenClaw agent
# Prerequisites: Gateway running (.\scripts\run-gateway.ps1)
# Run from project root: .\scripts\send-workout-prompt.ps1

$prompt = "Build me a workout timer app. Preset timers (30s, 60s, 90s), start/pause button, countdown with circular progress. Follow the android-app-builder skill and produce a working APK."

Write-Host "Sending prompt to OpenClaw agent..." -ForegroundColor Cyan
Write-Host ""
Write-Host $prompt -ForegroundColor Gray
Write-Host ""
Write-Host "Tip: Gateway must be running. Emulator helps for Phase 5 testing." -ForegroundColor Yellow
Write-Host ""

npx openclaw@latest agent --agent main --message $prompt
