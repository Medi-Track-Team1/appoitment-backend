package meditrack.repository;


import meditrack.model.Appointment;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import java.time.LocalDateTime;
import java.util.List;

public interface AppointmentRepository extends MongoRepository<Appointment, String> {
    List<Appointment> findByPatientId(String patientId);
    List<Appointment> findByDoctorId(String doctorId);

    @Query("{ 'appointmentDateTime' : { $gte: ?0 }, 'status' : { $ne: 'COMPLETED' } }")
    List<Appointment> findUpcomingAppointments(LocalDateTime now);

    @Query("{ 'doctorId' : ?0, 'status' : 'COMPLETED' }")
    List<Appointment> findCompletedAppointmentsByDoctor(String doctorId);

    @Query("{ 'appointmentDateTime' : { $gte: ?0, $lte: ?1 }, 'doctorId' : ?2 }")
    List<Appointment> findByDateTimeBetweenAndDoctorId(LocalDateTime start, LocalDateTime end, String doctorId);

    long countByIsEmergency(boolean isEmergency);
    long countByStatus(String status);
}
