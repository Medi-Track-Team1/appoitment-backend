package meditrack.feign;

import meditrack.dto.ApiResponse;
import meditrack.dto.DoctorDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "doctor-service", url = "https://doctorpanel-backend.onrender.com/api")
public interface DoctorServiceClient {

    @GetMapping("/doctor/{doctorId}")
    DoctorDTO getDoctorById(@PathVariable("doctorId") String doctorId);

}