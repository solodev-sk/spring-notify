package sk.solodev.notify.outbox;

import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.UUID;

/**
 * One persisted notification awaiting or completing durable delivery — mirrors a row of the
 * {@code notification_outbox} table. The {@code traceContext} is the serialized trace context
 * captured at enqueue time; null when tracing is absent.
 *
 * @author Dominik Kovács
 * @since 1.1.0
 */
public record OutboxEntry(UUID id, String requestType, String payload, OutboxStatus status,
                          int attempts, int maxAttempts, @Nullable String messageId,
                          @Nullable String lastError, Instant createdAt, Instant nextAttemptAt,
                          @Nullable Instant sentAt, @Nullable String traceContext) {
}