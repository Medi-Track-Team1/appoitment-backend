package meditrack.service.impl;

import meditrack.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.InternetAddress;

@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailServiceImpl.class);

    @Autowired
    private JavaMailSender mailSender;
    
    @Value("${spring.mail.username}")
    private String senderEmail;

   @Override
public void sendEmail(String toEmail, String subject, String body) {
    try {
        // 🔍 CRITICAL DEBUG: Log the exact content being sent
        logger.info("=== EMAIL CONTENT DEBUG ===");
        logger.info("To: {}", toEmail);
        logger.info("Subject: {}", subject);
        logger.info("Body starts with: {}", body.substring(0, Math.min(100, body.length())));
        logger.info("Body contains 'FOLLOW-UP APPOINTMENT DETAILS': {}", body.contains("FOLLOW-UP APPOINTMENT DETAILS"));
        logger.info("Body contains emojis: {}", body.contains("📅") || body.contains("⏰"));
        logger.info("Body length: {} characters", body.length());
        logger.info("===========================");
        
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        
        helper.setFrom(new InternetAddress(senderEmail, "MediTrack Health Center"));
        helper.setTo(toEmail);
        helper.setSubject(subject);
        helper.setText(body, false); // false = plain text
        
        message.setHeader("X-Priority", "3");
        message.setHeader("X-Mailer", "MediTrack-v1.0");
        message.setHeader("Return-Path", senderEmail);
        
        mailSender.send(message);
        
        logger.info("✅ Email sent successfully to: {} with {} characters", toEmail, body.length());

    } catch (Exception e) {
        logger.error("❌ Failed to send email: {}", e.getMessage(), e);
    }
}
    @Override
    public void sendAppointmentBooked(String to, String patientName, String doctorName, String date, String time) {
        String subject = "📅 Appointment Booking Acknowledgement – MediTrack";
        String body = buildAppointmentBookedBody(patientName, doctorName, date, time);
        sendEmail(to, subject, body);
    }

    @Override
    public void sendAppointmentConfirmation(String to, String patientName, String doctorName, String date, String time) {
        String subject = "✅ Appointment Confirmation – MediTrack";
        String body = buildAppointmentConfirmationBody(patientName, doctorName, date, time);
        sendEmail(to, subject, body);
    }

    @Override
    public void sendAppointmentCancellation(String to, String patientName, String doctorName, String date, String time, String reason) {
        String subject = "❌ Appointment Cancellation Notice – MediTrack";
        String body = buildCancellationBody(patientName, doctorName, date, time, reason);
        sendEmail(to, subject, body);
    }

    @Override
    public void sendAppointmentRescheduled(String to, String patientName, String doctorName, String newDate, String newTime) {
        String subject = "🔄 Appointment Rescheduled Successfully – MediTrack";
        String body = buildRescheduleBody(patientName, doctorName, newDate, newTime);
        sendEmail(to, subject, body);
    }

    @Override
    public void sendAppointmentRevisit(String to, String patientName, String doctorName, String date, String time, String reason) {
        String subject = "🔄 Follow-up Appointment Scheduled – MediTrack";
        String body = buildRevisitBody(patientName, doctorName, date, time, reason);
        
        // ✅ Add specific logging for revisit emails
        logger.info("🔄 Sending REVISIT email to: {} | Patient: {} | Doctor: {} | Date: {} | Time: {} | Reason: {}", 
                   to, patientName, doctorName, date, time, reason);
        
        sendEmail(to, subject, body);
        
        // ✅ Log after sending attempt
        logger.info("📤 REVISIT email sending completed for: {}", to);
    }

    @Override
    public void sendAppointmentCompletion(String to, String patientName, String doctorName) {
        String subject = "✅ Appointment Completed - MediTrack";
        String body = buildCompletionBody(patientName, doctorName);
        sendEmail(to, subject, body);
    }

    // ✅ Improved email body builders with better formatting
    private String buildAppointmentBookedBody(String patientName, String doctorName, String date, String time) {
        return String.format(
            "Dear %s,\n\n" +
            "Thank you for booking an appointment with %s through the MediTrack platform.\n\n" +
            "This is to acknowledge that we have received your appointment request.\n\n" +
            "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
            "APPOINTMENT REQUEST DETAILS\n" +
            "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
            "📅 Date: %s\n" +
            "⏰ Time: %s\n" +
            "👨‍⚕️ Doctor: %s\n" +
            "🏥 Location: MediTrack Health Center\n\n" +
            "⚠️ IMPORTANT NOTES:\n" +
            "• This is NOT a confirmation. You will receive a separate email once the appointment is confirmed by the doctor.\n" +
            "• You will be notified if your appointment is rescheduled or canceled.\n\n" +
            "If you have any questions or need to make changes, feel free to contact us at 8610260854 or reply to this email.\n\n" +
            "Thank you for choosing MediTrack. We look forward to serving you.\n\n" +
            "Warm Regards,\n" +
            "MediTrack Team\n" +
            "MediTrack Health Center\n" +
            "📞 Phone: 8610260854\n" +
            "📧 Email: meditrackhealthinfo@gmail.com",
            patientName, doctorName, date, time, doctorName
        );
    }

    private String buildRevisitBody(String patientName, String doctorName, String date, String time, String reason) {
        return String.format(
            "Dear %s,\n\n" +
            "A follow-up appointment has been scheduled for you with %s based on your previous consultation.\n\n" +
            "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
            "FOLLOW-UP APPOINTMENT DETAILS\n" +
            "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
            "📅 Date: %s\n" +
            "⏰ Time: %s\n" +
            "👨‍⚕️ Doctor: %s\n" +
            "🏥 Location: MediTrack Health Center\n" +
            "📋 Purpose: %s\n\n" +
            "🔍 THIS FOLLOW-UP IS IMPORTANT FOR:\n" +
            "• Monitoring your progress since the last visit\n" +
            "• Reviewing test results or treatment effectiveness\n" +
            "• Adjusting treatment plan if necessary\n" +
            "• Addressing any new concerns or symptoms\n\n" +
            "📝 PLEASE BRING:\n" +
            "• Any test reports or medical documents from your previous visit\n" +
            "• Current medications you are taking\n" +
            "• A list of any new symptoms or concerns\n" +
            "• Your previous prescription (if applicable)\n\n" +
            "⏰ Please arrive at least 15 minutes early for registration.\n\n" +
            "If you need to reschedule or cancel this follow-up appointment, please contact us at 8610260854 or reply to this email at least 24 hours in advance.\n\n" +
            "We look forward to continuing your healthcare journey and ensuring your well-being.\n\n" +
            "Warm Regards,\n" +
            "MediTrack Team\n" +
            "MediTrack Health Center\n" +
            "📞 Phone: 8610260854\n" +
            "📧 Email: meditrackhealthinfo@gmail.com",
            patientName, doctorName, date, time, doctorName, reason
        );
    }

    private String buildAppointmentConfirmationBody(String patientName, String doctorName, String date, String time) {
        return String.format(
            "Dear %s,\n\n" +
            "This is to confirm your upcoming appointment at MediTrack.\n\n" +
            "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
            "CONFIRMED APPOINTMENT DETAILS\n" +
            "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
            "📅 Date: %s\n" +
            "⏰ Time: %s\n" +
            "👨‍⚕️ Doctor: %s\n" +
            "🏥 Location: MediTrack Health Center\n\n" +
            "Please arrive at least 15 minutes early for registration and bring any relevant medical records or test reports with you.\n\n" +
            "If you need to reschedule or cancel your appointment, you may contact us at 8610260854 or reply to this email at least 24 hours in advance.\n\n" +
            "Thank you for choosing MediTrack. We look forward to assisting you with your healthcare needs.\n\n" +
            "Warm Regards,\n" +
            "MediTrack Team\n" +
            "📞 Phone: 8610260854\n" +
            "📧 Email: meditrackhealthinfo@gmail.com",
            patientName, date, time, doctorName
        );
    }

    private String buildCancellationBody(String patientName, String doctorName, String date, String time, String reason) {
        return String.format(
            "Dear %s,\n\n" +
            "We regret to inform you that your upcoming appointment with %s, originally scheduled for %s at %s, has been cancelled due to the following reason:\n\n" +
            "❌ CANCELLATION REASON: %s\n\n" +
            "We sincerely apologize for any inconvenience this may cause and truly appreciate your understanding.\n\n" +
            "Our team would be happy to assist you in rescheduling the appointment at your convenience. Please contact us at 8610260854 or reply to this email to choose a new date and time.\n\n" +
            "Thank you for your patience and continued trust in MediTrack. We remain committed to your health and well-being.\n\n" +
            "Warm Regards,\n" +
            "MediTrack Team\n" +
            "📞 Phone: 8610260854\n" +
            "📧 Email: meditrackhealthinfo@gmail.com",
            patientName, doctorName, date, time, reason
        );
    }

    private String buildRescheduleBody(String patientName, String doctorName, String newDate, String newTime) {
        return String.format(
            "Dear %s,\n\n" +
            "Your appointment with %s has been successfully rescheduled.\n\n" +
            "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
            "UPDATED APPOINTMENT DETAILS\n" +
            "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
            "📅 Date: %s\n" +
            "⏰ Time: %s\n" +
            "👨‍⚕️ Doctor: %s\n" +
            "🏥 Location: MediTrack Health Center\n\n" +
            "Please arrive at least 15 minutes early for registration and carry any relevant medical records or documents.\n\n" +
            "If you have any questions or need to make further changes, please contact us at 8610260854 or reply to this email.\n\n" +
            "Thank you for using MediTrack.\n\n" +
            "Warm Regards,\n" +
            "MediTrack Team\n" +
            "📞 Phone: 8610260854\n" +
            "📧 Email: meditrackhealthinfo@gmail.com",
            patientName, doctorName, newDate, newTime, doctorName
        );
    }

    private String buildCompletionBody(String patientName, String doctorName) {
        return String.format(
            "Dear %s,\n\n" +
            "We hope your appointment with %s was productive and helpful.\n\n" +
            "Your appointment has been marked as completed in our system. Here are some next steps:\n" +
            "• Follow any instructions provided by your doctor\n" +
            "• Schedule follow-up appointments if needed\n" +
            "• Contact us if you have any questions\n\n" +
            "If you need to access your medical records or prescription details, please log in to your MediTrack account.\n\n" +
            "Thank you for choosing MediTrack for your healthcare needs.\n\n" +
            "Warm Regards,\n" +
            "MediTrack Team\n" +
            "📞 Phone: 8610260854\n" +
            "📧 Email: meditrackhealthinfo@gmail.com",
            patientName, doctorName
        );
    }
}
