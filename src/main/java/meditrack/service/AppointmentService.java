package meditrack.service;


import meditrack.dto.AppointmentDTO;
import meditrack.dto.StatsDTO;
import java.time.LocalDateTime;
import java.util.List;

public interface AppointmentService {
    AppointmentDTO createAppointment(AppointmentDTO appointmentDTO);
    AppointmentDTO getAppointmentById(String id);
    List<AppointmentDTO> getAllAppointments();
    AppointmentDTO updateAppointment(String id, AppointmentDTO appointmentDTO);
    void deleteAppointment(String id);

    List<AppointmentDTO> getUpcomingAppointmentsByPatient(String patientId);
    List<AppointmentDTO> getUpcomingAppointmentsByDoctor(String doctorId);
    List<AppointmentDTO> getAppointmentHistoryByDoctor(String doctorId);

    AppointmentDTO rescheduleAppointment(String id, LocalDateTime newDateTime);

    StatsDTO getAppointmentStats();
    List<AppointmentDTO> getEmergencyAppointments();
}
