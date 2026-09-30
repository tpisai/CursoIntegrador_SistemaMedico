-- ============================================================
-- BASE DE DATOS: SLGR
-- Sistema Web de Gestión de Citas
-- Centro de Salud Miguel Grau
-- PostgreSQL
--
-- Estructura:
-- 21 tablas
-- Índices
-- Sin datos iniciales
-- ============================================================


-- ============================================================
-- 1. ROL
-- Define el tipo general de usuario del sistema.
-- ============================================================

CREATE TABLE rol (
    id_rol SERIAL PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL UNIQUE,
    descripcion VARCHAR(255)
);


-- ============================================================
-- 2. PERMISO
-- Define las acciones que pueden realizar los usuarios.
-- ============================================================

CREATE TABLE permiso (
    id_permiso SERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL UNIQUE,
    descripcion VARCHAR(255)
);


-- ============================================================
-- 3. CARGO
-- Define el cargo específico del personal administrativo.
-- ============================================================

CREATE TABLE cargo (
    id_cargo SERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL UNIQUE,
    descripcion VARCHAR(255),

    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',

    CONSTRAINT chk_cargo_estado
        CHECK (estado IN ('ACTIVO', 'INACTIVO'))
);


-- ============================================================
-- 4. CARGO_PERMISO
-- Relación muchos a muchos entre cargos y permisos.
-- ============================================================

CREATE TABLE cargo_permiso (
    id_cargo INTEGER NOT NULL,
    id_permiso INTEGER NOT NULL,

    PRIMARY KEY (id_cargo, id_permiso),

    CONSTRAINT fk_cargo_permiso_cargo
        FOREIGN KEY (id_cargo)
        REFERENCES cargo(id_cargo)
        ON DELETE CASCADE,

    CONSTRAINT fk_cargo_permiso_permiso
        FOREIGN KEY (id_permiso)
        REFERENCES permiso(id_permiso)
        ON DELETE CASCADE
);


-- ============================================================
-- 5. USUARIO
-- Información general de todos los usuarios.
-- ============================================================

CREATE TABLE usuario (
    id_usuario SERIAL PRIMARY KEY,

    id_rol INTEGER NOT NULL,

    dni VARCHAR(15) NOT NULL UNIQUE,

    nombres VARCHAR(100) NOT NULL,
    apellidos VARCHAR(100) NOT NULL,

    correo VARCHAR(150) NOT NULL UNIQUE,
    telefono VARCHAR(20),

    pin_hash VARCHAR(255),

    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',

    fecha_registro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    terminos_aceptados BOOLEAN NOT NULL DEFAULT FALSE,

    bloqueado_hasta TIMESTAMP,

    intentos_fallidos INTEGER NOT NULL DEFAULT 0,

    CONSTRAINT fk_usuario_rol
        FOREIGN KEY (id_rol)
        REFERENCES rol(id_rol),

    CONSTRAINT chk_usuario_estado
        CHECK (
            estado IN (
                'ACTIVO',
                'INACTIVO',
                'BLOQUEADO'
            )
        ),

    CONSTRAINT chk_usuario_intentos
        CHECK (intentos_fallidos >= 0)
);


-- ============================================================
-- 6. PERSONAL_ADMINISTRATIVO
-- Representa al personal administrativo.
-- ============================================================

CREATE TABLE personal_administrativo (
    id_personal_administrativo SERIAL PRIMARY KEY,

    id_usuario INTEGER NOT NULL UNIQUE,

    id_cargo INTEGER NOT NULL,

    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',

    CONSTRAINT fk_personal_usuario
        FOREIGN KEY (id_usuario)
        REFERENCES usuario(id_usuario),

    CONSTRAINT fk_personal_cargo
        FOREIGN KEY (id_cargo)
        REFERENCES cargo(id_cargo),

    CONSTRAINT chk_personal_estado
        CHECK (
            estado IN (
                'ACTIVO',
                'INACTIVO'
            )
        )
);


-- ============================================================
-- 7. ESPECIALIDAD
-- Especialidades médicas disponibles.
-- ============================================================

