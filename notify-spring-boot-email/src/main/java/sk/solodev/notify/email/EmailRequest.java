package sk.solodev.notify.email;

import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;
import sk.solodev.notify.NotificationRequest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        Assert.notEmpty(to, "at least one 'to' recipient must be set");
        Assert.notNull(from, "from must be set");
        Assert.hasText(subject, "subject must be set");
        Assert.hasText(body, "body must be set");
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

        public Builder to(String... addresses) {
            for (String address : addresses) {
                this.to.add(EmailAddress.of(address));
            }
            return this;
        }

        public Builder to(EmailAddress... addresses) {
            this.to.addAll(List.of(addresses));
            return this;
        }

        public Builder to(Collection<EmailAddress> addresses) {
            this.to.addAll(addresses);
            return this;
        }

        public Builder cc(String... addresses) {
            for (String address : addresses) {
                this.cc.add(EmailAddress.of(address));
            }
            return this;
        }

        public Builder cc(EmailAddress... addresses) {
            this.cc.addAll(List.of(addresses));
            return this;
        }

        public Builder cc(Collection<EmailAddress> addresses) {
            this.cc.addAll(addresses);
            return this;
        }

        public Builder bcc(String... addresses) {
            for (String address : addresses) {
                this.bcc.add(EmailAddress.of(address));
            }
            return this;
        }

        public Builder bcc(EmailAddress... addresses) {
            this.bcc.addAll(List.of(addresses));
            return this;
        }

        public Builder bcc(Collection<EmailAddress> addresses) {
            this.bcc.addAll(addresses);
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

        public Builder attachments(Attachment... attachments) {
            this.attachments.addAll(List.of(attachments));
            return this;
        }

        public Builder attachments(Collection<Attachment> attachments) {
            this.attachments.addAll(attachments);
            return this;
        }

        public Builder header(String name, String value) {
            this.headers.put(name, value);
            return this;
        }

        public Builder headers(Map<String, String> headers) {
            this.headers.putAll(headers);
            return this;
        }

        public Builder attribute(String key, Object value) {
            this.attributes.put(key, value);
            return this;
        }

        public Builder attributes(Map<String, Object> attributes) {
            this.attributes.putAll(attributes);
            return this;
        }

        public EmailRequest build() {
            return new EmailRequest(to, cc, bcc, from, replyTo, subject, body,
                    htmlBody, attachments, headers, attributes);
        }
    }
}
