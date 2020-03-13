/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.monitors.VMWare.collectors;

import com.appdynamics.extensions.executorservice.MonitorExecutorService;
import com.appdynamics.extensions.logging.ExtensionsLoggerFactory;
import com.appdynamics.extensions.metrics.Metric;
import com.appdynamics.monitors.VMWare.metrics.VMWareMetrics;
import com.google.common.collect.Lists;
import com.vmware.vim25.HostListSummaryQuickStats;
import com.vmware.vim25.ManagedEntityStatus;
import com.vmware.vim25.mo.HostSystem;
import com.vmware.vim25.mo.ManagedEntity;
import com.vmware.vim25.mo.VirtualMachine;
import org.slf4j.Logger;

import java.math.BigDecimal;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Phaser;

/**
 * @author Satish Muddam
 */
public class HostMetricCollector extends BaseMetricCollector {

    private static final Logger logger = ExtensionsLoggerFactory.getLogger(HostMetricCollector.class);

    private final ManagedEntity hostEntity;
    private final List<Map<String, Object>> hostConfig;
    private MonitorExecutorService executorService;
    private VMWareMetrics vmWareMetrics;

    public HostMetricCollector(String metricPrefix, ManagedEntity hostEntity, List<Map<String, Object>> hostConfig, Phaser metricCollectorsPhaser, MonitorExecutorService executorService, List<Metric> collectedMetrics, VMWareMetrics vmWareMetrics) {
        super(metricPrefix, metricCollectorsPhaser, collectedMetrics);
        this.hostEntity = hostEntity;
        this.hostConfig = hostConfig;
        this.getMetricCollectorsPhaser().register();
        this.executorService = executorService;
        this.vmWareMetrics = vmWareMetrics;
    }

