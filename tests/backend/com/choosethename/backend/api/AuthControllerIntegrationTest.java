package com.choosethename.backend.api;

import com.choosethename.backend.dto.AuthResponseDTO;
import com.choosethename.backend.dto.LoginRequestDTO;
import com.choosethename.backend.dto.RegisterRequestDTO;
import com.choosethename.backend.dto.UserDTO;
import com.choosethename.backend.security.LoginRateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ApplicationContext applicationContext;

    @BeforeEach
    void resetRateLimiter() throws Exception {
        LoginRateLimiter limiter = applicationContext.getBean(LoginRateLimiter.class);
        Field field = LoginRateLimiter.class.getDeclaredField("failuresByIp");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        ConcurrentMap<String, List<Instant>> failuresByIp =
                (ConcurrentMap<String, List<Instant>>) field.get(limiter);
        failuresByIp.clear();
    }

    @Test
    void shouldReturn401WhenAccessingProtectedEndpointWithoutToken() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/v1/test/protected", String.class);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void shouldReturn401WhenAccessingProtectedEndpointWithInvalidToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer invalid.token");
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/test/protected", HttpMethod.GET, new HttpEntity<>(headers), String.class);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void shouldLoginAndAccessProtectedEndpoint() {
        String username = "loginuser";
        String password = "Password123";

        RegisterRequestDTO registerRequest = new RegisterRequestDTO();
        registerRequest.setUsername(username);
        registerRequest.setPassword(password);
        restTemplate.postForEntity(
                "/api/v1/auth/register",
                new HttpEntity<>(registerRequest, jsonHeaders()),
                Void.class
        );

        LoginRequestDTO loginRequest = new LoginRequestDTO();
        loginRequest.setUsername(username);
        loginRequest.setPassword(password);
        ResponseEntity<AuthResponseDTO> loginResponse = restTemplate.postForEntity(
                "/api/v1/auth/login",
                new HttpEntity<>(loginRequest, jsonHeaders()),
                AuthResponseDTO.class
        );

        assertEquals(HttpStatus.OK, loginResponse.getStatusCode());
        AuthResponseDTO auth = loginResponse.getBody();
        assertNotNull(auth);
        assertNotNull(auth.getAccessToken());
        assertEquals("Bearer", auth.getTokenType());

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + auth.getAccessToken());
        ResponseEntity<String> protectedResponse = restTemplate.exchange(
                "/api/v1/test/protected", HttpMethod.GET, new HttpEntity<>(headers), String.class);
        assertEquals(HttpStatus.OK, protectedResponse.getStatusCode());
    }

    private HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    @Test
    void shouldAccessMeEndpointWithValidToken() {
        String username = "meuser";
        String password = "Password123";

        RegisterRequestDTO registerRequest = new RegisterRequestDTO();
        registerRequest.setUsername(username);
        registerRequest.setPassword(password);
        restTemplate.postForEntity(
                "/api/v1/auth/register",
                new HttpEntity<>(registerRequest, jsonHeaders()),
                Void.class
        );

        LoginRequestDTO loginRequest = new LoginRequestDTO();
        loginRequest.setUsername(username);
        loginRequest.setPassword(password);
        ResponseEntity<AuthResponseDTO> loginResponse = restTemplate.postForEntity(
                "/api/v1/auth/login",
                new HttpEntity<>(loginRequest, jsonHeaders()),
                AuthResponseDTO.class
        );

        String token = loginResponse.getBody().getAccessToken();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        ResponseEntity<UserDTO> meResponse = restTemplate.exchange(
                "/api/v1/auth/me", HttpMethod.GET, new HttpEntity<>(headers), UserDTO.class);

        assertEquals(HttpStatus.OK, meResponse.getStatusCode());
        UserDTO me = meResponse.getBody();
        assertNotNull(me);
        assertNotNull(me.getId());
        assertEquals(username, me.getUsername());
        assertNotNull(me.getRole());
    }

    @Test
    void shouldReturn401WhenAccessingMeWithoutToken() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/v1/auth/me", String.class);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void shouldReturn429AfterFailedLoginsAndResetTheCounterOnSuccess() {
        LoginRequestDTO failedLogin = new LoginRequestDTO();
        failedLogin.setUsername("nobody");
        failedLogin.setPassword("WrongPass1");

        for (int i = 0; i < 4; i++) {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    "/api/v1/auth/login",
                    new HttpEntity<>(failedLogin, jsonHeaders()),
                    String.class);
            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode(), "attempt " + (i + 1));
        }

        String username = "ratelimituser";
        String password = "Password123";
        RegisterRequestDTO registerRequest = new RegisterRequestDTO();
        registerRequest.setUsername(username);
        registerRequest.setPassword(password);
        restTemplate.postForEntity(
                "/api/v1/auth/register",
                new HttpEntity<>(registerRequest, jsonHeaders()),
                Void.class
        );

        LoginRequestDTO successLogin = new LoginRequestDTO();
        successLogin.setUsername(username);
        successLogin.setPassword(password);
        ResponseEntity<AuthResponseDTO> success = restTemplate.postForEntity(
                "/api/v1/auth/login",
                new HttpEntity<>(successLogin, jsonHeaders()),
                AuthResponseDTO.class);
        assertEquals(HttpStatus.OK, success.getStatusCode());

        for (int i = 0; i < 5; i++) {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    "/api/v1/auth/login",
                    new HttpEntity<>(failedLogin, jsonHeaders()),
                    String.class);
            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode(), "post-reset attempt " + (i + 1));
        }

        ResponseEntity<String> blocked = restTemplate.postForEntity(
                "/api/v1/auth/login",
                new HttpEntity<>(failedLogin, jsonHeaders()),
                String.class);
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, blocked.getStatusCode());
    }
}