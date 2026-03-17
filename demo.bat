@echo off
setlocal

cd /d "%~dp0"

echo ========================================
echo LL(1) Parser Demo
echo ========================================
echo.
echo Building project...
call build.bat
if errorlevel 1 goto :fail

if not exist output mkdir output

del /q output\grammar_transformed.txt >nul 2>&1
del /q output\first_follow_sets.txt >nul 2>&1
del /q output\parsing_table.txt >nul 2>&1
del /q output\parsing_trace1.txt >nul 2>&1
del /q output\parsing_trace2.txt >nul 2>&1
del /q output\parse_trees.txt >nul 2>&1

echo.
echo Running demo with expression grammar...
echo Grammar: input\grammar2.txt
echo Input files: input\input_valid.txt, input\input_errors.txt
echo.

java -cp out Main input/grammar2.txt output input/input_valid.txt input/input_errors.txt
if errorlevel 1 goto :fail

echo.
echo ========================================
echo Demo completed successfully.
echo Output files are available in the output folder.
echo ========================================
echo.
pause
exit /b 0

:fail
echo.
echo ========================================
echo Demo failed.
echo Check the messages above.
echo ========================================
echo.
pause
exit /b 1