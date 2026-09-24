<#
.SYNOPSIS
    Проверка keystore/.pfx: пароль, alias ключа, сертификат (Subject/Issuer/SAN) и готовые строки .env.

.DESCRIPTION
    Запускать там, где лежит кейстор (на сервере — рядом с jar):
        powershell -ExecutionPolicy Bypass -File .\check-keystore.ps1 -KeystorePath devteam7keystore.pfx

    Скрипт проверяет именно то, из-за чего приложение падает при старте:
      * "keystore password was incorrect" / "Unable to create key store"
            -> неверный пароль кейстора (SSL_PASSWORD);
      * "Get Key failed: Given final block not properly padded"
            -> кейстор открылся, но ключ не расшифровался: неверный пароль ключа
               (SSL_KEY_PASSWORD) или alias (SSL_KEY_ALIAS), либо приложение грузит
               ДРУГОЙ кейстор (см. блок "SSL-конфигурация приложения" в логе запуска).

.PARAMETER KeystorePath
    Путь к файлу .pfx/.p12 (по умолчанию devteam7keystore.pfx в текущем каталоге).

.PARAMETER StorePassword
    Пароль кейстора. Если не указан — будет запрошен.

.PARAMETER Alias
    Alias ключа, который использует приложение (если не указан — берётся первый найденный).
#>
param(
    [string]$KeystorePath = 'devteam7keystore.pfx',
    [string]$StorePassword,
    [string]$Alias
)

$ErrorActionPreference = 'Continue'

# --- keytool -----------------------------------------------------------------------------------
$keytool = (Get-Command keytool -ErrorAction SilentlyContinue).Source
if (-not $keytool) {
    if ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME 'bin\keytool.exe'))) {
        $keytool = Join-Path $env:JAVA_HOME 'bin\keytool.exe'
    }
    else {
        Write-Host '[ERROR] keytool не найден. Установите JDK или задайте JAVA_HOME.' -ForegroundColor Red
        exit 1
    }
}

if (-not (Test-Path -LiteralPath $KeystorePath)) {
    Write-Host "[ERROR] Файл не найден: $KeystorePath" -ForegroundColor Red
    Write-Host '        Укажите путь явно: -KeystorePath C:\путь\devteam7keystore.pfx' -ForegroundColor Yellow
    exit 1
}
$KeystorePath = (Resolve-Path -LiteralPath $KeystorePath).Path
$fileName = Split-Path -Leaf $KeystorePath

Write-Host "=== Кейстор: $KeystorePath ===" -ForegroundColor Cyan
$fileInfo = Get-Item -LiteralPath $KeystorePath
Write-Host ("  размер: {0} байт, изменён: {1}" -f $fileInfo.Length, $fileInfo.LastWriteTime)
Write-Host ("  рабочий каталог: {0}" -f (Get-Location).Path)

if (-not $StorePassword) {
    $StorePassword = Read-Host "Пароль кейстора (пароль от $fileName)"
}
if (-not $StorePassword) {
    Write-Host '[ERROR] Пароль не задан.' -ForegroundColor Red
    exit 1
}

# Английский вывод keytool — чтобы разбор не зависел от локали.
$prevToolOptions = $env:JAVA_TOOL_OPTIONS
$env:JAVA_TOOL_OPTIONS = '-Duser.language=en -Duser.country=US'

function Restore-Env {
    if ($null -ne $prevToolOptions) { $env:JAVA_TOOL_OPTIONS = $prevToolOptions }
    else { Remove-Item Env:JAVA_TOOL_OPTIONS -ErrorAction SilentlyContinue }
}

# --- 1. пароль кейстора ------------------------------------------------------------------------
Write-Host ''
Write-Host '=== 1. Проверка пароля кейстора ===' -ForegroundColor Cyan
$listOutput = & $keytool -list -keystore $KeystorePath -storepass $StorePassword 2>&1 | Out-String
$listExit = $LASTEXITCODE

