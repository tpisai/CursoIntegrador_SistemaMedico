-- =========================================================
-- BASE DE DATOS: CENTRO DE SALUD
-- SISTEMA WEB DE GESTIÓN DE CITAS
-- PostgreSQL
-- =========================================================


-- =========================================================
-- 1. TABLA ROL
-- =========================================================

CREATE TABLE rol (
    id_rol SERIAL PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL UNIQUE,
    descripcion VARCHAR(255)
);


-- =========================================================
-- 2. TABLA USUARIO
-- =========================================================

CREATE TABLE usuario (
    id_usuario SERIAL PRIMARY KEY,

    id_rol INTEGER NOT NULL,

    dni VARCHAR(20) NOT NULL UNIQUE,
    nombres VARCHAR(100) NOT NULL,
    apellidos VARCHAR(100) NOT NULL,

    correo VARCHAR(150) NOT NULL UNIQUE,
    telefono VARCHAR(20),

    pin_hash VARCHAR(255) NOT NULL,

    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',

    fecha_registro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    terminos_aceptados BOOLEAN NOT NULL DEFAULT FALSE,
    fecha_aceptacion_terminos TIMESTAMP,

    bloqueado_hasta TIMESTAMP,

    intentos_fallidos INTEGER NOT NULL DEFAULT 0,

    CONSTRAINT fk_usuario_rol
        FOREIGN KEY (id_rol)
        REFERENCES rol(id_rol),

    CONSTRAINT chk_usuario_estado
        CHECK (estado IN ('ACTIVO', 'INACTIVO', 'BLOQUEADO')),

    CONSTRAINT chk_intentos_fallidos
        CHECK (intentos_fallidos >= 0)
);


-- =========================================================
-- 3. TABLA PERSONAL ADMINISTRATIVO
-- =========================================================

CREATE TABLE personal_administrativo (
    id_personal_administrativo SERIAL PRIMARY KEY,

    id_usuario INTEGER NOT NULL UNIQUE,

    cargo VARCHAR(100) NOT NULL,

    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',

    CONSTRAINT fk_personal_usuario
        FOREIGN KEY (id_usuario)
        REFERENCES usuario(id_usuario),

    CONSTRAINT chk_personal_estado
        CHECK (estado IN ('ACTIVO', 'INACTIVO'))
);


-- =========================================================
-- 4. TABLA ESPECIALIDAD
-- =========================================================

CREATE TABLE especialidad (
    id_especialidad SERIAL PRIMARY KEY,

    nombre VARCHAR(100) NOT NULL UNIQUE,

    descripcion VARCHAR(255),

    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',

    CONSTRAINT chk_especialidad_estado
        CHECK (estado IN ('ACTIVO', 'INACTIVO'))
);


-- =========================================================
-- 5. TABLA DOCTOR
-- =========================================================

CREATE TABLE doctor (
    id_doctor SERIAL PRIMARY KEY,

    id_usuario INTEGER NOT NULL UNIQUE,

    id_especialidad INTEGER NOT NULL,

    cmp VARCHAR(30) NOT NULL UNIQUE,

    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',

    CONSTRAINT fk_doctor_usuario
        FOREIGN KEY (id_usuario)
        REFERENCES usuario(id_usuario),

    CONSTRAINT fk_doctor_especialidad
        FOREIGN KEY (id_especialidad)
        REFERENCES especialidad(id_especialidad),

    CONSTRAINT chk_doctor_estado
        CHECK (estado IN ('ACTIVO', 'INACTIVO'))
);


-- =========================================================
-- 6. TABLA PACIENTE
-- =========================================================

CREATE TABLE paciente (
    id_paciente SERIAL PRIMARY KEY,

    id_usuario INTEGER NOT NULL UNIQUE,

    fecha_nacimiento DATE NOT NULL,

    sexo VARCHAR(20),

    direccion VARCHAR(255),

    grupo_sanguineo VARCHAR(10),

    contacto_emergencia VARCHAR(150),

    telefono_emergencia VARCHAR(20),

    CONSTRAINT fk_paciente_usuario
        FOREIGN KEY (id_usuario)
        REFERENCES usuario(id_usuario),

    CONSTRAINT chk_paciente_sexo
        CHECK (
            sexo IS NULL
            OR sexo IN ('MASCULINO', 'FEMENINO', 'OTRO')
        ),

    CONSTRAINT chk_grupo_sanguineo
        CHECK (
            grupo_sanguineo IS NULL
            OR grupo_sanguineo IN (
                'A+','A-',
                'B+','B-',
                'AB+','AB-',
                'O+','O-'
            )
        )
);


