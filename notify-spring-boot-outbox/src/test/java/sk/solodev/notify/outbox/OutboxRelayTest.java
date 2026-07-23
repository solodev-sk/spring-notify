package sk.solodev.notify.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import sk.solodev.notify.NotificationDeliveryException;
import sk.solodev.notify.NotificationRequest;
import sk.solodev.notify.Notifier;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxRelayTest {

    record SampleRequest(String to) implements NotificationRequest {}

    static final OutboxProperties PROPS =
            new OutboxProperties(true, Duration.ofSeconds(5), 100, 3, Duration.ofSeconds(10), Duration.ofMinutes(10), "notification_outbox");
    static final ObjectMapper MAPPER = new ObjectMapper();

    /** Store with one claimable entry; records the terminal transition. */
    static class OneEntryStore implements OutboxStore {
        OutboxEntry entry;
        String outcome;   // "sent" | "retry" | "failed"
        Instant nextAttemptAt;
        OneEntryStore(int attempts) throws Exception {
            var now = Instant.now();
            entry = new OutboxEntry(UUID.randomUUID(), SampleRequest.class.getName(),
                    MAPPER.writeValueAsString(new SampleRequest("+421900")),
                    OutboxStatus.PENDING, attempts, 3, null, null, now, now, null);
        }
        public void insert(OutboxEntry e) {}
        public List<OutboxEntry> claimBatch(int batchSize, Instant now) {
            var e = entry; entry = null; return e == null ? List.of() : List.of(e);
        }
        public void markSent(UUID id, String messageId, Instant sentAt) { outcome = "sent"; }
        public void markForRetry(UUID id, String lastError, Instant next) { outcome = "retry"; nextAttemptAt = next; }
        public void markFailed(UUID id, String lastError) { outcome = "failed"; }
    }

    @Test
    void deliversPendingEntryAndMarksSent() throws Exception {
        var store = new OneEntryStore(0);
        var captured = new AtomicReference<NotificationRequest>();
        Notifier notifier = new Notifier() {
            public String notify(NotificationRequest r) { captured.set(r); return "MID-1"; }
        };

        new OutboxRelay(notifier, store, MAPPER, PROPS).poll();

        assertThat(store.outcome).isEqualTo("sent");
        assertThat(captured.get()).isInstanceOf(SampleRequest.class);
        assertThat(((SampleRequest) captured.get()).to()).isEqualTo("+421900");
    }

    @Test
    void failedDeliveryWithAttemptsRemainingSchedulesRetryWithBackoff() throws Exception {
        var store = new OneEntryStore(0); // attempts=0, maxAttempts=3
        Notifier notifier = new Notifier() {
            public String notify(NotificationRequest r) {
                throw new NotificationDeliveryException("boom", r, new RuntimeException());
            }
        };

        var before = Instant.now();
        new OutboxRelay(notifier, store, MAPPER, PROPS).poll();

        assertThat(store.outcome).isEqualTo("retry");
        // initialBackoff is 10s; next attempt must be at least ~10s out
        assertThat(store.nextAttemptAt).isAfter(before.plusSeconds(9));
    }

    @Test
    void failedDeliveryOnLastAttemptMarksFailed() throws Exception {
        var store = new OneEntryStore(2); // attempts=2, one away from maxAttempts=3
        Notifier notifier = new Notifier() {
            public String notify(NotificationRequest r) {
                throw new NotificationDeliveryException("boom", r, new RuntimeException());
            }
        };

        new OutboxRelay(notifier, store, MAPPER, PROPS).poll();

        assertThat(store.outcome).isEqualTo("failed");
    }
}