package com.smartparking.email_service.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
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

            mailSender.send(message);
            log.info("Verification email sent successfully to: {}", toEmail);
        } catch (MessagingException e) {
            log.error("Failed to send verification email to: {}", toEmail, e);
            throw new RuntimeException("Failed to send verification email", e);
        }
    }
    
    private boolean isValidEmail(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    private String buildVerificationEmailHtml(String verificationUrl) {
        return """
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
                        background: linear-gradient(135deg, #6B46C1 0%, #8B5CF6 100%); 
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
                        background: linear-gradient(135deg, #6B46C1 0%, #8B5CF6 100%); 
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
            """.formatted(verificationUrl, verificationUrl);
    }
}

