package uk.gov.justice.services.eventsourcing.source.core.spliterator;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.junit.Test;

public class EventPageChunkerTest {

    @Test
    public void shouldSplitPagesOfEventsByStreamId() {

        final EventFactory eventFactory = new EventFactory(
                10,
                5);

        final List<JsonEnvelope> allEvents = eventFactory.generateEvents(150);
        final TestEventProvider eventProvider = new TestEventProvider(allEvents);
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

        final EventFactory eventFactory = new EventFactory(
                1,
                5);

        final List<JsonEnvelope> allEvents = eventFactory.generateEvents(150);
        final TestEventProvider eventProvider = new TestEventProvider(allEvents);
        final EventPageChunker eventPageChunker = new EventPageChunker(eventProvider, 1, 150, 100);

        long position = 1L;

        while (eventPageChunker.hasNext()) {
            final List<JsonEnvelope> jsonEnvelopes = eventPageChunker.nextStream();
            final UUID streamId = jsonEnvelopes.get(0).metadata().streamId().get();
            final Iterator<JsonEnvelope> jsonEnvelopeIterator = jsonEnvelopes.iterator();

            while (jsonEnvelopeIterator.hasNext()) {
                final JsonEnvelope jsonEnvelope = jsonEnvelopeIterator.next();
                final UUID actualStreamId = jsonEnvelope.metadata().streamId().get();
                final Long actualPosition = jsonEnvelope.metadata().position().get();

                assertThat(actualStreamId, is(streamId));
                assertThat(actualPosition, is(position++));
            }
        }
    }
}