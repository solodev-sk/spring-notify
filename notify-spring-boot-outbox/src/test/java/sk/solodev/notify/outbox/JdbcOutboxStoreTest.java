package sk.solodev.notify.outbox;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class JdbcOutboxStoreTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withInitScript("schema-notification-outbox.sql");

    @Configuration
    @EnableAutoConfiguration(exclude = OutboxAutoConfiguration.class)
    static class TestConfig { }

    @Autowired
    JdbcClient jdbcClient;

    JdbcOutboxStore store;

    @BeforeEach
    void setUp() {
        store = new JdbcOutboxStore(jdbcClient, "notification_outbox");
        jdbcClient.sql("DELETE FROM notification_outbox").update();
    }

    private OutboxEntry pending(Instant nextAttemptAt) {
        var now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        return new OutboxEntry(UUID.randomUUID(), "com.example.SampleRequest", "{\"to\":\"x\"}",
                OutboxStatus.PENDING, 0, 5, null, null, now, nextAttemptAt, null, null);
    }

    private String statusOf(UUID id) {
        return jdbcClient.sql("SELECT status FROM notification_outbox WHERE id = ?")
                .param(id).query(String.class).single();
    }

    private int attemptsOf(UUID id) {
        return jdbcClient.sql("SELECT attempts FROM notification_outbox WHERE id = ?")
                .param(id).query(Integer.class).single();
    }

    @Test
    void roundTripsEveryColumnOfAnEntry() {
        var inserted = pending(Instant.now().minusSeconds(1));

        store.insert(inserted);

        assertThat(store.claimBatch(10, Instant.now())).singleElement().satisfies(claimed -> {
            assertThat(claimed.id()).isEqualTo(inserted.id());
            assertThat(claimed.requestType()).isEqualTo(inserted.requestType());
            assertThat(claimed.payload()).isEqualTo(inserted.payload());
            assertThat(claimed.status()).isEqualTo(OutboxStatus.PENDING);
            assertThat(claimed.attempts()).isZero();
            assertThat(claimed.maxAttempts()).isEqualTo(5);
            assertThat(claimed.messageId()).isNull();
            assertThat(claimed.lastError()).isNull();
            assertThat(claimed.sentAt()).isNull();
        });
    }

    @Test
    void doesNotClaimEntriesWhoseNextAttemptIsInTheFuture() {
        store.insert(pending(Instant.now().plusSeconds(600)));

        assertThat(store.claimBatch(10, Instant.now())).isEmpty();
    }

    @Test
    void honoursTheBatchSize() {
        for (int i = 0; i < 5; i++) {
            store.insert(pending(Instant.now().minusSeconds(1)));
        }

        assertThat(store.claimBatch(2, Instant.now())).hasSize(2);
    }

    @Test
    void claimsTheOldestEntriesFirst() {
        var older = pending(Instant.now().minusSeconds(60));
        var newer = pending(Instant.now().minusSeconds(1));
        // insert newest first so ordering cannot come from insertion order
        store.insert(newer);
        store.insert(older);

        var claimed = store.claimBatch(10, Instant.now());

        assertThat(claimed).hasSize(2);
        assertThat(claimed.getFirst().createdAt()).isBeforeOrEqualTo(claimed.get(1).createdAt());
    }

    @Test
    void markSentTakesTheEntryOutOfRotation() {
        var entry = pending(Instant.now().minusSeconds(1));
        store.insert(entry);

        store.markSent(entry.id(), "PROVIDER-MID", Instant.now());

        assertThat(statusOf(entry.id())).isEqualTo("SENT");
        assertThat(store.claimBatch(10, Instant.now())).isEmpty();
    }

    @Test
    void markForRetryCountsTheAttemptAndDefersTheEntry() {
        var entry = pending(Instant.now().minusSeconds(1));
        store.insert(entry);

        store.markForRetry(entry.id(), "provider rejected", Instant.now().plusSeconds(600));

        assertThat(attemptsOf(entry.id())).isEqualTo(1);
        assertThat(statusOf(entry.id())).isEqualTo("PENDING");
        assertThat(store.claimBatch(10, Instant.now())).isEmpty();
    }

    @Test
    void aDeferredEntryBecomesClaimableAgainOnceItIsDue() {
        var entry = pending(Instant.now().minusSeconds(1));
        store.insert(entry);
        store.markForRetry(entry.id(), "provider rejected", Instant.now().plusSeconds(30));

        // ask as if we were polling after the retry became due
        assertThat(store.claimBatch(10, Instant.now().plusSeconds(60))).hasSize(1);
    }

    @Test
    void markFailedTakesTheEntryOutOfRotationPermanently() {
        var entry = pending(Instant.now().minusSeconds(1));
        store.insert(entry);

        store.markFailed(entry.id(), "gave up");

        assertThat(statusOf(entry.id())).isEqualTo("FAILED");
        assertThat(store.claimBatch(10, Instant.now().plusSeconds(3600))).isEmpty();
    }

    @Test
    void roundTripsTheTraceContext() {
        var now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        var traced = new OutboxEntry(UUID.randomUUID(), "com.example.SampleRequest", "{}",
                OutboxStatus.PENDING, 0, 5, null, null, now, now.minusSeconds(1), null,
                "00-0af7651916cd43dd8448eb211c80319c-b7ad6b7169203331-01");

        store.insert(traced);

        assertThat(store.claimBatch(10, Instant.now())).singleElement()
                .satisfies(claimed -> assertThat(claimed.traceContext())
                        .isEqualTo("00-0af7651916cd43dd8448eb211c80319c-b7ad6b7169203331-01"));
    }

    @Test
    void storesEntriesWithoutATraceContext() {
        var entry = pending(Instant.now().minusSeconds(1));

        store.insert(entry);

        assertThat(store.claimBatch(10, Instant.now())).singleElement()
                .satisfies(claimed -> assertThat(claimed.traceContext()).isNull());
    }
}
