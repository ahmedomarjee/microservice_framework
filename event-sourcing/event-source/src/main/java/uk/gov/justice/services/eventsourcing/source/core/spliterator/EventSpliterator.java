package uk.gov.justice.services.eventsourcing.source.core.spliterator;

import static java.util.stream.Collectors.toList;

import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.List;
import java.util.Spliterators.AbstractSpliterator;
import java.util.function.Consumer;

public class EventSpliterator extends AbstractSpliterator<JsonEnvelope> {

    private static final int PAGE_SIZE = 10;
    private final EventProvider eventProvider;
    private final int lastPosition;
    private int currentPosition;
    private int currentPageIndex;

    private List<JsonEnvelope> jsonEnvelopes;

    public EventSpliterator(final EventProvider eventProvider, final int fromPosition, final int lastPosition) {
        super(Long.MAX_VALUE, ORDERED);
        this.eventProvider = eventProvider;
        this.lastPosition = lastPosition;
        this.currentPosition = fromPosition;
    }

    @Override
    public boolean tryAdvance(final Consumer<? super JsonEnvelope> action) {

        if (null == jsonEnvelopes || (currentPosition > PAGE_SIZE && currentPosition <= lastPosition)) {
            jsonEnvelopes = eventProvider.getAllEventsFrom(currentPosition, PAGE_SIZE).collect(toList());
            currentPageIndex = 0;
        }

        if (currentPageIndex <= PAGE_SIZE && currentPosition <= lastPosition) {
            action.accept(jsonEnvelopes.get(currentPageIndex++));
            currentPosition++;
            return true;
        }

        return false;
    }

}
