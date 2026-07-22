package sk.solodev.notify.push.fcm;

import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.ApnsConfig;
import com.google.firebase.messaging.Aps;
import com.google.firebase.messaging.ApsAlert;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import sk.solodev.notify.push.PushRequest;
import sk.solodev.notify.push.PushSender;

import java.time.Clock;

/**
 * {@link PushSender} backed by the Firebase Admin SDK (FCM). FCM is a multi-platform router: a
 * token may target Android or iOS, so {@code priority}, {@code collapseKey}, and {@code ttl} are
 * applied to both the {@link AndroidConfig} and {@link ApnsConfig} blocks — FCM uses whichever
 * matches the recipient device.
 *
 * @author Dominik Kovács
 * @since 1.0.0
 */
public class FcmPushSender implements PushSender {

    private final FirebaseMessaging messaging;

    private final Clock clock;

    public FcmPushSender(FirebaseMessaging messaging) {
        this(messaging, Clock.systemDefaultZone());
    }

    public FcmPushSender(FirebaseMessaging messaging, Clock clock) {
        this.messaging = messaging;
        this.clock = clock;
    }

    @Override
    public String send(PushRequest request) throws FirebaseMessagingException {
        var builder = Message.builder()
                .setToken(request.to())
                .setNotification(Notification.builder()
                        .setTitle(request.title())
                        .setBody(request.body())
                        .build())
                .setAndroidConfig(androidConfig(request))
                .setApnsConfig(apnsConfig(request));
        request.attributes().forEach((key, value) -> builder.putData(key, String.valueOf(value)));
        return messaging.send(builder.build());
    }

    private static AndroidConfig androidConfig(PushRequest request) {
        var android = AndroidConfig.builder()
                .setPriority(switch (request.priority()) {
                    case HIGH -> AndroidConfig.Priority.HIGH;
                    case NORMAL -> AndroidConfig.Priority.NORMAL;
                });
        if (request.collapseKey() != null) {
            android.setCollapseKey(request.collapseKey());
        }
        if (request.ttl() != null) {
            android.setTtl(request.ttl().toMillis());
        }
        return android.build();
    }

    private ApnsConfig apnsConfig(PushRequest request) {
        // FCM requires an aps payload on the APNs block; carry the same alert as the notification
        var apns = ApnsConfig.builder()
                .setAps(Aps.builder()
                        .setAlert(ApsAlert.builder()
                                .setTitle(request.title())
                                .setBody(request.body())
                                .build())
                        .build())
                .putHeader("apns-priority", switch (request.priority()) {
                    case HIGH -> "10";
                    case NORMAL -> "5";
                });
        if (request.collapseKey() != null) {
            apns.putHeader("apns-collapse-id", request.collapseKey());
        }
        if (request.ttl() != null) {
            apns.putHeader("apns-expiration", String.valueOf(clock.instant().plus(request.ttl()).getEpochSecond()));
        }
        return apns.build();
    }
}
