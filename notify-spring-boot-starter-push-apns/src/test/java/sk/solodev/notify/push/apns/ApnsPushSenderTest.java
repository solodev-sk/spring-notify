package sk.solodev.notify.push.apns;

import com.eatthepath.pushy.apns.ApnsClient;
import com.eatthepath.pushy.apns.PushNotificationResponse;
import com.eatthepath.pushy.apns.util.SimpleApnsPushNotification;
import com.eatthepath.pushy.apns.util.concurrent.PushNotificationFuture;
import org.junit.jupiter.api.Test;
import sk.solodev.notify.push.PushRequest;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ApnsPushSenderTest {

    private final ApnsClient client = mock(ApnsClient.class);
    private final ApnsPushSender sender = new ApnsPushSender(client, "com.example.app");

    private void stubResponse(PushNotificationResponse<SimpleApnsPushNotification> response) {
        var future = new PushNotificationFuture<SimpleApnsPushNotification, PushNotificationResponse<SimpleApnsPushNotification>>(
                mock(SimpleApnsPushNotification.class));
        future.complete(response);
        when(client.sendNotification(any(SimpleApnsPushNotification.class))).thenReturn(future);
    }

    @SuppressWarnings("unchecked")
    @Test
    void sendsAcceptedNotificationAndReturnsApnsId() throws Exception {
        var id = UUID.randomUUID();
        var response = (PushNotificationResponse<SimpleApnsPushNotification>) mock(PushNotificationResponse.class);
        when(response.isAccepted()).thenReturn(true);
        when(response.getApnsId()).thenReturn(id);
        stubResponse(response);

        var result = sender.send(PushRequest.builder().to("device-token").title("Shipped").body("On its way").build());

        assertThat(result).isEqualTo(id.toString());
    }

    @SuppressWarnings("unchecked")
    @Test
    void throwsWhenApnsRejects() {
        var response = (PushNotificationResponse<SimpleApnsPushNotification>) mock(PushNotificationResponse.class);
        when(response.isAccepted()).thenReturn(false);
        when(response.getRejectionReason()).thenReturn(Optional.of("BadDeviceToken"));
        stubResponse(response);

        assertThatThrownBy(() -> sender.send(PushRequest.builder().to("bad").title("t").body("b").build()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("BadDeviceToken");
    }
}
