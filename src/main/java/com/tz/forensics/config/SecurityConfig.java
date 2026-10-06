package com.tz.forensics.config;

import com.tz.forensics.service.CustomOAuth2UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private final CustomOAuth2UserService oauth2UserService;
    private final OAuth2LoginSuccessHandler oauth2Handler;
    private final ClientRegistrationRepository clientRegistrationRepository;
    private final LoginAuthenticationFailureHandler loginFailureHandler;

    public SecurityConfig(CustomOAuth2UserService oauth2UserService,
                          OAuth2LoginSuccessHandler oauth2Handler,
                          ClientRegistrationRepository clientRegistrationRepository,
                          LoginAuthenticationFailureHandler loginFailureHandler) {
        this.oauth2UserService = oauth2UserService;
        this.oauth2Handler = oauth2Handler;
        this.clientRegistrationRepository = clientRegistrationRepository;
        this.loginFailureHandler = loginFailureHandler;
    }

    @Bean
    public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    public DefaultOAuth2AuthorizationRequestResolver oauth2AuthorizationRequestResolver() {
        DefaultOAuth2AuthorizationRequestResolver resolver =
                new DefaultOAuth2AuthorizationRequestResolver(
                        clientRegistrationRepository, "/oauth2/authorization");
        resolver.setAuthorizationRequestCustomizer(builder ->
                builder.additionalParameters(java.util.Map.of("prompt", "select_account")));
        return resolver;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/", "/features", "/about", "/contact",
                    "/login", "/register", "/access-denied", "/error", "/health",
                    "/forgot-password", "/forgot-password/sms", "/reset-password", "/set-password", "/verify-email",
                    "/whistleblower", "/whistleblower/report", "/whistleblower/submit",
                    "/whistleblower/success", "/whistleblower/track", "/whistleblower/track/**",
                    "/css/**", "/js/**", "/images/**", "/static/**",
                    "/favicon.ico", "/webjars/**",
                    "/announcements", "/announcements/**",
                    "/oauth2/**", "/login/oauth2/**",
                    "/oauth2/verify", "/oauth2/verify/**",
                    "/lang"
                ).permitAll()

                .requestMatchers("/admin/audit", "/admin/audit/**")
                    .hasAnyAuthority("ROLE_ADMIN", "ROLE_CYBER_PRO", "ROLE_FORENSICS")

                .requestMatchers("/admin/**")
                    .hasAuthority("ROLE_ADMIN")

                .requestMatchers("/whistleblower/admin/**")
                    .hasAnyAuthority("ROLE_ADMIN", "ROLE_CYBER_PRO", "ROLE_FORENSICS", "ROLE_ANALYST")

                .requestMatchers("/compliance", "/compliance/**")
                    .hasAnyAuthority("ROLE_ADMIN", "ROLE_CYBER_PRO", "ROLE_FORENSICS")

                .requestMatchers("/report-attack/admin", "/report-attack/admin/**")
                    .hasAnyAuthority("ROLE_ADMIN", "ROLE_CYBER_PRO", "ROLE_FORENSICS", "ROLE_ANALYST")

                .requestMatchers("/incidents/my-tasks", "/incidents/my-tasks/**")
                    .hasAnyAuthority("ROLE_ADMIN", "ROLE_CYBER_PRO", "ROLE_FORENSICS", "ROLE_ANALYST")

                .requestMatchers("/incidents", "/incidents/**")
                    .authenticated()

                .requestMatchers("/cases", "/cases/**")
                    .hasAnyAuthority("ROLE_ADMIN", "ROLE_CYBER_PRO", "ROLE_FORENSICS", "ROLE_ANALYST")

                .requestMatchers("/case-tasks", "/case-tasks/**")
                    .hasAnyAuthority("ROLE_ADMIN", "ROLE_CYBER_PRO", "ROLE_FORENSICS", "ROLE_ANALYST")

                .requestMatchers("/case-communications", "/case-communications/**")
                    .hasAnyAuthority("ROLE_ADMIN", "ROLE_CYBER_PRO", "ROLE_FORENSICS", "ROLE_ANALYST")

                .requestMatchers("/evidence/**")
                    .authenticated()

                .requestMatchers("/threat-map", "/threat-map/**")
                    .hasAnyAuthority("ROLE_ADMIN", "ROLE_CYBER_PRO", "ROLE_FORENSICS", "ROLE_ANALYST")

                // Basic security tools are available to every authenticated role.
                .requestMatchers("/tools/security-center", "/tools/password-check", "/tools/url-check",
                                 "/tools/url-check-api", "/tools/hash-check")
                    .authenticated()

                // Network/reputation integrations remain staff-only.
                .requestMatchers("/tools/ssl-check", "/tools/dns-lookup", "/tools/headers-check",
                                 "/tools/password-breach", "/tools/domain-age", "/tools/ip-check",
                                 "/tools/vt-url", "/tools/vt-hash", "/tools/vt-ip", "/tools/vt-domain")
                    .hasAnyAuthority("ROLE_ADMIN", "ROLE_CYBER_PRO", "ROLE_FORENSICS", "ROLE_ANALYST")

                .requestMatchers("/ai/debug", "/ai/test-models")
                    .hasAuthority("ROLE_ADMIN")

                .requestMatchers("/ai/threat-detection", "/ai/analyze-incident", "/ai/analysis-api", "/ai/prediction-api")
                    .hasAnyAuthority("ROLE_ADMIN", "ROLE_CYBER_PRO", "ROLE_FORENSICS", "ROLE_ANALYST")

                .requestMatchers("/admin/check")
                    .hasAuthority("ROLE_ADMIN")

                .requestMatchers("/ai/**", "/report-attack/**", "/notifications/**", "/downloads/**", "/change-password")
                    .authenticated()

                .requestMatchers("/tools/**")
                    .hasAnyAuthority("ROLE_ADMIN", "ROLE_CYBER_PRO", "ROLE_FORENSICS", "ROLE_ANALYST")

                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/dashboard", true)
                .failureHandler(loginFailureHandler)
                .permitAll()
            )
            .oauth2Login(oauth -> oauth
                .loginPage("/login")
                .authorizationEndpoint(a -> a.authorizationRequestResolver(oauth2AuthorizationRequestResolver()))
                .userInfoEndpoint(u -> u.userService(oauth2UserService))
                .successHandler(oauth2Handler)
                .failureUrl("/login?error")
            )
            .exceptionHandling(exceptions -> exceptions.accessDeniedPage("/access-denied"))
            .sessionManagement(session -> session.sessionFixation(fixation -> fixation.migrateSession()))
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            );
        return http.build();
    }
}