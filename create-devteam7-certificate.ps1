<#
.SYNOPSIS
    Создаёт собственный корневой центр сертификации (CA) и серверный TLS-сертификат
    для Quality Control App (https://devteam7:8082/).

.DESCRIPTION
    Проблема, которую решает скрипт:
    сертификат, подписанный сам собой (self-signed) и/или выпущенный на CN=localhost,
    Chrome считает недоверенным -> "https" перечёркнут и написано "Не защищено".

    Порядок работы:
      1. В папке "pki" создаётся корневой CA (pki\root-ca.p12) с ключом на 4096 бит.
         Если CA уже существует - он переиспользуется, чтобы не переустанавливать
         сертификат на всех ПК пользователей.
      2. Создаётся ключ сервера и CSR, который подписывается корневым CA.
         В SAN попадают: имя сервера, его FQDN, localhost, IP сервера и 127.0.0.1.
      3. Готовый PKCS12-кейстор с цепочкой "серверный сертификат -> корневой CA"
         записывается в src\main\resources\keystore\keystore.p12 (его использует приложение).
      4. Публичный корневой сертификат записывается в
         src\main\resources\keystore\root-ca.crt.
         Его нужно установить в "Доверенные корневые центры сертификации" на сервере
         и на всех ПК пользователей: .\install-devteam7-cert.ps1 (см. HTTPS-SETUP.md).

.PARAMETER HostName
    Короткое имя сервера, на который выпускается сертификат (по умолчанию devteam7).

.PARAMETER AdditionalDnsNames
    Дополнительные DNS-имена для SAN (например, qc.bsw.iron, localhost).

.PARAMETER AdditionalIpAddresses
    Дополнительные IP-адреса для SAN.

.PARAMETER KeyStorePassword
    Пароль для pki\*.p12 и keystore.p12 (по умолчанию changeit).

.PARAMETER OutputDirectory
    Папка, куда попадут keystore.p12, root-ca.crt, server.crt (по умолчанию resources\keystore).

.PARAMETER CaDirectory
    Папка, где хранится закрытый ключ корневого CA (по умолчанию <репозиторий>\pki).
    Эту папку нельзя копировать на клиентские ПК и коммитить в git.

.PARAMETER NewRootCa
    Пересоздать корневой CA. После этого root-ca.crt нужно заново установить
    на всех ПК (иначе браузеры снова начнут ругаться).

.PARAMETER ValidityDays
    Срок действия сертификатов в днях (по умолчанию 3650 = 10 лет).

.EXAMPLE
    # Обычный выпуск/продление серверного сертификата (CA переиспользуется)
    .\create-devteam7-certificate.ps1

.EXAMPLE
    # Полностью новая PKI
    .\create-devteam7-certificate.ps1 -NewRootCa

.EXAMPLE
    # Дополнительные имена и IP в SAN
    .\create-devteam7-certificate.ps1 -AdditionalDnsNames qc.bsw.iron -AdditionalIpAddresses 172.16.21.2
#>
param(
    [string]$HostName = "devteam7",
    [string[]]$AdditionalDnsNames = @(),
    [string[]]$AdditionalIpAddresses = @(),
    [string]$KeyStorePassword = "changeit",
    [string]$OutputDirectory = (Join-Path $PSScriptRoot "src\main\resources\keystore"),
    [string]$CaDirectory = (Join-Path $PSScriptRoot "pki"),
    [string]$RootCaCommonName = "BMZ Quality Control Root CA",
    [int]$ValidityDays = 3650,
    [switch]$NewRootCa,
    [switch]$Force
)

$ErrorActionPreference = "Stop"

$serverAlias = "qualitycontrollapp"
$rootAlias = "devteam7-root-ca"
$subjectSuffix = "OU=Quality Control, O=BMZ, L=Zhlobin, ST=Brest, C=BY"

if ($Force) {
    $NewRootCa = $true
}

function Get-KeytoolPath {
    $command = Get-Command keytool -ErrorAction SilentlyContinue
    if (-not $command) {
        throw "keytool не найден. Установите JDK 17+ и добавьте %JAVA_HOME%\bin в PATH."
    }
    return $command.Source
}

function Invoke-Keytool {
    param(
        [string]$Keytool,
        [string[]]$Arguments,
        [string]$Description
    )

    & $Keytool @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "keytool завершился с кодом $LASTEXITCODE ($Description)."
    }
}

