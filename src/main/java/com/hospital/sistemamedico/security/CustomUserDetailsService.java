package com.hospital.sistemamedico.security;

import com.hospital.sistemamedico.repository.InternalUserRepository;
import com.hospital.sistemamedico.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final PatientRepository patientRepository;
    private final InternalUserRepository internalUserRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return patientRepository.findByUsername(username)
                .map(AppUserPrincipal::fromPatient)
                .map(UserDetails.class::cast)
                .orElseGet(() -> internalUserRepository.findByUsername(username)
                        .map(AppUserPrincipal::fromInternalUser)
                        .orElseThrow(() -> new UsernameNotFoundException(
                                "No existe un usuario con el nombre: " + username)));
    }
}