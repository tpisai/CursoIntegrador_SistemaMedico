# SaludGrau · Frontend

Frontend del sistema de citas médicas del **Centro de Salud Miguel Grau** (Chaclacayo).
Stack: **Next.js 16** (App Router) + **TypeScript** + **Tailwind CSS v4** + **shadcn/ui** (Radix) + **SWR** + **zod**.
Todos los datos vienen de la API de Spring Boot (`citas_salud/`) y se guardan en PostgreSQL.

Diseño basado en el prototipo de Figma "SaludGrau — Prototipo Sistema de Citas Médicas".

## Cómo correr el sistema completo

Requisitos: PostgreSQL 17, JDK 25, Node.js 20.9 o superior.

**1. Contraseña local de PostgreSQL.** Crea `citas_salud/config/application.properties` (git lo ignora) con:

```properties
spring.datasource.password=TU_CONTRASEÑA_DE_POSTGRES
```

**2. Base de datos.** Desde la carpeta raíz del proyecto:

```powershell
powershell -ExecutionPolicy Bypass -File sql\instalar_bd.ps1            # crea centro_salud y carga los datos
powershell -ExecutionPolicy Bypass -File sql\instalar_bd.ps1 -Reiniciar # la borra y la vuelve a crear
```

El script ejecuta en orden `sql/citas_salud.sql` (tablas del equipo), `sql/02_correlativo_ticket.sql`
(secuencia del número de ticket) y `sql/03_datos_iniciales.sql` (catálogos y datos de prueba).

**3. Backend** (puerto 8080). Desde `citas_salud/`, con `JAVA_HOME` apuntando al JDK 25:

```powershell
.\mvnw spring-boot:run
```

Al arrancar genera los turnos libres de los próximos 60 días para cada doctor (hasta que exista el
módulo de admisión, RF-13).

**4. Frontend** (puerto 3000). Desde `frontend/`:

```bash
npm install
npm run dev
```

Abre <http://localhost:3000>. Next.js reenvía `/api/*` al backend (ver `next.config.ts`), así que no hace
falta configurar CORS. Si el backend corre en otra dirección, define `BACKEND_URL` en `.env.local`.

### Cuentas de prueba

| Rol      | DNI        | PIN      | Nombre                                |
| -------- | ---------- | -------- | ------------------------------------- |
| Paciente | `12345678` | `123456` | Ana Quispe Rojas                      |
| Doctor   | `87654321` | `654321` | Rodrigo Mendoza (Medicina General)    |

Los otros doctores (`87654322` a `87654326`) usan el PIN `654321`, y los otros pacientes el `123456`.
Desde el login también se pueden crear cuentas nuevas de paciente o de doctor.

## Pantallas

| Ruta                     | Pantalla del prototipo                               | Requerimientos       |
| ------------------------ | ---------------------------------------------------- | -------------------- |
| `/login`                 | 01 · Login, M1 · Registro, M2 · Recuperar PIN        | RF-01 a RF-04        |
| `/paciente/agendar`      | 02 · Portal del Paciente, M3 · Cita confirmada       | RF-05 a RF-09, RF-11 |
| `/paciente`              | Inicio: resumen de citas e historial                 | RF-07, RF-09         |
| `/paciente/citas`        | Mis citas activas (ver ticket, cancelar)             | RF-07, RF-08         |
| `/paciente/historial`    | Historial de atención y documentos                   | RF-09, RF-11         |
| `/paciente/comunicacion` | Pendiente (pantalla 05 · Comunicación y Campañas)    | RF-19 a RF-21        |
| `/doctor`                | 03 · Panel del Doctor                                | RF-14 a RF-16        |
| `/doctor/agenda`         | Citas asignadas de cualquier día                     | RF-14                |
| `/doctor/solicitudes`    | Estado de cambios de fecha y derivaciones            | RF-15, RF-16         |
| Campana (barra superior) | Notificaciones del usuario                           | RF-17, RF-22         |

Pendientes para las siguientes entregas: panel de administración (04) para aprobar solicitudes,
admisión (validación de tickets y creación de horarios) y comunicación y campañas (05).

## Estructura

