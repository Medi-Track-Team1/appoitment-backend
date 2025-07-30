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
import org.modelmapper.ModelMapper;  // Correct import
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AppointmentServiceImpl implements AppointmentService {

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Override
    public AppointmentDTO createAppointment(AppointmentDTO appointmentDTO) {
        // Validate patient exists
        Patient patient = patientRepository.findById(appointmentDTO.getPatientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with id: " + appointmentDTO.getPatientId()));

        // Validate doctor exists
        Doctor doctor = doctorRepository.findById(appointmentDTO.getDoctorId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with id: " + appointmentDTO.getDoctorId()));

        // Check for conflicting appointments
        LocalDateTime startTime = appointmentDTO.getAppointmentDateTime();
        LocalDateTime endTime = startTime.plusMinutes(appointmentDTO.getDuration());

        List<Appointment> conflictingAppointments = appointmentRepository
                .findByDateTimeBetweenAndDoctorId(startTime, endTime, doctor.getId());

        if (!conflictingAppointments.isEmpty()) {
            throw new ValidationException("Doctor has a conflicting appointment at this time");
        }

        Appointment appointment = modelMapper.map(appointmentDTO, Appointment.class);
        appointment.setPatient(patient);
        appointment.setDoctor(doctor);
        appointment.setStatus("PENDING");
        appointment.setCreatedAt(LocalDateTime.now());
        appointment.setUpdatedAt(LocalDateTime.now());

        Appointment savedAppointment = appointmentRepository.save(appointment);
        return convertToDTO(savedAppointment);
    }

    @Override
    public AppointmentDTO getAppointmentById(String id) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with id: " + id));
        return convertToDTO(appointment);
    }

    @Override
    public List<AppointmentDTO> getAllAppointments() {
        return appointmentRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public AppointmentDTO updateAppointment(String id, AppointmentDTO appointmentDTO) {
        Appointment existingAppointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with id: " + id));

        modelMapper.map(appointmentDTO, existingAppointment);
        existingAppointment.setUpdatedAt(LocalDateTime.now());

        Appointment updatedAppointment = appointmentRepository.save(existingAppointment);
        return convertToDTO(updatedAppointment);
    }

    @Override
    public void deleteAppointment(String id) {
        if (!appointmentRepository.existsById(id)) {
            throw new ResourceNotFoundException("Appointment not found with id: " + id);
        }
        appointmentRepository.deleteById(id);
    }

    @Override
    public List<AppointmentDTO> getUpcomingAppointmentsByPatient(String patientId) {
        if (!patientRepository.existsById(patientId)) {
            throw new ResourceNotFoundException("Patient not found with id: " + patientId);
        }

        return appointmentRepository.findByPatientIdAndAppointmentDateTimeAfter(patientId, LocalDateTime.now()).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<AppointmentDTO> getUpcomingAppointmentsByDoctor(String doctorId) {
        if (!doctorRepository.existsById(doctorId)) {
            throw new ResourceNotFoundException("Doctor not found with id: " + doctorId);
        }

        return appointmentRepository.findByDoctorIdAndAppointmentDateTimeAfter(doctorId, LocalDateTime.now()).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<AppointmentDTO> getAppointmentHistoryByDoctor(String doctorId) {
        if (!doctorRepository.existsById(doctorId)) {
            throw new ResourceNotFoundException("Doctor not found with id: " + doctorId);
        }

        return appointmentRepository.findByDoctorIdAndStatus(doctorId, "COMPLETED").stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public AppointmentDTO rescheduleAppointment(String id, LocalDateTime newDateTime) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with id: " + id));

        // Check if new time is in the future
        if (newDateTime.isBefore(LocalDateTime.now())) {
            throw new ValidationException("New appointment time must be in the future");
        }

        // Check for conflicts with doctor's schedule
        LocalDateTime endTime = newDateTime.plusMinutes(appointment.getDuration());
        List<Appointment> conflictingAppointments = appointmentRepository
                .findByDateTimeBetweenAndDoctorId(newDateTime, endTime, appointment.getDoctor().getId());

        if (!conflictingAppointments.isEmpty() &&
                (conflictingAppointments.size() > 1 || !conflictingAppointments.get(0).getId().equals(id))) {
            throw new ValidationException("Doctor has a conflicting appointment at this time");
        }

        appointment.setAppointmentDateTime(newDateTime);
        appointment.setStatus("RESCHEDULED");
        appointment.setUpdatedAt(LocalDateTime.now());

        Appointment rescheduledAppointment = appointmentRepository.save(appointment);
        return convertToDTO(rescheduledAppointment);
    }

    @Override
    public StatsDTO getAppointmentStats() {
        StatsDTO stats = new StatsDTO();
        stats.setTotalAppointments(appointmentRepository.count());
        stats.setEmergencyCases(appointmentRepository.countByIsEmergency(true));
        stats.setConfirmedAppointments(appointmentRepository.countByStatus("CONFIRMED"));
        stats.setPendingAppointments(appointmentRepository.countByStatus("PENDING"));
        stats.setCompletedAppointments(appointmentRepository.countByStatus("COMPLETED"));
        return stats;
    }

    @Override
    public List<AppointmentDTO> getEmergencyAppointments() {
        return appointmentRepository.findByIsEmergency(true).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private AppointmentDTO convertToDTO(Appointment appointment) {
        AppointmentDTO dto = modelMapper.map(appointment, AppointmentDTO.class);
        dto.setPatientId(appointment.getPatient().getId());
        dto.setDoctorId(appointment.getDoctor().getId());
        dto.setPatientName(appointment.getPatient().getFullName());
        dto.setDoctorName(appointment.getDoctor().getFullName());
        return dto;
    }
}
