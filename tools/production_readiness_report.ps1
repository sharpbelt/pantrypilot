param(
    [string]$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path,
    [string]$GitRoot = "C:\tmp\PantryPilot-git",
    [string]$Output = "C:\tmp\PantryPilot-production-readiness.md"
)

$ErrorActionPreference = "Stop"
$checks = New-Object System.Collections.Generic.List[object]

function Add-Check([string]$Name, [bool]$Passed, [string]$Evidence) {
    $checks.Add([pscustomobject]@{
        Name = $Name
        Passed = $Passed
        Evidence = $Evidence
    }) | Out-Null
}

function File-Hash([string]$Path) {
    if (!(Test-Path -LiteralPath $Path)) { return "missing" }
    return (Get-FileHash -LiteralPath $Path -Algorithm SHA256).Hash
}

function Public-Url-Check([string]$Name, [string]$Url, [string]$RequiredText) {
    try {
        $response = Invoke-WebRequest -UseBasicParsing -Uri $Url -TimeoutSec 20
        $containsText = [string]::IsNullOrEmpty($RequiredText) -or $response.Content.Contains($RequiredText)
        Add-Check $Name ($response.StatusCode -eq 200 -and $containsText) "$Url returned HTTP $($response.StatusCode)"
    } catch {
        Add-Check $Name $false "$Url failed: $($_.Exception.Message)"
    }
}

$releaseAab = "C:\tmp\PantryPilot-release.aab"
$releaseApk = "C:\tmp\PantryPilot-release.apk"
$storePackage = "C:\tmp\PantryPilot-store-page-package.zip"
$keystore = Join-Path $ProjectRoot "pantrypilot-upload.jks"
$keystoreProperties = Join-Path $ProjectRoot "keystore.properties"

Add-Check "Release AAB exists" (Test-Path -LiteralPath $releaseAab) "SHA256: $(File-Hash $releaseAab)"
Add-Check "Signed release APK exists" (Test-Path -LiteralPath $releaseApk) "SHA256: $(File-Hash $releaseApk)"
Add-Check "Store upload package exists" (Test-Path -LiteralPath $storePackage) "SHA256: $(File-Hash $storePackage)"
Add-Check "Upload keystore exists" (Test-Path -LiteralPath $keystore) "Back up this file outside the development computer."
Add-Check "Signing properties exist" (Test-Path -LiteralPath $keystoreProperties) "Keep this secret and back it up securely."

if (Test-Path -LiteralPath $storePackage) {
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $archive = [IO.Compression.ZipFile]::OpenRead($storePackage)
    try {
        $names = @($archive.Entries | ForEach-Object { $_.FullName })
        $screenshots = @($names | Where-Object { $_ -match '^phone_screenshots\\0[1-6]_.+\.png$' })
        $hasKit = $names -contains "CLOSED_TESTING_GUIDE.md" -and $names -contains "CLOSED_TESTER_TRACKER.csv"
        $hasDebugCaptures = @($names | Where-Object { $_ -match 'raw|07_|08_' }).Count -gt 0
        Add-Check "Store package has six production screenshots" ($screenshots.Count -eq 6) "$($screenshots.Count) expected screenshots found."
        Add-Check "Store package includes closed-test kit" $hasKit "Guide and tester tracker are packaged."
        Add-Check "Store package excludes debug captures" (!$hasDebugCaptures) "No raw, 07, or 08 screenshots found."
    } finally {
        $archive.Dispose()
    }
}

if (Test-Path -LiteralPath $GitRoot) {
    $gitStatus = (& git -C $GitRoot status --porcelain) -join "`n"
    Add-Check "Git launch source is clean" ([string]::IsNullOrWhiteSpace($gitStatus)) $(if ($gitStatus) { $gitStatus } else { "No uncommitted changes." })
}

Public-Url-Check "Hosted privacy policy is live" "https://sharpbelt.github.io/pantrypilot/PRIVACY_POLICY.md" "tarkovchains@gmail.com"
Public-Url-Check "Root app-ads.txt is live" "https://sharpbelt.github.io/app-ads.txt" "pub-4099703658403844"

$manualGates = @(
    "Create, price, and activate all three one-time products in Play Console.",
    "Upload version code 103 to Internal testing and complete real Play Billing tests.",
    "Complete the Play Console store listing and all app-content declarations.",
    "Recruit at least 15 closed testers so 12 can remain opted in continuously for 14 days.",
    "Apply for production access after Play Console marks the closed-test requirement complete.",
    "Back up the upload keystore and signing credentials outside this computer."
)

$failed = @($checks | Where-Object { !$_.Passed })
$lines = New-Object System.Collections.Generic.List[string]
$lines.Add("# PantryPilot Production Readiness Report") | Out-Null
$lines.Add("") | Out-Null
$lines.Add("Generated: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss K')") | Out-Null
$lines.Add("") | Out-Null
$lines.Add("## Automated Checks") | Out-Null
$lines.Add("") | Out-Null
foreach ($check in $checks) {
    $status = if ($check.Passed) { "PASS" } else { "FAIL" }
    $lines.Add("- **$status** - $($check.Name): $($check.Evidence)") | Out-Null
}
$lines.Add("") | Out-Null
$lines.Add("## Manual Play Console Gates") | Out-Null
$lines.Add("") | Out-Null
foreach ($gate in $manualGates) {
    $lines.Add("- [ ] $gate") | Out-Null
}
$lines.Add("") | Out-Null
$lines.Add("## Verdict") | Out-Null
$lines.Add("") | Out-Null
if ($failed.Count -eq 0) {
    $lines.Add("Automated readiness checks pass. Public production remains blocked only by the manual Play Console gates above.") | Out-Null
} else {
    $lines.Add("Do not upload or promote until the failed automated checks above are resolved.") | Out-Null
}

$lines | Set-Content -LiteralPath $Output -Encoding UTF8
$lines | ForEach-Object { Write-Output $_ }
Write-Output ""
Write-Output "Report written to $Output"

if ($failed.Count -gt 0) { exit 1 }
exit 0

