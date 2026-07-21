package sk.solodev.notify.autoconfigure.observation;

import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import sk.solodev.notify.DefaultNotificationObservationConvention;
import sk.solodev.notify.NotificationObservationConvention;
import sk.solodev.notify.ObservationNotificationInterceptor;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationObservationAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(NotificationObservationAutoConfiguration.class));

    @Test
    void registersInterceptorAndConventionWhenRegistryPresent() {
        runner.withBean(ObservationRegistry.class, ObservationRegistry::create).run(ctx -> {
            assertThat(ctx).hasSingleBean(ObservationNotificationInterceptor.class);
            assertThat(ctx).getBean(NotificationObservationConvention.class)
                    .isInstanceOf(DefaultNotificationObservationConvention.class);
        });
    }

    @Test
    void backsOffWhenNoObservationRegistry() {
        runner.run(ctx -> assertThat(ctx).doesNotHaveBean(ObservationNotificationInterceptor.class));
    }

    @Test
    void consumerConventionOverridesTheDefault() {
        NotificationObservationConvention custom = new NotificationObservationConvention() {
            @Override
            public String getName() {
                return "custom.notify";
            }
        };
        runner.withBean(ObservationRegistry.class, ObservationRegistry::create)
                .withBean(NotificationObservationConvention.class, () -> custom)
                .run(ctx -> {
                    assertThat(ctx).getBean(NotificationObservationConvention.class).isSameAs(custom);
                    assertThat(ctx).getBean(NotificationObservationConvention.class)
                            .isNotInstanceOf(DefaultNotificationObservationConvention.class);
                });
    }
}
