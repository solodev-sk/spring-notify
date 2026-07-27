package sk.solodev.notify.outbox;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import sk.solodev.notify.Notifier;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxAutoConfigurationTest {

    @Configuration
    static class StubNotifierConfig {

        @Bean
        Notifier notifier() {
            return request -> "PROVIDER-MID";
        }

        @Bean
        JsonMapper jsonMapper() {
            return JsonMapper.builder().build();
        }
    }

    @Configuration
    static class CustomStoreConfig {

        @Bean
        OutboxStore outboxStore() {
            return new OutboxStore() {

                @Override
                public void insert(OutboxEntry entry) { }

                @Override
                public List<OutboxEntry> claimBatch(int batchSize, Instant now) {
                    return List.of();
                }

                @Override
                public void markSent(UUID id, String messageId, Instant sentAt) { }

                @Override
                public void markForRetry(UUID id, String lastError, Instant nextAttemptAt) { }

                @Override
                public void markFailed(UUID id, String lastError) { }
            };
        }
    }

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class,
                    OutboxAutoConfiguration.class))
            .withUserConfiguration(StubNotifierConfig.class)
            .withPropertyValues(
                    "spring.datasource.url=jdbc:h2:mem:outbox-autoconfig;DB_CLOSE_DELAY=-1",
                    "spring.datasource.driver-class-name=org.h2.Driver");

    @Test
    void registersTheOutboxWhenADataSourceIsPresent() {
        runner.run(context -> assertThat(context)
                .hasSingleBean(OutboxNotifier.class)
                .hasSingleBean(OutboxStore.class)
                .hasSingleBean(OutboxRelay.class)
                .hasSingleBean(OutboxRelayScheduler.class));
    }

    @Test
    void suppliesItsOwnSchedulerSoApplicationsNeedNotEnableScheduling() {
        runner.run(context -> assertThat(context).hasBean("outboxTaskScheduler")
                .getBean("outboxTaskScheduler").isInstanceOf(TaskScheduler.class));
    }

    @Test
    void bindsTheConfiguredProperties() {
        runner.withPropertyValues("spring.notify.outbox.poll-interval=250ms",
                        "spring.notify.outbox.batch-size=7",
                        "spring.notify.outbox.table-name=my_outbox")
                .run(context -> {
                    var properties = context.getBean(OutboxProperties.class);
                    assertThat(properties.batchSize()).isEqualTo(7);
                    assertThat(properties.tableName()).isEqualTo("my_outbox");
                });
    }

    @Test
    void backsOffWhenTheApplicationSuppliesItsOwnStore() {
        runner.withUserConfiguration(CustomStoreConfig.class).run(context -> {
            assertThat(context).hasSingleBean(OutboxStore.class);
            assertThat(context.getBean(OutboxStore.class)).isNotInstanceOf(JdbcOutboxStore.class);
        });
    }

    @Test
    void registersNothingWithoutADataSource() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(OutboxAutoConfiguration.class))
                .withUserConfiguration(StubNotifierConfig.class)
                .run(context -> assertThat(context).doesNotHaveBean(OutboxNotifier.class));
    }
}
