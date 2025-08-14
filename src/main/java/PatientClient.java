import meditrack.dto.PatientDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "patient-service", url = "https://patient-service-ntk0.onrender.com/api/patient")
public interface PatientClient {

    @GetMapping("/{patientId}")
    PatientDTO getPatientById(@PathVariable("patientId") String patientId);

    // Example: Optional, create patient (if needed)
    @PostMapping("/")
    PatientDTO createPatient(@RequestBody PatientDTO patientDTO);
}
