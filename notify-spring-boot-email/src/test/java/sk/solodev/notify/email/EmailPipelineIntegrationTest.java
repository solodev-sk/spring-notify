package sk.solodev.notify.email;

import sk.solodev.notify.ChannelAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import sk.solodev.notify.Notifier;
import sk.solodev.notify.autoconfigure.NotificationAutoConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Composes the core pipeline auto-config with the email channel auto-config and a stub
 * {@link EmailSender}, then asserts an injectable {@link Notifier} that routes
 * an {@link EmailRequest} end to end.
 */
class EmailPipelineIntegrationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(NotificationAutoConfiguration.class, EmailAutoConfiguration.class))
            .withBean(EmailSender.class, () -> request -> "MID-" + request.to());

    @Test
    void notifierRoutesEmailRequestThroughTheChannelAdapter() {
        runner.run(ctx -> {
            assertThat(ctx).hasSingleBean(Notifier.class);
            assertThat(ctx).hasSingleBean(ChannelAdapter.class);

            var messageId = ctx.getBean(Notifier.class).notify(
                    EmailRequest.builder().to("a@b.com").from("x@x.com").subject("s").body("b").build());
            assertThat(messageId).isEqualTo("MID-a@b.com");
        });
    }
}
