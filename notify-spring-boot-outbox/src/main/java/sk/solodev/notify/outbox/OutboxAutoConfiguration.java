package sk.solodev.notify.outbox;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.jdbc.core.simple.JdbcClient;
import sk.solodev.notify.Notifier;
import tools.jackson.databind.json.JsonMapper;

import javax.sql.DataSource;

/**
 * Wires the transactional outbox when {@code spring.notify.outbox.enabled=true} and a
 * {@link DataSource} is present. Registers a {@link JdbcOutboxStore}, a {@link DefaultOutboxNotifier},
 * and an {@link OutboxRelay} driven by a scheduled trigger at {@code spring.notify.outbox.poll-interval}.
 *
 * @author Dominik Kovács
 * @since 1.0.1
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "spring.notify.outbox", name = "enabled", havingValue = "true")
@ConditionalOnBean(DataSource.class)
@EnableConfigurationProperties(OutboxProperties.class)
@EnableScheduling
public class OutboxAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public OutboxStore outboxStore(DataSource dataSource, OutboxProperties properties) {
        return new JdbcOutboxStore(JdbcClient.create(dataSource), properties.tableName());
    }

    @Bean
    @ConditionalOnMissingBean
    public OutboxNotifier outboxNotifier(OutboxStore store, JsonMapper jsonMapper,
                                         OutboxProperties properties) {
        return new DefaultOutboxNotifier(store, jsonMapper, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public OutboxRelay outboxRelay(Notifier notifier, OutboxStore store,
                                   JsonMapper jsonMapper, OutboxProperties properties) {
        return new OutboxRelay(notifier, store, jsonMapper, properties);
    }

    @Bean
    public OutboxRelayScheduler outboxRelayScheduler(OutboxRelay relay) {
        return new OutboxRelayScheduler(relay);
    }

    /**
     * Fixed-delay trigger; interval bound from {@code spring.notify.outbox.poll-interval}.
     */
    public static class OutboxRelayScheduler {

        private final OutboxRelay relay;

        OutboxRelayScheduler(OutboxRelay relay) {
            this.relay = relay;
        }

        @Scheduled(fixedDelayString = "${spring.notify.outbox.poll-interval:PT5S}")
        public void run() {
            relay.poll();
        }
    }
}