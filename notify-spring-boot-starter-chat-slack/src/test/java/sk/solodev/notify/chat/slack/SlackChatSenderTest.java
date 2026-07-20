package sk.solodev.notify.chat.slack;

import com.slack.api.RequestConfigurator;
import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import org.junit.jupiter.api.Test;
import sk.solodev.notify.chat.ChatRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SlackChatSenderTest {

    private final MethodsClient methods = mock(MethodsClient.class);
    private final SlackChatSender sender = new SlackChatSender(methods);

    @SuppressWarnings("unchecked")
    @Test
    void postsMessageAndReturnsTimestamp() throws Exception {
        var response = new ChatPostMessageResponse();
        response.setOk(true);
        response.setTs("1503435956.000247");
        when(methods.chatPostMessage(any(RequestConfigurator.class))).thenReturn(response);

        var ts = sender.send(ChatRequest.builder().to("#alerts").message("Deploy finished").build());

        assertThat(ts).isEqualTo("1503435956.000247");
    }

    @SuppressWarnings("unchecked")
    @Test
    void throwsWhenSlackReturnsNotOk() throws Exception {
        var response = new ChatPostMessageResponse();
        response.setOk(false);
        response.setError("channel_not_found");
        when(methods.chatPostMessage(any(RequestConfigurator.class))).thenReturn(response);

        assertThatThrownBy(() -> sender.send(ChatRequest.builder().to("#nope").message("hi").build()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("channel_not_found");
    }
}
