@echo off
setlocal

cd /d "%~dp0"

echo Running consolidated demo...
powershell -ExecutionPolicy Bypass -File consolidate.ps1

if errorlevel 1 (
    echo.
    echo Demo failed. Check messages above.
    exit /b 1
) else (
    echo.
    echo Demo completed successfully.
    exit /b 0
)


:fail
echo.
echo Demo failed. Check messages above.
exit /b 1