package sk.solodev.notify.chat;

import sk.solodev.notify.ChannelAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import sk.solodev.notify.Notifier;
import sk.solodev.notify.autoconfigure.NotificationAutoConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Composes the core pipeline auto-config with the chat channel auto-config and a stub
 * {@link ChatSender}, then asserts an injectable {@link Notifier} that routes
 * a {@link ChatRequest} end to end.
 */
class ChatPipelineIntegrationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(NotificationAutoConfiguration.class, ChatAutoConfiguration.class))
            .withBean(ChatSender.class, () -> request -> "ts-" + request.to());

    @Test
    void notifierRoutesChatRequestThroughTheChannelAdapter() {
        runner.run(ctx -> {
            assertThat(ctx).hasSingleBean(Notifier.class);
            assertThat(ctx).hasSingleBean(ChannelAdapter.class);

            var messageId = ctx.getBean(Notifier.class).notify(
                    ChatRequest.builder().to("#alerts").message("hi").build());
            assertThat(messageId).isEqualTo("ts-#alerts");
        });
    }
}
