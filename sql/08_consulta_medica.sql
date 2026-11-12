-- ============================================================
-- BASE DE DATOS: SLGR
-- Script 08 · Página de consulta del doctor.
--   1. Tipo de especialidad: consulta médica o apoyo al diagnóstico
--      (laboratorio, ecografía, rayos X), que reporta resultados.
--   2. Antecedentes del paciente en su historia clínica.
--   3. Resultados de exámenes por atención.
--   4. Servicio de Laboratorio con un profesional de prueba.
--   5. Datos de demostración.
-- Se puede ejecutar más de una vez sobre una base existente.
-- ============================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;


-- ============================================================
-- 1. TIPO DE ESPECIALIDAD
-- ============================================================

ALTER TABLE especialidad
    ADD COLUMN IF NOT EXISTS tipo VARCHAR(20) NOT NULL DEFAULT 'CONSULTA';

ALTER TABLE especialidad DROP CONSTRAINT IF EXISTS chk_especialidad_tipo;
ALTER TABLE especialidad
    ADD CONSTRAINT chk_especialidad_tipo
        CHECK (tipo IN ('CONSULTA', 'APOYO_DIAGNOSTICO'));

INSERT INTO especialidad (nombre, descripcion, tipo) VALUES
    ('Laboratorio', 'Análisis clínicos y toma de muestras', 'APOYO_DIAGNOSTICO')
ON CONFLICT (nombre) DO NOTHING;

UPDATE especialidad
SET tipo = 'APOYO_DIAGNOSTICO'
WHERE nombre IN ('Laboratorio', 'Ecografía', 'Rayos X')
  AND tipo <> 'APOYO_DIAGNOSTICO';


-- ============================================================
-- 2. ANTECEDENTES DEL PACIENTE
-- ============================================================

ALTER TABLE historia_clinica ADD COLUMN IF NOT EXISTS alergias TEXT;
ALTER TABLE historia_clinica ADD COLUMN IF NOT EXISTS antecedentes TEXT;
ALTER TABLE historia_clinica ADD COLUMN IF NOT EXISTS medicacion_habitual TEXT;


-- ============================================================
-- 3. RESULTADO_EXAMEN
-- Resultados que reporta un servicio de apoyo al diagnóstico.
-- ============================================================

