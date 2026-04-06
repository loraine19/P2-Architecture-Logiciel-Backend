package com.openclassrooms.etudiant.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.FileSystemResource;

/**
 * Simple app config to load .env file
 * Loads environment variables like DB credentials, JWT secrets, etc.
 */
@Configuration
public class AppConfig {

    /**
     * Load .env file for environment variables
     * Handles missing file gracefully for Docker deployments
     */
    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();
        configurer.setLocation(new FileSystemResource(".env"));
        // docker deployments use env vars directly, .env may not exist
        configurer.setIgnoreResourceNotFound(true);
        return configurer;
    }
}
