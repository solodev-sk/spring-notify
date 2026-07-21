package sk.solodev.notify.autoconfigure.observation;

import io.micrometer.observation.tck.TestObservationRegistry;
import org.junit.jupiter.api.Test;
import sk.solodev.notify.NotificationInterceptor;
import sk.solodev.notify.NotificationRequest;

import static io.micrometer.observation.tck.TestObservationRegistryAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ObservationNotificationInterceptorTest {

    private record SmsRequest(String to) implements NotificationRequest {
    }

    private final TestObservationRegistry registry = TestObservationRegistry.create();

    private final ObservationNotificationInterceptor interceptor =
            new ObservationNotificationInterceptor(registry, new DefaultNotificationObservationConvention());

    @Test
    void recordsAnObservationTaggedWithTheChannel() {
        NotificationInterceptor.Chain chain = r -> "MID";

        var result = interceptor.intercept(new SmsRequest("+43"), chain);

        assertThat(result).isEqualTo("MID");
        assertThat(registry)
                .hasObservationWithNameEqualTo("spring.notify.send")
                .that()
                .hasLowCardinalityKeyValue("notify.channel", "sms")
                .hasBeenStarted()
                .hasBeenStopped();
    }

    @Test
    void recordsErrorWhenDeliveryFails() {
        var boom = new RuntimeException("provider down");
        NotificationInterceptor.Chain chain = r -> { throw boom; };

        assertThatThrownBy(() -> interceptor.intercept(new SmsRequest("+43"), chain)).isSameAs(boom);

        assertThat(registry)
                .hasObservationWithNameEqualTo("spring.notify.send")
                .that()
                .hasError();
    }
}
