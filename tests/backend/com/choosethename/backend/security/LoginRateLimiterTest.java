package com.choosethename.backend.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

class LoginRateLimiterTest {

    private LoginRateLimiter limiter;

    @BeforeEach
    void setUp() {
        limiter = new LoginRateLimiter();
    }

    @Test
    void shouldAllowUpToFiveFailuresAndBlockTheSixth() {
        for (int i = 0; i < 5; i++) {
            assertFalse(limiter.isBlocked("203.0.113.1"), "attempt " + (i + 1) + " should be allowed");
            limiter.onFailure("203.0.113.1");
        }
        assertTrue(limiter.isBlocked("203.0.113.1"));
    }

    @Test
    void shouldResetTheCounterOnSuccess() {
        for (int i = 0; i < 5; i++) {
            limiter.onFailure("203.0.113.2");
        }
        assertTrue(limiter.isBlocked("203.0.113.2"));

        limiter.onSuccess("203.0.113.2");

        assertFalse(limiter.isBlocked("203.0.113.2"));
        limiter.onFailure("203.0.113.2");
        assertFalse(limiter.isBlocked("203.0.113.2"));
    }

    @Test
    void shouldLetFailuresExpireAfterTheWindow() throws Exception {
        Field field = LoginRateLimiter.class.getDeclaredField("failuresByIp");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        var failuresByIp = (java.util.concurrent.ConcurrentMap<String, List<Instant>>) field.get(limiter);

        failuresByIp.put("203.0.113.3", new java.util.ArrayList<>(List.of(
                Instant.now().minus(LoginRateLimiter.WINDOW).minusSeconds(60),
                Instant.now().minus(LoginRateLimiter.WINDOW).minusSeconds(30)
        )));

        assertFalse(limiter.isBlocked("203.0.113.3"));
    }
}