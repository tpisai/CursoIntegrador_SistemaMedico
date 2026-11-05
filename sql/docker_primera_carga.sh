#!/bin/bash
# Primera carga de la base en Docker. docker-compose.yml monta este archivo en
# /docker-entrypoint-initdb.d y la carpeta sql/ en /sql-inicial.
#
# No se monta sql/ directamente en docker-entrypoint-initdb.d porque la imagen ejecuta los archivos
# por orden alfabético: citas_salud.sql quedaría después de 02, 03... y fallaría. Aquí se sigue el
# orden de instalar_bd.ps1: primero citas_salud.sql y luego los scripts numerados.
#
# Va todo en una sola transacción. Si un script falla la base queda vacía y el backend no arranca,
# en vez de quedar a medias: la imagen no repite esta carga cuando el contenedor se reinicia.
#
# Solo corre la primera vez, con el volumen vacío. No usar "set -u": la imagen puede ejecutar este
# archivo dentro de su propio script y la opción afectaría al resto del arranque.
set -e

carpeta=/sql-inicial
archivos=()

for requerido in citas_salud.sql 02_correlativo_ticket.sql 03_datos_iniciales.sql; do
    if [ ! -f "$carpeta/$requerido" ]; then
        echo "Falta $carpeta/$requerido: no se puede cargar la base." >&2
        exit 1
    fi
done

for script in "$carpeta"/citas_salud.sql "$carpeta"/[0-9][0-9]_*.sql; do
    echo "Se cargará $(basename "$script")"
    archivos+=(-f "$script")
done

PGHOST= PGHOSTADDR= psql -v ON_ERROR_STOP=1 --single-transaction --quiet --no-password --no-psqlrc \
    --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" "${archivos[@]}"

echo "Primera carga terminada."
