package meditrack.dto;


import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AppointmentDTO {
    private String id;
    private String patientId;
    private String doctorId;
    private String patientName;
    private String doctorName;
    private String department;
    private LocalDateTime appointmentDateTime;
    private Integer duration;
    private String reason;
    private String symptoms;
    private String additionalNotes;
    private boolean isEmergency;
    private String status;
}
