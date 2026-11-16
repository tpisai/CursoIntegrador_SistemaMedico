#!/usr/bin/env bash
# Respaldo en el servidor con Docker: base de datos y documentos médicos en una carpeta con fecha,
# conservando las 7 más recientes. Es el equivalente de respaldar_bd.ps1 para el VPS.
#
# Uso (desde cualquier carpeta):
#   bash sql/respaldar_docker.sh                  # guarda en respaldos/ dentro del proyecto
#   bash sql/respaldar_docker.sh /ruta/destino
#   CONSERVAR=14 bash sql/respaldar_docker.sh
#
# La programación con cron y la restauración están en docs/DESPLIEGUE.md.
set -euo pipefail

# Los respaldos tienen datos de pacientes: solo los puede leer el dueño.
umask 077

cd "$(dirname "$0")/.."
destino="${1:-respaldos}"
conservar="${CONSERVAR:-7}"
fecha="$(date +%F_%H%M%S)"
temporal="$destino/$fecha.incompleto"

mkdir -p "$temporal"
trap 'rm -rf "$temporal"' EXIT

echo "Respaldando la base..."
docker compose exec -T postgres sh -c 'pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Fc' \
    > "$temporal/centro_salud.dump"

# Un respaldo que no se puede leer no sirve: si falla aquí no se borra ninguno de los anteriores.
docker compose exec -T postgres pg_restore --list < "$temporal/centro_salud.dump" > /dev/null

echo "Respaldando los documentos..."
docker compose exec -T backend tar -C /app/archivos -czf - . > "$temporal/archivos.tar.gz"

mv "$temporal" "$destino/$fecha"
echo "Respaldo listo: $destino/$fecha"

# Solo cuenta las carpetas terminadas y con este formato de nombre; nunca toca otros archivos.
ls -1dr "$destino"/[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]_[0-9][0-9][0-9][0-9][0-9][0-9] \
    | tail -n +"$((conservar + 1))" \
    | while read -r antiguo; do
        echo "Borrando respaldo antiguo: $antiguo"
        rm -rf "$antiguo"
    done
