package uk.gov.justice.services.eventsourcing.source.core.spliterator;

import uk.gov.justice.services.core.interceptor.InterceptorChainProcessor;
import uk.gov.justice.services.core.interceptor.InterceptorContext;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.logging.Logger;
import java.util.stream.Stream;

import javax.enterprise.concurrent.ManagedTask;
import javax.enterprise.concurrent.ManagedTaskListener;

import org.slf4j.LoggerFactory;

public class PageDispatchTask implements Callable<List<Optional<JsonEnvelope>>>, ManagedTask {

    private final Iterator<JsonEnvelope> jsonEnvelopeIterator;
    private final InterceptorChainProcessor interceptorChainProcessor;
    private final ManagedTaskListener managedTaskListener;

    public PageDispatchTask(final List<JsonEnvelope> jsonEnvelopeStream,
                            final InterceptorChainProcessor interceptorChainProcessor,
                            final ManagedTaskListener managedTaskListener) {
        this.jsonEnvelopeIterator = jsonEnvelopeStream.iterator();
        this.interceptorChainProcessor = interceptorChainProcessor;
        this.managedTaskListener = managedTaskListener;
    }

    @Override
    public List<Optional<JsonEnvelope>> call() throws Exception {
        final List<Optional<JsonEnvelope>> processed = new ArrayList<>();

        while (jsonEnvelopeIterator.hasNext()) {
            processed.add(interceptorChainProcessor.process(InterceptorContext.interceptorContextWithInput(jsonEnvelopeIterator.next())));
        }

        return processed;
    }

    @Override
    public ManagedTaskListener getManagedTaskListener() {
        return managedTaskListener;
    }

    @Override
    public Map<String, String> getExecutionProperties() {
        return null;
    }
}

