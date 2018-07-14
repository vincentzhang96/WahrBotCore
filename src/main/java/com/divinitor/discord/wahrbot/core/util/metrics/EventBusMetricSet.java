package com.divinitor.discord.wahrbot.core.util.metrics;

import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.MetricSet;
import com.divinitor.discord.wahrbot.core.WahrBot;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A set of metrics for the event bus, tracking event rate, error rates, etc.
 */
public class EventBusMetricSet implements MetricSet {

    /**
     * The event bus to monitor
     */
    private final EventBus eventBus;

    /**
     * The event meter
     */
    private final Meter eventMeter;

    /**
     * The error meter
     */
    private final Meter errorMeter;

    public EventBusMetricSet(EventBus eventBus) {
        this.eventBus = eventBus;
        this.eventBus.register(this);
        this.eventMeter = new Meter();
        this.errorMeter = new Meter();
    }

    /**
     * Listens for all events from the event bus
     * @param event The event
     */
    @Subscribe
    public void handle(Object event) {
        this.eventMeter.mark();
    }

    /**
     * Increment the event error count
     */
    public void incrEventExceptionCount() {
        this.errorMeter.mark();
    }

    @Override
    public Map<String, Metric> getMetrics() {
        Map<String, Metric> ret = new HashMap<>();
        ret.put(MetricRegistry.name(WahrBot.class, "eventbus", "events"), this.eventMeter);
        ret.put(MetricRegistry.name(WahrBot.class, "eventbus", "errors"), this.errorMeter);

        return Collections.unmodifiableMap(ret);
    }
}
