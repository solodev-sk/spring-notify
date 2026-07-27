package com.example.outbox;

import org.jspecify.annotations.Nullable;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.util.List;
import java.util.Map;

import static java.net.http.HttpResponse.BodyHandlers;

/**
 * Thin client over <a href="https://mailpit.axllent.org/docs/api-v1/">Mailpit's HTTP API</a> for
 * asserting what actually arrived: the message summary list, a message's full detail (bodies,
 * recipients, attachment metadata), its raw headers, and an attachment's decoded bytes.
 */
final class MailpitClient {

    // Mailpit uses capitalized JSON keys (Subject, From, HTML, PartID, …); match them case-insensitively.
    private final JsonMapper json = JsonMapper.builder()
            .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
            .build();

    private final HttpClient http = HttpClient.newHttpClient();

    private final String baseUrl;

    MailpitClient(String host, int port) {
        this.baseUrl = "http://%s:%d".formatted(host, port);
    }

    Messages messages() throws Exception {
        return get("/api/v1/messages", Messages.class);
    }

    Message message(String id) throws Exception {
        return get("/api/v1/message/" + id, Message.class);
    }

    /**
     * Raw RFC 5322 headers; each name maps to its list of values.
     */
    Map<String, List<String>> headers(String id) throws Exception {
        return json.readValue(getRaw("/api/v1/message/" + id + "/headers"),
                new TypeReference<>() {
                });
    }

    /**
     * The decoded bytes of one MIME part (e.g. an attachment), as a string.
     */
    String part(String id, String partId) throws Exception {
        return getRaw("/api/v1/message/" + id + "/part/" + partId);
    }

    private <T> T get(String path, Class<T> type) throws Exception {
        return json.readValue(getRaw(path), type);
    }

    private String getRaw(String path) throws Exception {
        return http.send(HttpRequest.newBuilder(URI.create(baseUrl + path)).GET().build(),
                BodyHandlers.ofString()).body();
    }

    record Messages(List<Summary> messages) {
        @Nullable Summary firstWithSubject(String subject) {
            return messages.stream().filter(m -> subject.equals(m.subject())).findFirst().orElse(null);
        }
    }

    record Summary(String id, String subject) {
    }

    record Message(String id, Address from, List<Address> to, List<Address> cc, List<Address> bcc,
                   List<Address> replyTo, String subject, String text, String html,
                   List<Attachment> attachments) {

        List<String> toAddresses() {
            return addresses(to);
        }

        List<String> ccAddresses() {
            return addresses(cc);
        }

        List<String> bccAddresses() {
            return addresses(bcc);
        }

        List<String> replyToAddresses() {
            return addresses(replyTo);
        }

        private static List<String> addresses(List<Address> list) {
            return list.stream().map(Address::address).toList();
        }
    }

    record Address(String name, String address) {

    }

    record Attachment(String partID, String fileName, String contentType) {

    }
}