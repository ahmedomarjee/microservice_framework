package uk.gov.justice.services.eventsourcing.source.core.spliterator;

import static java.util.stream.Collectors.toList;
import static javax.ejb.TransactionAttributeType.REQUIRED;
import static uk.gov.justice.services.core.interceptor.InterceptorContext.interceptorContextWithInput;

import uk.gov.justice.services.core.interceptor.InterceptorChainProcessor;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.Optional;
import java.util.stream.Stream;

import javax.annotation.Resource;
import javax.ejb.Asynchronous;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.inject.Inject;
import javax.naming.InitialContext;
import javax.sql.DataSource;

import org.slf4j.Logger;

@Singleton
public class AsyncPageDispatch {

    @Inject
    Logger logger;

    @Resource(name = "openejb/Resource/frameworkviewstore")
    private DataSource dataSource;

    public void init() throws Exception {
        InitialContext initialContext = new InitialContext();
        initialContext.bind("java:/DS.SpliteratorEventBufferIT", dataSource);
    }

    @Asynchronous
    @TransactionAttribute(REQUIRED)
    public void call(final Stream<JsonEnvelope> jsonEnvelopeStream,
                     final InterceptorChainProcessor interceptorChainProcessor) {
        jsonEnvelopeStream
                .map((JsonEnvelope jsonEnvelope) -> process(jsonEnvelope, interceptorChainProcessor))
                .collect(toList());
    }

    private Optional<JsonEnvelope> process(final JsonEnvelope jsonEnvelope,
                                           final InterceptorChainProcessor interceptorChainProcessor) {
        return interceptorChainProcessor.process(interceptorContextWithInput(jsonEnvelope));
    }
}
