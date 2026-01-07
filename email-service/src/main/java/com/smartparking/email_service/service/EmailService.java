package com.smartparking.email_service.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.regex.Pattern;

@Service
public class EmailService {
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$"
    );
    
    private final JavaMailSender mailSender;
    private final String fromEmail;

    public EmailService(JavaMailSender mailSender,
                       @Value("${spring.mail.username}") String fromEmail) {
        this.mailSender = mailSender;
        this.fromEmail = fromEmail;
    }

    public void sendVerificationEmail(String toEmail, String verificationUrl) {
        // Validate Gmail configuration
        if (fromEmail == null || fromEmail.isBlank()) {
            String errorMsg = "Gmail username (GMAIL_USERNAME) is not configured. " +
                    "Please set GMAIL_USERNAME environment variable in your .env file or docker-compose.yml";
            log.error("================================================");
            log.error("CONFIGURATION ERROR: {}", errorMsg);
            log.error("Email cannot be sent to: {}", toEmail);
            log.error("================================================");
            throw new IllegalStateException(errorMsg);
        }
        
        // Validate email address
        if (toEmail == null || toEmail.isBlank()) {
            log.error("Attempted to send email to null or blank address");
            throw new IllegalArgumentException("Email address cannot be null or empty");
        }
        
        if (!isValidEmail(toEmail)) {
            log.error("Attempted to send email to invalid address: {}", toEmail);
            throw new IllegalArgumentException("Invalid email address format: " + toEmail);
        }
        
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Potwierdź rejestrację w ParkFlow");
            
            String htmlContent = buildVerificationEmailHtml(verificationUrl);
            helper.setText(htmlContent, true);

            log.debug("Attempting to send verification email from {} to {}", fromEmail, toEmail);
            mailSender.send(message);
            log.info("Verification email sent successfully to: {}", toEmail);
        } catch (jakarta.mail.AuthenticationFailedException e) {
            String errorMsg = String.format(
                    "Gmail authentication failed. Please check your GMAIL_USERNAME and GMAIL_APP_PASSWORD. " +
                    "Error: %s. " +
                    "Make sure you are using Gmail App Password (not regular password). " +
                    "See https://support.google.com/accounts/answer/185833 for instructions.",
                    e.getMessage()
            );
            log.error("================================================");
            log.error("GMAIL AUTHENTICATION FAILED");
            log.error("Failed to send verification email to: {}", toEmail);
            log.error("Error details: {}", errorMsg);
            log.error("================================================");
            throw new RuntimeException(errorMsg, e);
        } catch (MessagingException e) {
            String errorMsg = String.format("Failed to send verification email to %s: %s", toEmail, e.getMessage());
            log.error("Failed to send verification email to: {}", toEmail, e);
            throw new RuntimeException(errorMsg, e);
        }
    }
    
    public void sendContactFormEmail(String userEmail, String userName, String subject, String message) {
        // Validate Gmail configuration
        if (fromEmail == null || fromEmail.isBlank()) {
            String errorMsg = "Gmail username (GMAIL_USERNAME) is not configured. " +
                    "Please set GMAIL_USERNAME environment variable in your .env file or docker-compose.yml";
            log.error("================================================");
            log.error("CONFIGURATION ERROR: {}", errorMsg);
            log.error("Contact form email cannot be sent from: {}", userEmail);
            log.error("================================================");
            throw new IllegalStateException(errorMsg);
        }
        
        // Validate email address
        if (userEmail == null || userEmail.isBlank()) {
            log.error("Attempted to send contact form email from null or blank address");
            throw new IllegalArgumentException("Email address cannot be null or empty");
        }
        
        if (!isValidEmail(userEmail)) {
            log.error("Attempted to send contact form email from invalid address: {}", userEmail);
            throw new IllegalArgumentException("Invalid email address format: " + userEmail);
        }
        
        try {
            MimeMessage emailMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(emailMessage, true, "UTF-8");

            // Send to ourselves (the configured Gmail account)
            helper.setFrom(fromEmail);
            helper.setTo(fromEmail);
            helper.setReplyTo(userEmail);
            helper.setSubject("Contact Form: " + (subject != null && !subject.isBlank() ? subject : "No Subject"));
            
            String htmlContent = buildContactFormEmailHtml(userEmail, userName, subject, message);
            helper.setText(htmlContent, true);

            log.debug("Attempting to send contact form email from {} to {}", userEmail, fromEmail);
            mailSender.send(emailMessage);
            log.info("Contact form email sent successfully from: {} to: {}", userEmail, fromEmail);
        } catch (jakarta.mail.AuthenticationFailedException e) {
            String errorMsg = String.format(
                    "Gmail authentication failed. Please check your GMAIL_USERNAME and GMAIL_APP_PASSWORD. " +
                    "Error: %s. " +
                    "Make sure you are using Gmail App Password (not regular password). " +
                    "See https://support.google.com/accounts/answer/185833 for instructions.",
                    e.getMessage()
            );
            log.error("================================================");
            log.error("GMAIL AUTHENTICATION FAILED");
            log.error("Failed to send contact form email from: {}", userEmail);
            log.error("Error details: {}", errorMsg);
            log.error("================================================");
            throw new RuntimeException(errorMsg, e);
        } catch (MessagingException e) {
            String errorMsg = String.format("Failed to send contact form email from %s: %s", userEmail, e.getMessage());
            log.error("Failed to send contact form email from: {}", userEmail, e);
            throw new RuntimeException(errorMsg, e);
        }
    }
    
    private boolean isValidEmail(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    private String buildVerificationEmailHtml(String verificationUrl) {
        String htmlTemplate = """
            <!DOCTYPE html>
            <html lang="pl">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    body { 
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif; 
                        line-height: 1.6; 
                        color: #333; 
                        margin: 0; 
                        padding: 0; 
                        background-color: #f5f5f5;
                    }
                    .email-wrapper { 
                        max-width: 600px; 
                        margin: 0 auto; 
                        background-color: #ffffff;
                    }
                    .header { 
                        background: linear-gradient(135deg, #6B46C1 0%%, #8B5CF6 100%%); 
                        color: white; 
                        padding: 40px 20px; 
                        text-align: center; 
                    }
                    .header h1 { 
                        margin: 0; 
                        font-size: 32px; 
                        font-weight: 700;
                        letter-spacing: -0.5px;
                    }
                    .content { 
                        padding: 40px 30px; 
                        background-color: #ffffff;
                    }
                    .content h2 {
                        color: #1F2937;
                        font-size: 24px;
                        margin-top: 0;
                        margin-bottom: 20px;
                    }
                    .content p {
                        color: #4B5563;
                        font-size: 16px;
                        margin-bottom: 16px;
                    }
                    .button-container {
                        text-align: center;
                        margin: 30px 0;
                    }
                    .button { 
                        display: inline-block; 
                        padding: 16px 32px; 
                        background: linear-gradient(135deg, #6B46C1 0%%, #8B5CF6 100%%); 
                        color: white; 
                        text-decoration: none; 
                        border-radius: 8px; 
                        font-weight: 600;
                        font-size: 16px;
                        box-shadow: 0 4px 6px rgba(107, 70, 193, 0.3);
                        transition: transform 0.2s;
                    }
                    .button:hover {
                        transform: translateY(-2px);
                        box-shadow: 0 6px 12px rgba(107, 70, 193, 0.4);
                    }
                    .link-fallback {
                        background-color: #F3F4F6;
                        padding: 15px;
                        border-radius: 6px;
                        margin: 20px 0;
                        word-break: break-all;
                        font-size: 14px;
                        color: #6B7280;
                        border-left: 4px solid #6B46C1;
                    }
                    .warning-box {
                        background-color: #FEF3C7;
                        border-left: 4px solid #F59E0B;
                        padding: 15px;
                        margin: 20px 0;
                        border-radius: 6px;
                    }
                    .warning-box strong {
                        color: #92400E;
                    }
                    .footer { 
                        text-align: center; 
                        padding: 30px 20px; 
                        background-color: #F9FAFB;
                        color: #6B7280; 
                        font-size: 14px;
                        border-top: 1px solid #E5E7EB;
                    }
                    .footer p {
                        margin: 8px 0;
                        color: #6B7280;
                    }
                    .divider {
                        height: 1px;
                        background-color: #E5E7EB;
                        margin: 30px 0;
                    }
                </style>
            </head>
            <body>
                <div class="email-wrapper">
                    <div class="header">
                        <h1>🚗 ParkFlow</h1>
                    </div>
                    <div class="content">
                        <h2>Witamy w ParkFlow!</h2>
                        <p>Dziękujemy za rejestrację w naszym systemie inteligentnego parkowania. Jesteśmy podekscytowani, że do nas dołączyłeś!</p>
                        
                        <p>Aby rozpocząć korzystanie z ParkFlow, musisz aktywować swoje konto. Kliknij poniższy przycisk, aby potwierdzić rejestrację:</p>
                        
                        <div class="button-container">
                            <a href="%s" class="button">✅ Aktywuj moje konto</a>
                        </div>
                        
                        <div class="divider"></div>
                        
                        <p><strong>Alternatywnie</strong>, możesz skopiować i wkleić poniższy link do przeglądarki:</p>
                        <div class="link-fallback">%s</div>
                        
                        <div class="warning-box">
                            <p><strong>⚠️ Ważne:</strong> Link weryfikacyjny jest ważny przez <strong>24 godziny</strong>. Po tym czasie będziesz musiał zarejestrować się ponownie.</p>
                        </div>
                        
                        <p>Po aktywacji konta będziesz mógł:</p>
                        <ul style="color: #4B5563; line-height: 1.8;">
                            <li>Znajdować dostępne miejsca parkingowe w czasie rzeczywistym</li>
                            <li>Rezerwować miejsca parkingowe z wyprzedzeniem</li>
                            <li>Płacić za parking wygodnie przez aplikację</li>
                            <li>Śledzić historię swoich rezerwacji</li>
                        </ul>
                    </div>
                    <div class="footer">
                        <p><strong>ParkFlow</strong> - Inteligentny system parkowania</p>
                        <p>Jeśli nie rejestrowałeś się w ParkFlow, możesz bezpiecznie zignorować tę wiadomość.</p>
                        <p style="margin-top: 20px; font-size: 12px; color: #9CA3AF;">
                            Ta wiadomość została wysłana automatycznie. Prosimy nie odpowiadać na ten email.
                        </p>
                    </div>
                </div>
            </body>
            </html>
            """;
        return String.format(htmlTemplate, verificationUrl, verificationUrl);
    }
    
    private String buildContactFormEmailHtml(String userEmail, String userName, String subject, String message) {
        String displayName = (userName != null && !userName.isBlank()) ? userName : "User";
        String displaySubject = (subject != null && !subject.isBlank()) ? subject : "No Subject";
        String displayMessage = (message != null && !message.isBlank()) ? message : "No message provided";
        
        String htmlTemplate = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    body { 
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif; 
                        line-height: 1.6; 
                        color: #333; 
                        margin: 0; 
                        padding: 0; 
                        background-color: #f5f5f5;
                    }
                    .email-wrapper { 
                        max-width: 600px; 
                        margin: 0 auto; 
                        background-color: #ffffff;
                    }
                    .header { 
                        background: linear-gradient(135deg, #6B46C1 0%%, #8B5CF6 100%%); 
                        color: white; 
                        padding: 40px 20px; 
                        text-align: center; 
                    }
                    .header h1 { 
                        margin: 0; 
                        font-size: 28px; 
                        font-weight: 700;
                        letter-spacing: -0.5px;
                    }
                    .content { 
                        padding: 40px 30px; 
                        background-color: #ffffff;
                    }
                    .content h2 {
                        color: #1F2937;
                        font-size: 22px;
                        margin-top: 0;
                        margin-bottom: 20px;
                    }
                    .info-box {
                        background-color: #F3F4F6;
                        padding: 20px;
                        border-radius: 8px;
                        margin: 20px 0;
                        border-left: 4px solid #6B46C1;
                    }
                    .info-box p {
                        margin: 8px 0;
                        color: #4B5563;
                        font-size: 15px;
                    }
                    .info-box strong {
                        color: #1F2937;
                        display: inline-block;
                        min-width: 100px;
                    }
                    .message-box {
                        background-color: #F9FAFB;
                        padding: 20px;
                        border-radius: 8px;
                        margin: 20px 0;
                        border: 1px solid #E5E7EB;
                    }
                    .message-box p {
                        color: #1F2937;
                        font-size: 15px;
                        line-height: 1.8;
                        white-space: pre-wrap;
                        margin: 0;
                    }
                    .footer { 
                        text-align: center; 
                        padding: 30px 20px; 
                        background-color: #F9FAFB;
                        color: #6B7280; 
                        font-size: 14px;
                        border-top: 1px solid #E5E7EB;
                    }
                    .footer p {
                        margin: 5px 0;
                    }
                </style>
            </head>
            <body>
                <div class="email-wrapper">
                    <div class="header">
                        <h1>📧 New Contact Form Submission</h1>
                    </div>
                    <div class="content">
                        <h2>You received a message from ParkFlow</h2>
                        <div class="info-box">
                            <p><strong>From:</strong> %s</p>
                            <p><strong>Name:</strong> %s</p>
                            <p><strong>Email:</strong> %s</p>
                            <p><strong>Subject:</strong> %s</p>
                        </div>
                        <h3 style="color: #1F2937; font-size: 18px; margin-top: 30px; margin-bottom: 10px;">Message:</h3>
                        <div class="message-box">
                            <p>%s</p>
                        </div>
                    </div>
                    <div class="footer">
                        <p><strong>ParkFlow</strong></p>
                        <p>This email was sent from the ParkFlow contact form.</p>
                        <p>You can reply directly to this email to respond to the user.</p>
                    </div>
                </div>
            </body>
            </html>
            """;
        return String.format(htmlTemplate, displayName, displayName, userEmail, displaySubject, displayMessage);
    }

    public void sendTopUpConfirmationEmail(String email, Long amountMinor, java.math.BigDecimal newBalance) {
        if (fromEmail == null || fromEmail.isBlank()) {
            log.warn("Gmail not configured, skipping top-up confirmation email to: {}", email);
            return;
        }
        
        if (!isValidEmail(email)) {
            log.error("Invalid email address for top-up confirmation: {}", email);
            return;
        }
        
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(email);
            helper.setSubject("Top-Up Confirmation - ParkFlow");
            
            double amount = amountMinor / 100.0;
            double balance = newBalance.doubleValue() / 100.0;
            String htmlContent = buildTopUpConfirmationHtml(amount, balance);
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
            log.info("Top-up confirmation email sent to: {}", email);
        } catch (Exception e) {
            log.error("Failed to send top-up confirmation email to: {}", email, e);
            throw new RuntimeException("Failed to send top-up confirmation email", e);
        }
    }

    public void sendReservationConfirmationEmail(String email, String parkingName, Long spotId, 
                                                 java.time.Instant startTime, java.time.Instant endTime) {
        if (fromEmail == null || fromEmail.isBlank()) {
            log.warn("Gmail not configured, skipping reservation confirmation email to: {}", email);
            return;
        }
        
        if (!isValidEmail(email)) {
            log.error("Invalid email address for reservation confirmation: {}", email);
            return;
        }
        
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(email);
            helper.setSubject("Parking Reservation Confirmed - ParkFlow");
            
            String htmlContent = buildReservationConfirmationHtml(parkingName, spotId, startTime, endTime);
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
            log.info("Reservation confirmation email sent to: {}", email);
        } catch (Exception e) {
            log.error("Failed to send reservation confirmation email to: {}", email, e);
            throw new RuntimeException("Failed to send reservation confirmation email", e);
        }
    }

    private String buildTopUpConfirmationHtml(double amount, double balance) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"></head>
            <body style="font-family: Arial, sans-serif; padding: 20px;">
                <h2 style="color: #6B46C1;">Top-Up Confirmation</h2>
                <p>Your wallet has been successfully topped up.</p>
                <p><strong>Amount:</strong> $%.2f</p>
                <p><strong>New Balance:</strong> $%.2f</p>
                <p>Thank you for using ParkFlow!</p>
            </body>
            </html>
            """, amount, balance);
    }

    private String buildReservationConfirmationHtml(String parkingName, Long spotId, 
                                                    java.time.Instant startTime, java.time.Instant endTime) {
        java.time.ZoneId zoneId = java.time.ZoneId.systemDefault();
        String startStr = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(zoneId).format(startTime);
        String endStr = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(zoneId).format(endTime);
        
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"></head>
            <body style="font-family: Arial, sans-serif; padding: 20px;">
                <h2 style="color: #6B46C1;">Parking Reservation Confirmed</h2>
                <p>Your parking reservation has been confirmed.</p>
                <p><strong>Parking:</strong> %s</p>
                <p><strong>Spot ID:</strong> %d</p>
                <p><strong>Start Time:</strong> %s</p>
                <p><strong>End Time:</strong> %s</p>
                <p>Please arrive on time. Payment will be charged when you enter the parking.</p>
                <p>Thank you for using ParkFlow!</p>
            </body>
            </html>
            """, parkingName, spotId, startStr, endStr);
    }

    public void sendParkingPaymentConfirmationEmail(String email, java.time.Instant entryTime, 
                                                    java.time.Instant exitTime, double amount,
                                                    long durationHours, long durationMinutes) {
        if (fromEmail == null || fromEmail.isBlank()) {
            log.warn("Gmail not configured, skipping parking payment confirmation email to: {}", email);
            return;
        }
        
        if (!isValidEmail(email)) {
            log.error("Invalid email address for parking payment confirmation: {}", email);
            return;
        }
        
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(email);
            helper.setSubject("Parking Payment Confirmation - ParkFlow");
            
            String htmlContent = buildParkingPaymentConfirmationHtml(entryTime, exitTime, amount, durationHours, durationMinutes);
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
            log.info("Parking payment confirmation email sent to: {}", email);
        } catch (Exception e) {
            log.error("Failed to send parking payment confirmation email to: {}", email, e);
            throw new RuntimeException("Failed to send parking payment confirmation email", e);
        }
    }

    private String buildParkingPaymentConfirmationHtml(java.time.Instant entryTime, java.time.Instant exitTime,
                                                      double amount, long durationHours, long durationMinutes) {
        java.time.ZoneId zoneId = java.time.ZoneId.systemDefault();
        String entryStr = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(zoneId).format(entryTime);
        String exitStr = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(zoneId).format(exitTime);
        
        String durationStr = durationHours > 0 ? 
            String.format("%d hour(s) %d minute(s)", durationHours, durationMinutes) :
            String.format("%d minute(s)", durationMinutes);
        
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"></head>
            <body style="font-family: Arial, sans-serif; padding: 20px;">
                <h2 style="color: #6B46C1;">Parking Payment Confirmation</h2>
                <p>Your parking session has been completed and payment processed.</p>
                <p><strong>Entry Time:</strong> %s</p>
                <p><strong>Exit Time:</strong> %s</p>
                <p><strong>Duration:</strong> %s</p>
                <p><strong>Amount Charged:</strong> $%.2f</p>
                <p>Thank you for using ParkFlow!</p>
            </body>
            </html>
            """, entryStr, exitStr, durationStr, amount);
    }
}