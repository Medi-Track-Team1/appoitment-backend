// Create this file: src/main/java/meditrack/config/CorsConfig.java

package meditrack.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/")  // Allow all endpoints
                .allowedOriginPatterns("*")  // Allow all origins (development only)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "HEAD")
                .allowedHeaders("*")
                .exposedHeaders("*")
                .allowCredentials(false)  // Set to false when using allowedOriginPatterns("*")
                .maxAge(3600);
}
}
