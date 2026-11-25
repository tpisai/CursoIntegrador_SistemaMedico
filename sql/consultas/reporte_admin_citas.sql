-- SaludGrau - Reporte administrativo de citas médicas (PostgreSQL)
--
-- Consulta de apoyo para el panel administrativo. Alimenta el reporte PDF
-- "reporte_admin_citas.jrxml" (indicadores + gráficos + detalle por doctor).
--
-- La fecha de una cita se obtiene de HORARIO (cita -> horario), porque la
-- tabla CITA no guarda fecha propia.
--
-- Probar desde psql:
--   psql -d <tu_base> -f sql/consultas/reporte_admin_citas.sql
-- Para cambiar el periodo, edita las dos variables siguientes.
-- ============================================================================
\set fecha_inicio '2026-01-01'
\set fecha_fin    '2026-12-31'


-- 1. INDICADORES GENERALES (tarjetas del reporte)
SELECT
    COUNT(*)                                          AS total_citas,
    COUNT(*) FILTER (WHERE c.estado = 'ATENDIDA')     AS atendidas,
    COUNT(*) FILTER (WHERE c.estado = 'CANCELADA')    AS canceladas,
    COUNT(*) FILTER (WHERE c.estado = 'NO_ASISTIO')   AS no_asistio,
    COUNT(*) FILTER (WHERE c.estado IN ('RESERVADA', 'CONFIRMADA')) AS pendientes,
    COALESCE(ROUND(100.0 * COUNT(*) FILTER (WHERE c.estado = 'ATENDIDA')
                   / NULLIF(COUNT(*), 0), 1), 0)      AS pct_atendidas
FROM cita c
JOIN horario h ON h.id_horario = c.id_horario
WHERE h.fecha BETWEEN :'fecha_inicio' AND :'fecha_fin';


-- 2. CITAS POR ESTADO (gráfico circular)
SELECT c.estado, COUNT(*) AS total
FROM cita c
JOIN horario h ON h.id_horario = c.id_horario
WHERE h.fecha BETWEEN :'fecha_inicio' AND :'fecha_fin'
GROUP BY c.estado
ORDER BY total DESC;


-- 3. CITAS POR ESPECIALIDAD (gráfico de barras)
SELECT e.nombre AS especialidad, COUNT(*) AS total
FROM cita c
JOIN horario h        ON h.id_horario      = c.id_horario
JOIN doctor d         ON d.id_doctor       = h.id_doctor
JOIN especialidad e   ON e.id_especialidad = d.id_especialidad
WHERE h.fecha BETWEEN :'fecha_inicio' AND :'fecha_fin'
GROUP BY e.nombre
ORDER BY total DESC;


-- 4. CITAS POR MES (gráfico de barras)
SELECT TO_CHAR(h.fecha, 'YYYY-MM') AS mes, COUNT(*) AS total
FROM cita c
JOIN horario h ON h.id_horario = c.id_horario
WHERE h.fecha BETWEEN :'fecha_inicio' AND :'fecha_fin'
GROUP BY 1
ORDER BY 1;


-- 5. DETALLE POR DOCTOR (tabla del reporte)
SELECT
    u.nombres || ' ' || u.apellidos                    AS doctor,
    e.nombre                                           AS especialidad,
    COUNT(*)                                           AS total,
    COUNT(*) FILTER (WHERE c.estado = 'ATENDIDA')      AS atendidas,
    COUNT(*) FILTER (WHERE c.estado = 'CANCELADA')     AS canceladas,
    COUNT(*) FILTER (WHERE c.estado = 'NO_ASISTIO')    AS no_asistio,
    COALESCE(ROUND(100.0 * COUNT(*) FILTER (WHERE c.estado = 'ATENDIDA')
                   / NULLIF(COUNT(*), 0), 1), 0)       AS pct_atendidas
FROM cita c
JOIN horario h        ON h.id_horario      = c.id_horario
JOIN doctor d         ON d.id_doctor       = h.id_doctor
JOIN usuario u        ON u.id_usuario      = d.id_usuario
JOIN especialidad e   ON e.id_especialidad = d.id_especialidad
WHERE h.fecha BETWEEN :'fecha_inicio' AND :'fecha_fin'
GROUP BY u.nombres, u.apellidos, e.nombre
ORDER BY total DESC, doctor;
