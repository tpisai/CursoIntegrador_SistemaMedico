package com.cis.citas_salud.controller;

import com.cis.citas_salud.repository.ConsultorioRepository;
import com.cis.citas_salud.repository.EspecialidadRepository;
import com.cis.citas_salud.repository.RolRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import com.cis.citas_salud.entity.Rol;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class InicioController {

    private final RolRepository rolRepository;
    private final EspecialidadRepository especialidadRepository;
    private final ConsultorioRepository consultorioRepository;

    public InicioController(
            RolRepository rolRepository,
            EspecialidadRepository especialidadRepository,
            ConsultorioRepository consultorioRepository) {

        this.rolRepository = rolRepository;
        this.especialidadRepository = especialidadRepository;
        this.consultorioRepository = consultorioRepository;
    }

    @GetMapping("/")
    public String inicio(Model model) {

        model.addAttribute("roles", rolRepository.findAll());
        model.addAttribute("especialidades", especialidadRepository.findAll());
        model.addAttribute("consultorios", consultorioRepository.findAll());

        return "inicio";
    }
    @PostMapping("/roles/agregar")
    public String agregarRol(@RequestParam String nombre) {

        Rol rol = new Rol();
        rol.setNombre(nombre);

        rolRepository.save(rol);

        return "redirect:/";
    }
}