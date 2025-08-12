package meditrack.model;

import meditrack.enums.AppointmentStatus;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.UUID;

@Document(collection = "appointments")
public class Appointment {

    @Id
    private String id;

    // References to Patient and Doctor by ID
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

    public Appointment() {

    }

    public Appointment(String id, String patientId, String doctorId, String appointmentId, String patientName, int age, String phoneNumber,
                       String doctorName, String department, String patientEmail, LocalDateTime appointmentDateTime,
                       Integer duration, String reason, String symptoms, String additionalNotes, AppointmentStatus status, // ✅ fixed
                       LocalDateTime createdAt, LocalDateTime updatedAt) {
        this();
        this.id = id;
        this.patientId = patientId;
        this.doctorId = doctorId;
        this.appointmentId=appointmentId;
        this.patientName = patientName;
        this.age = age;
        this.phoneNumber = phoneNumber;
        this.doctorName = doctorName;
        this.department = department;
        this.patientEmail = patientEmail;
        this.appointmentDateTime = appointmentDateTime;
        this.duration = duration;
        this.reason = reason;
        this.symptoms = symptoms;
        this.additionalNotes = additionalNotes;
        this.status = status; // ✅ assign enum
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }





    public String getAppointmentId() {
        return appointmentId;
    }

    public void setAppointmentId(String appointmentId) {
        this.appointmentId = appointmentId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public String getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(String doctorId) {
        this.doctorId = doctorId;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getPatientEmail() {
        return patientEmail;
    }

    public void setPatientEmail(String patientEmail) {
        this.patientEmail = patientEmail;
    }

    public LocalDateTime getAppointmentDateTime() {
        return appointmentDateTime;
    }

    public void setAppointmentDateTime(LocalDateTime appointmentDateTime) {
        this.appointmentDateTime = appointmentDateTime;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getSymptoms() {
        return symptoms;
    }

    public void setSymptoms(String symptoms) {
        this.symptoms = symptoms;
    }

    public String getAdditionalNotes() {
        return additionalNotes;
    }

    public void setAdditionalNotes(String additionalNotes) {
        this.additionalNotes = additionalNotes;
    }

    public AppointmentStatus getStatus() {
        return status;
    }

    public void setStatus(AppointmentStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
