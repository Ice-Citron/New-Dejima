# Start OpenClaw gateway for the Android app builder pipeline
# Prerequisites: ANTHROPIC_API_KEY and GOOGLE_AI_API_KEY set
# Run from project root: .\scripts\run-gateway.ps1

$ErrorActionPreference = "Stop"

# Ensure config is in place
$configSrc = "c:\Users\nahom\New-Dejima\config\openclaw.json"
$configDst = "$env:USERPROFILE\.openclaw\openclaw.json"
if (-not (Test-Path (Split-Path $configDst))) {
    New-Item -ItemType Directory -Force -Path (Split-Path $configDst) | Out-Null
}
Copy-Item $configSrc $configDst -Force
Write-Host "Config synced to $configDst" -ForegroundColor Cyan

# Sync android-app-builder skill (required for Android builds)
$skillSrc = "c:\Users\nahom\New-Dejima\skills\android-app-builder"
$skillDst = "$env:USERPROFILE\.openclaw\skills\android-app-builder"
if (Test-Path $skillSrc) {
    if (-not (Test-Path (Split-Path $skillDst))) {
        New-Item -ItemType Directory -Force -Path (Split-Path $skillDst) | Out-Null
    }
    Copy-Item $skillSrc\* $skillDst -Force -Recurse
    Write-Host "Skill synced to $skillDst" -ForegroundColor Cyan
}

# Copy hello-dejima reference app to %TEMP% (agent needs it for Gradle wrapper)
$refSrc = "c:\Users\nahom\New-Dejima\android\hello-dejima"
$refDst = "$env:TEMP\hello-dejima"
if (Test-Path $refSrc) {
    Copy-Item $refSrc $refDst -Force -Recurse
    Write-Host "Reference app synced to $refDst" -ForegroundColor Cyan
}

# Ensure workspace exists and has Gradle template (agent copies from here for new projects)
$workspace = "$env:TEMP\dejima-workspace"
if (-not (Test-Path $workspace)) {
    New-Item -ItemType Directory -Force -Path $workspace | Out-Null
}
$gradleTemplate = "$workspace\gradle-template"
if (-not (Test-Path $gradleTemplate) -and (Test-Path "$env:TEMP\hello-dejima\gradle")) {
    Copy-Item "$env:TEMP\hello-dejima\gradle" $gradleTemplate -Recurse -Force
    Copy-Item "$env:TEMP\hello-dejima\gradlew.bat" "$workspace\gradlew-template.bat" -Force -ErrorAction SilentlyContinue
    Write-Host "Gradle template created in workspace" -ForegroundColor Cyan
}

# Check API keys
if (-not $env:ANTHROPIC_API_KEY) {
    Write-Host "WARNING: ANTHROPIC_API_KEY not set. Set it before sending prompts." -ForegroundColor Yellow
}
if (-not $env:GOOGLE_AI_API_KEY) {
    Write-Host "WARNING: GOOGLE_AI_API_KEY not set. Vision (screenshot analysis) may fail." -ForegroundColor Yellow
}

Write-Host ""
Write-Host "Starting OpenClaw gateway on port 18789..." -ForegroundColor Cyan
Write-Host "Control UI: http://127.0.0.1:18789" -ForegroundColor Cyan
Write-Host "Press Ctrl+C to stop." -ForegroundColor Yellow
Write-Host ""

npx openclaw@latest gateway run --port 18789
