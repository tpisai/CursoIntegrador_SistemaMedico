# Cambio de fecha con decisión del paciente y constancia PDF

Objetivo: reformar la solicitud de cambio de fecha para que el doctor solo pida el cambio con motivo, administración lo apruebe y el paciente decida en un modal (mismo doctor / otro doctor / cancelar), con constancia PDF descargable por doctor, paciente y administración.
Rama: `main` (no se crea rama ni se commitea sin pedido explícito). Entrega: `ask-on-risk`, sin PR hasta pedido.
Pruebas: backend `.\mvnw test -Dmaven.compiler.release=21` (esta PC no tiene JDK 25); frontend `npx tsc --noEmit` y `npx eslint <archivo>`.

## Specs

- S1. El doctor envía la cita activa y el motivo, sin elegir horario nuevo: «ahora el doctor debe enviar la cita activa y el motivo y los demas campos que consideres para completar este reporte que te mostre en foto».
- S2. Administración aprueba o rechaza según el motivo: «luego administracion acepta si ve valido el motivo».
- S3. Al aprobar, el paciente ve un modal apenas entra al portal, con tres botones: «uno de cambiar la hora con el mismo doctor, opcion 2 elegir cita con otro doctor y por ende cancelar esta, y 3 directamente cancelar cita y seguir».
- S4. Al resolver, se notifica al doctor y termina la solicitud: «con eso le llegara la notificacion a doctor y finalizar esta seccion de solicitud de cambio de fecha de cita».
- S5. El paciente descarga la constancia: «este archivo tambien puede descargarlo el paciente como constancia de que su cita fue cambiada».
- S6. Doctor y paciente pueden descargarla: «Recuerda el archivo lo pueden descargar tanto doctor como paciente».
- S7. Una plantilla con dos estados: «si esta pediente que confirme el cambio de horario de paciente que se quede tal cual como solicitud, pero cuando ya el paciente decida si cancela o cambia de fecha o de doctor, se le agregue debajo un contenido de respuesta».
- S8. Mismo formato JRXML y datos de la base: «debes utilizar el mismo formato usando jrxml y que estos reutilicen el base de datos».
- S9. La plantilla debe compilar y verse en Jaspersoft Studio: «debe poder verse ahi para comprobar».
- S10. El reporte se ve solo cuando la solicitud fue aprobada: «donde lo vera el admin y el doctor cuando se aprueba, en caso contrario no».

## Contrato

- `POST /api/doctor/solicitudes/cambio-fecha` → `{ idCita, motivo }` (sin `idHorarioNuevo`).
- `PATCH /api/admin/solicitudes/{id}` → sin cambios de forma; al aprobar un `CAMBIO_HORARIO` ya no reprograma la cita.
- `GET /api/solicitudes/pendientes` (paciente) → solicitudes `CAMBIO_HORARIO` en `APROBADA` de sus citas activas y futuras.
- `POST /api/solicitudes/{id}/resolver` (paciente) → `{ decision: "MISMO_DOCTOR" | "OTRO_DOCTOR" | "CANCELADA", idHorarioNuevo? }`.
- `GET /api/solicitudes/mias` (paciente) → historial propio con `constanciaDisponible`.
- `GET /api/solicitudes/{id}/constancia` → PDF para el doctor solicitante, el paciente de la cita o administración.
- Estado nuevo `RESUELTA` en `solicitud` (migración `sql/09_*.sql`, re-ejecutable).
- `datos_adicionales` guarda la foto original al crear (`fechaOriginal`, `horaOriginal`, `consultorioOriginal`, `doctorOriginal`, `especialidadOriginal`) y la resolución (`decisionPaciente`, `fechaResolucion`, `nuevaFecha`, `nuevaHora`, `nuevoConsultorio`, `nuevoDoctor`, `nuevaEspecialidad`, `nuevoTicket`).
- `GET /api/admin/solicitudes` pasa a devolver todas las solicitudes con su estado (pendientes e histórico), para poder descargar la constancia después de aprobar.

## Tasks

