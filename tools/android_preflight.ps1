param(
    [string]$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path,
    [string]$Gradle = "C:\tmp\gradle-8.7\bin\gradle.bat",
    [switch]$SkipBuild
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

function Warn($message) {
    Write-Output "[WARN] $message"
}

function Require($condition, $message) {
    if ($condition) { Pass $message } else { Fail $message }
}

function ReadText($path) {
    return Get-Content -LiteralPath $path -Raw
}

$appBuild = Join-Path $ProjectRoot "app\build.gradle"
$manifest = Join-Path $ProjectRoot "app\src\main\AndroidManifest.xml"
$mainActivity = Join-Path $ProjectRoot "app\src\main\java\app\pantrypilot\app\MainActivity.java"
$rules = Join-Path $ProjectRoot "app\src\main\java\app\pantrypilot\app\PantryRules.java"
$rulesTest = Join-Path $ProjectRoot "tools\PantryRulesSelfTest.java"
$testOut = Join-Path $ProjectRoot "tools\out"

Require (Test-Path -LiteralPath $appBuild) "Android Gradle file exists"
Require (Test-Path -LiteralPath $manifest) "Android manifest exists"
Require (Test-Path -LiteralPath $mainActivity) "MainActivity source exists"
Require (Test-Path -LiteralPath $rules) "PantryRules source exists"
Require (Test-Path -LiteralPath $rulesTest) "PantryRules self-test source exists"
Require (!(Test-Path -LiteralPath (Join-Path $ProjectRoot "ios"))) "iOS target folder is absent"

$gradleText = ReadText $appBuild
Require ($gradleText -match 'namespace\s+"app\.pantrypilot\.app"') "Namespace is app.pantrypilot.app"
Require ($gradleText -match 'applicationId\s+"app\.pantrypilot\.app"') "Application ID is app.pantrypilot.app"
Require ($gradleText -match 'targetSdk\s+35') "Target SDK is 35"
Require ($gradleText -match 'versionCode\s+102') "Version code is 102"
Require ($gradleText -match 'versionName\s+"1\.0\.2"') "Version name is 1.0.2"
Require ($gradleText -match 'debuggable\s+false') "Release build is not debuggable"

$keystoreProperties = Join-Path $ProjectRoot "keystore.properties"
$hasKeystoreProperties = Test-Path -LiteralPath $keystoreProperties
if ($hasKeystoreProperties) {
    $keyText = ReadText $keystoreProperties
    Require ($keyText -match 'storeFile\s*=\s*.+' -and $keyText -notmatch 'replace-with') "Release upload keystore properties are configured"
} else {
    Warn "No keystore.properties found; artifacts are local/prod-test builds, not Play-upload signed with your upload key"
}

$manifestText = ReadText $manifest
Require ($manifestText -match 'android.permission.INTERNET') "Internet permission is declared for AdMob"
Require ($manifestText -match 'android.permission.ACCESS_NETWORK_STATE') "Network-state permission is declared for AdMob"
Require ($manifestText -match 'com.google.android.gms.ads.APPLICATION_ID') "AdMob application ID metadata is declared"
Require ($manifestText -match 'android:allowBackup="false"') "Android backup is disabled"
Require ($manifestText -match 'android:usesCleartextTraffic="false"') "Cleartext traffic is disabled"
Require ($manifestText -match 'android:exported="true"') "Launcher activity explicitly declares exported=true"
Require ($manifestText -match 'android:exported="false"') "Image provider explicitly declares exported=false"

$srcRoot = Join-Path $ProjectRoot "app\src\main"
$forbiddenPermissionTerms = @(
    "ACCESS_FINE_LOCATION",
    "ACCESS_COARSE_LOCATION",
    "ACCESS_BACKGROUND_LOCATION",
    "POST_NOTIFICATIONS",
    "com.android.vending.BILLING",
    "READ_EXTERNAL_STORAGE",
    "WRITE_EXTERNAL_STORAGE"
)
foreach ($term in $forbiddenPermissionTerms) {
    $hit = Get-ChildItem -LiteralPath $srcRoot -Recurse -File | Select-String -Pattern $term -ErrorAction SilentlyContinue
    Require ($null -eq $hit) "No $term permission/reference in Android source"
}

$mainText = ReadText $mainActivity
$rulesText = ReadText $rules
Require ($rulesText -match 'PLAN_FREE\s*=\s*"Free"' -and $rulesText -match 'PLAN_PLUS\s*=\s*"Plus"' -and $rulesText -match 'PLAN_PRO\s*=\s*"Pro"') "Free/Plus/Pro plan constants exist"
Require ($rulesText -match 'pantrypilot_remove_ads_one_time' -and $rulesText -match 'pantrypilot_plus_one_time' -and $rulesText -match 'pantrypilot_pro_one_time') "Play product IDs are declared"
Require ($mainText -match 'Demo purchase' -and $mainText -match 'does not charge money') "Demo purchase flow is clearly marked as non-charging"
Require ($mainText -match 'requirePlan\("Photo import review", PantryRules\.PLAN_PLUS\)' -and $mainText -match 'requirePlan\("Camera review", PantryRules\.PLAN_PLUS\)') "Photo review is Plus gated"
Require ($mainText -match 'requirePlan\("Label text parsing", PantryRules\.PLAN_PLUS\)' -and $mainText -match 'parseLabelText') "Label text parser is Plus gated"
Require ($mainText -match 'requirePlan\("Pantry sharing", PantryRules\.PLAN_PLUS\)' -and $mainText -match 'requirePlan\("Grocery sharing", PantryRules\.PLAN_PLUS\)') "Sharing is Plus gated"
Require ($mainText -match 'requirePlan\("Meal extras", PantryRules\.PLAN_PRO\)') "Meal extras are Pro gated"
Require ($mainText -match 'canAddPantryOrShowUpgrade' -and $mainText -match 'canAddGroceryOrShowUpgrade') "Pantry and grocery limits are enforced"
Require ($rulesText -match 'normalizeDateInput' -and $mainText -match 'dateField\("Expiry YYYY-MM-DD"\)' -and $mainText -match 'DigitsKeyListener\.getInstance\("0123456789-"\)') "Expiry input accepts hyphen-capable numeric dates"
Require ($mainText -match 'PREF_TUTORIAL_SEEN' -and $mainText -match 'showTutorialStep' -and $mainText -match 'Welcome to PantryPilot') "First-run tutorial is implemented"
Require ($mainText -match 'freeAdBanner' -and $mainText -match 'Remove ads' -and $mainText -match 'PantryRules\.PLAN_FREE') "Free-only ad surface is implemented"
Require ($mainText -match 'if \(adsRemoved\(\)\) return null;' -and $mainText -match 'if \(!PantryRules\.PLAN_FREE\.equals\(currentPlan\)\) return null;') "Paid and ad-free users cannot receive the Free banner"
Require ($mainText -match 'confirmDemoRemoveAds' -and $mainText -match 'PREF_AD_FREE' -and $mainText -match 'PantryRules\.REMOVE_ADS_PRODUCT_ID') "Standalone remove-ads entitlement is implemented"
Require ($gradleText -match 'play-services-ads:24\.9\.0' -and $gradleText -match 'user-messaging-platform:4\.0\.0') "Current Mobile Ads and UMP SDKs are integrated"
Require ($gradleText -match '3940256099942544/9214589741' -and $gradleText -match 'buildConfigField\s+"String",\s+"ADMOB_BANNER_ID"') "Debug builds use Google's test banner unit"
Require ($mainText -match 'requestAdConsent' -and $mainText -match 'loadAndShowConsentFormIfRequired' -and $mainText -match 'canRequestAds') "Ad requests are gated by UMP consent"
Require ($mainText -match 'getCurrentOrientationAnchoredAdaptiveBannerAdSize' -and $mainText -match 'new AdRequest\.Builder\(\)') "Adaptive AdMob banner loading is implemented"
Require ($mainText -match 'BILLING_MODE_DEMO' -and $mainText -match 'BILLING_MODE_PRODUCTION' -and $mainText -match 'isProductionBillingConfigured') "Billing demo/production activation gate exists"

$javac = "C:\Program Files\Java\jdk-21\bin\javac.exe"
$java = "C:\Program Files\Java\jdk-21\bin\java.exe"
if (!(Test-Path -LiteralPath $javac)) { $javac = "javac" }
if (!(Test-Path -LiteralPath $java)) { $java = "java" }

New-Item -ItemType Directory -Force -Path $testOut | Out-Null
& $javac -d $testOut $rules $rulesTest
Require ($LASTEXITCODE -eq 0) "PantryRules self-test compiles"
& $java -cp $testOut app.pantrypilot.app.PantryRulesSelfTest
Require ($LASTEXITCODE -eq 0) "PantryRules self-test passes"

if (!$SkipBuild) {
    Require (Test-Path -LiteralPath $Gradle) "Gradle executable exists"
    $gradleNetworkMode = @()
    if (Test-Path -LiteralPath "C:\tmp\pantrypilot-maven") {
        $gradleNetworkMode = @("--offline")
    }
    & $Gradle @gradleNetworkMode assembleDebug assembleRelease bundleRelease -x lintVitalAnalyzeRelease -x lintVitalReportRelease -x lintVitalRelease
    Require ($LASTEXITCODE -eq 0) "Android debug/release APK and AAB build succeeds"
}

$debugApk = Join-Path $ProjectRoot "app\build\outputs\apk\debug\app-debug.apk"
$signedReleaseApk = Join-Path $ProjectRoot "app\build\outputs\apk\release\app-release.apk"
$unsignedReleaseApk = Join-Path $ProjectRoot "app\build\outputs\apk\release\app-release-unsigned.apk"
$releaseApk = if ($hasKeystoreProperties) { $signedReleaseApk } else { $unsignedReleaseApk }
$releaseAab = Join-Path $ProjectRoot "app\build\outputs\bundle\release\app-release.aab"
Require (Test-Path -LiteralPath $debugApk) "Debug APK exists"
Require (Test-Path -LiteralPath $releaseApk) "Release APK exists"
Require (Test-Path -LiteralPath $releaseAab) "Release AAB exists"

$copiedDebug = "C:\tmp\PantryPilot-debug.apk"
$copiedReleaseApk = if ($hasKeystoreProperties) { "C:\tmp\PantryPilot-release.apk" } else { "C:\tmp\PantryPilot-release-unsigned.apk" }
$copiedReleaseAab = "C:\tmp\PantryPilot-release.aab"
if (Test-Path -LiteralPath $debugApk) { Copy-Item -LiteralPath $debugApk -Destination $copiedDebug -Force }
if (Test-Path -LiteralPath $releaseApk) { Copy-Item -LiteralPath $releaseApk -Destination $copiedReleaseApk -Force }
if (Test-Path -LiteralPath $releaseAab) { Copy-Item -LiteralPath $releaseAab -Destination $copiedReleaseAab -Force }
Require (Test-Path -LiteralPath $copiedDebug) "Copied debug APK exists"
Require (Test-Path -LiteralPath $copiedReleaseApk) "Copied release APK exists"
Require (Test-Path -LiteralPath $copiedReleaseAab) "Copied release AAB exists"

if ($failures.Count -gt 0) {
    Write-Output ""
    Write-Output "Android preflight failed:"
    foreach ($failure in $failures) {
        Write-Output "- $failure"
    }
    exit 1
}

Write-Output ""
Write-Output "Android preflight passed."
exit 0