```
src/
  app/                  Rutas (App Router): /login, /paciente/*, /doctor/*
  components/
    ui/                 Componentes de shadcn/ui (se agregan con `npx shadcn@latest add`)
    auth/               Login, registro (paciente o doctor) y recuperación de PIN
    citas/              Reserva, calendario, horarios, mis citas, historial, ticket
    doctor/             Agenda, cambio de fecha, derivación y solicitudes
    portal/             Barra, notificaciones y estructura común de los portales
  hooks/                Consultas con SWR (use-citas, use-doctor, use-notificaciones) y use-session
  lib/
    api/                Cliente HTTP, tipos (DTOs) y servicios por módulo
    auth/               Sesión en localStorage
    format.ts           Formato de fechas ("Mar 22 sep · 09:30")
    validation.ts       Esquemas zod de los formularios
```

El tema (colores teal/slate del prototipo) está en `src/app/globals.css`.

## API REST

Base `/api`. JSON en camelCase, fechas `"YYYY-MM-DD"` y horas `"HH:mm"`. Las rutas protegidas reciben
`Authorization: Bearer <token>` (token firmado que entrega el login). Los errores responden
`{ "message": "texto para el usuario" }`. Los tipos están en `src/lib/api/types.ts` y los DTO de Java en
`citas_salud/src/main/java/com/cis/citas_salud/api/dto/`.

| Método  | Ruta                                                                | Descripción                                             |
| ------- | ------------------------------------------------------------------- | ------------------------------------------------------- |
| `POST`  | `/auth/login`                                                       | `{ dni, pin }` → `{ token, usuario }`. 423 si la cuenta está bloqueada (3 intentos) |
| `POST`  | `/auth/registro`                                                    | `{ tipoCuenta: PACIENTE\|DOCTOR, nombres, apellidos, dni, correo, pin, terminosAceptados, idEspecialidad?, cmp? }` |
| `POST`  | `/auth/recuperar-pin`                                               | `{ correo }` → 204                                      |
| `GET`   | `/especialidades`                                                   | Especialidades activas                                  |
| `GET`   | `/doctores?especialidad={id}`                                       | Doctores de la especialidad                             |
| `GET`   | `/doctores/{id}/consultorios`                                       | Consultorios del doctor                                 |
| `GET`   | `/horarios/disponibilidad?doctor={id}&consultorio={id}&mes=YYYY-MM` | Días con cupos libres                                   |
| `GET`   | `/horarios?doctor={id}&consultorio={id}&fecha=YYYY-MM-DD`           | Turnos del día con su estado                            |
| `POST`  | `/citas`                                                            | `{ idHorario }` → cita con ticket. 409 si otro paciente ganó el cupo |
| `GET`   | `/citas/mias`                                                       | Citas activas del paciente                              |
| `PATCH` | `/citas/{id}/cancelar`                                              | Cancela y libera el turno                               |
| `GET`   | `/citas/{id}/ticket`                                                | PDF del ticket                                          |
| `GET`   | `/pacientes/me/historial`                                           | Atenciones (asistió / no asistió)                       |
| `GET`   | `/pacientes/me/documentos`                                          | Documentos médicos                                      |
| `GET`   | `/documentos/{id}/descarga`                                         | Archivo del documento                                   |
| `GET`   | `/doctor/citas?fecha=YYYY-MM-DD`                                    | Citas asignadas del día (por defecto hoy)               |
| `GET`   | `/doctor/citas/proximas`                                            | Citas activas desde hoy                                 |
| `GET`   | `/doctor/horarios-libres`                                           | Turnos libres propios (próximos 21 días)                |
| `POST`  | `/doctor/solicitudes/cambio-fecha`                                  | `{ idCita, idHorarioNuevo, motivo }`                    |
| `POST`  | `/doctor/solicitudes/derivacion`                                    | `{ idCita, establecimiento, especialidad, motivo }`     |
| `GET`   | `/doctor/solicitudes`                                               | Solicitudes enviadas por el doctor                      |
| `GET`   | `/notificaciones`                                                   | Últimas 30 notificaciones del usuario                   |
| `PATCH` | `/notificaciones/{id}/leida`                                        | Marca una como leída                                    |
| `PATCH` | `/notificaciones/leidas`                                            | Marca todas como leídas                                 |
