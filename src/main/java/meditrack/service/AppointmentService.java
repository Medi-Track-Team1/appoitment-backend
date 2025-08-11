package meditrack.service;

import meditrack.dto.AppointmentDTO;
import meditrack.dto.StatsDTO;
import meditrack.model.Appointment;

import java.time.LocalDateTime;
import java.util.List;


public interface AppointmentService {
    AppointmentDTO createAppointment(AppointmentDTO appointmentDTO);
    AppointmentDTO getAppointmentById(String id);
    List<AppointmentDTO> getAllAppointments();
    AppointmentDTO updateAppointment(String id, AppointmentDTO appointmentDTO);
    AppointmentDTO markCompleted(String appointmentId);
    AppointmentDTO rescheduleAppointment(String id, LocalDateTime newDateTime);
    List<AppointmentDTO> getUpcomingAppointmentsByPatient(String patientId);
    List<AppointmentDTO> getUpcomingAppointmentsByDoctor(String doctorId);
    List<AppointmentDTO> getAppointmentHistoryByDoctor(String doctorId);
    StatsDTO getAppointmentStats();
    //    List<AppointmentDTO> getEmergencyAppointments();
    void cancelAppointment(String appointmentId);

    //    void deleteAppointment(String appointmentId);
    Appointment confirmAppointment(String appointmentId);

    List<AppointmentDTO> searchAppointments(String status, String startDate, String endDate);
    List<Appointment> findByPatientIdAndStatus(String patientId, String status);
    List<AppointmentDTO> getCompletedAppointmentsByDoctor(String doctorId);
    List<AppointmentDTO> getCompletedAppointmentsByPatient(String patientId);
    List<AppointmentDTO> getAppointmentHistoryByPatient(String patientId);

    boolean deleteAppointmentById(String appointmentId);
}
