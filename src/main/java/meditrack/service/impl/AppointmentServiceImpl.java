



package meditrack.service.impl;

import meditrack.dto.AppointmentDTO;
import meditrack.dto.StatsDTO;
import meditrack.exception.ResourceNotFoundException;
import meditrack.exception.ValidationException;
import meditrack.model.Appointment;
import meditrack.model.Doctor;
import meditrack.model.Patient;
import meditrack.repository.AppointmentRepository;
import meditrack.repository.DoctorRepository;
import meditrack.repository.PatientRepository;
import meditrack.service.AppointmentService;
import meditrack.service.EmailService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import meditrack.enums.AppointmentStatus;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class AppointmentServiceImpl implements AppointmentService {

    private static final Logger logger = LoggerFactory.getLogger(AppointmentServiceImpl.class);
    private static final Random random = new Random();

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private ModelMapper modelMapper;

    private String generateAppointmentId() {
        String id;
        do {
            id = "APP-" + String.format("%04d", random.nextInt(1000));
        } while (appointmentRepository.existsByAppointmentId(id));
        return id;
    }

    @Override
    public AppointmentDTO createAppointment(AppointmentDTO appointmentDTO) {
        logger.info("Creating appointment for patientId: {}", appointmentDTO.getPatientId());

        Patient patient = patientRepository.findByPatientId(appointmentDTO.getPatientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with id: " + appointmentDTO.getPatientId()));

        Doctor doctor = doctorRepository.findByDoctorId(appointmentDTO.getDoctorId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with id: " + appointmentDTO.getDoctorId()));

        LocalDateTime startTime = appointmentDTO.getAppointmentDateTime();
        LocalDateTime endTime = startTime.plusMinutes(appointmentDTO.getDuration());

        List<Appointment> conflicts = appointmentRepository
                .findByDateTimeBetweenAndDoctorId(startTime, endTime, doctor.getDoctorId());
        if (!conflicts.isEmpty()) {
            throw new ValidationException("Doctor has a conflicting appointment at this time.");
        }

        Appointment appointment = modelMapper.map(appointmentDTO, Appointment.class);
        appointment.setAppointmentId(generateAppointmentId());
        appointment.setPatientId(patient.getPatientId());
        appointment.setDoctorId(doctor.getDoctorId());
        appointment.setPatientName(patient.getFullName());
        appointment.setPatientEmail(patient.getEmail());
        appointment.setDoctorName(doctor.getFullName());
        appointment.setStatus(AppointmentStatus.valueOf("PENDING"));
        appointment.setCreatedAt(LocalDateTime.now());
        appointment.setUpdatedAt(LocalDateTime.now());

        Appointment saved = appointmentRepository.save(appointment);
        logger.info("Appointment created with ID: {}", saved.getAppointmentId());

        try {
            if (patient.getEmail() != null && !patient.getEmail().isBlank()) {
                String formattedDate = startTime.format(DateTimeFormatter.ofPattern("dd MMM yyyy"));
                String formattedTime = startTime.format(DateTimeFormatter.ofPattern("hh:mm a"));

                emailService.sendAppointmentBooked(
                        patient.getEmail(),
                        patient.getFullName(),
                        doctor.getFullName(),
                        formattedDate,
                        formattedTime
                );
            }
        } catch (Exception e) {
            logger.error("Error sending appointment email: {}", e.getMessage());
        }

        return convertToDTO(saved);
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
        Appointment existing = appointmentRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with id: " + appointmentId));

        modelMapper.map(appointmentDTO, existing);
        existing.setUpdatedAt(LocalDateTime.now());
        Appointment updated = appointmentRepository.save(existing);
        return convertToDTO(updated);
    }

//    @Override
//    public void deleteAppointment(String appointmentId) {
//        Appointment appointment = appointmentRepository.findByAppointmentId(appointmentId)
//                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with id: " + appointmentId));
//        appointmentRepository.delete(appointment);
//    }


    @Override
    public void cancelAppointment(String appointmentId) {
        Appointment appointment = appointmentRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with id: " + appointmentId));

        appointment.setStatus(AppointmentStatus.CANCELLED);

        appointmentRepository.save(appointment); // 🔁 Save the updated status
    }




    @Override
    public List<AppointmentDTO> getUpcomingAppointmentsByPatient(String patientId) {
        if (!patientRepository.existsByPatientId(patientId)) {
            throw new ResourceNotFoundException("Patient not found with id: " + patientId);
        }

        return appointmentRepository.findByPatientIdAndAppointmentDateTimeAfter(patientId, LocalDateTime.now()).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<AppointmentDTO> getUpcomingAppointmentsByDoctor(String doctorId) {
        if (!doctorRepository.existsByDoctorId(doctorId)) {
            throw new ResourceNotFoundException("Doctor not found with id: " + doctorId);
        }

        return appointmentRepository.findByDoctorIdAndAppointmentDateTimeAfter(doctorId, LocalDateTime.now()).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<AppointmentDTO> getAppointmentHistoryByDoctor(String doctorId) {
        if (!doctorRepository.existsByDoctorId(doctorId)) {
            throw new ResourceNotFoundException("Doctor not found with id: " + doctorId);
        }

        return appointmentRepository.findByDoctorIdAndStatus(doctorId, "COMPLETED").stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public AppointmentDTO rescheduleAppointment(String appointmentId, LocalDateTime newDateTime) {
        Appointment appointment = appointmentRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with id: " + appointmentId));

        if (newDateTime.isBefore(LocalDateTime.now())) {
            throw new ValidationException("New appointment time must be in the future.");
        }

        LocalDateTime endTime = newDateTime.plusMinutes(appointment.getDuration());
        List<Appointment> conflicts = appointmentRepository
                .findByDateTimeBetweenAndDoctorId(newDateTime, endTime, appointment.getDoctorId());

        boolean hasConflict = conflicts.stream().anyMatch(a -> !a.getAppointmentId().equals(appointmentId));
        if (hasConflict) {
            throw new ValidationException("Doctor has a conflicting appointment at this time.");
        }

        appointment.setAppointmentDateTime(newDateTime);
        appointment.setStatus(AppointmentStatus.valueOf("RESCHEDULED"));
        appointment.setUpdatedAt(LocalDateTime.now());

        return convertToDTO(appointmentRepository.save(appointment));
    }

    @Override
    public StatsDTO getAppointmentStats() {
        StatsDTO stats = new StatsDTO();
        stats.setTotalAppointments(appointmentRepository.count());
//        stats.setEmergencyCases(appointmentRepository.countByIsEmergency(true));
        stats.setConfirmedAppointments(appointmentRepository.countByStatus("CONFIRMED"));
        stats.setPendingAppointments(appointmentRepository.countByStatus("PENDING"));
        stats.setCompletedAppointments(appointmentRepository.countByStatus("COMPLETED"));
        return stats;
    }
    @Override
    public Appointment confirmAppointment(String appointmentId) {
        Appointment appointment = appointmentRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with ID: " + appointmentId));

        appointment.setStatus(AppointmentStatus.valueOf("CONFIRMED"));

        appointmentRepository.save(appointment);

        // Send confirmation email
        String email = appointment.getPatientEmail();
        String patientName = appointment.getPatientName();
        String doctorName = appointment.getDoctorName();

        String date = appointment.getAppointmentDateTime().toLocalDate().toString();
        String time = appointment.getAppointmentDateTime().toLocalTime().toString();

        emailService.sendAppointmentConfirmation(email, patientName, doctorName, date, time);
        return appointment;
    }


    // @Override
//    public List<AppointmentDTO> getEmergencyAppointments() {
//        return appointmentRepository.findByIsEmergency(true).stream()
//                .map(this::convertToDTO)
//                .collect(Collectors.toList());
//    }

    private AppointmentDTO convertToDTO(Appointment appointment) {
        return modelMapper.map(appointment, AppointmentDTO.class);
    }




}
