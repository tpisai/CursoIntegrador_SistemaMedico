-- Agrega soporte para órdenes de análisis y pruebas médicas.
-- Se puede ejecutar varias veces y no modifica las órdenes existentes.

CREATE TABLE IF NOT EXISTS orden_examen (
    id_orden_examen SERIAL PRIMARY KEY,
    id_atencion INTEGER NOT NULL UNIQUE,
    examenes_requeridos TEXT NOT NULL,
    motivo_clinico TEXT NOT NULL,
    fecha_emision TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_orden_examen_atencion
        FOREIGN KEY (id_atencion)
        REFERENCES atencion(id_atencion)
        ON DELETE CASCADE
);
