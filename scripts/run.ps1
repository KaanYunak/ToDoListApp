$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$jar = Join-Path $root "build\ToDoListApp.jar"
$buildScript = Join-Path $PSScriptRoot "build.ps1"

if (-not (Test-Path $jar)) {
    & powershell.exe -NoProfile -ExecutionPolicy Bypass -File $buildScript
}

java -jar $jar
