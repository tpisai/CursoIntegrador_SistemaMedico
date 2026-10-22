package com.cis.citas_salud;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling // Recordatorios automáticos de citas y campañas (RecordatorioService).
public class CitasSaludApplication {

	public static void main(String[] args) {
		SpringApplication.run(CitasSaludApplication.class, args);
	}

}
