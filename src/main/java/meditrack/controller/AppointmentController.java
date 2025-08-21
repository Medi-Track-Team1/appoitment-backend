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
@CrossOrigin(origins = "*") // Add CORS support if needed
public class AppointmentController {

    private static final Logger logger = LoggerFactory.getLogger(AppointmentController.class);
    private final AppointmentService appointmentService;
    private final EmailService emailService;

    @Autowired
    public AppointmentController(AppointmentService appointmentService, EmailService emailService) {
        this.appointmentService = appointmentService;
        this.emailService = emailService;
    }

    // ✅ FIXED: Move all specific routes BEFORE the generic /{appointmentId} route

    @PostMapping(value = "/create", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AppointmentDTO> createAppointment(@Valid @RequestBody AppointmentDTO appointmentDTO) {
        logger.info("Received appointment creation request for patient: {}", appointmentDTO.getPatientId());
        logger.debug("Full appointment data: {}", appointmentDTO);

        try {
            AppointmentDTO createdAppointment = appointmentService.createAppointment(appointmentDTO);
            logger.info("Successfully created appointment with ID: {}", createdAppointment.getAppointmentId());
            return ResponseEntity.status(HttpStatus.CREATED).body(createdAppointment);
        } catch (Exception e) {
            logger.error("Error creating appointment: {}", e.getMessage(), e);
            throw e; // Let GlobalExceptionHandler handle it
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

    @GetMapping("/stats")
    public ResponseEntity<StatsDTO> getAppointmentStats() {
        return ResponseEntity.ok(appointmentService.getAppointmentStats());
    }

    @GetMapping("/patient/{patientId}/upcoming")
    public ResponseEntity<List<AppointmentDTO>> getUpcomingAppointmentsByPatient(@PathVariable String patientId) {
        return ResponseEntity.ok(appointmentService.getUpcomingAppointmentsByPatient(patientId));
    }

    @GetMapping("/patient/{patientId}/history")
    public ResponseEntity<List<AppointmentDTO>> getAppointmentHistoryByPatient(@PathVariable String patientId) {
        return ResponseEntity.ok(appointmentService.getAppointmentHistoryByPatient(patientId));
    }

    @GetMapping("/patient/{patientId}/completed")
    public ResponseEntity<List<AppointmentDTO>> getCompletedAppointmentsByPatient(@PathVariable String patientId) {
        return ResponseEntity.ok(appointmentService.getCompletedAppointmentsByPatient(patientId));
    }

    @GetMapping("/doctor/{doctorId}/upcoming")
    public ResponseEntity<List<AppointmentDTO>> getUpcomingAppointmentsByDoctor(@PathVariable String doctorId) {
        return ResponseEntity.ok(appointmentService.getUpcomingAppointmentsByDoctor(doctorId));
    }

    @GetMapping("/doctor/{doctorId}/history")
    public ResponseEntity<List<AppointmentDTO>> getAppointmentHistoryByDoctor(@PathVariable String doctorId) {
        return ResponseEntity.ok(appointmentService.getAppointmentHistoryByDoctor(doctorId));
    }

    @GetMapping("/doctor/{doctorId}/completed")
    public ResponseEntity<List<AppointmentDTO>> getCompletedAppointmentsByDoctor(@PathVariable String doctorId) {
        return ResponseEntity.ok(appointmentService.getCompletedAppointmentsByDoctor(doctorId));
    }

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

    @PutMapping("/cancel/{appointmentId}")
    public ResponseEntity<AppointmentDTO> cancelAppointment(
            @PathVariable String appointmentId,
            @RequestBody Map<String, String> payload) {
        try {
            String reason = payload.get("reason");
            logger.info("Cancelling appointment: {} | Reason: {}", appointmentId, reason);

            if (reason == null || reason.trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            appointmentService.cancelAppointment(appointmentId, reason);
            AppointmentDTO canceledAppointment = appointmentService.getAppointmentById(appointmentId);

            logger.info("Appointment {} cancelled successfully", appointmentId);
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

    @PostMapping("/revisit/{appointmentId}")
    public ResponseEntity<AppointmentDTO> createRevisitAppointment(
            @PathVariable String appointmentId,
            @RequestBody RevisitRequest revisitRequest) {
        try {
            logger.info("Creating revisit appointment for original appointment: {} | Reason: {} | New Date: {} | New Time: {}",
                    appointmentId, revisitRequest.getReason(), revisitRequest.getNewDate(), revisitRequest.getNewTime());

            LocalDate date = LocalDate.parse(revisitRequest.getNewDate());
            LocalTime time = LocalTime.parse(revisitRequest.getNewTime());
            LocalDateTime newDateTime = LocalDateTime.of(date, time);

            AppointmentDTO newRevisitAppointment = appointmentService.revisitAppointment(appointmentId, newDateTime, revisitRequest.getReason());

            logger.info("New revisit appointment created with ID: {}", newRevisitAppointment.getAppointmentId());
            return ResponseEntity.status(HttpStatus.CREATED).body(newRevisitAppointment);

        } catch (ResourceNotFoundException e) {
            logger.error("Original appointment not found: {}", appointmentId);
            return ResponseEntity.notFound().build();
        } catch (ValidationException e) {
            logger.error("Validation failed for revisit appointment: {}", e.getMessage());
            return ResponseEntity.badRequest().body(null);
        } catch (ConflictException e) {
            logger.error("Scheduling conflict for revisit appointment: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(null);
        } catch (Exception e) {
            logger.error("Error creating revisit appointment for {}: {}", appointmentId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ✅ MOVED: Generic routes AFTER specific ones
    @GetMapping
    public ResponseEntity<List<AppointmentDTO>> getAllAppointments() {
        return ResponseEntity.ok(appointmentService.getAllAppointments());
    }

    @GetMapping("/{appointmentId}")
    public ResponseEntity<AppointmentDTO> getAppointmentById(@PathVariable String appointmentId) {
        logger.info("Fetching appointment by ID: {}", appointmentId);
        try {
            AppointmentDTO appointment = appointmentService.getAppointmentById(appointmentId);
            return ResponseEntity.ok(appointment);
        } catch (ResourceNotFoundException e) {
            logger.error("Appointment not found: {}", appointmentId);
            return ResponseEntity.notFound().build();
        }
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
}