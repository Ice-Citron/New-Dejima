# Show which Anthropic key OpenClaw is using (preview only, for debugging)
# Run from project root: .\scripts\show-anthropic-key-preview.ps1

$authPath = "$env:USERPROFILE\.openclaw\agents\dev\agent\auth-profiles.json"
$mainPath = "$env:USERPROFILE\.openclaw\agents\main\agent\auth-profiles.json"

function Show-KeyPreview {
    param([string]$Label, [string]$Key)
    if (-not $Key) { Write-Host "$Label (none)" -ForegroundColor Yellow; return }
    $preview = if ($Key.Length -le 12) { $Key } else { $Key.Substring(0,8) + "..." + $Key.Substring($Key.Length - 8) }
    Write-Host "$Label $preview" -ForegroundColor Cyan
    Write-Host "  Length: $($Key.Length) chars" -ForegroundColor Gray
}

Write-Host "`n=== Anthropic key sources (agent: dev) ===" -ForegroundColor White

# Env var (gateway uses this when running)
$envKey = $env:ANTHROPIC_API_KEY
Show-KeyPreview "ANTHROPIC_API_KEY (env):" $envKey

# Auth profiles - dev agent
if (Test-Path $authPath) {
    $auth = Get-Content $authPath -Raw | ConvertFrom-Json
    foreach ($prof in $auth.profiles.PSObject.Properties) {
        $cred = $prof.Value
        if ($cred.provider -eq "anthropic") {
            $key = if ($cred.key) { $cred.key } elseif ($cred.token) { $cred.token } else { "" }
            Show-KeyPreview "  auth-profiles ($($prof.Name)):" $key
        }
    }
} else {
    Write-Host "  auth-profiles (dev): (file not found)" -ForegroundColor Yellow
}

Write-Host "`nTip: Gateway must have the key when it STARTS." -ForegroundColor Gray
Write-Host "  - Env: set ANTHROPIC_API_KEY before running gateway" -ForegroundColor Gray
Write-Host "  - Stored: run 'openclaw models auth paste-token --provider anthropic' then restart gateway" -ForegroundColor Gray
Write-Host ""
