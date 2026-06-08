param(
    [string]$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path,
    [string]$Output = "C:\tmp\PantryPilot-store-page-package.zip"
)

$ErrorActionPreference = "Stop"
$storeDir = Join-Path $ProjectRoot "store_page"
$verificationPath = Join-Path $storeDir "SCREENSHOT_VERIFICATION.json"
$staging = Join-Path "C:\tmp" "PantryPilot-store-page-package"
$requiredScreenshots = @(
    "01_home.png",
    "02_scan.png",
    "03_scan_parser.png",
    "04_pantry.png",
    "05_grocery.png",
    "06_meals.png"
)
$requiredTabletScreenshots = @(
    "01_home.png",
    "02_scan_parser.png",
    "03_pantry.png",
    "04_meals.png"
)
$requiredFiles = @(
    "PLAY_STORE_PAGE.md",
    "PLAY_CONSOLE_FINAL_SUBMISSION.md",
    "CLOSED_TESTING_GUIDE.md",
    "CLOSED_TESTER_TRACKER.csv",
    "UPLOAD_CHECKLIST.md",
    "SCREENSHOT_MANIFEST.csv",
    "SCREENSHOT_VERIFICATION.json",
    "graphics\app_icon_512.png",
    "graphics\feature_graphic_1024x500.png"
)

function Require($condition, $message) {
    if (!$condition) { throw $message }
    Write-Output "[PASS] $message"
}

Require (Test-Path -LiteralPath $verificationPath) "Screenshot verification manifest exists; recapture screenshots if missing"
$verification = Get-Content -LiteralPath $verificationPath -Raw | ConvertFrom-Json
Require ($verification.assertions -contains "Debug Plans screens excluded") "Screenshot verification excludes debug Plans screens"

foreach ($file in $requiredFiles) {
    Require (Test-Path -LiteralPath (Join-Path $storeDir $file)) "Required store file exists: $file"
}

foreach ($file in $requiredScreenshots) {
    $path = Join-Path $storeDir "phone_screenshots\$file"
    Require (Test-Path -LiteralPath $path) "Required screenshot exists: $file"
    $entry = $verification.screenshots | Where-Object { $_.file -eq "phone_screenshots/$file" }
    Require ($null -ne $entry) "Verification contains screenshot: $file"
    Require ((Get-FileHash -LiteralPath $path -Algorithm SHA256).Hash -eq $entry.sha256) "Screenshot hash matches verification: $file"
}

Add-Type -AssemblyName System.Drawing
foreach ($tablet in @(
    @{ Folder = "tablet_7_screenshots"; MinimumShortSide = 320 },
    @{ Folder = "tablet_10_screenshots"; MinimumShortSide = 1080 }
)) {
    $tabletDir = Join-Path $storeDir $tablet.Folder
    $tabletVerificationPath = Join-Path $tabletDir "VERIFICATION.json"
    Require (Test-Path -LiteralPath $tabletVerificationPath) "Tablet verification exists: $($tablet.Folder)"
    $tabletVerification = Get-Content -LiteralPath $tabletVerificationPath -Raw | ConvertFrom-Json
    foreach ($file in $requiredTabletScreenshots) {
        $path = Join-Path $tabletDir $file
        Require (Test-Path -LiteralPath $path) "Required tablet screenshot exists: $($tablet.Folder)/$file"
        $entry = $tabletVerification.screenshots | Where-Object { $_.file -eq "$($tablet.Folder)/$file" }
        Require ($null -ne $entry) "Tablet verification contains: $($tablet.Folder)/$file"
        Require ((Get-FileHash -LiteralPath $path -Algorithm SHA256).Hash -eq $entry.sha256) "Tablet screenshot hash matches: $($tablet.Folder)/$file"
        $image = [Drawing.Image]::FromFile($path)
        try {
            $shortSide = [Math]::Min($image.Width, $image.Height)
            $longSide = [Math]::Max($image.Width, $image.Height)
            Require ($shortSide -ge $tablet.MinimumShortSide) "Tablet screenshot minimum side is valid: $($tablet.Folder)/$file"
            Require ([Math]::Abs(($longSide / $shortSide) - (16.0 / 9.0)) -lt 0.01) "Tablet screenshot is 9:16 or 16:9: $($tablet.Folder)/$file"
        } finally {
            $image.Dispose()
        }
    }
}

if (Test-Path -LiteralPath $staging) {
    Remove-Item -LiteralPath $staging -Recurse -Force
}
New-Item -ItemType Directory -Force -Path (Join-Path $staging "graphics"), (Join-Path $staging "phone_screenshots"), (Join-Path $staging "tablet_7_screenshots"), (Join-Path $staging "tablet_10_screenshots") | Out-Null

foreach ($file in $requiredFiles | Where-Object { $_ -notmatch "^(graphics|phone_screenshots)\\" }) {
    Copy-Item -LiteralPath (Join-Path $storeDir $file) -Destination (Join-Path $staging $file)
}
Copy-Item -LiteralPath (Join-Path $storeDir "graphics\app_icon_512.png") -Destination (Join-Path $staging "graphics\app_icon_512.png")
Copy-Item -LiteralPath (Join-Path $storeDir "graphics\feature_graphic_1024x500.png") -Destination (Join-Path $staging "graphics\feature_graphic_1024x500.png")
foreach ($file in $requiredScreenshots) {
    Copy-Item -LiteralPath (Join-Path $storeDir "phone_screenshots\$file") -Destination (Join-Path $staging "phone_screenshots\$file")
}
foreach ($folder in @("tablet_7_screenshots", "tablet_10_screenshots")) {
    foreach ($file in $requiredTabletScreenshots + @("VERIFICATION.json")) {
        Copy-Item -LiteralPath (Join-Path $storeDir "$folder\$file") -Destination (Join-Path $staging "$folder\$file")
    }
}

foreach ($file in @("PRIVACY_POLICY.md", "DATA_SAFETY.md", "STORE_LAUNCH_READINESS.md", "GOOGLE_PLAY_MONETIZATION_SETUP.md")) {
    $source = Join-Path $ProjectRoot $file
    Require (Test-Path -LiteralPath $source) "Required launch document exists: $file"
    Copy-Item -LiteralPath $source -Destination (Join-Path $staging $file)
}

if (Test-Path -LiteralPath $Output) {
    Remove-Item -LiteralPath $Output -Force
}
Compress-Archive -Path (Join-Path $staging "*") -DestinationPath $Output -Force
Require (Test-Path -LiteralPath $Output) "Store page package created: $Output"
Write-Output "Store page package contains verified phone, 7-inch tablet, and 10-inch tablet screenshots with no debug Plans screenshots."
