package sk.solodev.notify.push.apns;

import com.eatthepath.pushy.apns.ApnsClient;
import com.eatthepath.pushy.apns.DeliveryPriority;
import com.eatthepath.pushy.apns.PushNotificationResponse;
import com.eatthepath.pushy.apns.util.SimpleApnsPayloadBuilder;
import com.eatthepath.pushy.apns.util.SimpleApnsPushNotification;
import com.eatthepath.pushy.apns.util.TokenUtil;
import sk.solodev.notify.push.PushRequest;
import sk.solodev.notify.push.PushSender;

import java.time.Clock;

/**
 * {@link PushSender} backed by the Apple Push Notification service (Pushy, token auth). Builds an
 * {@code aps} alert payload from the request, sends to the configured topic, and returns the APNs id.
 *
 * @author Dominik Kovács
 * @since 1.0.0
 */
public class ApnsPushSender implements PushSender {

    private final ApnsClient client;

    private final String topic;

    private final Clock clock;

    public ApnsPushSender(ApnsClient client, String topic) {
        this(client, topic, Clock.systemDefaultZone());
    }

    public ApnsPushSender(ApnsClient client, String topic, Clock clock) {
        this.client = client;
        this.topic = topic;
        this.clock = clock;
    }

    @Override
    public String send(PushRequest request) throws Exception {
        var payload = new SimpleApnsPayloadBuilder()
                .setAlertTitle(request.title())
                .setAlertBody(request.body());
        request.attributes().forEach(payload::addCustomProperty);

        var priority = switch (request.priority()) {
            case HIGH -> DeliveryPriority.IMMEDIATE;
            case NORMAL -> DeliveryPriority.CONSERVE_POWER;
        };
        var expiration = request.ttl() != null ? clock.instant().plus(request.ttl()) : null;
        var notification = new SimpleApnsPushNotification(
                TokenUtil.sanitizeTokenString(request.to()), topic, payload.build(),
                expiration, priority, request.collapseKey());

        PushNotificationResponse<SimpleApnsPushNotification> response =
                client.sendNotification(notification).get();

        if (!response.isAccepted()) {
            throw new RuntimeException("APNs rejected the notification: "
                    + response.getRejectionReason().orElse("unknown"));
        }
        return response.getApnsId().toString();
    }
}
