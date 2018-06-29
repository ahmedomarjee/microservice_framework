package uk.gov.justice.services.event.sourcing.subscription.manager;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static uk.gov.justice.services.core.interceptor.InterceptorContext.interceptorContextWithInput;

import uk.gov.justice.services.core.interceptor.InterceptorChainProcessor;
import uk.gov.justice.services.core.interceptor.InterceptorContext;
import uk.gov.justice.services.event.buffer.api.EventBufferService;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.subscription.SubscriptionManager;
import uk.gov.justice.subscription.domain.subscriptiondescriptor.Subscription;

import java.util.Optional;
import java.util.stream.Stream;

import org.slf4j.Logger;

public class DefaultSubscriptionManager implements SubscriptionManager {

    private final Subscription subscription;
    private final EventSource eventSource;
    private final InterceptorChainProcessor interceptorChainProcessor;
    private final Optional<EventBufferService> eventBufferService;
    private final Logger logger;

    public DefaultSubscriptionManager(final Subscription subscription,
                                      final EventSource eventSource,
                                      final InterceptorChainProcessor interceptorChainProcessor,
                                      final EventBufferService eventBufferService,
                                      final Logger logger) {
        this.subscription = subscription;
        this.eventSource = eventSource;
        this.interceptorChainProcessor = interceptorChainProcessor;
        this.eventBufferService = of(eventBufferService);
        this.logger = logger;
    }

    public DefaultSubscriptionManager(final Subscription subscription,
                                      final EventSource eventSource,
                                      final InterceptorChainProcessor interceptorChainProcessor,
                                      final Logger logger) {
        this.subscription = subscription;
        this.eventSource = eventSource;
        this.interceptorChainProcessor = interceptorChainProcessor;
        this.eventBufferService = empty();
        this.logger = logger;
    }

    @Override
    public void process(final JsonEnvelope incomingJsonEnvelope) {

        if (eventBufferService.isPresent()) {
            try (final Stream<JsonEnvelope> jsonEnvelopeStream = eventBufferService.get().currentOrderedEventsWith(incomingJsonEnvelope)) {
                jsonEnvelopeStream.forEach(jsonEnvelope -> {
                    final InterceptorContext interceptorContext = interceptorContextWithInput(jsonEnvelope);
                    interceptorChainProcessor.process(interceptorContext);
                });
            }
        } else {
            final InterceptorContext interceptorContext = interceptorContextWithInput(incomingJsonEnvelope);
            interceptorChainProcessor.process(interceptorContext);
        }
    }

    @Override
    public void startSubscription() {
        logger.debug(format("Starting subscription: %s for event source: %s", subscription.getName(), subscription.getEventSourceName()));
    }
}
