$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$src = Join-Path $root "src\main\java"
$build = Join-Path $root "build"
$classes = Join-Path $build "classes"
$sources = Join-Path $build "sources.txt"
$manifest = Join-Path $build "manifest.mf"
$jar = Join-Path $build "ToDoListApp.jar"

New-Item -ItemType Directory -Force -Path $classes | Out-Null
$sourceFiles = Get-ChildItem -Path $src -Recurse -Filter *.java | ForEach-Object {
    $_.FullName.Substring($root.Length + 1)
}
$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
[System.IO.File]::WriteAllLines($sources, $sourceFiles, $utf8NoBom)

Push-Location $root
try {
    & javac -encoding UTF-8 -d "build\classes" "@build\sources.txt"
    if ($LASTEXITCODE -ne 0) {
        throw "javac başarısız oldu. Çıkış kodu: $LASTEXITCODE"
    }

    [System.IO.File]::WriteAllText($manifest, "Main-Class: com.kaanyunak.todolistapp.App`n", [System.Text.Encoding]::ASCII)
    & jar cfm "build\ToDoListApp.jar" "build\manifest.mf" -C "build\classes" .
    if ($LASTEXITCODE -ne 0) {
        throw "jar oluşturulamadı. Çıkış kodu: $LASTEXITCODE"
    }
} finally {
    Pop-Location
}

Write-Host "Build tamamlandı: $jar"
