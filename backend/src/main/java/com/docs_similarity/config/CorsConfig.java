package com.docs_similarity.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Lets the React development server (http://localhost:5173 by default) call the API on
 * http://localhost:8080. Allowed origins come from {@code docs-similarity.cors.allowed-origins}.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private final AppProperties properties;

    public CorsConfig(AppProperties properties) {
        this.properties = properties;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(properties.cors().allowedOrigins().toArray(String[]::new))
                .allowedMethods("GET", "POST", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
    }
}
