package meditrack.controller;

import jakarta.validation.Valid;
import meditrack.dto.AppointmentDTO;
import meditrack.dto.RevisitRequest;
import meditrack.dto.StatsDTO;
import meditrack.exception.ConflictException;
import meditrack.exception.SlotUnavailableException;
import meditrack.exception.ValidationException;
import meditrack.model.Appointment;
import meditrack.service.AppointmentService;
import meditrack.service.EmailService;
import meditrack.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
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

    // MOVED UP: Specific routes first
    @PostMapping(value = "/create", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AppointmentDTO> createAppointment(@Valid @RequestBody AppointmentDTO appointmentDTO) {
        logger.info("Received appointment creation request for patient: {}", appointmentDTO.getPatientId());

        // Don't catch any exceptions here - let them bubble up to GlobalExceptionHandler
        AppointmentDTO createdAppointment = appointmentService.createAppointment(appointmentDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdAppointment);
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

    @GetMapping("/stats")
    public ResponseEntity<StatsDTO> getAppointmentStats() {
        return ResponseEntity.ok(appointmentService.getAppointmentStats());
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

    @GetMapping("/doctor/{doctorId}/completed")
    public ResponseEntity<List<AppointmentDTO>> getCompletedAppointmentsByDoctor(@PathVariable String doctorId) {
        return ResponseEntity.ok(appointmentService.getCompletedAppointmentsByDoctor(doctorId));
    }
    
    @GetMapping("/patient/{patientId}/completed")
    public ResponseEntity<List<AppointmentDTO>> getCompletedAppointmentsByPatient(@PathVariable String patientId) {
        return ResponseEntity.ok(appointmentService.getCompletedAppointmentsByPatient(patientId));
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

    // ✅ FIXED: Cancel appointment with reason - NO EMAIL SENDING HERE (service handles it)
  @PutMapping("/cancel/{appointmentId}")
public ResponseEntity<?> cancelAppointment(
        @PathVariable String appointmentId,
        @RequestBody Map<String, String> payload) {
    try {
        String reason = payload.get("reason");

        // ✅ IMPROVED: Better validation and logging
        logger.info("Cancelling appointment: {} | Reason: {} | Payload: {}",
                appointmentId, reason, payload);

        if (reason == null || reason.trim().isEmpty()) {
            logger.warn("Cancel request missing reason for appointment: {}", appointmentId);
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Validation failed",
                    "message", "Cancellation reason is required"
            ));
        }

        if (reason.trim().length() > 200) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Validation failed",
                    "message", "Cancellation reason must not exceed 200 characters"
            ));
        }

        // ✅ IMPROVED: First check if appointment exists before cancelling
        AppointmentDTO existingAppointment = appointmentService.getAppointmentById(appointmentId);
        logger.info("Found appointment to cancel: {} for patient: {}",
                appointmentId, existingAppointment.getPatientName());

        // ✅ FIXED: Remove duplicate call to cancelAppointment
        appointmentService.cancelAppointment(appointmentId, reason.trim());

        // Get updated appointment to return in response
        AppointmentDTO canceledAppointment = appointmentService.getAppointmentById(appointmentId);

        logger.info("Appointment {} cancelled successfully, status: {}",
                appointmentId, canceledAppointment.getStatus());

        return ResponseEntity.ok(Map.of(
                "message", "Appointment cancelled successfully",
                "appointment", canceledAppointment
        ));

    } catch (ResourceNotFoundException e) {
        logger.error("Appointment not found for cancellation: {}", appointmentId);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "error", "Appointment not found",
                "message", "Appointment not found with ID: " + appointmentId
        ));
    } catch (Exception e) {
        logger.error("Error cancelling appointment {}: {}", appointmentId, e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "Internal server error",
                "message", "Failed to cancel appointment: " + e.getMessage()
        ));
    }
}
    @PostMapping("/{appointmentId}/reschedule")
    public ResponseEntity<?> rescheduleAppointment(
            @PathVariable String appointmentId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime newDateTime) {

        try {
            logger.info("Attempting to reschedule appointment {} to {}", appointmentId, newDateTime);
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
                // Continue even if email fails
            }

            return ResponseEntity.ok(rescheduledAppointment);
        } catch (ValidationException e) {
            logger.warn("Validation failed for rescheduling: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Validation failed",
                    "message", e.getMessage()
            ));
        } catch (ConflictException e) {
            logger.warn("Conflict found for rescheduling: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "error", "Scheduling conflict",
                    "message", e.getMessage()
            ));
        } catch (ResourceNotFoundException e) {
            logger.warn("Appointment not found: {}", appointmentId);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Unexpected error rescheduling appointment: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "error", "Internal server error",
                    "message", "Failed to reschedule appointment"
            ));
        }
    }




    // ✅ FIXED: Create revisit appointment - NO EMAIL SENDING HERE (service handles it)


   @PostMapping("/revisit/{appointmentId}")
