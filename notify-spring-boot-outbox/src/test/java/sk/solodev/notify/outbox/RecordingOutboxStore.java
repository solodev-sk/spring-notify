package sk.solodev.notify.outbox;

import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * In-memory {@link OutboxStore} for tests: records inserts, serves a pre-seeded batch once, and
 * captures the terminal transition the relay applied.
 */
class RecordingOutboxStore implements OutboxStore {

    final List<OutboxEntry> inserted = new ArrayList<>();

    private List<OutboxEntry> claimable;

    /** "sent", "retry" or "failed" — whichever the relay applied last. */
    @Nullable String outcome;

    @Nullable String messageId;

    @Nullable String lastError;

    @Nullable Instant nextAttemptAt;

    RecordingOutboxStore(OutboxEntry... claimable) {
        this.claimable = List.of(claimable);
    }

    @Override
    public void insert(OutboxEntry entry) {
        inserted.add(entry);
    }

    @Override
    public List<OutboxEntry> claimBatch(int batchSize, Instant now) {
        var batch = claimable;
        claimable = List.of();
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
