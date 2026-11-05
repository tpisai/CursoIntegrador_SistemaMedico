BEGIN;

-- ============================================================
-- 1. ROL (4 registros: catálogo realista)
-- ============================================================
INSERT INTO rol (id_rol, nombre, descripcion) VALUES
(1, 'ADMINISTRADOR',  'Gestiona usuarios, catálogos y configuración del sistema'),
(2, 'DOCTOR',         'Personal médico que atiende citas y consultas'),
(3, 'PACIENTE',       'Usuario que reserva y consulta sus citas médicas'),
(4, 'RECEPCIONISTA',  'Personal de admisión que apoya en la gestión de citas');

-- ============================================================
-- 2. ESPECIALIDAD (20 registros)
-- ============================================================
INSERT INTO especialidad (id_especialidad, nombre, descripcion, estado) VALUES
(1,  'Medicina General',               'Atención primaria y evaluación general de salud', 'ACTIVO'),
(2,  'Medicina Familiar',              'Atención integral de la persona y su familia', 'ACTIVO'),
(3,  'Cirugía General',                'Evaluación y procedimientos de cirugía menor', 'ACTIVO'),
(4,  'Ginecología',                    'Salud reproductiva y prevención en la mujer', 'ACTIVO'),
(5,  'Pediatría',                      'Atención médica de niños y adolescentes', 'ACTIVO'),
(6,  'Obstetricia',                    'Control prenatal, parto y puerperio', 'ACTIVO'),
(7,  'Psicología',                     'Evaluación y apoyo en salud mental', 'ACTIVO'),
(8,  'Odontología',                    'Salud bucal y tratamientos dentales', 'ACTIVO'),
(9,  'Nutrición',                      'Evaluación nutricional y planes de alimentación', 'ACTIVO'),
(10, 'Control de Niño Sano',           'Control de crecimiento y desarrollo infantil (CRED)', 'ACTIVO'),
(11, 'Inmunizaciones',                 'Aplicación de vacunas según el esquema nacional', 'ACTIVO'),
(12, 'Ecografía',                      'Estudios de imagen por ultrasonido', 'ACTIVO'),
(13, 'Ecografía Gineco-Obstétrica',    'Ecografías ginecológicas y obstétricas', 'ACTIVO'),
(14, 'Radiología (Rayos X)',           'Estudios radiográficos', 'ACTIVO'),
(15, 'Telesalud',                      'Atención y seguimiento a distancia', 'ACTIVO'),
(16, 'Medicina de Emergencia',         'Atención inmediata de urgencias y emergencias', 'ACTIVO'),
(17, 'Cardiología',                    'Enfermedades del corazón (no programada actualmente)', 'INACTIVO'),
(18, 'Dermatología',                   'Enfermedades de la piel (no programada actualmente)', 'INACTIVO'),
(19, 'Traumatología',                  'Lesiones músculo-esqueléticas (no programada actualmente)', 'INACTIVO'),
(20, 'Oftalmología',                   'Salud visual (no programada actualmente)', 'INACTIVO');

