param(
    [string]$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path,
    [string]$GitRoot = "C:\tmp\PantryPilot-git",
    [string]$OutputDirectory = "C:\tmp\PantryPilot-play-console-submission",
    [string]$OutputZip = "C:\tmp\PantryPilot-play-console-submission.zip",
    [switch]$SkipPreflight
)

$ErrorActionPreference = "Stop"

function Require($condition, $message) {
    if (!$condition) { throw $message }
    Write-Output "[PASS] $message"
}

function Assert-SafeOutputPath([string]$Path) {
    $resolvedParent = [IO.Path]::GetFullPath((Split-Path -Parent $Path))
    Require ($resolvedParent -eq "C:\tmp") "Output stays directly under C:\tmp"
}

Assert-SafeOutputPath $OutputDirectory
Assert-SafeOutputPath $OutputZip

$preflight = Join-Path $ProjectRoot "tools\android_preflight.ps1"
$storePackager = Join-Path $ProjectRoot "tools\create_store_page_package.ps1"
$readinessTool = Join-Path $ProjectRoot "tools\production_readiness_report.ps1"
$aab = "C:\tmp\PantryPilot-release.aab"
$storePackage = "C:\tmp\PantryPilot-store-page-package.zip"
$readinessReport = "C:\tmp\PantryPilot-production-readiness.md"

if (!$SkipPreflight) {
    & $preflight
    $preflightSucceeded = $?
    Require $preflightSucceeded "Production preflight passes"
}

& $storePackager
$storePackageSucceeded = $?
Require $storePackageSucceeded "Store page package is current"

& $readinessTool -GitRoot $GitRoot
$readinessSucceeded = $?
Require $readinessSucceeded "Automated production readiness checks pass"

foreach ($path in @($aab, $storePackage, $readinessReport)) {
    Require (Test-Path -LiteralPath $path) "Required submission artifact exists: $path"
}

if (Test-Path -LiteralPath $OutputDirectory) {
    Remove-Item -LiteralPath $OutputDirectory -Recurse -Force
}
New-Item -ItemType Directory -Path $OutputDirectory | Out-Null

$copiedAab = Join-Path $OutputDirectory "PantryPilot-v103-release.aab"
$copiedStorePackage = Join-Path $OutputDirectory "PantryPilot-store-page-assets.zip"
$copiedReadiness = Join-Path $OutputDirectory "PRODUCTION_READINESS.md"
$submissionGuide = Join-Path $OutputDirectory "PLAY_CONSOLE_FINAL_SUBMISSION.md"
$monetizationGuide = Join-Path $OutputDirectory "GOOGLE_PLAY_MONETIZATION_SETUP.md"
$dataSafety = Join-Path $OutputDirectory "DATA_SAFETY.md"
$privacyPolicy = Join-Path $OutputDirectory "PRIVACY_POLICY.md"
$startHere = Join-Path $OutputDirectory "START_HERE.md"
$checksums = Join-Path $OutputDirectory "SHA256SUMS.txt"
$manifest = Join-Path $OutputDirectory "SUBMISSION_MANIFEST.json"

Copy-Item -LiteralPath $aab -Destination $copiedAab
Copy-Item -LiteralPath $storePackage -Destination $copiedStorePackage
Copy-Item -LiteralPath $readinessReport -Destination $copiedReadiness
Copy-Item -LiteralPath (Join-Path $ProjectRoot "store_page\PLAY_CONSOLE_FINAL_SUBMISSION.md") -Destination $submissionGuide
Copy-Item -LiteralPath (Join-Path $ProjectRoot "GOOGLE_PLAY_MONETIZATION_SETUP.md") -Destination $monetizationGuide
Copy-Item -LiteralPath (Join-Path $ProjectRoot "DATA_SAFETY.md") -Destination $dataSafety
Copy-Item -LiteralPath (Join-Path $ProjectRoot "PRIVACY_POLICY.md") -Destination $privacyPolicy

@'
# PantryPilot Play Console Submission

