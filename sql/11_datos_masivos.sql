-- ============================================================
-- BASE DE DATOS: SLGR
-- Script 11 · Datos masivos de prueba.
-- Deja cada tabla del sistema con más de 75 registros para las
-- pruebas de reportes, filtros y paginación.
--
-- Se ejecuta después de los scripts 03 a 10. Es re-ejecutable:
-- si ya cargó (existe el usuario de DNI 70000001) no hace nada,
-- así instalar_bd.ps1 -Actualizar no falla ni duplica datos.
--
-- Cuentas nuevas (PIN guardado con BCrypt, igual que en el 03):
--   Pacientes       DNI 70000001 a 70000090  PIN 123456
--   Doctores        DNI 71000001 a 71000080  PIN 654321
--   Administración  DNI 72000001 a 72000080  PIN 445566
--
-- Nombres, DNI, colegiaturas y correos son inventados. Las fechas
-- se calculan desde el día en que se ejecuta el script.
--
-- Registros nuevos por tabla:
--   rol 80 · permiso 80 · cargo 80 · cargo_permiso 240
--   usuario 250 · paciente 90 · historia_clinica 90
--   personal_administrativo 80 · especialidad 80 · doctor 80
--   consultorio 80 · doctor_consultorio 80 · horario 200 · cita 200
--   atencion 120 · triaje 120 · orden_examen 90
--   documento_medico 120 · detalle_medicamento 240
--   resultado_examen 120 · campania 80 · inscripcion_campania 180
--   solicitud 90 · auditoria 120 · notificacion 450
--   recuperacion_pin 80 · anuncio 80
-- ============================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;

DO $$
DECLARE
    -- Nombres para armar las personas de prueba.
    v_nombres_f TEXT[] := ARRAY['María', 'Rosa', 'Carmen', 'Lucía', 'Ana', 'Sofía', 'Valeria', 'Gabriela',
        'Patricia', 'Elena', 'Milagros', 'Diana', 'Karina', 'Fiorella', 'Jessica', 'Silvia', 'Claudia',
        'Mónica', 'Paola', 'Andrea'];
    v_nombres_m TEXT[] := ARRAY['José', 'Luis', 'Carlos', 'Jorge', 'Miguel', 'Juan', 'Pedro', 'Ricardo',
        'Fernando', 'Raúl', 'Víctor', 'César', 'Alberto', 'Diego', 'Martín', 'Óscar', 'Manuel', 'Javier',
        'Hugo', 'Renato'];
    v_apellidos TEXT[] := ARRAY['Quispe', 'Huamán', 'Flores', 'Rojas', 'Mamani', 'García', 'Sánchez',
        'Ramírez', 'Torres', 'Chávez', 'Vargas', 'Castillo', 'Mendoza', 'Gutiérrez', 'Espinoza', 'Díaz',
        'Ramos', 'Cruz', 'Vásquez', 'Salazar', 'Paredes', 'Cárdenas', 'Palomino', 'Ccori', 'Ticona',
        'Condori', 'Romero', 'Aguilar', 'Silva', 'Medina', 'Herrera', 'Morales', 'Valverde', 'Cabrera',
        'Navarro', 'Zevallos', 'Poma', 'Inga', 'Apaza', 'Villanueva'];
    v_calles TEXT[] := ARRAY['Av. Nicolás Ayllón', 'Jr. Los Pinos', 'Calle Las Flores', 'Av. Perú',
        'Jr. Santa Rosa', 'Av. Huascata', 'Calle Los Olivos', 'Jr. Lima', 'Av. Miguel Grau',
        'Calle San Martín', 'Jr. Tacna', 'Av. Los Ángeles'];
    v_grupos TEXT[] := ARRAY['O+', 'O+', 'O+', 'A+', 'A+', 'B+', 'AB+', 'O-', 'A-', 'B-'];

    -- Catálogos.
    v_areas TEXT[] := ARRAY['LABORATORIO', 'FARMACIA', 'ENFERMERIA', 'ADMISION_CITAS', 'CAJA',
        'ARCHIVO_CLINICO', 'TRIAJE', 'EMERGENCIA', 'IMAGENES', 'ESTADISTICA', 'LOGISTICA', 'SISTEMAS',
        'CALIDAD', 'EPIDEMIOLOGIA', 'PROMOCION_SALUD', 'SALUD_MENTAL', 'NUTRICION', 'REHABILITACION',
        'SEGUROS_SIS', 'REFERENCIAS'];
    v_areas_texto TEXT[] := ARRAY['laboratorio', 'farmacia', 'enfermería', 'admisión de citas', 'caja',
        'archivo clínico', 'triaje', 'emergencia', 'diagnóstico por imágenes', 'estadística', 'logística',
        'sistemas', 'gestión de la calidad', 'epidemiología', 'promoción de la salud', 'salud mental',
        'nutrición', 'rehabilitación', 'seguros SIS', 'referencias y contrarreferencias'];
    v_niveles TEXT[] := ARRAY['SUPERVISOR', 'COORDINADOR', 'TECNICO', 'ASISTENTE'];
    v_niveles_texto TEXT[] := ARRAY['Supervisor', 'Coordinador', 'Técnico', 'Asistente'];
    v_modulos TEXT[] := ARRAY['CITAS', 'HORARIOS', 'PACIENTES', 'DOCTORES', 'ATENCIONES', 'TRIAJE',
        'RECETAS', 'ORDENES_EXAMEN', 'RESULTADOS', 'DOCUMENTOS', 'CAMPANIAS', 'ANUNCIOS', 'SOLICITUDES',
        'REPORTES', 'USUARIOS', 'AUDITORIA'];
    v_acciones TEXT[] := ARRAY['VER', 'CREAR', 'EDITAR', 'ELIMINAR', 'EXPORTAR'];
    v_acciones_texto TEXT[] := ARRAY['Consultar', 'Registrar', 'Modificar', 'Eliminar', 'Exportar'];

    v_especialidades TEXT[] := ARRAY['Cardiología', 'Dermatología', 'Endocrinología', 'Gastroenterología',
        'Neumología', 'Neurología', 'Nefrología', 'Reumatología', 'Oftalmología', 'Otorrinolaringología',
        'Traumatología', 'Urología', 'Psiquiatría', 'Geriatría', 'Hematología', 'Oncología', 'Infectología',
        'Alergología', 'Inmunología Clínica', 'Medicina Interna', 'Cirugía General', 'Cirugía Pediátrica',
        'Cirugía Plástica', 'Cirugía Cardiovascular', 'Cirugía de Tórax', 'Neurocirugía',
        'Cirugía Oncológica', 'Cirugía de Cabeza y Cuello', 'Proctología', 'Angiología',
        'Medicina Física y Rehabilitación', 'Terapia Física', 'Terapia de Lenguaje', 'Terapia Ocupacional',
        'Psicopedagogía', 'Neonatología', 'Neuropediatría', 'Cardiología Pediátrica',
        'Endocrinología Pediátrica', 'Neumología Pediátrica', 'Gastroenterología Pediátrica',
        'Odontopediatría', 'Ortodoncia', 'Endodoncia', 'Periodoncia', 'Cirugía Bucomaxilofacial',
        'Rehabilitación Oral', 'Medicina Deportiva', 'Medicina Ocupacional', 'Medicina Tropical',
        'Salud Pública', 'Planificación Familiar', 'Psicoprofilaxis Obstétrica', 'Estimulación Temprana',
        'Enfermería', 'Podología', 'Optometría', 'Audiología', 'Farmacia Clínica', 'Mamografía',
        'Tomografía', 'Resonancia Magnética', 'Densitometría Ósea', 'Electrocardiografía',
        'Anatomía Patológica', 'Medicina Nuclear', 'Endoscopía Digestiva', 'Espirometría',
        'Medicina del Dolor', 'Cuidados Paliativos', 'Medicina Alternativa', 'Acupuntura',
        'Medicina Estética', 'Sexología Clínica', 'Toxicología', 'Genética Médica', 'Mastología',
        'Flebología', 'Hepatología', 'Diabetología'];
    v_apoyo TEXT[] := ARRAY['Mamografía', 'Tomografía', 'Resonancia Magnética', 'Densitometría Ósea',
        'Electrocardiografía', 'Anatomía Patológica', 'Medicina Nuclear', 'Endoscopía Digestiva',
        'Espirometría'];

    v_campanias TEXT[] := ARRAY['Vacunación contra la influenza', 'Despistaje de anemia infantil',
        'Tamizaje de diabetes e hipertensión', 'Campaña de salud bucal', 'Charla de nutrición saludable',
        'Despistaje de cáncer de cuello uterino', 'Despistaje de cáncer de mama',
        'Campaña de agudeza visual', 'Tamizaje de salud mental', 'Vacunación contra el VPH',
        'Desparasitación escolar', 'Control de peso y talla', 'Prevención del dengue',
        'Planificación familiar', 'Despistaje de tuberculosis', 'Prueba rápida de VIH y sífilis',
        'Salud del adulto mayor', 'Lactancia materna', 'Prevención de la osteoporosis',
        'Primeros auxilios para la familia'];
    v_lugares TEXT[] := ARRAY['Patio principal del centro de salud', 'Auditorio principal',
        'Local comunal de Huascata', 'I.E. Miguel Grau', 'Parque central de Chaclacayo',
        'Mercado Morón', 'Local comunal de Ñaña', 'Losa deportiva de Santa Inés'];

    v_hash_paciente TEXT;
    v_hash_doctor TEXT;
    v_hash_admin TEXT;
    v_hash_codigo TEXT;

    i INTEGER;
    v_doctor INTEGER;
    v_consultorio INTEGER;
    v_paciente INTEGER;
    v_historia INTEGER;
    v_fecha DATE;
    v_hora TIME;
    v_estado TEXT;
    v_horario INTEGER;
    v_cita INTEGER;
