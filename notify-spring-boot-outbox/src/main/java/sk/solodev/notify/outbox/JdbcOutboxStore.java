package sk.solodev.notify.outbox;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * {@link OutboxStore} backed by {@link JdbcClient} on the application's shared {@code DataSource}.
 * {@link #insert} joins whatever transaction is active on the calling thread, so the row commits
 * with the caller's business change. {@link #claimBatch} uses {@code FOR UPDATE SKIP LOCKED} so
 * concurrent relays never claim the same row.
 *
 * @author Dominik Kovács
 * @since 1.1.0
 */
public class JdbcOutboxStore implements OutboxStore {

    private final JdbcClient jdbcClient;

    private final String tableName;

    public JdbcOutboxStore(JdbcClient jdbcClient, String tableName) {
        this.jdbcClient = jdbcClient;
        this.tableName = tableName;
    }

    @Override
    public void insert(OutboxEntry e) {
        jdbcClient.sql("INSERT INTO " + tableName + " (id, request_type, payload, status, attempts, "
                        + "max_attempts, message_id, last_error, created_at, next_attempt_at, sent_at) "
                        + "VALUES (?,?,?,?,?,?,?,?,?,?,?)")
                .param(e.id()).param(e.requestType()).param(e.payload()).param(e.status().name())
                .param(e.attempts()).param(e.maxAttempts()).param(e.messageId()).param(e.lastError())
                .param(Timestamp.from(e.createdAt())).param(Timestamp.from(e.nextAttemptAt()))
                .param(e.sentAt() == null ? null : Timestamp.from(e.sentAt()))
                .update();
    }

    @Override
    @Transactional
    public List<OutboxEntry> claimBatch(int batchSize, Instant now) {
        return jdbcClient.sql("SELECT id, request_type, payload, status, attempts, max_attempts, "
                        + "message_id, last_error, created_at, next_attempt_at, sent_at FROM " + tableName
                        + " WHERE status = 'PENDING' AND next_attempt_at <= ? ORDER BY created_at "
                        + "FOR UPDATE SKIP LOCKED LIMIT ?")
                .param(Timestamp.from(now)).param(batchSize)
                .query((rs, rowNum) -> new OutboxEntry(
                        rs.getObject("id", UUID.class),
                        rs.getString("request_type"),
                        rs.getString("payload"),
                        OutboxStatus.valueOf(rs.getString("status")),
                        rs.getInt("attempts"),
                        rs.getInt("max_attempts"),
                        rs.getString("message_id"),
                        rs.getString("last_error"),
                        rs.getTimestamp("created_at").toInstant(),
                        rs.getTimestamp("next_attempt_at").toInstant(),
                        rs.getTimestamp("sent_at") == null ? null : rs.getTimestamp("sent_at").toInstant()))
                .list();
    }

    @Override
    public void markSent(UUID id, String messageId, Instant sentAt) {
        jdbcClient.sql("UPDATE " + tableName + " SET status = 'SENT', message_id = ?, sent_at = ? WHERE id = ?")
                .param(messageId).param(Timestamp.from(sentAt)).param(id)
                .update();
    }

    @Override
    public void markForRetry(UUID id, String lastError, Instant nextAttemptAt) {
        jdbcClient.sql("UPDATE " + tableName + " SET attempts = attempts + 1, last_error = ?, "
                        + "next_attempt_at = ? WHERE id = ?")
                .param(lastError).param(Timestamp.from(nextAttemptAt)).param(id)
                .update();
    }

    @Override
    public void markFailed(UUID id, String lastError) {
        jdbcClient.sql("UPDATE " + tableName + " SET status = 'FAILED', attempts = attempts + 1, "
                        + "last_error = ? WHERE id = ?")
                .param(lastError).param(id)
                .update();
    }
}