package sk.solodev.notify;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultNotifierTest {

    private final NotificationRequest request = new TestRequest("r1");

    private ChannelAdapter<?> recordingAdapter(List<NotificationRequest> seen) {
        var adapter = mock(ChannelAdapter.class);
        when(adapter.supports(any())).thenReturn(true);
        when(adapter.deliver(any())).thenAnswer(invocation -> {
            seen.add(invocation.getArgument(0));
            return "MID";
        });
        return adapter;
    }

    private DefaultNotifier service(List<ChannelAdapter<?>> adapters, List<NotificationInterceptor> interceptors) {
        return new DefaultNotifier(adapters, new DefaultAdapterResolver(), interceptors);
    }

    @Test
    void sendResolvesTheAdapterAndReturnsItsMessageId() {
        var seen = new ArrayList<NotificationRequest>();
        var svc = service(List.of(recordingAdapter(seen)), List.of());

        var messageId = svc.notify(request);

        assertThat(messageId).isEqualTo("MID");
        assertThat(seen).hasSize(1);
    }

    @Test
    void interceptorsRunInOrderAndWrapTheAdapterCall() {
        var calls = new ArrayList<String>();
        NotificationInterceptor a = (r, c) -> {
            calls.add("a-before");
            var id = c.proceed(r);
            calls.add("a-after");
            return id;
        };
        NotificationInterceptor b = (r, c) -> {
            calls.add("b-before");
            var id = c.proceed(r);
            calls.add("b-after");
            return id;
        };
        var svc = service(List.of(recordingAdapter(new ArrayList<>())), List.of(a, b));

        svc.notify(request);

        assertThat(calls).containsExactly("a-before", "b-before", "b-after", "a-after");
    }

    @Test
    void anInterceptorCanShortCircuitWithoutCallingTheAdapter() {
        var seen = new ArrayList<NotificationRequest>();
        NotificationInterceptor shortCircuit = (_, _) -> "SHORT";
        var svc = service(List.of(recordingAdapter(seen)), List.of(shortCircuit));

        var messageId = svc.notify(request);

        assertThat(messageId).isEqualTo("SHORT");
        assertThat(seen).isEmpty();
    }
}
