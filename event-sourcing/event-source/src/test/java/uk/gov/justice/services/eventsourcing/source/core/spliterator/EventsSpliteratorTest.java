package uk.gov.justice.services.eventsourcing.source.core.spliterator;

import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.Test;

public class EventsSpliteratorTest {

    @Test
    public void shouldSpliterateStreamOfJsonEnvelopes() {

        final EventFactory eventFactory = new EventFactory(
                1,
                5);

        final List<JsonEnvelope> allEvents = eventFactory.generateEvents(150);
        final TestEventProvider eventProvider = new TestEventProvider(allEvents);

        final EventSpliterator eventSpliterator = new EventSpliterator(eventProvider, 1, 150);

        final List<JsonEnvelope> jsonEnvelopes = stream(eventSpliterator, false).collect(toList());

        assertThat(jsonEnvelopes.size(), is(150));
        assertThat(jsonEnvelopes.get(0).metadata().position(), is(Optional.of(1L)));
        assertThat(jsonEnvelopes.get(149).metadata().position(), is(Optional.of(150L)));
    }

    @Test
    public void shouldChunkStreamAccordingToStreamId() {

        final EventFactory eventFactory = new EventFactory(
                10,
                5);

        final List<JsonEnvelope> allEvents = eventFactory.generateEvents(1000);
        final TestEventProvider eventProvider = new TestEventProvider(allEvents);

        final Map<UUID, Stream.Builder<JsonEnvelope>> uuidStreamMap = new HashMap<>();

        eventProvider.getAllEventsFrom(1, 1000)
                .forEach(jsonEnvelope -> {

                    final UUID streamId = jsonEnvelope.metadata().streamId().get();

                    if (uuidStreamMap.containsKey(streamId)) {
                        uuidStreamMap.put(streamId, uuidStreamMap.get(streamId).add(jsonEnvelope));
                    } else {
                        uuidStreamMap.putIfAbsent(streamId, Stream.<JsonEnvelope>builder().add(jsonEnvelope));
                    }
                });

        uuidStreamMap.entrySet().forEach(uuidBuilderEntry -> System.out.println(uuidBuilderEntry.getKey() + " = " + uuidBuilderEntry.getValue().build().collect(Collectors.toList())));
    }

    public static <T> Stream<List<T>> ofSubLists(List<T> source, int length) {
        if (length <= 0)
            throw new IllegalArgumentException("length = " + length);
        int size = source.size();
        if (size <= 0)
            return Stream.empty();
        int fullChunks = (size - 1) / length;
        return IntStream.range(0, fullChunks + 1).mapToObj(
                n -> source.subList(n * length, n == fullChunks ? size : (n + 1) * length));
    }
}
