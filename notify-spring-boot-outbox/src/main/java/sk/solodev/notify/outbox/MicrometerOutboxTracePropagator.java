package sk.solodev.notify.outbox;

import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Propagates trace context through the outbox using the configured {@link Propagator} — in practice
 * a W3C {@code traceparent} string, the same mechanism messaging instrumentation uses to link a
 * producer to a consumer.
 *
 * @author Dominik Kovács
 * @since 1.1.0
 */
public class MicrometerOutboxTracePropagator implements OutboxTracePropagator {

    private static final Logger log = LoggerFactory.getLogger(MicrometerOutboxTracePropagator.class);

    private final Tracer tracer;

    private final Propagator propagator;

    public MicrometerOutboxTracePropagator(Tracer tracer, Propagator propagator) {
        this.tracer = tracer;
        this.propagator = propagator;
    }

    @Override
    public Optional<String> capture() {
        var span = tracer.currentSpan();
        if (span == null) {
            return Optional.empty();
        }
        var carrier = new HashMap<String, String>();
        propagator.inject(span.context(), carrier, Map::put);
        return carrier.isEmpty() ? Optional.empty() : Optional.of(serialize(carrier));
    }

    @Override
    public <T> T withRestoredContext(@Nullable String traceContext, Supplier<T> delivery) {
        if (traceContext == null) {
            return delivery.get();
        }
        var carrier = deserialize(traceContext);
        if (carrier.isEmpty()) {
            return delivery.get();
        }
        // Propagation is best-effort: a context we cannot parse must not stop the delivery.
        try {
            var span = propagator.extract(carrier, Map::get).start();
            try (var _ = tracer.withSpan(span)) {
                return delivery.get();
            } finally {
                span.end();
            }
        } catch (RuntimeException ex) {
            log.debug("Could not restore outbox trace context '{}'; delivering unparented",
                    traceContext, ex);
            return delivery.get();
        }
    }

    /** Encodes the carrier as {@code key=value} pairs separated by {@code |}. */
    private static String serialize(Map<String, String> carrier) {
        return carrier.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .reduce((first, second) -> first + "|" + second)
                .orElse("");
    }

    private static Map<String, String> deserialize(String traceContext) {
        var carrier = new HashMap<String, String>();
        for (var pair : traceContext.split("\\|")) {
            var separator = pair.indexOf('=');
            if (separator > 0) {
                carrier.put(pair.substring(0, separator), pair.substring(separator + 1));
            }
        }
        return carrier;
    }
}
