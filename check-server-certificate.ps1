<#
.SYNOPSIS
    Полная проверка HTTPS для Quality Control App: сертификат сервера, доверие, работа приложения.

.DESCRIPTION
    Проверяет всё, что важно для браузера:
      1) какой сертификат отдаёт сервер (имя, издатель, SAN, срок действия, версия TLS);
      2) совпадает ли имя, по которому вы подключаетесь, с именем в сертификате (SAN);
      3) доверенный ли сертификат на этом компьютере (то же самое проверяет Chrome/Edge);
      4) где установлен корневой сертификат (LocalMachine или CurrentUser);
      5) отвечает ли само приложение (HTTP GET /login) и нет ли смешанного контента.

    Запускать можно с любого ПК:
      .\check-server-certificate.ps1 -ServerName devteam7
      .\check-server-certificate.ps1 -ServerName localhost

.PARAMETER ServerName
    Имя или IP, по которому открывается приложение (по умолчанию devteam7).

.PARAMETER Port
    Порт приложения (по умолчанию 8082).

.PARAMETER Path
    Страница для HTTP-проверки (по умолчанию /login — публичная страница входа).

.PARAMETER ReadTimeoutMilliseconds
    Сколько миллисекунд ждать ответ приложения (по умолчанию 8000).

.PARAMETER SkipHttpCheck
    Не проверять HTTP-ответ приложения (только сертификат).

.EXAMPLE
    .\check-server-certificate.ps1 -ServerName devteam7
#>
param(
    [string]$ServerName = "devteam7",
    [int]$Port = 8082,
    [string]$Path = "/login",
    [int]$ReadTimeoutMilliseconds = 8000,
    [switch]$SkipHttpCheck
)

$ErrorActionPreference = "Stop"

# --- 1. TCP-подключение ------------------------------------------------------------------------

$tcp = New-Object Net.Sockets.TcpClient
try {
    $tcp.Connect($ServerName, $Port)
}
catch {
    Write-Host "Порт ${ServerName}:${Port} ЗАКРЫТ — приложение не запущено или не слушает порт." -ForegroundColor Red
    Write-Host "  Запустите приложение на сервере (start-server.bat) и повторите проверку."
    exit 1
}

# --- 2. TLS и сертификат -----------------------------------------------------------------------