-- =========================================================
-- 7. TABLA HISTORIA CLINICA
-- =========================================================

CREATE TABLE historia_clinica (
    id_historia SERIAL PRIMARY KEY,

    id_paciente INTEGER NOT NULL UNIQUE,

    numero_historia VARCHAR(30) NOT NULL UNIQUE,

    fecha_apertura DATE NOT NULL DEFAULT CURRENT_DATE,

    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVA',

    observaciones TEXT,

    CONSTRAINT fk_historia_paciente
        FOREIGN KEY (id_paciente)
        REFERENCES paciente(id_paciente),

    CONSTRAINT chk_historia_estado
        CHECK (estado IN ('ACTIVA', 'INACTIVA', 'CERRADA'))
);


-- =========================================================
-- 8. TABLA CONSULTORIO
-- =========================================================

CREATE TABLE consultorio (
    id_consultorio SERIAL PRIMARY KEY,

    nombre VARCHAR(100) NOT NULL,

    zona VARCHAR(100),

    piso INTEGER,

    numero VARCHAR(20),

    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',

    CONSTRAINT chk_consultorio_estado
        CHECK (estado IN ('ACTIVO', 'INACTIVO')),

    CONSTRAINT chk_consultorio_piso
        CHECK (piso IS NULL OR piso >= 0)
);


-- =========================================================
-- 9. TABLA HORARIO
-- =========================================================

CREATE TABLE horario (
    id_horario SERIAL PRIMARY KEY,

    id_doctor INTEGER NOT NULL,

    id_consultorio INTEGER NOT NULL,

    fecha DATE NOT NULL,

    hora_inicio TIME NOT NULL,

    hora_fin TIME NOT NULL,

    cantidad_cupos INTEGER NOT NULL,

    cupos_disponibles INTEGER NOT NULL,

    estado VARCHAR(20) NOT NULL DEFAULT 'DISPONIBLE',

    CONSTRAINT fk_horario_doctor
        FOREIGN KEY (id_doctor)
        REFERENCES doctor(id_doctor),

    CONSTRAINT fk_horario_consultorio
        FOREIGN KEY (id_consultorio)
        REFERENCES consultorio(id_consultorio),

    CONSTRAINT chk_horario_horas
        CHECK (hora_fin > hora_inicio),

    CONSTRAINT chk_horario_cupos
        CHECK (
            cantidad_cupos > 0
            AND cupos_disponibles >= 0
            AND cupos_disponibles <= cantidad_cupos
        ),

    CONSTRAINT chk_horario_estado
        CHECK (
            estado IN (
                'DISPONIBLE',
                'AGOTADO',
                'CANCELADO',
                'FINALIZADO'
            )
        )
);


-- =========================================================
-- 10. TABLA CITA
-- =========================================================

CREATE TABLE cita (
    id_cita SERIAL PRIMARY KEY,

    id_paciente INTEGER NOT NULL,

    id_doctor INTEGER NOT NULL,

    id_horario INTEGER NOT NULL,

    fecha_cita DATE NOT NULL,

    hora_cita TIME NOT NULL,

    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    motivo TEXT,

    estado VARCHAR(30) NOT NULL DEFAULT 'RESERVADA',

    numero_ticket VARCHAR(30) NOT NULL UNIQUE,

    CONSTRAINT fk_cita_paciente
        FOREIGN KEY (id_paciente)
        REFERENCES paciente(id_paciente),

    CONSTRAINT fk_cita_doctor
        FOREIGN KEY (id_doctor)
        REFERENCES doctor(id_doctor),

    CONSTRAINT fk_cita_horario
        FOREIGN KEY (id_horario)
        REFERENCES horario(id_horario),

    CONSTRAINT chk_cita_estado
        CHECK (
            estado IN (
                'RESERVADA',
                'CONFIRMADA',
                'ATENDIDA',
                'CANCELADA',
                'NO_ASISTIO'
            )
        )
);


-- =========================================================
-- 11. TABLA ATENCION
-- =========================================================

