package com.cis.citas_salud.service;

import com.cis.citas_salud.api.ApiException;
import com.cis.citas_salud.entity.Cita;
import com.cis.citas_salud.entity.Consultorio;
import com.cis.citas_salud.entity.Doctor;
import com.cis.citas_salud.entity.Especialidad;
import com.cis.citas_salud.entity.Horario;
import com.cis.citas_salud.entity.Paciente;
import com.cis.citas_salud.entity.Rol;
import com.cis.citas_salud.entity.Usuario;
import org.junit.jupiter.api.function.Executable;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Entidades de ejemplo para probar los servicios sin base de datos. */
final class DatosPrueba {

    static final String TICKET = "A-0154";

    private DatosPrueba() {
    }

    static Usuario usuario(int id, String rol, String dni) {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(id);
        usuario.setRol(new Rol(rol));
        usuario.setDni(dni);
        usuario.setNombres("Ana María");
        usuario.setApellidos("Quispe Rojas");
        usuario.setCorreo("usuario" + id + "@correo.test");
        return usuario;
    }

    static Paciente paciente(int id, Usuario usuario) {
        Paciente paciente = new Paciente();
        paciente.setIdPaciente(id);
        paciente.setUsuario(usuario);
        return paciente;
    }

    static Doctor doctor(int id, Usuario usuario) {
        Especialidad especialidad = new Especialidad("Medicina General");
        especialidad.setIdEspecialidad(1);
        Doctor doctor = new Doctor();
        doctor.setIdDoctor(id);
        doctor.setUsuario(usuario);
        doctor.setEspecialidad(especialidad);
        return doctor;
    }

    static Consultorio consultorio(int id) {
        Consultorio consultorio = new Consultorio("Consultorio " + id, "Módulo A", "1", String.valueOf(id));
        consultorio.setIdConsultorio(id);
        return consultorio;
    }

    /** Turno de 09:00 a 09:30, disponible. */
    static Horario horario(int id, Doctor doctor, LocalDate fecha) {
        Horario horario = new Horario(doctor, consultorio(1), fecha, LocalTime.of(9, 0), LocalTime.of(9, 30));
        horario.setIdHorario(id);
        return horario;
    }

    static Cita cita(int id, Paciente paciente, Horario horario, String estado) {
        Cita cita = new Cita();
        cita.setIdCita(id);
        cita.setPaciente(paciente);
        cita.setHorario(horario);
        cita.setEstado(estado);
        cita.setNumeroTicket(TICKET);
        return cita;
    }

    /** Exige que la acción falle con ese código HTTP y devuelve el error para revisar el mensaje. */
    static ApiException exigeError(HttpStatus esperado, Executable accion) {
        ApiException error = assertThrows(ApiException.class, accion);
        assertEquals(esperado, error.getStatus());
        return error;
    }
}
