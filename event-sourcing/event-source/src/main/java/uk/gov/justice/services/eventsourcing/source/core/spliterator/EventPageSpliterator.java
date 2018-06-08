package uk.gov.justice.services.eventsourcing.source.core.spliterator;

import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class EventPageSpliterator extends Spliterators.AbstractSpliterator<JsonEnvelope> {

    private final PagedEventStream pagedEventStream;
    private Iterator<JsonEnvelope> jsonEnvelopeIterator;

    public EventPageSpliterator(final PagedEventStream pagedEventStream,
                                final Stream<JsonEnvelope> jsonEnvelopeStream) {
        super(Long.MAX_VALUE, ORDERED);
        this.pagedEventStream = pagedEventStream;
        jsonEnvelopeIterator = jsonEnvelopeStream.iterator();
    }

    @Override
    public boolean tryAdvance(final Consumer<? super JsonEnvelope> action) {

        if (!jsonEnvelopeIterator.hasNext()) {
            if (pagedEventStream.hasNext()) {
                jsonEnvelopeIterator = pagedEventStream.nextStream().iterator();
            } else {
                return false;
            }
        }

        if (jsonEnvelopeIterator.hasNext()) {
            final JsonEnvelope jsonEnvelope = jsonEnvelopeIterator.next();
            action.accept(jsonEnvelope);
            return true;
        }

        return false;
    }

    @Override
    public Spliterator<JsonEnvelope> trySplit() {

        if (pagedEventStream.hasNext()) {
            return new EventPageSpliterator(pagedEventStream, pagedEventStream.nextStream());
        }

        return null;
    }
}