function Resolve-ServerNames {
    param([string]$Name)

    $dnsNames = New-Object System.Collections.Generic.List[string]
    $ipAddresses = New-Object System.Collections.Generic.List[string]
    $dnsNames.Add($Name.ToLowerInvariant())

    $resolved = $false
    try {
        $records = Resolve-DnsName -Name $Name -Type A -ErrorAction Stop
        $resolved = $true
        foreach ($record in $records) {
            if ($record.PSObject.Properties["IPAddress"] -and $record.IPAddress) {
                $ip = [string]$record.IPAddress
                if (-not $ipAddresses.Contains($ip)) { $ipAddresses.Add($ip) }
            }
            if ($record.PSObject.Properties["NameHost"] -and $record.NameHost) {
                $fqdn = ([string]$record.NameHost).ToLowerInvariant()
                if (-not $dnsNames.Contains($fqdn)) { $dnsNames.Add($fqdn) }
            }
            if ($record.PSObject.Properties["Name"] -and $record.Name) {
                $alias = ([string]$record.Name).ToLowerInvariant()
                if ($alias.Contains(".") -and -not $dnsNames.Contains($alias)) { $dnsNames.Add($alias) }
            }
        }
    }
    catch {
        Write-Warning "DNS-имя '$Name' разрешить не удалось: $($_.Exception.Message)"
    }

    # Полное DNS-имя (FQDN) и все IPv4-адреса узла
    try {
        $entry = [Net.Dns]::GetHostEntry($Name)
        $fqdn = $entry.HostName.ToLowerInvariant()
        if ($fqdn -and $fqdn -ne $Name.ToLowerInvariant() -and -not $dnsNames.Contains($fqdn)) {
            $dnsNames.Add($fqdn)
        }
        foreach ($address in $entry.AddressList) {
            if ($address.AddressFamily -eq [Net.Sockets.AddressFamily]::InterNetwork) {
                $ip = $address.IPAddressToString
                if (-not $ipAddresses.Contains($ip)) { $ipAddresses.Add($ip) }
            }
        }
    }
    catch {
        if (-not $resolved) {
            Write-Warning "IP-адрес для '$Name' определить не удалось."
        }
    }

    return [pscustomobject]@{
        DnsNames    = $dnsNames
        IpAddresses = $ipAddresses
    }
}

$keytool = Get-KeytoolPath

New-Item -ItemType Directory -Force -Path $OutputDirectory | Out-Null
New-Item -ItemType Directory -Force -Path $CaDirectory | Out-Null

$rootKeyStorePath = Join-Path $CaDirectory "root-ca.p12"
$rootCertificatePath = Join-Path $CaDirectory "root-ca.crt"
$serverKeyStorePath = Join-Path $OutputDirectory "keystore.p12"
$serverCertificatePath = Join-Path $OutputDirectory "server.crt"
$distributedRootCertificatePath = Join-Path $OutputDirectory "root-ca.crt"
$csrPath = Join-Path $CaDirectory "server.csr"
$signedCertificatePath = Join-Path $CaDirectory "server-signed.crt"
$fullChainPath = Join-Path $CaDirectory "server-fullchain.crt"
$legacyCertificatePath = Join-Path $OutputDirectory "localhost.crt"

# --- 1. Имена и адреса, которые попадут в SAN -------------------------------------------------

$names = Resolve-ServerNames -Name $HostName