-- ============================================================
-- 3. CONSULTORIO (20 registros)
-- ============================================================
INSERT INTO consultorio (id_consultorio, nombre, zona, piso, numero, estado) VALUES
(1, 'Emergencia - Medicina General', 'Emergencia', NULL, 'EM-01', 'ACTIVO'),
(2, 'Emergencia - Gineco-Obstetricia', 'Emergencia', NULL, 'EM-02', 'ACTIVO'),
(3, 'Medicina General 1', 'Consultorios Externos', NULL, 'CE-01', 'ACTIVO'),
(4, 'Medicina General 2', 'Consultorios Externos', NULL, 'CE-02', 'ACTIVO'),
(5, 'Medicina General - Cirugía', 'Consultorios Externos', NULL, 'CE-03', 'ACTIVO'),
(6, 'Medicina Familiar', 'Consultorios Externos', NULL, 'CE-04', 'ACTIVO'),
(7, 'Ginecología', 'Consultorios Externos', NULL, 'CE-05', 'ACTIVO'),
(8, 'Pediatría', 'Consultorios Externos', NULL, 'CE-06', 'ACTIVO'),
(9, 'Obstetricia', 'Consultorios Externos', NULL, 'CE-07', 'ACTIVO'),
(10, 'Psicología', 'Consultorios Externos', NULL, 'CE-08', 'ACTIVO'),
(11, 'Odontología 1', 'Consultorios Externos', NULL, 'CE-09', 'ACTIVO'),
(12, 'Odontología 2', 'Consultorios Externos', NULL, 'CE-10', 'ACTIVO'),
(13, 'Nutrición', 'Consultorios Externos', NULL, 'CE-11', 'ACTIVO'),
(14, 'Control de Niño Sano', 'Consultorios Externos', NULL, 'CE-12', 'ACTIVO'),
(15, 'Inmunizaciones', 'Consultorios Externos', NULL, 'CE-13', 'ACTIVO'),
(16, 'Ecografía', 'Consultorios Externos', NULL, 'CE-14', 'ACTIVO'),
(17, 'Ecografía Gineco-Obstétrica', 'Consultorios Externos', NULL, 'CE-15', 'ACTIVO'),
(18, 'Rayos X', 'Consultorios Externos', NULL, 'CE-16', 'ACTIVO'),
(19, 'Telesalud 1', 'Consultorios Externos', NULL, 'CE-17', 'ACTIVO'),
(20, 'Telesalud 2', 'Consultorios Externos', NULL, 'CE-18', 'INACTIVO');

