@echo off
rem ============================================================================
rem  Quality Control App - server launcher (https://devteam7:8082/)
rem
rem  Files expected in this folder:
rem    quality-control-app-0.0.1-SNAPSHOT.jar
rem    .env                          (DB_URL / DB_USERNAME / DB_PASSWORD / SSL_*)
rem    devteam7keystore.pfx          (сертификат сервера, лежит рядом с jar)
rem    cert\keystore.p12             (альтернатива: кейстор в папке cert)
rem    cert\root-ca.crt              (root CA, см. install-devteam7-cert.ps1)
rem
rem  Если внешний кейстор не найден — используется кейстор, встроенный в JAR.
rem  Загружает переменные из .env (SSL_STORE / SSL_PASSWORD / SERVER_PORT / ...),
rem  при их отсутствии использует значения по умолчанию из application.properties.
rem ============================================================================
setlocal enableextensions
cd /d "%~dp0"

rem --- загрузка .env (если файл есть) -----------------------------------------
if exist ".env" (
    for /f "usebackq eol=# tokens=1,* delims==" %%A in (".env") do (
        if not defined %%A set "%%A=%%B"
    )
    echo [INFO] Loaded .env
)

set "JAR_FILE=quality-control-app-0.0.1-SNAPSHOT.jar"

if not exist "%JAR_FILE%" (
    echo [ERROR] JAR not found: %JAR_FILE%
    pause
    exit /b 1
)

set "KEYSTORE_CANDIDATE=devteam7keystore.pfx"
if not exist "%KEYSTORE_CANDIDATE%" set "KEYSTORE_CANDIDATE=cert/devteam7keystore.pfx"
if not exist "%KEYSTORE_CANDIDATE%" set "KEYSTORE_CANDIDATE=cert/keystore.p12"
if not exist "%KEYSTORE_CANDIDATE%" set "KEYSTORE_CANDIDATE=keystore.p12"
if not exist "%KEYSTORE_CANDIDATE%" set "KEYSTORE_CANDIDATE=src/main/resources/keystore/keystore.p12"

if not defined SSL_STORE if not defined SERVER_SSL_KEY_STORE if exist "%KEYSTORE_CANDIDATE%" set "SERVER_SSL_KEY_STORE=file:./%KEYSTORE_CANDIDATE%"

if defined SERVER_SSL_KEY_STORE (
    if not defined SERVER_SSL_KEY_STORE_PASSWORD set "SERVER_SSL_KEY_STORE_PASSWORD=changeit"
    if not defined SERVER_SSL_KEY_ALIAS set "SERVER_SSL_KEY_ALIAS=qualitycontrollapp"
    echo [INFO] Keystore: %SERVER_SSL_KEY_STORE%
) else (
    echo [WARN] External keystore not found - using the keystore embedded in the JAR.
    echo [WARN] Put your keystore into cert\keystore.p12 to renew the certificate
    echo [WARN] without rebuilding the application.
)

echo [INFO] Starting Quality Control App (HTTPS, port 8082)...
echo [INFO] Log file: logs\app.log
echo.

if not exist "logs" mkdir "logs"

java -Dlogging.file.name=logs/app.log -jar "%JAR_FILE%"

echo.
echo [INFO] Application exited with code %ERRORLEVEL%
echo [INFO] Look at logs\app.log for details.
pause
