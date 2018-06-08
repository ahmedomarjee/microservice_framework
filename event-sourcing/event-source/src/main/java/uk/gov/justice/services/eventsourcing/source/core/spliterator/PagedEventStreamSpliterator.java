package uk.gov.justice.services.eventsourcing.source.core.spliterator;

import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.HashMap;
import java.util.Map;
import java.util.Spliterators;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class PagedEventStreamSpliterator extends Spliterators.AbstractSpliterator<Stream<JsonEnvelope>> {

    private final Map<UUID, Stream.Builder<JsonEnvelope>> uuidStreamMap = new HashMap<>();
    private final PagedEventStream pagedEventStream;

    public PagedEventStreamSpliterator(final PagedEventStream pagedEventStream) {
        super(Long.MAX_VALUE, ORDERED);
        this.pagedEventStream = pagedEventStream;
    }

    @Override
    public boolean tryAdvance(final Consumer<? super Stream<JsonEnvelope>> action) {
        if (pagedEventStream.hasNext()) {
            action.accept(pagedEventStream.nextStream());
            return true;
        }

        return false;
    }
}