public ResponseEntity<?> createRevisitAppointment(
        @PathVariable String appointmentId,
        @RequestBody RevisitRequest revisitRequest) {
    try {
        // ✅ IMPROVED: Better validation and logging
        logger.info("Creating revisit appointment for original appointment: {} | Request: {}",
                appointmentId, revisitRequest);

        if (revisitRequest.getReason() == null || revisitRequest.getReason().trim().isEmpty()) {
            logger.warn("Revisit request missing reason for appointment: {}", appointmentId);
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Validation failed",
                    "message", "Reason for revisit is required"
            ));
        }

        if (revisitRequest.getNewDate() == null || revisitRequest.getNewTime() == null) {
            logger.warn("Revisit request missing date/time for appointment: {}", appointmentId);
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Validation failed",
                    "message", "Date and time are required"
            ));
        }

        // Parse date and time
        LocalDate date = LocalDate.parse(revisitRequest.getNewDate());
        LocalTime time = LocalTime.parse(revisitRequest.getNewTime());
        LocalDateTime newDateTime = LocalDateTime.of(date, time);

        // ✅ IMPROVED: Better validation of date/time
        if (newDateTime.isBefore(LocalDateTime.now())) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Validation failed",
                    "message", "Revisit appointment cannot be scheduled in the past"
            ));
        }

        // ✅ FIXED: Remove duplicate code and variable declaration
        AppointmentDTO revisitAppointment = appointmentService.revisitAppointment(
                appointmentId, newDateTime, revisitRequest.getReason().trim());

        logger.info("New revisit appointment created successfully with ID: {}",
                revisitAppointment.getAppointmentId());

        return ResponseEntity.status(HttpStatus.CREATED).body(revisitAppointment);

    } catch (ResourceNotFoundException e) {
        logger.error("Original appointment not found: {}", appointmentId);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "error", "Appointment not found",
                "message", "Original appointment not found with ID: " + appointmentId
        ));
    } catch (ValidationException e) {
        logger.error("Validation failed for revisit appointment: {}", e.getMessage());
        return ResponseEntity.badRequest().body(Map.of(
                "error", "Validation failed",
                "message", e.getMessage()
        ));
    } catch (ConflictException e) {
        logger.error("Scheduling conflict for revisit appointment: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "error", "Scheduling conflict",
                "message", e.getMessage()
        ));
    } catch (Exception e) {
        logger.error("Unexpected error creating revisit appointment for {}: {}", appointmentId, e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "Internal server error",
                "message", "Failed to create revisit appointment: " + e.getMessage()
        ));
    }
}

    // MOVED DOWN: Generic routes last

    @GetMapping
    public ResponseEntity<List<AppointmentDTO>> getAllAppointments() {
        return ResponseEntity.ok(appointmentService.getAllAppointments());
    }

    @GetMapping("/{appointmentId}")
    public ResponseEntity<AppointmentDTO> getAppointmentById(@PathVariable String appointmentId) {
        return ResponseEntity.ok(appointmentService.getAppointmentById(appointmentId));
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

    @DeleteMapping("/{appointmentId}")
    public ResponseEntity<String> deleteAppointment(@PathVariable String appointmentId) {
        boolean isDeleted = appointmentService.deleteAppointmentById(appointmentId);
        if (isDeleted) {
            return ResponseEntity.ok("Appointment deleted successfully");
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Appointment not found");
        }
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
}
