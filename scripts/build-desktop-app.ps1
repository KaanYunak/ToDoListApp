param(
    [string]$IconPath = "",
    [switch]$ShortcutOnly,
    [switch]$NoShortcut
)

$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$buildScript = Join-Path $PSScriptRoot "build.ps1"
$dest = Join-Path $root "build\desktop-app"
$packageInput = Join-Path $root "build\package-input"
$appDir = Join-Path $dest "Sisifos"
$exe = Join-Path $appDir "Sisifos.exe"
$defaultData = Join-Path $env:USERPROFILE ".sisifos\data.json"
$legacyData = Join-Path $env:USERPROFILE ".todolistapp\data.json"
$bundledIconPath = Join-Path $root "sisifos.ico"
$iconPathFromSettings = $false

if ([string]::IsNullOrWhiteSpace($IconPath) -and -not (Test-Path $defaultData) -and (Test-Path $legacyData)) {
    $defaultData = $legacyData
}

if ([string]::IsNullOrWhiteSpace($IconPath) -and (Test-Path $defaultData)) {
    try {
        $data = Get-Content $defaultData -Raw | ConvertFrom-Json
        if ($data.settings.exeIconPath) {
            $IconPath = [string]$data.settings.exeIconPath
            $iconPathFromSettings = $true
        }
    } catch {
        Write-Warning "Ayar dosyasından icon okunamadı: $($_.Exception.Message)"
    }
}

if ([string]::IsNullOrWhiteSpace($IconPath) -and (Test-Path $bundledIconPath)) {
    $IconPath = $bundledIconPath
}

if (-not [string]::IsNullOrWhiteSpace($IconPath) -and -not (Test-Path $IconPath)) {
    if ($iconPathFromSettings) {
        Write-Warning "Ayar dosyasındaki icon bulunamadı, proje iconu kullanılacak: $IconPath"
        $IconPath = if (Test-Path $bundledIconPath) { $bundledIconPath } else { "" }
    } else {
        throw "Icon dosyası bulunamadı: $IconPath"
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
    $shortcutPath = Join-Path $desktop "Sisifos.lnk"
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

    $legacyShortcutPath = Join-Path $desktop "ToDoListApp.lnk"
    if (Test-Path $legacyShortcutPath) {
        $legacy = $shell.CreateShortcut($legacyShortcutPath)
        if ($legacy.TargetPath -like "*\ToDoListApp.exe" -or $legacy.TargetPath -like "*\desktop-app\ToDoListApp\*") {
            Remove-Item -LiteralPath $legacyShortcutPath -Force
            Write-Host "Eski ToDoListApp kısayolu kaldırıldı: $legacyShortcutPath"
        }
    }
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
if (Test-Path $packageInput) {
    Remove-Item -LiteralPath $packageInput -Recurse -Force
}
New-Item -ItemType Directory -Force -Path $dest | Out-Null
New-Item -ItemType Directory -Force -Path $packageInput | Out-Null
Copy-Item -LiteralPath (Join-Path $root "build\Sisifos.jar") -Destination $packageInput -Force

$args = @(
    "--type", "app-image",
    "--name", "Sisifos",
    "--input", $packageInput,
    "--main-jar", "Sisifos.jar",
    "--main-class", "com.kaanyunak.todolistapp.App",
    "--dest", $dest
)

if (-not [string]::IsNullOrWhiteSpace($IconPath)) {
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
