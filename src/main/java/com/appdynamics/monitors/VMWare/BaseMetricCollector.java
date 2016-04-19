package com.appdynamics.monitors.VMWare;

import com.appdynamics.extensions.util.MetricWriteHelper;
import com.singularity.ee.agent.systemagent.api.MetricWriter;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Satish Muddam
 */
public abstract class BaseMetricCollector {

    private MetricWriteHelper metricWriter;
    private String metricPrefix;

    public BaseMetricCollector(MetricWriteHelper metricWriter, String metricPrefix) {
        this.metricWriter = metricWriter;
        this.metricPrefix = metricPrefix;
    }

    private static final String ONE = "1";

    protected String applyReplacers(String name, Map<Pattern, String> replacers) {

        if (name == null || name.length() == 0 || replacers == null) {
            return name;
        }

        for (Map.Entry<Pattern, String> replacerEntry : replacers.entrySet()) {

            Pattern pattern = replacerEntry.getKey();

            Matcher matcher = pattern.matcher(name);
            name = matcher.replaceAll(replacerEntry.getValue());
        }

        return name;
    }

    /**
     * Returns the metric to the AppDynamics Controller.
     *
     * @param metricName  Name of the Metric
     * @param metricValue Value of the Metric
     */
    protected void printMetric(String metricName, Object metricValue) {

        metricWriter.printMetric(metricPrefix + "|" + metricName, toWholeNumberString(metricValue), MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION,
                MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT, MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_INDIVIDUAL);
    }

    /**
     * Currently, appD controller only supports Integer values. This function will round all the decimals into integers and convert them into strings.
     * If number is less than 0.5, Math.round will round it to 0 which is not useful on the controller.
     *
     * @param attribute value before rounding
     * @return rounded value
     */
    private static String toWholeNumberString(Object attribute) {
        if (attribute instanceof Double) {
            Double d = (Double) attribute;
            if (d > 0 && d < 1.0d) {
                return ONE;
            }
            return String.valueOf(Math.round(d));
        } else if (attribute instanceof Float) {
            Float f = (Float) attribute;
            if (f > 0 && f < 1.0f) {
                return ONE;
            }
            return String.valueOf(Math.round((Float) attribute));
        }
        return attribute.toString();
    }
}
