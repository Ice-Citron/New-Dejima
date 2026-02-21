# Reset OpenClaw auth — clears auth-profiles.json so you can set keys fresh
# Run: .\scripts\reset-auth.ps1

$authPath = "$env:USERPROFILE\.openclaw\agents\main\agent\auth-profiles.json"
if (Test-Path $authPath) {
    Remove-Item $authPath -Force
    Write-Host "Cleared auth-profiles.json" -ForegroundColor Green
} else {
    Write-Host "No auth-profiles.json found (already clean)" -ForegroundColor Gray
}
Write-Host ""
Write-Host "Next: Set keys and run setup-auth.ps1" -ForegroundColor Cyan
Write-Host '  $env:ANTHROPIC_API_KEY = "sk-ant-..."' -ForegroundColor Gray
Write-Host '  $env:GOOGLE_AI_API_KEY = "..."' -ForegroundColor Gray
Write-Host '  .\scripts\setup-auth.ps1' -ForegroundColor Gray
