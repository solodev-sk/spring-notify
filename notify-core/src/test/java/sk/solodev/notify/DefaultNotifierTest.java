package sk.solodev.notify;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

    @Test
    void notifyAsyncRunsTheWholePipelineOnTheConfiguredExecutorAndReturnsTheMessageId() throws Exception {
        var executorThreads = new ArrayList<String>();
        Executor executor = task -> Executors.newSingleThreadExecutor(r -> new Thread(r, "notify-async"))
                .execute(() -> {
                    executorThreads.add(Thread.currentThread().getName());
                    task.run();
                });
        var svc = new DefaultNotifier(List.of(recordingAdapter(new ArrayList<>())),
                new DefaultAdapterResolver(), List.of(), executor);

        var messageId = svc.notifyAsync(request).get();

        assertThat(messageId).isEqualTo("MID");
        assertThat(executorThreads).containsExactly("notify-async");
    }

    @Test
    void notifyAsyncCompletesExceptionallyWhenDeliveryFails() {
        NotificationInterceptor boom = (_, _) -> {
            throw new NotificationDeliveryException("delivery failed", request, new IllegalStateException("down"));
        };
        var svc = service(List.of(recordingAdapter(new ArrayList<>())), List.of(boom));

        assertThatThrownBy(() -> svc.notifyAsync(request).get())
                .hasCauseInstanceOf(NotificationDeliveryException.class);
    }
}
