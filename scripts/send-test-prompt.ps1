# Quick test: send "Hello" to the OpenClaw agent
# Prerequisites: Gateway running, API keys (ANTHROPIC_API_KEY, GOOGLE_AI_API_KEY) set
# Run from project root: .\scripts\send-test-prompt.ps1

$configPath = "$env:USERPROFILE\.openclaw\openclaw.json"
if (Test-Path $configPath) {
    $config = Get-Content $configPath -Raw | ConvertFrom-Json
    $token = $config.gateway.auth.token
    if ($token) { $env:OPENCLAW_GATEWAY_TOKEN = $token }
}

Write-Host "Sending test message to OpenClaw agent (dev)..." -ForegroundColor Cyan
cd $PSScriptRoot\..\openclaw
node openclaw.mjs agent --agent dev --message "Hello, confirm you're working"
