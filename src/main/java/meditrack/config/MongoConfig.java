package meditrack.config;

import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoRepositories(basePackages = "meditrack.repository")
@EnableMongoAuditing
public class MongoConfig {
    @Bean
    public ModelMapper modelMapper() {
        return new ModelMapper();
    }
}
