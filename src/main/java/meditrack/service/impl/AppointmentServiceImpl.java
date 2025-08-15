package meditrack.service.impl;

import meditrack.dto.*;
import meditrack.enums.AppointmentStatus;
import meditrack.exception.*;
import meditrack.feign.*;
import meditrack.model.*;
import meditrack.repository.*;
import meditrack.service.*;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AppointmentServiceImpl implements AppointmentService {

    private static final Logger logger = LoggerFactory.getLogger(AppointmentServiceImpl.class);
    private static final Random random = new Random();
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("hh:mm a");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final int BUFFER_MINUTES = 30;
    private static final int WORKING_HOUR_START = 9;
    private static final int WORKING_HOUR_END = 17;

    @Autowired private AppointmentRepository appointmentRepository;
    @Autowired private PatientRepository patientRepository;
    @Autowired private DoctorRepository doctorRepository;
    @Autowired private EmailService emailService;
    @Autowired private ModelMapper modelMapper;
    @Autowired private PatientFeign patientFeign;
    @Autowired private DoctorServiceClient doctorFeign;

    @Override
    public AppointmentDTO createAppointment(AppointmentDTO appointmentDTO) {
        logger.info("Creating appointment for patient: {}", appointmentDTO.getPatientId());

        validatePatient(appointmentDTO.getPatientId());
        DoctorDTO doctor = fetchDoctorDetails(appointmentDTO.getDoctorId());
        validateAppointmentTime(appointmentDTO.getAppointmentDateTime(), appointmentDTO.getDuration());
        checkForConflictingAppointments(appointmentDTO, doctor);

        Appointment appointment = buildAppointmentFromDTO(appointmentDTO, doctor);
        Appointment savedAppointment = appointmentRepository.save(appointment);

        sendAppointmentConfirmationEmail(savedAppointment);
        logger.info("Appointment created successfully with ID: {}", savedAppointment.getAppointmentId());

        return convertToDTO(savedAppointment);
    }

    private DoctorDTO fetchDoctorDetails(String doctorId) {
        DoctorDTO doctor = doctorFeign.getDoctorById(doctorId);
        if (doctor == null) {
            throw new ResourceNotFoundException("Doctor not found with id: " + doctorId);
        }
        return doctor;
    }


    private void validatePatient(String patientId) {
        ApiResponse<PatientDTO> patientResponse = patientFeign.getPatientById(patientId);
        if (patientResponse == null || !patientResponse.isSuccess() || patientResponse.getData() == null) {
            throw new ResourceNotFoundException("Patient not found with id: " + patientId);
        }
    }

    private void validateAppointmentTime(LocalDateTime requestedStart, int duration) {
        LocalDateTime requestedEnd = requestedStart.plusMinutes(duration);

        if (requestedStart.isBefore(LocalDateTime.now())) {
            throw new ValidationException("Appointment time must be in the future");
        }

        if (requestedStart.getHour() < WORKING_HOUR_START || requestedEnd.getHour() >= WORKING_HOUR_END) {
            throw new ValidationException(String.format(
                    "Appointments must be between %dAM and %dPM", WORKING_HOUR_START, WORKING_HOUR_END));
        }
    }

    private void checkForConflictingAppointments(AppointmentDTO appointmentDTO, DoctorDTO doctor) {
        LocalDateTime requestedStart = appointmentDTO.getAppointmentDateTime();
        LocalDateTime requestedEnd = requestedStart.plusMinutes(appointmentDTO.getDuration());
        LocalDateTime startOfDay = requestedStart.toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);

        List<Appointment> existingAppointments = appointmentRepository
                .findByDoctorIdAndDate(doctor.getDoctorId(), startOfDay, endOfDay);

        for (Appointment existing : existingAppointments) {
            LocalDateTime existingStart = existing.getAppointmentDateTime();
            LocalDateTime existingEnd = existingStart.plusMinutes(existing.getDuration());

            LocalDateTime bufferStart = existingStart.minusMinutes(BUFFER_MINUTES);
            LocalDateTime bufferEnd = existingEnd.plusMinutes(BUFFER_MINUTES);

            if (requestedStart.isBefore(bufferEnd) && requestedEnd.isAfter(bufferStart)) {
                // Round up to the next full hour
                if (bufferEnd.getMinute() != 0) {
                    bufferEnd = bufferEnd.withMinute(0).plusHours(1);
                }

                // Special rule: If time is between 12:00 PM and 12:59 PM, set to exactly 1:00 PM
                if (bufferEnd.getHour() == 12) {
                    bufferEnd = bufferEnd.withHour(13).withMinute(0);
                }

                // Format in 12-hour AM/PM style
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy h:mm a");
                String suggestedTime = bufferEnd.format(formatter);

                throw new ValidationException(String.format(
                        "Doctor %s already has an appointment at this time. Please choose a slot after %s",
                        doctor.getDoctorName(), suggestedTime));
            }
        }
    }

    private Appointment buildAppointmentFromDTO(AppointmentDTO appointmentDTO, DoctorDTO doctor) {
        Appointment appointment = modelMapper.map(appointmentDTO, Appointment.class);
        appointment.setAppointmentId(generateAppointmentId());
        appointment.setStatus(AppointmentStatus.PENDING);
        appointment.setCreatedAt(LocalDateTime.now());
        appointment.setUpdatedAt(LocalDateTime.now());
        appointment.setDoctorName(doctor.getDoctorName());
        return appointment;
    }

    private String generateAppointmentId() {
        String id;
        do {
            id = "APP-" + String.format("%04d", random.nextInt(10000));
        } while (appointmentRepository.existsByAppointmentId(id));
        return id;
    }

    @Override
    public AppointmentDTO getAppointmentById(String appointmentId) {
        Appointment appointment = appointmentRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with id: " + appointmentId));
        return convertToDTO(appointment);
    }

    @Override
    public List<AppointmentDTO> getAllAppointments() {
        return appointmentRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public AppointmentDTO updateAppointment(String appointmentId, AppointmentDTO appointmentDTO) {
        Appointment existing = getExistingAppointment(appointmentId);
        DoctorDTO doctor = fetchDoctorDetails(appointmentDTO.getDoctorId());

        validateAppointmentTime(appointmentDTO.getAppointmentDateTime(), appointmentDTO.getDuration());
        checkForConflictingAppointments(appointmentDTO, doctor);

        modelMapper.map(appointmentDTO, existing);
        existing.setUpdatedAt(LocalDateTime.now());
        existing.setDoctorName(doctor.getDoctorName());

        Appointment updated = appointmentRepository.save(existing);
        return convertToDTO(updated);
    }

    private Appointment getExistingAppointment(String appointmentId) {
        return appointmentRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with id: " + appointmentId));
    }

    @Override
    public void cancelAppointment(String appointmentId, String reason) {
        logger.info("Cancelling appointment: {}", appointmentId);

        Appointment appointment = getExistingAppointment(appointmentId);
        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.setCancellationReason(reason);
        appointment.setUpdatedAt(LocalDateTime.now());

        appointmentRepository.save(appointment);
        sendCancellationEmail(appointment);
        logger.info("Appointment {} cancelled successfully", appointmentId);
    }

    @Override
    public AppointmentDTO rescheduleAppointment(String appointmentId, LocalDateTime newDateTime) {
        logger.info("Rescheduling appointment: {} to {}", appointmentId, newDateTime);

        Appointment appointment = getExistingAppointment(appointmentId);
        validateAppointmentTime(newDateTime, appointment.getDuration());

        LocalDateTime newEnd = newDateTime.plusMinutes(appointment.getDuration());
        List<Appointment> conflicts = appointmentRepository
                .findByDateTimeBetweenAndDoctorId(newDateTime, newEnd, appointment.getDoctorId())
                .stream()
                .filter(a -> !a.getAppointmentId().equals(appointmentId))
                .collect(Collectors.toList());

        if (!conflicts.isEmpty()) {
            throw new ValidationException("Doctor has conflicting appointments at this time");
        }

        appointment.setAppointmentDateTime(newDateTime);
        appointment.setStatus(AppointmentStatus.RESCHEDULED);
        appointment.setUpdatedAt(LocalDateTime.now());

        Appointment rescheduled = appointmentRepository.save(appointment);
        sendRescheduleEmail(rescheduled);

        return convertToDTO(rescheduled);
    }

    @Override
    public AppointmentDTO revisitAppointment(String appointmentId, LocalDateTime newDateTime, String reason) {
        logger.info("Revisiting appointment: {} at {}", appointmentId, newDateTime);

        Appointment originalAppointment = getExistingAppointment(appointmentId);
        validateAppointmentTime(newDateTime, originalAppointment.getDuration());

        Appointment revisitAppointment = new Appointment();
        modelMapper.map(originalAppointment, revisitAppointment);

        revisitAppointment.setId(null);
        revisitAppointment.setAppointmentId(generateAppointmentId());
        revisitAppointment.setAppointmentDateTime(newDateTime);
        revisitAppointment.setStatus(AppointmentStatus.PENDING);
        revisitAppointment.setRevisitReason(reason);
        revisitAppointment.setPreviousAppointmentId(appointmentId);
        revisitAppointment.setCreatedAt(LocalDateTime.now());
        revisitAppointment.setUpdatedAt(LocalDateTime.now());

        Appointment savedAppointment = appointmentRepository.save(revisitAppointment);
        sendRevisitEmail(savedAppointment);

        return convertToDTO(savedAppointment);
    }

    @Override
    public AppointmentDTO markCompleted(String appointmentId) {
        logger.info("Marking appointment as completed: {}", appointmentId);

        Appointment appointment = getExistingAppointment(appointmentId);
        appointment.setStatus(AppointmentStatus.COMPLETED);
        appointment.setUpdatedAt(LocalDateTime.now());

        Appointment completed = appointmentRepository.save(appointment);
        sendCompletionEmail(completed);

        return convertToDTO(completed);
    }

    @Override
    public List<AppointmentDTO> getUpcomingAppointmentsByPatient(String patientId) {
        validatePatientExists(patientId);
        return appointmentRepository
                .findByPatientIdAndAppointmentDateTimeAfter(patientId, LocalDateTime.now())
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<AppointmentDTO> getUpcomingAppointmentsByDoctor(String doctorId) {
        validateDoctorExists(doctorId);
        return appointmentRepository
                .findByDoctorIdAndAppointmentDateTimeAfter(doctorId, LocalDateTime.now())
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<AppointmentDTO> getAppointmentHistoryByPatient(String patientId) {
        validatePatientExists(patientId);

        List<Appointment> completed = appointmentRepository
                .findByPatientIdAndStatus(patientId, AppointmentStatus.COMPLETED.name());
        List<Appointment> cancelled = appointmentRepository
                .findByPatientIdAndStatus(patientId, AppointmentStatus.CANCELLED.name());

        List<Appointment> history = new ArrayList<>();
        history.addAll(completed);
        history.addAll(cancelled);

        return history.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<AppointmentDTO> getAppointmentHistoryByDoctor(String doctorId) {
        validateDoctorExists(doctorId);

        List<Appointment> completed = appointmentRepository
                .findByDoctorIdAndStatus(doctorId, AppointmentStatus.COMPLETED.name());
        List<Appointment> cancelled = appointmentRepository
                .findByDoctorIdAndStatus(doctorId, AppointmentStatus.CANCELLED.name());

        List<Appointment> history = new ArrayList<>();
        history.addAll(completed);
        history.addAll(cancelled);

        return history.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public StatsDTO getAppointmentStats() {
        StatsDTO stats = new StatsDTO();
        stats.setTotalAppointments(appointmentRepository.count());
        stats.setPendingAppointments(appointmentRepository.countByStatus(AppointmentStatus.PENDING.name()));
        stats.setConfirmedAppointments(appointmentRepository.countByStatus(AppointmentStatus.CONFIRMED.name()));
        stats.setCompletedAppointments(appointmentRepository.countByStatus(AppointmentStatus.COMPLETED.name()));
        stats.setCancelledAppointments(appointmentRepository.countByStatus(AppointmentStatus.CANCELLED.name()));
        return stats;
    }

    @Override
    public boolean deleteAppointmentById(String appointmentId) {
        if (appointmentRepository.existsByAppointmentId(appointmentId)) {
            appointmentRepository.deleteByAppointmentId(appointmentId);
            logger.info("Appointment {} deleted successfully", appointmentId);
            return true;
        }
        return false;
    }

    @Override
    public Appointment confirmAppointment(String appointmentId) {
        Appointment appointment = getExistingAppointment(appointmentId);
        appointment.setStatus(AppointmentStatus.CONFIRMED);
        appointment.setUpdatedAt(LocalDateTime.now());

        Appointment confirmed = appointmentRepository.save(appointment);
        sendConfirmationEmail(confirmed);

        return confirmed;
    }

    @Override
    public List<AppointmentDTO> searchAppointments(String status, String startDate, String endDate) {
        LocalDateTime start = startDate != null ?
                LocalDate.parse(startDate).atStartOfDay() :
                LocalDateTime.now().minusMonths(1);

        LocalDateTime end = endDate != null ?
                LocalDate.parse(endDate).atTime(23, 59, 59) :
                LocalDateTime.now().plusMonths(1);

        if (status != null && !status.isEmpty()) {
            return appointmentRepository
                    .findByStatusAndAppointmentDateTimeBetween(status, start, end)
                    .stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
        } else {
            return appointmentRepository
                    .findByAppointmentDateTimeBetween(start, end)
                    .stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
        }
    }

    @Override
    public List<Appointment> findByPatientIdAndStatus(String patientId, String status) {
        validatePatientExists(patientId);
        return appointmentRepository.findByPatientIdAndStatus(patientId, status);
    }

    @Override
    public List<AppointmentDTO> getCompletedAppointmentsByDoctor(String doctorId) {
        validateDoctorExists(doctorId);
        return appointmentRepository
                .findByDoctorIdAndStatus(doctorId, AppointmentStatus.COMPLETED.name())
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<AppointmentDTO> getCompletedAppointmentsByPatient(String patientId) {
        validatePatientExists(patientId);
        return appointmentRepository
                .findByPatientIdAndStatus(patientId, AppointmentStatus.COMPLETED.name())
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private void validatePatientExists(String patientId) {
        if (!patientRepository.existsByPatientId(patientId)) {
            throw new ResourceNotFoundException("Patient not found with id: " + patientId);
        }
    }

    private void validateDoctorExists(String doctorId) {
        if (!doctorRepository.existsByDoctorId(doctorId)) {
            throw new ResourceNotFoundException("Doctor not found with id: " + doctorId);
        }
    }

    private void sendAppointmentConfirmationEmail(Appointment appointment) {
        try {
            if (appointment.getPatientEmail() != null && !appointment.getPatientEmail().isBlank()) {
                emailService.sendAppointmentBooked(
                        appointment.getPatientEmail(),
                        appointment.getPatientName(),
                        appointment.getDoctorName(),
                        appointment.getAppointmentDateTime().format(DATE_FORMATTER),
                        appointment.getAppointmentDateTime().format(TIME_FORMATTER)
                );
            }
        } catch (Exception e) {
            logger.error("Error sending appointment email: {}", e.getMessage());
        }
    }

    private void sendCancellationEmail(Appointment appointment) {
        try {
            if (appointment.getPatientEmail() != null && !appointment.getPatientEmail().isBlank()) {
                emailService.sendAppointmentCancellation(
                        appointment.getPatientEmail(),
                        appointment.getPatientName(),
                        appointment.getDoctorName(),
                        appointment.getAppointmentDateTime().format(DATE_FORMATTER),
                        appointment.getAppointmentDateTime().format(TIME_FORMATTER),
                        appointment.getCancellationReason()
                );
            }
        } catch (Exception e) {
            logger.error("Error sending cancellation email: {}", e.getMessage());
        }
    }

    private void sendRescheduleEmail(Appointment appointment) {
        try {
            if (appointment.getPatientEmail() != null && !appointment.getPatientEmail().isBlank()) {
                emailService.sendAppointmentRescheduled(
                        appointment.getPatientEmail(),
                        appointment.getPatientName(),
                        appointment.getDoctorName(),
                        appointment.getAppointmentDateTime().format(DATE_FORMATTER),
                        appointment.getAppointmentDateTime().format(TIME_FORMATTER)
                );
            }
        } catch (Exception e) {
            logger.error("Error sending reschedule email: {}", e.getMessage());
        }
    }

    private void sendRevisitEmail(Appointment appointment) {
        try {
            if (appointment.getPatientEmail() != null && !appointment.getPatientEmail().isBlank()) {
                emailService.sendAppointmentRevisit(
                        appointment.getPatientEmail(),
                        appointment.getPatientName(),
                        appointment.getDoctorName(),
                        appointment.getAppointmentDateTime().format(DATE_FORMATTER),
                        appointment.getAppointmentDateTime().format(TIME_FORMATTER),
                        appointment.getRevisitReason()
                );
            }
        } catch (Exception e) {
            logger.error("Error sending revisit email: {}", e.getMessage());
        }
    }

    private void sendCompletionEmail(Appointment appointment) {
        try {
            if (appointment.getPatientEmail() != null && !appointment.getPatientEmail().isBlank()) {
                emailService.sendAppointmentCompletion(
                        appointment.getPatientEmail(),
                        appointment.getPatientName(),
                        appointment.getDoctorName()
                );
            }
        } catch (Exception e) {
            logger.error("Error sending completion email: {}", e.getMessage());
        }
    }

    private void sendConfirmationEmail(Appointment appointment) {
        try {
            if (appointment.getPatientEmail() != null && !appointment.getPatientEmail().isBlank()) {
                emailService.sendAppointmentConfirmation(
                        appointment.getPatientEmail(),
                        appointment.getPatientName(),
                        appointment.getDoctorName(),
                        appointment.getAppointmentDateTime().format(DATE_FORMATTER),
                        appointment.getAppointmentDateTime().format(TIME_FORMATTER)
                );
            }
        } catch (Exception e) {
            logger.error("Error sending confirmation email: {}", e.getMessage());
        }
    }

    private AppointmentDTO convertToDTO(Appointment appointment) {
        return modelMapper.map(appointment, AppointmentDTO.class);
    }
}