package sk.solodev.notify.sms;

import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class SmsRequestTest {

    @Test
    void builderSetsFieldsAndAttributes() {
        var request = SmsRequest.builder().to("+43111").from("+43000").message("hello")
                .attribute("mediaUrl", "http://img")
                .build();

        assertThat(request.to()).isEqualTo("+43111");
        assertThat(request.from()).isEqualTo("+43000");
        assertThat(request.message()).isEqualTo("hello");
        assertThat(request.attributes()).containsEntry("mediaUrl", "http://img");
    }

    @Test
    void buildRejectsMissingMandatoryField() {
        assertThat(catchThrowable(() -> SmsRequest.builder().to("+43111").build()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void buildRejectsBlankMandatoryField() {
        assertThat(catchThrowable(() -> SmsRequest.builder().to(" ").from("+43000").message("hi").build()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void defensivelyCopiesAttributes() {
        var mutable = new HashMap<String, Object>();
        mutable.put("mediaUrl", "http://img");
        var request = new SmsRequest("+43111", "+43000", "hi", mutable);

        mutable.put("sneak", "in");

        assertThat(request.attributes()).containsOnlyKeys("mediaUrl");
    }

}