BEGIN
    IF EXISTS (SELECT 1 FROM usuario WHERE dni = '70000001') THEN
        RAISE NOTICE 'Los datos masivos ya estaban cargados; no se insertó nada.';
        RETURN;
    END IF;

    -- Un hash por PIN: BCrypt es lento a propósito y el mismo hash sirve para todas las cuentas.
    v_hash_paciente := crypt('123456', gen_salt('bf', 10));
    v_hash_doctor := crypt('654321', gen_salt('bf', 10));
    v_hash_admin := crypt('445566', gen_salt('bf', 10));
    v_hash_codigo := crypt('482915', gen_salt('bf', 10));


    -- ========================================================
    -- CATÁLOGOS: rol, permiso, cargo, cargo_permiso
    -- ========================================================

    -- 20 áreas x 4 niveles = 80 roles.
    INSERT INTO rol (nombre, descripcion)
    SELECT v_niveles[n] || '_' || v_areas[a],
           v_niveles_texto[n] || ' del área de ' || v_areas_texto[a]
    FROM generate_series(1, 20) a, generate_series(1, 4) n
    ORDER BY a, n
    ON CONFLICT (nombre) DO NOTHING;

    -- 16 módulos x 5 acciones = 80 permisos.
    INSERT INTO permiso (nombre, descripcion)
    SELECT v_modulos[m] || '_' || v_acciones[x],
           v_acciones_texto[x] || ' en el módulo ' || lower(replace(v_modulos[m], '_', ' '))
    FROM generate_series(1, 16) m, generate_series(1, 5) x
    ORDER BY m, x
    ON CONFLICT (nombre) DO NOTHING;

    -- 20 áreas x 4 niveles = 80 cargos; uno de cada diez queda inactivo.
    INSERT INTO cargo (nombre, descripcion, estado)
    SELECT v_niveles_texto[n] || ' de ' || v_areas_texto[a],
           'Responsable de las tareas de ' || v_areas_texto[a] || ' en el nivel de '
               || lower(v_niveles_texto[n]),
           CASE WHEN (a * 4 + n) % 10 = 0 THEN 'INACTIVO' ELSE 'ACTIVO' END
    FROM generate_series(1, 20) a, generate_series(1, 4) n
    ORDER BY a, n
    ON CONFLICT (nombre) DO NOTHING;

    -- Cada cargo nuevo recibe 3 permisos (240 filas).
    INSERT INTO cargo_permiso (id_cargo, id_permiso)
    SELECT c.id_cargo, p.id_permiso
    FROM (SELECT id_cargo, row_number() OVER (ORDER BY id_cargo) AS k
          FROM cargo
          WHERE descripcion LIKE 'Responsable de las tareas de %') c
    CROSS JOIN generate_series(0, 2) s
    JOIN (SELECT id_permiso, row_number() OVER (ORDER BY id_permiso) AS k
          FROM permiso
          WHERE nombre ~ '_(VER|CREAR|EDITAR|ELIMINAR|EXPORTAR)$') p
      ON p.k = ((c.k - 1) * 3 + s * 7) % 80 + 1
    ON CONFLICT DO NOTHING;


    -- ========================================================
    -- ESPECIALIDADES Y CONSULTORIOS (80 de cada uno)
    -- ========================================================

    INSERT INTO especialidad (nombre, descripcion, tipo)
    SELECT e, 'Consulta especializada de ' || lower(e),
           CASE WHEN e = ANY (v_apoyo) THEN 'APOYO_DIAGNOSTICO' ELSE 'CONSULTA' END
    FROM unnest(v_especialidades) WITH ORDINALITY AS t(e, k)
    ORDER BY k
    ON CONFLICT (nombre) DO NOTHING;

    -- Torre de especialidades: 8 pisos con 10 consultorios cada uno.
    INSERT INTO consultorio (nombre, zona, piso, numero)
    SELECT 'Consultorio ' || k, 'Torre de especialidades', ((k - 1) / 10 + 1)::TEXT, k::TEXT
    FROM generate_series(1, 80) k
    WHERE NOT EXISTS (
        SELECT 1 FROM consultorio c
        WHERE c.zona = 'Torre de especialidades' AND c.numero = k::TEXT
    );


    -- ========================================================
    -- USUARIOS
    -- ========================================================

    -- 90 pacientes: DNI 70000001 a 70000090.
    INSERT INTO usuario (id_rol, dni, nombres, apellidos, correo, telefono, pin_hash, estado,
                         fecha_registro, terminos_aceptados)
    SELECT r.id_rol, (70000000 + k)::TEXT, p.nombre, p.apellidos,
           translate(lower(split_part(p.nombre, ' ', 1) || '.' || split_part(p.apellidos, ' ', 1)),
                     'áéíóúñü', 'aeiounu') || k || '@correo.com',
           (930000000 + k)::TEXT, v_hash_paciente,
           CASE WHEN k % 30 = 0 THEN 'INACTIVO' ELSE 'ACTIVO' END,
           current_timestamp - make_interval(days => 400 - k * 4), TRUE
    FROM generate_series(1, 90) k
    CROSS JOIN LATERAL (
        SELECT CASE WHEN k % 2 = 0 THEN v_nombres_f[(k * 7) % 20 + 1] ELSE v_nombres_m[(k * 7) % 20 + 1] END AS nombre,
               v_apellidos[(k * 3) % 40 + 1] || ' ' || v_apellidos[(k * 11 + 5) % 40 + 1] AS apellidos
    ) p
    JOIN rol r ON r.nombre = 'PACIENTE'
    ORDER BY k;

    -- 80 doctores: DNI 71000001 a 71000080.
    INSERT INTO usuario (id_rol, dni, nombres, apellidos, correo, telefono, pin_hash, fecha_registro,
                         terminos_aceptados)
    SELECT r.id_rol, (71000000 + k)::TEXT, p.nombre, p.apellidos,
           translate(lower(split_part(p.nombre, ' ', 1) || '.' || split_part(p.apellidos, ' ', 1)),
                     'áéíóúñü', 'aeiounu') || '.md' || k || '@saludgrau.pe',
           (940000000 + k)::TEXT, v_hash_doctor,
           current_timestamp - make_interval(days => 500 - k * 3), TRUE
    FROM generate_series(1, 80) k
    CROSS JOIN LATERAL (
        SELECT CASE WHEN k % 2 = 1 THEN v_nombres_f[(k * 3) % 20 + 1] ELSE v_nombres_m[(k * 3) % 20 + 1] END AS nombre,
               v_apellidos[(k * 7 + 2) % 40 + 1] || ' ' || v_apellidos[(k * 13 + 9) % 40 + 1] AS apellidos
    ) p
    JOIN rol r ON r.nombre = 'DOCTOR'
    ORDER BY k;

    -- 80 del personal administrativo: DNI 72000001 a 72000080.
    INSERT INTO usuario (id_rol, dni, nombres, apellidos, correo, telefono, pin_hash, fecha_registro,
                         terminos_aceptados)
    SELECT r.id_rol, (72000000 + k)::TEXT, p.nombre, p.apellidos,
           translate(lower(split_part(p.nombre, ' ', 1) || '.' || split_part(p.apellidos, ' ', 1)),
                     'áéíóúñü', 'aeiounu') || '.adm' || k || '@saludgrau.pe',
           (950000000 + k)::TEXT, v_hash_admin,
           current_timestamp - make_interval(days => 600 - k * 5), TRUE
    FROM generate_series(1, 80) k
    CROSS JOIN LATERAL (
        SELECT CASE WHEN k % 3 = 0 THEN v_nombres_m[(k * 9) % 20 + 1] ELSE v_nombres_f[(k * 9) % 20 + 1] END AS nombre,
               v_apellidos[(k * 17 + 3) % 40 + 1] || ' ' || v_apellidos[(k * 5 + 21) % 40 + 1] AS apellidos
    ) p
    JOIN rol r ON r.nombre = 'ADMINISTRADOR'
    ORDER BY k;


    -- ========================================================
    -- PACIENTES E HISTORIAS CLÍNICAS (90)
    -- ========================================================

    INSERT INTO paciente (id_usuario, fecha_nacimiento, sexo, direccion, grupo_sanguineo,
                          contacto_emergencia, telefono_emergencia)
    SELECT u.id_usuario,
           current_date - make_interval(years => 2 + (k * 7) % 78, days => (k * 37) % 365),
           CASE WHEN k % 2 = 0 THEN 'FEMENINO' ELSE 'MASCULINO' END,
           v_calles[k % 12 + 1] || ' ' || (100 + k * 13) || ', Chaclacayo',
           v_grupos[k % 10 + 1],
           v_nombres_f[(k * 5) % 20 + 1] || ' ' || split_part(u.apellidos, ' ', 1),
           (960000000 + k)::TEXT
    FROM generate_series(1, 90) k
    JOIN usuario u ON u.dni = (70000000 + k)::TEXT
    ORDER BY k;

    INSERT INTO historia_clinica (id_paciente, numero_historia, fecha_apertura, alergias, antecedentes,
                                  medicacion_habitual)
    SELECT p.id_paciente, 'HC-' || lpad(p.id_paciente::TEXT, 6, '0'), u.fecha_registro::DATE,
           (ARRAY['Ninguna conocida', 'Ninguna conocida', 'Penicilina', 'Sulfas', 'AINES', 'Mariscos'])[k % 6 + 1],
           (ARRAY['Sin antecedentes de importancia.', 'Hipertensión arterial en tratamiento.',
                  'Diabetes mellitus tipo 2.', 'Asma bronquial leve.', 'Gastritis crónica.',
                  'Cesárea hace 5 años.', 'Apendicectomía en la infancia.', 'Hipotiroidismo.'])[k % 8 + 1],
           (ARRAY['Ninguna', 'Losartán 50 mg cada 24 horas', 'Metformina 850 mg cada 12 horas', 'Ninguna',
                  'Omeprazol 20 mg en ayunas', 'Ninguna', 'Ninguna', 'Levotiroxina 50 µg en ayunas'])[k % 8 + 1]
    FROM generate_series(1, 90) k
    JOIN usuario u ON u.dni = (70000000 + k)::TEXT
    JOIN paciente p ON p.id_usuario = u.id_usuario
    ORDER BY k;


    -- ========================================================
    -- PERSONAL ADMINISTRATIVO (80): cada uno en un cargo nuevo
    -- ========================================================

    INSERT INTO personal_administrativo (id_usuario, id_cargo, estado)
    SELECT u.id_usuario, c.id_cargo, CASE WHEN k % 25 = 0 THEN 'INACTIVO' ELSE 'ACTIVO' END
    FROM generate_series(1, 80) k
    JOIN usuario u ON u.dni = (72000000 + k)::TEXT
    JOIN (SELECT id_cargo, row_number() OVER (ORDER BY id_cargo) AS n
          FROM cargo
          WHERE descripcion LIKE 'Responsable de las tareas de %') c ON c.n = k
    ORDER BY k;


    -- ========================================================
    -- DOCTORES (80): uno por especialidad nueva, cada uno con
    -- su consultorio en la torre de especialidades.
    -- ========================================================

    INSERT INTO doctor (id_usuario, id_especialidad, cmp)
    SELECT u.id_usuario, e.id_especialidad, lpad((70000 + k * 17)::TEXT, 6, '0')
    FROM generate_series(1, 80) k
    JOIN usuario u ON u.dni = (71000000 + k)::TEXT
    JOIN especialidad e ON e.nombre = v_especialidades[k]
    ORDER BY k;

    INSERT INTO doctor_consultorio (id_doctor, id_consultorio)
    SELECT d.id_doctor, c.id_consultorio
    FROM generate_series(1, 80) k
    JOIN usuario u ON u.dni = (71000000 + k)::TEXT
    JOIN doctor d ON d.id_usuario = u.id_usuario
    JOIN consultorio c ON c.zona = 'Torre de especialidades' AND c.numero = k::TEXT
    ON CONFLICT DO NOTHING;


    -- ========================================================
    -- CASOS CLÍNICOS Y MEDICAMENTOS DE REFERENCIA
    -- ========================================================

    CREATE TEMP TABLE tmp_caso (
        n INTEGER PRIMARY KEY,
        motivo TEXT,
        examen_fisico TEXT,
        diagnostico TEXT,
        tratamiento TEXT,
        examenes TEXT,
        tipo_examen TEXT,
        resultado TEXT,
        referencia TEXT
    ) ON COMMIT DROP;

    INSERT INTO tmp_caso VALUES
        (1, 'Dolor de garganta y fiebre', 'T 38.4 °C. Amígdalas hipertróficas con exudado.', 'Faringoamigdalitis aguda.',
         'Amoxicilina y paracetamol. Abundantes líquidos.', 'Hemograma completo', 'Leucocitos', '12 400 /µL', '4 500 a 11 000 /µL'),
        (2, 'Dolor de cabeza frecuente', 'PA 148/94 mmHg. Fondo de ojo sin alteraciones.', 'Hipertensión arterial estadio 1.',
         'Losartán diario y dieta baja en sal.', 'Perfil lipídico, creatinina', 'Colesterol total', '232 mg/dL', 'Menor de 200 mg/dL'),
        (3, 'Sed excesiva y pérdida de peso', 'IMC 31. Acantosis nigricans en cuello.', 'Diabetes mellitus tipo 2.',
         'Metformina y plan de alimentación.', 'Glucosa en ayunas, hemoglobina glicosilada', 'Glucosa en ayunas', '148 mg/dL', '70 a 100 mg/dL'),
        (4, 'Ardor al orinar', 'Puño percusión lumbar negativa. Afebril.', 'Infección del tracto urinario baja.',
         'Nitrofurantoína por 7 días.', 'Examen completo de orina, urocultivo', 'Leucocitos en orina', '25 por campo', '0 a 5 por campo'),
        (5, 'Dolor abdominal y acidez', 'Dolor a la palpación en epigastrio.', 'Gastritis aguda.',
         'Omeprazol en ayunas por 4 semanas.', 'Prueba de Helicobacter pylori', 'Antígeno de H. pylori', 'Positivo', 'Negativo'),
        (6, 'Tos con flema por 5 días', 'Roncantes difusos. Saturación 96 %.', 'Bronquitis aguda.',
         'Salbutamol inhalado y ambroxol.', 'Radiografía de tórax', 'Radiografía de tórax', 'Sin infiltrados', 'Normal'),
        (7, 'Cansancio y palidez', 'Palidez de piel y mucosas. FC 98 lpm.', 'Anemia ferropénica leve.',
         'Sulfato ferroso y dieta rica en hierro.', 'Hemoglobina, ferritina', 'Hemoglobina', '10.8 g/dL', '12.0 a 16.0 g/dL'),
        (8, 'Dolor lumbar', 'Contractura paravertebral. Lasègue negativo.', 'Lumbalgia mecánica.',
         'Naproxeno, calor local y terapia física.', 'Radiografía de columna lumbar', 'Radiografía lumbosacra', 'Rectificación de lordosis', 'Normal'),
        (9, 'Picazón y ronchas en la piel', 'Habones en tronco y brazos.', 'Urticaria aguda.',
         'Loratadina por 7 días. Evitar el alérgeno.', 'Hemograma con eosinófilos', 'Eosinófilos', '7 %', '1 a 4 %'),
        (10, 'Control preventivo anual', 'Examen físico sin alteraciones.', 'Paciente sano.',
         'Mantener actividad física y alimentación balanceada.', 'Hemograma, glucosa y perfil lipídico', 'Hemoglobina', '13.9 g/dL', '12.0 a 16.0 g/dL'),
        (11, 'Diarrea y vómitos', 'Mucosas secas. Ruidos hidroaéreos aumentados.', 'Gastroenteritis aguda.',
         'Sales de rehidratación oral y dieta blanda.', 'Coprológico funcional', 'Reacción inflamatoria en heces', 'Negativa', 'Negativa'),
        (12, 'Dolor de rodilla al caminar', 'Crepitación en rodilla derecha.', 'Gonartrosis grado II.',
         'Paracetamol, bajar de peso y ejercicios de fortalecimiento.', 'Radiografía de rodillas', 'Radiografía de rodilla', 'Disminución del espacio articular', 'Normal'),
        (13, 'Ansiedad y problemas para dormir', 'Paciente orientado, ansioso.', 'Trastorno de ansiedad generalizada.',
         'Psicoterapia y sertralina.', 'Hormonas tiroideas', 'TSH', '2.1 mUI/L', '0.4 a 4.0 mUI/L'),
        (14, 'Visión borrosa', 'Agudeza visual 20/40 en ambos ojos.', 'Miopía.',
         'Lentes correctores.', 'Glucosa en ayunas', 'Glucosa en ayunas', '92 mg/dL', '70 a 100 mg/dL'),
        (15, 'Dolor de oído', 'Membrana timpánica abombada y eritematosa.', 'Otitis media aguda.',
         'Amoxicilina por 10 días e ibuprofeno.', 'Hemograma completo', 'Leucocitos', '11 800 /µL', '4 500 a 11 000 /µL');

    CREATE TEMP TABLE tmp_medicamento (
        n INTEGER PRIMARY KEY,
        nombre TEXT,
        dosis TEXT,
        presentacion TEXT,
        via TEXT,
        unidades TEXT,
        frecuencia TEXT,
        duracion TEXT,
        cantidad TEXT
    ) ON COMMIT DROP;

    INSERT INTO tmp_medicamento VALUES
        (1, 'Amoxicilina', '500 mg', 'Cápsula', 'Oral', '1', 'Cada 8 horas', '7 días', '21'),
        (2, 'Paracetamol', '500 mg', 'Tableta', 'Oral', '1', 'Cada 8 horas si hay dolor o fiebre', '3 días', '9'),
        (3, 'Losartán', '50 mg', 'Tableta', 'Oral', '1', 'Cada 24 horas', '30 días', '30'),
        (4, 'Metformina', '850 mg', 'Tableta', 'Oral', '1', 'Cada 12 horas', '30 días', '60'),
        (5, 'Nitrofurantoína', '100 mg', 'Cápsula', 'Oral', '1', 'Cada 12 horas', '7 días', '14'),
        (6, 'Omeprazol', '20 mg', 'Cápsula', 'Oral', '1', 'Cada 24 horas en ayunas', '28 días', '28'),
        (7, 'Salbutamol', '100 µg', 'Inhalador', 'Inhalatoria', '2 puff', 'Cada 6 horas', '7 días', '1'),
        (8, 'Sulfato ferroso', '300 mg', 'Tableta', 'Oral', '1', 'Cada 24 horas', '90 días', '90'),
        (9, 'Naproxeno', '550 mg', 'Tableta', 'Oral', '1', 'Cada 12 horas', '5 días', '10'),
        (10, 'Loratadina', '10 mg', 'Tableta', 'Oral', '1', 'Cada 24 horas', '7 días', '7'),
        (11, 'Sales de rehidratación oral', '1 sobre', 'Sobre', 'Oral', '1', 'Después de cada deposición', '3 días', '6'),
        (12, 'Ibuprofeno', '400 mg', 'Tableta', 'Oral', '1', 'Cada 8 horas', '5 días', '15'),
        (13, 'Sertralina', '50 mg', 'Tableta', 'Oral', '1', 'Cada 24 horas', '30 días', '30'),
        (14, 'Ambroxol', '30 mg', 'Jarabe', 'Oral', '5 mL', 'Cada 8 horas', '7 días', '1'),
        (15, 'Ácido fólico', '0.5 mg', 'Tableta', 'Oral', '1', 'Cada 24 horas', '30 días', '30');


    -- ========================================================
    -- HORARIOS, CITAS Y ATENCIONES (200 citas)
    --   1 a 120   ATENDIDA    (pasadas, con atención)
    --   121 a 140 NO_ASISTIO  (pasadas)
    --   141 a 160 CANCELADA   (pasadas)
    --   161 a 180 CONFIRMADA  (próximos días)
    --   181 a 200 RESERVADA   (próximos días)
    -- Cada doctor nuevo recibe 2 o 3 citas, en horas distintas,
    -- así que sus turnos nunca se cruzan.
    -- ========================================================

    CREATE TEMP TABLE tmp_cita (
        n INTEGER PRIMARY KEY,
        id_cita INTEGER,
        id_paciente INTEGER,
        id_historia INTEGER,
        id_doctor INTEGER,
        id_consultorio INTEGER,
        fecha DATE,
        hora TIME,
        estado TEXT
    ) ON COMMIT DROP;

    FOR i IN 1..200 LOOP
        SELECT d.id_doctor, dc.id_consultorio INTO v_doctor, v_consultorio
        FROM usuario u
        JOIN doctor d ON d.id_usuario = u.id_usuario
        JOIN doctor_consultorio dc ON dc.id_doctor = d.id_doctor
        WHERE u.dni = (71000000 + (i - 1) % 80 + 1)::TEXT;

        SELECT p.id_paciente, hc.id_historia INTO v_paciente, v_historia
        FROM usuario u
        JOIN paciente p ON p.id_usuario = u.id_usuario
        JOIN historia_clinica hc ON hc.id_paciente = p.id_paciente
        WHERE u.dni = (70000000 + (i * 7 - 1) % 90 + 1)::TEXT;

        v_estado := CASE
            WHEN i <= 120 THEN 'ATENDIDA'
            WHEN i <= 140 THEN 'NO_ASISTIO'
            WHEN i <= 160 THEN 'CANCELADA'
            WHEN i <= 180 THEN 'CONFIRMADA'
            ELSE 'RESERVADA'
        END;

        -- Pasadas: entre 3 y 332 días atrás. Futuras: entre 1 y 25 días adelante. Sin domingos.
        IF i <= 160 THEN
            v_fecha := current_date - (3 + (i * 13) % 330);
            IF extract(dow FROM v_fecha) = 0 THEN v_fecha := v_fecha - 1; END IF;
        ELSE
            v_fecha := current_date + 1 + (i % 25);
            IF extract(dow FROM v_fecha) = 0 THEN v_fecha := v_fecha + 1; END IF;
        END IF;

        -- La vuelta (0, 1 o 2) por la lista de doctores fija la hora: 08:00, 09:30 u 11:00.
        v_hora := TIME '08:00' + make_interval(mins => ((i - 1) / 80) * 90);

        INSERT INTO horario (id_doctor, id_consultorio, fecha, hora_inicio, hora_fin, estado)
        VALUES (v_doctor, v_consultorio, v_fecha, v_hora, v_hora + INTERVAL '30 minutes',
                CASE v_estado
                    WHEN 'ATENDIDA' THEN 'FINALIZADO'
                    WHEN 'NO_ASISTIO' THEN 'FINALIZADO'
                    WHEN 'CANCELADA' THEN 'CANCELADO'
                    ELSE 'RESERVADO'
                END)
        RETURNING id_horario INTO v_horario;

        INSERT INTO cita (id_paciente, id_horario, fecha_creacion, motivo, estado, numero_ticket)
        SELECT v_paciente, v_horario,
               LEAST(v_fecha - (2 + i % 6), current_date - 1)::TIMESTAMP + TIME '08:00'
                   + make_interval(mins => (i * 17) % 600),
               c.motivo, v_estado,
               'A-' || lpad(nextval('seq_ticket_cita')::TEXT, 4, '0')
        FROM tmp_caso c
        WHERE c.n = (i - 1) % 15 + 1
        RETURNING id_cita INTO v_cita;

        INSERT INTO tmp_cita VALUES (i, v_cita, v_paciente, v_historia, v_doctor, v_consultorio, v_fecha, v_hora, v_estado);
    END LOOP;

    -- Atención de cada cita ATENDIDA.
    INSERT INTO atencion (id_historia, id_cita, id_doctor, fecha_atencion, estado, anamnesis, examen_fisico,
                          diagnostico, tratamiento, observaciones)
    SELECT t.id_historia, t.id_cita, t.id_doctor, t.fecha + t.hora + INTERVAL '5 minutes', 'FINALIZADA',
           c.motivo || '. Inicio hace ' || (2 + t.n % 10) || ' días.',
           c.examen_fisico, c.diagnostico, c.tratamiento,
           CASE WHEN t.n % 3 = 0 THEN 'Control en 15 días.' ELSE 'Volver si los síntomas persisten.' END
    FROM tmp_cita t
    JOIN tmp_caso c ON c.n = (t.n - 1) % 15 + 1
    WHERE t.estado = 'ATENDIDA'
    ORDER BY t.n;

    -- Triaje antes de cada atención.
    INSERT INTO triaje (id_historia, fecha_triaje, peso, talla, presion_arterial, frecuencia_cardiaca,
                        temperatura, saturacion_oxigeno, observaciones)
    SELECT t.id_historia, t.fecha + t.hora - INTERVAL '15 minutes',
           48 + (t.n * 37) % 45 + (t.n % 10) / 10.0,
           148 + (t.n * 23) % 35,
           (105 + (t.n * 7) % 40) || '/' || (65 + (t.n * 3) % 25),
           62 + (t.n * 11) % 35,
           36.2 + (t.n % 12) / 5.0,
           94 + (t.n % 6),
           CASE WHEN t.n % 4 = 0 THEN 'Paciente refiere dolor leve.' ELSE 'Sin observaciones.' END
    FROM tmp_cita t
    WHERE t.estado = 'ATENDIDA'
    ORDER BY t.n;

    -- Orden de exámenes en 90 de las atenciones.
    INSERT INTO orden_examen (id_atencion, examenes_requeridos, motivo_clinico, fecha_emision)
    SELECT a.id_atencion, c.examenes, 'Confirmar diagnóstico: ' || lower(c.diagnostico), a.fecha_atencion + INTERVAL '20 minutes'
    FROM tmp_cita t
    JOIN atencion a ON a.id_cita = t.id_cita
    JOIN tmp_caso c ON c.n = (t.n - 1) % 15 + 1
    WHERE t.n <= 90
    ORDER BY t.n;

    -- Un documento por atención.
    INSERT INTO documento_medico (id_atencion, tipo_documento, nombre_archivo, ruta_archivo, tipo_mime, estado, fecha_carga)
    SELECT a.id_atencion, d.tipo, d.archivo || '-' || t.n || '.pdf', 'demo/' || d.archivo || '-' || t.n || '.pdf',
           'application/pdf', CASE WHEN t.n % 20 = 0 THEN 'ELIMINADO' ELSE 'ACTIVO' END,
           a.fecha_atencion + INTERVAL '30 minutes'
    FROM tmp_cita t
    JOIN atencion a ON a.id_cita = t.id_cita
    JOIN (VALUES
        (0, 'Receta médica', 'receta'),
        (1, 'Resultados de laboratorio', 'resultados-laboratorio'),
        (2, 'Informe médico', 'informe-medico'),
        (3, 'Certificado médico', 'certificado-medico')
    ) AS d(k, tipo, archivo) ON d.k = t.n % 4
    ORDER BY t.n;

    -- Dos medicamentos por atención.
    INSERT INTO detalle_medicamento (id_atencion, nombre_medicamento, dosis, cantidad, via_administracion,
                                     presentacion, unidades_por_toma, frecuencia, duracion, orden)
    SELECT a.id_atencion, m.nombre, m.dosis, m.cantidad, m.via, m.presentacion, m.unidades, m.frecuencia,
           m.duracion, s.orden
    FROM tmp_cita t
    JOIN atencion a ON a.id_cita = t.id_cita
    CROSS JOIN (VALUES (1), (2)) AS s(orden)
    JOIN tmp_medicamento m ON m.n = CASE s.orden WHEN 1 THEN (t.n - 1) % 15 + 1 ELSE (t.n + 5) % 15 + 1 END
    ORDER BY t.n, s.orden;

    -- Tres resultados por atención en las primeras 40.
    INSERT INTO resultado_examen (id_atencion, tipo_examen, resultado, valor_referencia, interpretacion, orden)
    SELECT a.id_atencion, r.tipo, r.resultado, r.referencia, r.interpretacion, r.orden
    FROM tmp_cita t
    JOIN atencion a ON a.id_cita = t.id_cita
    JOIN tmp_caso c ON c.n = (t.n - 1) % 15 + 1
    CROSS JOIN LATERAL (VALUES
        (1, c.tipo_examen, c.resultado, c.referencia,
         CASE WHEN c.n IN (10, 11, 13, 14) THEN 'Normal' ELSE 'Alterado: correlacionar con la clínica' END),
        (2, 'Hematocrito', (36 + t.n % 9) || ' %', '36 a 46 %', 'Normal'),
        (3, 'Plaquetas', (180 + (t.n * 7) % 200) || ' 000 /µL', '150 000 a 450 000 /µL', 'Normal')
    ) AS r(orden, tipo, resultado, referencia, interpretacion)
    WHERE t.n <= 40
    ORDER BY t.n, r.orden;


    -- ========================================================
    -- SOLICITUDES (90)
    -- ========================================================

    -- 50 derivaciones de citas atendidas: 5 pendientes, el resto respondidas.
    INSERT INTO solicitud (id_cita, id_doctor_solicitante, id_personal_revisor, tipo, motivo, fecha_solicitud,
                           estado, fecha_respuesta, respuesta_admin, datos_adicionales)
    SELECT t.id_cita, t.id_doctor,
           CASE WHEN t.n % 10 = 0 THEN NULL ELSE pa.id_personal_administrativo END,
           'DERIVACION',
           'Requiere evaluación especializada por ' || lower(c.diagnostico),
           t.fecha + t.hora + INTERVAL '1 hour',
           CASE WHEN t.n % 10 = 0 THEN 'PENDIENTE' WHEN t.n % 4 = 0 THEN 'RECHAZADA' ELSE 'APROBADA' END,
           CASE WHEN t.n % 10 = 0 THEN NULL ELSE t.fecha + 1 + TIME '10:00' END,
           CASE WHEN t.n % 10 = 0 THEN NULL
                WHEN t.n % 4 = 0 THEN 'Completar primero el tratamiento indicado y reevaluar en el control.'
                ELSE 'Hoja de referencia lista para recoger en admisión.' END,
           json_build_object(
               'establecimiento', (ARRAY['Hospital de Chosica — José Agurto Tello',
                                         'Hospital Nacional Hipólito Unanue',
                                         'Hospital Nacional Arzobispo Loayza',
                                         'Instituto Nacional de Salud del Niño'])[t.n % 4 + 1],
               'especialidad', e.nombre,
               'antecedentes', hc.antecedentes,
               'factoresRiesgo', CASE WHEN t.n % 2 = 0 THEN 'Sedentarismo' ELSE 'Ninguno' END
           )::TEXT
    FROM tmp_cita t
    JOIN tmp_caso c ON c.n = (t.n - 1) % 15 + 1
    JOIN doctor d ON d.id_doctor = t.id_doctor
    JOIN especialidad e ON e.id_especialidad = d.id_especialidad
    JOIN historia_clinica hc ON hc.id_historia = t.id_historia
    JOIN usuario ua ON ua.dni = (72000000 + (t.n - 1) % 80 + 1)::TEXT
    JOIN personal_administrativo pa ON pa.id_usuario = ua.id_usuario
    WHERE t.n <= 50
    ORDER BY t.n;

    -- 20 cambios de fecha rechazados (citas a las que el paciente no asistió) y
    -- 20 resueltos por el paciente con la cancelación de la cita.
    INSERT INTO solicitud (id_cita, id_doctor_solicitante, id_personal_revisor, tipo, motivo, fecha_solicitud,
                           estado, fecha_respuesta, respuesta_admin, datos_adicionales)
    SELECT t.id_cita, t.id_doctor, pa.id_personal_administrativo, 'CAMBIO_HORARIO',
           (ARRAY['Capacitación obligatoria del MINSA ese día.', 'Mantenimiento programado del consultorio.',
                  'Licencia por salud del profesional.', 'Reunión clínica programada por la jefatura.'])[t.n % 4 + 1],
           t.fecha - 4 + TIME '15:00',
           CASE WHEN t.estado = 'NO_ASISTIO' THEN 'RECHAZADA' ELSE 'RESUELTA' END,
           t.fecha - 3 + TIME '10:00',
           CASE WHEN t.estado = 'NO_ASISTIO'
                THEN 'La actividad se reprogramó; la cita se mantiene en su fecha original.'
                ELSE 'Aprobado. El paciente elegirá cómo seguir con su cita.' END,
           (jsonb_build_object(
               'fechaOriginal', to_char(t.fecha, 'YYYY-MM-DD'),
               'horaOriginal', to_char(t.hora, 'HH24:MI'),
               'consultorioOriginal', co.zona || ' · ' || co.nombre,
               'doctorOriginal', split_part(ud.nombres, ' ', 1) || ' ' || split_part(ud.apellidos, ' ', 1),
               'especialidadOriginal', e.nombre
           ) || CASE WHEN t.estado = 'CANCELADA'
                     THEN jsonb_build_object('decisionPaciente', 'CANCELADA',
                                             'fechaResolucion', to_char(t.fecha - 2 + TIME '18:30', 'YYYY-MM-DD"T"HH24:MI:SS'))
                     ELSE '{}'::jsonb END)::TEXT
    FROM tmp_cita t
    JOIN consultorio co ON co.id_consultorio = t.id_consultorio
    JOIN doctor d ON d.id_doctor = t.id_doctor
    JOIN usuario ud ON ud.id_usuario = d.id_usuario
    JOIN especialidad e ON e.id_especialidad = d.id_especialidad
    JOIN usuario ua ON ua.dni = (72000000 + (t.n * 3) % 80 + 1)::TEXT
    JOIN personal_administrativo pa ON pa.id_usuario = ua.id_usuario
    WHERE t.estado IN ('NO_ASISTIO', 'CANCELADA')
    ORDER BY t.n;


    -- ========================================================
    -- AUDITORÍA (120): validación del ticket de cada cita atendida
    -- ========================================================

    INSERT INTO auditoria (id_usuario, accion, entidad, descripcion, id_registro, resultado, ip, fecha)
    SELECT ua.id_usuario, 'VALIDAR_TICKET', 'cita',
           'Ticket ' || ci.numero_ticket || ' validado en admisión.',
           t.id_cita, 'EXITOSO', '192.168.10.' || (10 + t.n % 200),
           t.fecha + t.hora - INTERVAL '25 minutes'
    FROM tmp_cita t
    JOIN cita ci ON ci.id_cita = t.id_cita
    JOIN usuario ua ON ua.dni = (72000000 + (t.n - 1) % 80 + 1)::TEXT
    WHERE t.estado = 'ATENDIDA'
    ORDER BY t.n;


    -- ========================================================
    -- CAMPAÑAS (80) E INSCRIPCIONES (180)
    --   1 a 60  FINALIZADA  (pasadas)
    --   61 a 64 ACTIVA      (en curso)
    --   65 a 76 PLANIFICADA (próximas)
    --   77 a 80 CANCELADA
    -- ========================================================

    INSERT INTO campania (id_personal_administrativo, titulo, descripcion, fecha_inicio, fecha_fin, hora, lugar,
                          estado, cupos, cupos_disponibles)
    SELECT pa.id_personal_administrativo,
           v_campanias[(k - 1) % 20 + 1] || ' · Jornada ' || ((k - 1) / 20 + 1),
           'Atención gratuita para la comunidad de Chaclacayo. Trae tu DNI.',
           f.inicio, f.fin, TIME '08:00' + make_interval(mins => (k % 5) * 30),
           v_lugares[k % 8 + 1],
           CASE WHEN k <= 60 THEN 'FINALIZADA' WHEN k <= 64 THEN 'ACTIVA'
                WHEN k <= 76 THEN 'PLANIFICADA' ELSE 'CANCELADA' END,
           30 + (k % 5) * 10, 30 + (k % 5) * 10
    FROM generate_series(1, 80) k
    CROSS JOIN LATERAL (
        -- Las activas empezaron hace 0 a 3 días y terminan en 3 días.
        SELECT g.inicio, CASE WHEN k BETWEEN 61 AND 64 THEN current_date + 3 ELSE g.inicio + k % 3 END AS fin
        FROM (SELECT CASE WHEN k <= 60 THEN current_date - (365 - k * 5)
                          WHEN k <= 64 THEN current_date - (k - 61)
                          ELSE current_date + (k - 64) * 3 END AS inicio) g
    ) f
    JOIN usuario u ON u.dni = (72000000 + k)::TEXT
    JOIN personal_administrativo pa ON pa.id_usuario = u.id_usuario
    ORDER BY k;

    -- Cada paciente nuevo se inscribe en dos campañas.
    INSERT INTO inscripcion_campania (id_campania, id_paciente, fecha_inscripcion, estado)
    SELECT ca.id_campania, p.id_paciente,
           LEAST(ca.fecha_inicio - 3, current_date - 1) + TIME '09:00' + make_interval(mins => (k * 13) % 600),
           CASE ca.estado
               WHEN 'FINALIZADA' THEN CASE WHEN k % 5 = 0 THEN 'NO_ASISTIO' ELSE 'ASISTIO' END
               WHEN 'CANCELADA' THEN 'CANCELADO'
               ELSE 'INSCRITO'
           END
    FROM generate_series(1, 90) k
    CROSS JOIN (VALUES (0), (1)) AS s(extra)
    JOIN usuario u ON u.dni = (70000000 + k)::TEXT
    JOIN paciente p ON p.id_usuario = u.id_usuario
    JOIN (SELECT ca.*, row_number() OVER (ORDER BY ca.id_campania) AS n
          FROM campania ca
          WHERE ca.titulo LIKE '% · Jornada %') ca ON ca.n = (k * 3 + s.extra) % 80 + 1
    ON CONFLICT DO NOTHING;

    -- Cada inscripción (no cancelada) ocupa un cupo.
    UPDATE campania ca
    SET cupos_disponibles = ca.cupos - (
        SELECT count(*) FROM inscripcion_campania ins
        WHERE ins.id_campania = ca.id_campania AND ins.estado <> 'CANCELADO')
    WHERE ca.titulo LIKE '% · Jornada %';


    -- ========================================================
    -- NOTIFICACIONES (450)
    -- ========================================================

    -- Bienvenida para cada cuenta nueva (250).
    INSERT INTO notificacion (id_usuario, titulo, mensaje, tipo, leida, fecha_envio)
    SELECT u.id_usuario, 'Te damos la bienvenida a SaludGrau',
           CASE r.nombre
               WHEN 'PACIENTE' THEN 'Desde aquí puedes reservar tus citas, ver tu ticket y descargar tus documentos médicos.'
               WHEN 'DOCTOR' THEN 'Desde aquí puedes revisar tu agenda, atender a tus pacientes y enviar solicitudes.'
               ELSE 'Desde aquí puedes validar tickets, crear horarios y responder solicitudes.'
           END,
           'SISTEMA', TRUE, u.fecha_registro + INTERVAL '1 minute'
    FROM usuario u
    JOIN rol r ON r.id_rol = u.id_rol
    WHERE u.dni ~ '^7[012]0000[0-9]{2}$'
    ORDER BY u.id_usuario;

    -- Aviso al paciente por cada cita (200).
    INSERT INTO notificacion (id_usuario, titulo, mensaje, tipo, leida, fecha_envio)
    SELECT p.id_usuario,
           CASE t.estado
               WHEN 'CANCELADA' THEN 'Tu cita fue cancelada'
               WHEN 'ATENDIDA' THEN 'Gracias por asistir a tu cita'
               WHEN 'NO_ASISTIO' THEN 'No registramos tu asistencia'
               ELSE 'Recordatorio de tu cita'
           END,
           'Cita de ' || e.nombre || ' el ' || to_char(t.fecha, 'DD/MM/YYYY') || ' a las '
               || to_char(t.hora, 'HH24:MI') || '. Ticket ' || ci.numero_ticket || '.',
           CASE WHEN t.estado IN ('CONFIRMADA', 'RESERVADA') THEN 'RECORDATORIO' ELSE 'CITA' END,
           t.estado NOT IN ('CONFIRMADA', 'RESERVADA'),
           LEAST(t.fecha + t.hora + INTERVAL '2 hours', current_timestamp - INTERVAL '1 hour')
    FROM tmp_cita t
    JOIN cita ci ON ci.id_cita = t.id_cita
    JOIN paciente p ON p.id_paciente = t.id_paciente
    JOIN doctor d ON d.id_doctor = t.id_doctor
    JOIN especialidad e ON e.id_especialidad = d.id_especialidad
    ORDER BY t.n;


    -- ========================================================
    -- RECUPERACIÓN DE PIN (80): códigos ya vencidos
    -- ========================================================

    INSERT INTO recuperacion_pin (id_usuario, codigo_hash, fecha_creacion, fecha_expiracion, intentos)
    SELECT u.id_usuario, v_hash_codigo,
           current_timestamp - make_interval(days => 1 + k % 60, mins => k * 7),
           current_timestamp - make_interval(days => 1 + k % 60, mins => k * 7) + INTERVAL '15 minutes',
           k % 4
    FROM generate_series(1, 80) k
    JOIN usuario u ON u.dni = (70000000 + k)::TEXT
    ON CONFLICT (id_usuario) DO NOTHING;


    -- ========================================================
    -- ANUNCIOS (80): historial de banners ya vencidos, para no
    -- llenar el carrusel actual con imágenes que no existen.
    -- ========================================================

    INSERT INTO anuncio (id_personal_administrativo, titulo, descripcion, etiqueta, enlace, texto_boton,
                         fecha_inicio, fecha_fin, activo, ruta_imagen, tipo_mime, ancho_imagen, alto_imagen,
                         fecha_creacion)
    SELECT pa.id_personal_administrativo,
           v_campanias[(k - 1) % 20 + 1],
           'Participa en la ' || lower(v_campanias[(k - 1) % 20 + 1]) || ' del centro de salud.',
           (ARRAY['Campaña', 'Evento', 'Charla', 'Nueva especialidad', 'Horario especial'])[k % 5 + 1],
           CASE WHEN k % 2 = 0 THEN '/paciente/comunicacion' ELSE NULL END,
           CASE WHEN k % 2 = 0 THEN 'Ver más' ELSE NULL END,
           current_date - (400 - k * 4), current_date - (400 - k * 4) + 14,
           k % 4 <> 0,
           'anuncios/demo/anuncio-' || k || '.jpg', 'image/jpeg', 1200, 640,
           (current_date - (402 - k * 4))::TIMESTAMP + TIME '11:00'
    FROM generate_series(1, 80) k
    JOIN usuario u ON u.dni = (72000000 + (k * 7) % 80 + 1)::TEXT
    JOIN personal_administrativo pa ON pa.id_usuario = u.id_usuario
    ORDER BY k;

    DROP TABLE tmp_caso;
    DROP TABLE tmp_medicamento;
    DROP TABLE tmp_cita;

    RAISE NOTICE 'Datos masivos cargados.';
END
$$;


-- ============================================================
-- FIN DEL SCRIPT
-- ============================================================
