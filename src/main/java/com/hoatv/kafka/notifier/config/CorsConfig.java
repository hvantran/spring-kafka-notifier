package com.hoatv.kafka.notifier.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedMethods("*")
                        .allowedOrigins("http://localhost:3000", "http://actmanagerui.local:3000", "http://localhost:3001", "http://localhost:6084", "http://actmanagerui.local:6084", "http://localhost:8070")
                        .allowedHeaders("*");
            }
        };
    }
}
