<#
.SYNOPSIS
    Устанавливает корневой сертификат Quality Control App (root-ca.crt) в хранилище
    "Доверенные корневые центры сертификации" (Trusted Root Certification Authorities).

.DESCRIPTION
    Зачем это нужно:
    Chrome/Edge считают сайт защищённым только тогда, когда цепочка сертификатов сервера
    заканчивается доверенным корневым сертификатом. Внутренний сертификат devteam7
    по умолчанию недоверенный, поэтому https перечёркнут и написано "Не защищено".
    Установив корневой сертификат, вы убираете это предупреждение.

    Скрипт нужно выполнить:
      * на сервере devteam7 (чтобы сервер доверял сам себе),
      * на каждом ПК, который открывает https://devteam7:8082/,
        либо один раз раздать сертификат через групповую политику (см. HTTPS-SETUP.md).

    Сертификаты создаются скриптом .\create-devteam7-certificate.ps1
    (в результате создаётся src\main\resources\keystore\root-ca.crt).

.PARAMETER CertificatePath
    Путь к корневому сертификату (по умолчанию src\main\resources\keystore\root-ca.crt).

.PARAMETER CurrentUser
    Установить только для текущего пользователя (права администратора не нужны).

.PARAMETER MachineWide
    Установить для всех пользователей компьютера (нужны права администратора).
    Это режим по умолчанию при запуске от имени администратора.

.PARAMETER Force
    Переустановить сертификат, даже если он уже есть в хранилище.

.PARAMETER RemoveLegacy
    Дополнительно удалить из хранилища старые самоподписанные сертификаты
    (CN=localhost / CN=devteam7), созданные прежними версиями скриптов.

.EXAMPLE
    # Для всех пользователей компьютера (запускать от имени администратора)
    .\install-devteam7-cert.ps1

.EXAMPLE
    # Только для текущего пользователя (без прав администратора)
    .\install-devteam7-cert.ps1 -CurrentUser

.EXAMPLE
    # Установить заранее скопированный сертификат
    .\install-devteam7-cert.ps1 -CertificatePath C:\certs\root-ca.crt
#>
param(
    [string]$CertificatePath = (Join-Path $PSScriptRoot "src\main\resources\keystore\root-ca.crt"),
    [switch]$CurrentUser,
    [switch]$MachineWide,
    [switch]$Force,
    [switch]$RemoveLegacy
)

$ErrorActionPreference = "Stop"

function Test-Administrator {
    $identity = [Security.Principal.WindowsIdentity]::GetCurrent()
    $principal = [Security.Principal.WindowsPrincipal]::new($identity)
    return $principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
}

if (-not (Test-Path -LiteralPath $CertificatePath)) {
    throw "Файл сертификата не найден: $CertificatePath`nСначала выполните .\create-devteam7-certificate.ps1 или скопируйте root-ca.crt из проекта."
}

$certificate = [Security.Cryptography.X509Certificates.X509Certificate2]::new($CertificatePath)

$basicConstraints = $certificate.Extensions | Where-Object { $_.Oid.Value -eq "2.5.29.19" }
$isCertificateAuthority = $false

if ($basicConstraints) {
    if ($basicConstraints.PSObject.Properties["CertificateAuthority"]) {
        $isCertificateAuthority = [bool]$basicConstraints.CertificateAuthority
    }
    elseif ($basicConstraints.Format($false) -match "Subject Type=CA") {
        $isCertificateAuthority = $true
    }
}

if (-not $isCertificateAuthority) {
    Write-Warning "Сертификат '$($certificate.Subject)' не помечен как центр сертификации (CA)."
    Write-Warning "Он всё равно будет установлен как доверенный, но правильнее использовать корневой CA из create-devteam7-certificate.ps1."
}


$isAdministrator = Test-Administrator

if ($CurrentUser) {
    $storeLocation = [Security.Cryptography.X509Certificates.StoreLocation]::CurrentUser
}
elseif ($isAdministrator) {
    $storeLocation = [Security.Cryptography.X509Certificates.StoreLocation]::LocalMachine
}
elseif ($MachineWide) {
    throw "Для установки в LocalMachine нужны права администратора. Запустите PowerShell от имени администратора или используйте -CurrentUser."
}
else {
    Write-Warning "Нет прав администратора — сертификат будет установлен только для текущего пользователя."
    Write-Warning "Чтобы убрать предупреждение браузера у всех пользователей ПК, запустите скрипт от имени администратора."
    $storeLocation = [Security.Cryptography.X509Certificates.StoreLocation]::CurrentUser
}

