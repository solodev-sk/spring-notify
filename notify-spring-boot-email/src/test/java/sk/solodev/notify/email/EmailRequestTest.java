package sk.solodev.notify.email;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

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
                .attachment(Attachment.builder()
                        .filename("invoice.pdf").content(new byte[]{1, 2, 3}).contentType("application/pdf")
                        .build())
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
    void varargsAndCollectionMethodsAddAllListEntries() {
        var att1 = Attachment.builder().filename("a.pdf").content(new byte[]{1}).contentType("application/pdf").build();
        var att2 = Attachment.builder().filename("b.pdf").content(new byte[]{2}).contentType("application/pdf").build();

        var request = EmailRequest.builder()
                .from("shop@x.com").subject("s").body("b")
                .to("a@b.com", "b@b.com")                                   // String varargs
                .cc(List.of(EmailAddress.of("c@b.com")))                    // Collection
                .bcc(EmailAddress.of("d@b.com"), EmailAddress.of("e@b.com")) // EmailAddress varargs
                .attachments(att1, att2)
                .headers(Map.of("X-A", "1", "X-B", "2"))
                .attributes(Map.of("priority", "high"))
                .build();

        assertThat(request.to()).containsExactly(EmailAddress.of("a@b.com"), EmailAddress.of("b@b.com"));
        assertThat(request.cc()).containsExactly(EmailAddress.of("c@b.com"));
        assertThat(request.bcc()).containsExactly(EmailAddress.of("d@b.com"), EmailAddress.of("e@b.com"));
        assertThat(request.attachments()).containsExactly(att1, att2);
        assertThat(request.headers()).containsEntry("X-A", "1").containsEntry("X-B", "2");
        assertThat(request.attributes()).containsEntry("priority", "high");
    }

    @Test
    void buildRejectsMissingFrom() {
        assertThat(catchThrowable(() -> EmailRequest.builder().to("a@b.com").subject("s").body("b").build()))
                .isInstanceOf(IllegalArgumentException.class);
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
