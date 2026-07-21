package sk.solodev.notify.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import sk.solodev.notify.dispatch.AdapterResolver;
import sk.solodev.notify.dispatch.DefaultAdapterResolver;
import sk.solodev.notify.Notifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class NotificationAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(NotificationAutoConfiguration.class));

    @Test
    void registersDefaultServiceAndResolver() {
        runner.run(ctx -> {
            assertThat(ctx).hasSingleBean(Notifier.class);
            assertThat(ctx).getBean(AdapterResolver.class).isInstanceOf(DefaultAdapterResolver.class);
        });
    }

    @Test
    void consumerAdapterResolverOverridesTheDefault() {
        var custom = mock(AdapterResolver.class);

        runner.withBean(AdapterResolver.class, () -> custom).run(ctx ->
                assertThat(ctx).getBean(AdapterResolver.class).isSameAs(custom)
        );
    }
}
