package meditrack;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.modelmapper.ModelMapper;
@EnableFeignClients
@EnableDiscoveryClient
@SpringBootApplication
public class MediTrackAppointmentApplication {

    public static void main(String[] args) {
        SpringApplication.run(MediTrackAppointmentApplication.class, args);
    }

}