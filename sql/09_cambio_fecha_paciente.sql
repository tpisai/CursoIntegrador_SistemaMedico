-- 09. Cambio de fecha con decisión del paciente.
--
-- El doctor ya no propone el horario nuevo: administración aprueba el motivo y el
-- paciente decide después si cambia de hora con el mismo doctor, elige otro doctor
-- o cancela la cita. Esa decisión cierra la solicitud en el estado RESUELTA.
--
-- Re-ejecutable: `instalar_bd.ps1 -Actualizar` vuelve a correr este script.

ALTER TABLE solicitud DROP CONSTRAINT IF EXISTS chk_solicitud_estado;

ALTER TABLE solicitud ADD CONSTRAINT chk_solicitud_estado
    CHECK (
        estado IN (
            'PENDIENTE',
            'EN_REVISION',
            'APROBADA',
            'RECHAZADA',
            'RESUELTA'
        )
    );
