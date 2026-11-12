# SaludGrau · Frontend

Frontend del sistema de citas médicas del **Centro de Salud Miguel Grau** (Chaclacayo).
Stack: **Next.js 16** (App Router) + **TypeScript** + **Tailwind CSS v4** + **shadcn/ui** (Radix) + **SWR** + **zod**.
Todos los datos vienen de la API de Spring Boot (`citas_salud/`) y se guardan en PostgreSQL.

Diseño basado en el prototipo de Figma "SaludGrau — Prototipo Sistema de Citas Médicas", con ajustes tomados de
portales de clínicas privadas: barra superior blanca con "Agendar cita" siempre a mano, inicio organizado por
tareas, controles más grandes y un teal un tono más oscuro para cumplir el contraste AA.

## Cómo correr el sistema completo

Requisitos: PostgreSQL 17, JDK 25, Node.js 20.9 o superior.

**1. Contraseña local de PostgreSQL.** Crea `citas_salud/config/application.properties` (git lo ignora) con:

```properties
spring.datasource.password=TU_CONTRASEÑA_DE_POSTGRES
```

**2. Base de datos.** Desde la carpeta raíz del proyecto:

```powershell
powershell -ExecutionPolicy Bypass -File sql\instalar_bd.ps1             # crea centro_salud y carga los datos
powershell -ExecutionPolicy Bypass -File sql\instalar_bd.ps1 -Actualizar # agrega lo nuevo sin borrar datos
powershell -ExecutionPolicy Bypass -File sql\instalar_bd.ps1 -Reiniciar  # la borra y la vuelve a crear
```

El script ejecuta en orden `sql/citas_salud.sql` (tablas del equipo), `sql/02_correlativo_ticket.sql`
(secuencia del número de ticket), `sql/03_datos_iniciales.sql` (catálogos y datos de prueba),
`sql/04_recuperacion_pin.sql` (códigos para recuperar el PIN) y `sql/05_profesionales_cmi.sql`
(profesionales del centro de salud), además de `sql/06_orden_examen.sql` (tabla de órdenes). Con
`-Actualizar` ejecuta las actualizaciones idempotentes sin borrar datos.

Los scripts numerados desde el 06 se detectan solos: basta con dejar el archivo en `sql/`. Hoy son
`sql/07_receta_medicamentos.sql` (medicamentos de la receta) y `sql/08_consulta_medica.sql` (tipo de
especialidad, antecedentes del paciente, resultados de exámenes y servicio de Laboratorio). **Cada vez que
bajes cambios, ejecuta el instalador con `-Actualizar`**: si falta una tabla, el backend no arranca. El
instalador usa el PostgreSQL que encuentre en la PC (17, 18...).

Las fechas de los datos de prueba se calculan desde el día de la instalación. Si pasan semanas, las citas y
campañas de ejemplo quedan en el pasado y los paneles se ven vacíos: `-Reiniciar` las renueva.

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
| Laboratorio    | `87654343` | `654321` | Valeria Campos (reporta exámenes)         |
| Administración | `44556677` | `445566` | Andrea Meza Cárdenas (Jefe de admisión)   |

Los otros doctores (`87654322` a `87654326`) usan el PIN `654321`, y los otros pacientes el `123456`.
Desde el login también se pueden crear cuentas nuevas de paciente o de doctor. Las cuentas de
administración solo se crean en la base de datos.

Los profesionales del cartel "Programación del día" del CMI Miguel Grau están en
`sql/05_profesionales_cmi.sql`, con DNI del `87654327` al `87654342` y PIN `654321`. Sus nombres vienen del
cartel; el DNI, el correo y la colegiatura son datos de prueba. El personal de emergencia no tiene
consultorio, así que no aparece en la reserva en línea.

## Pantallas

