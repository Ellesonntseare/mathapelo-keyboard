@echo off
where tailscale >nul 2>&1
if errorlevel 1 (
  echo Tailscale is not installed.
  pause
  exit /b 1
)
tailscale serve off
echo Private Mathapelo HTTPS proxy disabled.
pause
