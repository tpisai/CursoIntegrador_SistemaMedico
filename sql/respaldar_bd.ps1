# Respalda la base centro_salud con pg_dump en una carpeta con fecha y conserva las 7 más recientes.
# También copia los documentos médicos de citas_salud/archivos, porque la base solo guarda su ruta.
# Es para el PostgreSQL local de Windows; en el servidor con Docker se usa sql/respaldar_docker.sh.
#
# Uso (desde la carpeta del proyecto):
#   powershell -ExecutionPolicy Bypass -File sql\respaldar_bd.ps1
#   powershell -ExecutionPolicy Bypass -File sql\respaldar_bd.ps1 -Destino D:\respaldos -Conservar 14
#
# La contraseña se lee de citas_salud/config/application.properties (archivo local ignorado por git).
# Cada respaldo queda en respaldos\AAAA-MM-DD_HHMMSS\ con centro_salud.dump y la carpeta archivos\.
#
# Cómo restaurar. Primero en una base nueva, para revisar el respaldo sin tocar la actual:
#   $env:PGPASSWORD = "TU_CONTRASEÑA"
#   $bin = "C:\Program Files\PostgreSQL\17\bin"
#   & "$bin\createdb.exe" -h localhost -U postgres -T template0 -E UTF8 centro_salud_restaurada
#   & "$bin\pg_restore.exe" -h localhost -U postgres -d centro_salud_restaurada --no-owner respaldos\FECHA\centro_salud.dump
#
# Para reemplazar la base actual, detén antes el backend (se perderá lo registrado después del respaldo):
#   & "$bin\pg_restore.exe" -h localhost -U postgres -d centro_salud --clean --if-exists --no-owner respaldos\FECHA\centro_salud.dump
#   Copy-Item respaldos\FECHA\archivos\* citas_salud\archivos -Recurse -Force

param(
    [string]$Destino = "",
    [ValidateRange(1, 365)]
    [int]$Conservar = 7,
    [string]$BaseDeDatos = "centro_salud",
    [string]$Usuario = "postgres",
    [string]$Servidor = "localhost",
    [int]$Puerto = 5432,
    [string]$CarpetaPostgres = "C:\Program Files\PostgreSQL\17\bin"
)

$ErrorActionPreference = "Stop"
$raiz = Split-Path -Parent $PSScriptRoot
$configLocal = Join-Path $raiz "citas_salud\config\application.properties"
$carpetaArchivos = Join-Path $raiz "citas_salud\archivos"
$pgDump = Join-Path $CarpetaPostgres "pg_dump.exe"
$pgRestore = Join-Path $CarpetaPostgres "pg_restore.exe"
if ($Destino -eq "") { $Destino = Join-Path $raiz "respaldos" }

if (-not (Test-Path $pgDump)) { throw "No se encontró pg_dump en '$pgDump'. Pasa la carpeta con -CarpetaPostgres." }
if (-not (Test-Path $configLocal)) { throw "Falta $configLocal con la línea spring.datasource.password=..." }

$linea = Get-Content $configLocal | Where-Object { $_ -match '^\s*spring\.datasource\.password\s*=' } | Select-Object -First 1
$contrasena = if ($linea) { ($linea -split '=', 2)[1].Trim() } else { "" }
if ($contrasena -eq "" -or $contrasena -eq "ESCRIBE_AQUI_TU_CONTRASENA") {
    throw "Escribe tu contraseña de postgres en $configLocal y vuelve a ejecutar."
}

$fecha = Get-Date -Format "yyyy-MM-dd_HHmmss"
$final = Join-Path $Destino $fecha
# Se trabaja en una carpeta temporal: un respaldo fallido nunca cuenta como uno de los 7.
$temporal = "$final.incompleto"
New-Item -ItemType Directory -Force -Path $temporal | Out-Null

$env:PGPASSWORD = $contrasena
try {
    $dump = Join-Path $temporal "$BaseDeDatos.dump"

    Write-Host "Respaldando la base '$BaseDeDatos'..."
    & $pgDump -h $Servidor -p $Puerto -U $Usuario -d $BaseDeDatos -Fc -f $dump
    if ($LASTEXITCODE -ne 0) { throw "pg_dump terminó con error ($LASTEXITCODE)." }

    # Un respaldo que no se puede leer no sirve: se comprueba antes de borrar los anteriores.
    & $pgRestore --list $dump | Out-Null
    if ($LASTEXITCODE -ne 0) { throw "El respaldo no se puede leer con pg_restore ($LASTEXITCODE)." }

    if (Test-Path $carpetaArchivos) {
        Write-Host "Copiando los documentos médicos..."
        Copy-Item $carpetaArchivos (Join-Path $temporal "archivos") -Recurse
    }

    Rename-Item $temporal $final
} catch {
    Remove-Item $temporal -Recurse -Force -ErrorAction SilentlyContinue
    throw
} finally {
    Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue
}

Write-Host "Respaldo listo: $final"

# Solo cuenta las carpetas terminadas y con este formato de nombre; nunca toca otros archivos.
$antiguos = Get-ChildItem $Destino -Directory |
    Where-Object { $_.Name -match '^\d{4}-\d{2}-\d{2}_\d{6}$' } |
    Sort-Object Name -Descending |
    Select-Object -Skip $Conservar

foreach ($carpeta in $antiguos) {
    Write-Host "Borrando respaldo antiguo: $($carpeta.Name)"
    Remove-Item $carpeta.FullName -Recurse -Force
}
