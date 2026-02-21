# Send workout timer prompt to the OpenClaw agent
# Prerequisites: Gateway running (.\scripts\run-gateway.ps1), API keys set
# Run from project root: .\scripts\send-workout-prompt.ps1

$prompt = "Build me a workout timer app. Preset timers (30s, 60s, 90s), start/pause button, countdown with circular progress. Follow the android-app-builder skill and produce a working APK."

# Use token from config (or set OPENCLAW_GATEWAY_TOKEN manually)
$configPath = "$env:USERPROFILE\.openclaw\openclaw.json"
if (Test-Path $configPath) {
    $config = Get-Content $configPath -Raw | ConvertFrom-Json
    $token = $config.gateway.auth.token
    if ($token) { $env:OPENCLAW_GATEWAY_TOKEN = $token }
}

Write-Host "Sending prompt to OpenClaw agent..." -ForegroundColor Cyan
Write-Host ""
Write-Host $prompt -ForegroundColor Gray
Write-Host ""
Write-Host "Tip: Gateway must be running. Emulator helps for Phase 5 testing." -ForegroundColor Yellow
Write-Host ""

cd $PSScriptRoot\..\openclaw
node openclaw.mjs agent --agent dev --message $prompt
