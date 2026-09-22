package com.tz.forensics.config;

import com.tz.forensics.service.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;

    public SecurityConfig(CustomUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider auth = new DaoAuthenticationProvider();
        auth.setUserDetailsService(userDetailsService);
        auth.setPasswordEncoder(passwordEncoder());
        return auth;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authenticationProvider(authenticationProvider())
            .authorizeHttpRequests(auth -> auth
                // ===== PUBLIC (bila login) =====
                .requestMatchers(
                    "/login", "/register",
                    "/css/**", "/js/**", "/images/**",
                    "/error", "/access-denied",
                    "/google*.html", "/*.html",
                    "/sitemap.xml", "/robots.txt"
                ).permitAll()

                // ===== WHISTLEBLOWER — PUBLIC =====
                .requestMatchers(
                    "/whistleblower",
                    "/whistleblower/report",
                    "/whistleblower/submit",
                    "/whistleblower/success",
                    "/whistleblower/track",
                    "/whistleblower/track/**"
                ).permitAll()

                // ===== ADMIN + PROFESSIONALS PEKEE =====
                // Incidents — Admin, Cyber Pro, Forensics PEKEE
                .requestMatchers("/incidents/**").hasAnyRole("ADMIN", "CYBER_PRO", "FORENSICS")
                
                // Threat Map — Admin, Cyber Pro, Forensics PEKEE
                .requestMatchers("/threat-map/**").hasAnyRole("ADMIN", "CYBER_PRO", "FORENSICS")
                .requestMatchers("/threat-map").hasAnyRole("ADMIN", "CYBER_PRO", "FORENSICS")
                
                // Cases — Admin, Cyber Pro, Forensics PEKEE
                .requestMatchers("/cases/**").hasAnyRole("ADMIN", "CYBER_PRO", "FORENSICS")
                
                // Audit — Admin, Cyber Pro, Forensics PEKEE
                .requestMatchers("/audit/**").hasAnyRole("ADMIN", "CYBER_PRO", "FORENSICS")

                // ===== ADMIN + CYBER PRO =====
                // Attack Reports (admin view) — Admin, Cyber Pro PEKEE
                .requestMatchers("/report-attack/admin/**").hasAnyRole("ADMIN", "CYBER_PRO", "FORENSICS")

                // ===== ADMIN ONLY =====
                .requestMatchers("/admin/**").hasRole("ADMIN")

                // ===== WHISTLEBLOWER ADMIN =====
                .requestMatchers("/whistleblower/admin/**").hasAnyRole("ADMIN", "CYBER_PRO")

                // ===== EVERYONE LOGGED IN =====
                .requestMatchers("/dashboard/**").authenticated()
                .requestMatchers("/notifications/**").authenticated()
                .requestMatchers("/tools/**").authenticated()
                .requestMatchers("/report-attack/**").authenticated()
                .requestMatchers("/my-accounts/**").authenticated()
                .requestMatchers("/ai/**").authenticated()
                .requestMatchers("/evidence/**").authenticated()
                .requestMatchers("/reports/**").authenticated()

                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/dashboard", true)
                .failureUrl("/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .permitAll()
            )
            .exceptionHandling(ex -> ex
                .accessDeniedPage("/access-denied")
            )
            .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"));
        return http.build();
    }
}
