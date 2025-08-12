




package meditrack.repository;

import meditrack.model.Appointment;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface AppointmentRepository extends MongoRepository<Appointment, String> {

    // For finding by the custom appointment ID
    Optional<Appointment> findByAppointmentId(String appointmentId);

    // ✅ Custom queries using @DBRef objectId reference
    @Query("{ 'patient.$id' : ?0 }")
    List<Appointment> findByPatientId(ObjectId patientId);

    @Query("{ 'doctor.$id' : ?0 }")
    List<Appointment> findByDoctorId(ObjectId doctorId);

    // ✅ Query by custom patient/doctor IDs inside @DBRef objects
    @Query("{ 'patient.patientId' : ?0 }")
    List<Appointment> findByPatient_PatientId(String patientId);

    @Query("{ 'doctor.doctorId' : ?0 }")
    List<Appointment> findByDoctor_DoctorId(String doctorId);

    // Upcoming appointments (not completed)
    @Query("{ 'appointmentDateTime' : { $gte: ?0 }, 'status' : { $ne: 'COMPLETED' } }")
    List<Appointment> findUpcomingAppointments(LocalDateTime now);

    // Completed appointments for a doctor
    @Query("{ 'doctor.doctorId' : ?0, 'status' : 'COMPLETED' }")
    List<Appointment> findCompletedAppointmentsByDoctor(String doctorId);

    // Appointments in a time range for a specific doctor
    @Query("{ 'appointmentDateTime' : { $gte: ?0, $lte: ?1 }, 'doctor.doctorId' : ?2 }")
    List<Appointment> findByDateTimeBetweenAndDoctorId(LocalDateTime start, LocalDateTime end, String doctorId);

    // Stats
//    long countByIsEmergency(boolean isEmergency);
    long countByStatus(String status);

    // Filtered queries (if you store patientId/doctorId as strings in future)
    // Safe to remove if not using raw string IDs anymore
    List<Appointment> findByPatientIdAndAppointmentDateTimeAfter(String patientId, LocalDateTime date);
    List<Appointment> findByDoctorIdAndAppointmentDateTimeAfter(String doctorId, LocalDateTime date);


    List<Appointment> findByDoctorIdAndStatusIgnoreCase(String doctorId, String status);

    List<Appointment> findByDoctorIdAndStatus(String doctorId, String completed);
    List<Appointment> findByPatientIdAndStatus(String doctorId, String completed);
//    List<Appointment> findByIsEmergency(boolean isEmergency);

    boolean existsByAppointmentId(String appointmentId);

    void deleteByAppointmentId(String appointmentId);


    // Check overlapping doctor appointment:

    // Check overlapping appointment for patient & doctor:
    @Query("{ 'patient.patientId': ?0, 'doctor.doctorId': ?1, " +
            " 'appointmentDateTime': { $lt: ?3 }, " +
            " 'appointmentEndDateTime': { $gt: ?2 } }")
    Boolean existsOverlappingAppointment(String patientId, String doctorId, LocalDateTime requestedStart, LocalDateTime requestedEnd);

    boolean existsByPatientIdAndDoctorIdAndAppointmentDateTime(String patientId, String doctorId, LocalDateTime appointmentDateTime);
}