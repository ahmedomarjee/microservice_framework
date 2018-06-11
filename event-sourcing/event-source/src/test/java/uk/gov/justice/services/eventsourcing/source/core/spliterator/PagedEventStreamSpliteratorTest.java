package uk.gov.justice.services.eventsourcing.source.core.spliterator;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.function.Consumer;
import java.util.stream.Stream;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.internal.verification.Times;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PagedEventStreamSpliteratorTest {

    @Mock
    private PagedEventStream pagedEventStream;

    @InjectMocks
    private PagedEventStreamSpliterator pagedEventStreamSpliterator;

    @SuppressWarnings("unchecked")
    @Test
    public void shouldReturnTrueAndConsumeNextStream() {
        final Stream<JsonEnvelope> stream = mock(Stream.class);
        final Consumer<Stream<JsonEnvelope>> consumer = mock(Consumer.class);

        when(pagedEventStream.hasNext()).thenReturn(true);
        when(pagedEventStream.nextStream()).thenReturn(stream);

        final boolean result = pagedEventStreamSpliterator.tryAdvance(consumer);

        assertThat(result, is(true));
        verify(consumer, new Times(1)).accept(stream);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldReturnFalseAndNotConsumeStream() {
        final Consumer<Stream<JsonEnvelope>> consumer = mock(Consumer.class);

        when(pagedEventStream.hasNext()).thenReturn(false);

        final boolean result = pagedEventStreamSpliterator.tryAdvance(consumer);

        assertThat(result, is(false));
        verifyZeroInteractions(consumer);
    }
}
