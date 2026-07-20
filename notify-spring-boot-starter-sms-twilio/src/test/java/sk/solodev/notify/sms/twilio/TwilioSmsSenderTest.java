package sk.solodev.notify.sms.twilio;

import com.twilio.rest.api.v2010.account.Message;
import com.twilio.rest.api.v2010.account.MessageCreator;
import com.twilio.type.PhoneNumber;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import sk.solodev.notify.sms.SmsRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class TwilioSmsSenderTest {

    @Test
    void mapsRequestToSdkAndReturnsSid() {
        var restClient = mock(com.twilio.http.TwilioRestClient.class);
        var sender = new TwilioSmsSender(restClient);
        var creator = mock(MessageCreator.class);
        var message = mock(Message.class);
        when(message.getSid()).thenReturn("SM555");
        when(creator.create(restClient)).thenReturn(message);

        try (MockedStatic<Message> statics = mockStatic(Message.class)) {
            statics.when(() -> Message.creator(any(PhoneNumber.class), any(PhoneNumber.class), any(String.class)))
                    .thenReturn(creator);

            var sid = sender.send(new SmsRequest("+43111", "+43000", "hi", java.util.Map.of()));

            assertThat(sid).isEqualTo("SM555");
        }
    }
}
