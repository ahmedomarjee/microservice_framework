package uk.gov.justice.services.eventsourcing.source.core.spliterator;

import uk.gov.justice.services.core.interceptor.InterceptorChainProcessor;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.stream.Stream;

import javax.annotation.Resource;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.concurrent.ManagedTaskListener;
import javax.inject.Inject;

import org.slf4j.Logger;

public class ParallelContainerStreamConsumer implements ManagedTaskListener {

    @Inject
    Logger logger;

    @Resource(name = "concurrent/managedExecutorService")
    ManagedExecutorService managedExecutorService;

    private Deque<Future<List<Optional<JsonEnvelope>>>> outstandingTasks = new LinkedBlockingDeque<>();

    public void stream(final Stream<Stream<JsonEnvelope>> jsonEnvelopeStreams,
                       final InterceptorChainProcessor interceptorChainProcessor) {

        try (final Stream<Stream<JsonEnvelope>> streamOfStreams = jsonEnvelopeStreams) {
            streamOfStreams.forEach(jsonEnvelopeStream -> {
                submitPageDispatchTask(interceptorChainProcessor, jsonEnvelopeStream);
            });
        }
    }

    private void submitPageDispatchTask(final InterceptorChainProcessor interceptorChainProcessor, final Stream<JsonEnvelope> jsonEnvelopeStream) {
        final PageDispatchTask pageDispatchTask = dispatch(
                interceptorChainProcessor,
                jsonEnvelopeStream);

        outstandingTasks.add(managedExecutorService.submit(pageDispatchTask));
    }

    private PageDispatchTask dispatch(final InterceptorChainProcessor interceptorChainProcessor,
                                      final Stream<JsonEnvelope> jsonEnvelopeStream) {

        return new PageDispatchTask(
                jsonEnvelopeStream,
                interceptorChainProcessor,
                this);
    }

    @Override
    public void taskSubmitted(final Future<?> future, final ManagedExecutorService executor, final Object task) {
        logger.debug("--------Submitted--------");
    }

    @Override
    public void taskAborted(final Future<?> future, final ManagedExecutorService executor, final Object task, final Throwable exception) {
        logger.error("--------Aborted--------", exception);
        boolean dispatchComplete = removeOutstandingTask(future);
    }

    @Override
    public void taskDone(final Future<?> future, final ManagedExecutorService executor, final Object task, final Throwable exception) {
        logger.debug("--------Done--------");
        boolean dispatchComplete = removeOutstandingTask(future);
    }

    @Override
    public void taskStarting(final Future<?> future, final ManagedExecutorService executor, final Object task) {
        logger.debug("--------Started--------");
    }

    private boolean removeOutstandingTask(final Future<?> dispatchTaskFuture) {
        outstandingTasks.remove(dispatchTaskFuture);
        return outstandingTasks.isEmpty();
    }
}
