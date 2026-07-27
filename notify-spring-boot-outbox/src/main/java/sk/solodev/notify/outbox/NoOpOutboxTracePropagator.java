package sk.solodev.notify.outbox;

import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

/**
 * {@link OutboxTracePropagator} used when tracing is not configured: captures nothing and runs
 * deliveries unchanged.
 *
 * @author Dominik Kovács
 * @since 1.1.0
 */
public class NoOpOutboxTracePropagator implements OutboxTracePropagator {

    @Override
    public @Nullable String capture() {
        return null;
    }

    @Override
    public <T> T withRestoredContext(@Nullable String traceContext, Supplier<T> delivery) {
        return delivery.get();
    }
}
