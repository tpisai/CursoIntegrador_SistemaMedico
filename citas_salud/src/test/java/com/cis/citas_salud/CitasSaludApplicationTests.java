package com.cis.citas_salud;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// Levanta todo el contexto y necesita PostgreSQL, por eso ./mvnw test no la corre.
// A pedido: ./mvnw test -DexcludedGroups= -Dtest=CitasSaludApplicationTests
@Tag("integracion")
@SpringBootTest
class CitasSaludApplicationTests {

	@Test
	void contextLoads() {
	}

}
