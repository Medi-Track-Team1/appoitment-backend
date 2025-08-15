package meditrack.service;

public interface EmailService {
    void sendEmail(String toEmail, String subject, String body);

    void sendAppointmentBooked(String toEmail, String patientName, String doctorName, String date, String time);

    void sendAppointmentConfirmation(String toEmail, String patientName, String doctorName, String date, String time);

    void sendAppointmentCancellation(
            String to,
            String patientName,
            String doctorName,
            String date,
            String time,
            String reason
    );

    void sendAppointmentRescheduled(
            String patientEmail,
            String patientName,
            String doctorName,
            String newDate,
            String newTime
    );

    void sendAppointmentCompletion(String toEmail, String patientName, String doctorName);

    // ✅ Updated to include doctorName
    void sendAppointmentRevisit(String to, String patientName, String doctorName, String date, String time, String reason);
}
