/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.monitors.VMWare.collectors;

import com.appdynamics.extensions.logging.ExtensionsLoggerFactory;
import com.appdynamics.monitors.VMWare.metrics.Metric;
import com.appdynamics.monitors.VMWare.metrics.VMMetrics;
import com.vmware.vim25.ManagedEntityStatus;
import com.vmware.vim25.VirtualMachineQuickStats;
import com.vmware.vim25.mo.VirtualMachine;
import org.slf4j.Logger;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Phaser;

/**
 * @author Satish Muddam
 */
public class VMMetricCollector extends BaseMetricCollector {

    private static final Logger logger = ExtensionsLoggerFactory.getLogger(VMMetricCollector.class);

    private VirtualMachine virtualMachine;
    private VMMetrics vmMetrics;

    public VMMetricCollector(VirtualMachine virtualMachine, String baseMetricPath, VMMetrics vmMetrics, List<com.appdynamics.extensions.metrics.Metric> collectedMetrics, Phaser metricCollectorsPhaser) {
        super(baseMetricPath, metricCollectorsPhaser, collectedMetrics);
        this.virtualMachine = virtualMachine;
        this.vmMetrics = vmMetrics;
        this.getMetricCollectorsPhaser().register();
    }

    public void run() {

        String virtualMachineName = virtualMachine.getName();

        ManagedEntityStatus overallStatus = virtualMachine.getOverallStatus();

        String baseMetricName = getMetricPrefix() + "|" + "VirtualMachine" + "|" + virtualMachineName;

        if (ManagedEntityStatus.red.equals(overallStatus)) {
            logger.error("VM [{}] status is red, not collecting metrics", virtualMachineName);
            com.appdynamics.extensions.metrics.Metric thisMetric = new com.appdynamics.extensions.metrics.Metric("status", String.valueOf(overallStatus.ordinal()), baseMetricName + "|Status");
            getCollectedMetrics().add(thisMetric);
            return;
        } else {
            com.appdynamics.extensions.metrics.Metric thisMetric = new com.appdynamics.extensions.metrics.Metric("status", String.valueOf(overallStatus.ordinal()), baseMetricName + "|Status");
            getCollectedMetrics().add(thisMetric);
        }

        logger.info("Started collecting metrics for vm [{}]", virtualMachineName);

        VirtualMachineQuickStats vmStats = virtualMachine.getSummary().getQuickStats();

        try {
            Metric[] metrics = vmMetrics.getMetrics();

            for (Metric metric : metrics) {

                String name = metric.getName();
                BigDecimal value = null;

                try {
                    if ("Ballooned Memory".equals(name)) {
                        value = BigDecimal.valueOf(vmStats.getBalloonedMemory());
                    } else if ("Compressed Memory".equals(name)) {
                        value = BigDecimal.valueOf(vmStats.getCompressedMemory());

                    } else if ("Overhead Memory Consumed".equals(name)) {
                        value = BigDecimal.valueOf(vmStats.getConsumedOverheadMemory());

                    } else if ("Distributed CPU Entitlement".equals(name)) {
                        value = BigDecimal.valueOf(vmStats.getDistributedCpuEntitlement());

                    } else if ("Distributed Memory Entitlement".equals(name)) {
                        value = BigDecimal.valueOf(vmStats.getDistributedMemoryEntitlement());

                    } else if ("Guest Memory Usage".equals(name)) {
                        value = BigDecimal.valueOf(vmStats.getGuestMemoryUsage());

                    } else if ("Host Memory Usage".equals(name)) {
                        value = BigDecimal.valueOf(vmStats.getHostMemoryUsage());

                    } else if ("Overall CPU Usage".equals(name)) {
                        value = BigDecimal.valueOf(vmStats.getOverallCpuUsage());

                    } else if ("Overall CPU Demand".equals(name)) {
                        value = BigDecimal.valueOf(vmStats.getOverallCpuDemand());

                    } else if ("Private Memory".equals(name)) {
                        value = BigDecimal.valueOf(vmStats.getPrivateMemory());

                    } else if ("Shared Memory".equals(name)) {
                        value = BigDecimal.valueOf(vmStats.getSharedMemory());

                    } else if ("Static CPU Entitlement".equals(name)) {
                        value = BigDecimal.valueOf(vmStats.getStaticCpuEntitlement());
                    } else if ("Static Memory Entitlement".equals(name)) {
                        value = BigDecimal.valueOf(vmStats.getStaticMemoryEntitlement());

                    } else if ("Swapped Memory".equals(name)) {
                        value = BigDecimal.valueOf(vmStats.getSwappedMemory());

                    } else if ("Up Time".equals(name)) {
                        value = BigDecimal.valueOf(vmStats.getUptimeSeconds());

                    } else if ("Memory MB".equals(name)) {
                        value = BigDecimal.valueOf(virtualMachine.getConfig().getHardware().getMemoryMB());

                    } else if ("Num CPU".equals(name)) {
                        value = BigDecimal.valueOf(virtualMachine.getConfig().getHardware().getNumCPU());
                    }
                } catch (Exception e) {
                    logger.debug("Error collecting metric [{}] on vm [{}]", name, virtualMachineName, e);
                }

                String alias = metric.getAlias();
                if (alias != null) {
                    name = alias;
                }

                Map<String, String> propertiesMap = getObjectMapper().convertValue(metric, Map.class);

                StringBuilder sb = new StringBuilder(baseMetricName);
                sb.append("|").append(name);

                String fullMetricPath = sb.toString();

                com.appdynamics.extensions.metrics.Metric thisMetric = new com.appdynamics.extensions.metrics.Metric(name, value.toString(), fullMetricPath, propertiesMap);
                getCollectedMetrics().add(thisMetric);
            }

            logger.info("Finished collecting metrics for vm [{}]", virtualMachineName);
        } catch (Exception e) {
            logger.error("Error while collection vm [{}] metrics", virtualMachineName, e);
        } finally {
            getMetricCollectorsPhaser().arriveAndDeregister();
        }
    }
}