foreach ($name in @("localhost") + $AdditionalDnsNames) {
    $normalized = $name.Trim().ToLowerInvariant()
    if ($normalized -and -not $names.DnsNames.Contains($normalized)) {
        $names.DnsNames.Add($normalized)
    }
}

foreach ($address in @("127.0.0.1") + $AdditionalIpAddresses) {
    $normalized = $address.Trim()
    if ($normalized -and -not $names.IpAddresses.Contains($normalized)) {
        $names.IpAddresses.Add($normalized)
    }
}

$sanValues = @()
$sanValues += $names.DnsNames | ForEach-Object { "dns:$_" }
$sanValues += $names.IpAddresses | ForEach-Object { "ip:$_" }
$san = $sanValues -join ","

Write-Host "SAN сертификата: $san"

# --- 2. Корневой CA ---------------------------------------------------------------------------

$reuseRootCa = (Test-Path -LiteralPath $rootKeyStorePath) -and (-not $NewRootCa)

if ($reuseRootCa) {
    & $keytool -list -alias $rootAlias -keystore $rootKeyStorePath -storepass $KeyStorePassword | Out-Null
    if ($LASTEXITCODE -ne 0) {
        Write-Warning "В '$rootKeyStorePath' нет алиаса '$rootAlias' — корневой CA будет создан заново."
        $reuseRootCa = $false
    }
}

if ($reuseRootCa) {
    Write-Host "Используется существующий корневой CA: $rootKeyStorePath"
}
else {
    Remove-Item -LiteralPath $rootKeyStorePath -Force -ErrorAction SilentlyContinue
    Write-Host "Создаётся новый корневой CA: $rootKeyStorePath"

    Invoke-Keytool -Keytool $keytool -Description "создание корневого CA" -Arguments @(
        "-genkeypair",
        "-alias", $rootAlias,
        "-keyalg", "RSA",
        "-keysize", "4096",
        "-sigalg", "SHA256withRSA",
        "-validity", "$ValidityDays",
        "-keystore", $rootKeyStorePath,
        "-storetype", "PKCS12",
        "-storepass", $KeyStorePassword,
        "-keypass", $KeyStorePassword,
        "-dname", "CN=$RootCaCommonName, $subjectSuffix",
        "-ext", "BC=ca:true,pathlen:0",
        "-ext", "KU=keyCertSign,cRLSign"
    )
}

Invoke-Keytool -Keytool $keytool -Description "экспорт корневого сертификата" -Arguments @(
    "-exportcert",
    "-alias", $rootAlias,
    "-keystore", $rootKeyStorePath,
    "-storepass", $KeyStorePassword,
    "-file", $rootCertificatePath,
    "-rfc"
)

# --- 3. Ключ сервера, CSR и подпись у корневого CA --------------------------------------------

Remove-Item -LiteralPath $serverKeyStorePath, $serverCertificatePath -Force -ErrorAction SilentlyContinue

Invoke-Keytool -Keytool $keytool -Description "создание ключа сервера" -Arguments @(
    "-genkeypair",
    "-alias", $serverAlias,
    "-keyalg", "RSA",
    "-keysize", "2048",
    "-sigalg", "SHA256withRSA",
    "-validity", "$ValidityDays",
    "-keystore", $serverKeyStorePath,
    "-storetype", "PKCS12",
    "-storepass", $KeyStorePassword,
    "-keypass", $KeyStorePassword,
    "-dname", "CN=$HostName, $subjectSuffix",
    "-ext", "SAN=$san",
    "-ext", "KU=digitalSignature,keyEncipherment",
    "-ext", "EKU=serverAuth"
)

Invoke-Keytool -Keytool $keytool -Description "создание CSR" -Arguments @(
    "-certreq",
    "-alias", $serverAlias,
    "-keystore", $serverKeyStorePath,
    "-storepass", $KeyStorePassword,
    "-file", $csrPath
)

