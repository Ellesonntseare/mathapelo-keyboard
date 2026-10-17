@echo off
where tailscale >nul 2>&1
if errorlevel 1 (
  echo Install and sign in to Tailscale on Windows and Android first:
  echo https://tailscale.com/download
  pause
  exit /b 1
)
echo Sharing local Mathapelo Studio on your PRIVATE Tailscale network only.
echo Do NOT run tailscale funnel, which makes the server public.
tailscale serve --bg 8787
if errorlevel 1 (
  echo Could not enable Tailscale Serve. Check login and enable HTTPS in Tailscale settings.
  pause
  exit /b 1
)
tailscale serve status
echo Use the HTTPS ts.net address printed above in the Android Remote Studio screen.
pause