Package: `app.pantrypilot.app`
Version: `1.0.3` / version code `103`

## Upload Order

1. Upload `PantryPilot-v103-release.aab` to Internal testing.
2. Extract `PantryPilot-store-page-assets.zip`.
3. Fill the store listing using `PLAY_CONSOLE_FINAL_SUBMISSION.md`.
4. Complete Data Safety using `DATA_SAFETY.md`.
5. Create and activate the products in `GOOGLE_PLAY_MONETIZATION_SETUP.md`.
6. Complete real Play Billing tests before promotion.
7. Run the closed test using the guide and tracker inside the store-assets ZIP.

`PRODUCTION_READINESS.md` records the automated evidence and remaining manual
gates. `SHA256SUMS.txt` can be used to verify files before upload.

This submission bundle intentionally contains no APK, keystore, signing
properties, password source, or debug output.
'@ | Set-Content -LiteralPath $startHere -Encoding UTF8

$submissionFiles = @(
    $copiedAab,
    $copiedStorePackage,
    $copiedReadiness,
    $submissionGuide,
    $monetizationGuide,
    $dataSafety,
    $privacyPolicy,
    $startHere
)

$hashRows = foreach ($path in $submissionFiles) {
    $hash = (Get-FileHash -LiteralPath $path -Algorithm SHA256).Hash
    "$hash  $(Split-Path -Leaf $path)"
}
$hashRows | Set-Content -LiteralPath $checksums -Encoding ASCII

$manifestObject = [ordered]@{
    generated_at = (Get-Date).ToString("o")
    package = "app.pantrypilot.app"
    version_name = "1.0.3"
    version_code = 103
    upload_file = "PantryPilot-v103-release.aab"
    store_assets = "PantryPilot-store-page-assets.zip"
    contains_secrets = $false
    files = @(
        Get-ChildItem -LiteralPath $OutputDirectory -File |
            Sort-Object Name |
            ForEach-Object {
                [ordered]@{
                    name = $_.Name
                    bytes = $_.Length
                    sha256 = (Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash
                }
            }
    )
}
$manifestObject | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath $manifest -Encoding UTF8

$forbidden = Get-ChildItem -LiteralPath $OutputDirectory -Recurse -File |
    Where-Object { $_.Name -match '\.(jks|keystore|apk|idsig)$' -or $_.Name -match 'keystore\.properties|password|secret' }
Require (@($forbidden).Count -eq 0) "Submission folder contains no APKs, keystores, credentials, or secret files"

if (Test-Path -LiteralPath $OutputZip) {
    Remove-Item -LiteralPath $OutputZip -Force
}
Compress-Archive -Path (Join-Path $OutputDirectory "*") -DestinationPath $OutputZip -Force
Require (Test-Path -LiteralPath $OutputZip) "Play Console submission ZIP created"

Add-Type -AssemblyName System.IO.Compression.FileSystem
$archive = [IO.Compression.ZipFile]::OpenRead($OutputZip)
try {
    $names = @($archive.Entries | ForEach-Object { $_.FullName })
    Require ($names -contains "PantryPilot-v103-release.aab") "Submission ZIP contains the upload AAB"
    Require ($names -contains "PantryPilot-store-page-assets.zip") "Submission ZIP contains store assets"
    Require ($names -contains "PRODUCTION_READINESS.md") "Submission ZIP contains readiness evidence"
    Require ($names -contains "START_HERE.md") "Submission ZIP contains upload-order instructions"
    Require (@($names | Where-Object { $_ -match '\.(jks|keystore|apk|idsig)$|password|secret|keystore\.properties' }).Count -eq 0) "Submission ZIP contains no secret or non-upload release files"
} finally {
    $archive.Dispose()
}

Write-Output ""
Write-Output "Play Console submission folder: $OutputDirectory"
Write-Output "Play Console submission ZIP: $OutputZip"
Write-Output "Submission ZIP SHA256: $((Get-FileHash -LiteralPath $OutputZip -Algorithm SHA256).Hash)"