    public void run() {

        HostSystem hostSystem = (HostSystem) hostEntity;
        String hostName = hostSystem.getName();
        String baseMetricName = getMetricPrefix() + "|" + "HostSystem" + "|" + hostName;

        logger.info("Collecting host [{}] metrics", hostName);
        try {

            ManagedEntityStatus overallStatus = hostSystem.getOverallStatus();

            if (ManagedEntityStatus.red.equals(overallStatus)) {
                logger.error("Host [{}] status is red, not collecting metrics", hostName);
                com.appdynamics.extensions.metrics.Metric thisMetric = new com.appdynamics.extensions.metrics.Metric("status", String.valueOf(overallStatus.ordinal()), baseMetricName + "|Status");
                getCollectedMetrics().add(thisMetric);
                return;
            } else {
                com.appdynamics.extensions.metrics.Metric thisMetric = new com.appdynamics.extensions.metrics.Metric("status", String.valueOf(overallStatus.ordinal()), baseMetricName + "|Status");
                getCollectedMetrics().add(thisMetric);
            }


            HostListSummaryQuickStats hostStats = hostSystem.getSummary().getQuickStats();

            long totalHz = hostSystem.getHardware().getCpuInfo().getHz();
            short numCpuCores = hostSystem.getHardware().getCpuInfo().getNumCpuCores();

            com.appdynamics.monitors.VMWare.metrics.Metric[] metrics = vmWareMetrics.getHostMetrics().getMetrics();
            for (com.appdynamics.monitors.VMWare.metrics.Metric metric : metrics) {

                String name = metric.getName();
                BigDecimal value = null;

                try {

                    if ("Distributed CPU Fairness".equals(name)) {
                        value = BigDecimal.valueOf(hostStats.getDistributedCpuFairness());
                    } else if ("Distributed Memory Fairness".equals(name)) {
                        value = BigDecimal.valueOf(hostStats.getDistributedMemoryFairness());
                    } else if ("Overall CPU Usage".equals(name)) {
                        value = BigDecimal.valueOf(hostStats.getOverallCpuUsage());
                    } else if ("Overall CPU Usage %".equals(name)) {
                        double totalCapacityMHz = totalHz * numCpuCores * 0.000001;
                        double cpuUsagePercent = (hostStats.getOverallCpuUsage() * 100) / totalCapacityMHz;
                        value = BigDecimal.valueOf(Math.round(cpuUsagePercent));
                    } else if ("Overall Memory Usage".equals(name)) {
                        value = BigDecimal.valueOf(hostStats.getOverallMemoryUsage());
                    } else if ("Up Time".equals(name)) {
                        value = BigDecimal.valueOf(hostStats.getUptime());
                    } else if ("Memory Size".equals(name)) {
                        value = BigDecimal.valueOf(hostSystem.getHardware().getMemorySize());
                    } else if ("CPU Cores".equals(name)) {
                        value = BigDecimal.valueOf(hostSystem.getHardware().getCpuInfo().getNumCpuCores());
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
                } catch (Exception e) {
                    logger.debug("Error collecting metric [{}] on host[{}]", name, hostName, e);
                }
            }
            logger.info("Finished collecting host [{}] metrics", hostName);
            logger.info("Started collecting VM metrics of host [{}]", hostName);

            List<VirtualMachine> vms = getVMs(hostSystem);

            for (VirtualMachine vm : vms) {
                logger.info("Collecting vm [{}] metrics of host [{}]", vm.getName(), hostName);
                VMMetricCollector vmMetricCollector = new VMMetricCollector(vm, baseMetricName, vmWareMetrics.getVmMetrics(), getCollectedMetrics(), getMetricCollectorsPhaser());
                executorService.execute("VMMetricCollector-" + vm.getName(), vmMetricCollector);
            }

        } catch (Exception e) {
            logger.error("Error collecting metrics from host [{}]", hostName, e);
        } finally {
            getMetricCollectorsPhaser().arriveAndDeregister();
        }


    }

    private List<VirtualMachine> getVMs(HostSystem hostSystem) {
        List<VirtualMachine> allVMs = new ArrayList<VirtualMachine>();
        String hostName = hostSystem.getName();
        logger.info("Collecting vms for [{}]", hostName);

        try {
            VirtualMachine[] vms = hostSystem.getVms();


            List<String> vmConfigForHost = getVMConfigForHost(hostName, hostConfig);

            if (vms != null && vms.length > 0 && vmConfigForHost != null && vmConfigForHost.size() > 0) {

                logger.debug("Found [{}] vms for host [{}]", vms.length, hostName);
                if (logger.isTraceEnabled()) {
                    StringBuilder sb = new StringBuilder();
                    for (VirtualMachine vm : vms) {
                        if (sb.length() > 0) {
                            sb.append(",");
                        }
                        sb.append(vm.getName());
                    }
                    logger.trace("VM machines [{}]", sb.toString());
                }


                for (String vmNameFromConfig : vmConfigForHost) {
                    boolean foundVM = false;
                    boolean allVms = false;

                    if ("*".equals(vmNameFromConfig)) {
                        allVms = true;
                    }

                    for (VirtualMachine virtualMachine : vms) {
                        String vmName = virtualMachine.getName();

                        if (vmName.equalsIgnoreCase(vmNameFromConfig) || "*".equals(vmNameFromConfig)) {
                            allVMs.add(virtualMachine);
                            foundVM = true;
                            if (!allVms) {
                                break;
                            }
                        }
                    }

                    if (!foundVM) {
                        logger.error("Could not find vm with name " + vmNameFromConfig);
                    }
                }
            } else {
                logger.info("No vm's configured for the host [{}]", hostName);
            }
        } catch (RemoteException e) {
            logger.error("Unable to get the VMs for host [{}]", hostName, e);
        }

        logger.info("VMs size for [{}] after filtering is [{}]", hostName, allVMs.size());

        if (logger.isTraceEnabled()) {
            logger.trace("Found VMs [{}]", allVMs);
        }

        return allVMs;
    }

    private List<String> getVMConfigForHost(String hostName, List<Map<String, Object>> hostConfigs) {
        for (Map<String, Object> hostConfig : hostConfigs) {

            String hostNameFromConfig = (String) hostConfig.get("host");

            if (hostName.equalsIgnoreCase(hostNameFromConfig) || "*".equals(hostNameFromConfig)) {
                return (List<String>) hostConfig.get("vms");
            }
        }
        return Lists.newArrayList();
    }
}

