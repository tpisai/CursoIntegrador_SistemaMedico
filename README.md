Version 0: Primer Commit de Git y configuracion del Git pull para seguridad y control de versiones.

## SaludGrau · estructura del repositorio

- `sql/` — script de la base de datos (`citas_salud.sql`), correlativo de tickets (`02_correlativo_ticket.sql`),
  datos iniciales (`03_datos_iniciales.sql`) e instalador (`instalar_bd.ps1`).
- `citas_salud/` — backend Spring Boot (API REST en `/api`).
- `frontend/` — sitio web en Next.js.

El reporte integral de citas del panel administrativo permite filtrar por periodo, especialidad, doctor y estado; muestra indicadores, evolución semanal/mensual, tablas y PDF Jasper con vista previa. La documentación detallada de rutas y endpoints está en [frontend/README.md](frontend/README.md). Las consultas actuales cubren estos filtros; quedan pendientes consultas adicionales si se amplía el análisis.

Los pasos para crear la base, arrancar el backend y el frontend, y las cuentas de prueba están en
[frontend/README.md](frontend/README.md).

Para publicar el sistema en un servidor y programar los respaldos, sigue [docs/DESPLIEGUE.md](docs/DESPLIEGUE.md).