$store = [Security.Cryptography.X509Certificates.X509Store]::new("Root", $storeLocation)
$store.Open([Security.Cryptography.X509Certificates.OpenFlags]::ReadWrite)

$alreadyInstalled = $false
$added = $false

try {
    $existing = $store.Certificates.Find(
        [Security.Cryptography.X509Certificates.X509FindType]::FindByThumbprint,
        $certificate.Thumbprint,
        $false
    )

    if ($existing.Count -gt 0) {
        $alreadyInstalled = $true
        if ($Force) {
            foreach ($item in $existing) {
                $store.Remove($item)
            }
            $alreadyInstalled = $false
        }
    }

    if (-not $alreadyInstalled) {
        $store.Add($certificate)
        $added = $true
    }
}
finally {
    $store.Close()
}

$sanExtension = $certificate.Extensions | Where-Object { $_.Oid.Value -eq "2.5.29.17" }

# --- Удаление старых самоподписанных сертификатов (необязательно) ------------------------------

$removedLegacy = @()
$failedLegacy = @()

if ($RemoveLegacy) {
    $legacyStore = [Security.Cryptography.X509Certificates.X509Store]::new("Root", $storeLocation)
    $legacyStore.Open([Security.Cryptography.X509Certificates.OpenFlags]::ReadWrite)

    try {
        $legacyCandidates = $legacyStore.Certificates | Where-Object {
            $_.Subject -eq $_.Issuer -and
            ($_.Subject -like "*CN=localhost*" -or $_.Subject -like "*CN=devteam7*") -and
            $_.Subject -notlike "*BMZ Quality Control Root CA*"
        }

        foreach ($legacy in $legacyCandidates) {
            try {
                $legacyStore.Remove($legacy)
                $removedLegacy += "$($legacy.Subject) [$($legacy.Thumbprint)]"
            }
            catch {
                $failedLegacy += "$($legacy.Subject) [$($legacy.Thumbprint)]"
            }
        }
    }
    finally {
        $legacyStore.Close()
    }
}

Write-Host ""
if ($alreadyInstalled) {
    Write-Host "Сертификат уже установлен в '$storeLocation\Root'." -ForegroundColor Yellow
}
else {
    Write-Host "Сертификат установлен в '$storeLocation\Root'." -ForegroundColor Green
}

Write-Host "  Subject    : $($certificate.Subject)"
Write-Host "  Thumbprint : $($certificate.Thumbprint)"
Write-Host "  Действует до: $($certificate.NotAfter)"
if ($sanExtension) {
    Write-Host "  SAN        : $($sanExtension.Format($false))"
}

if ($RemoveLegacy) {
    Write-Host ""
    if ($removedLegacy.Count -gt 0) {
        Write-Host "Удалены старые самоподписанные сертификаты ($($removedLegacy.Count)):" -ForegroundColor Green
        $removedLegacy | ForEach-Object { Write-Host "  $_" }
    }
    elseif ($failedLegacy.Count -eq 0) {
        Write-Host "Старых самоподписанных сертификатов в '$storeLocation\Root' не найдено."
    }

    if ($failedLegacy.Count -gt 0) {
        Write-Host "Не удалось удалить ($($failedLegacy.Count)) — Windows требует подтверждения:" -ForegroundColor Yellow
        $failedLegacy | ForEach-Object { Write-Host "  $_" }
        Write-Host "  Удалите их вручную: certmgr.msc -> Доверенные корневые центры сертификации." -ForegroundColor Yellow
    }
}

if ($added) {
    Write-Host ""
    Write-Host "Полностью закройте и заново откройте браузер (Chrome/Edge), затем проверьте https://devteam7:8082/" -ForegroundColor Yellow
    Write-Host "Проверить, какой сертификат реально отдаёт сервер: .\check-server-certificate.ps1 -ServerName devteam7"
}
