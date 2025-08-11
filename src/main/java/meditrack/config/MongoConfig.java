package meditrack.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoRepositories(basePackages = "meditrack.repository")
@EnableMongoAuditing
public class MongoConfig {
    // Removed modelMapper bean to avoid conflict
}