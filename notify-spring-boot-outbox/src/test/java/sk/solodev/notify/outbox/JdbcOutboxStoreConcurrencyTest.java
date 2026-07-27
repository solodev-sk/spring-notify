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

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class JdbcOutboxStoreConcurrencyTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withInitScript("schema-notification-outbox.sql");

    @Configuration
    @EnableAutoConfiguration(exclude = OutboxAutoConfiguration.class)
    static class TestConfig { }

    @Autowired
    JdbcClient jdbcClient;

    @Autowired
    DataSource dataSource;

    @BeforeEach
    void setUp() {
        jdbcClient.sql("DELETE FROM notification_outbox").update();
        for (int i = 0; i < 4; i++) {
            insertPending(UUID.randomUUID());
        }
    }

    private void insertPending(UUID id) {
        var now = Timestamp.from(Instant.now().minusSeconds(1));
        jdbcClient.sql("""
                        INSERT INTO notification_outbox (id, request_type, payload, status, attempts,
                            max_attempts, created_at, next_attempt_at)
                        VALUES (?, ?, ?, 'PENDING', 0, 5, ?, ?)
                        """)
                .param(id).param("com.example.SampleRequest").param("{}").param(now).param(now)
                .update();
    }

    /** The claim query under test — run directly so the caller controls the transaction. */
    private List<UUID> claim(Connection connection, int batchSize) throws Exception {
        var ids = new ArrayList<UUID>();
        try (var statement = connection.prepareStatement("""
                SELECT id FROM notification_outbox
                WHERE status = 'PENDING' AND next_attempt_at <= ?
                ORDER BY created_at
                FOR UPDATE SKIP LOCKED
                LIMIT ?
                """)) {
            statement.setTimestamp(1, Timestamp.from(Instant.now()));
            statement.setInt(2, batchSize);
            try (var rows = statement.executeQuery()) {
                while (rows.next()) {
                    ids.add(rows.getObject("id", UUID.class));
                }
            }
        }
        return ids;
    }

    @Test
    void concurrentClaimersNeverReceiveTheSameEntry() throws Exception {
        try (var first = dataSource.getConnection(); var second = dataSource.getConnection()) {
            first.setAutoCommit(false);
            second.setAutoCommit(false);

            // first claimer locks two rows and holds them
            var firstBatch = claim(first, 2);
            // second claimer must skip those and take the other two
            var secondBatch = claim(second, 2);

            assertThat(firstBatch).hasSize(2);
            assertThat(secondBatch).hasSize(2);
            assertThat(firstBatch).doesNotContainAnyElementsOf(secondBatch);

            first.rollback();
            second.rollback();
        }
    }

    @Test
    void aSecondClaimerGetsNothingWhenAllDueEntriesAreLocked() throws Exception {
        try (var first = dataSource.getConnection(); var second = dataSource.getConnection()) {
            first.setAutoCommit(false);
            second.setAutoCommit(false);

            assertThat(claim(first, 100)).hasSize(4);
            assertThat(claim(second, 100)).isEmpty();

            first.rollback();
            second.rollback();
        }
    }
}
