/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.monitors.VMWare.collectors;

import com.appdynamics.extensions.metrics.Metric;
import org.codehaus.jackson.map.ObjectMapper;

import java.util.List;
import java.util.concurrent.Phaser;

/**
 * @author Satish Muddam
 */
public abstract class BaseMetricCollector implements Runnable {

    private ObjectMapper objectMapper = new ObjectMapper();
    private List<Metric> collectedMetrics;
    private Phaser metricCollectorsPhaser;
    private String metricPrefix;

    public BaseMetricCollector(String metricPrefix, Phaser metricCollectorsPhaser, List<Metric> collectedMetrics) {
        this.metricPrefix = metricPrefix;
        this.metricCollectorsPhaser = metricCollectorsPhaser;
        this.collectedMetrics = collectedMetrics;
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    public List<Metric> getCollectedMetrics() {
        return collectedMetrics;
    }

    public Phaser getMetricCollectorsPhaser() {
        return metricCollectorsPhaser;
    }

    public String getMetricPrefix() {
        return metricPrefix;
    }
}