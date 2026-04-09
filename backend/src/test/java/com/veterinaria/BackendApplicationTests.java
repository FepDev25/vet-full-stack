package com.veterinaria;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Test de integración: verifica que el ApplicationContext arranca correctamente
 * con todos sus beans correctamente configurados.
 *
 * Prerequisito: `docker-compose up -d` en la raíz del proyecto.
 * El perfil "test" usa ddl-auto: none → solo verifica la conexión,
 * no intenta crear ni validar el esquema.
 */
@SpringBootTest
@ActiveProfiles("test")
class BackendApplicationTests {

    @Test
    void contextLoads() {
        // Si el contexto Spring arranca sin excepción, todos los beans
        // están correctamente definidos y conectados.
    }
}
