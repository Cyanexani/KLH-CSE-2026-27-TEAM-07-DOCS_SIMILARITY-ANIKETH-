package com.docs_similarity.config;

import jakarta.servlet.MultipartConfigElement;
import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.util.unit.DataSize;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Allows the local React dev server (Vite, default port 5173) to call
 * the Spring Boot backend (default port 8080) during development.
 * No production URLs are hardcoded — update allowedOrigins for deployment.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:5173")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }

    /** Largest accepted individual file. Keep in sync with application.properties and the frontend. */
    public static final DataSize MAX_FILE_SIZE = DataSize.ofMegabytes(25);
    /** Largest accepted multipart request (all files together). */
    public static final DataSize MAX_REQUEST_SIZE = DataSize.ofMegabytes(100);

    /**
     * Programmatic multipart limits. Spring Boot only applies limits from a
     * MultipartConfigElement bean (a bare MultipartConfigFactory bean is
     * ignored), and when this bean exists it takes precedence over the
     * spring.servlet.multipart.* properties, so both are set to the same values.
     */
    @Bean
    public MultipartConfigElement multipartConfigElement() {
        MultipartConfigFactory factory = new MultipartConfigFactory();
        factory.setMaxFileSize(MAX_FILE_SIZE);
        factory.setMaxRequestSize(MAX_REQUEST_SIZE);
        return factory.createMultipartConfig();
    }
}
