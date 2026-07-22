package com.example.push.apns;

import com.eatthepath.pushy.apns.DeliveryPriority;
import com.eatthepath.pushy.apns.server.MockApnsServer;
import com.example.push.apns.support.ApnsPayload;
import com.example.push.apns.support.MockApnsServerSupport;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import sk.solodev.notify.push.PushRequest;
import sk.solodev.notify.push.apns.ApnsProperties;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static com.example.push.apns.support.JsonHelper.asJson;
import static com.example.push.apns.support.JsonHelper.fromJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.TestInstance.Lifecycle;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Drives the {@link com.example.push.apns.controller.PushController} through MockMvc and sends real
 * APNs traffic — over TLS and HTTP/2 — into Pushy's in-process {@link MockApnsServer} (wrapped by
 * {@link MockApnsServerSupport}), then asserts the notification the server actually received (token,
 * topic, {@code aps} payload). No Apple account, no device, no cost: the always-on CI net that keeps
 * the example honest.
 *
 * <p>The whole APNs client is built by the real starter from static {@code application.yml}
 * configuration. The mock server and the client share a fixed self-signed certificate under
 * {@code src/test/resources/certs} — the server presents it, and the client trusts it via
 * {@code spring.notify.push.apns.trusted-certificate}, exactly as it would trust a corporate proxy's
 * CA in production. Nothing about the starter's wiring is replaced.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(Lifecycle.PER_CLASS)
class PushApnsMockServerTest {

    private static final Instant NOW = Instant.parse("2026-07-22T12:00:00Z");

    @TestConfiguration(proxyBeanMethods = false)
    static class FixedClockConfig {

        // a fixed Clock lets the starter map ttl to an exact, assertable expiration
        @Bean
        Clock clock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }
    }

    private final MockApnsServerSupport mockServer = MockApnsServerSupport.acceptingAll();

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ApnsProperties apnsProperties;

    @BeforeAll
    void startMockServer() throws Exception {
        mockServer.start(apnsProperties.port());
    }

    @AfterAll
    void stopMockServer() throws Exception {
        mockServer.stop();
    }

    @Test
    void deliversPushNotificationToApns() throws Exception {
        send(PushRequest.builder()
                .to("0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef")
                .title("Shipped")
                .body("Your order is on its way")
                .build());

        var notification = mockServer.awaitNotification();
        assertThat(notification.getToken()).isEqualToIgnoringCase(
                "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef");
        assertThat(notification.getTopic()).isEqualTo(apnsProperties.topic());

        var payload = fromJson(notification.getPayload(), ApnsPayload.class);
        assertThat(payload.aps().alert().title()).isEqualTo("Shipped");
        assertThat(payload.aps().alert().body()).isEqualTo("Your order is on its way");
    }

    @Test
    void carriesCollapseKeyPriorityAndTtl() throws Exception {
        send(PushRequest.builder()
                .to("0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef")
                .title("Flash sale")
                .body("Ends in an hour")
                .collapseKey("promo")
                .priority(PushRequest.Priority.HIGH)
                .ttl(Duration.ofHours(1))
                .build());

        var notification = mockServer.awaitNotification();
        assertThat(notification.getCollapseId()).isEqualTo("promo");
        assertThat(notification.getPriority()).isEqualTo(DeliveryPriority.IMMEDIATE);
        // with the fixed clock, ttl maps to an exact expiration of NOW + 1h
        assertThat(notification.getExpiration()).isEqualTo(NOW.plus(Duration.ofHours(1)));
    }

    @Test
    void carriesCustomAttributesInThePayload() throws Exception {
        send(PushRequest.builder()
                .to("00000000000000000000000000000000000000000000000000000000000000ff")
                .title("Sale")
                .body("50% off today")
                .attribute("campaign", "summer-sale")
                .build());

        var notification = mockServer.awaitNotification();
        var payload = fromJson(notification.getPayload(), ApnsPayload.class);
        assertThat(payload.custom()).containsEntry("campaign", "summer-sale");
    }

    private void send(PushRequest request) throws Exception {
        mockMvc.perform(post("/push")
                        .contentType(APPLICATION_JSON)
                        .content(asJson(request)))
                .andExpect(status().isOk());
    }
}
