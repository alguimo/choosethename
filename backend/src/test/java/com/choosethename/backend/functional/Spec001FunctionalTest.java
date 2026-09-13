package com.choosethename.backend.functional;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class Spec001FunctionalTest {

    @LocalServerPort
    private int port;

    @Test
    void testSpec001Scenarios() {
        SystemClient client = new RestAssuredSystemClient(port);

        // TS-1: Register successfully
        assertEquals(201, client.register("newuser", "Password123"));

        // TS-2: Duplicate username
        assertEquals(409, client.register("newuser", "Password123"));

        // TS-3: Login valid
        String token = client.login("newuser", "Password123");
        assertNotNull(token);

        // TS-4: Login invalid
        assertNull(client.login("newuser", "WrongPass"));

        // TS-5: Access protected
        assertEquals(200, client.getProtectedResource(token));
        assertEquals(401, client.getProtectedResource("invalid.token"));
    }
}