- T1. Migración `09` con estado `RESUELTA` + constantes en `Solicitud`. `[x]` — `sql/09_cambio_fecha_paciente.sql` aplicada en `centro_salud`.
- T2. Doctor: quitar `idHorarioNuevo` del request y guardar la foto original; formulario sin selector de horario. `[x]` — `DoctorServiceTest`, `cambio-fecha-card.tsx`.
- T3. Administración: aprobar sin reprogramar y avisar al paciente para que resuelva. `[x]` — `AdminServiceTest`, `responder-solicitud-dialog.tsx`.
- T4. Paciente: `pendientes`, `resolver` (3 decisiones) y `mias`, con notificación al doctor. `[x]` — `SolicitudService`, `SolicitudController`, `SolicitudServiceTest` (13 pruebas).
- T5. Constancia `ConstanciaCambioFecha.jrxml` + servicio + endpoints + `PlantillasClinicasTest`. `[x]` — consulta validada con `psql` contra `centro_salud` (solicitud 1).
- T6. Frontend doctor/admin: formulario nuevo y botón de constancia en las listas. `[x]` — `solicitudes-card.tsx`, `solicitudes-admin-card.tsx`, `boton-constancia-cambio.tsx`.
- T7. Frontend paciente: modal bloqueante con los 3 caminos y descarga de constancia. `[x]` — `cambio-fecha-dialog.tsx` montado en `app/paciente/layout.tsx`, `cambios-paciente-card.tsx` en `/paciente/citas`.
- T8. Ayuda/docs, suite completa y typecheck/lint. `[x]` — README actualizado; `mvnw test` 223 verdes; `tsc`, `eslint` y `next build` sin errores.

## Log

- L1 (pedido original, literal): «este es un reporte sencillo para solicitar cambio de fecha, sabes este archivo tambien puede descargarlo el paciente como constancia de que su cita fue cambiada para asi indicarle a su jefe pues que su cita se cambio, ahora debes modificar el modo que se hace esta solicitud, de momento esta que el doctor indica la cita activa que cambiara, el motivo y cual sera la nueva hora y luego administracion la acepta, pero no consideramos la necesidad del paciente, entonces debes reformar eso, ahora el doctor debe enviar la cita activa y el motivo y los demas campos que consideres para completar este reporte que te mostre en foto, luego administracion acepta si ve valido el motivo, con eso cuando se le envie la notificacion a paciente, este debe aparecerle apenas entrar un modal donde se le indique que el doctor no podra atender su cita de esa hora, y le colocara 3 botones, uno de cambiar la hora con el mismo doctor, opcion 2 elegir cita con otro doctor y por ende cancelar esta, y 3 directamente cancelar cita y seguir, con eso le llegara la notificacion a doctor y finalizar esta seccion de solicitud de cambio de fecha de cita. Recuerda el archivo lo pueden descargar tanto doctor como paciente en caso necesiten pues no.»
- L2 (decisión de contenido): constancia emitida por el centro con dos estados; mientras está aprobada y pendiente de decisión se muestra como solicitud, y al resolver se agrega debajo el bloque de respuesta.
- L3 (diseño tomado): aprobación no reprograma; modal bloqueante; "otro doctor" = misma especialidad; estado `RESUELTA`; decisión en `datos_adicionales`; notificación al doctor al resolver.
- L4 (alcance): esta entrega cubre solo el reporte de cambio de fecha; el reporte de derivación queda pendiente de su imagen de diseño.
- L5 (restricción de entorno): no se commitea ni se crea rama sin pedido explícito; el árbol ya trae cambios sin commitear de la sesión anterior.
- L6 (verificación real): la consulta del JRXML se ejecutó con `psql` contra `centro_salud` y falló por `co.ubicacion`, columna inexistente; se reemplazó por el mismo criterio de `Consultorio.getUbicacion()` (`zona · nombre`). `PlantillasClinicasTest` solo compila, no detecta eso.
- L7 (entorno): la base `centro_salud` no tenía `RESUELTA` en `chk_solicitud_estado`; se aplicó `sql/09_cambio_fecha_paciente.sql` para que el flujo funcione en local.
- L8 (alcance de UI): `idConsultorio` se agregó a `CambioPendienteResponse` porque el paciente puede elegir consultorio al reprogramar; el modal usa el original como valor por defecto.
- L9 (pendiente de decisión): la solicitud 1 de la base de desarrollo (Ana Quispe, APROBADA) es el caso real para probar el modal; no se resolvió para no alterar datos de desarrollo.
