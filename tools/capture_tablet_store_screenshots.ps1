param(
    [ValidateSet("7-inch", "10-inch")]
    [string]$TabletSize,
    [string]$Serial,
    [string]$Apk = "C:\tmp\PantryPilot-debug.apk",
    [string]$Package = "app.pantrypilot.app",
    [string]$Activity = ".MainActivity",
    [string]$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
)

$ErrorActionPreference = "Stop"
$adb = "adb"
$folderName = if ($TabletSize -eq "7-inch") { "tablet_7_screenshots" } else { "tablet_10_screenshots" }
$shotDir = Join-Path $ProjectRoot "store_page\$folderName"
$rawDir = Join-Path $ProjectRoot "store_page\raw\$folderName"

function Require($condition, $message) {
    if (!$condition) { throw $message }
    Write-Output "[PASS] $message"
}

function DumpUi($name) {
    $remote = "/sdcard/$name.xml"
    $local = Join-Path "C:\tmp" "$name.xml"
    for ($attempt = 0; $attempt -lt 8; $attempt++) {
        Remove-Item -LiteralPath $local -Force -ErrorAction SilentlyContinue
        & $adb -s $Serial shell uiautomator dump $remote | Out-Null
        if ($LASTEXITCODE -eq 0) {
            & $adb -s $Serial pull $remote $local | Out-Null
            if ($LASTEXITCODE -eq 0 -and (Test-Path -LiteralPath $local)) {
                return Get-Content -LiteralPath $local -Raw
            }
        }
        Start-Sleep -Seconds 1
    }
    throw "Could not dump tablet UI: $name"
}

function TapText([string[]]$Labels, [string]$Name, [string]$Direction = "none", [int]$Attempts = 8) {
    for ($attempt = 0; $attempt -lt $Attempts; $attempt++) {
        $xml = DumpUi "pantrypilot_tablet_$Name"
        foreach ($label in $Labels) {
            $pattern = 'text="' + [regex]::Escape($label) + '".*?bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"'
            $match = [regex]::Match($xml, $pattern)
            if ($match.Success) {
                $x = [int](([int]$match.Groups[1].Value + [int]$match.Groups[3].Value) / 2)
                $y = [int](([int]$match.Groups[2].Value + [int]$match.Groups[4].Value) / 2)
                & $adb -s $Serial shell input tap $x $y | Out-Null
                Start-Sleep -Milliseconds 700
                return
            }
        }
        if ($Direction -eq "down") {
            & $adb -s $Serial shell input swipe 960 850 960 300 450 | Out-Null
        } elseif ($Direction -eq "up") {
            & $adb -s $Serial shell input swipe 960 300 960 850 450 | Out-Null
        }
        Start-Sleep -Milliseconds 400
    }
    throw "Could not find: $($Labels -join ', ')"
}

function OpenTab([string]$Label) {
    & $adb -s $Serial shell am force-stop $Package | Out-Null
    & $adb -s $Serial shell am start -n "$Package/$Activity" | Out-Null
    Start-Sleep -Milliseconds 800
    TapText @($Label) "tab_$Label"
}

function Capture([string]$Name) {
    $remote = "/sdcard/$Name.png"
    $raw = Join-Path $rawDir "$Name.png"
    $final = Join-Path $shotDir "$Name.png"
    & $adb -s $Serial shell screencap -p $remote | Out-Null
    Require ($LASTEXITCODE -eq 0) "Captured $Name"
    & $adb -s $Serial pull $remote $raw | Out-Null
    Require ($LASTEXITCODE -eq 0 -and (Test-Path -LiteralPath $raw)) "Pulled $Name"
    Copy-Item -LiteralPath $raw -Destination $final -Force
    Require (Test-Path -LiteralPath $final) "Wrote $Name"
}

$devices = & $adb devices
Require (($devices | Where-Object { $_ -match "^$([regex]::Escape($Serial))\s+device$" }) -ne $null) "Tablet emulator $Serial is connected"
Require (Test-Path -LiteralPath $Apk) "Debug APK exists"
New-Item -ItemType Directory -Force -Path $shotDir, $rawDir | Out-Null

$install = & $adb -s $Serial install -r $Apk
Require ($LASTEXITCODE -eq 0 -and ($install -join "`n") -match "Success") "Debug APK installs"
& $adb -s $Serial shell pm clear $Package | Out-Null
& $adb -s $Serial shell settings put system accelerometer_rotation 0 | Out-Null
& $adb -s $Serial shell settings put system user_rotation 1 | Out-Null
if ($TabletSize -eq "7-inch") {
    & $adb -s $Serial shell wm size 1280x720 | Out-Null
} else {
    & $adb -s $Serial shell wm size 1920x1080 | Out-Null
}
& $adb -s $Serial shell am start -n "$Package/$Activity" | Out-Null
Start-Sleep -Seconds 3

TapText @("Next", "NEXT") "tutorial_1"
TapText @("Next", "NEXT") "tutorial_2"
TapText @("Next", "NEXT") "tutorial_3"
TapText @("Start", "START") "tutorial_start"
OpenTab "Plans"
TapText @("Demo purchase Remove Ads") "remove_ads" "down"
TapText @("Unlock demo", "UNLOCK DEMO") "unlock"

OpenTab "Home"
Capture "01_home"
OpenTab "Scan"
TapText @("Try sample label") "sample_label"
Capture "02_scan_parser"
OpenTab "Pantry"
Capture "03_pantry"
OpenTab "Meals"
Capture "04_meals"

$verification = [ordered]@{
    captured_at_utc = (Get-Date).ToUniversalTime().ToString("o")
    tablet_size = $TabletSize
    serial = $Serial
    apk_sha256 = (Get-FileHash -LiteralPath $Apk -Algorithm SHA256).Hash
    screenshots = @()
}
foreach ($name in @("01_home", "02_scan_parser", "03_pantry", "04_meals")) {
    $path = Join-Path $shotDir "$name.png"
    $verification.screenshots += [ordered]@{
        file = "$folderName/$name.png"
        sha256 = (Get-FileHash -LiteralPath $path -Algorithm SHA256).Hash
    }
}
$verification | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath (Join-Path $shotDir "VERIFICATION.json") -Encoding ASCII
Write-Output "$TabletSize screenshots captured."
