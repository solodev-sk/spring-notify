package sk.solodev.notify.outbox;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Persistence SPI for outbox entries. The default {@code JdbcOutboxStore} uses the application's
 * shared {@code DataSource}; applications may supply their own implementation.
 *
 * @author Dominik Kovács
 * @since 1.1.0
 */
public interface OutboxStore {

    /** Insert a new entry. Runs in the caller's transaction so it commits with their business change. */
    void insert(OutboxEntry entry);

    /**
     * Claim up to {@code batchSize} entries that are {@code PENDING} and due
     * ({@code nextAttemptAt <= now}), locking them so concurrent relays skip them.
     *
     * @return the claimed entries, oldest first
     */
    List<OutboxEntry> claimBatch(int batchSize, Instant now);

    /** Mark an entry delivered. */
    void markSent(UUID id, String messageId, Instant sentAt);

    /** Record a failed attempt and schedule the next one. */
    void markForRetry(UUID id, String lastError, Instant nextAttemptAt);

    /** Mark an entry permanently failed after exhausting attempts. */
    void markFailed(UUID id, String lastError);
}