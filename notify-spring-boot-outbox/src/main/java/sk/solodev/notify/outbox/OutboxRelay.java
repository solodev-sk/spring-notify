package sk.solodev.notify.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sk.solodev.notify.NotificationRequest;
import sk.solodev.notify.Notifier;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.time.Instant;

/**
 * Polls the {@link OutboxStore} for due {@code PENDING} entries and delivers each through the
 * application's {@link Notifier} — so interceptors, events, and observations all apply at real
 * delivery time. A failed delivery is retried with capped exponential backoff until
 * {@code maxAttempts}, after which the entry is marked {@code FAILED}.
 *
 * @author Dominik Kovács
 * @since 1.1.0
 */
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

    private final Notifier notifier;

    private final OutboxStore store;

    private final JsonMapper jsonMapper;

    private final OutboxProperties properties;

    private final OutboxTracePropagator tracePropagator;

    public OutboxRelay(Notifier notifier, OutboxStore store, JsonMapper jsonMapper,
                       OutboxProperties properties, OutboxTracePropagator tracePropagator) {
        this.notifier = notifier;
        this.store = store;
        this.jsonMapper = jsonMapper;
        this.properties = properties;
        this.tracePropagator = tracePropagator;
    }

    /**
     * Claim a batch of due entries and attempt delivery of each. Never throws: an infrastructure
     * failure (the database being unreachable, say) is logged and the next poll simply tries again,
     * so a transient outage cannot stop the relay permanently.
     */
    public void poll() {
        try {
            var batch = store.claimBatch(properties.batchSize(), Instant.now());
            for (var entry : batch) {
                deliver(entry);
            }
        } catch (RuntimeException ex) {
            log.warn("Outbox poll failed; retrying at the next interval: {}", ex.getMessage());
            log.debug("Outbox poll failure", ex);
        }
    }

    private void deliver(OutboxEntry entry) {
        NotificationRequest request;
        try {
            request = (NotificationRequest) jsonMapper.readValue(
                    entry.payload(), Class.forName(entry.requestType()));
        } catch (ClassNotFoundException | ClassCastException | JacksonException ex) {
            log.error("Outbox entry {} has an undeserializable payload of type {}; marking failed",
                    entry.id(), entry.requestType(), ex);
            store.markFailed(entry.id(), ex.getMessage());
            return;
        }

        try {
            var messageId = tracePropagator.withRestoredContext(entry.traceContext(),
                    () -> notifier.notify(request));
            store.markSent(entry.id(), messageId, Instant.now());
        } catch (RuntimeException ex) {
            var nextAttempts = entry.attempts() + 1;
            if (nextAttempts >= entry.maxAttempts()) {
                log.warn("Outbox entry {} failed on final attempt {}/{}: {}",
                        entry.id(), nextAttempts, entry.maxAttempts(), ex.getMessage());
                store.markFailed(entry.id(), ex.getMessage());
            } else {
                var next = Instant.now().plus(backoff(nextAttempts));
                log.debug("Outbox entry {} failed (attempt {}/{}), retrying at {}",
                        entry.id(), nextAttempts, entry.maxAttempts(), next);
                store.markForRetry(entry.id(), ex.getMessage(), next);
            }
        }
    }

    /** Exponential backoff (doubling from initial), capped at maxBackoff. attemptsSoFar >= 1. */
    Duration backoff(int attemptsSoFar) {
        var initialMillis = properties.initialBackoff().toMillis();
        var maxMillis = properties.maxBackoff().toMillis();
        var scaled = initialMillis * (1L << Math.min(attemptsSoFar - 1, 30));
        return Duration.ofMillis(Math.min(scaled, maxMillis));
    }
}