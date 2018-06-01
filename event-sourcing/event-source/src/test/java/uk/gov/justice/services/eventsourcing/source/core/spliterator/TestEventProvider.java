package uk.gov.justice.services.eventsourcing.source.core.spliterator;

import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.rangeClosed;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import org.apache.commons.lang3.RandomStringUtils;

public class TestEventProvider implements EventProvider {

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

    public Stream<JsonEnvelope> getAllEventsFrom(final long position, final long pageSize) {

        final Iterator<Integer> randomStreamIdIndexes = new Random().ints(pageSize, 1, streamIds.size()).boxed().collect(toList()).iterator();
        final Iterator<Integer> randomNamesIndexes = new Random().ints(pageSize, 1, names.size()).boxed().collect(toList()).iterator();

        final Stream.Builder<JsonEnvelope> streamBuilder = Stream.builder();

        final long endInclusive = getEndInclusive(position, pageSize);

        LongStream.range(position, endInclusive).forEach(value ->
                streamBuilder.add(envelope()
                        .with(metadataWithRandomUUID(names.get(randomNamesIndexes.next()))
                                .withStreamId(streamIds.get(randomStreamIdIndexes.next()))
                                .withVersion(value))
                        .build()
                )
        );

        return streamBuilder.build();
    }

    private long getEndInclusive(final long position, final long pageSize) {
        final long endOfPage = position + pageSize;

        if (endOfPage > maximumEvents) {
            return maximumEvents + 1;
        }

        return endOfPage;
    }
}