| Ruta                     | Pantalla del prototipo                               | Requerimientos       |
| ------------------------ | ---------------------------------------------------- | -------------------- |
| `/login`                 | 01 · Login, M1 · Registro, M2 · Recuperar PIN        | RF-01 a RF-04        |
| `/paciente/agendar`      | 02 · Portal del Paciente, M3 · Cita confirmada       | RF-05 a RF-09, RF-11 |
| `/paciente`              | Inicio: próxima cita, accesos, citas e historial     | RF-07, RF-09         |
| `/paciente/citas`        | Mis citas activas (ver ticket, confirmar, cancelar)  | RF-07, RF-08         |
| `/paciente/historial`    | Historial de atención y documentos                   | RF-09, RF-11         |
| `/paciente/comunicacion` | 05 · Comunicación y Campañas: inscripción y comprobante | RF-19 a RF-21     |
| `/doctor/agenda`         | Agenda del doctor: hoy, 7 días o un rango de fechas  | RF-14                |
| `/doctor/consulta/[idCita]` | Consulta: ficha o reporte de exámenes e historia clínica del paciente | RF-10, RF-11 |
| `/doctor/solicitudes`    | Pedir cambio de fecha o derivación y seguir la respuesta | RF-15, RF-16     |
| `/admin`                 | 04 · Panel de Administración: indicadores, validación de tickets, solicitudes, horarios | RF-12, RF-13, RF-17, RF-18 |
| `/admin/comunicacion`    | 05 · Comunicación y Campañas (lado administración)   | RF-19, RF-20         |
| `/admin/reportes`        | Reportes de control de citas y ausentismo            | EDT 4.8              |
| Campana (barra superior) | Notificaciones del usuario                           | RF-17, RF-22         |

Al registrar una atención, el doctor puede solicitar exámenes de forma opcional: escribe un examen
por línea e indica el motivo clínico. Se guarda la orden y se genera un PDF Jasper con el paciente,
los exámenes, el motivo, el médico solicitante y la fecha. El doctor puede descargarla al cerrar la
atención y el paciente desde `/paciente/historial`.

### Recepción: validación de tickets (RF-12)

En `/admin` → **Validar ticket de atención**, recepción escribe el número que presenta el paciente
(`A-0154`, `a-154` o solo `154`) y ve a quién corresponde y si puede pasar:

| Resultado     | Cuándo                                              |
| ------------- | --------------------------------------------------- |
| `VALIDO`      | Cita activa de hoy aún sin validar → **Registrar llegada** |
| `YA_VALIDADO` | Recepción ya registró la llegada (muestra la hora)  |
| `OTRO_DIA`    | La cita es de otra fecha: no le corresponde el turno |
| `NO_ACTIVA`   | La cita fue cancelada, atendida o marcada como inasistencia |

Al registrar la llegada se guarda una fila en `auditoria` (acción `VALIDAR_TICKET`, entidad `cita`) y el
doctor recibe la notificación "Paciente en recepción". El estado de la cita no cambia: lo sigue cerrando
el doctor al registrar la atención. Con los datos iniciales hay citas de hoy con el doctor `87654321`
para probarlo.

### Recordatorios automáticos (RF-22)

El backend avisa a cada paciente de sus **citas activas** y de las **campañas a las que se inscribió**
que son al día siguiente (`RecordatorioService`). Corre al arrancar y luego cada hora
(`saludgrau.recordatorios.cron`); no repite un aviso que ya envió.

- En la campana aparece como notificación de tipo `RECORDATORIO` ("Recordatorio de cita · A-0154").
- Se envía una copia por correo al `correo` del usuario (`CorreoService`). Para que salga de verdad hay que
  configurar un servidor SMTP en `citas_salud/config/application.properties` (ver los comentarios de
  `src/main/resources/application.properties`); sin esa configuración el correo solo se registra en el log.
- SMS/WhatsApp quedan para cuando se contrate un proveedor (Twilio, API de WhatsApp Business): bastaría con
  otro canal junto a `CorreoService`.

