@echo off
cd /d "%~dp0"
set /p PHONE=Device name:
node server.js --pair "%PHONE%"
echo Copy this token into the Mathapelo app's Remote Control settings.
pause
