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

public class EventBusMetricSet implements MetricSet {

    private final EventBus eventBus;
    private final Meter eventMeter;
    private final Meter errorMeter;

    public EventBusMetricSet(EventBus eventBus) {
        this.eventBus = eventBus;
        this.eventBus.register(this);
        this.eventMeter = new Meter();
        this.errorMeter = new Meter();
    }

    @Subscribe
    public void handle(Object event) {
        this.eventMeter.mark();
    }

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
