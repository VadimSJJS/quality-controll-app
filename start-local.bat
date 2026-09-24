@echo off
rem ============================================================================
rem  Quality Control App - ЛОКАЛЬНЫЙ запуск для разработки  (https://localhost:8082/)
rem
rem  Читаются .env (общие настройки, БД) и .env.local (локальные перекрытия:
rem  встроенный в jar кейстор вместо боевого .pfx). На сервере этого скрипта нет.
rem
rem  Сначала соберите jar:   .\mvnw.cmd -q clean package -DskipTests
rem  Альтернатива без сборки: .\mvnw.cmd spring-boot:run
rem ============================================================================
setlocal enableextensions
cd /d "%~dp0"

set "JAR_FILE=target\quality-control-app-0.0.1-SNAPSHOT.jar"

if not exist "%JAR_FILE%" (
    echo [ERROR] JAR not found: %JAR_FILE%
    echo [INFO] Build it first:  .\mvnw.cmd -q clean package -DskipTests
    pause
    exit /b 1
)

if exist ".env" (
    echo [INFO] Found .env ^(DB и общие настройки^)
) else (
    echo [WARN] .env not found - check DB_URL / DB_USERNAME / DB_PASSWORD
)

if exist ".env.local" (
    echo [INFO] Found .env.local - local HTTPS overrides will be applied
) else (
    echo [WARN] .env.local not found - will use .env as is ^(нужен боевой .pfx рядом с jar^)
)

echo [INFO] Starting locally... HTTPS: https://localhost:8082/
echo [INFO] Log file: logs\app-local.log
echo.

if not exist "logs" mkdir "logs"

java -Dlogging.file.name=logs/app-local.log -jar "%JAR_FILE%"

echo.
echo [INFO] Application exited with code %ERRORLEVEL%
echo [INFO] Look at logs\app-local.log for details.
pause
