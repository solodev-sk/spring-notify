package com.example.push.apns;

import com.eatthepath.pushy.apns.server.RejectionReason;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import com.example.push.apns.support.MockApnsServerSupport;
import sk.solodev.notify.NotificationDeliveryException;
import sk.solodev.notify.push.PushRequest;
import sk.solodev.notify.push.apns.ApnsProperties;

import static com.example.push.apns.support.JsonHelper.asJson;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.TestInstance.Lifecycle;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Verifies the failure path: when the APNs gateway rejects a notification, the rejection surfaces
 * through the pipeline as a {@link NotificationDeliveryException} rather than being swallowed. The
 * mock server is configured to reject every notification with {@code BAD_DEVICE_TOKEN}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(Lifecycle.PER_CLASS)
class PushApnsRejectionTest {

    private final MockApnsServerSupport mockServer = MockApnsServerSupport.rejecting(RejectionReason.BAD_DEVICE_TOKEN);

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
    void apnsRejectionSurfacesAsDeliveryException() {
        var request = PushRequest.builder()
                .to("0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef")
                .title("Shipped")
                .body("Your order is on its way")
                .build();

        assertThatThrownBy(() -> mockMvc.perform(post("/push")
                        .contentType(APPLICATION_JSON)
                        .content(asJson(request))))
                .hasCauseInstanceOf(NotificationDeliveryException.class)
                .rootCause()
                .hasMessageContaining("BadDeviceToken");
    }
}
