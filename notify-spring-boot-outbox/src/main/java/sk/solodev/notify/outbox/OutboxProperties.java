package sk.solodev.notify.outbox;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Outbox settings, under {@code spring.notify.outbox}.
 *
 * @param pollInterval   how often the relay polls for pending entries (default 5s)
 * @param batchSize      rows claimed per poll (default 100)
 * @param maxAttempts    delivery attempts before an entry is marked FAILED (default 5)
 * @param initialBackoff delay before the first retry (default 10s)
 * @param maxBackoff     cap on the exponential backoff (default 10m)
 * @param tableName      the outbox table name (default notification_outbox)
 *
 * @author Dominik Kovács
 * @since 1.0.1
 */
@ConfigurationProperties("spring.notify.outbox")
public record OutboxProperties(Duration pollInterval, int batchSize, int maxAttempts,
                               Duration initialBackoff, Duration maxBackoff, String tableName) {

    public OutboxProperties {
        pollInterval = pollInterval == null ? Duration.ofSeconds(5) : pollInterval;
        batchSize = batchSize <= 0 ? 100 : batchSize;
        maxAttempts = maxAttempts <= 0 ? 5 : maxAttempts;
        initialBackoff = initialBackoff == null ? Duration.ofSeconds(10) : initialBackoff;
        maxBackoff = maxBackoff == null ? Duration.ofMinutes(10) : maxBackoff;
        tableName = (tableName == null || tableName.isBlank()) ? "notification_outbox" : tableName;
    }
}