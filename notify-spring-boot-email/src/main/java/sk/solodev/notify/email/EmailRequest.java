package sk.solodev.notify.email;

import sk.solodev.notify.NotificationRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Canonical email request. {@code to}/{@code from} are addresses; {@code subject}/{@code body}
 * are the plain-text content. Provider-specific options (cc, bcc, HTML, attachments, …) travel
 * in {@code attributes}. Nullness is a compile-time contract (jspecify); there is no runtime
 * validation beyond {@code attributes} immutability normalisation.
 */
public record EmailRequest(String to, String from, String subject, String body,
                           Map<String, Object> attributes) implements NotificationRequest {

    public EmailRequest {
        attributes = Map.copyOf(attributes);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private String to;

        private String from;

        private String subject;

        private String body;

        private final Map<String, Object> attributes = new HashMap<>();

        private Builder() {
        }

        public Builder to(String to) {
            this.to = to;
            return this;
        }

        public Builder from(String from) {
            this.from = from;
            return this;
        }

        public Builder subject(String subject) {
            this.subject = subject;
            return this;
        }

        public Builder body(String body) {
            this.body = body;
            return this;
        }

        public Builder attribute(String key, Object value) {
            attributes.put(key, value);
            return this;
        }

        public EmailRequest build() {
            return new EmailRequest(
                    Objects.requireNonNull(to, "to must be set"),
                    Objects.requireNonNull(from, "from must be set"),
                    Objects.requireNonNull(subject, "subject must be set"),
                    Objects.requireNonNull(body, "body must be set"),
                    attributes);
        }
    }
}
