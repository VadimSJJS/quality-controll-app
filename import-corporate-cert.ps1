# ==============================================================================
#  import-corporate-cert.ps1
#  Repacks a certificate issued by the corporate CA (AD CS) into the
#  application keystore (cert\keystore.p12).
#
#  Scenario: the IT department issues a certificate for devteam7 from the
#  corporate Certification Authority and hands over a .pfx file WITH the
#  private key. Every domain PC already trusts the corporate CA root, so
#  after this repack no root-ca installation and no GPO are needed at all -
#  https://devteam7:8082/ is trusted on any domain computer automatically.
#
#  Usage:
#    powershell -ExecutionPolicy Bypass -File import-corporate-cert.ps1 ^
#        -PfxPath C:\temp\devteam7.pfx [-PfxPassword 'secret']
#
#  Optional parameters:
#    -OutKeystore cert\keystore.p12              destination keystore file
#    -KeystorePassword changeit                  password of the destination
#    -Alias qualitycontrollapp                   key alias expected by the app
#    -ExpectedNames devteam7,devteam7.bsw.iron   names that must be in SAN
# ==============================================================================
param(
    [Parameter(Mandatory = $true, Position = 0)]
    [string]$PfxPath,

    [string]$PfxPassword,

    [string]$OutKeystore = 'cert\keystore.p12',

    [string]$KeystorePassword = 'changeit',

    [string]$Alias = 'qualitycontrollapp',

    [string[]]$ExpectedNames = @('devteam7', 'devteam7.bsw.iron')
)

$ErrorActionPreference = 'Stop'

function Fail([string]$message) {
    if ($null -ne $prevToolOptions) { $env:JAVA_TOOL_OPTIONS = $prevToolOptions }
    else { Remove-Item Env:JAVA_TOOL_OPTIONS -ErrorAction SilentlyContinue }
    Write-Host "[ERROR] $message" -ForegroundColor Red
    exit 1
}

Write-Host '=== import-corporate-cert: corporate CA certificate -> app keystore ==='
Write-Host ''

# --- locate the PFX file -------------------------------------------------------
if (-not (Test-Path -LiteralPath $PfxPath)) {
    Fail "PFX file not found: $PfxPath"
}
$PfxPath = (Resolve-Path -LiteralPath $PfxPath).Path

if (-not $PfxPassword) {
    $PfxPassword = Read-Host 'Enter the PFX password (IT gives it together with the file)'
}
if (-not $PfxPassword) {
    Fail 'PFX password is empty.'
}

# --- locate keytool ------------------------------------------------------------
$keytool = Get-Command keytool -ErrorAction SilentlyContinue
if ($keytool) {
    $keytool = $keytool.Source
}
elseif ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME 'bin\keytool.exe'))) {
    $keytool = Join-Path $env:JAVA_HOME 'bin\keytool.exe'
}
else {
    Fail 'keytool not found. Install a JDK or set the JAVA_HOME environment variable.'
}

# Force English keytool output so that parsing stays locale independent.
$prevToolOptions = $env:JAVA_TOOL_OPTIONS
$env:JAVA_TOOL_OPTIONS = '-Duser.language=en -Duser.country=US'

# --- read and validate the certificate -----------------------------------------
try {
    $cert = New-Object System.Security.Cryptography.X509Certificates.X509Certificate2(
        $PfxPath, $PfxPassword,
        [System.Security.Cryptography.X509Certificates.X509KeyStorageFlags]::Exportable)
}
catch {
    Fail "Cannot read the PFX file (wrong password?): $($_.Exception.Message)"
}

Write-Host '[INFO] Certificate inside the PFX:'
Write-Host ("       Subject : {0}" -f $cert.Subject)
Write-Host ("       Issuer  : {0}" -f $cert.Issuer)
Write-Host ("       Valid   : {0} .. {1}" -f $cert.NotBefore.ToString('yyyy-MM-dd'), $cert.NotAfter.ToString('yyyy-MM-dd'))
Write-Host ("       Key     : {0}, private key present: {1}" -f $cert.PublicKey.Oid.FriendlyName, $cert.HasPrivateKey)
Write-Host ''

if (-not $cert.HasPrivateKey) {
    Fail 'The PFX has no private key. Ask IT to re-export it WITH the private key.'
}

$alg = $cert.PublicKey.Oid.FriendlyName
if ($alg -notmatch '^RSA') {
    Write-Host "[WARN] Key algorithm is '$alg'. RSA is recommended for maximum client compatibility." -ForegroundColor Yellow
}

# SAN (check by OID value 2.5.29.17 - locale independent)
$sanExt = $cert.Extensions | Where-Object { $_.Oid.Value -eq '2.5.29.17' } | Select-Object -First 1
$sanItems = @()
if ($sanExt) {
    $sanItems = @($sanExt.Format($false) -split ',' | ForEach-Object {
        $token = ($_ -split '=')[-1].Trim()
        $token
    } | Where-Object { $_ })
}
Write-Host ("[INFO] SAN: {0}" -f ($sanItems -join ', '))

