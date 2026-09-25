package com.tz.forensics.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

import java.time.Duration;
import java.util.Locale;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Bean
    public LocaleResolver localeResolver() {
        CookieLocaleResolver resolver = new CookieLocaleResolver("lang") {
            @Override
            public Locale resolveLocale(HttpServletRequest request) {
                // 1. Session locale (kama user amechagua)
                Locale sessionLocale = (Locale) request.getSession()
                        .getAttribute("SPRING_SECURITY_LAST_LOCALE");
                if (sessionLocale != null) return sessionLocale;

                // 2. Cookie locale
                Locale cookieLocale = super.resolveLocale(request);
                if (cookieLocale != null && cookieLocale.getLanguage() != null
                        && !cookieLocale.getLanguage().isEmpty()) {
                    return cookieLocale;
                }

                // 3. Auto-detect browser
                String acceptLanguage = request.getHeader("Accept-Language");
                if (acceptLanguage != null) {
                    String lang = acceptLanguage.toLowerCase();
                    if (lang.startsWith("sw") || lang.contains("sw-") || lang.contains(",sw"))
                        return new Locale("sw");
                    if (lang.startsWith("fr") || lang.contains("fr-") || lang.contains(",fr"))
                        return Locale.FRENCH;
                    if (lang.startsWith("ar") || lang.contains("ar-") || lang.contains(",ar"))
                        return new Locale("ar");
                }
                return Locale.ENGLISH;
            }
        };
        resolver.setDefaultLocale(Locale.ENGLISH);
        resolver.setCookieMaxAge(Duration.ofDays(365));
        return resolver;
    }

    @Bean
    public LocaleChangeInterceptor localeChangeInterceptor() {
        LocaleChangeInterceptor interceptor = new LocaleChangeInterceptor();
        interceptor.setParamName("lang");
        return interceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(localeChangeInterceptor());
    }
}
