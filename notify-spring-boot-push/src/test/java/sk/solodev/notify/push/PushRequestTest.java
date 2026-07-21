package sk.solodev.notify.push;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class PushRequestTest {

    @Test
    void builderSetsFieldsAndAttributes() {
        var request = PushRequest.builder().to("tok").title("Shipped").body("On its way")
                .attribute("badge", 1)
                .build();

        assertThat(request.to()).isEqualTo("tok");
        assertThat(request.title()).isEqualTo("Shipped");
        assertThat(request.body()).isEqualTo("On its way");
        assertThat(request.attributes()).containsEntry("badge", 1);
    }

    @Test
    void buildRejectsMissingMandatoryField() {
        assertThat(catchThrowable(() -> PushRequest.builder().to("tok").build()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void buildRejectsBlankMandatoryField() {
        assertThat(catchThrowable(() -> PushRequest.builder().to("tok").title(" ").body("b").build()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
