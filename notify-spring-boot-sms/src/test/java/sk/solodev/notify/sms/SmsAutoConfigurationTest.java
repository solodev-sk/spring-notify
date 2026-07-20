package sk.solodev.notify.sms;

import sk.solodev.notify.ChannelAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class SmsAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(SmsAutoConfiguration.class));

    @Test
    void registersAdapterWhenAnSmsSenderBeanExists() {
        runner.withBean(SmsSender.class, () -> request -> "SID").run(ctx ->
                assertThat(ctx).hasSingleBean(ChannelAdapter.class));
    }

    @Test
    void noAdapterWhenNoSmsSenderBean() {
        runner.run(ctx -> assertThat(ctx).doesNotHaveBean(ChannelAdapter.class));
    }
}
