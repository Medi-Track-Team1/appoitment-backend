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

    AppointmentDTO rescheduleAppointment(String id, LocalDateTime newDateTime);
    List<AppointmentDTO> getUpcomingAppointmentsByPatient(String patientId);
    List<AppointmentDTO> getUpcomingAppointmentsByDoctor(String doctorId);
    List<AppointmentDTO> getAppointmentHistoryByDoctor(String doctorId);
    StatsDTO getAppointmentStats();
//    List<AppointmentDTO> getEmergencyAppointments();

    void deleteAppointment(String appointmentId);
    Appointment confirmAppointment(String appointmentId);

}