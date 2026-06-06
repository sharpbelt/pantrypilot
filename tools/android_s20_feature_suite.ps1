param(
    [string]$Serial = "R5CN30X6DDT",
    [string]$Apk = "C:\tmp\PantryPilot-debug.apk",
    [string]$Package = "app.pantrypilot.app",
    [string]$Activity = ".MainActivity",
    [string]$OutDir = "C:\tmp"
)

$ErrorActionPreference = "Stop"
$adb = "adb"
$failures = New-Object System.Collections.Generic.List[string]

function Pass($message) { Write-Output "[PASS] $message" }
function Fail($message) { Write-Output "[FAIL] $message"; $failures.Add($message) | Out-Null }
function Require($condition, $message) { if ($condition) { Pass $message } else { Fail $message } }
function StopFail($message) { Fail $message; throw $message }

function Finish() {
    if ($failures.Count -gt 0) {
        Write-Output ""
        Write-Output "S20 feature suite failed:"
        foreach ($failure in $failures) { Write-Output "- $failure" }
        exit 1
    }
    Write-Output ""
    Write-Output "S20 feature suite passed."
    exit 0
}

function DumpUi($name = "pantrypilot_feature") {
    $remoteXml = "/sdcard/$name.xml"
    $localXml = Join-Path $OutDir "$name.xml"
    & $adb -s $Serial shell uiautomator dump $remoteXml | Out-Null
    Require ($LASTEXITCODE -eq 0) "UI dump succeeds for $name"
    & $adb -s $Serial pull $remoteXml $localXml | Out-Null
    Require ($LASTEXITCODE -eq 0 -and (Test-Path -LiteralPath $localXml)) "UI dump pulled for $name"
    return Get-Content -LiteralPath $localXml -Raw
}

function BoundsCenter($match) {
    $x = [int](([int]$match.Groups[1].Value + [int]$match.Groups[3].Value) / 2)
    $y = [int](([int]$match.Groups[2].Value + [int]$match.Groups[4].Value) / 2)
    return @($x, $y)
}

function FindTextMatch($xml, $label) {
    $pattern = 'text="' + [regex]::Escape($label) + '".*?bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"'
    foreach ($match in [regex]::Matches($xml, $pattern)) {
        if ($match.Groups[1].Value -ne "0" -or $match.Groups[2].Value -ne "0" -or
                $match.Groups[3].Value -ne "0" -or $match.Groups[4].Value -ne "0") {
            return $match
        }
    }
    return [regex]::Match("", "never-match")
}

function TapText([string[]]$labels, [string]$name = "tap", [string]$direction = "none", [int]$attempts = 5) {
    for ($attempt = 0; $attempt -lt $attempts; $attempt++) {
        $xml = DumpUi "pantrypilot_feature_$name"
        foreach ($label in $labels) {
            $match = FindTextMatch $xml $label
            if ($match.Success) {
                $xy = BoundsCenter $match
                & $adb -s $Serial shell input tap $xy[0] $xy[1] | Out-Null
                Require ($LASTEXITCODE -eq 0) "Tapped $label"
                Start-Sleep -Milliseconds 600
                return $true
            }
        }
        if ($direction -eq "down") {
            & $adb -s $Serial shell input touchscreen swipe 10 1200 10 500 500 | Out-Null
        } elseif ($direction -eq "up") {
            & $adb -s $Serial shell input touchscreen swipe 10 500 10 1200 500 | Out-Null
        }
        Start-Sleep -Milliseconds 450
    }
    StopFail "Could not tap any of: $($labels -join ', ')"
}

function AssertText($label, [string]$name = "assert", [string]$direction = "none", [int]$attempts = 5) {
    for ($attempt = 0; $attempt -lt $attempts; $attempt++) {
        $xml = DumpUi "pantrypilot_feature_$name"
        if ($xml -match [regex]::Escape($label)) {
            Pass "UI contains $label"
            return $xml
        }
        if ($direction -eq "down") {
            ScrollDown
        } elseif ($direction -eq "up") {
            ScrollUp
        }
    }
    StopFail "UI does not contain $label"
}

function TypeText($value) {
    $escaped = $value.Replace(" ", "%s")
    & $adb -s $Serial shell input text $escaped | Out-Null
    Require ($LASTEXITCODE -eq 0) "Typed $value"
    Start-Sleep -Milliseconds 300
}

function TapEditByIndex($index) {
    $xml = DumpUi "pantrypilot_feature_edit_$index"
    $matches = [regex]::Matches($xml, 'class="android\.widget\.EditText".*?bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"')
    if ($matches.Count -le $index) {
        StopFail "EditText index $index not found"
    }
    $xy = BoundsCenter $matches[$index]
    & $adb -s $Serial shell input tap $xy[0] $xy[1] | Out-Null
    Require ($LASTEXITCODE -eq 0) "Tapped EditText $index"
    Start-Sleep -Milliseconds 300
    return $true
}

function HideKeyboard() {
    & $adb -s $Serial shell input keyevent 4 | Out-Null
    Start-Sleep -Milliseconds 400
}

function FocusNextField() {
    & $adb -s $Serial shell input keyevent 61 | Out-Null
    Start-Sleep -Milliseconds 300
}

function TapTab([string]$label) {
    & $adb -s $Serial shell am force-stop $Package | Out-Null
    & $adb -s $Serial shell am start -n "$Package/$Activity" | Out-Null
    Start-Sleep -Milliseconds 800
    TapText @($label) "tab_$label" "none" 2 | Out-Null
}

