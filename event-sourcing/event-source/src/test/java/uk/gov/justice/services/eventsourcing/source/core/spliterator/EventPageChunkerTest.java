package uk.gov.justice.services.eventsourcing.source.core.spliterator;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;

public class EventPageChunkerTest {

    @Test
    public void shouldSplitPagesOfEventsByStreamId() {

        final TestEventProvider eventProvider = new TestEventProvider(10, 5, 150);
        final EventPageChunker eventPageChunker = new EventPageChunker(eventProvider, 1, 150, 100);

        long accumulatedSize = 0;
        int streamsProcessed = 0;

        while (eventPageChunker.hasNext()) {
            accumulatedSize = accumulatedSize + eventPageChunker.nextStream().size();
            streamsProcessed++;
        }

        System.out.println("Streams processed = " + streamsProcessed);
        assertThat(accumulatedSize, is(150L));
    }

    @Test
    public void shouldReturnChunkOfSameStreamIdAndInSequenceVersions() {

        final TestEventProvider eventProvider = new TestEventProvider(1, 5, 150);
        final EventPageChunker eventPageChunker = new EventPageChunker(eventProvider, 1, 150, 100);

        while (eventPageChunker.hasNext()) {
            final Iterator<JsonEnvelope> jsonEnvelopeIterator = eventPageChunker.nextStream().iterator();

            final JsonEnvelope startEnvelope = jsonEnvelopeIterator.next();

            final UUID streamId = startEnvelope.metadata().streamId().get();
            long position = startEnvelope.metadata().position().get();

            while (jsonEnvelopeIterator.hasNext()){
                final JsonEnvelope jsonEnvelope = jsonEnvelopeIterator.next();

                assertThat(jsonEnvelope.metadata().streamId().get(), is(streamId));
                assertThat(jsonEnvelope.metadata().position().get(), is(++position));
            }
        }
    }
}