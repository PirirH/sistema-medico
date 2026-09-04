package com.hospital.sistemamedico.security;

import com.hospital.sistemamedico.repository.InternalUserRepository;
import com.hospital.sistemamedico.repository.PatientRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class CustomAuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final PatientRepository patientRepository;
    private final InternalUserRepository internalUserRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        AppUserPrincipal principal = (AppUserPrincipal) authentication.getPrincipal();
        boolean isPatient = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_PACIENTE"::equals);

        if (isPatient) {
            patientRepository.findByUsername(principal.getUsername()).ifPresent(p -> {
                p.setFailedLoginAttempts(0);
                p.setLockedUntil(null);
                patientRepository.save(p);
            });
            getRedirectStrategy().sendRedirect(request, response, "/paciente/dashboard");
        } else {
            internalUserRepository.findByUsername(principal.getUsername()).ifPresent(u -> {
                u.setFailedLoginAttempts(0);
                u.setLockedUntil(null);
                internalUserRepository.save(u);
            });
            getRedirectStrategy().sendRedirect(request, response, "/admin/dashboard");
        }
    }
}