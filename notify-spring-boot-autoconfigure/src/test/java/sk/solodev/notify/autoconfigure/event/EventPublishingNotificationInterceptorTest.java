package sk.solodev.notify.autoconfigure.event;

import org.junit.jupiter.api.Test;
import sk.solodev.notify.NotificationEvent;
import sk.solodev.notify.NotificationFailed;
import sk.solodev.notify.NotificationInterceptor;
import sk.solodev.notify.NotificationRequest;
import sk.solodev.notify.NotificationSent;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EventPublishingNotificationInterceptorTest {

    private record SmsRequest(String to) implements NotificationRequest {
    }

    private final SmsRequest request = new SmsRequest("+43");

    private final List<NotificationEvent> events = new ArrayList<>();

    private final EventPublishingNotificationInterceptor interceptor =
            new EventPublishingNotificationInterceptor(event -> {
                if (event instanceof NotificationEvent e) {
                    events.add(e);
                }
            });

    @Test
    void publishesNotificationSentOnSuccess() {
        NotificationInterceptor.Chain chain = r -> "MID";

        var messageId = interceptor.intercept(request, chain);

        assertThat(messageId).isEqualTo("MID");
        assertThat(events).singleElement()
                .isEqualTo(new NotificationSent(request, "MID"));
    }

    @Test
    void publishesNotificationFailedAndRethrowsOnFailure() {
        var boom = new RuntimeException("provider down");
        NotificationInterceptor.Chain chain = r -> { throw boom; };

        assertThatThrownBy(() -> interceptor.intercept(request, chain)).isSameAs(boom);

        assertThat(events).singleElement()
                .isEqualTo(new NotificationFailed(request, boom));
    }
}
