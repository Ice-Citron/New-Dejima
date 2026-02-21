# Run OpenClaw gateway
# Set API keys before running: $env:ANTHROPIC_API_KEY = "sk-ant-..."; $env:GOOGLE_AI_API_KEY = "..."

$openclawPath = Join-Path $PSScriptRoot "..\openclaw"
if (-not (Test-Path $openclawPath)) {
    Write-Error "OpenClaw not found at $openclawPath. Run: git clone https://github.com/openclaw/openclaw.git"
    exit 1
}

Set-Location $openclawPath
pnpm gateway:watch
