package sk.solodev.notify.push.fcm;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import org.junit.jupiter.api.Test;
import sk.solodev.notify.push.PushRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FcmPushSenderTest {

    private final FirebaseMessaging messaging = mock(FirebaseMessaging.class);
    private final FcmPushSender sender = new FcmPushSender(messaging);

    @Test
    void mapsRequestToMessageAndReturnsProviderId() throws Exception {
        when(messaging.send(any(Message.class))).thenReturn("projects/x/messages/42");

        var id = sender.send(PushRequest.builder().to("tok").title("Shipped").body("On its way")
                .attribute("orderId", 7)
                .build());

        assertThat(id).isEqualTo("projects/x/messages/42");
    }

    @Test
    void propagatesSdkFailure() throws Exception {
        var failure = mock(com.google.firebase.messaging.FirebaseMessagingException.class);
        when(messaging.send(any(Message.class))).thenThrow(failure);

        assertThatThrownBy(() -> sender.send(PushRequest.builder().to("tok-9").title("t").body("b").build()))
                .isSameAs(failure);
    }
}
