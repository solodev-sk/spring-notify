package sk.solodev.notify;

import org.junit.jupiter.api.Test;
import sk.solodev.notify.interceptor.ChannelInterceptor;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class ChannelInterceptorTest {

    private record OtherRequest() implements NotificationRequest {
    }

    private final AtomicBoolean ran = new AtomicBoolean(false);

    private final ChannelInterceptor<TestRequest> interceptor = new ChannelInterceptor<>(TestRequest.class) {
        @Override
        protected String interceptForChannel(TestRequest request, Chain chain) {
            ran.set(true);
            return "handled-" + request.id();
        }
    };

    @Test
    void runsForItsOwnRequestType() {
        var result = interceptor.intercept(new TestRequest("r1"), r -> "chain");

        assertThat(ran).isTrue();
        assertThat(result).isEqualTo("handled-r1");
    }

    @Test
    void passesOtherRequestTypesStraightDownTheChain() {
        var result = interceptor.intercept(new OtherRequest(), r -> "chain");

        assertThat(ran).isFalse();
        assertThat(result).isEqualTo("chain");
    }
}
