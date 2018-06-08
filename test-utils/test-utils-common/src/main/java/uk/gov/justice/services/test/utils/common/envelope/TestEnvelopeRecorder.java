package uk.gov.justice.services.test.utils.common.envelope;


import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * To be used in unit and integration tests for testing invocations of handlers, senders etc..
 */
public abstract class TestEnvelopeRecorder {
    private final Deque<JsonEnvelope> recordedEnvelopes = new ConcurrentLinkedDeque<>();

    public void reset() {
        recordedEnvelopes.clear();
    }

    public JsonEnvelope firstRecordedEnvelope() {
        return !recordedEnvelopes.isEmpty() ? recordedEnvelopes.peekFirst() : null;
    }

    public List<JsonEnvelope> recordedEnvelopes() {
        return new ArrayList<>(recordedEnvelopes);
    }

    protected void record(final JsonEnvelope envelope) {
        recordedEnvelopes.add(envelope);
    }

}
