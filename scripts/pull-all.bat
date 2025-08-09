@echo off
SETLOCAL

REM --- BACKEND ---
for /d %%i in (backend\*) do (
    if exist "%%i\.git" (
        echo Pulling latest in %%i...
        pushd "%%i"
        git pull origin main || git pull origin master
        popd
    )
)

REM --- FRONTEND ---
for /d %%i in (frontend\*) do (
    if exist "%%i\.git" (
        echo Pulling latest in %%i...
        pushd "%%i"
        git pull origin main || git pull origin master
        popd
    )
)

ENDLOCAL
pause