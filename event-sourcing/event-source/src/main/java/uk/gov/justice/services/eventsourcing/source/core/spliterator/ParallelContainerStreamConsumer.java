package uk.gov.justice.services.eventsourcing.source.core.spliterator;

import uk.gov.justice.services.core.interceptor.InterceptorChainProcessor;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;

import javax.annotation.Resource;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.concurrent.ManagedTaskListener;
import javax.inject.Inject;

import org.slf4j.Logger;

public class ParallelContainerStreamConsumer implements ManagedTaskListener {

    @Inject
    Logger logger;

    @Resource
    ManagedExecutorService managedExecutorService;

    private Deque<Future<List<Optional<JsonEnvelope>>>> outstandingTasks = new LinkedBlockingDeque<>();

    public void stream(final EventPageChunker eventPageChunker,
                       final InterceptorChainProcessor interceptorChainProcessor) {

        while (eventPageChunker.hasNext()) {
            final PageDispatchTask pageDispatchTask = new PageDispatchTask(
                    eventPageChunker.nextStream(),
                    interceptorChainProcessor,
                    this);

            outstandingTasks.add(managedExecutorService.submit(pageDispatchTask));
        }
    }

    @Override
    public void taskSubmitted(final Future<?> future, final ManagedExecutorService executor, final Object task) {
        logger.info("--------Submitted--------");
    }

    @Override
    public void taskAborted(final Future<?> future, final ManagedExecutorService executor, final Object task, final Throwable exception) {
        logger.info("--------Aborted--------");
        boolean dispatchComplete = removeOutstandingTask(future);
    }

    @Override
    public void taskDone(final Future<?> future, final ManagedExecutorService executor, final Object task, final Throwable exception) {
        logger.info("--------Done--------");
        boolean dispatchComplete = removeOutstandingTask(future);
    }

    @Override
    public void taskStarting(final Future<?> future, final ManagedExecutorService executor, final Object task) {
        logger.info("--------Started--------");
    }

    private boolean removeOutstandingTask(final Future<?> dispatchTaskFuture) {
        outstandingTasks.remove(dispatchTaskFuture);
        return outstandingTasks.isEmpty();
    }
}
