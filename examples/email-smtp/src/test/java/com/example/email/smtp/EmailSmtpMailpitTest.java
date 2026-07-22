package com.example.email.smtp;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import sk.solodev.notify.email.Attachment;
import sk.solodev.notify.email.EmailAddress;
import sk.solodev.notify.email.EmailRequest;
import tools.jackson.databind.json.JsonMapper;

import java.util.concurrent.atomic.AtomicReference;

import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Drives the {@link com.example.email.smtp.controller.EmailController} through MockMvc for the main
 * {@code EmailRequest} shapes — plain text, multipart/alternative (HTML), attachments, and
 * cc/bcc/reply-to/headers — sends each through the real SMTP starter into a throwaway Mailpit
 * container, then asserts it actually arrived via Mailpit's HTTP API. No external accounts, no cost:
 * the always-on CI net that keeps the example honest.
 *
 * <p>Mailpit runs in a Testcontainers container started before the Spring context; its dynamic
 * SMTP host/port are bound to {@code spring.notify.email.smtp.*} via {@link DynamicPropertySource}
 * so they are present when the SMTP starter's {@code @ConditionalOnProperty} auto-configuration is
 * evaluated.
 */
@SpringBootTest
@Testcontainers
@AutoConfigureMockMvc
class EmailSmtpMailpitTest {

    @Container
    static GenericContainer<?> mailpit = new GenericContainer<>("axllent/mailpit:v1.30.5")
            .withExposedPorts(1025, 8025)
            .waitingFor(Wait.forHttp("/readyz").forPort(8025));

    @DynamicPropertySource
    static void smtpProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.notify.email.smtp.host", mailpit::getHost);
        registry.add("spring.notify.email.smtp.port", () -> mailpit.getMappedPort(1025));
    }

    private static MailpitClient mailpitClient;

    @BeforeAll
    static void initMailpitClient() {
        mailpitClient = new MailpitClient(mailpit.getHost(), mailpit.getMappedPort(8025));
    }

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Autowired
    MockMvc mockMvc;

    @Test
    void deliversPlainTextEmailWithDisplayNames() throws Exception {
        send(EmailRequest.builder()
                .to(new EmailAddress("Alice", "plain@example.com"))
                .from(new EmailAddress("Shop", "shop@example.com"))
                .subject("Plain hello")
                .body("Just text.")
                .build());

        var message = mailpitClient.message(awaitMessageId("Plain hello"));

        assertThat(message.from().name()).isEqualTo("Shop");
        assertThat(message.from().address()).isEqualTo("shop@example.com");
        assertThat(message.to()).singleElement().satisfies(to -> {
            assertThat(to.name()).isEqualTo("Alice");
            assertThat(to.address()).isEqualTo("plain@example.com");
        });
        assertThat(message.text().strip()).isEqualTo("Just text.");
        assertThat(message.html()).isEmpty();
        assertThat(message.attachments()).isEmpty();
    }

    @Test
    void deliversMultipartAlternativeWithHtmlBody() throws Exception {
        send(EmailRequest.builder()
                .to("html@example.com")
                .from("shop@example.com")
                .subject("HTML hello")
                .body("text fallback")
                .htmlBody("<h1>Hello</h1>")
                .build());

        var message = mailpitClient.message(awaitMessageId("HTML hello"));

        assertThat(message.text().strip()).isEqualTo("text fallback");
        assertThat(message.html().strip()).isEqualTo("<h1>Hello</h1>");
    }

    @Test
    void deliversEmailWithAttachment() throws Exception {
        send(EmailRequest.builder()
                .to("attach@example.com")
                .from("shop@example.com")
                .subject("With attachment")
                .body("See attached.")
                .attachments(Attachment.builder()
                        .filename("invoice.txt")
                        .content("hello attachment".getBytes())
                        .contentType("text/plain")
                        .build())
                .build());

        var message = mailpitClient.message(awaitMessageId("With attachment"));

        assertThat(message.attachments()).hasSize(1);
        var attachment = message.attachments().getFirst();
        assertThat(attachment.fileName()).isEqualTo("invoice.txt");
        // fetch the raw part and confirm the bytes survived the round trip
        assertThat(mailpitClient.part(message.id(), attachment.partID())).isEqualTo("hello attachment");
    }

    @Test
    void deliversEmailWithCcBccReplyToAndHeaders() throws Exception {
        send(EmailRequest.builder()
                .to("to@example.com")
                .cc("cc@example.com")
                .bcc("bcc@example.com")
                .from("shop@example.com")
                .replyTo("reply@example.com")
                .subject("Full headers")
                .body("Body.")
                .header("X-Campaign", "summer-sale")
                .build());

        var message = mailpitClient.message(awaitMessageId("Full headers"));

        assertThat(message.toAddresses()).containsExactly("to@example.com");
        assertThat(message.ccAddresses()).containsExactly("cc@example.com");
        assertThat(message.bccAddresses()).containsExactly("bcc@example.com");
        assertThat(message.replyToAddresses()).containsExactly("reply@example.com");
        assertThat(mailpitClient.headers(message.id()).get("X-Campaign")).containsExactly("summer-sale");
    }

    private void send(EmailRequest request) throws Exception {
        mockMvc.perform(post("/emails")
                        .contentType(APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    /** Polls Mailpit until a message with the given subject is delivered, then returns its id. */
    private String awaitMessageId(String subject) {
        var id = new AtomicReference<String>();
        await().atMost(ofSeconds(10)).untilAsserted(() -> {
            var found = mailpitClient.messages().firstWithSubject(subject);
            assertThat(found).as("message with subject '%s'", subject).isNotNull();
            id.set(found.id());
        });
        return id.get();
    }
}
