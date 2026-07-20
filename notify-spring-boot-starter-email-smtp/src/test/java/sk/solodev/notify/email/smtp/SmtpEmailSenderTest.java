package sk.solodev.notify.email.smtp;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import sk.solodev.notify.email.EmailRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SmtpEmailSenderTest {

    private final JavaMailSender mailSender = mock(JavaMailSender.class);
    private final SmtpEmailSender sender = new SmtpEmailSender(mailSender);

    @Test
    void mapsRequestToSimpleMailMessageAndReturnsGeneratedId() {
        var id = sender.send(EmailRequest.builder()
                .to("a@b.com").from("no-reply@x.com").subject("Shipped").body("On its way")
                .build());

        var captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        var sent = captor.getValue();
        assertThat(sent.getTo()).containsExactly("a@b.com");
        assertThat(sent.getFrom()).isEqualTo("no-reply@x.com");
        assertThat(sent.getSubject()).isEqualTo("Shipped");
        assertThat(sent.getText()).isEqualTo("On its way");
        assertThat(id).isNotBlank();
    }

    @Test
    void propagatesMailSenderFailures() {
        doThrow(new org.springframework.mail.MailSendException("smtp down"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        assertThatThrownBy(() -> sender.send(EmailRequest.builder()
                .to("a@b.com").from("x@x.com").subject("s").body("b").build()))
                .isInstanceOf(org.springframework.mail.MailSendException.class);
    }
}
