package meditrack.feign;

import meditrack.dto.ApiResponse;
import meditrack.dto.PatientDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

// ✅ Feign Client pointing to deployed Patient Service
@FeignClient(name = "patient-service", url = "https://patient-service-ntk0.onrender.com/api/patient")
public interface PatientFeign {

    @GetMapping("/{patientId}")
    ApiResponse<PatientDTO> getPatientById(@PathVariable("patientId") String patientId);
}
