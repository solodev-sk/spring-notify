package sk.solodev.notify.outbox;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = JdbcOutboxStoreTest.TestApp.class,
        properties = {
                "spring.sql.init.mode=always",
                "spring.sql.init.schema-locations=classpath:schema-notification-outbox.sql"
        })
@Testcontainers
class JdbcOutboxStoreTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    JdbcClient jdbcClient;

    JdbcOutboxStore store;

    // Minimal Boot context: just enough autoconfig to get a DataSource + JdbcClient + schema init.
    @SpringBootApplication
    static class TestApp { }

    JdbcOutboxStore store() {
        if (store == null) {
            store = new JdbcOutboxStore(jdbcClient, "notification_outbox");
        }
        return store;
    }

    @AfterEach
    void clean() {
        jdbcClient.sql("DELETE FROM notification_outbox").update();
    }

    private OutboxEntry pending(Instant nextAttemptAt) {
        var now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        return new OutboxEntry(UUID.randomUUID(), "com.example.Req", "{}", OutboxStatus.PENDING,
                0, 5, null, null, now, nextAttemptAt, null);
    }

    @Test
    void insertsAndClaimsDuePendingEntries() {
        var due = pending(Instant.now().minusSeconds(1));
        store().insert(due);

        List<OutboxEntry> claimed = store().claimBatch(10, Instant.now());

        assertThat(claimed).singleElement().satisfies(e -> {
            assertThat(e.id()).isEqualTo(due.id());
            assertThat(e.status()).isEqualTo(OutboxStatus.PENDING);
            assertThat(e.requestType()).isEqualTo("com.example.Req");
        });
    }

    @Test
    void doesNotClaimEntriesNotYetDue() {
        store().insert(pending(Instant.now().plusSeconds(60)));

        assertThat(store().claimBatch(10, Instant.now())).isEmpty();
    }

    @Test
    void markSentUpdatesStatusAndMessageId() {
        var entry = pending(Instant.now().minusSeconds(1));
        store().insert(entry);

        store().markSent(entry.id(), "PROVIDER-123", Instant.now());

        assertThat(store().claimBatch(10, Instant.now())).isEmpty();
        var status = jdbcClient.sql("SELECT status FROM notification_outbox WHERE id = ?")
                .param(entry.id()).query(String.class).single();
        assertThat(status).isEqualTo("SENT");
    }

    @Test
    void markForRetryIncrementsAttemptsAndDelaysNextAttempt() {
        var entry = pending(Instant.now().minusSeconds(1));
        store().insert(entry);

        store().markForRetry(entry.id(), "boom", Instant.now().plusSeconds(60));

        assertThat(store().claimBatch(10, Instant.now())).isEmpty();
        var attempts = jdbcClient.sql("SELECT attempts FROM notification_outbox WHERE id = ?")
                .param(entry.id()).query(Integer.class).single();
        assertThat(attempts).isEqualTo(1);
    }

    @Test
    void markFailedTakesEntryOutOfRotation() {
        var entry = pending(Instant.now().minusSeconds(1));
        store().insert(entry);

        store().markFailed(entry.id(), "gave up");

        assertThat(store().claimBatch(10, Instant.now())).isEmpty();
        var status = jdbcClient.sql("SELECT status FROM notification_outbox WHERE id = ?")
                .param(entry.id()).query(String.class).single();
        assertThat(status).isEqualTo("FAILED");
    }
}