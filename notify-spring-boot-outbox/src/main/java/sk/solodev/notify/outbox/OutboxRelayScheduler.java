package sk.solodev.notify.outbox;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.scheduling.TaskScheduler;

import java.time.Duration;
import java.util.concurrent.ScheduledFuture;

/**
 * Drives {@link OutboxRelay#poll()} at a fixed delay on the application's {@link TaskScheduler},
 * using the interval from {@link OutboxProperties}.
 *
 * @author Dominik Kovács
 * @since 1.1.0
 */
public class OutboxRelayScheduler implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelayScheduler.class);

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
            log.info("Notification outbox relay started, polling every {}", pollInterval);
        }
    }

    @Override
    public void stop() {
        if (polling != null) {
            polling.cancel(false);
            polling = null;
            log.debug("Notification outbox relay stopped");
        }
    }

    @Override
    public boolean isRunning() {
        return polling != null;
    }
}