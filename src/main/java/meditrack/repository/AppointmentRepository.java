//package meditrack.repository;
//
//
//import meditrack.model.Appointment;
//import org.bson.types.ObjectId;
//import org.springframework.data.mongodb.repository.MongoRepository;
//import org.springframework.data.mongodb.repository.Query;
//import org.springframework.stereotype.Repository;
//
//import java.time.LocalDateTime;
//import java.util.List;
//import java.util.Optional;
//
//@Repository
//public interface AppointmentRepository extends MongoRepository<Appointment, String> {
//    Optional<Appointment> findByAppointmentId(String appointmentId);
//    void deleteByAppointmentId(String appointmentId);
//
//    List<Appointment> findByPatientId(String patientId); // This won't work with @DBRef
//    List<Appointment> findByDoctorId(String doctorId); // This won't work with @DBRef
//
//    // Corrected queries:
//    @Query("{ 'patient.$id' : ?0 }")
//    List<Appointment> findByPatientId(ObjectId patientId);
//
//    @Query("{ 'doctor.$id' : ?0 }")
//    List<Appointment> findByDoctorId(ObjectId doctorId);
//
//    @Query("{ 'appointmentDateTime' : { $gte: ?0 }, 'status' : { $ne: 'COMPLETED' } }")
//    List<Appointment> findUpcomingAppointments(LocalDateTime now);
//
//    @Query("{ 'doctorId' : ?0, 'status' : 'COMPLETED' }")
//    List<Appointment> findCompletedAppointmentsByDoctor(String doctorId);
//
//    @Query("{ 'appointmentDateTime' : { $gte: ?0, $lte: ?1 }, 'doctorId' : ?2 }")
//    List<Appointment> findByDateTimeBetweenAndDoctorId(LocalDateTime start, LocalDateTime end, String doctorId);
//
//    long countByIsEmergency(boolean isEmergency);
//    long countByStatus(String status);
//
//    List<Appointment> findByPatientIdAndAppointmentDateTimeAfter(String patientId, LocalDateTime date);
//    List<Appointment> findByDoctorIdAndAppointmentDateTimeAfter(String doctorId, LocalDateTime date);
//    List<Appointment> findByDoctorIdAndStatus(String doctorId, String status);
//    List<Appointment> findByIsEmergency(boolean isEmergency);
//}




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
    void deleteByAppointmentId(String appointmentId);
    boolean existsByAppointmentId(String appointmentId); // ✅ Needed for random ID generation

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
//    List<Appointment> findByIsEmergency(boolean isEmergency);
}
