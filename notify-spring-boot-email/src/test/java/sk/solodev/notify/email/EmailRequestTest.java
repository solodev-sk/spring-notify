package sk.solodev.notify.email;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class EmailRequestTest {

    @Test
    void builderSetsMandatoryFieldsAndDefaultsCollections() {
        var request = EmailRequest.builder()
                .to("a@b.com").from("no-reply@x.com").subject("Shipped").body("On its way")
                .build();

        assertThat(request.to()).containsExactly(EmailAddress.of("a@b.com"));
        assertThat(request.from()).isEqualTo(EmailAddress.of("no-reply@x.com"));
        assertThat(request.subject()).isEqualTo("Shipped");
        assertThat(request.body()).isEqualTo("On its way");
        assertThat(request.htmlBody()).isNull();
        assertThat(request.cc()).isEmpty();
        assertThat(request.bcc()).isEmpty();
        assertThat(request.attachments()).isEmpty();
        assertThat(request.headers()).isEmpty();
        assertThat(request.attributes()).isEmpty();
    }

    @Test
    void builderCapturesAllFields() {
        var request = EmailRequest.builder()
                .to("a@b.com").to(new EmailAddress("Bob", "bob@b.com"))
                .cc("c@b.com").bcc("bcc@b.com")
                .from(new EmailAddress("Shop", "shop@x.com")).replyTo("reply@x.com")
                .subject("s").body("text").htmlBody("<p>html</p>")
                .attachment("invoice.pdf", new byte[]{1, 2, 3}, "application/pdf")
                .header("X-Campaign", "spring")
                .attribute("priority", "high")
                .build();

        assertThat(request.to()).containsExactly(EmailAddress.of("a@b.com"), new EmailAddress("Bob", "bob@b.com"));
        assertThat(request.cc()).containsExactly(EmailAddress.of("c@b.com"));
        assertThat(request.bcc()).containsExactly(EmailAddress.of("bcc@b.com"));
        assertThat(request.from()).isEqualTo(new EmailAddress("Shop", "shop@x.com"));
        assertThat(request.replyTo()).isEqualTo(EmailAddress.of("reply@x.com"));
        assertThat(request.htmlBody()).isEqualTo("<p>html</p>");
        assertThat(request.attachments()).singleElement()
                .satisfies(a -> assertThat(a.filename()).isEqualTo("invoice.pdf"));
        assertThat(request.headers()).containsEntry("X-Campaign", "spring");
        assertThat(request.attributes()).containsEntry("priority", "high");
    }

    @Test
    void buildRejectsMissingFrom() {
        assertThat(catchThrowable(() -> EmailRequest.builder().to("a@b.com").subject("s").body("b").build()))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void buildRejectsNoRecipients() {
        assertThat(catchThrowable(() -> EmailRequest.builder().from("x@x.com").subject("s").body("b").build()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void buildRejectsBlankSubjectOrBody() {
        assertThat(catchThrowable(() -> EmailRequest.builder().to("a@b.com").from("x@x.com").subject(" ").body("b").build()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(catchThrowable(() -> EmailRequest.builder().to("a@b.com").from("x@x.com").subject("s").body(" ").build()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
