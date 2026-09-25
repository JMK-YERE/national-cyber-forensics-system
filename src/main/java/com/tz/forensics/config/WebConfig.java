package com.tz.forensics.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

import java.util.Locale;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Bean
    public LocaleResolver localeResolver() {
        SessionLocaleResolver resolver = new SessionLocaleResolver() {
            @Override
            public Locale resolveLocale(HttpServletRequest request) {
                // 1. Angalia kama user amechagua lugha manually kwenye session
                Locale sessionLocale = (Locale) request.getSession()
                        .getAttribute(SessionLocaleResolver.LOCALE_SESSION_ATTRIBUTE_NAME);
                if (sessionLocale != null) return sessionLocale;

                // 2. Auto-detect kutoka browser Accept-Language
                String acceptLanguage = request.getHeader("Accept-Language");
                if (acceptLanguage != null) {
                    String lang = acceptLanguage.toLowerCase();
                    if (lang.startsWith("sw") || lang.contains("sw-") || lang.contains(",sw")) {
                        return new Locale("sw");
                    }
                    if (lang.startsWith("fr") || lang.contains("fr-") || lang.contains(",fr")) {
                        return Locale.FRENCH;
                    }
                    if (lang.startsWith("ar") || lang.contains("ar-") || lang.contains(",ar")) {
                        return new Locale("ar");
                    }
                }
                return Locale.ENGLISH;
            }
        };
        resolver.setDefaultLocale(Locale.ENGLISH);
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
