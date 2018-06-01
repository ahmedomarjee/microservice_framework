package uk.gov.justice.services.eventsourcing.source.core.spliterator;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class EventPageChunkerTest {

    @Test
    public void shouldSplitPagesOfEventsByStreamId() {

        final TestEventProvider eventProvider = new TestEventProvider(10, 5, 150);
        final EventPageChunker eventPageChunker = new EventPageChunker(eventProvider, 1, 150, 100);

        long accumulatedSize = 0;
        int streamsProcessed = 0;

        while (eventPageChunker.hasNext()) {
            accumulatedSize = accumulatedSize + eventPageChunker.nextStream().count();
            streamsProcessed++;
        }

        System.out.println("Streams processed = " + streamsProcessed);
        assertThat(accumulatedSize, is(150L));
    }
}