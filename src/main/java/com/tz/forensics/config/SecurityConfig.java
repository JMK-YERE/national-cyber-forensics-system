package com.tz.forensics.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                // PUBLIC
                .requestMatchers(
                    "/", "/features", "/about", "/contact",
                    "/login", "/register", "/access-denied", "/error",
                    "/forgot-password", "/reset-password",
                    "/whistleblower", "/whistleblower/**",
                    "/css/**", "/js/**", "/images/**", "/static/**",
                    "/favicon.ico", "/webjars/**",
                    "/announcements", "/announcements/**"
                ).permitAll()
                // ADMIN
                .requestMatchers("/admin/**", "/report-attack/admin/**")
                .hasAnyAuthority("ROLE_ADMIN", "ROLE_CYBER_PRO", "ROLE_FORENSICS", "ROLE_ANALYST")
                // EVERYTHING ELSE
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/dashboard", true)
                .failureUrl("/login?error")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            );
        return http.build();
    }
}