CREATE TABLE atencion (
    id_atencion SERIAL PRIMARY KEY,

    id_historia INTEGER NOT NULL,

    id_cita INTEGER NOT NULL UNIQUE,

    id_doctor INTEGER NOT NULL,

    fecha_atencion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    estado VARCHAR(20) NOT NULL DEFAULT 'ABIERTA',

    observaciones TEXT,

    tratamiento TEXT,

    diagnostico TEXT,

    anamnesis TEXT,

    examen_fisico TEXT,

    CONSTRAINT fk_atencion_historia
        FOREIGN KEY (id_historia)
        REFERENCES historia_clinica(id_historia),

    CONSTRAINT fk_atencion_cita
        FOREIGN KEY (id_cita)
        REFERENCES cita(id_cita),

    CONSTRAINT fk_atencion_doctor
        FOREIGN KEY (id_doctor)
        REFERENCES doctor(id_doctor),

    CONSTRAINT chk_atencion_estado
        CHECK (
            estado IN (
                'ABIERTA',
                'FINALIZADA',
                'CANCELADA'
            )
        )
);


-- =========================================================
-- 12. TABLA DOCUMENTO MEDICO
-- =========================================================

CREATE TABLE documento_medico (
    id_documento SERIAL PRIMARY KEY,

    id_atencion INTEGER NOT NULL,

    tipo_documento VARCHAR(50) NOT NULL,

    nombre_archivo VARCHAR(255) NOT NULL,

    ruta_archivo VARCHAR(500) NOT NULL,

    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',

    fecha_carga TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    tipo_mime VARCHAR(100),

    CONSTRAINT fk_documento_atencion
        FOREIGN KEY (id_atencion)
        REFERENCES atencion(id_atencion),

    CONSTRAINT chk_documento_estado
        CHECK (
            estado IN ('ACTIVO', 'ELIMINADO')
        )
);


-- =========================================================
-- 13. TABLA CAMPAÑA
-- =========================================================

CREATE TABLE campania (
    id_campania SERIAL PRIMARY KEY,

    id_personal_administrativo INTEGER NOT NULL,

    titulo VARCHAR(150) NOT NULL,

    descripcion TEXT,

    fecha_inicio DATE NOT NULL,

    fecha_fin DATE NOT NULL,

    hora TIME,

    lugar VARCHAR(255),

    estado VARCHAR(30) NOT NULL DEFAULT 'PLANIFICADA',

    imagen VARCHAR(500),

    cupos INTEGER NOT NULL,

    cupos_disponibles INTEGER NOT NULL,

    CONSTRAINT fk_campania_personal
        FOREIGN KEY (id_personal_administrativo)
        REFERENCES personal_administrativo(
            id_personal_administrativo
        ),

    CONSTRAINT chk_campania_fechas
        CHECK (fecha_fin >= fecha_inicio),

    CONSTRAINT chk_campania_cupos
        CHECK (
            cupos > 0
            AND cupos_disponibles >= 0
            AND cupos_disponibles <= cupos
        ),

    CONSTRAINT chk_campania_estado
        CHECK (
            estado IN (
                'PLANIFICADA',
                'ACTIVA',
                'FINALIZADA',
                'CANCELADA'
            )
        )
);


-- =========================================================
-- 14. TABLA INSCRIPCION_CAMPAÑA
-- =========================================================

CREATE TABLE inscripcion_campania (
    id_inscripcion SERIAL PRIMARY KEY,

    id_campania INTEGER NOT NULL,

    id_paciente INTEGER NOT NULL,

    fecha_inscripcion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    estado VARCHAR(30) NOT NULL DEFAULT 'INSCRITO',

    CONSTRAINT fk_inscripcion_campania
        FOREIGN KEY (id_campania)
        REFERENCES campania(id_campania),

    CONSTRAINT fk_inscripcion_paciente
        FOREIGN KEY (id_paciente)
        REFERENCES paciente(id_paciente),

    CONSTRAINT uq_inscripcion_campania_paciente
        UNIQUE (id_campania, id_paciente),

    CONSTRAINT chk_inscripcion_estado
        CHECK (
            estado IN (
                'INSCRITO',
                'ASISTIO',
                'NO_ASISTIO',
                'CANCELADO'
            )
        )
);


-- =========================================================
-- 15. TABLA SOLICITUD
-- =========================================================

