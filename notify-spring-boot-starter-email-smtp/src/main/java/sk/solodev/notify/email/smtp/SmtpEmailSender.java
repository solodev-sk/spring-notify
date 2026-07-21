package sk.solodev.notify.email.smtp;

import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import sk.solodev.notify.email.EmailAddress;
import sk.solodev.notify.email.EmailRequest;
import sk.solodev.notify.email.EmailSender;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

/**
 * {@link EmailSender} backed by Spring's {@link JavaMailSender} (SMTP). Builds a MIME message
 * covering recipients (to/cc/bcc), reply-to, an optional HTML alternative, attachments, and
 * custom headers. SMTP returns no message id, so a generated UUID is used as the receipt id.
 *
 * @author Dominik Kovács
 * @since 1.0.0
 */
public class SmtpEmailSender implements EmailSender {

    private final JavaMailSender mailSender;

    public SmtpEmailSender(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public String send(EmailRequest request) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        // multipart is required for attachments and for a plain+HTML alternative body
        boolean multipart = !request.attachments().isEmpty() || request.htmlBody() != null;
        var helper = new MimeMessageHelper(message, multipart, StandardCharsets.UTF_8.name());

        helper.setFrom(address(request.from()));
        helper.setTo(addresses(request.to()));
        if (!request.cc().isEmpty()) {
            helper.setCc(addresses(request.cc()));
        }
        if (!request.bcc().isEmpty()) {
            helper.setBcc(addresses(request.bcc()));
        }
        if (request.replyTo() != null) {
            helper.setReplyTo(address(request.replyTo()));
        }
        helper.setSubject(request.subject());

        if (request.htmlBody() != null) {
            helper.setText(request.body(), request.htmlBody());   // multipart/alternative
        } else {
            helper.setText(request.body(), false);
        }

        for (var attachment : request.attachments()) {
            helper.addAttachment(attachment.filename(),
                    new ByteArrayResource(attachment.content()), attachment.contentType());
        }

        for (var header : request.headers().entrySet()) {
            message.setHeader(header.getKey(), header.getValue());
        }

        mailSender.send(message);
        return UUID.randomUUID().toString();
    }

    private static InternetAddress[] addresses(List<EmailAddress> list) throws AddressException, UnsupportedEncodingException {
        var result = new InternetAddress[list.size()];
        for (int i = 0; i < list.size(); i++) {
            result[i] = address(list.get(i));
        }
        return result;
    }

    private static InternetAddress address(EmailAddress address) throws UnsupportedEncodingException, AddressException {
        return address.name() != null
                ? new InternetAddress(address.address(), address.name(), StandardCharsets.UTF_8.name())
                : new InternetAddress(address.address());
    }
}
