@echo off
SETLOCAL ENABLEDELAYEDEXPANSION
set "ROOT_DIR=%cd%"
set "LOG_DIR=%ROOT_DIR%\_logs"
if not exist "%LOG_DIR%" mkdir "%LOG_DIR%"

echo ===============================
echo Install + Start: backend + frontend
echo ===============================

REM ---------- helpers ----------
:install_node_if_needed
  if exist "package.json" (
    echo [INSTALL] %cd%
    call npm install 1>>"%LOG_DIR%\install.log" 2>&1
  )
  exit /b

:start_node_or_angular
  set "NAME=%~n1"
  if exist "angular.json" (
    echo [START][Angular] %cd%
    start "FE:%NAME%" cmd /k "npm start"
    exit /b
  )
  REM Prefer dev, else start
  for /f "tokens=1,* delims=:" %%S in ('findstr /ri "\"dev\"\s*:" package.json 2^>nul') do set HAS_DEV=1
  if defined HAS_DEV (
    echo [START][Node(dev)] %cd%
    start "BE:%NAME%" cmd /k "npm run dev"
  ) else (
    echo [START][Node(start)] %cd%
    start "BE:%NAME%" cmd /k "npm start"
  )
  set HAS_DEV=
  exit /b

:start_spring
  set "NAME=%~n1"
  if exist "mvnw.cmd" (
    echo [START][Spring Boot][mvnw] %cd%
    start "SB:%NAME%" cmd /k "mvnw spring-boot:run"
    exit /b
  )
  if exist "gradlew.bat" (
    echo [START][Spring Boot][gradlew] %cd%
    start "SB:%NAME%" cmd /k
