package uk.gov.justice.services.eventsourcing.source.core.spliterator;

import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

public class PagedEventStream {

    private final Map<UUID, Stream.Builder<JsonEnvelope>> uuidStreamMap = new HashMap<>();
    private final EventProvider eventProvider;
    private final int lastPosition;
    private int currentPosition;
    private final int pageSize;

    public PagedEventStream(final EventProvider eventProvider,
                            final int fromPosition,
                            final int lastPosition,
                            final int pageSize) {
        this.eventProvider = eventProvider;
        this.lastPosition = lastPosition;
        this.currentPosition = fromPosition;
        this.pageSize = pageSize;
    }

    public Stream<JsonEnvelope> nextStream() {
        if (uuidStreamMap.isEmpty()) {
            processPageOfEvents();
        }

        final Iterator<UUID> keyIterator = uuidStreamMap.keySet().iterator();

        if (keyIterator.hasNext()) {
            return uuidStreamMap
                    .remove(keyIterator.next())
                    .build();
        }

        return Stream.empty();
    }

    public boolean hasNext() {
        if (uuidStreamMap.isEmpty()) {
            processPageOfEvents();
        }

        return !uuidStreamMap.isEmpty();
    }

    private void processPageOfEvents() {
        if (currentPosition <= lastPosition) {

            try (final Stream<JsonEnvelope> pageOfEvents = eventProvider.getAllEventsFrom(currentPosition, pageSize)) {
                pageOfEvents.forEach(this::splitStreamByStreamId);
            }

            currentPosition = currentPosition + pageSize;
        }
    }

    private void splitStreamByStreamId(final JsonEnvelope jsonEnvelope) {
        final UUID streamId = jsonEnvelope.metadata().streamId().get();

        if (uuidStreamMap.containsKey(streamId)) {
            uuidStreamMap.get(streamId).add(jsonEnvelope);
        } else {
            final Stream.Builder<JsonEnvelope> jsonEnvelopes = Stream.builder();
            jsonEnvelopes.add(jsonEnvelope);

            uuidStreamMap.putIfAbsent(streamId, jsonEnvelopes);
        }
    }
}
