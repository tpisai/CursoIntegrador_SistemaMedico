-- ============================================================
-- BASE DE DATOS: SLGR
-- Script 03 · Datos iniciales y de demostración.
-- Ejecutar DESPUÉS de citas_salud.sql y 02_correlativo_ticket.sql,
-- sobre una base vacía. Las fechas se calculan desde el día en
-- que se ejecuta el script.
--
-- Cuentas de prueba (PIN guardado con BCrypt):
--   Paciente  DNI 12345678  PIN 123456
--   Doctor    DNI 87654321  PIN 654321  (Rodrigo Mendoza)
-- ============================================================

-- Para cifrar los PIN con BCrypt (crypt + gen_salt).
CREATE EXTENSION IF NOT EXISTS pgcrypto;


-- ============================================================
-- CATÁLOGOS
-- ============================================================

INSERT INTO rol (nombre, descripcion) VALUES
    ('PACIENTE', 'Paciente del centro de salud'),
    ('DOCTOR', 'Personal médico que atiende citas'),
    ('ADMISION', 'Personal de admisión y recepción'),
    ('ADMINISTRADOR', 'Administración del centro de salud');

INSERT INTO especialidad (nombre, descripcion) VALUES
    ('Medicina General', 'Consulta externa de medicina general'),
    ('Odontología', 'Atención dental preventiva y recuperativa'),
    ('Nutrición', 'Evaluación y consejería nutricional'),
    ('Pediatría', 'Atención de niñas, niños y adolescentes'),
    ('Obstetricia', 'Control prenatal y planificación familiar');

INSERT INTO consultorio (nombre, zona, piso, numero) VALUES
    ('Consultorio 3', 'Módulo A', '1', '3'),
    ('Consultorio 4', 'Módulo A', '1', '4'),
    ('Consultorio 1', 'Módulo B', '1', '1'),
    ('Consultorio 2', 'Módulo B', '1', '2'),
    ('Consultorio 1', 'Módulo C', '2', '1'),
    ('Consultorio 2', 'Módulo C', '2', '2');


-- ============================================================
-- DOCTORES (PIN 654321)
-- ============================================================

INSERT INTO usuario (id_rol, dni, nombres, apellidos, correo, telefono, pin_hash, terminos_aceptados)
SELECT r.id_rol, v.dni, v.nombres, v.apellidos, v.correo, v.telefono,
       crypt('654321', gen_salt('bf', 10)), TRUE
FROM rol r
CROSS JOIN (VALUES
    ('87654321', 'Rodrigo', 'Mendoza Ríos', 'rodrigo.mendoza@saludgrau.pe', '987100001'),
    ('87654322', 'Lucía', 'Paredes Soto', 'lucia.paredes@saludgrau.pe', '987100002'),
    ('87654323', 'Carla', 'Espinoza Vega', 'carla.espinoza@saludgrau.pe', '987100003'),
    ('87654324', 'María', 'Torres León', 'maria.torres@saludgrau.pe', '987100004'),
    ('87654325', 'Jorge', 'Salazar Núñez', 'jorge.salazar@saludgrau.pe', '987100005'),
    ('87654326', 'Elena', 'Huamán Cruz', 'elena.huaman@saludgrau.pe', '987100006')
) AS v(dni, nombres, apellidos, correo, telefono)
WHERE r.nombre = 'DOCTOR'
ORDER BY v.dni;

INSERT INTO doctor (id_usuario, id_especialidad, cmp)
SELECT u.id_usuario, e.id_especialidad, v.cmp
FROM (VALUES
    ('87654321', 'Medicina General', '045812'),
    ('87654322', 'Medicina General', '051277'),
    ('87654323', 'Odontología', '038904'),
    ('87654324', 'Nutrición', '062115'),
    ('87654325', 'Pediatría', '047330'),
    ('87654326', 'Obstetricia', '058641')
) AS v(dni, especialidad, cmp)
JOIN usuario u ON u.dni = v.dni
JOIN especialidad e ON e.nombre = v.especialidad
ORDER BY v.dni;

