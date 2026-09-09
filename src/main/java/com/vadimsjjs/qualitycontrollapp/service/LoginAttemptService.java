package com.vadimsjjs.qualitycontrollapp.service;

import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginAttemptService {

    private final Map<String, LoginAttempt> loginAttemptCache = new ConcurrentHashMap<>();
    public static final int MAX_ATTEMPTS = 8;
    public static final long BLOCK_DURATION_MINUTES = 15L;

    public void loginFailed(String key) {
        registerFailedLogin(key);
    }

    public void loginSucceeded(String key) {
        resetAttempts(key);
    }

    public boolean isLocked(String key) {
        return isBlocked(key);
    }

    public long getLockRemainingTime(String key) {
        LoginAttempt attempt = loginAttemptCache.get(key);
        if (attempt == null || attempt.getBlockUntil() == null) {
            return 0;
        }
        Instant now = Instant.now();
        Instant blockUntil = attempt.getBlockUntil();
        if (now.isAfter(blockUntil)) {
            loginAttemptCache.remove(key);
            return 0;
        }
        long remainingSeconds = blockUntil.getEpochSecond() - now.getEpochSecond();
        return (remainingSeconds + 59) / 60; // округляем вверх до минут
    }

    public void registerFailedLogin(String key) {
        LoginAttempt attempt = loginAttemptCache.getOrDefault(key, new LoginAttempt());
        if (attempt.isBlocked()) return;
        attempt.increment();
        loginAttemptCache.put(key, attempt);
    }

    public boolean isBlocked(String key) {
        LoginAttempt attempt = loginAttemptCache.get(key);
        if (attempt == null) return false;
        if (attempt.isBlocked()) {
            if (Instant.now().isAfter(attempt.getBlockUntil())) {
                loginAttemptCache.remove(key);
                return false;
            }
            return true;
        }
        return false;
    }

    public void resetAttempts(String key) {
        loginAttemptCache.remove(key);
    }

    private static class LoginAttempt {
        private int attempts = 0;
        private Instant blockUntil = null;

        void increment() {
            attempts++;
            if (attempts >= MAX_ATTEMPTS) {
                blockUntil = Instant.now().plusSeconds(BLOCK_DURATION_MINUTES * 60);
            }
        }

        boolean isBlocked() {
            return blockUntil != null && Instant.now().isBefore(blockUntil);
        }

        int getAttempts() { return attempts; }
        Instant getBlockUntil() { return blockUntil; }
    }
}
