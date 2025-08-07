package meditrack.controller;

import meditrack.dto.AppointmentDTO;
import meditrack.dto.StatsDTO;
import meditrack.model.Appointment;
import meditrack.service.AppointmentService;
import meditrack.service.EmailService;
import meditrack.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/appointments")
@CrossOrigin(
        origins = {"http://localhost:5174", "http://localhost:3000"}
)
public class AppointmentController {

    private static final Logger logger = LoggerFactory.getLogger(AppointmentController.class);
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
            }
        } catch (Exception e) {
            logger.error("Error sending appointment booking email: {}", e.getMessage());
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
            try {
                emailService.sendAppointmentConfirmation(
                        updatedAppointment.getPatientEmail(),
                        updatedAppointment.getPatientName(),
                        updatedAppointment.getDoctorName(),
                        updatedAppointment.getAppointmentDateTime().toLocalDate().toString(),
                        updatedAppointment.getAppointmentDateTime().toLocalTime().toString()
                );
            } catch (Exception e) {
                logger.error("Error sending confirmation email: {}", e.getMessage());
            }
        }

        return ResponseEntity.ok(updatedAppointment);
    }

    // ✅ FIXED: Mark appointment as completed (called when prescription is saved)
    @PutMapping("/{appointmentId}/complete")
    public ResponseEntity<AppointmentDTO> markAsCompleted(@PathVariable String appointmentId) {
        try {
            logger.info("Received request to mark appointment as completed: {}", appointmentId);
            AppointmentDTO completed = appointmentService.markCompleted(appointmentId);
            logger.info("Successfully marked appointment {} as completed", appointmentId);
            return ResponseEntity.ok(completed);
        } catch (ResourceNotFoundException e) {
            logger.error("Appointment not found with ID: {}", appointmentId);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error marking appointment {} as completed: {}", appointmentId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ✅ FIXED: Cancel appointment (UPDATE status to CANCELED, don't delete)
    @PutMapping("/cancel/{appointmentId}")
    public ResponseEntity<AppointmentDTO> cancelAppointment(@PathVariable String appointmentId) {
        try {
            logger.info("Cancelling appointment: {}", appointmentId);

            // Get appointment details before canceling (for email)
            AppointmentDTO appointment = appointmentService.getAppointmentById(appointmentId);

            // Cancel the appointment (updates status to CANCELED)
            appointmentService.cancelAppointment(appointmentId);

            // Get the updated appointment to return
            AppointmentDTO canceledAppointment = appointmentService.getAppointmentById(appointmentId);

            // Send cancellation email
            try {
                emailService.sendAppointmentCancellation(
                        appointment.getPatientEmail(),
                        appointment.getPatientName(),
                        appointment.getDoctorName(),
                        appointment.getAppointmentDateTime().toLocalDate().toString(),
                        appointment.getAppointmentDateTime().toLocalTime().toString()
                );
            } catch (Exception emailError) {
                logger.warn("Failed to send cancellation email: {}", emailError.getMessage());
            }

            return ResponseEntity.ok(canceledAppointment);

        } catch (ResourceNotFoundException e) {
            logger.error("Appointment not found: {}", appointmentId);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error cancelling appointment {}: {}", appointmentId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/{id}/reschedule")
    public ResponseEntity<AppointmentDTO> rescheduleAppointment(
            @PathVariable String id, @RequestParam LocalDateTime newDateTime) {
        try {
            AppointmentDTO rescheduledAppointment = appointmentService.rescheduleAppointment(id, newDateTime);

            try {
                emailService.sendAppointmentReschedule(
                        rescheduledAppointment.getPatientEmail(),
                        rescheduledAppointment.getPatientName(),
                        rescheduledAppointment.getDoctorName(),
                        rescheduledAppointment.getAppointmentDateTime().toLocalDate().toString(),
                        rescheduledAppointment.getAppointmentDateTime().toLocalTime().toString()
                );
            } catch (Exception e) {
                logger.error("Error sending reschedule email: {}", e.getMessage());
            }

            return ResponseEntity.ok(rescheduledAppointment);
        } catch (ResourceNotFoundException e) {
            logger.error("Appointment not found: {}", id);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error rescheduling appointment: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
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
        try {
            Appointment appointment = appointmentService.confirmAppointment(appointmentId);

            try {
                emailService.sendAppointmentConfirmation(
                        appointment.getPatientEmail(),
                        appointment.getPatientName(),
                        appointment.getDoctorName(),
                        appointment.getAppointmentDateTime().toLocalDate().toString(),
                        appointment.getAppointmentDateTime().toLocalTime().toString()
                );
            } catch (Exception e) {
                logger.error("Error sending confirmation email: {}", e.getMessage());
            }

            return ResponseEntity.ok("Appointment confirmed successfully. Confirmation email sent.");
        } catch (ResourceNotFoundException e) {
            logger.error("Appointment not found: {}", appointmentId);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error confirming appointment: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/search")
    public ResponseEntity<List<AppointmentDTO>> searchAppointments(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
    ) {
        List<AppointmentDTO> results = appointmentService.searchAppointments(status, startDate, endDate);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/doctor/{doctorId}/completed")
    public ResponseEntity<List<AppointmentDTO>> getCompletedAppointmentsByDoctor(@PathVariable String doctorId) {
        return ResponseEntity.ok(appointmentService.getCompletedAppointmentsByDoctor(doctorId));
    }
}