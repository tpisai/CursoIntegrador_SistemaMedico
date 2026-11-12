-- ============================================================
-- BASE DE DATOS: SLGR
-- Script 07 · Medicamentos de la receta médica.
-- La tabla ya viene en citas_salud.sql; este script la agrega a las
-- bases instaladas antes de ese cambio.
-- Se puede ejecutar más de una vez sobre una base existente.
-- ============================================================

CREATE TABLE IF NOT EXISTS detalle_medicamento (
    id_detalle SERIAL PRIMARY KEY,
    id_atencion INTEGER NOT NULL,
    nombre_medicamento VARCHAR(200) NOT NULL,
    dosis VARCHAR(100),
    cantidad VARCHAR(50),
    via_administracion VARCHAR(100),
    presentacion VARCHAR(100),
    unidades_por_toma VARCHAR(50),
    frecuencia VARCHAR(100),
    duracion VARCHAR(100),
    orden INTEGER DEFAULT 1,

    CONSTRAINT fk_detalle_medicamento_atencion
        FOREIGN KEY (id_atencion)
        REFERENCES atencion(id_atencion)
        ON DELETE CASCADE
);

-- La receta siempre se consulta por atención.
CREATE INDEX IF NOT EXISTS idx_detalle_medicamento_atencion
    ON detalle_medicamento(id_atencion);
