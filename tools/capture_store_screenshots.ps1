param(
    [string]$Serial = "R5CN30X6DDT",
    [string]$Apk = "C:\tmp\PantryPilot-debug.apk",
    [string]$Package = "app.pantrypilot.app",
    [string]$Activity = ".MainActivity",
    [string]$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path,
    [string]$Ffmpeg = "ffmpeg"
)

$ErrorActionPreference = "Stop"
$adb = "adb"
$rawDir = Join-Path $ProjectRoot "store_page\raw"
$shotDir = Join-Path $ProjectRoot "store_page\phone_screenshots"

function Require($condition, $message) {
    if (!$condition) { throw $message }
    Write-Output "[PASS] $message"
}

function DumpUi($name) {
    $remoteXml = "/sdcard/$name.xml"
    $localXml = Join-Path "C:\tmp" "$name.xml"
    & $adb -s $Serial shell uiautomator dump $remoteXml | Out-Null
    Require ($LASTEXITCODE -eq 0) "UI dump $name"
    & $adb -s $Serial pull $remoteXml $localXml | Out-Null
    Require ($LASTEXITCODE -eq 0 -and (Test-Path -LiteralPath $localXml)) "Pulled UI $name"
    return Get-Content -LiteralPath $localXml -Raw
}

function TapText([string[]]$labels) {
    for ($attempt = 0; $attempt -lt 3; $attempt++) {
        $xml = DumpUi "pantrypilot_store_nav"
        foreach ($label in $labels) {
            $pattern = 'text="' + [regex]::Escape($label) + '".*?bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"'
            $match = [regex]::Match($xml, $pattern)
            if ($match.Success) {
                $x = [int](([int]$match.Groups[1].Value + [int]$match.Groups[3].Value) / 2)
                $y = [int](([int]$match.Groups[2].Value + [int]$match.Groups[4].Value) / 2)
                & $adb -s $Serial shell input tap $x $y | Out-Null
                Require ($LASTEXITCODE -eq 0) "Tapped $label"
                Start-Sleep -Milliseconds 700
                return
            }
        }
        & $adb -s $Serial shell input swipe 540 500 540 1500 500 | Out-Null
        Start-Sleep -Milliseconds 500
    }
    throw "Could not find any of: $($labels -join ', ')"
}

function Capture($name) {
    $remotePng = "/sdcard/$name.png"
    $rawPng = Join-Path $rawDir "$name.png"
    $finalPng = Join-Path $shotDir "$name.png"
    & $adb -s $Serial shell screencap -p $remotePng | Out-Null
    Require ($LASTEXITCODE -eq 0) "Captured $name"
    & $adb -s $Serial pull $remotePng $rawPng | Out-Null
    Require ($LASTEXITCODE -eq 0 -and (Test-Path -LiteralPath $rawPng)) "Pulled $name raw"
    & $Ffmpeg -y -i $rawPng -vf "crop=iw:iw*2:0:0,scale=1080:2160" -frames:v 1 $finalPng | Out-Null
    Require ($LASTEXITCODE -eq 0 -and (Test-Path -LiteralPath $finalPng)) "Wrote Play screenshot $name"
}

$devices = & $adb devices
Require ($LASTEXITCODE -eq 0) "adb devices command succeeds"
Require (($devices | Where-Object { $_ -match "^$([regex]::Escape($Serial))\s+device$" }) -ne $null) "S20 serial $Serial is connected"
Require (Test-Path -LiteralPath $Apk) "Debug APK exists"
New-Item -ItemType Directory -Force -Path $rawDir, $shotDir | Out-Null

$installOutput = & $adb -s $Serial install -r $Apk
Require ($LASTEXITCODE -eq 0 -and ($installOutput -join "`n") -match "Success") "Debug APK installs"
& $adb -s $Serial shell pm clear $Package | Out-Null
Require ($LASTEXITCODE -eq 0) "App data cleared"
& $adb -s $Serial shell am start -n "$Package/$Activity" | Out-Null
Require ($LASTEXITCODE -eq 0) "App launched"
Start-Sleep -Seconds 1

TapText @("Next", "NEXT")
TapText @("Next", "NEXT")
TapText @("Next", "NEXT")
TapText @("Start", "START")

Capture "01_home"
TapText @("Scan")
Capture "02_scan"
TapText @("Test sample label")
Capture "03_scan_parser"
TapText @("Pantry")
Capture "04_pantry"
TapText @("Grocery")
Capture "05_grocery"
TapText @("Meals")
Capture "06_meals"
TapText @("Plans")
Capture "07_plans"
& $adb -s $Serial shell input swipe 540 1900 540 700 600 | Out-Null
Start-Sleep -Milliseconds 700
Capture "08_plans_products"

Write-Output "Store screenshots captured."
