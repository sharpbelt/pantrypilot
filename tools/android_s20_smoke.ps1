param(
    [string]$Serial = "R5CN30X6DDT",
    [string]$Apk = "C:\tmp\PantryPilot-debug.apk",
    [string]$Package = "app.pantrypilot.app",
    [string]$Activity = ".MainActivity",
    [string]$OutDir = "C:\tmp",
    [switch]$KeepData
)

$ErrorActionPreference = "Stop"
$failures = New-Object System.Collections.Generic.List[string]

function Pass($message) {
    Write-Output "[PASS] $message"
}

function Fail($message) {
    Write-Output "[FAIL] $message"
    $failures.Add($message) | Out-Null
}

function Require($condition, $message) {
    if ($condition) { Pass $message } else { Fail $message }
}

function DumpUi($name) {
    $remoteXml = "/sdcard/$name.xml"
    $localXml = Join-Path $OutDir "$name.xml"
    & $adb -s $Serial shell uiautomator dump $remoteXml | Out-Null
    Require ($LASTEXITCODE -eq 0) "UI hierarchy dump succeeds for $name"
    & $adb -s $Serial pull $remoteXml $localXml | Out-Null
    Require ($LASTEXITCODE -eq 0 -and (Test-Path -LiteralPath $localXml)) "UI hierarchy pulled to $localXml"
    return Get-Content -LiteralPath $localXml -Raw
}

function TapText($xml, [string[]]$labels) {
    foreach ($label in $labels) {
        $pattern = 'text="' + [regex]::Escape($label) + '".*?bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"'
        $match = [regex]::Match($xml, $pattern)
        if ($match.Success) {
            $x = [int](([int]$match.Groups[1].Value + [int]$match.Groups[3].Value) / 2)
            $y = [int](([int]$match.Groups[2].Value + [int]$match.Groups[4].Value) / 2)
            & $adb -s $Serial shell input tap $x $y | Out-Null
            Require ($LASTEXITCODE -eq 0) "Tapped $label"
            Start-Sleep -Milliseconds 500
            return $true
        }
    }
    return $false
}

function Finish() {
    if ($failures.Count -gt 0) {
        Write-Output ""
        Write-Output "S20 smoke test failed:"
        foreach ($failure in $failures) {
            Write-Output "- $failure"
        }
        exit 1
    }
    Write-Output ""
    Write-Output "S20 smoke test passed."
    exit 0
}

$adb = "adb"
$devices = & $adb devices
Require ($LASTEXITCODE -eq 0) "adb devices command succeeds"

$deviceLine = $devices | Where-Object { $_ -match "^$([regex]::Escape($Serial))\s+device$" }
if ($null -eq $deviceLine) {
    Fail "Required S20 serial $Serial is not connected as a device. Refusing to target any other Android device."
    Finish
}
Pass "S20 serial $Serial is connected"

Require (Test-Path -LiteralPath $Apk) "Debug APK exists at $Apk"

$installOutput = & $adb -s $Serial install -r $Apk
Require ($LASTEXITCODE -eq 0 -and ($installOutput -join "`n") -match "Success") "Debug APK installs on S20"

if (!$KeepData) {
    & $adb -s $Serial shell pm clear $Package | Out-Null
    Require ($LASTEXITCODE -eq 0) "PantryPilot app data cleared for first-run smoke"
}

$component = "$Package/$Activity"
& $adb -s $Serial shell am start -n $component | Out-Null
Require ($LASTEXITCODE -eq 0) "App launch command succeeds"

Start-Sleep -Seconds 1

$appPid = & $adb -s $Serial shell pidof $Package
Require ($LASTEXITCODE -eq 0 -and ![string]::IsNullOrWhiteSpace(($appPid -join ""))) "App process is running"

$remotePng = "/sdcard/pantrypilot_s20_smoke.png"
$localPng = Join-Path $OutDir "pantrypilot_s20_smoke.png"

if (!$KeepData) {
    $tutorialXml = DumpUi "pantrypilot_s20_tutorial"
    Require ($tutorialXml -match "Welcome to PantryPilot") "First-run tutorial appears"
    for ($i = 0; $i -lt 3; $i++) {
        Require (TapText $tutorialXml @("Next", "NEXT")) "Tutorial Next button is tappable"
        $tutorialXml = DumpUi "pantrypilot_s20_tutorial_$i"
    }
    Require (TapText $tutorialXml @("Start", "START")) "Tutorial Start button is tappable"
    Start-Sleep -Milliseconds 700
}

$xml = DumpUi "pantrypilot_s20_smoke"

& $adb -s $Serial shell screencap -p $remotePng | Out-Null
Require ($LASTEXITCODE -eq 0) "Screenshot capture succeeds"

& $adb -s $Serial pull $remotePng $localPng | Out-Null
Require ($LASTEXITCODE -eq 0 -and (Test-Path -LiteralPath $localPng)) "Screenshot pulled to $localPng"

foreach ($required in @("PantryPilot", "Home", "Scan", "Pantry", "Grocery", "Meals", "Plans", "Free plan", "Sponsored", "Remove ads")) {
    Require ($xml -match [regex]::Escape($required)) "Smoke UI contains $required"
}

foreach ($forbidden in @("Billing")) {
    Require ($xml -notmatch "\b$forbidden\b") "Smoke UI does not contain $forbidden"
}

Finish
