package meditrack.controller;

import meditrack.dto.AppointmentDTO;
import meditrack.dto.StatsDTO;
import meditrack.model.Appointment;
import meditrack.service.AppointmentService;
import meditrack.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/appointments")
@CrossOrigin(origins = "http://localhost:5173") // Adjust as needed
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final EmailService emailService;

    @Autowired
    public AppointmentController(AppointmentService appointmentService, EmailService emailService) {
        this.appointmentService = appointmentService;
        this.emailService = emailService;
    }

    @PostMapping
    public ResponseEntity<AppointmentDTO> createAppointment(@RequestBody AppointmentDTO appointmentDTO) {
        AppointmentDTO createdAppointment = appointmentService.createAppointment(appointmentDTO);

        try {
            if (createdAppointment.getPatientEmail() != null &&
                    createdAppointment.getPatientName() != null &&
                    createdAppointment.getDoctorName() != null &&
                    createdAppointment.getAppointmentDateTime() != null) {

                emailService.sendAppointmentBooked(
                        createdAppointment.getPatientEmail(),
                        createdAppointment.getPatientName(),
                        createdAppointment.getDoctorName(),
                        createdAppointment.getAppointmentDateTime().toLocalDate().toString(),
                        createdAppointment.getAppointmentDateTime().toLocalTime().toString()
                );
            } else {
                System.err.println("Email not sent due to missing fields:");
                System.err.println("Email: " + createdAppointment.getPatientEmail());
                System.err.println("Name: " + createdAppointment.getPatientName());
                System.err.println("Doctor: " + createdAppointment.getDoctorName());
                System.err.println("DateTime: " + createdAppointment.getAppointmentDateTime());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return ResponseEntity.ok(createdAppointment);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AppointmentDTO> getAppointmentById(@PathVariable String id) {
        return ResponseEntity.ok(appointmentService.getAppointmentById(id));
    }

    @GetMapping
    public ResponseEntity<List<AppointmentDTO>> getAllAppointments() {
        return ResponseEntity.ok(appointmentService.getAllAppointments());
    }

    @PutMapping("/{id}")
    public ResponseEntity<AppointmentDTO> updateAppointment(
            @PathVariable String id, @RequestBody AppointmentDTO appointmentDTO) {
        AppointmentDTO updatedAppointment = appointmentService.updateAppointment(id, appointmentDTO);

        if ("CONFIRMED".equalsIgnoreCase(updatedAppointment.getStatus())) {
            emailService.sendAppointmentConfirmation(
                    updatedAppointment.getPatientEmail(),
                    updatedAppointment.getPatientName(),
                    updatedAppointment.getDoctorName(),
                    updatedAppointment.getAppointmentDateTime().toLocalDate().toString(),
                    updatedAppointment.getAppointmentDateTime().toLocalTime().toString()
            );
        }

        return ResponseEntity.ok(updatedAppointment);
    }

    @DeleteMapping("/{appointmentId}")
    public ResponseEntity<Void> deleteAppointment(@PathVariable String appointmentId) {
        AppointmentDTO appointment = appointmentService.getAppointmentById(appointmentId);
        appointmentService.deleteAppointment(appointmentId);

        emailService.sendAppointmentCancellation(
                appointment.getPatientEmail(),
                appointment.getPatientName(),
                appointment.getDoctorName(),
                appointment.getAppointmentDateTime().toLocalDate().toString(),
                appointment.getAppointmentDateTime().toLocalTime().toString()
        );

        return ResponseEntity.noContent().build();
    }




    @PostMapping("/{id}/reschedule")
    public ResponseEntity<AppointmentDTO> rescheduleAppointment(
            @PathVariable String id, @RequestParam LocalDateTime newDateTime) {
        AppointmentDTO rescheduledAppointment = appointmentService.rescheduleAppointment(id, newDateTime);

        emailService.sendAppointmentReschedule(
                rescheduledAppointment.getPatientEmail(),
                rescheduledAppointment.getPatientName(),
                rescheduledAppointment.getDoctorName(),
                rescheduledAppointment.getAppointmentDateTime().toLocalDate().toString(),
                rescheduledAppointment.getAppointmentDateTime().toLocalTime().toString()
        );

        return ResponseEntity.ok(rescheduledAppointment);
    }

    @GetMapping("/patient/{patientId}/upcoming")
    public ResponseEntity<List<AppointmentDTO>> getUpcomingAppointmentsByPatient(@PathVariable String patientId) {
        return ResponseEntity.ok(appointmentService.getUpcomingAppointmentsByPatient(patientId));
    }

    @GetMapping("/doctor/{doctorId}/upcoming")
    public ResponseEntity<List<AppointmentDTO>> getUpcomingAppointmentsByDoctor(@PathVariable String doctorId) {
        return ResponseEntity.ok(appointmentService.getUpcomingAppointmentsByDoctor(doctorId));
    }

    @GetMapping("/doctor/{doctorId}/history")
    public ResponseEntity<List<AppointmentDTO>> getAppointmentHistoryByDoctor(@PathVariable String doctorId) {
        return ResponseEntity.ok(appointmentService.getAppointmentHistoryByDoctor(doctorId));
    }

    @GetMapping("/stats")
    public ResponseEntity<StatsDTO> getAppointmentStats() {
        return ResponseEntity.ok(appointmentService.getAppointmentStats());
    }

    @PutMapping("/{appointmentId}/confirm")
    public ResponseEntity<String> confirmAppointment(@PathVariable String appointmentId) {
        Appointment appointment = appointmentService.confirmAppointment(appointmentId); // return updated appointment
        emailService.sendAppointmentConfirmation(
                appointment.getPatientEmail(),
                appointment.getPatientName(),
                appointment.getDoctorName(),
                appointment.getAppointmentDateTime().toLocalDate().toString(),
                appointment.getAppointmentDateTime().toLocalTime().toString()
        );
        return ResponseEntity.ok("Appointment confirmed successfully. Confirmation email sent.");
    }



}
