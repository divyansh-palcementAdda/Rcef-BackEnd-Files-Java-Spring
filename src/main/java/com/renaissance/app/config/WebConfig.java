package com.renaissance.app.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(
                        "http://localhost:4200",   // Angular local dev
                        "https://rcef-frontend.com", // Example deployed frontend (replace with real URL)
                        "http://localhost:8080",
                        "http://192.168.0.182:4200"
                )
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Authorization") // expose JWT header if needed
                .allowCredentials(true)          // allow cookies/headers with credentials
                .maxAge(3600);                   // cache preflight response for 1 hour
    }
}
