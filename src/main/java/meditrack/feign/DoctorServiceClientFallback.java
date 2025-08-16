package meditrack.feign;

import meditrack.dto.DoctorDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class DoctorServiceClientFallback implements DoctorServiceClient {

    @Override
    public ResponseEntity<DoctorDTO> getDoctorById(String doctorId) {
        // Return null or default doctor when service is unavailable
        return ResponseEntity.ok().body(null);
    }
}