package com.hospital.sistemamedico.config;

import com.hospital.sistemamedico.security.CustomAuthenticationFailureHandler;
import com.hospital.sistemamedico.security.CustomAuthenticationSuccessHandler;
import com.hospital.sistemamedico.security.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuración de seguridad COMPLETA (Fases 1 a 5). Las reglas específicas van ANTES de la
 * general de /admin/**: Spring usa la primera que coincide. Las rutas de módulos que aún no
 * hayas instalado simplemente no existen todavía; no causan ningún problema.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final CustomAuthenticationSuccessHandler successHandler;
    private final CustomAuthenticationFailureHandler failureHandler;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/registro/**", "/verificar-dpi", "/css/**", "/js/**", "/images/**").permitAll()
                        .requestMatchers("/login").permitAll()
                        // ---- Personal operativo
                        .requestMatchers("/admin/caja/**").hasAnyRole("CAJERO", "ADMINISTRADOR")
                        .requestMatchers("/admin/recepcion/**").hasAnyRole("RECEPCIONISTA", "ADMINISTRADOR")
                        .requestMatchers("/admin/enfermeria/**").hasRole("ENFERMERO")
                        .requestMatchers("/admin/medico/**", "/admin/agenda/**").hasRole("MEDICO")
                        .requestMatchers("/admin/laboratorio/**").hasAnyRole("LABORATORISTA", "ADMINISTRADOR")
                        .requestMatchers("/admin/farmacia/**").hasAnyRole("FARMACEUTICO", "ADMINISTRADOR")
                        // ---- Administración
                        .requestMatchers("/admin/usuarios/**", "/admin/catalogos/**", "/admin/sedes-especialidades/**")
                        .hasRole("ADMINISTRADOR")
                        .requestMatchers("/admin/**").hasAnyRole(
                                "ADMINISTRADOR", "RECEPCIONISTA", "CAJERO", "MEDICO",
                                "ENFERMERO", "LABORATORISTA", "FARMACEUTICO")
                        .requestMatchers("/paciente/**").hasRole("PACIENTE")
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .successHandler(successHandler)
                        .failureHandler(failureHandler)
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/?logout")
                        .permitAll()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                )
                .authenticationProvider(authenticationProvider());

        return http.build();
    }
}
