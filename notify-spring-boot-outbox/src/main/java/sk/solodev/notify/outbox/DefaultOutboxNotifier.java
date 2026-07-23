package sk.solodev.notify.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import sk.solodev.notify.NotificationRequest;

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

    private final ObjectMapper objectMapper;

    private final OutboxProperties properties;

    public DefaultOutboxNotifier(OutboxStore store, ObjectMapper objectMapper, OutboxProperties properties) {
        this.store = store;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Override
    public UUID enqueue(NotificationRequest request) {
        var id = UUID.randomUUID();
        var now = Instant.now();
        var entry = new OutboxEntry(id, request.getClass().getName(), serialize(request),
                OutboxStatus.PENDING, 0, properties.maxAttempts(), null, null, now, now, null);
        store.insert(entry);
        return id;
    }

    private String serialize(NotificationRequest request) {
        try {
            return objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException(
                    "Cannot serialize " + request.getClass().getName() + " for the outbox", ex);
        }
    }
}