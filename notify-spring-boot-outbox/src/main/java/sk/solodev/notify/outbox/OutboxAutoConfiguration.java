package sk.solodev.notify.outbox;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.jdbc.core.simple.JdbcClient;
import sk.solodev.notify.Notifier;
import tools.jackson.databind.json.JsonMapper;

import javax.sql.DataSource;

/**
 * Wires the transactional outbox whenever a {@link DataSource} is present — adding the module is the
 * opt-in. Registers a {@link JdbcOutboxStore}, a {@link DefaultOutboxNotifier}, and an
 * {@link OutboxRelay} driven by a scheduled trigger at {@code spring.notify.outbox.poll-interval}.
 *
 * @author Dominik Kovács
 * @since 1.0.1
 */
@AutoConfiguration
@ConditionalOnBean(DataSource.class)
@EnableConfigurationProperties(OutboxProperties.class)
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
    @ConditionalOnMissingBean
    public OutboxRelayScheduler outboxRelayScheduler(OutboxRelay relay, TaskScheduler taskScheduler,
                                                     OutboxProperties properties) {
        return new OutboxRelayScheduler(relay, taskScheduler, properties.pollInterval());
    }
}