-- Un consultorio tiene como máximo 2 doctores.
INSERT INTO doctor_consultorio (id_doctor, id_consultorio)
SELECT d.id_doctor, c.id_consultorio
FROM (VALUES
    ('87654321', 'Módulo A', 'Consultorio 3'),
    ('87654321', 'Módulo A', 'Consultorio 4'),
    ('87654322', 'Módulo A', 'Consultorio 4'),
    ('87654323', 'Módulo B', 'Consultorio 1'),
    ('87654324', 'Módulo B', 'Consultorio 2'),
    ('87654325', 'Módulo C', 'Consultorio 1'),
    ('87654326', 'Módulo C', 'Consultorio 2')
) AS v(dni, zona, nombre)
JOIN usuario u ON u.dni = v.dni
JOIN doctor d ON d.id_usuario = u.id_usuario
JOIN consultorio c ON c.zona = v.zona AND c.nombre = v.nombre;


-- ============================================================
-- PACIENTES (PIN 123456)
-- ============================================================

INSERT INTO usuario (id_rol, dni, nombres, apellidos, correo, telefono, pin_hash, terminos_aceptados)
SELECT r.id_rol, v.dni, v.nombres, v.apellidos, v.correo, v.telefono,
       crypt('123456', gen_salt('bf', 10)), TRUE
FROM rol r
CROSS JOIN (VALUES
    ('12345678', 'Ana', 'Quispe Rojas', 'ana.quispe@correo.com', '912300001'),
    ('45879621', 'Luis', 'Ramírez Huertas', 'luis.ramirez@correo.com', '912300002'),
    ('40125879', 'Rosa', 'Quispe Mamani', 'rosa.quispe@correo.com', '912300003'),
    ('42365981', 'Jorge', 'Palomino Tello', 'jorge.palomino@correo.com', '912300004')
) AS v(dni, nombres, apellidos, correo, telefono)
WHERE r.nombre = 'PACIENTE'
ORDER BY v.dni = '12345678' DESC, v.dni;

INSERT INTO paciente (id_usuario, fecha_nacimiento, sexo, direccion, grupo_sanguineo)
SELECT u.id_usuario, v.nacimiento::date, v.sexo, v.direccion, v.grupo
FROM (VALUES
    ('12345678', '1998-04-12', 'FEMENINO', 'Av. Nicolás Ayllón 1520, Chaclacayo', 'O+'),
    ('45879621', '1975-11-03', 'MASCULINO', 'Jr. Los Pinos 230, Chaclacayo', 'A+'),
    ('40125879', '1969-06-21', 'FEMENINO', 'Calle Las Flores 118, Chaclacayo', 'O+'),
    ('42365981', '1981-02-14', 'MASCULINO', 'Av. Perú 410, Chaclacayo', 'B+')
) AS v(dni, nacimiento, sexo, direccion, grupo)
JOIN usuario u ON u.dni = v.dni
ORDER BY u.id_usuario;

INSERT INTO historia_clinica (id_paciente, numero_historia)
SELECT id_paciente, 'HC-' || lpad(id_paciente::text, 6, '0')
FROM paciente;


-- ============================================================
-- CITAS DE DEMOSTRACIÓN
-- Los horarios libres de los próximos días los genera la API al
-- arrancar; aquí solo se crean los horarios de estas citas.
-- ============================================================

-- Consultorio donde atiende el doctor ese día: rota por día de la
-- semana entre sus consultorios (misma regla que la API).
CREATE FUNCTION pg_temp.consultorio_del_dia(p_doctor INTEGER, p_fecha DATE)
RETURNS INTEGER AS $$
    SELECT id_consultorio
    FROM (
        SELECT id_consultorio,
               row_number() OVER (ORDER BY id_consultorio) - 1 AS posicion,
               count(*) OVER () AS total
        FROM doctor_consultorio
        WHERE id_doctor = p_doctor
    ) t
    WHERE posicion = extract(dow FROM p_fecha)::INTEGER % total
