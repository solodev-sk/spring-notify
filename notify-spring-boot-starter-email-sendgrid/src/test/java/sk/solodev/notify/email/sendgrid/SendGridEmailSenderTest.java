package sk.solodev.notify.email.sendgrid;

import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import sk.solodev.notify.email.Attachment;
import sk.solodev.notify.email.EmailAddress;
import sk.solodev.notify.email.EmailRequest;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SendGridEmailSenderTest {

    private final SendGrid sendGrid = mock(SendGrid.class);
    private final SendGridEmailSender sender = new SendGridEmailSender(sendGrid);

    @Test
    void mapsRequestToMailAndReturnsMessageId() throws Exception {
        when(sendGrid.api(any(Request.class)))
                .thenReturn(new Response(202, "", Map.of("X-Message-Id", "sg-123")));

        var id = sender.send(EmailRequest.builder()
                .to(new EmailAddress("Alice", "alice@b.com"))
                .cc("cc@b.com")
                .from("no-reply@x.com").replyTo("reply@x.com")
                .subject("Shipped").body("text").htmlBody("<p>html</p>")
                .attachments(Attachment.builder()
                        .filename("invoice.pdf").content(new byte[]{1, 2, 3}).contentType("application/pdf")
                        .build())
                .header("X-Campaign", "spring")
                .build());

        assertThat(id).isEqualTo("sg-123");

        var captor = ArgumentCaptor.forClass(Request.class);
        verify(sendGrid).api(captor.capture());
        var req = captor.getValue();
        assertThat(req.getEndpoint()).isEqualTo("mail/send");
        assertThat(req.getBody())
                .contains("alice@b.com").contains("cc@b.com").contains("no-reply@x.com")
                .contains("Shipped").contains("text/plain").contains("text/html")
                .contains("invoice.pdf");
    }

    @Test
    void throwsOnNon2xxResponse() throws Exception {
        when(sendGrid.api(any(Request.class)))
                .thenReturn(new Response(401, "unauthorized", Map.of()));

        assertThatThrownBy(() -> sender.send(EmailRequest.builder()
                .to("a@b.com").from("x@x.com").subject("s").body("b").build()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("401");
    }
}
