package com.hospital.sistemamedico.security;

import com.hospital.sistemamedico.model.entity.InternalUser;
import com.hospital.sistemamedico.model.entity.Patient;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public class AppUserPrincipal implements UserDetails {

    private final Integer id;
    private final String username;
    private final String password;
    private final String fullName;
    private final boolean active;
    private final LocalDateTime lockedUntil;
    private final Collection<GrantedAuthority> authorities;

    private AppUserPrincipal(Integer id, String username, String password, String fullName,
                             boolean active, LocalDateTime lockedUntil, String roleName) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.active = active;
        this.lockedUntil = lockedUntil;
        this.authorities = List.of(new SimpleGrantedAuthority("ROLE_" + roleName.toUpperCase()));
    }

    public static AppUserPrincipal fromPatient(Patient p) {
        return new AppUserPrincipal(p.getId(), p.getUsername(), p.getPassword(), p.getFullName(),
                p.isActive(), p.getLockedUntil(), "PACIENTE");
    }

    public static AppUserPrincipal fromInternalUser(InternalUser u) {
        return new AppUserPrincipal(u.getId(), u.getUsername(), u.getPassword(), u.getFullName(),
                u.isActive(), u.getLockedUntil(), u.getRole().getName());
    }

    public Integer getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonLocked() {
        return lockedUntil == null || lockedUntil.isBefore(LocalDateTime.now());
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}