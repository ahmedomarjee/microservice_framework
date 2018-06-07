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

    private final Map<UUID, Long> streamIdVersions = new HashMap<>();

    private final List<UUID> streamIds;
    private final List<String> names;
    private final int maximumEvents;

    public TestEventProvider(final int numberOfStreams, final int numberOfNames, final int maximumEvents) {
        this.maximumEvents = maximumEvents;
        this.streamIds = new ArrayList<>();
        this.names = new ArrayList<>();

        rangeClosed(1, numberOfStreams).forEach(value -> streamIds.add(randomUUID()));

        rangeClosed(1, numberOfNames).forEach(value -> names.add(RandomStringUtils.randomAlphabetic(10)));
    }

    public Stream<JsonEnvelope> getAllEventsFrom(final long position, final int pageSize) {

        final Iterator<Integer> randomStreamIdIndexes = randomStreamIdIndexes(pageSize);

        final Iterator<Integer> randomNamesIndexes = new Random()
                .ints(pageSize, 1, names.size())
                .boxed().collect(toList())
                .iterator();

        final List<JsonEnvelope> jsonEnvelopes = new LinkedList<>();

        final long endInclusive = getEndInclusive(position, pageSize);

        LongStream.range(position, endInclusive).forEach(value ->
                {
                    final UUID streamId = streamIds.get(randomStreamIdIndexes.next());

                    final long version;

                    if (!streamIdVersions.containsKey(streamId)) {
                        streamIdVersions.put(streamId, 1L);
                        version = 1L;
                    } else {
                        version = streamIdVersions.get(streamId) + 1L;
                        streamIdVersions.put(streamId, version);
                    }

                    jsonEnvelopes.add(envelope()
                            .with(metadataWithRandomUUID(names.get(randomNamesIndexes.next()))
                                    .withStreamId(streamId)
                                    .withVersion(version)
                                    .withSource("test")
                            )
                            .build()
                    );
                }
        );

        return jsonEnvelopes.stream();
    }

    private Iterator<Integer> randomStreamIdIndexes(final int pageSize) {
        if (streamIds.size() > 1) {
            return new Random().ints(pageSize, 0, streamIds.size())
                    .boxed()
                    .collect(toList())
                    .iterator();
        }

        return Collections.nCopies(pageSize, 0).iterator();
    }

    private long getEndInclusive(final long position, final int pageSize) {
        final long endOfPage = position + pageSize;

        if (endOfPage > maximumEvents) {
            return maximumEvents + 1;
        }

        return endOfPage;
    }
}
