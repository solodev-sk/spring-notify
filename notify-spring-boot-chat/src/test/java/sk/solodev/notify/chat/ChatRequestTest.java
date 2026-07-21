package sk.solodev.notify.chat;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class ChatRequestTest {

    @Test
    void builderSetsFieldsAndAttributes() {
        var request = ChatRequest.builder().to("#alerts").message("Deploy finished")
                .attribute("blocks", "[]")
                .build();

        assertThat(request.to()).isEqualTo("#alerts");
        assertThat(request.message()).isEqualTo("Deploy finished");
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
