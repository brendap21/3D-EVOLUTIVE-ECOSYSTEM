@echo off
REM Run script for 3D EVOLUTIVE ECOSYSTEM (Windows cmd.exe)
REM Assumes compiled classes are in bin and main.EcosistemaApp is the main class.
if not exist bin (
  echo bin directory not found. Run build.bat first.
  pause
  exit /b 1
)
java -Xmx1G -cp bin main.EcosistemaApp
pause
