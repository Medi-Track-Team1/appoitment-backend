package meditrack.repository;

import meditrack.model.Doctor;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface DoctorRepository extends MongoRepository<Doctor, String> {
    Optional<Doctor> findByDoctorId(String doctorId);
    boolean existsByDoctorId(String doctorId);
// Add this line
}
