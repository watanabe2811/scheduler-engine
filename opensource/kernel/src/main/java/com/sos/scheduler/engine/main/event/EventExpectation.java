package com.sos.scheduler.engine.main.event;

import com.sos.scheduler.engine.common.time.Time;
import com.sos.scheduler.engine.eventbus.EventPredicate;


public class EventExpectation {
    private final Time timeout;
    private final EventPredicate predicate;

    
    public EventExpectation(Time timeout, EventPredicate predicate) {
        this.timeout = timeout;
        this.predicate = predicate;
    }


    public final Time getTimeout() {
        return timeout;
    }


    public final EventPredicate getPredicate() {
        return predicate;
    }
}