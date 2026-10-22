package com.cis.citas_salud.service;

import com.cis.citas_salud.entity.Usuario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Canal externo de las notificaciones (RF-22). Spring Boot crea el JavaMailSender solo si está
 * configurado spring.mail.host; sin él, el correo se registra en el log para no frenar el sistema.
 */
@Service
public class CorreoService {

    private static final Logger log = LoggerFactory.getLogger(CorreoService.class);

    private final ObjectProvider<JavaMailSender> mailSender;
    private final String remitente;

    public CorreoService(ObjectProvider<JavaMailSender> mailSender,
                         @Value("${saludgrau.correo.remitente:SaludGrau <no-responder@saludgrau.pe>}") String remitente) {
        this.mailSender = mailSender;
        this.remitente = remitente;
    }

    /** Devuelve true si el correo salió por SMTP. Un fallo de envío no interrumpe al que llama. */
    public boolean enviar(Usuario destinatario, String asunto, String texto) {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null) {
            log.info("Correo sin servidor SMTP configurado para {}: {}", destinatario.getCorreo(), asunto);
            return false;
        }
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setFrom(remitente);
        mensaje.setTo(destinatario.getCorreo());
        mensaje.setSubject(asunto);
        mensaje.setText("Hola, " + destinatario.getNombreCorto() + ":\n\n" + texto
                + "\n\nCentro de Salud Miguel Grau · Chaclacayo\nEste es un mensaje automático de SaludGrau.");
        try {
            sender.send(mensaje);
            return true;
        } catch (MailException e) {
            log.warn("No se pudo enviar el correo a {}: {}", destinatario.getCorreo(), e.getMessage());
            return false;
        }
    }
}
