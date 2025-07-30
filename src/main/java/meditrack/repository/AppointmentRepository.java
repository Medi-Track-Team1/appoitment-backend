package meditrack.repository;


import meditrack.model.Appointment;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AppointmentRepository extends MongoRepository<Appointment, String> {
    List<Appointment> findByPatientId(String patientId); // This won't work with @DBRef
    List<Appointment> findByDoctorId(String doctorId); // This won't work with @DBRef

    // Corrected queries:
    @Query("{ 'patient.$id' : ?0 }")
    List<Appointment> findByPatientId(ObjectId patientId);

    @Query("{ 'doctor.$id' : ?0 }")
    List<Appointment> findByDoctorId(ObjectId doctorId);

    @Query("{ 'appointmentDateTime' : { $gte: ?0 }, 'status' : { $ne: 'COMPLETED' } }")
    List<Appointment> findUpcomingAppointments(LocalDateTime now);

    @Query("{ 'doctorId' : ?0, 'status' : 'COMPLETED' }")
    List<Appointment> findCompletedAppointmentsByDoctor(String doctorId);

    @Query("{ 'appointmentDateTime' : { $gte: ?0, $lte: ?1 }, 'doctorId' : ?2 }")
    List<Appointment> findByDateTimeBetweenAndDoctorId(LocalDateTime start, LocalDateTime end, String doctorId);

    long countByIsEmergency(boolean isEmergency);
    long countByStatus(String status);

    List<Appointment> findByPatientIdAndAppointmentDateTimeAfter(String patientId, LocalDateTime date);
    List<Appointment> findByDoctorIdAndAppointmentDateTimeAfter(String doctorId, LocalDateTime date);
    List<Appointment> findByDoctorIdAndStatus(String doctorId, String status);
    List<Appointment> findByIsEmergency(boolean isEmergency);
}
