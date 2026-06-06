param(
    [string]$Destination = "C:\tmp\pantrypilot-maven"
)

$ErrorActionPreference = "Stop"
$seen = New-Object "System.Collections.Generic.HashSet[string]"
$repositories = @(
    "https://dl.google.com/dl/android/maven2",
    "https://repo.maven.apache.org/maven2"
)
Add-Type -AssemblyName System.IO.Compression.FileSystem

function ChildText($node, $name) {
    $child = $node.SelectSingleNode("*[local-name()='$name']")
    if ($null -eq $child) { return "" }
    return $child.InnerText.Trim()
}

function NormalizeVersion($version, $properties) {
    $value = $version.Trim()
    foreach ($key in $properties.Keys) {
        $value = $value.Replace('${' + $key + '}', [string]$properties[$key])
    }
    if ($value.StartsWith("[") -and $value.EndsWith("]")) {
        $value = $value.Trim('[', ']')
    }
    if ($value.Contains(",")) {
        $value = ($value.Split(",") | Where-Object { $_.Trim() } | Select-Object -First 1).Trim()
    }
    return $value
}

function DownloadFile($relativePath, $outputPath) {
    function TestDownloadedFile($path) {
        if (!(Test-Path -LiteralPath $path)) { return $false }
        if ($path -notmatch '\.(jar|aar)(\.part)?$') { return $true }
        try {
            $archive = [System.IO.Compression.ZipFile]::OpenRead($path)
            $archive.Dispose()
            return $true
        } catch {
            return $false
        }
    }

    if (TestDownloadedFile $outputPath) { return $true }
    if (Test-Path -LiteralPath $outputPath) {
        Remove-Item -LiteralPath $outputPath -Force
    }
    New-Item -ItemType Directory -Force -Path (Split-Path -Parent $outputPath) | Out-Null
    $temporaryPath = "$outputPath.part"
    foreach ($repository in $repositories) {
        try {
            if (Test-Path -LiteralPath $temporaryPath) {
                Remove-Item -LiteralPath $temporaryPath -Force
            }
            Invoke-WebRequest -Uri "$repository/$relativePath" -OutFile $temporaryPath
            if (!(TestDownloadedFile $temporaryPath)) {
                throw "Downloaded artifact is incomplete or corrupt: $relativePath"
            }
            Move-Item -LiteralPath $temporaryPath -Destination $outputPath -Force
            return TestDownloadedFile $outputPath
        } catch {
            if (Test-Path -LiteralPath $temporaryPath) {
                Remove-Item -LiteralPath $temporaryPath -Force
            }
        }
    }
    return $false
}

function MirrorArtifact($group, $artifact, $version, $requestedType = "") {
    $version = NormalizeVersion $version @{}
    if (!$group -or !$artifact -or !$version -or $version.Contains('$')) { return }
    $key = "$group`:$artifact`:$version"
    if (!$seen.Add($key)) { return }

    $groupPath = $group.Replace(".", "/")
    $relativeBase = "$groupPath/$artifact/$version"
    $localBase = Join-Path $Destination ($relativeBase.Replace("/", "\"))
    $pomName = "$artifact-$version.pom"
    $pomPath = Join-Path $localBase $pomName
    if (!(DownloadFile "$relativeBase/$pomName" $pomPath)) {
        Write-Warning "Could not mirror POM for $key"
        return
    }

    [xml]$pom = Get-Content -LiteralPath $pomPath -Raw
    $properties = @{}
    $projectVersion = ChildText $pom.project "version"
    if (!$projectVersion) { $projectVersion = $version }
    $properties["project.version"] = $projectVersion
    $properties["pom.version"] = $projectVersion
    $propertiesNode = $pom.SelectSingleNode("/*[local-name()='project']/*[local-name()='properties']")
    if ($null -ne $propertiesNode) {
        foreach ($property in $propertiesNode.ChildNodes) {
            if ($property.NodeType -eq "Element") {
                $properties[$property.LocalName] = $property.InnerText.Trim()
            }
        }
    }

    $parent = $pom.SelectSingleNode("/*[local-name()='project']/*[local-name()='parent']")
    if ($null -ne $parent) {
        MirrorArtifact `
            (ChildText $parent "groupId") `
            (ChildText $parent "artifactId") `
            (NormalizeVersion (ChildText $parent "version") $properties) `
            "pom"
    }

    foreach ($managed in $pom.SelectNodes("/*[local-name()='project']/*[local-name()='dependencyManagement']/*[local-name()='dependencies']/*[local-name()='dependency']")) {
        if ((ChildText $managed "scope") -eq "import" -or (ChildText $managed "type") -eq "pom") {
            MirrorArtifact `
                (ChildText $managed "groupId") `
                (ChildText $managed "artifactId") `
                (NormalizeVersion (ChildText $managed "version") $properties) `
                "pom"
        }
    }

    $packaging = ChildText $pom.project "packaging"
    if (!$packaging) { $packaging = if ($requestedType) { $requestedType } else { "jar" } }
    if ($packaging -ne "pom") {
        $extension = if ($packaging -eq "bundle") { "jar" } else { $packaging }
        if ($extension -eq "bundle") { $extension = "jar" }
        $artifactName = "$artifact-$version.$extension"
        if (!(DownloadFile "$relativeBase/$artifactName" (Join-Path $localBase $artifactName))) {
            Write-Warning "Could not mirror artifact for $key ($extension)"
        }
    }

    foreach ($dependency in $pom.SelectNodes("/*[local-name()='project']/*[local-name()='dependencies']/*[local-name()='dependency']")) {
        $scope = ChildText $dependency "scope"
        $optional = ChildText $dependency "optional"
        if ($scope -in @("test", "provided", "system") -or $optional -eq "true") { continue }
        $dependencyVersion = NormalizeVersion (ChildText $dependency "version") $properties
        MirrorArtifact `
            (ChildText $dependency "groupId") `
            (ChildText $dependency "artifactId") `
            $dependencyVersion `
            (ChildText $dependency "type")
    }
}

New-Item -ItemType Directory -Force -Path $Destination | Out-Null
MirrorArtifact "com.google.android.gms" "play-services-ads" "24.9.0" "aar"
MirrorArtifact "com.google.android.ump" "user-messaging-platform" "4.0.0" "aar"
MirrorArtifact "com.android.billingclient" "billing" "9.0.0" "aar"
MirrorArtifact "com.android.tools.lint" "lint-gradle" "31.5.2" "jar"
MirrorArtifact "commons-codec" "commons-codec" "1.10" "jar"
MirrorArtifact "org.checkerframework" "checker-qual" "3.12.0" "jar"
MirrorArtifact "com.google.errorprone" "error_prone_annotations" "2.11.0" "jar"
MirrorArtifact "com.google.j2objc" "j2objc-annotations" "1.3" "jar"
MirrorArtifact "com.google.code.findbugs" "jsr305" "3.0.2" "jar"
Write-Output "Mirrored $($seen.Count) Maven modules to $Destination"