CREATE TABLE especialidad (
    id_especialidad SERIAL PRIMARY KEY,

    nombre VARCHAR(100) NOT NULL UNIQUE,
    descripcion VARCHAR(255),

    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',

    CONSTRAINT chk_especialidad_estado
        CHECK (
            estado IN (
                'ACTIVO',
                'INACTIVO'
            )
        )
);


-- ============================================================
-- 8. DOCTOR
-- Información específica del personal médico.
-- ============================================================

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
        CHECK (
            estado IN (
                'ACTIVO',
                'INACTIVO'
            )
        )
);


-- ============================================================
-- 9. PACIENTE
-- Información específica del paciente.
-- ============================================================

CREATE TABLE paciente (
    id_paciente SERIAL PRIMARY KEY,

    id_usuario INTEGER NOT NULL UNIQUE,

    fecha_nacimiento DATE,

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
            sexo IS NULL OR
            sexo IN (
                'MASCULINO',
                'FEMENINO',
                'OTRO'
            )
        ),

    CONSTRAINT chk_grupo_sanguineo
        CHECK (
            grupo_sanguineo IS NULL OR
            grupo_sanguineo IN (
                'A+',
                'A-',
                'B+',
                'B-',
                'AB+',
                'AB-',
                'O+',
                'O-'
            )
        )
);


-- ============================================================
-- 10. HISTORIA_CLINICA
-- Representa el expediente clínico permanente del paciente.
-- Un paciente tiene una única historia clínica.
-- ============================================================

CREATE TABLE historia_clinica (
    id_historia SERIAL PRIMARY KEY,

    id_paciente INTEGER NOT NULL UNIQUE,

    numero_historia VARCHAR(50) NOT NULL UNIQUE,

    fecha_apertura DATE NOT NULL DEFAULT CURRENT_DATE,

    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVA',

    observaciones TEXT,

    CONSTRAINT fk_historia_paciente
        FOREIGN KEY (id_paciente)
        REFERENCES paciente(id_paciente),

    CONSTRAINT chk_historia_estado
        CHECK (
            estado IN (
                'ACTIVA',
                'INACTIVA',
                'CERRADA'
            )
        )
);


-- ============================================================
-- 11. CONSULTORIO
-- Espacios físicos donde trabajan los doctores.
-- ============================================================

CREATE TABLE consultorio (
    id_consultorio SERIAL PRIMARY KEY,

    nombre VARCHAR(100) NOT NULL,

    zona VARCHAR(100),

    piso VARCHAR(20),

    numero VARCHAR(20),

    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',

    CONSTRAINT chk_consultorio_estado
        CHECK (
            estado IN (
                'ACTIVO',
                'INACTIVO'
            )
        )
);


-- ============================================================
-- 12. DOCTOR_CONSULTORIO
-- Relación entre doctores y consultorios.
--
-- Un consultorio puede tener como máximo 2 doctores.
-- El límite será validado desde Spring Boot.
-- ============================================================

CREATE TABLE doctor_consultorio (
    id_doctor INTEGER NOT NULL,

    id_consultorio INTEGER NOT NULL,

    PRIMARY KEY (id_doctor, id_consultorio),

    CONSTRAINT fk_doctor_consultorio_doctor
        FOREIGN KEY (id_doctor)
        REFERENCES doctor(id_doctor)
        ON DELETE CASCADE,

    CONSTRAINT fk_doctor_consultorio_consultorio
        FOREIGN KEY (id_consultorio)
        REFERENCES consultorio(id_consultorio)
        ON DELETE CASCADE
);


-- ============================================================
-- 13. HORARIO
-- Representa un horario individual de atención.
--
-- Ejemplo:
-- 10:00 - 10:15 = un horario
-- 10:15 - 10:30 = otro horario
--
-- Cada horario puede ser reservado por un único paciente.
-- ============================================================

