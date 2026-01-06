package com.jeremy.courses;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private static final String UPLOAD_DIR = "uploads";

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String uploadLocation = "file:" + UPLOAD_DIR + "/";
        registry.addResourceHandler("/files/**")
                .addResourceLocations(uploadLocation);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(
                        "https://mind-leaf.netlify.app", // Deployed frontend
                        "http://localhost:3000", // Local dev (Vite)
                        "http://127.0.0.1:3000" // Local dev (loopback)
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                // Allow all headers so preflight doesn't fail when browser adds extra ones
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
