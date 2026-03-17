@echo off
setlocal

if not exist out mkdir out

javac -d out src\*.java
if errorlevel 1 exit /b %errorlevel%

echo Build completed successfully.