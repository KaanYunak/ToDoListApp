$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$buildScript = Join-Path $PSScriptRoot "build.ps1"
$mainSrc = Join-Path $root "src\main\java"
$testSrc = Join-Path $root "src\test\java"
$testClasses = Join-Path $root "build\test-classes"
$testSources = Join-Path $root "build\test-sources.txt"

& powershell.exe -NoProfile -ExecutionPolicy Bypass -File $buildScript
if ($LASTEXITCODE -ne 0) {
    throw "Ana build başarısız oldu. Çıkış kodu: $LASTEXITCODE"
}

New-Item -ItemType Directory -Force -Path $testClasses | Out-Null
$sourceFiles = @()
$sourceFiles += Get-ChildItem -Path $mainSrc -Recurse -Filter *.java | ForEach-Object {
    $_.FullName.Substring($root.Length + 1)
}
$sourceFiles += Get-ChildItem -Path $testSrc -Recurse -Filter *.java | ForEach-Object {
    $_.FullName.Substring($root.Length + 1)
}
$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
[System.IO.File]::WriteAllLines($testSources, $sourceFiles, $utf8NoBom)

Push-Location $root
try {
    & javac -encoding UTF-8 -d "build/test-classes" "@build/test-sources.txt"
    if ($LASTEXITCODE -ne 0) {
        throw "Test derlemesi başarısız oldu. Çıkış kodu: $LASTEXITCODE"
    }

    & java -cp "build/test-classes" com.kaanyunak.todolistapp.SmokeTest
    if ($LASTEXITCODE -ne 0) {
        throw "Smoke test başarısız oldu. Çıkış kodu: $LASTEXITCODE"
    }
} finally {
    Pop-Location
}
