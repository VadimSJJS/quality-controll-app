param(
    [string]$ServerName = "devteam7",
    [int]$Port = 8082,
    [string]$Path = "/",
    [int]$ReadTimeoutMilliseconds = 8000,
    [string]$SaveResponseTo = ""
)

$ErrorActionPreference = "Stop"

Write-Host "=== 1. TCP-подключение ${ServerName}:${Port} ==="
$tcp = New-Object Net.Sockets.TcpClient
try {
    $tcp.Connect($ServerName, $Port)
    Write-Host "порт открыт" -ForegroundColor Green
}
catch {
    Write-Host "порт ЗАКРЫТ / приложение не слушает: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "=== 2. TLS-сертификат ==="
$ssl = New-Object Net.Security.SslStream($tcp.GetStream(), $false, { param($s, $c, $ch, $e) $true })
$ssl.AuthenticateAsClient($ServerName)
$certificate = New-Object Security.Cryptography.X509Certificates.X509Certificate2($ssl.RemoteCertificate)
Write-Host "Subject : $($certificate.Subject)"
Write-Host "Issuer  : $($certificate.Issuer)"
Write-Host "Thumb   : $($certificate.Thumbprint)"
Write-Host "Protocol: $($ssl.SslProtocol)"

Write-Host ""
Write-Host "=== 3. HTTP-ответ на GET $Path ==="
$request = "GET $Path HTTP/1.1`r`nHost: ${ServerName}:${Port}`r`nUser-Agent: qc-check`r`nConnection: close`r`n`r`n"
$requestBytes = [Text.Encoding]::ASCII.GetBytes($request)
$ssl.Write($requestBytes, 0, $requestBytes.Length)
$ssl.Flush()

$buffer = New-Object byte[] 8192
$builder = New-Object Text.StringBuilder
$ssl.ReadTimeout = $ReadTimeoutMilliseconds
try {
    $read = $ssl.Read($buffer, 0, $buffer.Length)
    while ($read -gt 0) {
        [void]$builder.Append([Text.Encoding]::UTF8.GetString($buffer, 0, $read))
        if ($builder.Length -gt 20000) { break }
        $read = $ssl.Read($buffer, 0, $buffer.Length)
    }
}
catch {
    Write-Host "(чтение прервано: $($_.Exception.Message))" -ForegroundColor Yellow
}

$response = $builder.ToString()

if ($SaveResponseTo) {
    [IO.File]::WriteAllText($SaveResponseTo, $response, (New-Object Text.UTF8Encoding($false)))
    Write-Host ("Ответ сохранён в: " + $SaveResponseTo)
}

if ([string]::IsNullOrWhiteSpace($response)) {
    Write-Host "Сервер не отправил ответ (возможно, приложение ещё стартует или упало)" -ForegroundColor Red
}
else {
    $lines = $response -split "`r?`n"
    Write-Host $lines[0]
    $lines | Select-Object -Skip 1 -First 12 | ForEach-Object { Write-Host "  $_" }
    Write-Host "..."
    Write-Host ("Длина полученного ответа: " + $response.Length + " символов")
}

$ssl.Close()
$tcp.Close()
