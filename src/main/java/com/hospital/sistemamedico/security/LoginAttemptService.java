package com.hospital.sistemamedico.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class LoginAttemptService {

    @Value("${app.security.max-login-attempts:5}")
    private int maxAttempts;

    @Value("${app.security.lock-duration-minutes:15}")
    private int lockDurationMinutes;

    public boolean isLocked(LocalDateTime lockedUntil) {
        return lockedUntil != null && lockedUntil.isAfter(LocalDateTime.now());
    }

    public int registerFailedAttempt(int currentAttempts) {
        return currentAttempts + 1;
    }

    public boolean shouldLock(int failedAttempts) {
        return failedAttempts >= maxAttempts;
    }

    public LocalDateTime calculateLockUntil() {
        return LocalDateTime.now().plusMinutes(lockDurationMinutes);
    }

    public int remainingAttempts(int failedAttempts) {
        return Math.max(0, maxAttempts - failedAttempts);
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public int getLockDurationMinutes() {
        return lockDurationMinutes;
    }
}