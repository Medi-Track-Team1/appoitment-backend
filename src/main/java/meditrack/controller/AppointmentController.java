package meditrack.controller;

import jakarta.validation.Valid;
import meditrack.dto.AppointmentDTO;
import meditrack.dto.StatsDTO;
import meditrack.exception.SlotUnavailableException;
import meditrack.model.Appointment;
import meditrack.service.AppointmentService;
import meditrack.service.EmailService;
import meditrack.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    private static final Logger logger = LoggerFactory.getLogger(AppointmentController.class);
    private final AppointmentService appointmentService;
    private final EmailService emailService;

    @Autowired
    public AppointmentController(AppointmentService appointmentService, EmailService emailService) {
        this.appointmentService = appointmentService;
        this.emailService = emailService;
    }


//    @PostMapping("/create")
@PostMapping(value = "/create", consumes = MediaType.APPLICATION_JSON_VALUE)
public ResponseEntity<AppointmentDTO> createAppointment(@Valid @RequestBody AppointmentDTO appointmentDTO) {
    logger.info("Received appointment creation request for patient: {}", appointmentDTO.getPatientId());

    // Don't catch any exceptions here - let them bubble up to GlobalExceptionHandler
    AppointmentDTO createdAppointment = appointmentService.createAppointment(appointmentDTO);
    return ResponseEntity.status(HttpStatus.CREATED).body(createdAppointment);
}
    private String fixDateTimeFormat(String dateTime) {
        // Fix malformed date like "2025-09-20112:31:002" to "2025-09-20 11:23:10"
        try {
            String datePart = dateTime.substring(0, 10); // Get yyyy-MM-dd
            String timePart = dateTime.substring(10); // Get remaining time part

            // Clean up malformed time
            timePart = timePart.replaceAll("[^0-9]", "");
            if (timePart.length() > 6) {
                timePart = timePart.substring(0, 6); // Take only HHmmss
            }

            // Reconstruct with proper formatting
            String fixedTime = String.format("%02d:%02d:%02d",
                    Integer.parseInt(timePart.substring(0, 2)) % 24,
                    Integer.parseInt(timePart.substring(2, 4)) % 60,
                    Integer.parseInt(timePart.substring(4, 6)) % 60);

            return datePart + " " + fixedTime;
        } catch (Exception e) {
            throw new DateTimeParseException("Invalid date format", dateTime, 0);
        }
    }

    @GetMapping("/{appointmentId}")
    public ResponseEntity<AppointmentDTO> getAppointmentById(@PathVariable String appointmentId) {
        return ResponseEntity.ok(appointmentService.getAppointmentById(appointmentId
        ));
    }

    @GetMapping
    public ResponseEntity<List<AppointmentDTO>> getAllAppointments() {
        return ResponseEntity.ok(appointmentService.getAllAppointments());
    }

    @PutMapping("/{appointmentId}")
    public ResponseEntity<AppointmentDTO> updateAppointment(
            @PathVariable String appointmentId, @RequestBody AppointmentDTO appointmentDTO) {
        AppointmentDTO updatedAppointment = appointmentService.updateAppointment(appointmentId, appointmentDTO);

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

    // ✅ Cancel appointment with reason (status updated to CANCELLED, not deleted)
    @PutMapping("/cancel/{appointmentId}")
    public ResponseEntity<AppointmentDTO> cancelAppointment(
            @PathVariable String appointmentId,
            @RequestBody Map<String, String> payload) {  // ✅ Accept reason from JSON body
        try {
            String reason = payload.get("reason");  // Extract reason
            logger.info("Cancelling appointment: {} | Reason: {}", appointmentId, reason);

            if (reason == null || reason.trim().isEmpty()) {
                return ResponseEntity.badRequest().build(); // ✅ Validation: reason required
            }

            // Get appointment details before canceling (for email)
            AppointmentDTO appointment = appointmentService.getAppointmentById(appointmentId);

            // Cancel the appointment and save reason in DB
            appointmentService.cancelAppointment(appointmentId, reason);

            // Get updated appointment to return in response
            AppointmentDTO canceledAppointment = appointmentService.getAppointmentById(appointmentId);

            // Send cancellation email with reason
            try {
                emailService.sendAppointmentCancellation(
                        appointment.getPatientEmail(),
                        appointment.getPatientName(),
                        appointment.getDoctorName(),
                        appointment.getAppointmentDateTime().toLocalDate().toString(),
                        appointment.getAppointmentDateTime().toLocalTime().toString(),
                        reason // ✅ Pass reason to email
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

    @PostMapping("/{appointmentId}/reschedule")
    public ResponseEntity<AppointmentDTO> rescheduleAppointment(
            @PathVariable String appointmentId, @RequestParam LocalDateTime newDateTime) {
        try {
            AppointmentDTO rescheduledAppointment = appointmentService.rescheduleAppointment(appointmentId, newDateTime);
            try {
                emailService.sendAppointmentRescheduled(
                        rescheduledAppointment.getPatientEmail(),
                        rescheduledAppointment.getPatientName(),
                        rescheduledAppointment.getDoctorName(),
                        rescheduledAppointment.getAppointmentDateTime().toLocalDate().toString(),
                        rescheduledAppointment.getAppointmentDateTime().toLocalTime().toString()
                );
            } catch (Exception e) {
                logger.error("Failed to send reschedule email: {}", e.getMessage());
            }
            return ResponseEntity.ok(rescheduledAppointment);
        } catch (Exception e) {
            logger.error("Error rescheduling appointment: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{appointmentId}")
    public ResponseEntity<String> deleteAppointment(@PathVariable String appointmentId) {
        boolean isDeleted = appointmentService.deleteAppointmentById(appointmentId);
        if (isDeleted) {
            return ResponseEntity.ok("Appointment deleted successfully");
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Appointment not found");
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

    @GetMapping("/patient/{patientId}/history")
    public ResponseEntity<List<AppointmentDTO>> getAppointmentHistoryByPatient(@PathVariable String patientId) {
        return ResponseEntity.ok(appointmentService.getAppointmentHistoryByPatient(patientId));
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
    @GetMapping("/patient/{patientId}/completed")
    public ResponseEntity<List<AppointmentDTO>> getCompletedAppointmentsByPatient(@PathVariable String patientId) {
        return ResponseEntity.ok(appointmentService.getCompletedAppointmentsByPatient(patientId));
    }


    // ✅ Revisit appointment with new date/time and reason
    @PutMapping("/revisit/{appointmentId}")
    public ResponseEntity<AppointmentDTO> revisitAppointment(
            @PathVariable String appointmentId,
            @RequestParam String reason,
            @RequestParam String newDate,    // Format: yyyy-MM-dd
            @RequestParam String newTime) {  // Format: HH:mm
        try {
            logger.info("Revisiting appointment: {} | Reason: {} | New Date: {} | New Time: {}", appointmentId, reason, newDate, newTime);

            // Get existing appointment before update (for email)
            AppointmentDTO existingAppointment = appointmentService.getAppointmentById(appointmentId);

            // Parse date and time
            LocalDate date = LocalDate.parse(newDate);
            LocalTime time = LocalTime.parse(newTime);
            LocalDateTime newDateTime = LocalDateTime.of(date, time);

            // Call service to update date, time, and reason
            appointmentService.revisitAppointment(appointmentId, newDateTime, reason);

            // Get updated appointment
            AppointmentDTO updatedAppointment = appointmentService.getAppointmentById(appointmentId);

            // Send email notification
            try {
                emailService.sendAppointmentRevisit(
                        existingAppointment.getPatientEmail(),
                        existingAppointment.getPatientName(),
                        existingAppointment.getDoctorName(), // ← missing argument
                        existingAppointment.getAppointmentDateTime().toLocalDate().toString(),
                        existingAppointment.getAppointmentDateTime().toLocalTime().toString(),
                        reason
                );


            } catch (Exception emailError) {
                logger.warn("Failed to send revisit email: {}", emailError.getMessage());
            }

            return ResponseEntity.ok(updatedAppointment);

        } catch (ResourceNotFoundException e) {
            logger.error("Appointment not found: {}", appointmentId);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error revisiting appointment {}: {}", appointmentId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

}