function ScrollDown($times = 1) {
    for ($i = 0; $i -lt $times; $i++) {
        & $adb -s $Serial shell input touchscreen swipe 10 1200 10 500 600 | Out-Null
        Start-Sleep -Milliseconds 450
    }
}

function ScrollUp($times = 1) {
    for ($i = 0; $i -lt $times; $i++) {
        & $adb -s $Serial shell input touchscreen swipe 10 500 10 1200 600 | Out-Null
        Start-Sleep -Milliseconds 450
    }
}

$devices = & $adb devices
Require ($LASTEXITCODE -eq 0) "adb devices command succeeds"
Require (($devices | Where-Object { $_ -match "^$([regex]::Escape($Serial))\s+device$" }) -ne $null) "S20 serial $Serial is connected"
Require (Test-Path -LiteralPath $Apk) "Debug APK exists"

& $adb -s $Serial uninstall $Package | Out-Null
$installOutput = & $adb -s $Serial install -r $Apk
Require ($LASTEXITCODE -eq 0 -and ($installOutput -join "`n") -match "Success") "Debug APK installs"
& $adb -s $Serial shell pm clear $Package | Out-Null
Require ($LASTEXITCODE -eq 0) "App data cleared"
& $adb -s $Serial shell am start -n "$Package/$Activity" | Out-Null
Require ($LASTEXITCODE -eq 0) "App launched"
Start-Sleep -Seconds 1

$xml = DumpUi "pantrypilot_feature_tutorial"
Require ($xml -match "Welcome to PantryPilot") "First-run tutorial is visible"
TapText @("Next", "NEXT") "tutorial_next_1" | Out-Null
TapText @("Next", "NEXT") "tutorial_next_2" | Out-Null
TapText @("Next", "NEXT") "tutorial_next_3" | Out-Null
TapText @("Start", "START") "tutorial_start" | Out-Null
AssertText "Sponsored" "home_ad" | Out-Null
AssertText "Remove ads" "home_remove_ads" | Out-Null

TapTab "Pantry"
TapText @("Manual entry for loose pantry items") "expand_add_pantry" "down" 10 | Out-Null
AssertText "Pantry item name" "manual_add_expanded" "down" 3 | Out-Null
TapText @("Pantry item name") "manual_name" "down" 3 | Out-Null
TypeText "AutoSoup"
AssertText "AutoSoup" "manual_name_value" | Out-Null
FocusNextField
TypeText "2"
AssertText 'text="2"' "manual_quantity_value" | Out-Null
FocusNextField
FocusNextField
FocusNextField
TypeText "20261231"
AssertText "20261231" "manual_expiry_value" | Out-Null
FocusNextField
& $adb -s $Serial shell input keyevent 66 | Out-Null
Start-Sleep -Milliseconds 700
AssertText "Added AutoSoup." "manual_add_status" "up" 5 | Out-Null
TapTab "Pantry"
$xml = AssertText "AutoSoup" "manual_add_item" "down" 6
Require ($xml -match "2026-12-31") "Compact date is saved as 2026-12-31"

TapTab "Scan"
TapText @("Try sample label") "sample_label" "down" 5 | Out-Null
$xml = AssertText "Greek Yogurt" "scanner_sample_name" "down" 6
Require ($xml -match "500g") "Scanner sample quantity appears"
Require ($xml -match "2026-12-31") "Scanner sample expiry appears"

TapTab "Grocery"
TapEditByIndex 0 | Out-Null
TypeText "AutoApples"
HideKeyboard
TapText @("Add") "grocery_add" | Out-Null
AssertText "Added AutoApples to grocery list." "grocery_added" | Out-Null
TapEditByIndex 0 | Out-Null
TypeText "AutoApples"
HideKeyboard
TapText @("Add") "grocery_duplicate_add" | Out-Null
AssertText "AutoApples is already on the grocery list." "grocery_duplicate" | Out-Null
$xml = AssertText "AutoApples" "grocery_item" "down" 5
$box = [regex]::Match($xml, 'class="android\.widget\.CheckBox".*?bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"')
Require ($box.Success) "Grocery checkbox exists"
if ($box.Success) {
    $xy = BoundsCenter $box
    & $adb -s $Serial shell input tap $xy[0] $xy[1] | Out-Null
    Require ($LASTEXITCODE -eq 0) "Tapped grocery checkbox"
    Start-Sleep -Milliseconds 500
}
TapText @("Clear checked items") "clear_checked" "down" 4 | Out-Null
AssertText "Cleared 1 checked grocery item." "grocery_cleared" | Out-Null

TapTab "Meals"
AssertText "Meal Ideas" "meals_title" | Out-Null
AssertText "black beans" "meals_content" "down" 5 | Out-Null

TapTab "Plans"
AssertText "Remove Ads" "plans_remove_ads" | Out-Null
TapText @("Demo purchase Remove Ads") "remove_ads_demo" "down" 6 | Out-Null
AssertText "Demo purchase Remove Ads" "remove_ads_dialog" | Out-Null
TapText @("Unlock demo", "UNLOCK DEMO") "remove_ads_unlock" | Out-Null
AssertText "Demo Remove Ads purchase unlocked." "remove_ads_status" | Out-Null
$xml = DumpUi "pantrypilot_feature_after_remove_ads"
Require ($xml -notmatch 'text="Sponsored"') "Remove Ads hides Free banner"

TapText @("Reset ad-free demo") "reset_free" "up" 6 | Out-Null
AssertText "Demo ad-free entitlement reset." "reset_free_status" | Out-Null

Finish
