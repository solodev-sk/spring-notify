package com.example.push.apns.support;

import com.eatthepath.pushy.apns.ApnsPushNotification;
import com.eatthepath.pushy.apns.server.AcceptAllPushNotificationHandlerFactory;
import com.eatthepath.pushy.apns.server.MockApnsServer;
import com.eatthepath.pushy.apns.server.MockApnsServerBuilder;
import com.eatthepath.pushy.apns.server.ParsingMockApnsServerListenerAdapter;
import com.eatthepath.pushy.apns.server.PushNotificationHandlerFactory;
import com.eatthepath.pushy.apns.server.RejectedNotificationException;
import com.eatthepath.pushy.apns.server.RejectionReason;
import org.springframework.core.io.ClassPathResource;

import java.time.Instant;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Wraps Pushy's in-process {@link MockApnsServer}: starts it on a fixed port with a fixed
 * self-signed certificate (shared with the client via {@code trusted-certificate}) and records what
 * arrived so tests can assert on it. By default it accepts every notification; use
 * {@link #rejecting(RejectionReason)} to simulate an APNs rejection.
 */
public final class MockApnsServerSupport {

    private final LinkedBlockingQueue<ApnsPushNotification> received = new LinkedBlockingQueue<>();

    private final PushNotificationHandlerFactory handlerFactory;

    private MockApnsServer server;

    private MockApnsServerSupport(PushNotificationHandlerFactory handlerFactory) {
        this.handlerFactory = handlerFactory;
    }

    /** A server that accepts every notification and records it for assertion. */
    public static MockApnsServerSupport acceptingAll() {
        return new MockApnsServerSupport(new AcceptAllPushNotificationHandlerFactory());
    }

    /** A server that rejects every notification with the given reason, e.g. a bad device token. */
    public static MockApnsServerSupport rejecting(RejectionReason reason) {
        return new MockApnsServerSupport(_ -> (_, _) -> {
            throw new RejectedNotificationException(reason);
        });
    }

    public void start(int port) throws Exception {
        server = new MockApnsServerBuilder()
                .setServerCredentials(new ClassPathResource("certs/mock-apns.crt").getInputStream(),
                        new ClassPathResource("certs/mock-apns.key").getInputStream(), null)
                .setHandlerFactory(handlerFactory)
                .setListener(new ParsingMockApnsServerListenerAdapter() {
                    @Override
                    public void handlePushNotificationAccepted(ApnsPushNotification notification) {
                        received.add(notification);
                    }

                    @Override
                    public void handlePushNotificationRejected(ApnsPushNotification notification,
                            RejectionReason reason, Instant deviceTokenExpiration) {
                        // recorded only on the accept path; rejections surface as a client-side error
                    }
                })
                .build();
        server.start(port).get();
    }

    public void stop() throws Exception {
        if (server != null) {
            server.shutdown().get();
        }
    }

    /** Waits for the next notification the server received, failing if none arrives in time. */
    public ApnsPushNotification awaitNotification() throws InterruptedException {
        var notification = received.poll(10, TimeUnit.SECONDS);
        assertThat(notification).as("a notification reached the mock APNs server").isNotNull();
        return notification;
    }
}
