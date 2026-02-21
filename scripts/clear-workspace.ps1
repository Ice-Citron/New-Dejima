# Clear dejima-workspace between test prompts
# Usage: .\scripts\clear-workspace.ps1

$workspace = "$env:TEMP\dejima-workspace"
if (Test-Path $workspace) {
    Get-ChildItem $workspace -Exclude "gradle-template","gradlew-template","gradlew-template.bat" | Remove-Item -Recurse -Force
    Write-Host "Workspace cleared (templates preserved)." -ForegroundColor Green
} else {
    Write-Host "Workspace does not exist." -ForegroundColor Yellow
}
