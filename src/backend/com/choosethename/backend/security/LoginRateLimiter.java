package com.choosethename.backend.security;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class LoginRateLimiter {

    static final int MAX_FAILURES = 5;
    static final Duration WINDOW = Duration.ofMinutes(15);

    private final ConcurrentMap<String, List<Instant>> failuresByIp = new ConcurrentHashMap<>();

    public boolean isBlocked(String clientIp) {
        List<Instant> failures = failuresByIp.get(clientIp);
        if (failures == null) {
            return false;
        }
        synchronized (failures) {
            prune(failures);
            return failures.size() >= MAX_FAILURES;
        }
    }

    public void onFailure(String clientIp) {
        List<Instant> failures = failuresByIp.computeIfAbsent(clientIp, key -> new ArrayList<>());
        synchronized (failures) {
            failures.add(Instant.now());
            prune(failures);
        }
    }

    public void onSuccess(String clientIp) {
        failuresByIp.remove(clientIp);
    }

    private void prune(List<Instant> failures) {
        Instant cutoff = Instant.now().minus(WINDOW);
        failures.removeIf(timestamp -> timestamp.isBefore(cutoff));
        if (failures.isEmpty()) {
            removeEntryReferencing(failures);
        }
    }

    private void removeEntryReferencing(List<Instant> failures) {
        failuresByIp.entrySet().removeIf(entry -> entry.getValue() == failures);
    }
}