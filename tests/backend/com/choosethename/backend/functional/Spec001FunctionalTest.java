package com.choosethename.backend.functional;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class Spec001FunctionalTest extends FunctionalTestBase {

    @Test
    void testSpec001Scenarios() {
        // TS-1: Register successfully
        assertThat(client.register("newuser", PASSWORD).getStatusCode()).isEqualTo(201);

        // TS-2: Duplicate username
        assertThat(client.register("newuser", PASSWORD).getStatusCode()).isEqualTo(409);

        // TS-3: Login valid
        String token = client.login("newuser", PASSWORD).jsonPath().getString("accessToken");
        assertThat(token).isNotNull();

        // TS-4: Login invalid
        assertThat(client.login("newuser", "WrongPass").getStatusCode()).isEqualTo(401);

        // TS-5: Access protected
        assertThat(client.getProtectedResource(token).getStatusCode()).isEqualTo(200);
        assertThat(client.getProtectedResource("invalid.token").getStatusCode()).isEqualTo(401);
    }
}