package sk.solodev.notify.chat;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class ChatRequestTest {

    @Test
    void builderSetsFieldsAndDefaultsAttributes() {
        var request = ChatRequest.builder().to("#alerts").message("Deploy finished").build();

        assertThat(request.to()).isEqualTo("#alerts");
        assertThat(request.message()).isEqualTo("Deploy finished");
        assertThat(request.attributes()).isEmpty();
    }

    @Test
    void builderSetsAttributes() {
        var request = ChatRequest.builder().to("#alerts").message("hi")
                .attribute("blocks", "[]")
                .build();

        assertThat(request.attributes()).containsEntry("blocks", "[]");
    }

    @Test
    void buildRejectsMissingMandatoryField() {
        assertThat(catchThrowable(() -> ChatRequest.builder().to("#alerts").build()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void buildRejectsBlankMandatoryField() {
        assertThat(catchThrowable(() -> ChatRequest.builder().to("#alerts").message(" ").build()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
