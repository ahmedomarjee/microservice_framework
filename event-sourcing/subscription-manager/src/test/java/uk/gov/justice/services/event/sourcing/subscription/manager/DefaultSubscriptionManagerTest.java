package uk.gov.justice.services.event.sourcing.subscription.manager;

import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.core.interceptor.InterceptorChainProcessor;
import uk.gov.justice.services.core.interceptor.InterceptorContext;
import uk.gov.justice.services.event.buffer.api.EventBufferService;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.subscription.domain.subscriptiondescriptor.Subscription;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;

@RunWith(MockitoJUnitRunner.class)
public class DefaultSubscriptionManagerTest {

    @Mock
    private Logger logger;

    @Mock
    private InterceptorChainProcessor interceptorChainProcessor;

    @Mock
    private Subscription subscription;

    @Mock
    private EventSource eventSource;

    @Mock
    private EventBufferService eventBufferService;

    @InjectMocks
    private DefaultSubscriptionManager defaultSubscriptionManager;

    @Captor
    private ArgumentCaptor<InterceptorContext> interceptorContextArgumentCaptor;

    @Test
    public void shouldProcessJsonEnvelope() {
        final JsonEnvelope incomingJsonEnvelope = mock(JsonEnvelope.class);
        final JsonEnvelope streamJsonEnvelope_1 = mock(JsonEnvelope.class);
        final JsonEnvelope streamJsonEnvelope_2 = mock(JsonEnvelope.class);


        final List<String> closeVerifierList = new ArrayList<>();

        final Stream<JsonEnvelope> jsonEnvelopeStream = Stream.of(
                incomingJsonEnvelope,
                streamJsonEnvelope_1,
                streamJsonEnvelope_2)
                .onClose(() -> closeVerifierList.add("Stream was closed"));

        when(eventBufferService.currentOrderedEventsWith(incomingJsonEnvelope)).thenReturn(jsonEnvelopeStream);

        defaultSubscriptionManager.process(incomingJsonEnvelope);

        verify(interceptorChainProcessor, times(3)).process(interceptorContextArgumentCaptor.capture());

        final List<InterceptorContext> interceptorContextList = interceptorContextArgumentCaptor.getAllValues();

        assertThat(interceptorContextList.size(), is(3));

        assertThat(interceptorContextList.get(0).inputEnvelope(), is(incomingJsonEnvelope));
        assertThat(interceptorContextList.get(1).inputEnvelope(), is(streamJsonEnvelope_1));
        assertThat(interceptorContextList.get(2).inputEnvelope(), is(streamJsonEnvelope_2));

        assertThat(closeVerifierList.size(), is(1));
        assertThat(closeVerifierList.get(0), is("Stream was closed"));
    }

    @Test
    public void shouldStartSubscription() {

        final String subscriptionName = "subscriptionName";
        when(subscription.getName()).thenReturn(subscriptionName);

        final String eventSourceName = "eventSourceName";
        when(subscription.getEventSourceName()).thenReturn(eventSourceName);

        defaultSubscriptionManager.startSubscription();

        verify(logger).debug(format("Starting subscription: %s for event source: %s", subscriptionName, eventSourceName));
    }
}
