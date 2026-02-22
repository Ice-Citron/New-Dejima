# Configure OpenClaw API keys (auth-profiles.json)
# OpenClaw uses auth-profiles.json, NOT env vars directly.
# This script copies keys from env vars or .env files into auth-profiles.json.
#
# Keys are read from (in order):
#   1. Existing env vars (ANTHROPIC_API_KEY, GOOGLE_AI_API_KEY)
#   2. ~/.openclaw/.env
#   3. .env in project root

$ErrorActionPreference = "Stop"

$configSrc = "c:\Users\nahom\New-Dejima\config\openclaw.json"
$configDst = "$env:USERPROFILE\.openclaw\openclaw.json"
$projectRoot = "c:\Users\nahom\New-Dejima"

# Load .env files if keys not already in env
function Load-DotEnv {
    param([string]$path)
    if (Test-Path $path) {
        Get-Content $path | ForEach-Object {
            if ($_ -match '^\s*([^#][^=]+)=(.*)$') {
                $key = $matches[1].Trim()
                $val = $matches[2].Trim().Trim('"').Trim("'")
                $current = [Environment]::GetEnvironmentVariable($key, "Process")
                if (-not $current) {
                    Set-Item -Path "Env:$key" -Value $val
                }
            }
        }
    }
}

# Try loading from .env files (only sets if not already in env)
Load-DotEnv "$env:USERPROFILE\.openclaw\.env"
Load-DotEnv "$projectRoot\.env"

Write-Host "OpenClaw Auth Setup" -ForegroundColor Cyan
Write-Host ""

if (-not $env:ANTHROPIC_API_KEY) {
    Write-Host "ANTHROPIC_API_KEY not set. Set it and run again:" -ForegroundColor Yellow
    Write-Host '  $env:ANTHROPIC_API_KEY = "sk-ant-..."' -ForegroundColor Gray
    exit 1
}

if (-not $env:GOOGLE_AI_API_KEY) {
    Write-Host "GOOGLE_AI_API_KEY not set. Vision may fail. Set it for screenshot analysis:" -ForegroundColor Yellow
    Write-Host '  $env:GOOGLE_AI_API_KEY = "..."   # From ai.google.dev' -ForegroundColor Gray
    Write-Host ""
}

# Paste tokens (paste-token overwrites openclaw.json each time; we restore our config at the end)
Write-Host "Configuring Anthropic auth..." -ForegroundColor Cyan
$env:ANTHROPIC_API_KEY | npx openclaw@latest models auth paste-token --provider anthropic *>&1 | Out-Null

if ($env:GOOGLE_AI_API_KEY) {
    Write-Host "Configuring Google (Gemini) auth..." -ForegroundColor Cyan
    $env:GOOGLE_AI_API_KEY | npx openclaw@latest models auth paste-token --provider google *>&1 | Out-Null
}

# Restore our config (paste-token overwrites it; we need skills, workspace, etc.)
Copy-Item $configSrc $configDst -Force
Write-Host "Config restored." -ForegroundColor Cyan

Write-Host ""
Write-Host "Auth configured. Restart the gateway and run send-test-prompt.ps1" -ForegroundColor Green
