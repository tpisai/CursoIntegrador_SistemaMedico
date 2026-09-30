-- ============================================================
-- BASE DE DATOS: SLGR
-- Script 02 · Objetos que usa la API del sistema web.
-- Ejecutar DESPUÉS de citas_salud.sql.
-- ============================================================


-- ============================================================
-- Correlativo del número de ticket de la cita (A-0001, A-0002...).
-- La API lo usa al reservar para que dos reservas simultáneas
-- nunca reciban el mismo número.
-- ============================================================

CREATE SEQUENCE seq_ticket_cita START WITH 1;


-- ============================================================
-- FIN DEL SCRIPT
-- ============================================================
