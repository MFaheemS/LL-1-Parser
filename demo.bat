@echo off
setlocal

cd /d "%~dp0"

echo Building project...
call build.bat
if errorlevel 1 goto :fail

if not exist output\g1 mkdir output\g1
if not exist output\g2 mkdir output\g2
if not exist output\g3 mkdir output\g3
if not exist output\g4 mkdir output\g4

echo.
echo Testing Grammar 1 (Simple)...
java -cp out Main input/grammar1.txt output\g1 input/input_grammar1_simple_valid_invalid_missing_extra_empty.txt
if errorlevel 1 goto :fail

echo.
echo Testing Grammar 2 (Expression Grammar)...
java -cp out Main input/grammar2.txt output\g2 input/input_grammar2_expression_valid_invalid_missing_extra_empty_error_recovery.txt
if errorlevel 1 goto :fail

echo.
echo Testing Grammar 3 (Statement Grammar)...
java -cp out Main input/grammar3.txt output\g3 input/input_grammar3_statement_left_factoring_valid_invalid_missing_extra.txt
if errorlevel 1 goto :fail

echo.
echo Testing Grammar 4 (Indirect Left Recursion)...
java -cp out Main input/grammar4.txt output\g4 input/input_grammar4_indirect_left_recursion_valid_invalid_missing_extra.txt
if errorlevel 1 goto :fail

echo.
echo Demo completed. Check output\g1, output\g2, output\g3, output\g4.
exit /b 0

:fail
echo.
echo Demo failed. Check messages above.
exit /b 1