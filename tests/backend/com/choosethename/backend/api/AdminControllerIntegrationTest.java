package com.choosethename.backend.api;

import com.choosethename.backend.model.Role;
import com.choosethename.backend.model.User;
import com.choosethename.backend.repository.UserRepository;
import com.choosethename.backend.security.LoginRateLimiter;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AdminControllerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ApplicationContext applicationContext;

    private final String adminUsername = "admin" + System.nanoTime() % 100000;
    private final String adminPassword = "Admin123";
    private final String participantUsername = "participant" + System.nanoTime() % 100000;
    private final String participantPassword = "Participant123";

    @BeforeEach
    void setUp() throws Exception {
        RestAssured.port = port;

        LoginRateLimiter limiter = applicationContext.getBean(LoginRateLimiter.class);
        Field field = LoginRateLimiter.class.getDeclaredField("failuresByIp");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        ConcurrentMap<String, List<Instant>> failuresByIp =
                (ConcurrentMap<String, List<Instant>>) field.get(limiter);
        failuresByIp.clear();

        userRepository.deleteAll();

        User admin = new User();
        admin.setUsername(adminUsername);
        admin.setPasswordHash(passwordEncoder.encode(adminPassword));
        admin.setRole(Role.ADMIN);
        userRepository.save(admin);

        register(participantUsername, participantPassword);
    }

    @Test
    void shouldListUsersAsAdminWithoutExposingPasswordHash() {
        Response response = given()
                .header("Authorization", "Bearer " + adminToken())
                .get("/api/v1/admin/users");

        assertEquals(200, response.getStatusCode());
        List<String> usernames = response.jsonPath().getList("username");
        assertEquals(2, usernames.size());
        assertEquals(List.of(adminUsername, participantUsername), usernames.stream().sorted().toList());

        String rawBody = given()
                .header("Authorization", "Bearer " + adminToken())
                .get("/api/v1/admin/users")
                .getBody().asString();
        assertFalse(rawBody.contains("passwordHash"), "admin payload must not contain passwordHash");
    }

    @Test
    void shouldForbidAdminAccessForParticipant() {
        Response response = given()
                .header("Authorization", "Bearer " + participantToken())
                .get("/api/v1/admin/users");
        assertEquals(403, response.getStatusCode());
    }

    @Test
    void shouldReturn401ForAdminEndpointsWithoutToken() {
        assertEquals(401, given().get("/api/v1/admin/users").getStatusCode());

        Response reset = given()
                .contentType(ContentType.JSON)
                .body(Map.of("newPassword", "NewPassword123"))
                .patch("/api/v1/admin/users/1/password");
        assertEquals(401, reset.getStatusCode());
    }

    @Test
    void shouldResetPasswordAndAllowLoginWithNewPassword() {
        Long userId = userRepository.findByUsername(participantUsername).orElseThrow().getId();

        Response response = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + adminToken())
                .body(Map.of("newPassword", "ResetPass123"))
                .patch("/api/v1/admin/users/{id}/password", userId);
        assertEquals(200, response.getStatusCode());

        assertEquals(401, login(participantUsername, participantPassword).getStatusCode());

        Response newLogin = login(participantUsername, "ResetPass123");
        assertEquals(200, newLogin.getStatusCode());
        assertNotNull(newLogin.jsonPath().getString("accessToken"));
    }

    @Test
    void shouldRejectWeakPasswordOnReset() {
        Long userId = userRepository.findByUsername(participantUsername).orElseThrow().getId();

        Response response = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + adminToken())
                .body(Map.of("newPassword", "weak"))
                .patch("/api/v1/admin/users/{id}/password", userId);
        assertEquals(400, response.getStatusCode());
    }

    @Test
    void shouldReturn404WhenResettingPasswordForUnknownUser() {
        Response response = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + adminToken())
                .body(Map.of("newPassword", "ResetPass123"))
                .patch("/api/v1/admin/users/999999999/password");
        assertEquals(404, response.getStatusCode());
    }

    private String adminToken() {
        return login(adminUsername, adminPassword).jsonPath().getString("accessToken");
    }

    private String participantToken() {
        return login(participantUsername, participantPassword).jsonPath().getString("accessToken");
    }

    private Response login(String username, String password) {
        return given()
                .contentType(ContentType.JSON)
                .body(Map.of("username", username, "password", password))
                .post("/api/v1/auth/login");
    }

    private void register(String username, String password) {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("username", username, "password", password))
                .post("/api/v1/auth/register")
                .then()
                .statusCode(201);
    }
}