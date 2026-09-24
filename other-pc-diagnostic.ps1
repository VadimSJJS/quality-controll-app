<#
.SYNOPSIS

.DESCRIPTION
        powershell -ExecutionPolicy Bypass -File .\other-pc-diagnostic.ps1

.PARAMETER ServerName

.PARAMETER ServerIp

.PARAMETER Port
#>
param(
    [string]$ServerName = "devteam7",
    [string]$ServerIp = "172.16.21.2",
    [int]$Port = 8082
)

$ErrorActionPreference = "Continue"
$okList = New-Object System.Collections.Generic.List[string]
$problems = New-Object System.Collections.Generic.List[string]

function Step($title) { Write-Host ""; Write-Host "=== $title ===" -ForegroundColor Cyan }

Step "1. Проверка имени '$ServerName' (DNS / hosts)"

$resolvedIp = $null
try {
    $addr = [Net.Dns]::GetHostAddresses($ServerName) |
        Where-Object { $_.AddressFamily -eq "InterNetwork" } | Select-Object -First 1
    if ($addr) { $resolvedIp = $addr.IPAddressToString }
}
catch { }

$hostsPath = Join-Path $env:SystemRoot "System32\drivers\etc\hosts"
$hostsLine = Select-String -Path $hostsPath -Pattern ("^\s*\d[\d.]+\s+" + [regex]::Escape($ServerName) + "\s*$") -ErrorAction SilentlyContinue
if ($hostsLine) {
    Write-Host "  найдена запись в файле hosts: $($hostsLine.Line.Trim())" -ForegroundColor DarkGray
}

if ($resolvedIp) {
    Write-Host "  имя резолвится -> $resolvedIp" -ForegroundColor Green
    $okList.Add("имя '$ServerName' резолвится в $resolvedIp")
    $ServerIp = $resolvedIp
}
else {
    Write-Host "  НЕ УДАЛОСЬ РАЗРЕШИТЬ ИМЯ '$ServerName' — браузер сразу покажет 'не удаётся получить доступ к сайту'" -ForegroundColor Red
    Write-Host "  Причины: этот ПК не в домене bsw.iron / другой DNS-сервер / нет записи в hosts." -ForegroundColor Yellow
    Write-Host "  Быстрое решение — добавить строку в $hostsPath (от администратора):" -ForegroundColor Yellow
    Write-Host "      $ServerIp    $ServerName" -ForegroundColor Yellow
    $problems.Add("имя '$ServerName' не резолвится с этого ПК (лечится записью в hosts)")
    Write-Host "  Продолжаю проверку по известному IP $ServerIp..." -ForegroundColor Yellow
}

Step "2. Доступность сервера $ServerIp (ping)"
$pingOk = Test-Connection -ComputerName $ServerIp -Count 2 -Quiet
if ($pingOk) {
    Write-Host "  сервер отвечает на ping" -ForegroundColor Green
    $okList.Add("сервер $ServerIp доступен по сети")
}
else {
    Write-Host "  сервер НЕ отвечает на ping" -ForegroundColor Red
    Write-Host "  Причины: этот ПК в другой сети/подсети, нет маршрута, либо ICMP режется файрволом." -ForegroundColor Yellow
    Write-Host "  ping для работы приложения не обязателен, но обычно означает отсутствие маршрута." -ForegroundColor Yellow
    $problems.Add("сервер $ServerIp не пингуется (разные сети или нет маршрута)")
}

Step "3. TCP-порт $Port на $ServerIp"
$tcp = New-Object Net.Sockets.TcpClient
try {
    $tcp.Connect($ServerIp, $Port)
    Write-Host "  порт $Port открыт — приложение запущено и слушает" -ForegroundColor Green
    $okList.Add("порт $Port на $ServerIp открыт")
}
catch {
    Write-Host "  порт $Port ЗАКРЫТ / недоступен: $($_.Exception.InnerException.Message)" -ForegroundColor Red
    Write-Host "  На сервере проверьте: запущен ли start-server.bat и открыт ли порт в файрволе." -ForegroundColor Yellow
    $problems.Add("TCP-порт $Port на $ServerIp недоступен (файрвол или приложение не запущено)")
}

Step "4. HTTPS-сертификат сервера"

