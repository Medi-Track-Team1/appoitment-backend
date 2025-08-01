package meditrack.repository;

import meditrack.model.Patient;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface PatientRepository extends MongoRepository<Patient, String> {
    Optional<Patient> findByPatientId(String patientId);
    boolean existsByPatientId(String patientId);// Add this line
}
