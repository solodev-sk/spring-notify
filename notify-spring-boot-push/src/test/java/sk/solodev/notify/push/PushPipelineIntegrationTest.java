package sk.solodev.notify.push;

import sk.solodev.notify.dispatch.ChannelAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import sk.solodev.notify.Notifier;
import sk.solodev.notify.autoconfigure.NotificationAutoConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Composes the core pipeline auto-config with the push channel auto-config and a stub
 * {@link PushSender}, then asserts an injectable {@link Notifier} that routes
 * a {@link PushRequest} end to end.
 */
class PushPipelineIntegrationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(NotificationAutoConfiguration.class, PushAutoConfiguration.class))
            .withBean(PushSender.class, () -> request -> "MID-" + request.to());

    @Test
    void notifierRoutesPushRequestThroughTheChannelAdapter() {
        runner.run(ctx -> {
            assertThat(ctx).hasSingleBean(Notifier.class);
            assertThat(ctx).hasSingleBean(ChannelAdapter.class);

            var messageId = ctx.getBean(Notifier.class).notify(
                    PushRequest.builder().to("tok").title("t").body("b").build());
            assertThat(messageId).isEqualTo("MID-tok");
        });
    }
}
