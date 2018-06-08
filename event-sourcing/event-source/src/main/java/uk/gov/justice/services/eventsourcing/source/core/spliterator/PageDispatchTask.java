package uk.gov.justice.services.eventsourcing.source.core.spliterator;

import static java.util.stream.Collectors.toList;
import static javax.ejb.TransactionAttributeType.NOT_SUPPORTED;
import static javax.ejb.TransactionAttributeType.REQUIRED;
import static uk.gov.justice.services.core.interceptor.InterceptorContext.interceptorContextWithInput;

import uk.gov.justice.services.core.interceptor.InterceptorChainProcessor;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

import javax.ejb.TransactionAttribute;
import javax.enterprise.concurrent.ManagedTask;
import javax.enterprise.concurrent.ManagedTaskListener;

public class PageDispatchTask implements Callable<List<Optional<JsonEnvelope>>>, ManagedTask {

    private final Stream<JsonEnvelope> jsonEnvelopeStream;
    private final InterceptorChainProcessor interceptorChainProcessor;
    private final ManagedTaskListener managedTaskListener;

    public PageDispatchTask(final Stream<JsonEnvelope> jsonEnvelopeStream,
                            final InterceptorChainProcessor interceptorChainProcessor,
                            final ManagedTaskListener managedTaskListener) {
        this.jsonEnvelopeStream = jsonEnvelopeStream;
        this.interceptorChainProcessor = interceptorChainProcessor;
        this.managedTaskListener = managedTaskListener;
    }

    @Override
    @TransactionAttribute(NOT_SUPPORTED)
    public List<Optional<JsonEnvelope>> call() throws Exception {
        return jsonEnvelopeStream
                .map(this::process)
                .collect(toList());
    }

    @TransactionAttribute(REQUIRED)
    private Optional<JsonEnvelope> process(final JsonEnvelope jsonEnvelope) {
        return interceptorChainProcessor.process(interceptorContextWithInput(jsonEnvelope));
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