### Recuperar el PIN (RF-03)

En el login → **¿Olvidaste tu PIN?**, el usuario escribe su correo y recibe un código de 6 dígitos que vence a
los 15 minutos (`RecuperacionPinService`). El código se guarda cifrado en `recuperacion_pin`, admite 5 intentos
y solo se puede reenviar una vez por minuto. Al cambiar el PIN también se desbloquea la cuenta.

Para que el correo salga de verdad hay que configurar SMTP (ver arriba). Sin SMTP el código queda en el log del
backend, en la línea `Código de recuperación de PIN para ...`, para poder probar el flujo.

### Consulta del doctor (RF-10, RF-14)

El doctor entra a **Agenda** (`/doctor` redirige ahí). Puede ver sus citas de hoy, de los próximos 7 días o de
un rango que elige en el calendario (hasta 31 días). **Atender** abre `/doctor/consulta/[idCita]`:

- **Paciente:** datos, alergias destacadas y antecedentes, que el doctor puede editar.
- **Historia clínica:** atenciones anteriores de todo el centro con su receta, resultados de exámenes y
  documentos descargables. El doctor solo ve la historia de pacientes con los que tiene o tuvo una cita.
- **Atención según la especialidad** (`especialidad.tipo`):
  - `CONSULTA`: ficha de consulta con síntomas, exploración física, diagnóstico, tratamiento, observaciones,
    receta médica y documento adjunto.
  - `APOYO_DIAGNOSTICO` (Laboratorio, Ecografía, Rayos X): reporte de exámenes con tipo de examen, resultado,
    valor de referencia e interpretación, más una conclusión. No pide tratamiento.

Una cita futura solo muestra la historia clínica; una ya atendida muestra lo registrado. Para probar el
reporte de exámenes entra como Laboratorio (`87654343`): tiene una cita de hoy con Ana Quispe.

## Formularios de entrada de datos

Cada formulario valida en el navegador y otra vez en la API, y guarda en PostgreSQL:

| Formulario                              | Quién          | Dónde                                     | Tabla que llena                         |
| --------------------------------------- | -------------- | ----------------------------------------- | --------------------------------------- |
| Crear horarios de atención              | Administración | `/admin` → Gestionar nuevos horarios      | `horario`                               |
| Nueva campaña médica                    | Administración | `/admin/comunicacion` → Nueva campaña     | `campania` (+ aviso a cada paciente en `notificacion`) |
| Inscribirse a una campaña               | Paciente       | `/paciente/comunicacion` → Inscribirme a la campaña | `inscripcion_campania` (descuenta un cupo) |
| Registrar atención médica               | Doctor         | `/doctor` → Atender                       | `atencion`, `documento_medico` (archivo opcional) |
| Emitir orden de exámenes                 | Doctor         | `/doctor` → Atender                       | `orden_examen` (PDF disponible para doctor y paciente) |
| Aprobar o rechazar solicitudes          | Administración | `/admin` → Solicitudes de cambio de fecha / de derivación | `solicitud` (+ mueve la `cita` si se aprueba un cambio de fecha) |

Además están los formularios de la primera entrega: registro de cuenta (`usuario`, `paciente` o
`doctor`), reservar cita (`cita`), solicitar cambio de fecha y derivar paciente (`solicitud`).

Los datos iniciales dejan al menos 5 registros en cada una de esas tablas:

| Tabla                  | Registros | Tabla                  | Registros |
| ---------------------- | --------- | ---------------------- | --------- |
| `usuario`              | 28        | `atencion`             | 6         |
| `paciente`             | 5         | `documento_medico`     | 8         |
| `doctor`               | 22        | `solicitud`            | 6         |
| `cita`                 | 13        | `campania`             | 5         |
| `horario`              | 15 (+ los que genera el backend) | `inscripcion_campania` | 7 |

`instalar_bd.ps1` muestra este conteo al terminar.

