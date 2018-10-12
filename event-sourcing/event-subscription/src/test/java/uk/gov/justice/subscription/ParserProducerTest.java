package uk.gov.justice.subscription;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class ParserProducerTest {

    @Test
    public void shouldProduceEventSourcesParser() {
        final EventSourcesParser eventSourcesParser = new ParserProducer().eventSourcesParser();

        assertThat(eventSourcesParser, is(instanceOf(EventSourcesParser.class)));
    }

    @Test
    public void shouldProduceSubscriptionDescriptorsParser() {
        final SubscriptionsDescriptorParser subscriptionsDescriptorParser = new ParserProducer().subscriptionDescriptorsParser();

        assertThat(subscriptionsDescriptorParser, is(instanceOf(SubscriptionsDescriptorParser.class)));
    }
}