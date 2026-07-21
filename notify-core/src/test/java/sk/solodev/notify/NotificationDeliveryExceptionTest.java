package sk.solodev.notify;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationDeliveryExceptionTest {

    private final NotificationRequest request = new TestRequest("r1");

    @Test
    void preservesCauseAndRequest() {
        var cause = new IllegalStateException("provider down");

        var ex = new NotificationDeliveryException("failed", request, cause);

        assertThat(ex.getCause()).isSameAs(cause);
        assertThat(ex.getMessage()).isEqualTo("failed");
        assertThat(ex.request()).isSameAs(request);
    }
}
