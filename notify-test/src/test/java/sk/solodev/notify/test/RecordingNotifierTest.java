package sk.solodev.notify.test;

import org.junit.jupiter.api.Test;
import sk.solodev.notify.NotificationRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RecordingNotifierTest {

    private record Sms(String to) implements NotificationRequest {
    }

    private record Email(String to) implements NotificationRequest {
    }

    private final RecordingNotifier notifier = new RecordingNotifier();

    @Test
    void recordsSendsAndReturnsADefaultMessageId() {
        var id = notifier.notify(new Sms("+43"));

        assertThat(id).isEqualTo("test-message-id");
        assertThat(notifier.count()).isEqualTo(1);
        assertThat(notifier.lastSent()).contains(new Sms("+43"));
    }

    @Test
    void filtersRecordedRequestsByType() {
        notifier.notify(new Sms("+43"));
        notifier.notify(new Email("a@b.com"));
        notifier.notify(new Sms("+44"));

        assertThat(notifier.sent(Sms.class)).containsExactly(new Sms("+43"), new Sms("+44"));
        assertThat(notifier.sent(Email.class)).containsExactly(new Email("a@b.com"));
    }

    @Test
    void reportsNothingSentUntilFirstSend() {
        assertThat(notifier.nothingSent()).isTrue();
        notifier.notify(new Sms("+43"));
        assertThat(notifier.nothingSent()).isFalse();
    }

    @Test
    void returningCustomisesTheMessageId() {
        notifier.returning(r -> "id-" + ((Sms) r).to());

        assertThat(notifier.notify(new Sms("+43"))).isEqualTo("id-+43");
    }

    @Test
    void notifyAsyncRunsSynchronouslyAndCompletes() {
        var future = notifier.notifyAsync(new Sms("+43"));

        assertThat(future).isCompletedWithValue("test-message-id");
        assertThat(notifier.count()).isEqualTo(1);
    }

    @Test
    void failWithRecordsThenThrows() {
        notifier.failWith(new IllegalStateException("boom"));

        assertThatThrownBy(() -> notifier.notify(new Sms("+43")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("boom");
        assertThat(notifier.count()).isEqualTo(1);   // still recorded
    }

    @Test
    void failWithSurfacesThroughNotifyAsyncAsAFailedFuture() {
        notifier.failWith(new IllegalStateException("boom"));

        assertThat(notifier.notifyAsync(new Sms("+43"))).isCompletedExceptionally();
    }

    @Test
    void clearForgetsRecordedRequests() {
        notifier.notify(new Sms("+43"));
        notifier.clear();

        assertThat(notifier.nothingSent()).isTrue();
    }
}
