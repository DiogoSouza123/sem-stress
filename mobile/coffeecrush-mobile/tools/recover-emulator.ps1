param(
    [Alias("AvdName")]
    [string]$PrimaryAvd = "Pixel_10_Pro",
    [string]$BackupAvd = "Pixel_10_Pro_Backup",
    [switch]$NoFallback
)

$ErrorActionPreference = "SilentlyContinue"

$sdkRoot = Join-Path $env:LOCALAPPDATA "Android\Sdk"
$emu = Join-Path $sdkRoot "emulator\emulator.exe"
$adb = Join-Path $sdkRoot "platform-tools\adb.exe"
$avdRoot = Join-Path $env:USERPROFILE ".android\avd"

function Get-AvdConfigPath([string]$AvdName) {
    Join-Path (Join-Path $avdRoot "$AvdName.avd") "config.ini"
}

function Stop-EmulatorStack {
    Get-Process | Where-Object { $_.ProcessName -match "emulator|qemu-system-x86_64|adb" } | Stop-Process -Force
    Start-Sleep -Seconds 2
}

function Clear-AvdLocks([string]$AvdName) {
    $avdDir = Join-Path $avdRoot "$AvdName.avd"
    if (Test-Path $avdDir) {
        Get-ChildItem $avdDir -Recurse -Force -Include "*.lock", "hardware-qemu.ini.lock", "multiinstance.lock" | Remove-Item -Force -Recurse
    }
}

function Apply-StableConfig([string]$AvdName) {
    $cfg = Get-AvdConfigPath $AvdName
    if (-not (Test-Path $cfg)) { return $false }

    $text = Get-Content $cfg -Raw
    $text = $text -replace "fastboot\.forceColdBoot=no", "fastboot.forceColdBoot=yes"
    $text = $text -replace "fastboot\.forceFastBoot=yes", "fastboot.forceFastBoot=no"

    if ($text -notmatch "fastboot\.forceColdBoot=") { $text += "`nfastboot.forceColdBoot=yes" }
    if ($text -notmatch "fastboot\.forceFastBoot=") { $text += "`nfastboot.forceFastBoot=no" }
    if ($text -notmatch "hw\.gpu\.mode=") {
        $text += "`nhw.gpu.mode=swiftshader_indirect"
    } else {
        $text = $text -replace "hw\.gpu\.mode=.*", "hw.gpu.mode=swiftshader_indirect"
    }

    $utf8NoBom = New-Object System.Text.UTF8Encoding($false)
    [System.IO.File]::WriteAllText($cfg, $text, $utf8NoBom)
    return $true
}

function Start-And-WaitAvd([string]$AvdName, [int]$TimeoutSeconds = 180) {
    & $adb kill-server | Out-Null
    & $adb start-server | Out-Null

    Start-Process -FilePath $emu -ArgumentList "-avd $AvdName -no-snapshot-load -no-snapshot-save -gpu swiftshader_indirect"

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        $out = (& $adb devices) -join "`n"
        if ($out -match "emulator-\d+\s+device") { return $true }
        Start-Sleep -Seconds 3
    }
    return $false
}

if (-not (Test-Path $emu)) { throw "Emulator nao encontrado em: $emu" }
if (-not (Test-Path $adb)) { throw "ADB nao encontrado em: $adb" }

$targets = @($PrimaryAvd)
if (-not $NoFallback -and $BackupAvd -and $BackupAvd -ne $PrimaryAvd) {
    $targets += $BackupAvd
}

foreach ($target in $targets) {
    $cfg = Get-AvdConfigPath $target
    if (-not (Test-Path $cfg)) {
        Write-Host "AVD '$target' nao encontrado. Pulando..."
        continue
    }

    Write-Host "Tentando subir AVD: $target"
    Write-Host "1) Encerrando processos travados..."
    Stop-EmulatorStack

    Write-Host "2) Limpando arquivos de lock..."
    Clear-AvdLocks $target

    Write-Host "3) Forcando config estavel..."
    [void](Apply-StableConfig $target)

    Write-Host "4) Abrindo AVD em modo seguro..."
    $ok = Start-And-WaitAvd -AvdName $target -TimeoutSeconds 180
    if ($ok) {
        Write-Host "OK: emulador online com '$target'."
        & $adb devices
        exit 0
    }

    Write-Host "Falha ao subir '$target'. Tentando proximo AVD (se houver)..."
}

Write-Host "Falha: nenhum AVD ficou online."
& $adb devices
exit 1
