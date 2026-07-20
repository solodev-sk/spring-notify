package sk.solodev.notify.sms;

import sk.solodev.notify.ChannelAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import sk.solodev.notify.Notifier;
import sk.solodev.notify.autoconfigure.NotificationAutoConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Composes the core pipeline auto-config with the SMS channel auto-config and a stub
 * {@link SmsSender}, then asserts an injectable {@link Notifier} that routes
 * an {@link SmsRequest} end to end. Guards the cross-module wiring that isolated slice
 * tests cannot see.
 */
class SmsPipelineIntegrationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(NotificationAutoConfiguration.class, SmsAutoConfiguration.class))
            .withBean(SmsSender.class, () -> request -> "SM-" + request.to());

    @Test
    void notifierRoutesSmsRequestThroughTheChannelAdapter() {
        runner.run(ctx -> {
            assertThat(ctx).hasSingleBean(Notifier.class);
            assertThat(ctx).hasSingleBean(ChannelAdapter.class);

            var messageId = ctx.getBean(Notifier.class).notify(
                    SmsRequest.builder().to("+43111").from("+43000").message("hi").build());
            assertThat(messageId).isEqualTo("SM-+43111");
        });
    }
}
