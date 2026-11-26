-- 10. Anuncios del carrusel de eventos (portal del paciente).
--
-- Administración publica banners rectangulares (recomendado 1200 x 640 px, proporción 15:8)
-- que el paciente ve en el carrusel de Campañas y en su inicio: nuevas especialidades,
-- ferias de salud, charlas, horarios especiales, etc. La imagen se guarda en
-- citas_salud/archivos/anuncios/ y aquí solo queda su ruta relativa.
--
-- Re-ejecutable: `instalar_bd.ps1 -Actualizar` vuelve a correr este script.

CREATE TABLE IF NOT EXISTS anuncio (
    id_anuncio SERIAL PRIMARY KEY,
    id_personal_administrativo INTEGER NOT NULL,
    titulo VARCHAR(120) NOT NULL,
    descripcion VARCHAR(300),
    -- Texto corto sobre el banner, p. ej. "Nueva especialidad" o "Evento".
    etiqueta VARCHAR(40),
    -- Ruta interna del portal o URL externa a la que lleva el botón (opcional).
    enlace VARCHAR(255),
    texto_boton VARCHAR(40),
    fecha_inicio DATE NOT NULL,
    fecha_fin DATE NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    ruta_imagen VARCHAR(500) NOT NULL,
    tipo_mime VARCHAR(50) NOT NULL,
    ancho_imagen INTEGER NOT NULL,
    alto_imagen INTEGER NOT NULL,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_anuncio_personal
        FOREIGN KEY (id_personal_administrativo)
        REFERENCES personal_administrativo(id_personal_administrativo),
    CONSTRAINT chk_anuncio_fechas CHECK (fecha_fin >= fecha_inicio)
);

CREATE INDEX IF NOT EXISTS idx_anuncio_vigencia ON anuncio (activo, fecha_inicio, fecha_fin);
