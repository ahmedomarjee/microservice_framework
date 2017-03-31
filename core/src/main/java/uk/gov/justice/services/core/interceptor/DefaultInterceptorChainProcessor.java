package uk.gov.justice.services.core.interceptor;

import static uk.gov.justice.services.core.interceptor.InterceptorContext.copyWithOutput;
import static uk.gov.justice.services.core.interceptor.InterceptorContext.interceptorContextWithInput;

import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.Optional;
import java.util.function.Function;

public class DefaultInterceptorChainProcessor implements InterceptorChainProcessor {

    private final InterceptorCache interceptorCache;
    private final Function<JsonEnvelope, JsonEnvelope> dispatch;
    private final String component;

    DefaultInterceptorChainProcessor(final InterceptorCache interceptorCache, final Function<JsonEnvelope, JsonEnvelope> dispatch, final String component) {
        this.interceptorCache = interceptorCache;
        this.dispatch = dispatch;
        this.component = component;
    }

    @Override
    public Optional<JsonEnvelope> process(final InterceptorContext interceptorContext) {
        return new InterceptorChain(interceptorCache.getInterceptors(component), targetOf(dispatch))
                .processNext(interceptorContext)
                .outputEnvelope();
    }

    @Override
    @Deprecated
    public Optional<JsonEnvelope> process(final JsonEnvelope jsonEnvelope) {
        return process(interceptorContextWithInput(jsonEnvelope));
    }

    private Target targetOf(final Function<JsonEnvelope, JsonEnvelope> dispatch) {
        return interceptorContext -> copyWithOutput(interceptorContext, dispatch.apply(interceptorContext.inputEnvelope()));
    }
}