param(
    [string]$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path,
    [string]$KeyStorePath = "C:\tmp\PantryPilot\pantrypilot-upload.jks",
    [string]$Alias = "pantrypilot",
    [string]$DName = "CN=PantryPilot, OU=Release, O=PantryPilot, L=Local, S=Local, C=GB",
    [string]$PasswordFile
)

$ErrorActionPreference = "Stop"

function Read-SecretText($prompt) {
    $secure = Read-Host $prompt -AsSecureString
    $ptr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
    try {
        return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($ptr)
    } finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ptr)
    }
}

function ConvertTo-JavaPropertyValue([string]$value) {
    $sb = [System.Text.StringBuilder]::new()
    for ($i = 0; $i -lt $value.Length; $i++) {
        $ch = $value[$i]
        $code = [int][char]$ch
        if ($ch -eq "\") { [void]$sb.Append("\\") }
        elseif ($ch -eq "`t") { [void]$sb.Append("\t") }
        elseif ($ch -eq "`r") { [void]$sb.Append("\r") }
        elseif ($ch -eq "`n") { [void]$sb.Append("\n") }
        elseif ($ch -eq "`f") { [void]$sb.Append("\f") }
        elseif ($ch -eq " " -and $i -eq 0) { [void]$sb.Append("\ ") }
        elseif ($ch -eq "=" -or $ch -eq ":" -or $ch -eq "#" -or $ch -eq "!") {
            [void]$sb.Append("\")
            [void]$sb.Append($ch)
        } elseif ($code -lt 32 -or $code -gt 126) {
            [void]$sb.Append(("\u{0:x4}" -f $code))
        } else {
            [void]$sb.Append($ch)
        }
    }
    return $sb.ToString()
}

function Join-ProcessArguments([string[]]$Arguments) {
    (($Arguments | ForEach-Object {
        if ($_ -match '[\s"]') { '"' + ($_.Replace('"', '\"')) + '"' } else { $_ }
    }) -join " ")
}

$keytool = "C:\Program Files\Java\jdk-21\bin\keytool.exe"
if (!(Test-Path -LiteralPath $keytool)) {
    $keytool = "keytool"
}

if (Test-Path -LiteralPath $KeyStorePath) {
    throw "Keystore already exists at $KeyStorePath. Move it first if you really want to replace it."
}

if ($PasswordFile) {
    if (!(Test-Path -LiteralPath $PasswordFile)) {
        throw "Password file not found: $PasswordFile"
    }
    $storePassword = [System.IO.File]::ReadAllText($PasswordFile).TrimEnd([char]13, [char]10)
    $keyPassword = $storePassword
} else {
    $storePassword = Read-SecretText "Upload keystore password"
    $keyPassword = Read-SecretText "Upload key password"
}

if ([string]::IsNullOrWhiteSpace($storePassword) -or [string]::IsNullOrWhiteSpace($keyPassword)) {
    throw "Passwords cannot be empty."
}

function Invoke-KeytoolWithSecretInput([string[]]$Arguments, [string[]]$PasswordLines) {
    $psi = [System.Diagnostics.ProcessStartInfo]::new()
    $psi.FileName = $keytool
    $psi.Arguments = Join-ProcessArguments $Arguments
    $psi.RedirectStandardInput = $true
    $psi.RedirectStandardOutput = $true
    $psi.RedirectStandardError = $true
    $psi.UseShellExecute = $false
    $process = [System.Diagnostics.Process]::Start($psi)
    foreach ($line in $PasswordLines) {
        $process.StandardInput.WriteLine($line)
    }
    $process.StandardInput.Close()
    $stdout = $process.StandardOutput.ReadToEnd()
    $stderr = $process.StandardError.ReadToEnd()
    $process.WaitForExit()
    if ($process.ExitCode -ne 0) {
        $safe = $stdout + "`n" + $stderr
        foreach ($line in $PasswordLines) {
            if (![string]::IsNullOrEmpty($line)) {
                $safe = $safe -replace [regex]::Escape($line), "[redacted]"
            }
        }
        throw "keytool failed with exit code $($process.ExitCode): $safe"
    }
}

$keyStoreDir = Split-Path -Parent $KeyStorePath
New-Item -ItemType Directory -Force -Path $keyStoreDir | Out-Null

Invoke-KeytoolWithSecretInput `
    -Arguments @("-genkeypair", "-noprompt", "-keystore", $KeyStorePath, "-storetype", "PKCS12", "-keyalg", "RSA", "-keysize", "4096", "-validity", "10000", "-alias", $Alias, "-dname", $DName) `
    -PasswordLines @($storePassword, $storePassword)

Invoke-KeytoolWithSecretInput `
    -Arguments @("-list", "-keystore", $KeyStorePath, "-alias", $Alias) `
    -PasswordLines @($storePassword)

$storeFile = $KeyStorePath.Replace("\", "/")
$propertiesPath = Join-Path $ProjectRoot "keystore.properties"
@(
    "storeFile=$storeFile",
    "storePassword=$(ConvertTo-JavaPropertyValue $storePassword)",
    "keyAlias=$Alias",
    "keyPassword=$(ConvertTo-JavaPropertyValue $keyPassword)"
) | Set-Content -LiteralPath $propertiesPath -Encoding ASCII

Write-Output "Created upload keystore: $KeyStorePath"
Write-Output "Created signing properties: $propertiesPath"
Write-Output "Back up the .jks and passwords. Losing them can block future app updates."
