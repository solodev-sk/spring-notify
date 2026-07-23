package sk.solodev.notify.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import sk.solodev.notify.NotificationRequest;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultOutboxNotifierTest {

    record SampleRequest(String to, String body) implements NotificationRequest {}

    static class RecordingStore implements OutboxStore {
        final List<OutboxEntry> inserted = new ArrayList<>();
        public void insert(OutboxEntry entry) { inserted.add(entry); }
        public List<OutboxEntry> claimBatch(int batchSize, Instant now) { return List.of(); }
        public void markSent(UUID id, String messageId, Instant sentAt) {}
        public void markForRetry(UUID id, String lastError, Instant nextAttemptAt) {}
        public void markFailed(UUID id, String lastError) {}
    }

    private final RecordingStore store = new RecordingStore();
    private final OutboxProperties properties =
            new OutboxProperties(true, Duration.ofSeconds(5), 100, 5, Duration.ofSeconds(10), Duration.ofMinutes(10), "notification_outbox");
    private final DefaultOutboxNotifier notifier =
            new DefaultOutboxNotifier(store, new ObjectMapper(), properties);

    @Test
    void enqueuePersistsPendingEntryWithSerializedPayloadAndReturnsItsId() {
        UUID id = notifier.enqueue(new SampleRequest("+421900", "hi"));

        assertThat(store.inserted).singleElement().satisfies(e -> {
            assertThat(e.id()).isEqualTo(id);
            assertThat(e.status()).isEqualTo(OutboxStatus.PENDING);
            assertThat(e.attempts()).isZero();
            assertThat(e.maxAttempts()).isEqualTo(5);
            assertThat(e.requestType()).isEqualTo(SampleRequest.class.getName());
            assertThat(e.payload()).contains("\"to\":\"+421900\"").contains("\"body\":\"hi\"");
        });
    }
}