package uk.gov.justice.services.eventsourcing.source.core.spliterator;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.junit.Test;

public class PagedEventStreamTest {

    @Test
    public void shouldSplitPagesOfEventsByStreamId() {

        final EventFactory eventFactory = new EventFactory(
                10,
                5);

        final List<JsonEnvelope> allEvents = eventFactory.generateEvents(100);
        final TestEventProvider eventProvider = new TestEventProvider(allEvents);
        final PagedEventStream pagedEventStream = new PagedEventStream(eventProvider, 1, 100, 100);

        int accumulatedSize = 0;
        int numberOfStreamsProcessed = 0;

        while (pagedEventStream.hasNext()) {
            final List<JsonEnvelope> jsonEnvelopes = pagedEventStream
                    .nextStream()
                    .collect(toList());

            final UUID streamId = jsonEnvelopes.get(0).metadata().streamId().get();
            long position = jsonEnvelopes.get(0).metadata().position().get();
            numberOfStreamsProcessed++;

            final Iterator<JsonEnvelope> jsonEnvelopeIterator = jsonEnvelopes.iterator();

            while (jsonEnvelopeIterator.hasNext()) {
                final JsonEnvelope jsonEnvelope = jsonEnvelopeIterator.next();
                final UUID actualStreamId = jsonEnvelope.metadata().streamId().get();
                final Long actualPosition = jsonEnvelope.metadata().position().get();

                assertThat(actualStreamId, is(streamId));
                assertThat(actualPosition, is(position++));

                accumulatedSize++;
            }
        }

        assertThat(accumulatedSize, is(100));
        assertThat(numberOfStreamsProcessed, is(10));
    }

    @Test
    public void shouldReturnChunkOfSameStreamIdAndInSequenceVersions() {

        final EventFactory eventFactory = new EventFactory(
                1,
                5);

        final List<JsonEnvelope> allEvents = eventFactory.generateEvents(150);
        final TestEventProvider eventProvider = new TestEventProvider(allEvents);
        final PagedEventStream pagedEventStream = new PagedEventStream(eventProvider, 1, 150, 100);

        long position = 1L;

        while (pagedEventStream.hasNext()) {
            final List<JsonEnvelope> jsonEnvelopes = pagedEventStream
                    .nextStream()
                    .collect(toList());

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