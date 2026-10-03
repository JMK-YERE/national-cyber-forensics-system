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

    public SecurityConfig(CustomOAuth2UserService oauth2UserService, OAuth2LoginSuccessHandler oauth2Handler) {
        this.oauth2UserService = oauth2UserService;
        this.oauth2Handler = oauth2Handler;
    }

    @Bean
    public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/", "/features", "/about", "/contact",
                    "/login", "/register", "/access-denied", "/error",
                    "/forgot-password", "/forgot-password/sms", "/reset-password", "/set-password", "/verify-email",
                    "/whistleblower", "/whistleblower/report", "/whistleblower/submit",
                    "/whistleblower/success", "/whistleblower/track", "/whistleblower/track/**",
                    "/css/**", "/js/**", "/images/**", "/static/**",
                    "/favicon.ico", "/webjars/**",
                    "/announcements", "/announcements/**",
                    "/oauth2/**", "/login/oauth2/**"
                ).permitAll()

                .requestMatchers("/admin/**", "/whistleblower/admin/**")
                    .hasAuthority("ROLE_ADMIN")

                .requestMatchers("/compliance", "/compliance/**")
                    .hasAnyAuthority("ROLE_ADMIN", "ROLE_CYBER_PRO", "ROLE_FORENSICS")

                .requestMatchers("/admin/audit", "/admin/audit/**")
                    .hasAuthority("ROLE_ADMIN")

                .requestMatchers("/report-attack/admin", "/report-attack/admin/**", "/incidents/**", "/evidence/**")
                    .hasAnyAuthority("ROLE_ADMIN", "ROLE_CYBER_PRO", "ROLE_FORENSICS", "ROLE_ANALYST")

                .requestMatchers("/threat-map", "/threat-map/**")
                    .hasAnyAuthority("ROLE_ADMIN", "ROLE_CYBER_PRO", "ROLE_FORENSICS", "ROLE_ANALYST")

                .requestMatchers("/tools/password-check", "/tools/url-check", "/tools/url-check-api").authenticated()

                .requestMatchers("/tools/ssl-check", "/tools/dns-lookup", "/tools/headers-check", "/tools/hash-check", "/tools/password-breach", "/tools/domain-age", "/tools/ip-check", "/tools/vt-url", "/tools/vt-hash", "/tools/vt-ip", "/tools/vt-domain")
                    .hasAnyAuthority("ROLE_ADMIN", "ROLE_CYBER_PRO", "ROLE_FORENSICS", "ROLE_ANALYST")

                .requestMatchers("/ai/**", "/tools/**", "/report-attack/**", "/notifications/**", "/downloads/**", "/change-password")
                    .authenticated()

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
