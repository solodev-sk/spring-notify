package sk.solodev.notify;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultAdapterResolverTest {

    private final DefaultAdapterResolver resolver = new DefaultAdapterResolver();
    private final NotificationRequest request = new TestRequest("r1");

    private ChannelAdapter adapter(boolean shouldSupport) {
        var adapter = mock(ChannelAdapter.class);
        when(adapter.supports(any())).thenReturn(shouldSupport);
        return adapter;
    }

    @Test
    void returnsTheSingleSupportingAdapter() {
        var wanted = adapter(true);

        var resolved = resolver.resolve(request, List.of(adapter(false), wanted));

        assertThat(resolved).isSameAs(wanted);
    }

    @Test
    void throwsWhenNoneSupports() {
        assertThatThrownBy(() -> resolver.resolve(request, List.of(adapter(false))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No ChannelAdapter");
    }

    @Test
    void throwsWhenMoreThanOneSupports() {
        assertThatThrownBy(() -> resolver.resolve(request, List.of(adapter(true), adapter(true))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("install one provider per channel");
    }
}
