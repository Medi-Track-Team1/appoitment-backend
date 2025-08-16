package meditrack.feign;

import meditrack.dto.DoctorDTO;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "doctor-service",
        url = "https://doctorpanel-backend.onrender.com/api",
        primary = true,
        fallback = DoctorServiceClientFallback.class

)
@Qualifier("doctorServiceClient") //
public interface DoctorServiceClient {

    @GetMapping("/doctor/{doctorId}")
    ResponseEntity<DoctorDTO> getDoctorById(@PathVariable("doctorId") String doctorId);
}