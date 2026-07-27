package sk.solodev.notify.outbox;

import org.jspecify.annotations.Nullable;
import org.springframework.context.SmartLifecycle;
import org.springframework.scheduling.TaskScheduler;

import java.time.Duration;
import java.util.concurrent.ScheduledFuture;

/**
 * Drives {@link OutboxRelay#poll()} at a fixed delay on the application's {@link TaskScheduler},
 * using the interval from {@link OutboxProperties}.
 *
 * @author Dominik Kovács
 * @since 1.0.1
 */
public class OutboxRelayScheduler implements SmartLifecycle {

    private final OutboxRelay relay;

    private final TaskScheduler taskScheduler;

    private final Duration pollInterval;

    private @Nullable ScheduledFuture<?> polling;

    public OutboxRelayScheduler(OutboxRelay relay, TaskScheduler taskScheduler, Duration pollInterval) {
        this.relay = relay;
        this.taskScheduler = taskScheduler;
        this.pollInterval = pollInterval;
    }

    @Override
    public void start() {
        if (polling == null) {
            polling = taskScheduler.scheduleWithFixedDelay(relay::poll, pollInterval);
        }
    }

    @Override
    public void stop() {
        if (polling != null) {
            polling.cancel(false);
            polling = null;
        }
    }

    @Override
    public boolean isRunning() {
        return polling != null;
    }
}