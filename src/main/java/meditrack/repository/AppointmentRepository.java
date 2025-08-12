
package meditrack.repository;

import meditrack.model.Appointment;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface AppointmentRepository extends MongoRepository<Appointment, String> {

    Optional<Appointment> findByAppointmentId(String appointmentId);

    @Query("{ 'patient.$id' : ?0 }")
    List<Appointment> findByPatientId(ObjectId patientId);

    @Query("{ 'doctor.$id' : ?0 }")
    List<Appointment> findByDoctorId(ObjectId doctorId);

    @Query("{ 'patient.patientId' : ?0 }")
    List<Appointment> findByPatient_PatientId(String patientId);

    @Query("{ 'doctor.doctorId' : ?0 }")
    List<Appointment> findByDoctor_DoctorId(String doctorId);

    @Query("{ 'appointmentDateTime' : { $gte: ?0 }, 'status' : { $ne: 'COMPLETED' } }")
    List<Appointment> findUpcomingAppointments(LocalDateTime now);

    @Query("{ 'doctor.doctorId' : ?0, 'status' : 'COMPLETED' }")
    List<Appointment> findCompletedAppointmentsByDoctor(String doctorId);

    @Query("{ 'appointmentDateTime' : { $gte: ?0, $lte: ?1 }, 'doctor.doctorId' : ?2 }")
    List<Appointment> findByDateTimeBetweenAndDoctorId(LocalDateTime start, LocalDateTime end, String doctorId);

    long countByStatus(String status);

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


    @Query("SELECT a FROM Appointment a WHERE a.doctorId = :doctorId AND FUNCTION('DATE', a.appointmentDateTime) = :date")
    List<Appointment> findByDoctorIdAndDate(@Param("doctorId") String doctorId, @Param("date") LocalDate date);

}

