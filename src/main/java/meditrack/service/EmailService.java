package meditrack.service;

public interface EmailService {
    void sendEmail(String toEmail, String subject, String body);
    void sendAppointmentBooked(String toEmail, String patientName, String doctorName, String date, String time);
    void sendAppointmentConfirmation(String toEmail, String patientName, String doctorName, String date, String time);
    void sendAppointmentCancellation(String toEmail, String patientName, String doctorName, String date, String time);
    void sendAppointmentReschedule(String toEmail, String patientName, String doctorName, String newDate, String newTime);
}
