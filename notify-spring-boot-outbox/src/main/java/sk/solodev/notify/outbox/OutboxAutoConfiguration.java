package sk.solodev.notify.outbox;

import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
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
 * @since 1.1.0
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
                                         OutboxProperties properties,
                                         OutboxTracePropagator tracePropagator) {
        return new DefaultOutboxNotifier(store, jsonMapper, properties, tracePropagator);
    }

    @Bean
    @ConditionalOnMissingBean
    public OutboxRelay outboxRelay(Notifier notifier, OutboxStore store,
                                   JsonMapper jsonMapper, OutboxProperties properties,
                                   OutboxTracePropagator tracePropagator) {
        return new OutboxRelay(notifier, store, jsonMapper, properties, tracePropagator);
    }

    /**
     * Fallback when tracing is absent. Conditional on the tracing beans being missing rather than
     * relying on {@link TracingConfiguration} being processed first, so which propagator wins does
     * not depend on declaration order.
     */
    @Bean
    @ConditionalOnMissingBean(value = OutboxTracePropagator.class, type = "io.micrometer.tracing.Tracer")
    public OutboxTracePropagator noOpOutboxTracePropagator() {
        return new NoOpOutboxTracePropagator();
    }

    /** Real trace propagation, when tracing is on the classpath and configured. */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(Tracer.class)
    @ConditionalOnBean({Tracer.class, Propagator.class})
    static class TracingConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public OutboxTracePropagator outboxTracePropagator(Tracer tracer, Propagator propagator) {
            return new MicrometerOutboxTracePropagator(tracer, propagator);
        }
    }

    /**
     * Scheduler for the relay. A dedicated single thread rather than the application's
     * {@code TaskScheduler}, so polling neither competes with unrelated scheduled work nor requires
     * the application to enable scheduling itself.
     */
    @Bean(destroyMethod = "shutdown")
    @ConditionalOnMissingBean(name = "outboxTaskScheduler")
    public ThreadPoolTaskScheduler outboxTaskScheduler() {
        var scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("notify-outbox-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        return scheduler;
    }

    @Bean
    @ConditionalOnMissingBean
    public OutboxRelayScheduler outboxRelayScheduler(OutboxRelay relay,
                                                     @Qualifier("outboxTaskScheduler") TaskScheduler taskScheduler,
                                                     OutboxProperties properties) {
        return new OutboxRelayScheduler(relay, taskScheduler, properties.pollInterval());
    }
}