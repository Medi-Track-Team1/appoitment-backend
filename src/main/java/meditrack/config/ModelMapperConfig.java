package meditrack.config;

import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ModelMapperConfig {

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();

        // Configure ModelMapper for stricter mapping
        modelMapper.getConfiguration()
                .setMatchingStrategy(MatchingStrategies.STRICT) // Ensures exact property name matching
                .setFieldMatchingEnabled(true)
                .setSkipNullEnabled(true) // Skips null values during mapping
                .setFieldAccessLevel(org.modelmapper.config.Configuration.AccessLevel.PRIVATE);

        return modelMapper;
    }
}