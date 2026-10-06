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

Al arrancar genera los turnos libres de los próximos 60 días para cada doctor; administración puede
crear más desde `/admin` (RF-13). Los documentos que adjuntan los doctores se guardan en
`citas_salud/archivos/` (git lo ignora).

**4. Frontend** (puerto 3000). Desde `frontend/`:

```bash
npm install
npm run dev
```

Abre <http://localhost:3000>. Next.js reenvía `/api/*` al backend (ver `next.config.ts`), así que no hace
falta configurar CORS. Si el backend corre en otra dirección, define `BACKEND_URL` en `.env.local`.

### Cuentas de prueba

| Rol            | DNI        | PIN      | Nombre                                    |
| -------------- | ---------- | -------- | ----------------------------------------- |
| Paciente       | `12345678` | `123456` | Ana Quispe Rojas                          |
| Doctor         | `87654321` | `654321` | Rodrigo Mendoza (Medicina General)        |
| Administración | `44556677` | `445566` | Andrea Meza Cárdenas (Jefe de admisión)   |

Los otros doctores (`87654322` a `87654326`) usan el PIN `654321`, y los otros pacientes el `123456`.
Desde el login también se pueden crear cuentas nuevas de paciente o de doctor. Las cuentas de
administración solo se crean en la base de datos.

## Pantallas

| Ruta                     | Pantalla del prototipo                               | Requerimientos       |
| ------------------------ | ---------------------------------------------------- | -------------------- |
| `/login`                 | 01 · Login, M1 · Registro, M2 · Recuperar PIN        | RF-01 a RF-04        |
| `/paciente/agendar`      | 02 · Portal del Paciente, M3 · Cita confirmada       | RF-05 a RF-09, RF-11 |
| `/paciente`              | Inicio: resumen de citas e historial                 | RF-07, RF-09         |
| `/paciente/citas`        | Mis citas activas (ver ticket, cancelar)             | RF-07, RF-08         |
| `/paciente/historial`    | Historial de atención y documentos                   | RF-09, RF-11         |
| `/paciente/comunicacion` | 05 · Comunicación y Campañas: inscripción y comprobante | RF-19 a RF-21     |
| `/doctor`                | 03 · Panel del Doctor, registrar atención            | RF-10, RF-14 a RF-16 |
| `/doctor/agenda`         | Citas asignadas de cualquier día                     | RF-10, RF-14         |
| `/doctor/solicitudes`    | Estado de cambios de fecha y derivaciones            | RF-15, RF-16         |
| `/admin`                 | 04 · Panel de Administración: indicadores, solicitudes, horarios | RF-13, RF-17, RF-18 |
| `/admin/comunicacion`    | 05 · Comunicación y Campañas (lado administración)   | RF-19, RF-20         |
| Campana (barra superior) | Notificaciones del usuario                           | RF-17, RF-22         |

Pendiente para las siguientes entregas: validación de tickets en admisión.

## Formularios de entrada de datos

Cada formulario valida en el navegador y otra vez en la API, y guarda en PostgreSQL:

| Formulario                              | Quién          | Dónde                                     | Tabla que llena                         |
| --------------------------------------- | -------------- | ----------------------------------------- | --------------------------------------- |
| Crear horarios de atención              | Administración | `/admin` → Gestionar nuevos horarios      | `horario`                               |
| Nueva campaña médica                    | Administración | `/admin/comunicacion` → Nueva campaña     | `campania` (+ aviso a cada paciente en `notificacion`) |
| Inscribirse a una campaña               | Paciente       | `/paciente/comunicacion` → Inscribirme a la campaña | `inscripcion_campania` (descuenta un cupo) |
| Registrar atención médica               | Doctor         | `/doctor` → Atender                       | `atencion`, `documento_medico` (archivo opcional) |
| Aprobar o rechazar solicitudes          | Administración | `/admin` → Solicitudes de cambio de fecha / de derivación | `solicitud` (+ mueve la `cita` si se aprueba un cambio de fecha) |

