package sk.solodev.notify.sms.vonage;

import com.vonage.client.sms.MessageStatus;
import com.vonage.client.sms.SmsClient;
import com.vonage.client.sms.SmsSubmissionResponse;
import com.vonage.client.sms.SmsSubmissionResponseMessage;
import com.vonage.client.sms.messages.Message;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import sk.solodev.notify.sms.SmsRequest;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VonageSmsSenderTest {

    private final SmsClient smsClient = mock(SmsClient.class);
    private final VonageSmsSender sender = new VonageSmsSender(smsClient);

    @Test
    void mapsRequestToSdkAndReturnsMessageId() throws Exception {
        var response = response(MessageStatus.OK, "vg-123", null);
        when(smsClient.submitMessage(any(Message.class))).thenReturn(response);

        var id = sender.send(new SmsRequest("+43111", "+43000", "hi", Map.of()));

        assertThat(id).isEqualTo("vg-123");

        var captor = ArgumentCaptor.forClass(Message.class);
        verify(smsClient).submitMessage(captor.capture());
        var sent = captor.getValue();
        assertThat(sent.getFrom()).isEqualTo("+43000");
        assertThat(sent.getTo()).isEqualTo("+43111");
    }

    @Test
    void throwsWhenVonageRejectsMessage() throws Exception {
        var response = response(MessageStatus.INVALID_CREDENTIALS, null, "Bad credentials");
        when(smsClient.submitMessage(any(Message.class))).thenReturn(response);

        assertThatThrownBy(() -> sender.send(new SmsRequest("+43111", "+43000", "hi", Map.of())))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("INVALID_CREDENTIALS")
                .hasMessageContaining("Bad credentials");
    }

    private static SmsSubmissionResponse response(MessageStatus status, String id, String errorText) {
        var message = mock(SmsSubmissionResponseMessage.class);
        when(message.getStatus()).thenReturn(status);
        if (id != null) {
            when(message.getId()).thenReturn(id);
        }
        if (errorText != null) {
            when(message.getErrorText()).thenReturn(errorText);
        }
        var response = mock(SmsSubmissionResponse.class);
        when(response.getMessages()).thenReturn(List.of(message));
        return response;
    }
}
