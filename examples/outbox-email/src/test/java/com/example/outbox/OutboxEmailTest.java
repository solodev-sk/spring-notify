package com.example.outbox;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Integration test proving the transactional outbox guarantee: a notification enqueued within a
 * business transaction is delivered if the transaction commits, and never sent if it rolls back.
 * Uses PostgreSQL (via Testcontainers) for the outbox table and orders table, and Mailpit as the
 * SMTP sink to verify actual delivery.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
class OutboxEmailTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

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

    @Autowired
    OrderService orderService;

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    JdbcClient jdbcClient;

    @Test
    void commitPath_orderPersistsAndEmailDelivers() throws Exception {
        var orderId = UUID.randomUUID();
        var order = new Order(orderId, "commit@example.com", "Widget", new BigDecimal("99.99"));

        orderService.placeOrder(order, false);

        assertThat(orderRepository.findById(orderId)).contains(order);

        var subject = "Order confirmation: Widget";
        var messageId = awaitMessageId(subject);
        var message = mailpitClient.message(messageId);

        assertThat(message.toAddresses()).containsExactly("commit@example.com");
        assertThat(message.from().address()).isEqualTo("orders@example.com");
        assertThat(message.subject()).isEqualTo(subject);
        assertThat(message.text()).contains("Widget", "99.99", orderId.toString());
    }

    @Test
    void rollbackPath_orderDoesNotPersistAndEmailNeverSent() throws Exception {
        var orderId = UUID.randomUUID();
        var order = new Order(orderId, "rollback@example.com", "Gadget", new BigDecimal("49.50"));

        try {
            orderService.placeOrder(order, true);
        } catch (IllegalStateException expected) {
            // Expected: the service throws to trigger rollback
        }

        assertThat(orderRepository.findById(orderId)).isEmpty();

        var outboxCount = jdbcClient.sql("SELECT COUNT(*) FROM notification_outbox WHERE id = :orderId")
                .param("orderId", orderId)
                .query(Integer.class)
                .single();
        assertThat(outboxCount).isZero();

        Thread.sleep(1000);

        var messages = mailpitClient.messages();
        var found = messages.firstWithSubject("Order confirmation: Gadget");
        assertThat(found).as("No email should arrive after rollback").isNull();
    }

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