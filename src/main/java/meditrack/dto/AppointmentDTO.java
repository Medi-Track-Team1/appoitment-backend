package meditrack.dto;

import meditrack.enums.AppointmentStatus;

import java.time.LocalDateTime;

public class AppointmentDTO {
    private String id;
    private String patientId;
    private String doctorId;
    private String appointmentId;

    // Additional details
    private String patientName;
    private int age;
    private String phoneNumber;
    private String doctorName;
    private String department;
    private String patientEmail;

    private LocalDateTime appointmentDateTime;
    private Integer duration;
    private String reason;
    private String symptoms;
    private String additionalNotes;
    private AppointmentStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;


}