$$ LANGUAGE sql;

-- Hoy +/- N días, saltando domingos (no hay consulta externa).
CREATE FUNCTION pg_temp.dia_habil(p_dias INTEGER)
RETURNS DATE AS $$
    SELECT CASE
        WHEN extract(dow FROM current_date + p_dias) = 0
            THEN current_date + p_dias + (CASE WHEN p_dias >= 0 THEN 1 ELSE -1 END)
        ELSE current_date + p_dias
    END
$$ LANGUAGE sql;

CREATE FUNCTION pg_temp.crear_cita(
    p_dni_paciente TEXT,
    p_dni_doctor TEXT,
    p_fecha DATE,
    p_hora TIME,
    p_estado TEXT,
    p_ticket INTEGER,
    p_motivo TEXT
) RETURNS INTEGER AS $$
DECLARE
    v_doctor INTEGER;
    v_paciente INTEGER;
    v_horario INTEGER;
    v_cita INTEGER;
BEGIN
    SELECT d.id_doctor INTO v_doctor
    FROM doctor d JOIN usuario u ON u.id_usuario = d.id_usuario
    WHERE u.dni = p_dni_doctor;

    SELECT p.id_paciente INTO v_paciente
    FROM paciente p JOIN usuario u ON u.id_usuario = p.id_usuario
    WHERE u.dni = p_dni_paciente;

    INSERT INTO horario (id_doctor, id_consultorio, fecha, hora_inicio, hora_fin, estado)
    VALUES (
        v_doctor,
        pg_temp.consultorio_del_dia(v_doctor, p_fecha),
        p_fecha,
        p_hora,
        p_hora + INTERVAL '30 minutes',
        CASE WHEN p_estado IN ('ATENDIDA', 'NO_ASISTIO') THEN 'FINALIZADO' ELSE 'RESERVADO' END
    )
    RETURNING id_horario INTO v_horario;

    INSERT INTO cita (id_paciente, id_horario, fecha_creacion, motivo, estado, numero_ticket)
    VALUES (
        v_paciente,
        v_horario,
        LEAST(p_fecha - 7, current_date)::TIMESTAMP + TIME '09:00',
        p_motivo,
        p_estado,
        'A-' || lpad(p_ticket::TEXT, 4, '0')
    )
    RETURNING id_cita INTO v_cita;

    RETURN v_cita;
END
$$ LANGUAGE plpgsql;

CREATE FUNCTION pg_temp.registrar_atencion(p_cita INTEGER, p_diagnostico TEXT, p_tratamiento TEXT)
RETURNS INTEGER AS $$
    INSERT INTO atencion (id_historia, id_cita, id_doctor, fecha_atencion, estado, anamnesis, diagnostico, tratamiento)
    SELECT hc.id_historia, c.id_cita, h.id_doctor, h.fecha + h.hora_inicio, 'FINALIZADA',
           c.motivo, p_diagnostico, p_tratamiento
    FROM cita c
    JOIN horario h ON h.id_horario = c.id_horario
    JOIN historia_clinica hc ON hc.id_paciente = c.id_paciente
    WHERE c.id_cita = p_cita
    RETURNING id_atencion
$$ LANGUAGE sql;

DO $$
DECLARE
    v_cita INTEGER;
    v_atencion_general INTEGER;
    v_atencion_nutricion INTEGER;
