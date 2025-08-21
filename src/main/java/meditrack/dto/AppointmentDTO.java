
package meditrack.dto;

import lombok.*;

import java.time.LocalDateTime;
@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class AppointmentDTO {

    private String id;
    private String patientId;
    private String doctorId;

    private String patientName;
    private String doctorName;
    private String department;
    private String patientEmail;
    private String appointmentId;
    private String phoneNumber;
    private LocalDateTime appointmentDateTime;
    private Integer duration;
    private String reason;
    private String symptoms;
    private String additionalNotes;
    private boolean isEmergency;
    private String status;
    private int age;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
