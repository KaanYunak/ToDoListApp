param(
    [string]$IconPath = "",
    [switch]$ShortcutOnly,
    [switch]$NoShortcut
)

$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$buildScript = Join-Path $PSScriptRoot "build.ps1"
$dest = Join-Path $root "build\desktop"
$appDir = Join-Path $dest "ToDoListApp"
$exe = Join-Path $appDir "ToDoListApp.exe"
$defaultData = Join-Path $env:USERPROFILE ".todolistapp\data.json"

if ([string]::IsNullOrWhiteSpace($IconPath) -and (Test-Path $defaultData)) {
    try {
        $data = Get-Content $defaultData -Raw | ConvertFrom-Json
        if ($data.settings.exeIconPath) {
            $IconPath = [string]$data.settings.exeIconPath
        }
    } catch {
        Write-Warning "Ayar dosyasından icon okunamadı: $($_.Exception.Message)"
    }
}

$resolvedRoot = [System.IO.Path]::GetFullPath($root)
$resolvedAppDir = [System.IO.Path]::GetFullPath($appDir)
if (-not $resolvedAppDir.StartsWith($resolvedRoot, [System.StringComparison]::OrdinalIgnoreCase)) {
    throw "Güvenlik kontrolü başarısız: app dizini proje dışında görünüyor."
}

function New-DesktopShortcut {
    param(
        [string]$TargetExe,
        [string]$WorkingDirectory,
        [string]$Icon
    )

    $desktop = [Environment]::GetFolderPath("Desktop")
    $shortcutPath = Join-Path $desktop "ToDoListApp.lnk"
    $shell = New-Object -ComObject WScript.Shell
    $shortcut = $shell.CreateShortcut($shortcutPath)
    $shortcut.TargetPath = $TargetExe
    $shortcut.WorkingDirectory = $WorkingDirectory
    if (-not [string]::IsNullOrWhiteSpace($Icon) -and (Test-Path $Icon)) {
        $shortcut.IconLocation = $Icon
    } else {
        $shortcut.IconLocation = $TargetExe
    }
    $shortcut.Save()
    Write-Host "Masaüstü kısayolu oluşturuldu: $shortcutPath"
}

if ($ShortcutOnly) {
    if (-not (Test-Path $exe)) {
        throw "ShortcutOnly için önce app-image üretilmeli: $exe"
    }
    if (-not $NoShortcut) {
        New-DesktopShortcut -TargetExe $exe -WorkingDirectory $appDir -Icon $IconPath
    }
    Write-Host "Desktop uygulaması hazır: $exe"
    exit 0
}

& powershell.exe -NoProfile -ExecutionPolicy Bypass -File $buildScript
if ($LASTEXITCODE -ne 0) {
    throw "Build başarısız oldu. Çıkış kodu: $LASTEXITCODE"
}

if (Test-Path $appDir) {
    Remove-Item -LiteralPath $appDir -Recurse -Force
}
New-Item -ItemType Directory -Force -Path $dest | Out-Null

$args = @(
    "--type", "app-image",
    "--name", "ToDoListApp",
    "--input", (Join-Path $root "build"),
    "--main-jar", "ToDoListApp.jar",
    "--main-class", "com.kaanyunak.todolistapp.App",
    "--dest", $dest
)

if (-not [string]::IsNullOrWhiteSpace($IconPath)) {
    if (-not (Test-Path $IconPath)) {
        throw "Icon dosyası bulunamadı: $IconPath"
    }
    $args += @("--icon", $IconPath)
}

& jpackage @args
if ($LASTEXITCODE -ne 0) {
    throw "jpackage başarısız oldu. Çıkış kodu: $LASTEXITCODE"
}

if (-not (Test-Path $exe)) {
    throw "Exe üretilemedi: $exe"
}

if (-not $NoShortcut) {
    New-DesktopShortcut -TargetExe $exe -WorkingDirectory $appDir -Icon $IconPath
}

Write-Host "Desktop uygulaması hazır: $exe"
