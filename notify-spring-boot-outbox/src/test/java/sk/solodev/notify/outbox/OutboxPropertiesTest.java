package sk.solodev.notify.outbox;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxPropertiesTest {

    @Test
    void appliesDefaultsWhenValuesAreNullOrNonPositive() {
        var props = new OutboxProperties(null, 0, -1, null, null, null);

        assertThat(props.pollInterval()).isEqualTo(Duration.ofSeconds(5));
        assertThat(props.batchSize()).isEqualTo(100);
        assertThat(props.maxAttempts()).isEqualTo(5);
        assertThat(props.initialBackoff()).isEqualTo(Duration.ofSeconds(10));
        assertThat(props.maxBackoff()).isEqualTo(Duration.ofMinutes(10));
        assertThat(props.tableName()).isEqualTo("notification_outbox");
    }

    @Test
    void keepsConfiguredValues() {
        var props = new OutboxProperties(
                Duration.ofMinutes(2),
                50,
                10,
                Duration.ofSeconds(30),
                Duration.ofMinutes(30),
                "custom_outbox"
        );

        assertThat(props.pollInterval()).isEqualTo(Duration.ofMinutes(2));
        assertThat(props.batchSize()).isEqualTo(50);
        assertThat(props.maxAttempts()).isEqualTo(10);
        assertThat(props.initialBackoff()).isEqualTo(Duration.ofSeconds(30));
        assertThat(props.maxBackoff()).isEqualTo(Duration.ofMinutes(30));
        assertThat(props.tableName()).isEqualTo("custom_outbox");
    }

    @Test
    void treatsBlankTableNameAsUnset() {
        var props = new OutboxProperties(null, 0, 0, null, null, "   ");

        assertThat(props.tableName()).isEqualTo("notification_outbox");
    }

    @Test
    void treatsNegativeCountsAsUnset() {
        var props = new OutboxProperties(null, -5, -10, null, null, null);

        assertThat(props.batchSize()).isEqualTo(100);
        assertThat(props.maxAttempts()).isEqualTo(5);
    }
}
