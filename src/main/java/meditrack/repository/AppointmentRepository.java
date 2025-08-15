package meditrack.repository;

import meditrack.model.Appointment;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AppointmentRepository extends MongoRepository<Appointment, String> {

    Optional<Appointment> findByAppointmentId(String appointmentId);

    @Query("{ 'patientId' : ?0 }")
    List<Appointment> findByPatientId(String patientId);

    @Query("{ 'doctorId' : ?0 }")
    List<Appointment> findByDoctorId(String doctorId);

    @Query("{ 'appointmentDateTime' : { $gte: ?0 }, 'status' : { $ne: 'COMPLETED' } }")
    List<Appointment> findUpcomingAppointments(LocalDateTime now);

    @Query("{ 'doctorId' : ?0, 'status' : 'COMPLETED' }")
    List<Appointment> findCompletedAppointmentsByDoctor(String doctorId);

    @Query("{ 'appointmentDateTime' : { $gte: ?0, $lte: ?1 }, 'doctorId' : ?2 }")
    List<Appointment> findByDateTimeBetweenAndDoctorId(LocalDateTime start, LocalDateTime end, String doctorId);

    @Query("{ 'doctorId' : ?0, 'appointmentDateTime' : { $gte: ?1, $lt: ?2 } }")
    List<Appointment> findByDoctorIdAndDate(String doctorId, LocalDateTime startOfDay, LocalDateTime endOfDay);

    @Query("{ 'status' : ?0 }")
    long countByStatus(String status);

    @Query("{ 'patientId' : ?0, 'appointmentDateTime' : { $gt: ?1 } }")
    List<Appointment> findByPatientIdAndAppointmentDateTimeAfter(String patientId, LocalDateTime date);

    @Query("{ 'doctorId' : ?0, 'appointmentDateTime' : { $gt: ?1 } }")
    List<Appointment> findByDoctorIdAndAppointmentDateTimeAfter(String doctorId, LocalDateTime date);

    @Query("{ 'doctorId' : ?0, 'status' : ?1 }")
    List<Appointment> findByDoctorIdAndStatus(String doctorId, String status);

    @Query("{ 'patientId' : ?0, 'status' : ?1 }")
    List<Appointment> findByPatientIdAndStatus(String patientId, String status);

    boolean existsByAppointmentId(String appointmentId);

    void deleteByAppointmentId(String appointmentId);

    @Query("{ 'patientId': ?0, 'doctorId': ?1, " +
            "'appointmentDateTime': { $lt: ?3 }, " +
            "$expr: { $gt: [{ $add: ['$appointmentDateTime', { $multiply: ['$duration', 60000] }] }, ?2] } }")
    Boolean existsOverlappingAppointment(String patientId, String doctorId,
                                         LocalDateTime requestedStart, LocalDateTime requestedEnd);

    boolean existsByPatientIdAndDoctorIdAndAppointmentDateTime(String patientId,
                                                               String doctorId,
                                                               LocalDateTime appointmentDateTime);

    // ✅ Added methods for searchAppointments()
    List<Appointment> findByStatusAndAppointmentDateTimeBetween(String status, LocalDateTime start, LocalDateTime end);

    List<Appointment> findByAppointmentDateTimeBetween(LocalDateTime start, LocalDateTime end);
}