## Pruebas

| Prueba                  | Cómo se ejecuta                                                         | Necesita                     |
| ----------------------- | ----------------------------------------------------------------------- | ---------------------------- |
| Unitarias (JUnit)       | `.\mvnw test`, desde `citas_salud/`                                     | Solo el JDK 25               |
| Integración (Postman)   | Pasos en la descripción de `docs/postman/SaludGrau.postman_collection.json` | Base de prueba recién instalada |
| Carga y concurrencia    | `node docs/pruebas/prueba-carga.mjs http://localhost:8081`              | Base de prueba recién instalada |

La colección y la prueba de carga crean datos: nunca las ejecutes contra la base `centro_salud`. Después de la
colección, reinstala la base de prueba antes de la prueba de carga, porque la colección bloquea una cuenta 5 minutos.

## Estructura

```
src/
  app/                  Rutas (App Router): /login, /paciente/*, /doctor/*, /admin/*
  components/
    ui/                 Componentes de shadcn/ui (se agregan con `npx shadcn@latest add`)
    auth/               Login, registro (paciente o doctor) y recuperación de PIN
    citas/              Reserva, calendario, horarios, mis citas, historial, ticket
    doctor/             Agenda por rango de fechas, cambio de fecha, derivación y solicitudes
    consulta/           Página de consulta: ficha, receta, reporte de exámenes e historia clínica
    admin/              Indicadores, responder solicitudes, crear horarios, campañas y reportes
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
| `POST`  | `/auth/recuperar-pin`                                               | `{ correo }` → 204. Envía el código si el correo existe |
| `POST`  | `/auth/restablecer-pin`                                             | `{ correo, codigo, pinNuevo }` → 204. 400 si el código no es válido o venció |
| `GET`   | `/especialidades`                                                   | Especialidades activas                                  |
| `GET`   | `/doctores?especialidad={id}`                                       | Doctores de la especialidad                             |
| `GET`   | `/doctores/{id}/consultorios`                                       | Consultorios del doctor                                 |
| `GET`   | `/horarios/disponibilidad?doctor={id}&consultorio={id}&mes=YYYY-MM` | Días con cupos libres                                   |
| `GET`   | `/horarios?doctor={id}&consultorio={id}&fecha=YYYY-MM-DD`           | Turnos del día con su estado                            |
| `POST`  | `/citas`                                                            | `{ idHorario }` → cita con ticket. 409 si otro paciente ganó el cupo |
| `GET`   | `/citas/mias`                                                       | Citas activas del paciente                              |
| `PATCH` | `/citas/{id}/cancelar`                                              | Cancela y libera el turno                               |
| `PATCH` | `/citas/{id}/confirmar`                                             | El paciente confirma su asistencia: RESERVADA pasa a CONFIRMADA. 409 si ya no se puede |
| `GET`   | `/citas/{id}/ticket`                                                | PDF del ticket                                          |
| `GET`   | `/pacientes/me/historial`                                           | Atenciones (asistió / no asistió)                       |
| `GET`   | `/pacientes/me/citas/{id}/orden-examenes`                           | PDF de la orden de exámenes de una cita propia           |
| `GET`   | `/pacientes/me/documentos`                                          | Documentos médicos                                      |
| `GET`   | `/documentos/{id}/descarga`                                         | Archivo del documento                                   |
| `GET`   | `/doctor/citas?fecha=YYYY-MM-DD`                                    | Citas asignadas del día (por defecto hoy)               |
| `GET`   | `/doctor/citas/proximas`                                            | Citas activas desde hoy                                 |
| `GET`   | `/doctor/horarios-libres`                                           | Turnos libres propios (próximos 21 días)                |
| `GET`   | `/doctor/agenda?desde=YYYY-MM-DD&hasta=YYYY-MM-DD`                  | Citas del doctor en el rango (hasta 31 días). Sin fechas, las de hoy |
| `GET`   | `/doctor/citas/{id}/consulta`                                       | Paciente, antecedentes, historia clínica y `modo` de atención de una cita propia |
| `PATCH` | `/doctor/citas/{id}/antecedentes`                                   | `{ alergias, antecedentes, medicacionHabitual }` |
| `POST`  | `/doctor/atenciones/{id}/resultados`                                | `{ resultados: [{ tipoExamen, resultado, valorReferencia?, interpretacion? }] }`. Solo apoyo al diagnóstico; 409 si ya tiene |
| `GET`   | `/doctor/documentos/{id}/descarga`                                  | Documento de un paciente que el doctor atiende |
| `POST`  | `/doctor/solicitudes/cambio-fecha`                                  | `{ idCita, idHorarioNuevo, motivo }`                    |
| `POST`  | `/doctor/solicitudes/derivacion`                                    | `{ idCita, establecimiento, especialidad, motivo }`     |
| `GET`   | `/doctor/solicitudes`                                               | Solicitudes enviadas por el doctor                      |
| `POST`  | `/doctor/citas/{id}/atencion`                                       | `{ asistio, anamnesis?, examenFisico?, diagnostico, tratamiento, observaciones?, examenesRequeridos?, motivoClinico? }`. Los dos últimos campos emiten opcionalmente una orden |
| `GET`   | `/doctor/tipos-documento`                                           | Tipos de documento médico                               |
| `POST`  | `/doctor/atenciones/{id}/documentos`                                | Multipart `tipoDocumento` + `archivo` (PDF, JPG o PNG, hasta 5 MB) |
| `GET`   | `/doctor/atenciones/{id}/orden-examenes`                            | PDF de la orden emitida por el doctor autenticado          |
| `GET`   | `/admin/resumen`                                                    | Citas de hoy, solicitudes pendientes, campañas vigentes, ausentismo del mes |
| `GET`   | `/admin/reportes/citas?desde=YYYY-MM-DD&hasta=YYYY-MM-DD`           | Citas y ausentismo por especialidad y doctor. Sin fechas, el mes en curso |
| `GET`   | `/admin/solicitudes`                                                | Solicitudes pendientes                                  |
| `PATCH` | `/admin/solicitudes/{id}`                                           | `{ decision: APROBAR\|RECHAZAR, respuesta? }` (el motivo es obligatorio al rechazar). 409 si ya se respondió |
| `GET`   | `/admin/tickets/{numero}`                                           | Cita del ticket y si puede pasar (`resultado`: `VALIDO`, `YA_VALIDADO`, `OTRO_DIA`, `NO_ACTIVA`) |
| `POST`  | `/admin/tickets/{numero}/validacion`                                | Registra la llegada del paciente y avisa al doctor. 409 si el ticket no es válido hoy |
| `POST`  | `/admin/horarios`                                                   | `{ idDoctor, idConsultorio, fecha, horaInicio, horaFin, duracionMinutos }` → `{ creados, omitidos }` |
| `GET`   | `/admin/campanias`                                                  | Todas las campañas con sus inscritos                    |
| `POST`  | `/admin/campanias`                                                  | `{ titulo, descripcion?, fechaInicio, fechaFin, hora?, lugar?, cupos }` |
| `GET`   | `/campanias`                                                        | Campañas vigentes, indicando si el paciente ya se inscribió |
| `POST`  | `/campanias/{id}/inscripcion`                                       | Inscribe al paciente. 409 si ya estaba inscrito o no quedan cupos |
| `GET`   | `/campanias/inscripciones/{id}/comprobante`                         | PDF del comprobante de inscripción                      |
| `GET`   | `/notificaciones`                                                   | Últimas 30 notificaciones del usuario                   |
| `PATCH` | `/notificaciones/{id}/leida`                                        | Marca una como leída                                    |
| `PATCH` | `/notificaciones/leidas`                                            | Marca todas como leídas                                 |
