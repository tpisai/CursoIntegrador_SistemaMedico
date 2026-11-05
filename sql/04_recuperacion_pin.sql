-- ============================================================
-- BASE DE DATOS: SLGR
-- Script 04 · Recuperación de PIN por correo (RF-03).
-- Se puede ejecutar más de una vez sobre una base existente.
-- ============================================================

-- Código vigente de cada usuario. Pedir uno nuevo reemplaza al anterior
-- y la fila se borra al usarlo, por eso hay como máximo una por usuario.
CREATE TABLE IF NOT EXISTS recuperacion_pin (
    id_recuperacion SERIAL PRIMARY KEY,

    id_usuario INTEGER NOT NULL UNIQUE,

    codigo_hash VARCHAR(255) NOT NULL,

    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    fecha_expiracion TIMESTAMP NOT NULL,

    intentos INTEGER NOT NULL DEFAULT 0,

    CONSTRAINT fk_recuperacion_usuario
        FOREIGN KEY (id_usuario)
        REFERENCES usuario(id_usuario)
        ON DELETE CASCADE,

    CONSTRAINT chk_recuperacion_intentos
        CHECK (intentos >= 0)
);
