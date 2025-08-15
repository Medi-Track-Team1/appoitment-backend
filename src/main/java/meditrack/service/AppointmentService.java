package meditrack.service;

import meditrack.dto.AppointmentDTO;
import meditrack.dto.StatsDTO;
import meditrack.model.Appointment;

import java.time.LocalDateTime;
import java.util.List;

public interface AppointmentService {

    // Appointment CRUD Operations
    AppointmentDTO createAppointment(AppointmentDTO appointmentDTO);
    AppointmentDTO getAppointmentById(String appointmentId);
    List<AppointmentDTO> getAllAppointments();
    AppointmentDTO updateAppointment(String appointmentId, AppointmentDTO appointmentDTO);
    boolean deleteAppointmentById(String appointmentId);

    // Appointment Status Management
    Appointment confirmAppointment(String appointmentId);
    AppointmentDTO markCompleted(String appointmentId);
    void cancelAppointment(String appointmentId, String reason);
    AppointmentDTO rescheduleAppointment(String appointmentId, LocalDateTime newDateTime);
    AppointmentDTO revisitAppointment(String appointmentId, LocalDateTime newDateTime, String reason);

    // Appointment Retrieval
    List<AppointmentDTO> getUpcomingAppointmentsByPatient(String patientId);
    List<AppointmentDTO> getUpcomingAppointmentsByDoctor(String doctorId);
    List<AppointmentDTO> getAppointmentHistoryByPatient(String patientId);
    List<AppointmentDTO> getAppointmentHistoryByDoctor(String doctorId);
    List<AppointmentDTO> getCompletedAppointmentsByPatient(String patientId);
    List<AppointmentDTO> getCompletedAppointmentsByDoctor(String doctorId);

    // Search and Filter
    List<AppointmentDTO> searchAppointments(String status, String startDate, String endDate);
    List<Appointment> findByPatientIdAndStatus(String patientId, String status);

    // Statistics
    StatsDTO getAppointmentStats();
}