CREATE TABLE solicitud (
    id_solicitud SERIAL PRIMARY KEY,

    id_cita INTEGER NOT NULL,

    id_doctor INTEGER,

    id_personal_administrativo INTEGER,

    tipo VARCHAR(50) NOT NULL,

    motivo TEXT NOT NULL,

    fecha_solicitud TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    estado VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE',

    fecha_respuesta TIMESTAMP,

    respuesta_admin TEXT,

    datos_adicionales TEXT,

    CONSTRAINT fk_solicitud_cita
        FOREIGN KEY (id_cita)
        REFERENCES cita(id_cita),

    CONSTRAINT fk_solicitud_doctor
        FOREIGN KEY (id_doctor)
        REFERENCES doctor(id_doctor),

    CONSTRAINT fk_solicitud_personal
        FOREIGN KEY (id_personal_administrativo)
        REFERENCES personal_administrativo(
            id_personal_administrativo
        ),

    CONSTRAINT chk_solicitud_estado
        CHECK (
            estado IN (
                'PENDIENTE',
                'EN_REVISION',
                'APROBADA',
                'RECHAZADA',
                'ATENDIDA'
            )
        )
);


-- =========================================================
-- 16. TABLA AUDITORIA
-- =========================================================

CREATE TABLE auditoria (
    id_auditoria BIGSERIAL PRIMARY KEY,

    id_usuario INTEGER,

    accion VARCHAR(100) NOT NULL,

    entidad VARCHAR(100) NOT NULL,

    descripcion TEXT,

    id_registro INTEGER,

    ip VARCHAR(45),

    fecha TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_auditoria_usuario
        FOREIGN KEY (id_usuario)
        REFERENCES usuario(id_usuario)
);


-- =========================================================
-- ÍNDICES
-- =========================================================

CREATE INDEX idx_usuario_rol
ON usuario(id_rol);

CREATE INDEX idx_paciente_usuario
ON paciente(id_usuario);

CREATE INDEX idx_doctor_especialidad
ON doctor(id_especialidad);

CREATE INDEX idx_horario_doctor
ON horario(id_doctor);

CREATE INDEX idx_horario_fecha
ON horario(fecha);

CREATE INDEX idx_horario_consultorio
ON horario(id_consultorio);

CREATE INDEX idx_cita_paciente
ON cita(id_paciente);

CREATE INDEX idx_cita_doctor
ON cita(id_doctor);

CREATE INDEX idx_cita_horario
ON cita(id_horario);

CREATE INDEX idx_cita_fecha
ON cita(fecha_cita);

CREATE INDEX idx_cita_estado
ON cita(estado);

CREATE INDEX idx_atencion_historia
ON atencion(id_historia);

CREATE INDEX idx_atencion_doctor
ON atencion(id_doctor);

CREATE INDEX idx_documento_atencion
ON documento_medico(id_atencion);

CREATE INDEX idx_inscripcion_campania
ON inscripcion_campania(id_campania);

CREATE INDEX idx_inscripcion_paciente
ON inscripcion_campania(id_paciente);

CREATE INDEX idx_solicitud_cita
ON solicitud(id_cita);

CREATE INDEX idx_solicitud_estado
ON solicitud(estado);

CREATE INDEX idx_auditoria_usuario
ON auditoria(id_usuario);

CREATE INDEX idx_auditoria_fecha
ON auditoria(fecha);

-- =========================================================
-- DATOS INICIALES
-- =========================================================

INSERT INTO rol (nombre, descripcion)
VALUES
('ADMINISTRADOR', 'Administrador del sistema'),
('PERSONAL_ADMINISTRATIVO', 'Personal administrativo del centro de salud'),
('DOCTOR', 'Médico del centro de salud'),
('PACIENTE', 'Paciente del centro de salud');


INSERT INTO especialidad (nombre, descripcion)
VALUES
('Medicina General', 'Atención médica general'),
('Pediatría', 'Atención médica para niños'),
('Cardiología', 'Especialidad del corazón y sistema circulatorio'),
('Dermatología', 'Especialidad de piel y anexos'),
('Ginecología', 'Salud reproductiva femenina');


INSERT INTO consultorio
(nombre, zona, piso, numero, estado)
VALUES
('Consultorio Medicina General', 'Zona A', 1, '101', 'ACTIVO'),
('Consultorio Pediatría', 'Zona A', 1, '102', 'ACTIVO'),
('Consultorio Cardiología', 'Zona B', 2, '201', 'ACTIVO'),
('Consultorio Dermatología', 'Zona B', 2, '202', 'ACTIVO');