BEGIN
    -- Historial de Ana Quispe (paciente de prueba).
    v_cita := pg_temp.crear_cita('12345678', '87654321', pg_temp.dia_habil(-49), '09:00', 'ATENDIDA', 120, 'Control general');
    v_atencion_general := pg_temp.registrar_atencion(v_cita, 'Paciente sano. Control preventivo.', 'Hemograma y glucosa en ayunas.');

    v_cita := pg_temp.crear_cita('12345678', '87654324', pg_temp.dia_habil(-62), '10:00', 'ATENDIDA', 101, 'Evaluación nutricional');
    v_atencion_nutricion := pg_temp.registrar_atencion(v_cita, 'Estado nutricional normal.', 'Plan de alimentación balanceada.');

    PERFORM pg_temp.crear_cita('12345678', '87654323', pg_temp.dia_habil(-77), '08:30', 'NO_ASISTIO', 88, 'Limpieza dental');

    INSERT INTO documento_medico (id_atencion, tipo_documento, nombre_archivo, ruta_archivo, tipo_mime, fecha_carga) VALUES
        (v_atencion_general, 'Historial médico completo', 'historial-medico.pdf', 'demo/historial-medico.pdf', 'application/pdf', pg_temp.dia_habil(-49)),
        (v_atencion_nutricion, 'Resultados de laboratorio', 'resultados-laboratorio.pdf', 'demo/resultados-laboratorio.pdf', 'application/pdf', pg_temp.dia_habil(-60)),
        (v_atencion_general, 'Receta médica', 'receta-medica.pdf', 'demo/receta-medica.pdf', 'application/pdf', pg_temp.dia_habil(-49)),
        (v_atencion_general, 'Diagnóstico del paciente', 'diagnostico.pdf', 'demo/diagnostico.pdf', 'application/pdf', pg_temp.dia_habil(-49));

    -- Próximas citas de Ana.
    PERFORM pg_temp.crear_cita('12345678', '87654321', pg_temp.dia_habil(2), '09:30', 'CONFIRMADA', 154, 'Control general');
    PERFORM pg_temp.crear_cita('12345678', '87654323', pg_temp.dia_habil(5), '11:00', 'RESERVADA', 161, 'Dolor de muela');
    PERFORM pg_temp.crear_cita('12345678', '87654324', pg_temp.dia_habil(8), '08:30', 'CONFIRMADA', 170, 'Control de peso');

    -- Agenda de hoy del Dr. Rodrigo Mendoza (panel del doctor).
    PERFORM pg_temp.crear_cita('45879621', '87654321', pg_temp.dia_habil(0), '09:00', 'RESERVADA', 165, 'Control general');
    PERFORM pg_temp.crear_cita('40125879', '87654321', pg_temp.dia_habil(0), '10:30', 'CONFIRMADA', 166, 'Resultados de laboratorio');
    PERFORM pg_temp.crear_cita('42365981', '87654321', pg_temp.dia_habil(0), '11:00', 'RESERVADA', 167, 'Control de presión');
END
$$;

-- El próximo ticket será A-0171.
DO $$ BEGIN PERFORM setval('seq_ticket_cita', 170); END $$;


-- ============================================================
-- NOTIFICACIONES INICIALES
-- ============================================================

INSERT INTO notificacion (id_usuario, tipo, titulo, mensaje, fecha_envio)
SELECT id_usuario, 'SISTEMA', 'Te damos la bienvenida a SaludGrau',
       'Desde aquí puedes reservar tus citas, ver tu ticket de atención y descargar tus documentos médicos.',
       current_timestamp - INTERVAL '2 days'
FROM usuario WHERE dni = '12345678';

INSERT INTO notificacion (id_usuario, tipo, titulo, mensaje)
SELECT p.id_usuario, 'RECORDATORIO', 'Recordatorio de tu cita',
       'Tienes una cita de Medicina General el ' || to_char(h.fecha, 'DD/MM/YYYY') || ' a las '
       || to_char(h.hora_inicio, 'HH24:MI') || '. Ticket ' || c.numero_ticket || '.'
FROM cita c
JOIN horario h ON h.id_horario = c.id_horario
JOIN paciente p ON p.id_paciente = c.id_paciente
WHERE c.numero_ticket = 'A-0154';

INSERT INTO notificacion (id_usuario, tipo, titulo, mensaje)
SELECT id_usuario, 'CITA', 'Agenda del día lista',
       'Tienes 3 citas asignadas para hoy. Revisa el panel para ver el detalle de cada paciente.'
FROM usuario WHERE dni = '87654321';


-- ============================================================
-- FIN DEL SCRIPT
-- ============================================================
