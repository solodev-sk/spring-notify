package sk.solodev.notify.outbox;

import org.junit.jupiter.api.Test;
import sk.solodev.notify.NotificationDeliveryException;
import sk.solodev.notify.NotificationRequest;
import sk.solodev.notify.Notifier;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxRelayTest {

    record SampleRequest(String to) implements NotificationRequest { }

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private static final OutboxProperties PROPERTIES = new OutboxProperties(
            Duration.ofSeconds(5), 100, 3, Duration.ofSeconds(10), Duration.ofMinutes(10),
            "notification_outbox");

    /** Serves a fixed batch once, then nothing; records every terminal transition. */
    static class FakeStore implements OutboxStore {

        private List<OutboxEntry> pending;

        String outcome;
        String lastError;
        Instant nextAttemptAt;
        String messageId;

        FakeStore(OutboxEntry... entries) {
            this.pending = List.of(entries);
        }

        @Override
        public void insert(OutboxEntry entry) { }

        @Override
        public List<OutboxEntry> claimBatch(int batchSize, Instant now) {
            var batch = pending;
            pending = List.of();
            return batch;
        }

        @Override
        public void markSent(UUID id, String messageId, Instant sentAt) {
            this.outcome = "sent";
            this.messageId = messageId;
        }

        @Override
        public void markForRetry(UUID id, String lastError, Instant nextAttemptAt) {
            this.outcome = "retry";
            this.lastError = lastError;
            this.nextAttemptAt = nextAttemptAt;
        }

        @Override
        public void markFailed(UUID id, String lastError) {
            this.outcome = "failed";
            this.lastError = lastError;
        }
    }

    /** Records what it was asked to deliver; optionally fails. */
    static class StubNotifier implements Notifier {

        private final RuntimeException failure;

        final List<NotificationRequest> delivered = new ArrayList<>();

        StubNotifier() {
            this(null);
        }

        StubNotifier(RuntimeException failure) {
            this.failure = failure;
        }

        @Override
        public String notify(NotificationRequest request) {
            delivered.add(request);
            if (failure != null) {
                throw failure;
            }
            return "PROVIDER-MID";
        }
    }

    private static OutboxEntry entry(int attempts, String requestType, String payload) {
        var now = Instant.now();
        return new OutboxEntry(UUID.randomUUID(), requestType, payload, OutboxStatus.PENDING,
                attempts, 3, null, null, now, now, null);
    }

    private static OutboxEntry pending(int attempts) {
        return entry(attempts, SampleRequest.class.getName(),
                JSON.writeValueAsString(new SampleRequest("+421900123456")));
    }

    private static OutboxRelay relay(Notifier notifier, OutboxStore store) {
        return new OutboxRelay(notifier, store, JSON, PROPERTIES);
    }

    @Test
    void deliversThePendingEntryThroughTheNotifier() {
        var store = new FakeStore(pending(0));
        var notifier = new StubNotifier();

        relay(notifier, store).poll();

        assertThat(notifier.delivered).singleElement()
                .isInstanceOf(SampleRequest.class)
                .satisfies(request -> assertThat(((SampleRequest) request).to())
                        .isEqualTo("+421900123456"));
    }

    @Test
    void recordsTheProviderMessageIdOnSuccess() {
        var store = new FakeStore(pending(0));

        relay(new StubNotifier(), store).poll();

        assertThat(store.outcome).isEqualTo("sent");
        assertThat(store.messageId).isEqualTo("PROVIDER-MID");
    }

    @Test
    void schedulesARetryWhenAttemptsRemain() {
        var store = new FakeStore(pending(0));
        var notifier = new StubNotifier(deliveryFailure());
        var before = Instant.now();

        relay(notifier, store).poll();

        assertThat(store.outcome).isEqualTo("retry");
        assertThat(store.lastError).isNotBlank();
        // initial backoff is 10s
        assertThat(store.nextAttemptAt).isAfter(before.plusSeconds(9));
    }

    @Test
    void abandonsTheEntryOnTheFinalAttempt() {
        // attempts=2, maxAttempts=3 → this failure is the last one
        var store = new FakeStore(pending(2));

        relay(new StubNotifier(deliveryFailure()), store).poll();

        assertThat(store.outcome).isEqualTo("failed");
    }

    @Test
    void abandonsAnEntryWhoseRequestTypeNoLongerExists() {
        var store = new FakeStore(entry(0, "com.example.Removed", "{}"));
        var notifier = new StubNotifier();

        relay(notifier, store).poll();

        assertThat(store.outcome).isEqualTo("failed");
        assertThat(notifier.delivered).isEmpty();
    }

    @Test
    void abandonsAnEntryWithAMalformedPayloadInsteadOfBlockingTheBatch() {
        var store = new FakeStore(entry(0, SampleRequest.class.getName(), "not json"));
        var notifier = new StubNotifier();

        relay(notifier, store).poll();

        assertThat(store.outcome).isEqualTo("failed");
        assertThat(notifier.delivered).isEmpty();
    }

    @Test
    void deliversEveryEntryInTheBatch() {
        var store = new FakeStore(pending(0), pending(0), pending(0));
        var notifier = new StubNotifier();

        relay(notifier, store).poll();

        assertThat(notifier.delivered).hasSize(3);
    }

    @Test
    void doesNothingWhenThereIsNoWork() {
        var store = new FakeStore();
        var notifier = new StubNotifier();

        relay(notifier, store).poll();

        assertThat(notifier.delivered).isEmpty();
        assertThat(store.outcome).isNull();
    }

    @Test
    void backoffGrowsExponentiallyAndIsCapped() {
        var relay = relay(new StubNotifier(), new FakeStore());

        assertThat(relay.backoff(1)).isEqualTo(Duration.ofSeconds(10));
        assertThat(relay.backoff(2)).isEqualTo(Duration.ofSeconds(20));
        assertThat(relay.backoff(3)).isEqualTo(Duration.ofSeconds(40));
        // capped at maxBackoff (10m)
        assertThat(relay.backoff(30)).isEqualTo(Duration.ofMinutes(10));
    }

    private static NotificationDeliveryException deliveryFailure() {
        return new NotificationDeliveryException("provider rejected",
                new SampleRequest("+421900123456"), new RuntimeException("503"));
    }
}
