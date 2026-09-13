package com.choosethename.backend.service;

import com.choosethename.backend.dto.AuthResponseDTO;
import com.choosethename.backend.dto.LoginRequestDTO;
import com.choosethename.backend.model.Role;
import com.choosethename.backend.model.User;
import com.choosethename.backend.repository.UserRepository;
import com.choosethename.backend.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;

    @InjectMocks private AuthenticationService authenticationService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setPasswordHash("encodedPassword");
        testUser.setRole(Role.PARTICIPANT);
    }

    @Test
    void authenticate_ShouldReturnToken_WhenCredentialsValid() {
        LoginRequestDTO request = new LoginRequestDTO();
        request.setUsername("testuser");
        request.setPassword("password");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password", "encodedPassword")).thenReturn(true);
        when(jwtService.generateToken(any())).thenReturn("mocked-jwt");

        AuthResponseDTO response = authenticationService.authenticate(request);

        assertNotNull(response);
        assertEquals("mocked-jwt", response.getAccessToken());
        assertEquals("Bearer", response.getTokenType());
    }

    @Test
    void authenticate_ShouldThrowException_WhenUserNotFound() {
        LoginRequestDTO request = new LoginRequestDTO();
        request.setUsername("nonexistent");
        request.setPassword("password");

        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        assertThrows(BadCredentialsException.class, () -> authenticationService.authenticate(request));
    }

    @Test
    void authenticate_ShouldThrowException_WhenPasswordInvalid() {
        LoginRequestDTO request = new LoginRequestDTO();
        request.setUsername("testuser");
        request.setPassword("wrong");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrong", "encodedPassword")).thenReturn(false);

        assertThrows(BadCredentialsException.class, () -> authenticationService.authenticate(request));
    }
}
