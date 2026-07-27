package sk.solodev.notify.outbox;

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
 * @since 1.0.1
 */
public class DefaultOutboxNotifier implements OutboxNotifier {

    private final OutboxStore store;

    private final JsonMapper jsonMapper;

    private final OutboxProperties properties;

    public DefaultOutboxNotifier(OutboxStore store, JsonMapper jsonMapper, OutboxProperties properties) {
        this.store = store;
        this.jsonMapper = jsonMapper;
        this.properties = properties;
    }

    @Override
    public UUID enqueue(NotificationRequest request) {
        var id = UUID.randomUUID();
        var now = Instant.now();
        var entry = new OutboxEntry(id, request.getClass().getName(),
                jsonMapper.writeValueAsString(request), OutboxStatus.PENDING, 0,
                properties.maxAttempts(), null, null, now, now, null);
        store.insert(entry);
        return id;
    }
}