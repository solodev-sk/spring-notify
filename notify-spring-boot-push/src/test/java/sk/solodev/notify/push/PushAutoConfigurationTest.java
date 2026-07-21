package sk.solodev.notify.push;

import sk.solodev.notify.dispatch.ChannelAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class PushAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(PushAutoConfiguration.class));

    @Test
    void registersAdapterWhenAPushSenderBeanExists() {
        runner.withBean(PushSender.class, () -> request -> "MID").run(ctx ->
                assertThat(ctx).hasSingleBean(ChannelAdapter.class));
    }

    @Test
    void noAdapterWhenNoPushSenderBean() {
        runner.run(ctx -> assertThat(ctx).doesNotHaveBean(ChannelAdapter.class));
    }
}
