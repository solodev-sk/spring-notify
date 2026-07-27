package sk.solodev.notify.outbox;

import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Carries trace context across the outbox's durable boundary: captured on the thread that enqueues
 * a notification, restored on the relay thread that eventually delivers it, so both appear in one
 * trace even though a database round-trip and an arbitrary delay separate them.
 *
 * <p>Implementations must never let a propagation problem break delivery — a context that cannot be
 * captured or restored degrades to an unparented delivery span.
 *
 * @author Dominik Kovács
 * @since 1.1.0
 */
public interface OutboxTracePropagator {

    /**
     * Serialize the trace context active on the calling thread.
     *
     * @return the serialized context, or {@code null} when there is nothing to propagate
     */
    @Nullable String capture();

    /**
     * Run {@code delivery} with {@code traceContext} restored, so spans it records descend from the
     * context captured at enqueue time.
     *
     * @param traceContext the context from {@link #capture()}; {@code null} runs {@code delivery} as-is
     * @param delivery     the delivery to perform
     * @return whatever {@code delivery} returns
     */
    <T> T withRestoredContext(@Nullable String traceContext, Supplier<T> delivery);
}
