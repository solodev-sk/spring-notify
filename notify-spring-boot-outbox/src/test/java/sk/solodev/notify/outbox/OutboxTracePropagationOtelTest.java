package sk.solodev.notify.outbox;

import io.micrometer.tracing.otel.bridge.OtelCurrentTraceContext;
import io.micrometer.tracing.otel.bridge.OtelPropagator;
import io.micrometer.tracing.otel.bridge.OtelTracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sk.solodev.notify.NotificationRequest;
import sk.solodev.notify.test.RecordingNotifier;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves trace context propagation across the outbox boundary using a real OpenTelemetry SDK: the
 * delivery span created by {@link OutboxRelay} becomes a child of the request span that enqueued,
 * even though they run in different transactions and possibly different threads.
 */
class OutboxTracePropagationOtelTest {

    record SampleRequest(String to, String body) implements NotificationRequest { }

    private static final OutboxProperties PROPERTIES = new OutboxProperties(
            Duration.ofSeconds(5), 100, 7, Duration.ofSeconds(10), Duration.ofMinutes(10),
            "notification_outbox");

    private InMemorySpanExporter spanExporter;
    private OtelTracer tracer;
    private MicrometerOutboxTracePropagator propagator;

    @BeforeEach
    void setUp() {
        spanExporter = InMemorySpanExporter.create();
        var tracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(SimpleSpanProcessor.create(spanExporter))
                .build();
        var otelTracer = tracerProvider.get("test");
        var propagators = ContextPropagators.create(W3CTraceContextPropagator.getInstance());

        tracer = new OtelTracer(otelTracer, new OtelCurrentTraceContext(), null);
        propagator = new MicrometerOutboxTracePropagator(tracer,
                new OtelPropagator(propagators, otelTracer));
    }

    @Test
    void theDeliverySpanInheritsTheTraceIdFromTheEnqueuingRequest() {
        // Start a span representing the incoming request
        var requestSpan = tracer.nextSpan().name("incoming-request").start();
        String originalTraceId = requestSpan.context().traceId();

        // Enqueue within the request span's scope
        OutboxEntry entry;
        var store = new RecordingOutboxStore();
        var enqueuer = new DefaultOutboxNotifier(store, JsonMapper.builder().build(),
                PROPERTIES, propagator);
        try (var _ = tracer.withSpan(requestSpan)) {
            enqueuer.enqueue(new SampleRequest("+421900123456", "hello"));
        } finally {
            requestSpan.end();
        }

        // The entry should have captured the trace context
        entry = store.inserted.getFirst();
        assertThat(entry.traceContext()).isNotNull();

        // Now deliver it through the relay
        var notifier = new RecordingNotifier();
        var relay = new OutboxRelay(notifier, new RecordingOutboxStore(entry),
                JsonMapper.builder().build(), PROPERTIES, propagator);

        relay.poll();

        // Export spans and find the delivery span
        var exportedSpans = spanExporter.getFinishedSpanItems();

        // The request span and the delivery span should both be present
        assertThat(exportedSpans).hasSizeGreaterThanOrEqualTo(2);

        // Find the delivery span (it's the one created after the request span ended)
        var deliverySpan = exportedSpans.stream()
                .filter(span -> !span.getSpanId().equals(requestSpan.context().spanId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Delivery span not found"));

        // The critical assertion: the delivery span must have the same trace ID as the request
        assertThat(deliverySpan.getTraceId())
                .as("Delivery span should inherit the request's trace ID")
                .isEqualTo(originalTraceId);
    }

}
