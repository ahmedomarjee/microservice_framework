package uk.gov.justice.services.eventsourcing.source.core.spliterator;

import static java.util.stream.Collectors.toList;

import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.List;
import java.util.Optional;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.Test;

public class EventFactoryTest {

    @Test
    public void shouldProduceListOfJsonEnvelopesOfGivenPageSize() {

        final EventFactory eventFactory = new EventFactory(
                1,
                5);

        final List<JsonEnvelope> allEvents = eventFactory.generateEvents(50);
        final TestEventProvider eventProvider = new TestEventProvider(allEvents);

        final List<JsonEnvelope> jsonEnvelopes = eventProvider.getAllEventsFrom(1, 50).collect(toList());

        MatcherAssert.assertThat(jsonEnvelopes.size(), CoreMatchers.is(50));
        MatcherAssert.assertThat(jsonEnvelopes.get(0).metadata().position(), CoreMatchers.is(Optional.of(1L)));
        MatcherAssert.assertThat(jsonEnvelopes.get(49).metadata().position(), CoreMatchers.is(Optional.of(50L)));
    }

    @Test
    public void shouldProduceListOfJsonEnvelopesOfGivenPageSizeButLimitedToMaximumEvents() {

        final EventFactory eventFactory = new EventFactory(
                1,
                5);

        final List<JsonEnvelope> allEvents = eventFactory.generateEvents(20);
        final TestEventProvider eventProvider = new TestEventProvider(allEvents);

        final List<JsonEnvelope> jsonEnvelopes = eventProvider.getAllEventsFrom(1, 50).collect(toList());

        MatcherAssert.assertThat(allEvents.size(), CoreMatchers.is(20));
        MatcherAssert.assertThat(allEvents.get(0).metadata().position(), CoreMatchers.is(Optional.of(1L)));
        MatcherAssert.assertThat(allEvents.get(19).metadata().position(), CoreMatchers.is(Optional.of(20L)));
    }

}
