package sk.solodev.notify.push.apns;

import com.eatthepath.pushy.apns.ApnsClient;
import com.eatthepath.pushy.apns.PushNotificationResponse;
import com.eatthepath.pushy.apns.util.ApnsPayloadBuilder;
import com.eatthepath.pushy.apns.util.SimpleApnsPayloadBuilder;
import com.eatthepath.pushy.apns.util.SimpleApnsPushNotification;
import com.eatthepath.pushy.apns.util.TokenUtil;
import sk.solodev.notify.push.PushRequest;
import sk.solodev.notify.push.PushSender;

/**
 * {@link PushSender} backed by the Apple Push Notification service (Pushy, token auth). Builds an
 * {@code aps} alert payload from the request, sends to the configured topic, and returns the APNs id.
 */
public class ApnsPushSender implements PushSender {

    private final ApnsClient client;

    private final String topic;

    public ApnsPushSender(ApnsClient client, String topic) {
        this.client = client;
        this.topic = topic;
    }

    @Override
    public String send(PushRequest request) throws Exception {
        ApnsPayloadBuilder payload = new SimpleApnsPayloadBuilder()
                .setAlertTitle(request.title())
                .setAlertBody(request.body());
        request.attributes().forEach(payload::addCustomProperty);

        var notification = new SimpleApnsPushNotification(
                TokenUtil.sanitizeTokenString(request.to()), topic, payload.build());

        PushNotificationResponse<SimpleApnsPushNotification> response =
                client.sendNotification(notification).get();

        if (!response.isAccepted()) {
            throw new RuntimeException("APNs rejected the notification: "
                    + response.getRejectionReason().orElse("unknown"));
        }
        return response.getApnsId().toString();
    }
}
