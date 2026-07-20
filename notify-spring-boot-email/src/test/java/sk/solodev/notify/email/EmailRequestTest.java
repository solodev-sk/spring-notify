package sk.solodev.notify.email;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class EmailRequestTest {

    @Test
    void builderSetsFieldsAndDefaultsAttributes() {
        var request = EmailRequest.builder()
                .to("a@b.com").from("no-reply@x.com").subject("Shipped").body("On its way")
                .build();

        assertThat(request.to()).isEqualTo("a@b.com");
        assertThat(request.from()).isEqualTo("no-reply@x.com");
        assertThat(request.subject()).isEqualTo("Shipped");
        assertThat(request.body()).isEqualTo("On its way");
        assertThat(request.attributes()).isEmpty();
    }

    @Test
    void builderSetsAttributes() {
        var request = EmailRequest.builder()
                .to("a@b.com").from("x@x.com").subject("s").body("b")
                .attribute("cc", "c@b.com")
                .build();

        assertThat(request.attributes()).containsEntry("cc", "c@b.com");
    }

    @Test
    void buildRejectsMissingMandatoryField() {
        assertThat(catchThrowable(() -> EmailRequest.builder().to("a@b.com").build()))
                .isInstanceOf(NullPointerException.class);
    }
}
