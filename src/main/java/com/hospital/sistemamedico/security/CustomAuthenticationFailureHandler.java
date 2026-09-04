package com.hospital.sistemamedico.security;

import com.hospital.sistemamedico.model.entity.InternalUser;
import com.hospital.sistemamedico.model.entity.Patient;
import com.hospital.sistemamedico.repository.InternalUserRepository;
import com.hospital.sistemamedico.repository.PatientRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class CustomAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private final PatientRepository patientRepository;
    private final InternalUserRepository internalUserRepository;
    private final LoginAttemptService loginAttemptService;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {

        String username = request.getParameter("username");
        String message = "Las credenciales ingresadas son incorrectas.";

        if (username != null) {
            var patientOpt = patientRepository.findByUsername(username);
            if (patientOpt.isPresent()) {
                message = applyFailedAttempt(patientOpt.get());
            } else {
                var internalOpt = internalUserRepository.findByUsername(username);
                if (internalOpt.isPresent()) {
                    message = applyFailedAttempt(internalOpt.get());
                }
            }
        }

        String encoded = URLEncoder.encode(message, StandardCharsets.UTF_8);
        setDefaultFailureUrl("/login?error&message=" + encoded);
        super.onAuthenticationFailure(request, response, exception);
    }

    private String applyFailedAttempt(Patient patient) {
        int attempts = loginAttemptService.registerFailedAttempt(patient.getFailedLoginAttempts());
        patient.setFailedLoginAttempts(attempts);
        if (loginAttemptService.shouldLock(attempts)) {
            patient.setLockedUntil(loginAttemptService.calculateLockUntil());
            patientRepository.save(patient);
            return "Cuenta bloqueada temporalmente. Intente de nuevo en "
                    + loginAttemptService.getLockDurationMinutes() + " minutos.";
        }
        patientRepository.save(patient);
        return "Usuario o contraseña incorrectos. Intentos restantes: "
                + loginAttemptService.remainingAttempts(attempts) + ".";
    }

    private String applyFailedAttempt(InternalUser user) {
        int attempts = loginAttemptService.registerFailedAttempt(user.getFailedLoginAttempts());
        user.setFailedLoginAttempts(attempts);
        if (loginAttemptService.shouldLock(attempts)) {
            user.setLockedUntil(loginAttemptService.calculateLockUntil());
            internalUserRepository.save(user);
            return "Su cuenta ha sido bloqueada temporalmente por múltiples intentos fallidos. Contacte al administrador del sistema.";
        }
        internalUserRepository.save(user);
        return "Las credenciales ingresadas son incorrectas. Tiene "
                + loginAttemptService.remainingAttempts(attempts) + " intentos restantes antes del bloqueo temporal.";
    }
}