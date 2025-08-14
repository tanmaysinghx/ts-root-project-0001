@echo off
SETLOCAL ENABLEDELAYEDEXPANSION

set "ROOT_DIR=%cd%"

echo ===============================
echo Installing node modules for all services...
echo ===============================

REM --- BACKEND ---
for /d %%i in (backend\*) do (
    echo Checking %%i
    if exist "%%i\package.json" (
        echo Installing in %%i...
        cd /d "%%i"
        call npm install
        cd /d "%ROOT_DIR%"
    )
)

REM --- FRONTEND ---
for /d %%i in (frontend\*) do (
    echo Checking %%i
    if exist "%%i\package.json" (
        echo Installing in %%i...
        cd /d "%%i"
        call npm install
        cd /d "%ROOT_DIR%"
    )
)

echo.
echo ✅ All node modules installed!
ENDLOCAL
pause