CREATE TABLE horario (
    id_horario SERIAL PRIMARY KEY,

    id_doctor INTEGER NOT NULL,

    id_consultorio INTEGER NOT NULL,

    fecha DATE NOT NULL,

    hora_inicio TIME NOT NULL,

    hora_fin TIME NOT NULL,

    estado VARCHAR(20) NOT NULL DEFAULT 'DISPONIBLE',

    CONSTRAINT fk_horario_doctor
        FOREIGN KEY (id_doctor)
        REFERENCES doctor(id_doctor),

    CONSTRAINT fk_horario_consultorio
        FOREIGN KEY (id_consultorio)
        REFERENCES consultorio(id_consultorio),

    CONSTRAINT chk_horario_horas
        CHECK (
            hora_fin > hora_inicio
        ),

    CONSTRAINT chk_horario_estado
        CHECK (
            estado IN (
                'DISPONIBLE',
                'RESERVADO',
                'CANCELADO',
                'FINALIZADO'
            )
        )
);


-- ============================================================
-- 14. CITA
-- Reserva realizada por un paciente sobre un horario.
--
-- El doctor, consultorio, fecha y hora se obtienen desde
-- HORARIO, evitando duplicación de información.
-- ============================================================

CREATE TABLE cita (
    id_cita SERIAL PRIMARY KEY,

    id_paciente INTEGER NOT NULL,

    id_horario INTEGER NOT NULL UNIQUE,

    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    motivo TEXT,

    estado VARCHAR(20) NOT NULL DEFAULT 'RESERVADA',

    numero_ticket VARCHAR(50) NOT NULL UNIQUE,

    CONSTRAINT fk_cita_paciente
        FOREIGN KEY (id_paciente)
        REFERENCES paciente(id_paciente),

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


-- ============================================================
-- 15. TRIAJE
-- Registra las mediciones realizadas al paciente.
--
-- El triaje pertenece a la historia clínica, no directamente
-- a una cita.
--
-- Puede existir más de un triaje para un mismo paciente,
-- conservando el historial de sus mediciones.
-- ============================================================

CREATE TABLE triaje (
    id_triaje SERIAL PRIMARY KEY,

    id_historia INTEGER NOT NULL,

    fecha_triaje TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    peso NUMERIC(5,2),

    talla NUMERIC(5,2),

    presion_arterial VARCHAR(20),

    frecuencia_cardiaca INTEGER,

    temperatura NUMERIC(4,1),

    saturacion_oxigeno NUMERIC(5,2),

    observaciones TEXT,

    CONSTRAINT fk_triaje_historia
        FOREIGN KEY (id_historia)
        REFERENCES historia_clinica(id_historia)
);


-- ============================================================
-- 16. ATENCION
-- Registra la atención médica realizada.
-- ============================================================

CREATE TABLE atencion (
    id_atencion SERIAL PRIMARY KEY,

    id_historia INTEGER NOT NULL,

    id_cita INTEGER NOT NULL UNIQUE,

    id_doctor INTEGER NOT NULL,

    fecha_atencion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    estado VARCHAR(20) NOT NULL DEFAULT 'ABIERTA',

    anamnesis TEXT,

    examen_fisico TEXT,

    diagnostico TEXT,

    tratamiento TEXT,

    observaciones TEXT,

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


-- ============================================================
-- 17. DOCUMENTO_MEDICO
-- Archivos asociados a las atenciones médicas.
--
-- Ejemplos:
-- Recetas
-- Resultados de laboratorio
-- Informes
-- Imágenes
-- Otros documentos médicos
-- ============================================================

CREATE TABLE documento_medico (
    id_documento SERIAL PRIMARY KEY,

    id_atencion INTEGER NOT NULL,

    tipo_documento VARCHAR(100) NOT NULL,

    nombre_archivo VARCHAR(255) NOT NULL,

    ruta_archivo VARCHAR(500) NOT NULL,

    tipo_mime VARCHAR(100),

    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',

    fecha_carga TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_documento_atencion
        FOREIGN KEY (id_atencion)
        REFERENCES atencion(id_atencion)
        ON DELETE CASCADE,

    CONSTRAINT chk_documento_estado
        CHECK (
            estado IN (
                'ACTIVO',
                'ELIMINADO'
            )
        )
);


-- ============================================================
-- 18. CAMPANIA
-- Campañas creadas por personal administrativo.
-- ============================================================

CREATE TABLE campania (
    id_campania SERIAL PRIMARY KEY,

    id_personal_administrativo INTEGER NOT NULL,

    titulo VARCHAR(150) NOT NULL,

    descripcion TEXT,

    fecha_inicio DATE NOT NULL,

    fecha_fin DATE NOT NULL,

    hora TIME,

    lugar VARCHAR(255),

    estado VARCHAR(20) NOT NULL DEFAULT 'PLANIFICADA',

    imagen VARCHAR(500),

    cupos INTEGER NOT NULL,

    cupos_disponibles INTEGER NOT NULL,

    CONSTRAINT fk_campania_personal
        FOREIGN KEY (id_personal_administrativo)
        REFERENCES personal_administrativo(id_personal_administrativo),

    CONSTRAINT chk_campania_fechas
        CHECK (
            fecha_fin >= fecha_inicio
        ),

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


-- ============================================================
-- 19. INSCRIPCION_CAMPANIA
-- Relaciona pacientes con campañas.
-- ============================================================

CREATE TABLE inscripcion_campania (
    id_inscripcion SERIAL PRIMARY KEY,

    id_campania INTEGER NOT NULL,

    id_paciente INTEGER NOT NULL,

    fecha_inscripcion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    estado VARCHAR(20) NOT NULL DEFAULT 'INSCRITO',

    CONSTRAINT fk_inscripcion_campania
        FOREIGN KEY (id_campania)
        REFERENCES campania(id_campania)
        ON DELETE CASCADE,

    CONSTRAINT fk_inscripcion_paciente
        FOREIGN KEY (id_paciente)
        REFERENCES paciente(id_paciente),

    CONSTRAINT uq_campania_paciente
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


-- ============================================================
-- 20. SOLICITUD
-- Solicitudes realizadas por los doctores.
--
-- Tipos:
-- CAMBIO_HORARIO
-- DERIVACION
--
-- Estados:
-- PENDIENTE
-- EN_REVISION
-- APROBADA
-- RECHAZADA
-- ============================================================

CREATE TABLE solicitud (
    id_solicitud SERIAL PRIMARY KEY,

    id_cita INTEGER NOT NULL,

    id_doctor_solicitante INTEGER NOT NULL,

    id_personal_revisor INTEGER,

    tipo VARCHAR(30) NOT NULL,

    motivo TEXT NOT NULL,

    fecha_solicitud TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',

    fecha_respuesta TIMESTAMP,

    respuesta_admin TEXT,

    datos_adicionales TEXT,

    CONSTRAINT fk_solicitud_cita
        FOREIGN KEY (id_cita)
        REFERENCES cita(id_cita),

    CONSTRAINT fk_solicitud_doctor
        FOREIGN KEY (id_doctor_solicitante)
        REFERENCES doctor(id_doctor),

    CONSTRAINT fk_solicitud_revisor
        FOREIGN KEY (id_personal_revisor)
        REFERENCES personal_administrativo(id_personal_administrativo),

    CONSTRAINT chk_solicitud_tipo
        CHECK (
            tipo IN (
                'CAMBIO_HORARIO',
                'DERIVACION'
            )
        ),

    CONSTRAINT chk_solicitud_estado
        CHECK (
            estado IN (
                'PENDIENTE',
                'EN_REVISION',
                'APROBADA',
                'RECHAZADA'
            )
        )
);


-- ============================================================
-- 21. AUDITORIA
-- Registra las acciones realizadas dentro del sistema.
-- ============================================================

CREATE TABLE auditoria (
    id_auditoria BIGSERIAL PRIMARY KEY,

    id_usuario INTEGER,

    accion VARCHAR(100) NOT NULL,

    entidad VARCHAR(100) NOT NULL,

    descripcion TEXT,

    id_registro INTEGER,

    resultado VARCHAR(20) NOT NULL DEFAULT 'EXITOSO',

    ip VARCHAR(45),

    fecha TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_auditoria_usuario
        FOREIGN KEY (id_usuario)
        REFERENCES usuario(id_usuario),

    CONSTRAINT chk_auditoria_resultado
        CHECK (
            resultado IN (
                'EXITOSO',
                'FALLIDO'
            )
        )
);


-- ============================================================
-- ÍNDICES
-- ============================================================


-- ------------------------------------------------------------
-- USUARIO
-- ------------------------------------------------------------

CREATE INDEX idx_usuario_rol
    ON usuario(id_rol);

CREATE INDEX idx_usuario_estado
    ON usuario(estado);


-- ------------------------------------------------------------
-- PERSONAL ADMINISTRATIVO
-- ------------------------------------------------------------

CREATE INDEX idx_personal_cargo
    ON personal_administrativo(id_cargo);


-- ------------------------------------------------------------
-- DOCTOR
-- ------------------------------------------------------------

CREATE INDEX idx_doctor_especialidad
    ON doctor(id_especialidad);


-- ------------------------------------------------------------
-- DOCTOR_CONSULTORIO
-- ------------------------------------------------------------

CREATE INDEX idx_doctor_consultorio_consultorio
    ON doctor_consultorio(id_consultorio);


-- ------------------------------------------------------------
-- HORARIO
-- ------------------------------------------------------------

CREATE INDEX idx_horario_doctor
    ON horario(id_doctor);

CREATE INDEX idx_horario_consultorio
    ON horario(id_consultorio);

CREATE INDEX idx_horario_fecha
    ON horario(fecha);

CREATE INDEX idx_horario_estado
    ON horario(estado);


-- ------------------------------------------------------------
-- CITA
-- ------------------------------------------------------------

CREATE INDEX idx_cita_paciente
    ON cita(id_paciente);

CREATE INDEX idx_cita_estado
    ON cita(estado);


-- ------------------------------------------------------------
-- TRIAJE
-- ------------------------------------------------------------

CREATE INDEX idx_triaje_historia
    ON triaje(id_historia);

CREATE INDEX idx_triaje_fecha
    ON triaje(fecha_triaje);


-- ------------------------------------------------------------
-- ATENCION
-- ------------------------------------------------------------

CREATE INDEX idx_atencion_historia
    ON atencion(id_historia);

CREATE INDEX idx_atencion_doctor
    ON atencion(id_doctor);

CREATE INDEX idx_atencion_fecha
    ON atencion(fecha_atencion);


-- ------------------------------------------------------------
-- DOCUMENTO_MEDICO
-- ------------------------------------------------------------

CREATE INDEX idx_documento_atencion
    ON documento_medico(id_atencion);


-- ------------------------------------------------------------
-- CAMPANIA
-- ------------------------------------------------------------

CREATE INDEX idx_campania_personal
    ON campania(id_personal_administrativo);

CREATE INDEX idx_campania_estado
    ON campania(estado);

CREATE INDEX idx_campania_fecha
    ON campania(fecha_inicio, fecha_fin);


-- ------------------------------------------------------------
-- INSCRIPCION_CAMPANIA
-- ------------------------------------------------------------

CREATE INDEX idx_inscripcion_campania
    ON inscripcion_campania(id_campania);

CREATE INDEX idx_inscripcion_paciente
    ON inscripcion_campania(id_paciente);


-- ------------------------------------------------------------
-- SOLICITUD
-- ------------------------------------------------------------

CREATE INDEX idx_solicitud_cita
    ON solicitud(id_cita);

CREATE INDEX idx_solicitud_doctor
    ON solicitud(id_doctor_solicitante);

CREATE INDEX idx_solicitud_revisor
    ON solicitud(id_personal_revisor);

CREATE INDEX idx_solicitud_estado
    ON solicitud(estado);


-- ------------------------------------------------------------
-- AUDITORIA
-- ------------------------------------------------------------

CREATE INDEX idx_auditoria_usuario
    ON auditoria(id_usuario);

CREATE INDEX idx_auditoria_entidad
    ON auditoria(entidad);

CREATE INDEX idx_auditoria_fecha
    ON auditoria(fecha);


-- ============================================================
-- FIN DEL SCRIPT
-- ============================================================
