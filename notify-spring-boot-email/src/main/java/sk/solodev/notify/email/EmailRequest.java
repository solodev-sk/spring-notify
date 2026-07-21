package sk.solodev.notify.email;

import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;
import sk.solodev.notify.NotificationRequest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Canonical email request covering the standard RFC 5322 fields: multiple {@code to}/{@code cc}/
 * {@code bcc} recipients, {@code from}/{@code replyTo}, subject, a plain-text {@code body} and an
 * optional {@code htmlBody} (both present → {@code multipart/alternative}), file {@code attachments},
 * and custom {@code headers}. Anything still provider-specific travels in {@code attributes}.
 * Nullness is a compile-time contract (jspecify); collections are defensively copied. {@code build()}
 * requires at least one {@code to} recipient and rejects blank {@code subject} and {@code body}.
 *
 * @author Dominik Kovács
 * @since 1.0.0
 */
public record EmailRequest(List<EmailAddress> to, List<EmailAddress> cc, List<EmailAddress> bcc,
                           EmailAddress from, @Nullable EmailAddress replyTo,
                           String subject, String body, @Nullable String htmlBody,
                           List<Attachment> attachments, Map<String, String> headers,
                           Map<String, Object> attributes) implements NotificationRequest {

    public EmailRequest {
        to = List.copyOf(to);
        cc = List.copyOf(cc);
        bcc = List.copyOf(bcc);
        attachments = List.copyOf(attachments);
        headers = Map.copyOf(headers);
        attributes = Map.copyOf(attributes);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private final List<EmailAddress> to = new ArrayList<>();

        private final List<EmailAddress> cc = new ArrayList<>();

        private final List<EmailAddress> bcc = new ArrayList<>();

        private EmailAddress from;

        private @Nullable EmailAddress replyTo;

        private String subject;

        private String body;

        private @Nullable String htmlBody;

        private final List<Attachment> attachments = new ArrayList<>();

        private final Map<String, String> headers = new HashMap<>();

        private final Map<String, Object> attributes = new HashMap<>();

        private Builder() {

        }

        public Builder to(String address) {
            this.to.add(EmailAddress.of(address));
            return this;
        }

        public Builder to(EmailAddress address) {
            this.to.add(address);
            return this;
        }

        public Builder cc(String address) {
            this.cc.add(EmailAddress.of(address));
            return this;
        }

        public Builder cc(EmailAddress address) {
            this.cc.add(address);
            return this;
        }

        public Builder bcc(String address) {
            this.bcc.add(EmailAddress.of(address));
            return this;
        }

        public Builder bcc(EmailAddress address) {
            this.bcc.add(address);
            return this;
        }

        public Builder from(String address) {
            this.from = EmailAddress.of(address);
            return this;
        }

        public Builder from(EmailAddress address) {
            this.from = address;
            return this;
        }

        public Builder replyTo(String address) {
            this.replyTo = EmailAddress.of(address);
            return this;
        }

        public Builder replyTo(EmailAddress address) {
            this.replyTo = address;
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

        public Builder htmlBody(String htmlBody) {
            this.htmlBody = htmlBody;
            return this;
        }

        public Builder attachment(Attachment attachment) {
            this.attachments.add(attachment);
            return this;
        }

        public Builder attachment(String filename, byte[] content, String contentType) {
            this.attachments.add(new Attachment(filename, content, contentType));
            return this;
        }

        public Builder header(String name, String value) {
            this.headers.put(name, value);
            return this;
        }

        public Builder attribute(String key, Object value) {
            this.attributes.put(key, value);
            return this;
        }

        public EmailRequest build() {
            Assert.notEmpty(to, "at least one 'to' recipient must be set");
            Assert.hasText(subject, "subject must be set");
            Assert.hasText(body, "body must be set");
            return new EmailRequest(to, cc, bcc,
                    Objects.requireNonNull(from, "from must be set"),
                    replyTo, subject, body,
                    htmlBody, attachments, headers, attributes);
        }
    }
}