Invoke-Keytool -Keytool $keytool -Description "подпись серверного сертификата" -Arguments @(
    "-gencert",
    "-alias", $rootAlias,
    "-keystore", $rootKeyStorePath,
    "-storepass", $KeyStorePassword,
    "-infile", $csrPath,
    "-outfile", $signedCertificatePath,
    "-rfc",
    "-validity", "$ValidityDays",
    "-ext", "SAN=$san",
    "-ext", "KU=digitalSignature,keyEncipherment",
    "-ext", "EKU=serverAuth"
)

# --- 4. Сборка кейстора с полной цепочкой ------------------------------------------------------

$fullChain = (Get-Content -LiteralPath $signedCertificatePath -Raw) + (Get-Content -LiteralPath $rootCertificatePath -Raw)
[IO.File]::WriteAllText($fullChainPath, $fullChain, (New-Object Text.UTF8Encoding($false)))
Copy-Item -LiteralPath $signedCertificatePath -Destination $serverCertificatePath -Force
Copy-Item -LiteralPath $rootCertificatePath -Destination $distributedRootCertificatePath -Force

Invoke-Keytool -Keytool $keytool -Description "импорт цепочки в кейстор приложения" -Arguments @(
    "-importcert",
    "-alias", $serverAlias,
    "-file", $fullChainPath,
    "-keystore", $serverKeyStorePath,
    "-storepass", $KeyStorePassword,
    "-noprompt"
)

Invoke-Keytool -Keytool $keytool -Description "импорт корневого сертификата в кейстор приложения" -Arguments @(
    "-importcert",
    "-alias", $rootAlias,
    "-file", $rootCertificatePath,
    "-keystore", $serverKeyStorePath,
    "-storepass", $KeyStorePassword,
    "-noprompt"
)

if (Test-Path -LiteralPath $legacyCertificatePath) {
    Remove-Item -LiteralPath $legacyCertificatePath -Force
    Write-Host "Удалён устаревший файл localhost.crt (больше не создаётся)."
}

# --- 5. Проверка результата --------------------------------------------------------------------

$listingText = (& $keytool -list -v -alias $serverAlias -keystore $serverKeyStorePath -storepass $KeyStorePassword) -join "`n"

$chainLength = 0
$chainMatch = [regex]::Match($listingText, "Certificate chain length:\s*(\d+)")
if ($chainMatch.Success) {
    $chainLength = [int]$chainMatch.Groups[1].Value
}

if ($chainLength -lt 2) {
    throw "Кейстор не содержит полной цепочки (длина цепочки: $chainLength)."
}

if ($listingText -notmatch [regex]::Escape("DNSName: $HostName")) {
    throw "В SAN сертификата нет DNSName: $HostName."
}

$serverCertificate = [Security.Cryptography.X509Certificates.X509Certificate2]::new($serverCertificatePath)
$rootCertificate = [Security.Cryptography.X509Certificates.X509Certificate2]::new($rootCertificatePath)

# --- 6. Итог -----------------------------------------------------------------------------------

Write-Host ""
Write-Host "Готово. Файлы сертификатов:" -ForegroundColor Green
Write-Host "  серверный кейстор       : $serverKeyStorePath"
Write-Host "  серверный сертификат    : $serverCertificatePath ($($serverCertificate.Thumbprint))"
Write-Host "  корневой сертификат (CA): $distributedRootCertificatePath ($($rootCertificate.Thumbprint))"
Write-Host "  закрытый ключ CA        : $rootKeyStorePath (не копировать на другие ПК и не коммитить в git!)"
Write-Host ""
Write-Host "Дальнейшие шаги:" -ForegroundColor Yellow
Write-Host "  1) Пересобрать приложение : .\mvnw.cmd -DskipTests package"
Write-Host "  2) Развернуть на сервере  : см. HTTPS-SETUP.md"
Write-Host "  3) Доверие корневому CA   : .\install-devteam7-cert.ps1"
Write-Host "     (на сервере и на каждом ПК пользователя либо через групповую политику)"
Write-Host "  4) Проверить сертификат   : .\check-server-certificate.ps1 -ServerName $HostName"

