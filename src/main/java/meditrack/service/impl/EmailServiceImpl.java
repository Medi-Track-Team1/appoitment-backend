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
                + "Appointment Details:\n"
                + "Date: " + date + "\n"
                + "Time: " + time + "\n"
                + "Doctor: " + doctorName + "\n"
                + "Location: MediTrack Health Center\n\n"
                + "Please arrive at least 15 minutes early for registration and bring any relevant medical records or test reports with you.\n\n"
                + "If you need to reschedule or cancel your appointment, you may contact us at 8610260854 or reply to this email at least 24 hours in advance.\n\n"
                + "Thank you for choosing MediTrack. We look forward to assisting you with your healthcare needs.\n\n"
                + "Warm Regards,\n"
                + "MediTrack Team\n"
                + "Phone: 8610260854\n"
                + "Email: meditrackhealthinfo@gmail.com";
        sendEmail(to, subject, body);
    }

    @Override
    public void sendAppointmentCancellation(String to, String patientName, String doctorName, String date, String time, String reason) {
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
    public void sendAppointmentRescheduled(String to, String patientName, String doctorName, String newDate, String newTime) {
        String subject = "Appointment Rescheduled Successfully – MediTrack";
        String body = "Dear " + patientName + ",\n\n"
                + "Your appointment with Dr. " + doctorName + " has been successfully rescheduled.\n\n"
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
    public void sendAppointmentRevisit(String to, String patientName, String doctorName, String date, String time, String reason) {
        String subject = "Appointment Revisit Scheduled";
        String body = String.format(
                "Dear %s,\n\nYour revisit appointment with Dr. %s is scheduled for %s at %s.\nReason: %s\n\nRegards,\nMediTrack Team",
                patientName, doctorName, date, time, reason
        );
        sendEmail(to, subject, body);
    }


    @Override
    public void sendAppointmentCompletion(String to, String patientName, String doctorName) {
        String subject = "Appointment Completed - MediTrack";
        String body = "Dear " + patientName + ",\n\n"
                + "We hope your appointment with Dr. " + doctorName + " was productive and helpful.\n\n"
                + "Your appointment has been marked as completed in our system. Here are some next steps:\n"
                + "- Follow any instructions provided by your doctor\n"
                + "- Schedule follow-up appointments if needed\n"
                + "- Contact us if you have any questions\n\n"
                + "If you need to access your medical records or prescription details, please log in to your MediTrack account.\n\n"
                + "Thank you for choosing MediTrack for your healthcare needs.\n\n"
                + "Warm Regards,\n"
                + "MediTrack Team\n"
                + "Phone: 8610260854\n"
                + "Email: meditrackhealthinfo@gmail.com";

        sendEmail(to, subject, body);
    }}