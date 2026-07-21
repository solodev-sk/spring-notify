package sk.solodev.notify;

import org.junit.jupiter.api.Test;
import sk.solodev.notify.dispatch.SenderChannelAdapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SenderChannelAdapterTest {

    private record OtherRequest() implements NotificationRequest {
    }

    private final SenderChannelAdapter<TestRequest> adapter =
            new SenderChannelAdapter<>(TestRequest.class, r -> "id-" + r.id());

    @Test
    void supportsOnlyItsRequestType() {
        assertThat(adapter.supports(new TestRequest("r1"))).isTrue();
        assertThat(adapter.supports(new OtherRequest())).isFalse();
    }

    @Test
    void delegatesToSenderAndReturnsMessageId() {
        var messageId = adapter.deliver(new TestRequest("r1"));

        assertThat(messageId).isEqualTo("id-r1");
    }

    @Test
    void wrapsSenderFailureInNotificationDeliveryExceptionCarryingTheRequest() {
        var request = new TestRequest("r9");
        var cause = new RuntimeException("provider down");
        SenderChannelAdapter<TestRequest> failing =
                new SenderChannelAdapter<>(TestRequest.class, r -> { throw cause; });

        assertThatThrownBy(() -> failing.deliver(request))
                .isInstanceOf(NotificationDeliveryException.class)
                .hasCause(cause)
                .extracting(ex -> ((NotificationDeliveryException) ex).request())
                .isSameAs(request);
    }
}