Además están los formularios de la primera entrega: registro de cuenta (`usuario`, `paciente` o
`doctor`), reservar cita (`cita`), solicitar cambio de fecha y derivar paciente (`solicitud`).

Los datos iniciales (`sql/03_datos_iniciales.sql`) dejan al menos 5 registros en cada una de esas tablas:

| Tabla                  | Registros | Tabla                  | Registros |
| ---------------------- | --------- | ---------------------- | --------- |
| `usuario`              | 12        | `atencion`             | 6         |
| `paciente`             | 5         | `documento_medico`     | 8         |
| `doctor`               | 6         | `solicitud`            | 6         |
| `cita`                 | 13        | `campania`             | 5         |
| `horario`              | 15 (+ los que genera el backend) | `inscripcion_campania` | 7 |

`instalar_bd.ps1` muestra este conteo al terminar. Para que una base ya instalada tenga estos datos,
vuelve a instalarla con `-Reiniciar` (borra lo que se haya registrado a mano).

## Estructura

```
src/
  app/                  Rutas (App Router): /login, /paciente/*, /doctor/*, /admin/*
  components/
    ui/                 Componentes de shadcn/ui (se agregan con `npx shadcn@latest add`)
    auth/               Login, registro (paciente o doctor) y recuperación de PIN
    citas/              Reserva, calendario, horarios, mis citas, historial, ticket
    doctor/             Agenda, registrar atención, cambio de fecha, derivación y solicitudes
    admin/              Indicadores, responder solicitudes, crear horarios y campañas
    campanias/          Campañas vigentes, inscripción y comprobante del paciente
    portal/             Barra, notificaciones y estructura común de los portales
  hooks/                Consultas con SWR (use-citas, use-doctor, use-admin, use-campanias,
                        use-notificaciones) y use-session
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
| `POST`  | `/doctor/citas/{id}/atencion`                                       | `{ asistio, anamnesis?, examenFisico?, diagnostico, tratamiento, observaciones? }`. Con `asistio: false` solo marca la inasistencia |
| `GET`   | `/doctor/tipos-documento`                                           | Tipos de documento médico                               |
| `POST`  | `/doctor/atenciones/{id}/documentos`                                | Multipart `tipoDocumento` + `archivo` (PDF, JPG o PNG, hasta 5 MB) |
| `GET`   | `/admin/resumen`                                                    | Citas de hoy, solicitudes pendientes, campañas vigentes, ausentismo del mes |
| `GET`   | `/admin/solicitudes`                                                | Solicitudes pendientes                                  |
| `PATCH` | `/admin/solicitudes/{id}`                                           | `{ decision: APROBAR\|RECHAZAR, respuesta? }` (el motivo es obligatorio al rechazar). 409 si ya se respondió |
| `POST`  | `/admin/horarios`                                                   | `{ idDoctor, idConsultorio, fecha, horaInicio, horaFin, duracionMinutos }` → `{ creados, omitidos }` |
| `GET`   | `/admin/campanias`                                                  | Todas las campañas con sus inscritos                    |
| `POST`  | `/admin/campanias`                                                  | `{ titulo, descripcion?, fechaInicio, fechaFin, hora?, lugar?, cupos }` |
| `GET`   | `/campanias`                                                        | Campañas vigentes, indicando si el paciente ya se inscribió |
| `POST`  | `/campanias/{id}/inscripcion`                                       | Inscribe al paciente. 409 si ya estaba inscrito o no quedan cupos |
| `GET`   | `/campanias/inscripciones/{id}/comprobante`                         | PDF del comprobante de inscripción                      |
| `GET`   | `/notificaciones`                                                   | Últimas 30 notificaciones del usuario                   |
| `PATCH` | `/notificaciones/{id}/leida`                                        | Marca una como leída                                    |
| `PATCH` | `/notificaciones/leidas`                                            | Marca todas como leídas                                 |
