package meditrack.service.impl;

import meditrack.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String senderEmail;

    @Override
    public void sendEmail(String toEmail, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(senderEmail);
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(body);

            mailSender.send(message);
            System.out.println("✅ Email sent to: " + toEmail);

        } catch (MailException e) {
            System.err.println("❌ Failed to send email to " + toEmail + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void sendAppointmentBooked(String to, String patientName, String doctorName, String date, String time) {
        String subject = "Appointment Booking Acknowledgement – MediTrack";
        String body = "Dear " + patientName + ",\n\n"
                + "Thank you for booking an appointment with " + doctorName + " through the MediTrack platform.\n\n"
                + "This is to acknowledge that we have received your appointment request.\n\n"
                + "Appointment Request Details:\n"
                + "Date: " + date + "\n"
                + "Time: " + time + "\n"
                + "Doctor: " + doctorName + "\n"
                + "Location: MediTrack Health Center\n\n"
                + "Please note:\n"
                + "- This is not a confirmation. A separate email will be sent once the appointment is confirmed by the doctor.\n"
                + "- You will also be notified if your appointment is rescheduled or canceled.\n\n"
                + "If you have any questions or need to make changes, feel free to contact us at 8610260854 or reply to this email.\n\n"
                + "Thank you for choosing MediTrack. We look forward to serving you.\n\n"
                + "Warm Regards,\n"
                + "MediTrack Team\n"
                + "MediTrack Health Center\n"
                + "Phone: 8610260854\n"
                + "Email: meditrackhealthinfo@gmail.com";
        sendEmail(to, subject, body);
    }

    @Override
    public void sendAppointmentConfirmation(String to, String patientName, String doctorName, String date, String time) {
 String subject = "Appointment Confirmation – MediTrack";
        String body = "Dear " + patientName + ",\n\n"
                + "This is to confirm your upcoming appointment at MediTrack.\n\n"
                +"Appointment Details:\n"
                + "Date: " + date + "\n"
                + "Time: " + time + "\n"
                + "Doctor:" + doctorName + "\n"
                + "Location: MediTrack Health Center\n\n"
                + "Please arrive at least 15 minutes early for registration and bring any relevant medical records or test reports with you.\n\n"
                +"If you need to reschedule or cancel your appointment, you may contact us at 8610260854 or reply to this email at least [notice period, e.g., 24 hours] in advance.\n\n"
                +"Thank you for choosing [Hospital Name]. We look forward to assisting you with your healthcare needs.\n\n"
                + "Warm Regards,\nMediTrack Team,\nMediTrack, \nPhone :8610260854, \nEmail:meditrackhealthinfo@gmail.com";
        sendEmail(to, subject, body);
    }

    @Override
    public void sendAppointmentCancellation(
            String to,
            String patientName,
            String doctorName,
            String date,
            String time,
            String reason) {

        String subject = "Appointment Cancellation Notice – MediTrack";

        String body = "Dear " + patientName + ",\n\n"
                + "We regret to inform you that your upcoming appointment with Dr. "
                + doctorName + ", originally scheduled for " + date + " at " + time
                + ", has been cancelled due to the following reason:\n\n"
                + reason + "\n\n"
                + "We sincerely apologize for any inconvenience this may cause and truly appreciate your understanding.\n\n"
                + "Our team would be happy to assist you in rescheduling the appointment at your convenience. "
                + "Please contact us at 8610260854 or reply to this email to choose a new date and time.\n\n"
                + "Thank you for your patience and continued trust in MediTrack. We remain committed to your health and well-being.\n\n"
                + "Warm Regards,\n"
                + "MediTrack Team\n"
                + "Phone: 8610260854\n"
                + "Email: meditrackhealthinfo@gmail.com";

        sendEmail(to, subject, body);
    }

    @Override
    public void sendAppointmentReschedule(String to, String patientName, String doctorName, String newDate, String newTime) {
        String subject = "Appointment Rescheduled Successfully – MediTrack";
        String body = "Dear " + patientName + ",\n\n"
                + "You have successfully rescheduled your appointment with Dr. " + doctorName + ".\n\n"
                + "Here are your updated appointment details:\n"
                + "Date: " + newDate + "\n"
                + "Time: " + newTime + "\n"
                + "Doctor: " + doctorName + "\n"
                + "Location: MediTrack Health Center\n\n"
                + "Please arrive at least 15 minutes early for registration and carry any relevant medical records or documents.\n\n"
                + "If you have any questions or need to make further changes, please contact us at 8610260854 or reply to this email.\n\n"
                + "Thank you for using MediTrack.\n\n"
                + "Warm Regards,\n"
                + "MediTrack Team\n"
                + "Phone: 8610260854\n"
                + "Email: meditrackhealthinfo@gmail.com";
        sendEmail(to, subject, body);
    }

    @Override
    public void sendAppointmentRevisit(
            String to,
            String patientName,
            String date,
            String time,
            String reason) {

        String subject = "Appointment Revisit Confirmation – MediTrack";

        String body = "Dear " + patientName + ",\n\n"
                + "We are confirming your revisit appointment details:\n\n"
                + "Date: " + date + "\n"
                + "Time: " + time + "\n"
                + "Reason: " + reason + "\n\n"
                + "Please arrive at least 10 minutes early.\n"
                + "If you need to reschedule or cancel, contact us at 8610260854 or reply to this email.\n\n"
                + "Thank you for choosing MediTrack for your healthcare needs.\n\n"
                + "Warm Regards,\n"
                + "MediTrack Team\n"
                + "Phone: 8610260854\n"
                + "Email: meditrackhealthinfo@gmail.com";

        sendEmail(to, subject, body);
    }


}
