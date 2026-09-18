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
        ReflectionTestUtils.setField(jwtService, "secretKey", "402e57849e7943d2c65a0b72a6b2c8a9e4d5f6a7b8c9d0e1f2a3b4c5d6e7f8a9");
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
