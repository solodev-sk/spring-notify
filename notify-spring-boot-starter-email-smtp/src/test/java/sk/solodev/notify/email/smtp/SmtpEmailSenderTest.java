package sk.solodev.notify.email.smtp;

import jakarta.mail.Message;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import sk.solodev.notify.email.Attachment;
import sk.solodev.notify.email.EmailAddress;
import sk.solodev.notify.email.EmailRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

class SmtpEmailSenderTest {

    // real impl so createMimeMessage() returns a genuine MimeMessage; spy to capture/stub send()
    private final JavaMailSenderImpl mailSender = spy(new JavaMailSenderImpl());
    private final SmtpEmailSender sender = new SmtpEmailSender(mailSender);

    @Test
    void mapsAllFieldsToMimeMessageAndReturnsGeneratedId() throws Exception {
        doNothing(mailSender);

        var id = sender.send(EmailRequest.builder()
                .to(new EmailAddress("Alice", "alice@b.com"))
                .cc("cc@b.com").bcc("bcc@b.com")
                .from("no-reply@x.com").replyTo("reply@x.com")
                .subject("Shipped").body("text").htmlBody("<p>html</p>")
                .attachments(Attachment.builder()
                        .filename("invoice.pdf").content(new byte[]{1, 2, 3}).contentType("application/pdf")
                        .build())
                .header("X-Campaign", "spring")
                .build());

        var captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        MimeMessage sent = captor.getValue();
        sent.saveChanges();   // finalize MIME structure/headers (normally done on transport send)

        assertThat(sent.getRecipients(Message.RecipientType.TO)).hasSize(1)
                .allSatisfy(a -> assertThat(a.toString()).contains("Alice").contains("alice@b.com"));
        assertThat(sent.getRecipients(Message.RecipientType.CC)).extracting(Object::toString).containsExactly("cc@b.com");
        assertThat(sent.getRecipients(Message.RecipientType.BCC)).extracting(Object::toString).containsExactly("bcc@b.com");
        assertThat(sent.getReplyTo()).extracting(Object::toString).containsExactly("reply@x.com");
        assertThat(sent.getSubject()).isEqualTo("Shipped");
        assertThat(sent.getHeader("X-Campaign")).containsExactly("spring");
        assertThat(sent.getContentType()).contains("multipart");   // html + attachment → multipart
        assertThat(id).isNotBlank();
    }

    @Test
    void propagatesMailSenderFailures() {
        doThrow(new org.springframework.mail.MailSendException("smtp down"))
                .when(mailSender).send(any(MimeMessage.class));

        assertThatThrownBy(() -> sender.send(EmailRequest.builder()
                .to("a@b.com").from("x@x.com").subject("s").body("b").build()))
                .isInstanceOf(org.springframework.mail.MailSendException.class);
    }

    private static void doNothing(JavaMailSenderImpl mailSender) {
        org.mockito.Mockito.doNothing().when(mailSender).send(any(MimeMessage.class));
    }
}
