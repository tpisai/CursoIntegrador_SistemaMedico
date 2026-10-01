package com.cis.citas_salud.api;

import com.cis.citas_salud.api.seguridad.SesionArgumentResolver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class ApiConfig implements WebMvcConfigurer {

    private final SesionArgumentResolver sesionArgumentResolver;
    private final String[] origenesPermitidos;

    public ApiConfig(SesionArgumentResolver sesionArgumentResolver,
                     @Value("${saludgrau.cors.origenes:http://localhost:3000}") String[] origenesPermitidos) {
        this.sesionArgumentResolver = sesionArgumentResolver;
        this.origenesPermitidos = origenesPermitidos;
    }

    /** PIN guardado con BCrypt (compatible con crypt() de pgcrypto usado en los datos iniciales). */
    @Bean
    public BCryptPasswordEncoder pinEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(sesionArgumentResolver);
    }

    // El frontend en desarrollo usa el proxy de Next.js; CORS queda para clientes que llamen directo.
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(origenesPermitidos)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE");
    }
}
