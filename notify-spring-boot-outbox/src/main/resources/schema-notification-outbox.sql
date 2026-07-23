-- Canonical DDL for the spring-notify transactional outbox.
-- Not auto-executed: run via your migration tool (Flyway/Liquibase), or in dev/test via
-- spring.sql.init. Shown for PostgreSQL; for MySQL 8+ use CHAR(36)/VARCHAR for the uuid column
-- and DATETIME(6) for timestamps. FOR UPDATE SKIP LOCKED requires PostgreSQL or MySQL 8.0+.
CREATE TABLE IF NOT EXISTS notification_outbox (
    id               UUID PRIMARY KEY,
    request_type     VARCHAR(255) NOT NULL,
    payload          TEXT         NOT NULL,
    status           VARCHAR(16)  NOT NULL,
    attempts         INT          NOT NULL,
    max_attempts     INT          NOT NULL,
    message_id       VARCHAR(255),
    last_error       TEXT,
    created_at       TIMESTAMP    NOT NULL,
    next_attempt_at  TIMESTAMP    NOT NULL,
    sent_at          TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_notification_outbox_poll
    ON notification_outbox (status, next_attempt_at);