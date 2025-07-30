package meditrack.repository;


import meditrack.model.Doctor;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface DoctorRepository extends MongoRepository<Doctor, String> {
    Doctor findByEmail(String email);
    List<Doctor> findByDepartment(String department);
}
