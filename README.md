Version 0: Primer Commit de Git y configuracion del Git pull para seguridad y control de versiones.

## SaludGrau · estructura del repositorio

- `sql/` — script de la base de datos (`citas_salud.sql`), correlativo de tickets (`02_correlativo_ticket.sql`),
  datos iniciales (`03_datos_iniciales.sql`) e instalador (`instalar_bd.ps1`).
- `citas_salud/` — backend Spring Boot (API REST en `/api`).
- `frontend/` — sitio web en Next.js.

Los pasos para crear la base, arrancar el backend y el frontend, y las cuentas de prueba están en
[frontend/README.md](frontend/README.md).
