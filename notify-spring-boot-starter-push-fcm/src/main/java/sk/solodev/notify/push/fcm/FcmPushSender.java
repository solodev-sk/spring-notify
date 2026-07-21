package sk.solodev.notify.push.fcm;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import sk.solodev.notify.push.PushRequest;
import sk.solodev.notify.push.PushSender;

/**
 * {@link PushSender} backed by the Firebase Admin SDK (FCM).
 *
 * @author Dominik Kovács
 * @since 1.0.0
 */
public class FcmPushSender implements PushSender {

    private final FirebaseMessaging messaging;

    public FcmPushSender(FirebaseMessaging messaging) {
        this.messaging = messaging;
    }

    @Override
    public String send(PushRequest request) throws FirebaseMessagingException {
        var builder = Message.builder()
                .setToken(request.to())
                .setNotification(Notification.builder()
                        .setTitle(request.title())
                        .setBody(request.body())
                        .build());
        request.attributes().forEach((key, value) -> builder.putData(key, String.valueOf(value)));
        return messaging.send(builder.build());
    }
}
