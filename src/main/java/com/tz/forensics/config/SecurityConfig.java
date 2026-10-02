package com.tz.forensics.config;

import com.tz.forensics.service.CustomOAuth2UserService;
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

    private final CustomOAuth2UserService oauth2UserService;
    private final OAuth2LoginSuccessHandler oauth2Handler;

    public SecurityConfig(CustomOAuth2UserService oauth2UserService,
                          OAuth2LoginSuccessHandler oauth2Handler) {
        this.oauth2UserService = oauth2UserService;
        this.oauth2Handler = oauth2Handler;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/", "/features", "/about", "/contact",
                    "/login", "/register", "/access-denied", "/error",
                    "/forgot-password", "/reset-password",
                    "/whistleblower", "/whistleblower/**",
                    "/css/**", "/js/**", "/images/**", "/static/**",
                    "/favicon.ico", "/webjars/**",
                    "/announcements", "/announcements/**",
                    "/oauth2/**", "/login/oauth2/**"
                ).permitAll()
                .requestMatchers("/admin/**", "/report-attack/admin/**")
                .hasAnyAuthority("ROLE_ADMIN", "ROLE_CYBER_PRO", "ROLE_FORENSICS", "ROLE_ANALYST")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/dashboard", true)
                .failureUrl("/login?error")
                .permitAll()
            )
            .oauth2Login(oauth -> oauth
                .loginPage("/login")
                .userInfoEndpoint(u -> u.userService(oauth2UserService))
                .successHandler(oauth2Handler)
                .failureUrl("/login?error")
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            );
        return http.build();
    }
}