$ssl = New-Object Net.Security.SslStream($tcp.GetStream(), $false, { param($sender, $certificate, $chain, $errors) $true })
try {
    $ssl.AuthenticateAsClient($ServerName)
}
catch {
    Write-Host "Ошибка TLS-соединения: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

$certificate = New-Object Security.Cryptography.X509Certificates.X509Certificate2($ssl.RemoteCertificate)
$sanExtension = $certificate.Extensions | Where-Object { $_.Oid.Value -eq "2.5.29.17" }
$sanText = if ($sanExtension) { $sanExtension.Format($false) } else { "отсутствует" }

Write-Host "=== Сертификат сервера ${ServerName}:${Port} ===" -ForegroundColor Cyan
Write-Host "Subject      : $($certificate.Subject)"
Write-Host "Issuer       : $($certificate.Issuer)"
Write-Host "Thumbprint   : $($certificate.Thumbprint)"
Write-Host "Действует с  : $($certificate.NotBefore)"
Write-Host "Действует до : $($certificate.NotAfter)"
Write-Host "Протокол TLS : $($ssl.SslProtocol)"
Write-Host "SAN          : $sanText"

# --- 3. Совпадение имени, срок действия, доверие ------------------------------------------------

$nameMatches = $false
if ($sanExtension) {
    # Метки SAN зависят от языка Windows ("DNS Name=" против "DNS-имя="), берём всё после "="
    $entries = $sanExtension.Format($false) -split ","
    foreach ($entry in $entries) {
        $value = (($entry -split "=", 2)[-1]).Trim()
        if ($value -and ($value -ieq $ServerName)) {
            $nameMatches = $true
        }
    }
}

$expired = $certificate.NotAfter -lt (Get-Date)

$chain = New-Object Security.Cryptography.X509Certificates.X509Chain
$chain.ChainPolicy.RevocationMode = [Security.Cryptography.X509Certificates.X509RevocationMode]::NoCheck
$trusted = $chain.Build($certificate)

Write-Host ""
Write-Host "Цепочка сертификатов:"
foreach ($element in $chain.ChainElements) {
    Write-Host "  -> $($element.Certificate.Subject)"
}
if (-not $trusted) {
    foreach ($status in $chain.ChainStatus) {
        Write-Host "  Ошибка проверки: $($status.Status) — $($status.StatusInformation.Trim())" -ForegroundColor Yellow
    }
}

# --- 4. Где установлен корневой сертификат ------------------------------------------------------

$rootThumbprint = $null
if ($chain.ChainElements.Count -gt 0) {
    $rootThumbprint = $chain.ChainElements[$chain.ChainElements.Count - 1].Certificate.Thumbprint
}

$installedInCurrentUser = $false
$installedInLocalMachine = $false

if ($rootThumbprint) {
    try {
        $currentUserThumbprints = @(Get-ChildItem Cert:\CurrentUser\Root | ForEach-Object { $_.Thumbprint })
        $installedInCurrentUser = $currentUserThumbprints -contains $rootThumbprint
    }
    catch {
        $installedInCurrentUser = $false
    }

    try {
        $localMachineThumbprints = @(Get-ChildItem Cert:\LocalMachine\Root | ForEach-Object { $_.Thumbprint })
        $installedInLocalMachine = $localMachineThumbprints -contains $rootThumbprint
    }
    catch {
        $installedInLocalMachine = $false
    }
}

Write-Host ""
Write-Host "Корневой сертификат ($rootThumbprint):"
Write-Host ("  LocalMachine\Root : " + $(if ($installedInLocalMachine) { "установлен" } else { "нет" }))
Write-Host ("  CurrentUser\Root  : " + $(if ($installedInCurrentUser) { "установлен" } else { "нет" }))

# --- 5. Проверка HTTP-ответа приложения --------------------------------------------------------

$httpStatus = $null
$httpError = $null
$mixedContent = @()

if (-not $SkipHttpCheck) {
    $request = "GET $Path HTTP/1.1`r`nHost: ${ServerName}:${Port}`r`nUser-Agent: qc-check`r`nAccept: text/html`r`nConnection: close`r`n`r`n"
    $requestBytes = [Text.Encoding]::ASCII.GetBytes($request)

    try {
        $ssl.Write($requestBytes, 0, $requestBytes.Length)
        $ssl.Flush()
        $ssl.ReadTimeout = $ReadTimeoutMilliseconds

        $buffer = New-Object byte[] 8192
        $builder = New-Object Text.StringBuilder

        $read = $ssl.Read($buffer, 0, $buffer.Length)
        while ($read -gt 0) {
            [void]$builder.Append([Text.Encoding]::UTF8.GetString($buffer, 0, $read))
            if ($builder.Length -gt 200000) { break }
            $read = $ssl.Read($buffer, 0, $buffer.Length)
        }
    }
    catch {
        $httpError = $_.Exception.Message
    }

    $responseText = $builder.ToString()

    if ($responseText -match "^HTTP/\d(\.\d)?\s+(\d{3})") {
        $httpStatus = [int]$Matches[2]
    }

    foreach ($match in [regex]::Matches($responseText, "(src|href|action)\s*=\s*""http://[^""]+""")) {
        $mixedContent += $match.Value
    }

    Write-Host ""
    Write-Host "Ответ приложения (GET $Path):"
    if ($httpStatus) {
        Write-Host "  HTTP-статус : $httpStatus" -ForegroundColor Green
    }
    elseif ($httpError) {
        Write-Host "  Нет ответа: $httpError" -ForegroundColor Red
    }
    else {
        Write-Host "  Нет ответа (пустой ответ, приложение ещё стартует?)" -ForegroundColor Red
    }

    if ($mixedContent.Count -gt 0) {
        Write-Host "  Смешанный контент (ресурсы по http://): найдено $($mixedContent.Count)" -ForegroundColor Yellow
        $mixedContent | Select-Object -Unique | ForEach-Object { Write-Host "    $_" }
    }
    else {
        Write-Host "  Смешанный контент: нет"
    }
}

# --- 6. Итог -----------------------------------------------------------------------------------

$appResponds = ($null -ne $httpStatus) -and ($httpStatus -ge 200) -and ($httpStatus -lt 500)
$secure = $trusted -and $nameMatches -and (-not $expired)

Write-Host ""
Write-Host "=== Итог проверки ===" -ForegroundColor Cyan
Write-Host ("  имя '$ServerName' есть в сертификате : " + $(if ($nameMatches) { "ДА" } else { "НЕТ" }))
Write-Host ("  сертификат доверенный на этом ПК     : " + $(if ($trusted) { "ДА" } else { "НЕТ" }))
Write-Host ("  срок действия                        : " + $(if ($expired) { "ИСТЁК" } else { "в порядке" }))
if (-not $SkipHttpCheck) {
    Write-Host ("  приложение отвечает по HTTPS         : " + $(if ($appResponds) { "ДА ($httpStatus)" } else { "НЕТ" }))
    Write-Host ("  смешанный контент                    : " + $(if ($mixedContent.Count -eq 0) { "нет" } else { "есть" }))
}
Write-Host ""

if ($secure -and ($SkipHttpCheck -or $appResponds) -and $mixedContent.Count -eq 0) {
    Write-Host "ИТОГ: всё в порядке — браузер должен показывать защищённое соединение." -ForegroundColor Green
}
else {
    Write-Host "ИТОГ: есть проблемы:" -ForegroundColor Red

    if (-not $nameMatches) {
        Write-Host "  * в SAN сертификата нет имени '$ServerName' — сервер отдаёт сертификат для другого имени." -ForegroundColor Red
        Write-Host "    Проверьте, что на сервере запущена свежая сборка с новым кейстором (HTTPS-SETUP.md)." -ForegroundColor Red
    }
    if (-not $trusted) {
        Write-Host "  * сертификат сервера не считается доверенным на этом ПК (это и видит Chrome)." -ForegroundColor Red
        Write-Host "    Выполните от администратора: .\install-devteam7-cert.ps1 -RemoveLegacy" -ForegroundColor Red
        Write-Host "    или раздайте root-ca.crt через групповую политику (HTTPS-SETUP.md, п.6.3)." -ForegroundColor Red
    }
    if ($expired) {
        Write-Host "  * срок действия сертификата истёк ($($certificate.NotAfter))." -ForegroundColor Red
        Write-Host "    Выпустите новый: .\create-devteam7-certificate.ps1 (и обновите кейстор на сервере)." -ForegroundColor Red
    }
    if ((-not $SkipHttpCheck) -and (-not $appResponds)) {
        Write-Host "  * приложение не ответило на GET $Path — оно не запущено или не закончило старт." -ForegroundColor Red
        Write-Host "    На сервере запустите start-server.bat и посмотрите logs\app.log." -ForegroundColor Red
    }
    if ($mixedContent.Count -gt 0) {
        Write-Host "  * страница загружает ресурсы по http:// — Chrome помечает такую страницу как небезопасную." -ForegroundColor Red
    }
}

$ssl.Close()
$tcp.Close()
