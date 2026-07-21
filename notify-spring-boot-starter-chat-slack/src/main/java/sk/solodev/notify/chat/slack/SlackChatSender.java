package sk.solodev.notify.chat.slack;

import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.SlackApiException;
import sk.solodev.notify.chat.ChatRequest;
import sk.solodev.notify.chat.ChatSender;

import java.io.IOException;

/**
 * {@link ChatSender} backed by the Slack SDK. Posts via {@code chat.postMessage} to the
 * channel given by {@code request.to()} and returns the message timestamp ({@code ts}).
 * Provider failures propagate (checked SDK exceptions or a runtime error for a non-ok
 * response); the {@code ChatChannelAdapter} converts them to {@code NotificationDeliveryException}.
 *
 * @author Dominik Kovács
 * @since 1.0.0
 */
public class SlackChatSender implements ChatSender {

    private final MethodsClient methods;

    public SlackChatSender(MethodsClient methods) {
        this.methods = methods;
    }

    @Override
    public String send(ChatRequest request) throws SlackApiException, IOException {
        var response = methods.chatPostMessage(r -> r
                .channel(request.to())
                .text(request.message()));
        if (!response.isOk()) {
            throw new RuntimeException("Slack chat.postMessage failed: " + response.getError());
        }
        return response.getTs();
    }
}
