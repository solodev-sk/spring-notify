package sk.solodev.notify.test;

import org.jspecify.annotations.Nullable;
import sk.solodev.notify.NotificationRequest;
import sk.solodev.notify.Notifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * A {@link Notifier} test double that records every send instead of delivering it, so tests can
 * assert what would have been sent without a real provider. Drop it in wherever a {@code Notifier}
 * is injected:
 *
 * <pre>{@code
 * var notifier = new RecordingNotifier();
 * orderService.onShipped(order);                 // calls notifier.notify(...)
 *
 * assertThat(notifier.sent(SmsRequest.class)).hasSize(1);
 * assertThat(notifier.lastSent()).isInstanceOf(SmsRequest.class);
 * }</pre>
 *
 * <p>{@link #notify} returns a message id (a fixed {@code "test-message-id"} by default; customise
 * with {@link #returning}) and records the request. {@link #notifyAsync} runs synchronously and
 * returns an already-completed future, so async sends are deterministic in tests. To exercise
 * failure handling, arm it with {@link #failWith}.
 *
 * <p>Not thread-safe; intended for single-threaded test use.
 */
public class RecordingNotifier implements Notifier {

    private final List<NotificationRequest> sent = new ArrayList<>();

    private Function<NotificationRequest, String> messageId = _ -> "test-message-id";

    private @Nullable RuntimeException failure;

    @Override
    public String notify(NotificationRequest request) {
        sent.add(request);
        if (failure != null) {
            throw failure;
        }
        return messageId.apply(request);
    }

    @Override
    public CompletableFuture<String> notifyAsync(NotificationRequest request) {
        try {
            return CompletableFuture.completedFuture(notify(request));
        }
        catch (RuntimeException ex) {
            return CompletableFuture.failedFuture(ex);
        }
    }

    /** Every request recorded so far, in send order. */
    public List<NotificationRequest> sent() {
        return List.copyOf(sent);
    }

    /** Recorded requests of the given type, in send order. */
    public <T extends NotificationRequest> List<T> sent(Class<T> type) {
        return sent.stream().filter(type::isInstance).map(type::cast).toList();
    }

    /** The most recently recorded request, or empty if nothing was sent. */
    public Optional<NotificationRequest> lastSent() {
        return sent.isEmpty() ? Optional.empty() : Optional.of(sent.getLast());
    }

    /** How many requests were recorded. */
    public int count() {
        return sent.size();
    }

    /** Whether nothing has been sent. */
    public boolean nothingSent() {
        return sent.isEmpty();
    }

    /** Forget all recorded requests. */
    public void clear() {
        sent.clear();
    }

    /** Return this message id from {@link #notify} for every request. */
    public RecordingNotifier returning(String messageId) {
        this.messageId = _ -> messageId;
        return this;
    }

    /** Derive the returned message id from each request. */
    public RecordingNotifier returning(Function<NotificationRequest, String> messageId) {
        this.messageId = messageId;
        return this;
    }

    /** Make every subsequent send record the request and then throw this exception. */
    public RecordingNotifier failWith(RuntimeException failure) {
        this.failure = failure;
        return this;
    }
}
