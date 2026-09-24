# Temp script: remove BOM (EF BB BF) written by Set-Content, keep UTF-8 without BOM
$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
$files = Get-ChildItem -Recurse -Filter *.java -Path src\main\java\com\vadimsjjs\qualitycontrollapp\controller, src\main\java\com\vadimsjjs\qualitycontrollapp\security, src\main\java\com\vadimsjjs\qualitycontrollapp\config
foreach ($f in $files) {
    $bytes = [System.IO.File]::ReadAllBytes($f.FullName)
    if ($bytes.Length -ge 3 -and $bytes[0] -eq 0xEF -and $bytes[1] -eq 0xBB -and $bytes[2] -eq 0xBF) {
        $text = [System.Text.Encoding]::UTF8.GetString($bytes, 3, $bytes.Length - 3)
        [System.IO.File]::WriteAllText($f.FullName, $text, $utf8NoBom)
        Write-Output "BOM removed: $($f.Name)"
    }
}
