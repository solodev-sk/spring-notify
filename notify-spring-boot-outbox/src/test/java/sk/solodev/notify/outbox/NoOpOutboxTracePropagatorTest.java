package sk.solodev.notify.outbox;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NoOpOutboxTracePropagatorTest {

    private final OutboxTracePropagator propagator = new NoOpOutboxTracePropagator();

    @Test
    void capturesNothing() {
        assertThat(propagator.capture()).isEmpty();
    }

    @Test
    void runsTheDeliveryUnchanged() {
        assertThat(propagator.withRestoredContext(null, () -> "delivered")).isEqualTo("delivered");
    }

    @Test
    void runsTheDeliveryEvenWhenGivenAContextItCannotUse() {
        assertThat(propagator.withRestoredContext("00-abc-def-01", () -> "delivered"))
                .isEqualTo("delivered");
    }
}