$certificate = $null
if ($tcp.Connected) {
    try {
        $ssl = New-Object Net.Security.SslStream($tcp.GetStream(), $false, { param($s, $c, $ch, $e) $true })
        $ssl.AuthenticateAsClient($ServerName)
        $certificate = New-Object Security.Cryptography.X509Certificates.X509Certificate2($ssl.RemoteCertificate)
        $sanExtension = $certificate.Extensions | Where-Object { $_.Oid.Value -eq "2.5.29.17" }
        Write-Host "  Subject : $($certificate.Subject)"
        Write-Host "  Issuer  : $($certificate.Issuer)"
        Write-Host "  Действует до : $($certificate.NotAfter)"
        Write-Host "  SAN     : $(if ($sanExtension) { $sanExtension.Format($false) } else { 'отсутствует' })"

        # Метки SAN зависят от языка Windows ("DNS Name=" против "DNS-имя=", "IP Address=" против "IP-адрес="),
        # поэтому отрезаем всё до первого "=" — сравнение не зависит от локали.
        $nameMatches = $false
        if ($sanExtension) {
            foreach ($entry in ($sanExtension.Format($false) -split ",")) {
                $value = (($entry -split "=", 2)[-1]).Trim()
                if ($value -and ($value -ieq $ServerName -or $value -ieq $ServerIp)) { $nameMatches = $true }
            }
        }
        if ($nameMatches) {
            Write-Host "  имя '$ServerName' есть в SAN сертификата" -ForegroundColor Green
            $okList.Add("имя '$ServerName' есть в сертификате")
        }
        else {
            Write-Host "  имени '$ServerName' НЕТ в SAN — браузер покажет ERR_CERT_COMMON_NAME_INVALID" -ForegroundColor Red
            $problems.Add("в SAN сертификата нет имени '$ServerName' — открывайте по имени из SAN или перевыпустите сертификат")
        }

        $chain = New-Object Security.Cryptography.X509Certificates.X509Chain
        $chain.ChainPolicy.RevocationMode = [Security.Cryptography.X509Certificates.X509RevocationMode]::NoCheck
        $trusted = $chain.Build($certificate)
        if ($trusted) {
            Write-Host "  сертификат ДОВЕРЕННЫЙ на этом ПК" -ForegroundColor Green
            $okList.Add("сертификат доверенный (root-ca установлен)")
        }
        else {
            $reasons = ($chain.ChainStatus | ForEach-Object { $_.StatusInformation.Trim() }) -join "; "
            Write-Host "  сертификат НЕ доверенный: $reasons" -ForegroundColor Red
            Write-Host "  Браузер покажет 'Ваше подключение не защищено'. Лечение: установить root-ca.crt" -ForegroundColor Yellow
            Write-Host "  (двойной клик -> Установить -> 'Доверенные корневые центры сертификации')." -ForegroundColor Yellow
            $problems.Add("корневой сертификат 'BMZ Quality Control Root CA' не установлен на этом ПК")
        }
    }
    catch {
        Write-Host "  TLS-рукопожатие не удалось: $($_.Exception.Message)" -ForegroundColor Red
        $problems.Add("TLS-соединение не устанавливается")
    }
}
else {
    Write-Host "  пропущено — нет TCP-соединения" -ForegroundColor DarkGray
}

Step "5. Настройки прокси в Windows"
$inet = Get-ItemProperty "HKCU:\Software\Microsoft\Windows\CurrentVersion\Internet Settings" -ErrorAction SilentlyContinue
if ($inet.ProxyEnable -eq 1 -and $inet.ProxyServer) {
    Write-Host "  включён системный прокси: $($inet.ProxyServer)" -ForegroundColor Yellow
    Write-Host "  Прокси может не знать внутреннее имя devteam7. Добавьте исключение (Параметры ->" -ForegroundColor Yellow
    Write-Host "  Сеть -> Прокси -> 'Не использовать прокси для локальных адресов' / список исключений)." -ForegroundColor Yellow
    $problems.Add("включён системный прокси $($inet.ProxyServer) — может блокировать внутренние имена")
}
elseif ($inet.AutoConfigURL) {
    Write-Host "  используется автонастройка прокси (PAC): $($inet.AutoConfigURL)" -ForegroundColor Yellow
    Write-Host "  Если PAC не знает devteam7, браузер пойдёт не туда. Проверьте исключения." -ForegroundColor Yellow
    $problems.Add("настроен PAC-прокси $($inet.AutoConfigURL) — может не знать внутреннее имя")
}
else {
    Write-Host "  прокси не используется" -ForegroundColor Green
}

Write-Host ""
Write-Host "=== ИТОГ ДИАГНОСТИКИ ===" -ForegroundColor Cyan
if ($okList.Count -gt 0) {
    Write-Host "  Что в порядке:" -ForegroundColor Green
    $okList | ForEach-Object { Write-Host "    [OK] $_" }
}
if ($problems.Count -eq 0) {
    Write-Host ""
    Write-Host "  Проблем с этого ПК не найдено — https://${ServerName}:${Port}/ должен открываться." -ForegroundColor Green
    Write-Host "  Если браузер всё равно ругается: chrome://restart и проверьте адрес." -ForegroundColor Green
}
else {
    Write-Host "  Найденные проблемы (по ним видна причина):" -ForegroundColor Red
    $problems | ForEach-Object { Write-Host "    [--] $_" -ForegroundColor Red }
}

if ($ssl) { $ssl.Close() }
$tcp.Close()
Write-Host ""
Write-Host "Нажмите Enter для выхода..."
[void](Read-Host)
