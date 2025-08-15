package meditrack.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoRepositories(basePackages = "meditrack.repository")
@EnableMongoAuditing
public class MongoConfig {
    /*
     * This configuration class enables MongoDB-specific features:
     * 1. @EnableMongoRepositories - scans for MongoDB repositories in the specified package
     * 2. @EnableMongoAuditing - enables auditing features like @CreatedDate, @LastModifiedDate
     *
     * The modelMapper bean was removed to:
     * - Avoid potential circular dependencies
     * - Follow single responsibility principle
     * - Allow ModelMapper to be configured separately if needed
     */
}