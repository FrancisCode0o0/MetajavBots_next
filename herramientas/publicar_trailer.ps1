# PUBLICAR EL TRÁILER — Lo hace Fran cuando haya elegido versión (las tres están en build\trailer\ y en
# C:\Users\Francis\git\archivo_para_eliminar\VIDEO_METAJAVBOTS\). Un solo comando, desde la carpeta del repositorio:
#
#   powershell -ExecutionPolicy Bypass -File herramientas\publicar_trailer.ps1 2
#
# (2 = trailer_2_modos; 1 = trailer_1_programar; 3 = trailer_3_epico). Hace esto:
#   1. Copia su GIF a docs\imagenes\trailer.gif y lo pone arriba del README, debajo del título, con el enlace al vídeo
#      completo en los tres idiomas (si ya había uno, lo cambia).
#   2. Hace commit de esos dos archivos y push de la rama actual.
#   3. Sube el MP4 como MetajavBots-trailer.mp4 a la Release «trailer» de GitHub (la crea, como versión previa, si no
#      está) y, si ya hay una versión publicada (v1.0…), también a esa. El vídeo no entra en el historial de git.
#      Las versiones que salgan luego (etiquetas v*) lo copian solas: .github/workflows/paquetes.yml.
# Para subir a GitHub usa la sesión que ya tiene git en este PC (el administrador de credenciales de Git).

param(
    [Parameter(Mandatory = $true)][ValidateSet('1', '2', '3')][string]$Version,
    [string]$Carpeta = ''
)

$ErrorActionPreference = 'Stop'
$repo = Split-Path -Parent $PSScriptRoot
Set-Location $repo
$nombres = @{ '1' = 'trailer_1_programar'; '2' = 'trailer_2_modos'; '3' = 'trailer_3_epico' }
$nombre = $nombres[$Version]
$sitios = @($Carpeta, (Join-Path $repo 'build\trailer'), 'C:\Users\Francis\git\archivo_para_eliminar\VIDEO_METAJAVBOTS') | Where-Object { $_ }
$origen = $sitios | Where-Object { Test-Path (Join-Path $_ "$nombre.mp4") } | Select-Object -First 1
if (-not $origen) { throw "No encuentro $nombre.mp4 (hazlo con: .\gradlew.bat lwjgl3:trailer)" }
$mp4 = Join-Path $origen "$nombre.mp4"
$gif = Join-Path $origen "$nombre.gif"
Write-Host "Publico $nombre (de $origen)"

# 1. El GIF arriba del README.
Copy-Item $gif (Join-Path $repo 'docs\imagenes\trailer.gif') -Force
$origenGit = (git config --get remote.origin.url) -replace '\.git$', ''
$rutaRepo = ($origenGit -replace '^https://github.com/', '') -replace '^git@github.com:', ''
$video = "https://github.com/$rutaRepo/releases/download/trailer/MetajavBots-trailer.mp4"
$bloque = @"
<!-- trailer -->
[![MetajavBots: tráiler · Trailer](docs/imagenes/trailer.gif)]($video)

**▶ [Ver el tráiler completo]($video)** · **[Den ganzen Trailer ansehen]($video)** · **[Watch the full trailer]($video)**
<!-- /trailer -->
"@
$utf8 = New-Object System.Text.UTF8Encoding($false)
$readme = [System.IO.File]::ReadAllText((Join-Path $repo 'README.md'), $utf8)
if ($readme -match '(?s)<!-- trailer -->.*?<!-- /trailer -->') {
    $readme = [regex]::Replace($readme, '(?s)<!-- trailer -->.*?<!-- /trailer -->', { param($m) $bloque })
} else {
    $readme = [regex]::Replace($readme, '^(# MetajavBots\r?\n)', { param($m) $m.Value + "`n" + $bloque + "`n" }, 'Multiline')
}
[System.IO.File]::WriteAllText((Join-Path $repo 'README.md'), $readme, $utf8)

# 2. Commit y push.
git add README.md docs/imagenes/trailer.gif
git commit -m "Tráiler en el README: $nombre"
if ($LASTEXITCODE -ne 0) { Write-Host '(nada nuevo que guardar en git)' }
git push
if ($LASTEXITCODE -ne 0) { throw 'git push falló' }

# 3. El MP4 a la Release.
$cred = "protocol=https`nhost=github.com`n`n" | git credential fill
$token = ($cred | Where-Object { $_ -like 'password=*' }) -replace '^password=', ''
if (-not $token) { throw 'git no tiene guardada una sesión de GitHub en este PC' }
$api = "https://api.github.com/repos/$rutaRepo"
$cabeceras = @{ Authorization = "Bearer $token"; Accept = 'application/vnd.github+json'; 'X-GitHub-Api-Version' = '2022-11-28' }

function Subir($release) {
    foreach ($a in $release.assets) {
        if ($a.name -eq 'MetajavBots-trailer.mp4') { Invoke-RestMethod -Method Delete -Uri "$api/releases/assets/$($a.id)" -Headers $cabeceras | Out-Null }
    }
    $subida = "https://uploads.github.com/repos/$rutaRepo/releases/$($release.id)/assets?name=MetajavBots-trailer.mp4"
    Invoke-RestMethod -Method Post -Uri $subida -Headers $cabeceras -ContentType 'video/mp4' -InFile $mp4 | Out-Null
    Write-Host "Subido a la Release $($release.tag_name)"
}

try {
    $trailer = Invoke-RestMethod -Uri "$api/releases/tags/trailer" -Headers $cabeceras
} catch {
    $cuerpo = @{ tag_name = 'trailer'; target_commitish = (git rev-parse HEAD).Trim(); name = 'Tráiler'; prerelease = $true
                 body = 'El tráiler de MetajavBots (MetajavBots-trailer.mp4): una partida de verdad, renderizada por el propio juego.' } | ConvertTo-Json
    $trailer = Invoke-RestMethod -Method Post -Uri "$api/releases" -Headers $cabeceras -ContentType 'application/json; charset=utf-8' -Body ([System.Text.Encoding]::UTF8.GetBytes($cuerpo))
}
Subir $trailer
try {
    $ultima = Invoke-RestMethod -Uri "$api/releases/latest" -Headers $cabeceras
    if ($ultima.tag_name -like 'v*') { Subir $ultima }
} catch {
    Write-Host 'Aún no hay ninguna versión publicada (v1.0…): cuando la haya, el flujo «Paquetes» le añade el tráiler solo.'
}
Write-Host "Hecho: $video"