$missing = @($ExpectedNames | Where-Object { $name = $_; -not ($sanItems -contains $name) })
if ($missing.Count -gt 0) {
    Fail ("SAN misses required name(s): {0}. Ask IT to reissue the certificate with these names in SAN." -f ($missing -join ', '))
}

# EKU (check by OID value 2.5.29.37)
$ekuExt = $cert.Extensions | Where-Object { $_.Oid.Value -eq '2.5.29.37' } | Select-Object -First 1
if ($ekuExt) {
    $ekuText = $ekuExt.Format($false)
    if ($ekuText -notmatch 'Server Authentication' -and $ekuText -notmatch '1\.3\.6\.1\.5\.5\.7\.3\.1') {
        Write-Host "[WARN] EKU does not include Server Authentication: $ekuText" -ForegroundColor Yellow
    }
}

# chain (informational only - the final trust check happens on the server)
$chain = New-Object System.Security.Cryptography.X509Certificates.X509Chain
$chain.ChainPolicy.RevocationMode = [System.Security.Cryptography.X509Certificates.X509RevocationMode]::NoCheck
$null = $chain.Build($cert)
if ($chain.ChainElements.Count -gt 1) {
    $root = $chain.ChainElements[$chain.ChainElements.Count - 1].Certificate
    Write-Host ("[INFO] Chain length: {0}; root: {1}" -f $chain.ChainElements.Count, $root.Subject)
}
else {
    Write-Host '[WARN] The PFX contains only the leaf certificate (no CA chain inside).' -ForegroundColor Yellow
}
Write-Host ''

# --- repack into the application keystore --------------------------------------
$outParent = Split-Path -Parent $OutKeystore
if ($outParent -and -not (Test-Path -LiteralPath $outParent)) {
    New-Item -ItemType Directory -Path $outParent -Force | Out-Null
}

if (Test-Path -LiteralPath $OutKeystore) {
    $stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
    $backup = "$OutKeystore.bak-$stamp"
    Copy-Item -LiteralPath $OutKeystore -Destination $backup -Force
    Write-Host "[INFO] Existing keystore backed up to: $backup"
    Remove-Item -LiteralPath $OutKeystore -Force
}

Write-Host '[INFO] Importing the key into the keystore (keytool -importkeystore)...'
& $keytool -importkeystore -noprompt `
    -srckeystore $PfxPath -srcstoretype PKCS12 -srcstorepass $PfxPassword `
    -destkeystore $OutKeystore -deststoretype PKCS12 -deststorepass $KeystorePassword | Out-Null
if ($LASTEXITCODE -ne 0) {
    Fail "keytool import failed (exit code $LASTEXITCODE). Check the PFX password."
}

$list = & $keytool -list -keystore $OutKeystore -storepass $KeystorePassword
$keyLines = @($list | Where-Object { $_ -match 'PrivateKeyEntry' })
if ($keyLines.Count -eq 0) {
    Fail 'No private key entry found in the new keystore.'
}
$oldAlias = ($keyLines[0] -split ',')[0].Trim()
Write-Host "[INFO] Imported key alias: $oldAlias"

if ($oldAlias -ne $Alias) {
    & $keytool -changealias -alias $oldAlias -destalias $Alias `
        -keystore $OutKeystore -storepass $KeystorePassword | Out-Null
    if ($LASTEXITCODE -ne 0) {
        Fail "keytool -changealias failed (exit code $LASTEXITCODE)."
    }
    Write-Host "[INFO] Alias renamed to: $Alias"
}

$detail = & $keytool -list -v -keystore $OutKeystore -storepass $KeystorePassword
Write-Host '[INFO] Keystore content:'
$detail | Where-Object { $_ -match '^(Alias name|Owner|Issuer|Valid from)' } |
    ForEach-Object { Write-Host ("       " + $_) }

Write-Host ''
Write-Host '=== Done. Next steps ==='
Write-Host ("  1. Copy '{0}' to the server folder of the application" -f $OutKeystore)
Write-Host '     (replace the old keystore.p12 there).'
Write-Host '  2. Restart the application (start-server.bat).'
Write-Host '  3. Verify from any PC:'
Write-Host '       powershell -File check-server-certificate.ps1 -ServerName devteam7'
Write-Host ''
Write-Host 'No root-ca installation and no GPO are needed: every domain PC already'
Write-Host 'trusts the corporate CA, so browsers show valid HTTPS on any computer.'

if ($null -ne $prevToolOptions) { $env:JAVA_TOOL_OPTIONS = $prevToolOptions }
else { Remove-Item Env:JAVA_TOOL_OPTIONS -ErrorAction SilentlyContinue }
exit 0