if ($listExit -ne 0) {
    Write-Host '  ПАРОЛЬ КЕЙСТОРА НЕВЕРНЫЙ (или файл повреждён):' -ForegroundColor Red
    Write-Host ('  ' + $listOutput.Trim())
    Write-Host '  -> приложение упадёт с ошибкой "keystore password was incorrect".' -ForegroundColor Yellow
    Write-Host '  -> проверьте SSL_PASSWORD в .env (пароль ИТ передаёт вместе с файлом).' -ForegroundColor Yellow
    Restore-Env
    exit 1
}
Write-Host '  пароль верный, кейстор открывается' -ForegroundColor Green

# --- 2. ключи и сертификат ---------------------------------------------------------------------
Write-Host ''
Write-Host '=== 2. Ключи и сертификат ===' -ForegroundColor Cyan

$privateKeyAliases = @()
foreach ($line in ($listOutput -split "`r?`n")) {
    if ($line -match 'PrivateKeyEntry') {
        $privateKeyAliases += ($line -split ',')[0].Trim()
    }
}

if ($privateKeyAliases.Count -eq 0) {
    Write-Host '  В кейсторе НЕТ записи с приватным ключом (только сертификаты)!' -ForegroundColor Red
    Write-Host '  -> приложение не сможет поднять HTTPS: нужен файл, выданный вместе с закрытым ключом.' -ForegroundColor Yellow
    Restore-Env
    exit 1
}

Write-Host ('  найдено ключей: ' + $privateKeyAliases.Count)
$privateKeyAliases | ForEach-Object { Write-Host "    - $_" -ForegroundColor Green }

if (-not $Alias) {
    $Alias = $privateKeyAliases[0]
    if ($privateKeyAliases.Count -gt 1) {
        Write-Host '  [!] ключей несколько — в .env нужен точный alias (SSL_KEY_ALIAS), иначе Tomcat может взять не тот' -ForegroundColor Yellow
    }
}

$detailOutput = & $keytool -list -v -keystore $KeystorePath -storepass $StorePassword 2>&1
$interesting = @('Alias name', 'Owner', 'Issuer', 'Valid from', 'SubjectAlternativeName', 'DNSName', 'IPAddress')
Write-Host ''
Write-Host "  --- сертификат ($Alias) ---"
foreach ($line in $detailOutput) {
    $text = "$line".Trim()
    foreach ($key in $interesting) {
        if ($text -like "$key*") { Write-Host "    $text" }
    }
}

# --- 3. готовые строки .env --------------------------------------------------------------------
Write-Host ''
Write-Host '=== 3. Строки для .env (проверьте, что они совпадают с серверными) ===' -ForegroundColor Cyan
Write-Host 'SSL_ENABLED=true'
Write-Host ("SSL_STORE=file:{0}" -f $fileName)
Write-Host 'SSL_STORE_TYPE=PKCS12'
Write-Host ("SSL_PASSWORD={0}" -f $StorePassword)
Write-Host ("SSL_KEY_PASSWORD={0}" -f $StorePassword)
Write-Host ("SSL_KEY_ALIAS={0}" -f $Alias)

Write-Host ''
Write-Host '=== Что дальше ===' -ForegroundColor Cyan
Write-Host '  1. Файл кейстора положить рядом с jar (в рабочую папку приложения).'
Write-Host '  2. В .env указать строки выше, затем перезапустить start-server.bat.'
Write-Host '  3. В логе запуска проверить блок "SSL-конфигурация приложения":'
Write-Host '     key-store, key-alias и совпадение store/key пароля с тем, что проверено здесь.'
Write-Host '  4. Проверить HTTPS:  .\check-server-certificate.ps1 -ServerName devteam7'
Write-Host ''
Write-Host '  Для файлов PKCS12 (.pfx/.p12) пароль ключа и пароль кейстора совпадают, поэтому' -ForegroundColor DarkGray
Write-Host '  ошибка "Get Key failed ... not properly padded" означает одно из двух:' -ForegroundColor DarkGray
Write-Host '    * в .env указан другой пароль/alias, чем у реального файла;' -ForegroundColor DarkGray
Write-Host '    * приложение грузит ДРУГОЙ кейстор (например, встроенный в jar) —' -ForegroundColor DarkGray
Write-Host '      это видно в блоке "SSL-конфигурация приложения" в логе.' -ForegroundColor DarkGray

Restore-Env
exit 0
