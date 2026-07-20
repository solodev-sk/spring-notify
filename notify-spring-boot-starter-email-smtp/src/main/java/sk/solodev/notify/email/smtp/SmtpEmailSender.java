package sk.solodev.notify.email.smtp;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import sk.solodev.notify.email.EmailRequest;
import sk.solodev.notify.email.EmailSender;

import java.util.UUID;

/**
 * {@link EmailSender} backed by Spring's {@link JavaMailSender} (SMTP). SMTP returns no
 * message id, so a generated UUID is used as the receipt id.
 */
public class SmtpEmailSender implements EmailSender {

    private final JavaMailSender mailSender;

    public SmtpEmailSender(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public String send(EmailRequest request) {
        var message = new SimpleMailMessage();
        message.setTo(request.to());
        message.setFrom(request.from());
        message.setSubject(request.subject());
        message.setText(request.body());
        mailSender.send(message);
        return UUID.randomUUID().toString();
    }
}
