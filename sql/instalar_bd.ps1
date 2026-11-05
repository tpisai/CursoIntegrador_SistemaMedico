# Crea la base de datos centro_salud en tu PostgreSQL local y carga los scripts en orden:
#   1. citas_salud.sql         (tablas del equipo)
#   2. 02_correlativo_ticket.sql (correlativo del número de ticket)
#   3. 03_datos_iniciales.sql  (catálogos y datos de demostración)
#   4. 04_recuperacion_pin.sql (tabla de códigos para recuperar el PIN)
#   5. 05_profesionales_cmi.sql (profesionales del cartel del centro de salud)
#
# Uso (desde la carpeta del proyecto):
#   powershell -ExecutionPolicy Bypass -File sql\instalar_bd.ps1
#   powershell -ExecutionPolicy Bypass -File sql\instalar_bd.ps1 -Actualizar  # agrega lo nuevo sin borrar datos
#   powershell -ExecutionPolicy Bypass -File sql\instalar_bd.ps1 -Reiniciar   # borra y vuelve a crear la base
#
# La contraseña se lee de citas_salud/config/application.properties (archivo local ignorado por git).

param(
    [switch]$Reiniciar,
    [switch]$Actualizar,
    [string]$BaseDeDatos = "centro_salud",
    [string]$Usuario = "postgres",
    [string]$Servidor = "localhost",
    [string]$Psql = "C:\Program Files\PostgreSQL\17\bin\psql.exe"
)

$ErrorActionPreference = "Stop"
$raiz = Split-Path -Parent $PSScriptRoot
$configLocal = Join-Path $raiz "citas_salud\config\application.properties"

if (-not (Test-Path $Psql)) { throw "No se encontró psql en '$Psql'. Pasa la ruta con -Psql." }
if (-not (Test-Path $configLocal)) { throw "Falta $configLocal con la línea spring.datasource.password=..." }

$linea = Get-Content $configLocal | Where-Object { $_ -match '^\s*spring\.datasource\.password\s*=' } | Select-Object -First 1
$contrasena = if ($linea) { ($linea -split '=', 2)[1].Trim() } else { "" }
if ($contrasena -eq "" -or $contrasena -eq "ESCRIBE_AQUI_TU_CONTRASENA") {
    throw "Escribe tu contraseña de postgres en $configLocal y vuelve a ejecutar."
}

$env:PGPASSWORD = $contrasena
$env:PGCLIENTENCODING = "UTF8"

function Invoke-Psql([string]$db, [string[]]$argumentos) {
    & $Psql -h $Servidor -U $Usuario -d $db -v ON_ERROR_STOP=1 -q @argumentos
    if ($LASTEXITCODE -ne 0) { throw "psql terminó con error ($LASTEXITCODE)." }
}

$existe = & $Psql -h $Servidor -U $Usuario -d postgres -tAc "SELECT 1 FROM pg_database WHERE datname = '$BaseDeDatos'"
if ($LASTEXITCODE -ne 0) { throw "No se pudo conectar a PostgreSQL. Revisa la contraseña." }

# Estos scripts se pueden repetir sobre una base con datos: -Actualizar solo ejecuta estos.
$actualizaciones = "04_recuperacion_pin.sql", "05_profesionales_cmi.sql"
$scripts = @("citas_salud.sql", "02_correlativo_ticket.sql", "03_datos_iniciales.sql") + $actualizaciones

if ($existe -eq "1") {
    if ($Actualizar) {
        $scripts = $actualizaciones
    } elseif ($Reiniciar) {
        Write-Host "Borrando la base '$BaseDeDatos'..."
        Invoke-Psql "postgres" @("-c", "DROP DATABASE `"$BaseDeDatos`" WITH (FORCE)")
    } else {
        Write-Host "La base '$BaseDeDatos' ya existe. Usa -Actualizar para agregar lo nuevo sin borrar datos, o -Reiniciar para crearla de nuevo."
        exit 0
    }
}

if (-not ($existe -eq "1" -and $Actualizar)) {
    Write-Host "Creando la base '$BaseDeDatos'..."
    Invoke-Psql "postgres" @("-c", "CREATE DATABASE `"$BaseDeDatos`" ENCODING 'UTF8' TEMPLATE template0")
}

foreach ($script in $scripts) {
    Write-Host "Ejecutando $script..."
    Invoke-Psql $BaseDeDatos @("-f", (Join-Path $PSScriptRoot $script))
}

Write-Host "Listo. Registros en las tablas que llenan los formularios:"
$tablas = "usuario", "paciente", "doctor", "horario", "cita", "atencion", "documento_medico", "solicitud", "campania", "inscripcion_campania"
$conteo = ($tablas | ForEach-Object { "('$_', (SELECT count(*) FROM $_))" }) -join ", "
& $Psql -h $Servidor -U $Usuario -d $BaseDeDatos -q -P footer=off -c "SELECT tabla, registros FROM (VALUES $conteo) AS t(tabla, registros)"
Write-Host "Los horarios de los próximos 60 días los genera el backend al arrancar."
