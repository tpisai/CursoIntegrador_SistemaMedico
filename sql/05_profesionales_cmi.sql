-- ============================================================
-- BASE DE DATOS: SLGR
-- Script 05 · Profesionales del cartel "Programación del día"
-- del CMI Miguel Grau, con sus especialidades y consultorios.
-- Se puede ejecutar más de una vez sobre una base existente.
--
-- Los nombres vienen del cartel. El DNI, el correo y la colegiatura
-- son datos de prueba; todas estas cuentas usan el PIN 654321.
-- ============================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;

INSERT INTO especialidad (nombre, descripcion) VALUES
    ('Medicina Familiar', 'Atención integral de la persona y la familia'),
    ('Ginecología', 'Salud de la mujer y consulta ginecológica'),
    ('Psicología', 'Evaluación y consejería en salud mental'),
    ('Control de Niño Sano', 'Control de crecimiento y desarrollo infantil'),
    ('Inmunizaciones', 'Vacunación según el esquema nacional'),
    ('Ecografía', 'Diagnóstico por ecografía'),
    ('Rayos X', 'Diagnóstico por imágenes de rayos X'),
    ('Telesalud', 'Atención y teleconsulta a distancia')
ON CONFLICT (nombre) DO NOTHING;

INSERT INTO consultorio (nombre, zona)
SELECT v.nombre, 'Consultorios externos'
FROM (VALUES
    ('Medicina General – Cirugía'),
    ('Medicina Familiar'),
    ('Ginecología'),
    ('Pediatría'),
    ('Obstetricia'),
    ('Psicología'),
    ('Odontología 1'),
    ('Odontología 2'),
    ('Nutrición'),
    ('Control de Niño Sano'),
    ('Inmunizaciones'),
    ('Ecografía'),
    ('Rayos X'),
    ('Telesalud 1')
) AS v(nombre)
WHERE NOT EXISTS (
    SELECT 1 FROM consultorio c
    WHERE c.zona = 'Consultorios externos' AND c.nombre = v.nombre
);

-- El personal de emergencia no tiene consultorio: atiende sin cita,
-- así que no aparece en la reserva en línea.
CREATE TEMP TABLE profesional_cmi (
    dni TEXT,
    nombres TEXT,
    apellidos TEXT,
    correo TEXT,
    especialidad TEXT,
    consultorio TEXT
);

INSERT INTO profesional_cmi VALUES
    ('87654327', 'Carmela Violeta', 'Muñoz Salazar', 'carmela.munoz@saludgrau.pe', 'Medicina General', NULL),
    ('87654328', 'Rocío del Pilar', 'Clemente', 'rocio.clemente@saludgrau.pe', 'Obstetricia', NULL),
    ('87654329', 'Carmen Elisa', 'Reynoso Zeballos', 'carmen.reynoso@saludgrau.pe', 'Medicina General', 'Medicina General – Cirugía'),
    ('87654330', 'María', 'Cárcamo Spaginol', 'maria.carcamo@saludgrau.pe', 'Medicina Familiar', 'Medicina Familiar'),
    ('87654331', 'Mayder', 'Torres Contreras', 'mayder.torres@saludgrau.pe', 'Ginecología', 'Ginecología'),
    ('87654332', 'Patricia', 'Quiroz Soto', 'patricia.quiroz@saludgrau.pe', 'Pediatría', 'Pediatría'),
    ('87654333', 'Gaby', 'Huaca Esteban', 'gaby.huaca@saludgrau.pe', 'Obstetricia', 'Obstetricia'),
    ('87654334', 'Miguel Ángel', 'Hermitaño', 'miguel.hermitano@saludgrau.pe', 'Psicología', 'Psicología'),
    ('87654335', 'Ángel Moisés', 'Miranda Ramírez', 'angel.miranda@saludgrau.pe', 'Odontología', 'Odontología 1'),
    ('87654336', 'Daniel Lizardo', 'Alva Avellaneda', 'daniel.alva@saludgrau.pe', 'Odontología', 'Odontología 2'),
    ('87654337', 'Josué', 'Cóndor Quispe', 'josue.condor@saludgrau.pe', 'Nutrición', 'Nutrición'),
    ('87654338', 'Rut', 'Vicente Ataurima', 'rut.vicente@saludgrau.pe', 'Control de Niño Sano', 'Control de Niño Sano'),
    ('87654339', 'Rocío', 'Ortega Puente', 'rocio.ortega@saludgrau.pe', 'Inmunizaciones', 'Inmunizaciones'),
    ('87654340', 'Fernando', 'Tello Álvarez', 'fernando.tello@saludgrau.pe', 'Ecografía', 'Ecografía'),
    ('87654341', 'Cleofe Rossana', 'Mayta Magge', 'cleofe.mayta@saludgrau.pe', 'Rayos X', 'Rayos X'),
    ('87654342', 'Juan José', 'Gutiérrez Álvarez', 'juan.gutierrez@saludgrau.pe', 'Telesalud', 'Telesalud 1');

INSERT INTO usuario (id_rol, dni, nombres, apellidos, correo, pin_hash, terminos_aceptados)
SELECT r.id_rol, p.dni, p.nombres, p.apellidos, p.correo,
       crypt('654321', gen_salt('bf', 10)), TRUE
FROM profesional_cmi p
JOIN rol r ON r.nombre = 'DOCTOR'
ORDER BY p.dni
ON CONFLICT DO NOTHING;

INSERT INTO doctor (id_usuario, id_especialidad, cmp)
SELECT u.id_usuario, e.id_especialidad, 'POR-CONFIRMAR-' || p.dni
FROM profesional_cmi p
JOIN usuario u ON u.dni = p.dni AND u.correo = p.correo
JOIN especialidad e ON e.nombre = p.especialidad
ORDER BY p.dni
ON CONFLICT DO NOTHING;

INSERT INTO doctor_consultorio (id_doctor, id_consultorio)
SELECT d.id_doctor, c.id_consultorio
FROM profesional_cmi p
JOIN usuario u ON u.dni = p.dni AND u.correo = p.correo
JOIN doctor d ON d.id_usuario = u.id_usuario
JOIN consultorio c ON c.zona = 'Consultorios externos' AND c.nombre = p.consultorio
ON CONFLICT DO NOTHING;

DROP TABLE profesional_cmi;