-- ============================================================
-- 4. USUARIO (40 registros: 20 doctores [id 1-20] y 20 pacientes [id 21-40])
-- ============================================================
INSERT INTO usuario
(id_usuario, id_rol, dni, nombres, apellidos, correo, telefono, pin_hash, estado, fecha_registro, terminos_aceptados, bloqueado_hasta, intentos_fallidos) VALUES
-- Doctores (rol 2)
(1,  2, '08234561', 'Carlos Alberto',     'Mendoza Rojas',       'carlos.mendoza@clinica-ejemplo.pe',   '987100101', '$2b$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ACTIVO',    '2026-01-12 08:15:00', TRUE, NULL, 0),
(2,  2, '09345672', 'María Fernanda',     'Quispe Huamán',       'maria.quispe@clinica-ejemplo.pe',     '987100102', '$2b$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ACTIVO',    '2026-01-12 08:40:00', TRUE, NULL, 0),
(3,  2, '10456783', 'Luis Enrique',       'Paredes Salazar',     'luis.paredes@clinica-ejemplo.pe',     '987100103', '$2b$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ACTIVO',    '2026-01-13 09:05:00', TRUE, NULL, 1),
(4,  2, '11567894', 'Ana Lucía',          'Torres Vargas',       'ana.torres@clinica-ejemplo.pe',       '987100104', '$2b$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ACTIVO',    '2026-01-13 09:30:00', TRUE, NULL, 0),
(5,  2, '12678905', 'Jorge Luis',         'Castillo Ramos',      'jorge.castillo@clinica-ejemplo.pe',   '987100105', '$2b$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ACTIVO',    '2026-01-14 10:00:00', TRUE, NULL, 0),
(6,  2, '13789016', 'Rosa Elena',         'Flores Gutiérrez',    'rosa.flores@clinica-ejemplo.pe',      '987100106', '$2b$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ACTIVO',    '2026-01-14 10:25:00', TRUE, NULL, 0),
(7,  2, '14890127', 'Miguel Ángel',       'Chávez Soto',         'miguel.chavez@clinica-ejemplo.pe',    '987100107', '$2b$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'BLOQUEADO', '2026-01-15 08:50:00', TRUE, '2026-11-05 08:00:00', 5),
(8,  2, '15901238', 'Patricia Isabel',   'Rivera Cárdenas',     'patricia.rivera@clinica-ejemplo.pe',  '987100108', '$2b$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ACTIVO',    '2026-01-15 09:20:00', TRUE, NULL, 0),
(9,  2, '16012349', 'Ricardo Manuel',     'Delgado Pinedo',      'ricardo.delgado@clinica-ejemplo.pe',  '987100109', '$2b$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ACTIVO',    '2026-01-16 11:10:00', TRUE, NULL, 0),
(10, 2, '17123450', 'Claudia Verónica',   'Herrera Núñez',       'claudia.herrera@clinica-ejemplo.pe',  '987100110', '$2b$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ACTIVO',    '2026-01-16 11:45:00', TRUE, NULL, 2),
(11, 2, '18234561', 'Fernando José',      'Ríos Medina',         'fernando.rios@clinica-ejemplo.pe',    '987100111', '$2b$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ACTIVO',    '2026-01-19 08:30:00', TRUE, NULL, 0),
(12, 2, '19345672', 'Gabriela Sofía',     'León Aguilar',        'gabriela.leon@clinica-ejemplo.pe',    '987100112', '$2b$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ACTIVO',    '2026-01-19 09:00:00', TRUE, NULL, 0),
(13, 2, '20456783', 'Víctor Hugo',        'Campos Zevallos',     'victor.campos@clinica-ejemplo.pe',    '987100113', '$2b$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ACTIVO',    '2026-01-20 10:15:00', TRUE, NULL, 0),
(14, 2, '21567894', 'Daniela Alejandra',  'Vega Cornejo',        'daniela.vega@clinica-ejemplo.pe',     '987100114', '$2b$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ACTIVO',    '2026-01-20 10:50:00', TRUE, NULL, 0),
(15, 2, '22678905', 'Raúl Eduardo',       'Palomino Bravo',      'raul.palomino@clinica-ejemplo.pe',    '987100115', '$2b$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'INACTIVO',  '2026-01-21 08:20:00', TRUE, NULL, 0),
(16, 2, '23789016', 'Silvia Marlene',     'Ortiz Villanueva',    'silvia.ortiz@clinica-ejemplo.pe',     '987100116', '$2b$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ACTIVO',    '2026-01-21 09:35:00', TRUE, NULL, 0),
(17, 2, '24890127', 'Andrés Felipe',      'Cabrera Lazo',        'andres.cabrera@clinica-ejemplo.pe',   '987100117', '$2b$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ACTIVO',    '2026-01-22 11:00:00', TRUE, NULL, 0),
(18, 2, '25901238', 'Lucía Beatriz',      'Sandoval Perea',      'lucia.sandoval@clinica-ejemplo.pe',   '987100118', '$2b$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ACTIVO',    '2026-01-22 11:30:00', TRUE, NULL, 0),
(19, 2, '26012349', 'Héctor Daniel',      'Guerrero Maldonado',  'hector.guerrero@clinica-ejemplo.pe',  '987100119', '$2b$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ACTIVO',    '2026-01-23 08:45:00', TRUE, NULL, 0),
(20, 2, '27123450', 'Elena Patricia',     'Valdivia Ccori',      'elena.valdivia@clinica-ejemplo.pe',   '987100120', '$2b$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ACTIVO',    '2026-01-23 09:10:00', TRUE, NULL, 0),
-- Pacientes (rol 3)
(21, 3, '40123456', 'José Antonio',       'Ramírez Cueva',       'jose.ramirez@gmail.com',              '986200201', '$2b$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ACTIVO',    '2026-02-02 14:10:00', TRUE, NULL, 0),
(22, 3, '41234567', 'Camila Andrea',      'Gómez Rivas',         'camila.gomez@hotmail.com',            '986200202', '$2b$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ACTIVO',    '2026-02-03 16:25:00', TRUE, NULL, 0),
(23, 3, '42345678', 'Diego Alonso',       'Fernández Ochoa',     'diego.fernandez@gmail.com',           '986200203', '$2b$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ACTIVO',    '2026-02-05 10:40:00', TRUE, NULL, 0),
(24, 3, '43456789', 'Valeria Nicole',     'Espinoza Tello',      'valeria.espinoza@outlook.com',        '986200204', '$2b$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ACTIVO',    '2026-02-06 12:05:00', TRUE, NULL, 1),
(25, 3, '44567890', 'Juan Carlos',        'Villalobos Peña',     'juan.villalobos@gmail.com',           '986200205', '$2b$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ACTIVO',    '2026-02-09 09:55:00', TRUE, NULL, 0),
(26, 3, '45678901', 'Mónica Beatriz',     'Salas Arce',          'monica.salas@yahoo.com',              '986200206', '$2b$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ACTIVO',    '2026-02-10 15:30:00', TRUE, NULL, 0),
(27, 3, '46789012', 'Sebastián Ignacio',  'Cruz Mamani',         'sebastian.cruz@gmail.com',            '986200207', '$2b$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ACTIVO',    '2026-02-12 11:20:00', TRUE, NULL, 0),
(28, 3, '47890123', 'Karen Jimena',       'Poma Bellido',        'karen.poma@hotmail.com',              '986200208', '$2b$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ACTIVO',    '2026-02-13 17:00:00', TRUE, NULL, 0),
(29, 3, '48901234', 'Pedro Pablo',        'Zamora Huerta',       'pedro.zamora@gmail.com',              '986200209', '$2b$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ACTIVO',    '2026-02-16 08:35:00', TRUE, NULL, 0),
(30, 3, '49012345', 'Nancy Roxana',       'Acuña Dávila',        'nancy.acuna@outlook.com',             '986200210', '$2b$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ACTIVO',    '2026-02-17 13:45:00', TRUE, NULL, 0),
(31, 3, '50123456', 'Alex Brayan',        'Ccopa Tito',          'alex.ccopa@gmail.com',                '986200211', '$2b$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ACTIVO',    '2026-02-19 10:10:00', TRUE, NULL, 0),
(32, 3, '51234567', 'Fiorella Milagros',  'Navarro Ibáñez',      'fiorella.navarro@hotmail.com',        '986200212', '$2b$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ACTIVO',    '2026-02-20 16:50:00', TRUE, NULL, 0),
(33, 3, '52345678', 'Walter Gustavo',     'Reyes Linares',       'walter.reyes@yahoo.com',              '986200213', '$2b$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'BLOQUEADO', '2026-02-23 09:15:00', TRUE, '2026-11-06 12:00:00', 5),
(34, 3, '53456789', 'Jessica Paola',      'Ayala Sifuentes',     'jessica.ayala@gmail.com',             '986200214', '$2b$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ACTIVO',    '2026-02-24 14:20:00', TRUE, NULL, 0),
(35, 3, '54567890', 'Óscar Rodrigo',      'Tapia Cáceres',       'oscar.tapia@outlook.com',             '986200215', '$2b$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ACTIVO',    '2026-02-26 11:40:00', TRUE, NULL, 2),
(36, 3, '55678901', 'Liliana Carmen',     'Montes Barrios',      'liliana.montes@gmail.com',            '986200216', '$2b$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ACTIVO',    '2026-03-02 15:05:00', TRUE, NULL, 0),
(37, 3, '56789012', 'Franco Nicolás',     'Medrano Lozano',      'franco.medrano@hotmail.com',          '986200217', '$2b$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ACTIVO'  ,  '2026-03-03 10:30:00', TRUE, NULL, 0),
(38, 3, '57890123', 'Adriana Rocío',      'Bustamante Pacheco',  'adriana.bustamante@gmail.com',        '986200218', '$2b$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ACTIVO',    '2026-03-05 12:55:00', TRUE, NULL, 0),
(39, 3, '58901234', 'Marco Antonio',      'Arévalo Quiroz',      'marco.arevalo@yahoo.com',             '986200219', '$2b$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ACTIVO',    '2026-03-06 17:15:00', TRUE, NULL, 0),
(40, 3, '59012345', 'Teresa del Pilar',   'Salinas Obregón',     'teresa.salinas@outlook.com',          '986200220', '$2b$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ACTIVO',    '2026-03-09 09:25:00', TRUE, NULL, 0);

-- ============================================================
-- 5. DOCTOR (20 registros, usuarios 1-20)
-- ============================================================
INSERT INTO doctor (id_doctor, id_usuario, id_especialidad, cmp, estado) VALUES
(1, 1, 1, 'CMP-045120', 'ACTIVO'),
(2, 2, 1, 'CMP-051873', 'ACTIVO'),
(3, 3, 3, 'CMP-048265', 'ACTIVO'),
(4, 4, 2, 'CMP-056391', 'ACTIVO'),
(5, 5, 4, 'CMP-043718', 'ACTIVO'),
(6, 6, 5, 'CMP-060254', 'ACTIVO'),
(7, 7, 6, 'CMP-049682', 'ACTIVO'),
(8, 8, 7, 'CMP-057940', 'ACTIVO'),
(9, 9, 8, 'CMP-052136', 'ACTIVO'),
(10, 10, 8, 'CMP-061477', 'ACTIVO'),
(11, 11, 9, 'CMP-046803', 'ACTIVO'),
(12, 12, 10, 'CMP-058519', 'ACTIVO'),
(13, 13, 11, 'CMP-050942', 'ACTIVO'),
(14, 14, 12, 'CMP-063288', 'ACTIVO'),
(15, 15, 13, 'CMP-041056', 'INACTIVO'),
(16, 16, 14, 'CMP-054725', 'ACTIVO'),
(17, 17, 15, 'CMP-059361', 'ACTIVO'),
(18, 18, 16, 'CMP-047894', 'ACTIVO'),
(19, 19, 6, 'CMP-062013', 'ACTIVO'),
(20, 20, 15, 'CMP-055647', 'ACTIVO');

-- ============================================================
-- 6. PACIENTE (20 registros, usuarios 21-40)
-- ============================================================
INSERT INTO paciente
(id_paciente, id_usuario, fecha_nacimiento, sexo, direccion, grupo_sanguineo, contacto_emergencia, telefono_emergencia) VALUES
(1,  21, '1985-03-14', 'MASCULINO', 'Av. Arequipa 1450, Lince',               'O+',  'Rosa Cueva Díaz',          '985300301'),
(2,  22, '1992-07-22', 'FEMENINO',  'Jr. Cusco 234, Miraflores',              'A+',  'Luis Gómez Paredes',       '985300302'),
(3,  23, '1978-11-05', 'MASCULINO', 'Calle Los Pinos 567, San Borja',         'B+',  'Marta Ochoa Silva',        '985300303'),
(4,  24, '2001-01-30', 'FEMENINO',  'Av. La Marina 2310, San Miguel',         'O-',  'Carmen Tello Ruiz',        '985300304'),
(5,  25, '1969-09-18', 'MASCULINO', 'Jr. Huancavelica 890, Cercado de Lima',  'A-',  'Elena Peña Torres',        '985300305'),
(6,  26, '1988-05-09', 'FEMENINO',  'Av. Javier Prado Este 3120, La Molina',  'AB+', 'Pablo Salas Vega',         '985300306'),
(7,  27, '1995-12-02', 'MASCULINO', 'Calle Las Flores 145, Surco',            'O+',  'Ana Mamani Quispe',        '985300307'),
(8,  28, '1990-04-27', 'FEMENINO',  'Av. Universitaria 1875, Los Olivos',     'B-',  'Julio Bellido Cano',       '985300308'),
(9,  29, '1974-08-11', 'MASCULINO', 'Jr. Ayacucho 320, Breña',                'A+',  'Silvia Huerta Lara',       '985300309'),
(10, 30, '1983-02-19', 'FEMENINO',  'Av. Brasil 2680, Jesús María',           'O+',  'Roberto Acuña Mejía',      '985300310'),
(11, 31, '2019-06-15', 'MASCULINO', 'Av. Túpac Amaru 4250, Independencia',    'AB-', 'Gladys Tito Flores',       '985300311'),
(12, 32, '1997-10-08', 'FEMENINO',  'Calle Schell 410, Miraflores',           'A+',  'Hugo Navarro Cruz',        '985300312'),
(13, 33, '1965-12-24', 'MASCULINO', 'Av. Angamos Oeste 1120, Surquillo',      'B+',  'Mirtha Linares Gil',       '985300313'),
(14, 34, '1986-03-03', 'FEMENINO',  'Jr. Puno 765, Barranco',                 'O+',  'César Sifuentes Rojas',    '985300314'),
(15, 35, '1979-07-29', 'MASCULINO', 'Av. Salaverry 2890, San Isidro',         'A-',  'Patricia Cáceres León',    '985300315'),
(16, 36, '1993-11-17', 'FEMENINO',  'Av. Colonial 1530, Callao',              'O-',  'Mario Montes Ibarra',      '985300316'),
(17, 37, '2025-02-10', 'MASCULINO', 'Calle Berlín 275, Miraflores',           'B+',  'Luz Lozano Prado',         '985300317'),
(18, 38, '1991-01-21', 'FEMENINO',  'Av. Benavides 4100, Surco',              'AB+', 'Jaime Pacheco Ríos',       '985300318'),
(19, 39, '1972-05-13', 'MASCULINO', 'Jr. Lampa 540, Cercado de Lima',         'O+',  'Norma Quiroz Vera',        '985300319'),
(20, 40, '1999-08-25', NULL,        'Av. Los Héroes 980, San Juan de Miraflores', NULL, 'Julia Obregón Castro',    '985300320');

-- ============================================================
-- 7. HORARIO (20 registros)
-- Coherencia con cita:
--   ATENDIDA / NO_ASISTIO  -> FINALIZADO
--   RESERVADA / CONFIRMADA -> RESERVADO
--   CANCELADA              -> DISPONIBLE (el horario se libera)
-- ============================================================
INSERT INTO horario (id_horario, id_doctor, id_consultorio, fecha, hora_inicio, hora_fin, estado) VALUES
(1, 1, 3, '2026-10-26', '08:00', '08:15', 'FINALIZADO'),
(2, 1, 3, '2026-10-26', '08:15', '08:30', 'FINALIZADO'),
(3, 2, 4, '2026-10-27', '09:00', '09:15', 'FINALIZADO'),
(4, 3, 5, '2026-10-27', '09:15', '09:30', 'FINALIZADO'),
(5, 4, 6, '2026-10-28', '10:00', '10:15', 'FINALIZADO'),
(6, 5, 7, '2026-10-28', '10:15', '10:30', 'FINALIZADO'),
(7, 6, 8, '2026-10-29', '11:00', '11:15', 'FINALIZADO'),
(8, 7, 9, '2026-10-30', '14:00', '14:15', 'FINALIZADO'),
(9, 8, 10, '2026-11-02', '08:30', '08:45', 'FINALIZADO'),
(10, 9, 11, '2026-11-03', '09:30', '09:45', 'FINALIZADO'),
(11, 10, 12, '2026-11-05', '08:00', '08:15', 'RESERVADO'),
(12, 11, 13, '2026-11-05', '08:15', '08:30', 'RESERVADO'),
(13, 12, 14, '2026-11-06', '09:00', '09:15', 'RESERVADO'),
(14, 13, 15, '2026-11-06', '09:15', '09:30', 'RESERVADO'),
(15, 14, 16, '2026-11-09', '10:00', '10:15', 'RESERVADO'),
(16, 16, 18, '2026-11-09', '10:15', '10:30', 'RESERVADO'),
(17, 17, 19, '2026-11-10', '11:00', '11:15', 'RESERVADO'),
(18, 18, 1, '2026-11-10', '11:15', '11:30', 'DISPONIBLE'),
(19, 19, 2, '2026-11-11', '15:00', '15:15', 'DISPONIBLE'),
(20, 20, 19, '2026-11-12', '16:00', '16:15', 'RESERVADO');

-- ============================================================
-- 8. CITA (20 registros)
-- ============================================================
INSERT INTO cita (id_cita, id_paciente, id_horario, fecha_creacion, motivo, estado, numero_ticket) VALUES
(1, 1, 1, '2026-10-20 09:10:00', 'Chequeo médico general anual', 'ATENDIDA', 'TKT-2026-0001'),
(2, 2, 2, '2026-10-20 10:25:00', 'Dolor de garganta y fiebre leve', 'ATENDIDA', 'TKT-2026-0002'),
(3, 3, 3, '2026-10-21 11:00:00', 'Control de presión arterial y malestar general', 'ATENDIDA', 'TKT-2026-0003'),
(4, 4, 4, '2026-10-21 15:40:00', 'Evaluación de bulto en la espalda (consulta quirúrgica)', 'NO_ASISTIO', 'TKT-2026-0004'),
(5, 5, 5, '2026-10-22 08:50:00', 'Control integral de salud', 'ATENDIDA', 'TKT-2026-0005'),
(6, 6, 6, '2026-10-22 16:05:00', 'Control ginecológico preventivo', 'ATENDIDA', 'TKT-2026-0006'),
(7, 11, 7, '2026-10-23 09:30:00', 'Control pediátrico de rutina', 'ATENDIDA', 'TKT-2026-0007'),
(8, 8, 8, '2026-10-23 12:15:00', 'Control prenatal', 'NO_ASISTIO', 'TKT-2026-0008'),
(9, 9, 9, '2026-10-26 10:45:00', 'Evaluación por estrés y problemas de sueño', 'ATENDIDA', 'TKT-2026-0009'),
(10, 10, 10, '2026-10-27 14:20:00', 'Limpieza dental y revisión de caries', 'ATENDIDA', 'TKT-2026-0010'),
(11, 7, 11, '2026-10-30 09:00:00', 'Dolor de muela y evaluación para extracción', 'CONFIRMADA', 'TKT-2026-0011'),
(12, 12, 12, '2026-10-30 11:35:00', 'Plan de alimentación y control de peso', 'CONFIRMADA', 'TKT-2026-0012'),
(13, 17, 13, '2026-10-31 08:25:00', 'Control de crecimiento y desarrollo del niño', 'RESERVADA', 'TKT-2026-0013'),
(14, 14, 14, '2026-10-31 13:10:00', 'Aplicación de vacuna contra la influenza', 'RESERVADA', 'TKT-2026-0014'),
(15, 15, 15, '2026-11-02 10:00:00', 'Ecografía abdominal por molestias estomacales', 'RESERVADA', 'TKT-2026-0015'),
(16, 16, 16, '2026-11-02 15:30:00', 'Radiografía de tórax por tos persistente', 'CONFIRMADA', 'TKT-2026-0016'),
(17, 13, 17, '2026-11-03 09:45:00', 'Seguimiento de tratamiento por teleconsulta', 'RESERVADA', 'TKT-2026-0017'),
(18, 19, 18, '2026-11-03 12:05:00', 'Consulta por fiebre persistente', 'CANCELADA', 'TKT-2026-0018'),
(19, 18, 19, '2026-11-03 16:40:00', NULL, 'CANCELADA', 'TKT-2026-0019'),
(20, 20, 20, '2026-11-04 08:15:00', 'Seguimiento postoperatorio por teleconsulta', 'RESERVADA', 'TKT-2026-0020');

-- ============================================================
-- Sincronizar las secuencias SERIAL (los INSERT usaron ids explícitos)
-- ============================================================
SELECT setval(pg_get_serial_sequence('rol', 'id_rol'),                   (SELECT MAX(id_rol) FROM rol));
SELECT setval(pg_get_serial_sequence('especialidad', 'id_especialidad'), (SELECT MAX(id_especialidad) FROM especialidad));
SELECT setval(pg_get_serial_sequence('consultorio', 'id_consultorio'),   (SELECT MAX(id_consultorio) FROM consultorio));
SELECT setval(pg_get_serial_sequence('usuario', 'id_usuario'),           (SELECT MAX(id_usuario) FROM usuario));
SELECT setval(pg_get_serial_sequence('doctor', 'id_doctor'),             (SELECT MAX(id_doctor) FROM doctor));
SELECT setval(pg_get_serial_sequence('paciente', 'id_paciente'),         (SELECT MAX(id_paciente) FROM paciente));
SELECT setval(pg_get_serial_sequence('horario', 'id_horario'),           (SELECT MAX(id_horario) FROM horario));
SELECT setval(pg_get_serial_sequence('cita', 'id_cita'),                 (SELECT MAX(id_cita) FROM cita));

COMMIT;

-- ============================================================
-- VERIFICACIÓN: cantidad de registros por tabla
-- Esperado: rol=4, especialidad=20, consultorio=20, usuario=40,
--           doctor=20, paciente=20, horario=20, cita=20
-- ============================================================
SELECT 'rol'          AS tabla, COUNT(*) AS total FROM rol
UNION ALL SELECT 'especialidad', COUNT(*) FROM especialidad
UNION ALL SELECT 'consultorio',  COUNT(*) FROM consultorio
UNION ALL SELECT 'usuario',      COUNT(*) FROM usuario
UNION ALL SELECT 'doctor',       COUNT(*) FROM doctor
UNION ALL SELECT 'paciente',     COUNT(*) FROM paciente
UNION ALL SELECT 'horario',      COUNT(*) FROM horario
UNION ALL SELECT 'cita',         COUNT(*) FROM cita;

-- Conteos individuales (por si prefieres ejecutarlos uno a uno)
SELECT COUNT(*) FROM rol;
SELECT COUNT(*) FROM especialidad;
SELECT COUNT(*) FROM consultorio;
SELECT COUNT(*) FROM usuario;
SELECT COUNT(*) FROM doctor;
SELECT COUNT(*) FROM paciente;
SELECT COUNT(*) FROM horario;
SELECT COUNT(*) FROM cita;

-- Coherencia entre cita y horario (debe devolver 0 filas)
SELECT c.id_cita, c.estado AS estado_cita, h.estado AS estado_horario
FROM cita c
JOIN horario h ON h.id_horario = c.id_horario
WHERE NOT (
       (c.estado IN ('ATENDIDA', 'NO_ASISTIO')  AND h.estado = 'FINALIZADO')
    OR (c.estado IN ('RESERVADA', 'CONFIRMADA') AND h.estado = 'RESERVADO')
    OR (c.estado = 'CANCELADA'                  AND h.estado IN ('DISPONIBLE', 'CANCELADO'))
);

-- Doctores y pacientes con el rol correcto (debe devolver 0 filas)
SELECT d.id_doctor FROM doctor d JOIN usuario u ON u.id_usuario = d.id_usuario
WHERE u.id_rol <> (SELECT id_rol FROM rol WHERE nombre = 'DOCTOR');
SELECT p.id_paciente FROM paciente p JOIN usuario u ON u.id_usuario = p.id_usuario
WHERE u.id_rol <> (SELECT id_rol FROM rol WHERE nombre = 'PACIENTE');
