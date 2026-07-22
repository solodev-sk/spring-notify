package sk.solodev.notify.push;

import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;
import sk.solodev.notify.NotificationRequest;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Canonical push request. {@code to} is the device token; {@code title}/{@code body}
 * are the notification content. {@code collapseKey} groups notifications that supersede one
 * another (only the latest is shown), {@code priority} hints how urgently to deliver, and
 * {@code ttl} is how long the provider may keep trying to deliver before discarding it.
 * Provider-specific options (data payload, sound, badge, topic, …) travel in {@code attributes}.
 * Nullness is a compile-time contract (jspecify); the constructor additionally rejects blank
 * {@code to}, {@code title}, and {@code body}.
 *
 * @author Dominik Kovács
 * @since 1.0.0
 */
public record PushRequest(String to, String title, String body,
                          @Nullable String collapseKey, Priority priority, @Nullable Duration ttl,
                          Map<String, Object> attributes) implements NotificationRequest {

    public PushRequest {
        Assert.hasText(to, "to must be set");
        Assert.hasText(title, "title must be set");
        Assert.hasText(body, "body must be set");
        Assert.notNull(priority, "priority must be set");
        attributes = Map.copyOf(attributes);
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Delivery urgency, mapped to each provider's two-level scheme. */
    public enum Priority {
        NORMAL, HIGH
    }

    public static final class Builder {

        private String to;

        private String title;

        private String body;

        private @Nullable String collapseKey;

        private Priority priority = Priority.NORMAL;

        private @Nullable Duration ttl;

        private final Map<String, Object> attributes = new HashMap<>();

        private Builder() {

        }

        public Builder to(String to) {
            this.to = to;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder body(String body) {
            this.body = body;
            return this;
        }

        public Builder collapseKey(String collapseKey) {
            this.collapseKey = collapseKey;
            return this;
        }

        public Builder priority(Priority priority) {
            this.priority = priority;
            return this;
        }

        public Builder ttl(Duration ttl) {
            this.ttl = ttl;
            return this;
        }

        public Builder attribute(String key, Object value) {
            attributes.put(key, value);
            return this;
        }

        public PushRequest build() {
            return new PushRequest(to, title, body, collapseKey, priority, ttl, attributes);
        }
    }
}
