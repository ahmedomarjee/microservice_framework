package uk.gov.justice.services.eventsourcing.source.core.spliterator;

import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.List;

import org.junit.Test;

public class EventPageSpliteratorTest {

    private static final int NUMBER_OF_STREAMS = 100;
    private static final int NUMBER_OF_EVENT_NAMES = 5;

    private static final int FIRST_POSITION = 1;
    private static final int MAXIMUM_EVENTS = 1_000;
    private static final int PAGE_SIZE = 100;

    @Test
    public void shouldProcessStreamSequentiallyButNotInOrder() {

        final EventFactory eventFactory = new EventFactory(
                NUMBER_OF_STREAMS,
                NUMBER_OF_EVENT_NAMES);

        final List<JsonEnvelope> allEvents = eventFactory.generateEvents(MAXIMUM_EVENTS);
        final TestEventProvider eventProvider = new TestEventProvider(allEvents);
        final EventPageChunker eventPageChunker = new EventPageChunker(eventProvider, FIRST_POSITION, MAXIMUM_EVENTS, PAGE_SIZE);

        final EventPageSpliterator eventPageSpliterator = new EventPageSpliterator(eventPageChunker, eventPageChunker.nextStream());

        final long start = System.currentTimeMillis();

        final List<JsonEnvelope> jsonEnvelopes = stream(eventPageSpliterator, false)
                .map(this::processJsonEnvelope)
                .collect(toList());

        final long finish = System.currentTimeMillis();

        System.out.println("Sequential total time in millis: " + (finish - start));

        assertThat(jsonEnvelopes.size(), is(MAXIMUM_EVENTS));
    }

    @Test
    public void shouldProcessStreamInParallel() {

        final EventFactory eventFactory = new EventFactory(
                NUMBER_OF_STREAMS,
                NUMBER_OF_EVENT_NAMES);

        final List<JsonEnvelope> allEvents = eventFactory.generateEvents(MAXIMUM_EVENTS);
        final TestEventProvider eventProvider = new TestEventProvider(allEvents);
        final EventPageChunker eventPageChunker = new EventPageChunker(eventProvider, FIRST_POSITION, MAXIMUM_EVENTS, PAGE_SIZE);

        final EventPageSpliterator eventPageSpliterator = new EventPageSpliterator(eventPageChunker, eventPageChunker.nextStream());

        final long start = System.currentTimeMillis();

        final List<JsonEnvelope> jsonEnvelopes = stream(eventPageSpliterator, true)
                .map(this::processJsonEnvelope)
                .collect(toList());

        final long finish = System.currentTimeMillis();

        System.out.println("Parallel total time in millis: " + (finish - start));

        assertThat(jsonEnvelopes.size(), is(MAXIMUM_EVENTS));
    }

    private JsonEnvelope processJsonEnvelope(final JsonEnvelope jsonEnvelope) {
        try {
            Thread.sleep(50L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }

        return jsonEnvelope;
    }
}