package uk.gov.justice.services.eventsourcing.source.core.spliterator;

import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.stream.Stream;

public interface EventProvider {

    Stream<JsonEnvelope> getAllEventsFrom(final long position, final int pageSize);
}
