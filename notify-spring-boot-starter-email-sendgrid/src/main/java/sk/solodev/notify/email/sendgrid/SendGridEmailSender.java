package sk.solodev.notify.email.sendgrid;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Attachments;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import com.sendgrid.helpers.mail.objects.Personalization;
import sk.solodev.notify.email.EmailAddress;
import sk.solodev.notify.email.EmailRequest;
import sk.solodev.notify.email.EmailSender;

import java.util.Base64;

/**
 * {@link EmailSender} backed by the SendGrid v3 API. Maps the request onto a SendGrid {@code Mail}
 * (recipients, reply-to, plain + optional HTML content, base64 attachments, custom headers) and
 * returns the {@code X-Message-Id} of the accepted send.
 *
 * @author Dominik Kovács
 * @since 1.0.0
 */
public class SendGridEmailSender implements EmailSender {

    private final SendGrid sendGrid;

    public SendGridEmailSender(SendGrid sendGrid) {
        this.sendGrid = sendGrid;
    }

    @Override
    public String send(EmailRequest request) throws Exception {
        var mail = new Mail();
        mail.setFrom(email(request.from()));
        mail.setSubject(request.subject());
        if (request.replyTo() != null) {
            mail.setReplyTo(email(request.replyTo()));
        }

        var personalization = new Personalization();
        request.to().forEach(a -> personalization.addTo(email(a)));
        request.cc().forEach(a -> personalization.addCc(email(a)));
        request.bcc().forEach(a -> personalization.addBcc(email(a)));
        mail.addPersonalization(personalization);

        // SendGrid requires text/plain before text/html
        mail.addContent(new Content("text/plain", request.body()));
        if (request.htmlBody() != null) {
            mail.addContent(new Content("text/html", request.htmlBody()));
        }

        request.attachments().forEach(a -> {
            var attachment = new Attachments();
            attachment.setFilename(a.filename());
            attachment.setType(a.contentType());
            attachment.setContent(Base64.getEncoder().encodeToString(a.content()));
            attachment.setDisposition("attachment");
            mail.addAttachments(attachment);
        });

        request.headers().forEach(mail::addHeader);

        var apiRequest = new Request();
        apiRequest.setMethod(Method.POST);
        apiRequest.setEndpoint("mail/send");
        apiRequest.setBody(mail.build());

        Response response = sendGrid.api(apiRequest);
        int status = response.getStatusCode();
        if (status < 200 || status >= 300) {
            throw new RuntimeException("SendGrid rejected the request: HTTP " + status + " " + response.getBody());
        }
        var messageId = response.getHeaders().get("X-Message-Id");
        return messageId != null ? messageId : "";
    }

    private static Email email(EmailAddress address) {
        return address.name() != null
                ? new Email(address.address(), address.name())
                : new Email(address.address());
    }
}
