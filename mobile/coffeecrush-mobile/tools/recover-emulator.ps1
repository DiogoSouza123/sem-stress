param(
    [Alias("AvdName")]
    [string]$PrimaryAvd = "Pixel_10_Pro",
    [string]$BackupAvd = "Pixel_10_Pro_Backup",
    [switch]$NoFallback,
    [switch]$Watch,
    [int]$WatchIntervalSeconds = 10
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

function Test-QemuAlive {
    return $null -ne (Get-Process -Name "qemu-system-x86_64" -ErrorAction SilentlyContinue)
}

<#
 .SYNOPSIS
 Mantem a conexao ADB de pe apos o AVD ja estar online.

 .DESCRIPTION
 O adb.exe deste SDK trava esporadicamente com STATUS_STACK_BUFFER_OVERRUN dentro de
 ucrtbase.dll (bug reproduzido de forma deterministica neste ambiente - ja confirmado que
 nao e por versao desatualizada, o platform-tools instalado e identico ao mais recente do
 Google). O Android Studio so ve o dispositivo desconectar ("device offline"/EOF) e nao se
 recupera solo. O processo qemu-system-x86_64 (o emulador em si) normalmente sobrevive a
 essa queda do adb - entao, na maioria dos casos, basta reiniciar o servidor adb, sem
 descartar o estado do emulador (o que o resto do script faz, ao custo de reiniciar tudo).
#>
function Watch-AvdConnection([string]$AvdName, [int]$IntervalSeconds) {
    Write-Host ""
    Write-Host "Modo -Watch ativo: verificando a conexao ADB a cada $IntervalSeconds s (Ctrl+C para sair)."
    while ($true) {
        Start-Sleep -Seconds $IntervalSeconds

        if (-not (Test-QemuAlive)) {
            Write-Host "$(Get-Date -Format 'HH:mm:ss') - o processo do emulador caiu. Reabrindo o AVD '$AvdName'..."
            Stop-EmulatorStack
            Clear-AvdLocks $AvdName
            [void](Apply-StableConfig $AvdName)
            $ok = Start-And-WaitAvd -AvdName $AvdName -TimeoutSeconds 180
            Write-Host $(if ($ok) { "OK: AVD reaberto." } else { "Falha ao reabrir o AVD - tentando de novo no proximo ciclo." })
            continue
        }

        $out = (& $adb devices) -join "`n"
        if ($out -notmatch "emulator-\d+\s+device") {
            Write-Host "$(Get-Date -Format 'HH:mm:ss') - adb perdeu o dispositivo (emulador ainda de pe). Reiniciando so o servidor adb..."
            & $adb kill-server | Out-Null
            Start-Sleep -Seconds 1
            & $adb start-server | Out-Null
            Start-Sleep -Seconds 2
            & $adb devices
        }
    }
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
        if ($Watch) {
            Watch-AvdConnection -AvdName $target -IntervalSeconds $WatchIntervalSeconds
        }
        exit 0
    }

    Write-Host "Falha ao subir '$target'. Tentando proximo AVD (se houver)..."
}

Write-Host "Falha: nenhum AVD ficou online."
& $adb devices
exit 1
