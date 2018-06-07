package uk.gov.justice.services.components.event.listener.interceptors.it;

import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.rangeClosed;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.eventsourcing.source.core.spliterator.EventProvider;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import org.apache.commons.lang3.RandomStringUtils;

public class TestEventProvider implements EventProvider {

    private final List<JsonEnvelope> jsonEnvelopes;

    public TestEventProvider(final List<JsonEnvelope> jsonEnvelopes) {
        this.jsonEnvelopes = jsonEnvelopes;
    }

    public Stream<JsonEnvelope> getAllEventsFrom(final long position, final int pageSize) {

        final List<JsonEnvelope> subList = new ArrayList<>(pageSize);

        for(int i = (int) position; i < position + pageSize; i++) {
            if(i == jsonEnvelopes.size()) {
                break;
            }

            subList.add(jsonEnvelopes.get(i));
        }

        return subList.stream();
    }
}
