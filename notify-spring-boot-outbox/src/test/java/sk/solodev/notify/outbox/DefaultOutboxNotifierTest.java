package sk.solodev.notify.outbox;

import org.junit.jupiter.api.Test;
import sk.solodev.notify.NotificationRequest;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultOutboxNotifierTest {

    record SampleRequest(String to, String body) implements NotificationRequest { }

    private static final OutboxProperties PROPERTIES = new OutboxProperties(
            Duration.ofSeconds(5), 100, 7, Duration.ofSeconds(10), Duration.ofMinutes(10),
            "notification_outbox");

    private final RecordingOutboxStore store = new RecordingOutboxStore();

    private final DefaultOutboxNotifier notifier =
            new DefaultOutboxNotifier(store, JsonMapper.builder().build(), PROPERTIES,
                    new NoOpOutboxTracePropagator());

    @Test
    void persistsThePendingEntryAndReturnsItsId() {
        var id = notifier.enqueue(new SampleRequest("+421900123456", "hello"));

        assertThat(store.inserted).singleElement().satisfies(entry -> {
            assertThat(entry.id()).isEqualTo(id);
            assertThat(entry.status()).isEqualTo(OutboxStatus.PENDING);
            assertThat(entry.attempts()).isZero();
            assertThat(entry.messageId()).isNull();
            assertThat(entry.lastError()).isNull();
            assertThat(entry.sentAt()).isNull();
        });
    }

    @Test
    void recordsTheConcreteRequestTypeSoTheRelayCanRebuildIt() {
        notifier.enqueue(new SampleRequest("+421900123456", "hello"));

        assertThat(store.inserted.getFirst().requestType())
                .isEqualTo(SampleRequest.class.getName());
    }

    @Test
    void serializesTheRequestToJson() {
        notifier.enqueue(new SampleRequest("+421900123456", "hello"));

        assertThat(store.inserted.getFirst().payload())
                .contains("\"to\":\"+421900123456\"")
                .contains("\"body\":\"hello\"");
    }

    @Test
    void stampsMaxAttemptsFromConfigurationSoLaterChangesDoNotAffectPendingEntries() {
        notifier.enqueue(new SampleRequest("+421900123456", "hello"));

        assertThat(store.inserted.getFirst().maxAttempts()).isEqualTo(7);
    }

    @Test
    void makesTheEntryImmediatelyDueForDelivery() {
        var before = Instant.now();

        notifier.enqueue(new SampleRequest("+421900123456", "hello"));

        var entry = store.inserted.getFirst();
        assertThat(entry.nextAttemptAt()).isBetween(before, Instant.now());
        assertThat(entry.createdAt()).isBetween(before, Instant.now());
    }

    @Test
    void eachEnqueueGetsItsOwnId() {
        var first = notifier.enqueue(new SampleRequest("+421900123456", "one"));
        var second = notifier.enqueue(new SampleRequest("+421900123456", "two"));

        assertThat(first).isNotEqualTo(second);
        assertThat(store.inserted).hasSize(2);
    }

    @Test
    void storesTheTraceContextCapturedAtEnqueue() {
        var propagator = new OutboxTracePropagator() {

            @Override
            public Optional<String> capture() {
                return Optional.of("00-0af7651916cd43dd8448eb211c80319c-b7ad6b7169203331-01");
            }

            @Override
            public <T> T withRestoredContext(String traceContext, Supplier<T> delivery) {
                return delivery.get();
            }
        };
        var tracing = new DefaultOutboxNotifier(store, JsonMapper.builder().build(), PROPERTIES,
                propagator);

        tracing.enqueue(new SampleRequest("+421900123456", "hello"));

        assertThat(store.inserted.getFirst().traceContext())
                .isEqualTo("00-0af7651916cd43dd8448eb211c80319c-b7ad6b7169203331-01");
    }
}
