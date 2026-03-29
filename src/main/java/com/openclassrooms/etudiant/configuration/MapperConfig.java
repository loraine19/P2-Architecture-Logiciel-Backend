package com.openclassrooms.etudiant.configuration;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class to ensure MapStruct mappers are properly scanned and
 * registered as Spring beans
 */
@Configuration
@ComponentScan(basePackages = {
        "com.openclassrooms.etudiant.mapper"
})
public class MapperConfig {
    // Configuration class to explicitly scan mapper package
    // This ensures MapStruct generated implementations are detected as Spring
    // components
}