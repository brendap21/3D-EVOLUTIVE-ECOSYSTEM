@echo off
REM Build script for 3D EVOLUTIVE ECOSYSTEM (Windows cmd.exe)
REM Creates bin directory and compiles all .java from src into bin.
if not exist bin mkdir bin
dir /s /b src\*.java > sources.txt
javac -d bin @sources.txt
if exist sources.txt del sources.txt
echo Build finished.
pause
