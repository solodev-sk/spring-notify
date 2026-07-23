package sk.solodev.notify;

import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.Order;
import sk.solodev.notify.dispatch.ChannelAdapter;
import sk.solodev.notify.dispatch.DefaultAdapterResolver;
import sk.solodev.notify.dispatch.DefaultNotifier;
import sk.solodev.notify.interceptor.NotificationInterceptor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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
    void notifyAsyncSubmitsViaExecutorExecuteSoATaskDecoratorSeesTheSend() throws Exception {
        // proves the send runs through Executor.execute(...) — the hook Spring/Boot use to wrap
        // the task with a ContextPropagatingTaskDecorator (tracing, MDC, security) on the worker thread.
        var decoratedAround = new ArrayList<String>();
        var pool = Executors.newSingleThreadExecutor();
        Executor decoratingExecutor = task -> pool.execute(() -> {
            decoratedAround.add("before");
            task.run();
            decoratedAround.add("after");
        });
        var svc = new DefaultNotifier(List.of(recordingAdapter(new ArrayList<>())),
                new DefaultAdapterResolver(), List.of(), decoratingExecutor);

        svc.notifyAsync(request).get();
        // notifyAsync completes inside task.run(), before the decorator appends "after" on the
        // pool thread — await termination so that write has happened (and is visible) before asserting.
        pool.shutdown();
        assertThat(pool.awaitTermination(5, TimeUnit.SECONDS)).isTrue();

        assertThat(decoratedAround).containsExactly("before", "after");
    }

    @Test
    void sortsInterceptorsByOrderRegardlessOfConstructionOrder() {
        var calls = new ArrayList<String>();

        @Order(2)
        class Second implements NotificationInterceptor {
            public String intercept(NotificationRequest r, Chain c) {
                calls.add("second");
                return c.proceed(r);
            }
        }

        @Order(1)
        class First implements NotificationInterceptor {
            public String intercept(NotificationRequest r, Chain c) {
                calls.add("first");
                return c.proceed(r);
            }
        }

        // passed lowest-priority first; the notifier must still run them in @Order sequence
        var svc = service(List.of(recordingAdapter(new ArrayList<>())), List.of(new Second(), new First()));

        svc.notify(request);

        assertThat(calls).containsExactly("first", "second");
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
