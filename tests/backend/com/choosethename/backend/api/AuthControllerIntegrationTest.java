package com.choosethename.backend.api;

import com.choosethename.backend.dto.AuthResponseDTO;
import com.choosethename.backend.dto.LoginRequestDTO;
import com.choosethename.backend.dto.RegisterRequestDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

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
}