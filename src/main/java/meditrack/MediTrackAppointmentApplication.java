package meditrack;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.modelmapper.ModelMapper;
import org.springframework.web.bind.annotation.CrossOrigin;

@SpringBootApplication
@EnableFeignClients(basePackages = "meditrack.feign") // 👈 enable Feign here
@CrossOrigin(origins = "*", maxAge = 3600)
public class MediTrackAppointmentApplication {

    public static void main(String[] args) {
        SpringApplication.run(MediTrackAppointmentApplication.class, args);
    }


}
