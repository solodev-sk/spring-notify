package sk.solodev.notify.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sk.solodev.notify.NotificationRequest;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.UUID;

/**
 * Default {@link OutboxNotifier}: serializes the request to JSON and inserts a {@code PENDING}
 * entry via the {@link OutboxStore}, joining the caller's transaction so the row commits with
 * their business change.
 *
 * @author Dominik Kovács
 * @since 1.1.0
 */
public class DefaultOutboxNotifier implements OutboxNotifier {

    private static final Logger log = LoggerFactory.getLogger(DefaultOutboxNotifier.class);

    private final OutboxStore store;

    private final JsonMapper jsonMapper;

    private final OutboxProperties properties;

    private final OutboxTracePropagator tracePropagator;

    public DefaultOutboxNotifier(OutboxStore store, JsonMapper jsonMapper, OutboxProperties properties,
                                 OutboxTracePropagator tracePropagator) {
        this.store = store;
        this.jsonMapper = jsonMapper;
        this.properties = properties;
        this.tracePropagator = tracePropagator;
    }

    @Override
    public UUID enqueue(NotificationRequest request) {
        var id = UUID.randomUUID();
        var now = Instant.now();
        var entry = new OutboxEntry(id, request.getClass().getName(),
                jsonMapper.writeValueAsString(request), OutboxStatus.PENDING, 0,
                properties.maxAttempts(), null, null, now, now, null,
                tracePropagator.capture().orElse(null));
        store.insert(entry);
        log.debug("Enqueued {} as outbox entry {}", entry.requestType(), id);
        return id;
    }
}