package com.choosethename.backend.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", "REDACTED");
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 86400000L);
    }

    @Test
    void shouldGenerateValidToken() {
        UserDetails user = User.withUsername("testuser")
                .password("password")
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_PARTICIPANT")))
                .build();

        String token = jwtService.generateToken(user);

        assertNotNull(token);
        assertEquals("testuser", jwtService.extractUsername(token));
    }
}
