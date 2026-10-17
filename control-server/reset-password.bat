@echo off
cd /d "%~dp0"
node server.js --reset-admin
pause