CREATE TABLE IF NOT EXISTS resultado_examen (
    id_resultado SERIAL PRIMARY KEY,

    id_atencion INTEGER NOT NULL,

    tipo_examen VARCHAR(150) NOT NULL,

    resultado VARCHAR(300) NOT NULL,

    valor_referencia VARCHAR(150),

    interpretacion TEXT,

    orden INTEGER NOT NULL DEFAULT 1,

    CONSTRAINT fk_resultado_examen_atencion
        FOREIGN KEY (id_atencion)
        REFERENCES atencion(id_atencion)
        ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_resultado_examen_atencion
    ON resultado_examen(id_atencion);


-- ============================================================
-- 4. SERVICIO DE LABORATORIO
-- Profesional de prueba (PIN 654321). El nombre, el DNI y la
-- colegiatura son datos inventados para la demostración.
-- ============================================================

INSERT INTO consultorio (nombre, zona)
SELECT 'Laboratorio', 'Consultorios externos'
WHERE NOT EXISTS (
    SELECT 1 FROM consultorio c
    WHERE c.zona = 'Consultorios externos' AND c.nombre = 'Laboratorio'
);

INSERT INTO usuario (id_rol, dni, nombres, apellidos, correo, pin_hash, terminos_aceptados)
SELECT r.id_rol, '87654343', 'Valeria', 'Campos Ruiz', 'valeria.campos@saludgrau.pe',
       crypt('654321', gen_salt('bf', 10)), TRUE
FROM rol r
WHERE r.nombre = 'DOCTOR'
ON CONFLICT DO NOTHING;

INSERT INTO doctor (id_usuario, id_especialidad, cmp)
SELECT u.id_usuario, e.id_especialidad, 'POR-CONFIRMAR-87654343'
FROM usuario u
JOIN especialidad e ON e.nombre = 'Laboratorio'
WHERE u.dni = '87654343' AND u.correo = 'valeria.campos@saludgrau.pe'
ON CONFLICT DO NOTHING;

INSERT INTO doctor_consultorio (id_doctor, id_consultorio)
SELECT d.id_doctor, c.id_consultorio
FROM doctor d
JOIN usuario u ON u.id_usuario = d.id_usuario AND u.dni = '87654343'
JOIN consultorio c ON c.zona = 'Consultorios externos' AND c.nombre = 'Laboratorio'
ON CONFLICT DO NOTHING;


-- ============================================================
-- 5. DATOS DE DEMOSTRACIÓN
-- ============================================================

-- Antecedentes de los pacientes de prueba (solo si aún no tienen).
UPDATE historia_clinica hc
SET alergias = v.alergias,
    antecedentes = v.antecedentes,
    medicacion_habitual = v.medicacion
FROM (VALUES
    ('12345678', 'Penicilina', 'Asma leve en la infancia. Sin cirugías.', 'Ninguna'),
    ('45879621', 'Ninguna conocida', 'Faringitis a repetición (3 episodios en 2026).', 'Ninguna'),
    ('40125879', 'Ninguna conocida', 'Soplo sistólico en estudio. Madre con hipertensión arterial.', 'Ninguna'),
    ('42365981', 'Ninguna conocida', 'Hipertensión arterial estadio 1 desde 2026.', 'Losartán 50 mg cada 24 horas'),
    ('47851236', 'Sulfas', 'Gestación en curso con control prenatal.', 'Ácido fólico y sulfato ferroso')
) AS v(dni, alergias, antecedentes, medicacion)
JOIN usuario u ON u.dni = v.dni
JOIN paciente p ON p.id_usuario = u.id_usuario
WHERE hc.id_paciente = p.id_paciente
  AND hc.alergias IS NULL
  AND hc.antecedentes IS NULL
  AND hc.medicacion_habitual IS NULL;

-- Laboratorio: un examen ya reportado y una cita para hoy de Ana Quispe.
-- Solo se crean la primera vez (mientras el laboratorio no tenga citas).
DO $$
DECLARE
    v_doctor INTEGER;
    v_consultorio INTEGER;
    v_paciente INTEGER;
    v_historia INTEGER;
    v_horario INTEGER;
    v_cita INTEGER;
    v_atencion INTEGER;
    v_anterior DATE := current_date - 12;
BEGIN
    SELECT d.id_doctor INTO v_doctor
    FROM doctor d JOIN usuario u ON u.id_usuario = d.id_usuario
    WHERE u.dni = '87654343';

    SELECT c.id_consultorio INTO v_consultorio
    FROM consultorio c
    WHERE c.zona = 'Consultorios externos' AND c.nombre = 'Laboratorio';

    SELECT p.id_paciente, hc.id_historia INTO v_paciente, v_historia
    FROM paciente p
    JOIN usuario u ON u.id_usuario = p.id_usuario
    JOIN historia_clinica hc ON hc.id_paciente = p.id_paciente
    WHERE u.dni = '12345678';

    IF v_doctor IS NULL OR v_consultorio IS NULL OR v_paciente IS NULL THEN
        RETURN;
    END IF;
    IF EXISTS (
        SELECT 1 FROM cita c JOIN horario h ON h.id_horario = c.id_horario
        WHERE h.id_doctor = v_doctor
    ) THEN
        RETURN;
    END IF;

    -- Examen de hace 12 días, ya reportado. El turno es a las 07:30, antes de los
    -- que genera la API (desde las 08:00), para no cruzarse con ellos.
    INSERT INTO horario (id_doctor, id_consultorio, fecha, hora_inicio, hora_fin, estado)
    VALUES (v_doctor, v_consultorio, v_anterior, '07:30', '08:00', 'FINALIZADO')
    RETURNING id_horario INTO v_horario;

    INSERT INTO cita (id_paciente, id_horario, fecha_creacion, motivo, estado, numero_ticket)
    VALUES (v_paciente, v_horario, (v_anterior - 3)::timestamp + TIME '10:00',
            'Hemograma y glucosa en ayunas', 'ATENDIDA',
            'A-' || lpad(nextval('seq_ticket_cita')::text, 4, '0'))
    RETURNING id_cita INTO v_cita;

    INSERT INTO atencion (id_historia, id_cita, id_doctor, fecha_atencion, estado, anamnesis, diagnostico, observaciones)
    VALUES (v_historia, v_cita, v_doctor, v_anterior + TIME '07:30', 'FINALIZADA',
            'Exámenes solicitados en el control general.',
            'Hemograma y glucosa dentro de los valores de referencia.',
            'Muestra tomada en ayunas.')
    RETURNING id_atencion INTO v_atencion;

    INSERT INTO resultado_examen (id_atencion, tipo_examen, resultado, valor_referencia, interpretacion, orden) VALUES
        (v_atencion, 'Hemoglobina', '13.4 g/dL', '12.0 a 16.0 g/dL', 'Normal', 1),
        (v_atencion, 'Leucocitos', '6 800 /µL', '4 500 a 11 000 /µL', 'Normal', 2),
        (v_atencion, 'Glucosa en ayunas', '88 mg/dL', '70 a 100 mg/dL', 'Normal', 3);

    -- Cita de hoy, pendiente de reportar.
    INSERT INTO horario (id_doctor, id_consultorio, fecha, hora_inicio, hora_fin, estado)
    VALUES (v_doctor, v_consultorio, current_date, '07:30', '08:00', 'RESERVADO')
    RETURNING id_horario INTO v_horario;

    INSERT INTO cita (id_paciente, id_horario, fecha_creacion, motivo, estado, numero_ticket)
    VALUES (v_paciente, v_horario, (current_date - 2)::timestamp + TIME '09:15',
            'Perfil lipídico de control', 'CONFIRMADA',
            'A-' || lpad(nextval('seq_ticket_cita')::text, 4, '0'));
END
$$;
