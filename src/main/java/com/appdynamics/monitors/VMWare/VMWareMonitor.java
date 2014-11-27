package com.appdynamics.monitors.VMWare;

import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.MetricWriter;
import com.singularity.ee.agent.systemagent.api.TaskExecutionContext;
import com.singularity.ee.agent.systemagent.api.TaskOutput;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import com.vmware.vim25.HostListSummaryQuickStats;
import com.vmware.vim25.VirtualMachineQuickStats;
import com.vmware.vim25.mo.Folder;
import com.vmware.vim25.mo.HostSystem;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ManagedEntity;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.VirtualMachine;
import org.apache.log4j.Logger;

import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VMWareMonitor extends AManagedMonitor {
    /**
     * The metric can be found in Application Infrastructure Performance|{@literal <}Node{@literal >}|Custom Metrics|vmware|Status
     */
    public static String metricPrefix = "Custom Metrics|vmware|Status|";
    private static final String ONE = "1";
    private static final Logger logger = Logger.getLogger(VMWareMonitor.class);

    public VMWareMonitor() {
        String msg = "Using Monitor Version [" + getImplementationVersion() + "]";
        logger.info(msg);
        System.out.println(msg);
    }

    private static String getImplementationVersion() {
        return VMWareMonitor.class.getPackage().getImplementationTitle();
    }


    /**
     * Fetches virtual machine statistics from VSphere
     *
     * @param root    root folder
     * @param vmNames vm names
     * @throws RemoteException
     */
    private Map<String, Number> populateVMMetrics(Folder root, List<String> vmNames) throws RemoteException {
        Map<String, Number> vmMetrics = new HashMap<String, Number>();
        if (vmNames == null || vmNames.size() <= 0) {
            logger.info("Please configure vmnames to get VM metrics");
            return vmMetrics;
        }
        List<ManagedEntity> managedEntities = new ArrayList<ManagedEntity>();
        // Get all VMs
        if (vmNames.size() == 1 && vmNames.get(0).equals("*")) {
            managedEntities = Arrays.asList(new InventoryNavigator(root).searchManagedEntities("VirtualMachine"));
        }
        // Get specific VMs
        else {
            for (String vmName : vmNames) {
                managedEntities.add(new InventoryNavigator(root).searchManagedEntity("VirtualMachine", vmName));
            }
        }
        for (ManagedEntity managedEntity : managedEntities) {

            if (managedEntity == null) {
                logger.info("Could not find VM");
                continue;
            }
            VirtualMachineQuickStats vmStats = ((VirtualMachine) managedEntity).getSummary().getQuickStats();

            String baseMetricName = "VirtualMachine" + "|" + managedEntity.getName();
            vmMetrics.put(baseMetricName + "|Ballooned Memory", vmStats.getBalloonedMemory());
            vmMetrics.put(baseMetricName + "|Compressed Memory", vmStats.getCompressedMemory());
            vmMetrics.put(baseMetricName + "|Overhead Memory Consumed", vmStats.getConsumedOverheadMemory());
            vmMetrics.put(baseMetricName + "|Distributed CPU Entitlement", vmStats.getDistributedCpuEntitlement());
            vmMetrics.put(baseMetricName + "|Distributed Memory Entitlement", vmStats.getDistributedMemoryEntitlement());
            vmMetrics.put(baseMetricName + "|Guest Memory Usage", vmStats.getGuestMemoryUsage());
            vmMetrics.put(baseMetricName + "|Host Memory Usage", vmStats.getHostMemoryUsage());
            vmMetrics.put(baseMetricName + "|Overall CPU Usage", vmStats.getOverallCpuUsage());
            vmMetrics.put(baseMetricName + "|Overall CPU Demand", vmStats.getOverallCpuDemand());
            vmMetrics.put(baseMetricName + "|Private Memory", vmStats.getPrivateMemory());
            vmMetrics.put(baseMetricName + "|Shared Memory", vmStats.getSharedMemory());
            vmMetrics.put(baseMetricName + "|Static CPU Entitlement", vmStats.getStaticCpuEntitlement());
            vmMetrics.put(baseMetricName + "|Static Memory Entitlement", vmStats.getStaticMemoryEntitlement());
            vmMetrics.put(baseMetricName + "|Swapped Memory", vmStats.getSwappedMemory());
            vmMetrics.put(baseMetricName + "|Up Time", vmStats.getUptimeSeconds());
        }
        return vmMetrics;
    }

    /**
     * Fetches host statistics from VSphere
     *
     * @param root      root folder
     * @param hostNames host names
     * @throws RemoteException
     */
    private Map<String, Number> populateHostMetrics(Folder root, List<String> hostNames) throws RemoteException {
        Map<String, Number> hostMetrics = new HashMap<String, Number>();
        if (hostNames == null || hostNames.size() <= 0) {
            logger.info("Please configure hostnames to get host metrics");
            return hostMetrics;
        }
        List<ManagedEntity> managedEntities = new ArrayList<ManagedEntity>();
        // Get all VMs
        if (hostNames.size() == 1 && hostNames.get(0).equals("*")) {
            managedEntities = Arrays.asList(new InventoryNavigator(root).searchManagedEntities("HostSystem"));
        }
        // Get specific VMs
        else {
            for (String hostName : hostNames) {
                managedEntities.add(new InventoryNavigator(root).searchManagedEntity("HostSystem", hostName));
            }
        }
        for (ManagedEntity managedEntity : managedEntities) {

            if (managedEntity == null) {
                logger.info("Could not find host");
                continue;
            }
            HostListSummaryQuickStats hostStats = ((HostSystem) managedEntity).getSummary().getQuickStats();


            String baseMetricName = "HostSystem" + "|" + managedEntity.getName();
            hostMetrics.put(baseMetricName + "|Distributed CPU Fairness", hostStats.getDistributedCpuFairness());

            hostMetrics.put(baseMetricName + "|Distributed Memory Fairness", hostStats.getDistributedMemoryFairness());

            hostMetrics.put(baseMetricName + "|Overall CPU Usage", hostStats.getOverallCpuUsage());
            hostMetrics.put(baseMetricName + "|Overall Memory Usage", hostStats.getOverallMemoryUsage());
            hostMetrics.put(baseMetricName + "|Up Time", hostStats.getUptime());
        }
        return hostMetrics;
    }

    /**
     * Creates an active connection to VSphere Manager
     *
     * @param host         host
     * @param username     username
     * @param password     password
     * @param vmNames      vm names
     * @param hostNames    host names
     * @param metricPrefix metric prefix
     * @throws TaskExecutionException
     */
    private void connectAndFetchStats(String host, String username, String password, List<String> vmNames, List<String> hostNames, String metricPrefix) throws TaskExecutionException {
        String url = "https://" + host + "/sdk";
        try {
            ServiceInstance serviceInstance = new ServiceInstance(new URL(url), username, password, true);
            Folder rootFolder = serviceInstance.getRootFolder();
            logger.info("Connection to: " + url + " Successful");

            Map<String, Number> vmStats = populateVMMetrics(rootFolder, vmNames);
            printStats(vmStats, metricPrefix);

            Map<String, Number> hostStats = populateHostMetrics(rootFolder, hostNames);
            printStats(hostStats, metricPrefix);

            close(serviceInstance);
        } catch (Exception e) {
            logger.error("Unable to connect to the host [" + host + "]", e);
            throw new TaskExecutionException("Unable to connect to the host [" + host + "]", e);
        }
    }

    private void printStats(Map<String, Number> stats, String metricPrefix) {
        if (stats == null) {
            return;
        }

        for (Map.Entry<String, Number> stat : stats.entrySet()) {
            printMetric(metricPrefix + stat.getKey(), stat.getValue());
        }
    }

    /**
     * Closes the active connection to VSphere
     *
     * @param serviceInstance service instance
     */
    private void close(ServiceInstance serviceInstance) {
        if (serviceInstance != null) {
            serviceInstance.getServerConnection().logout();
            logger.info("Connection closed");
        }
    }

    /**
     * Main execution of the Monitor
     *
     * @see com.singularity.ee.agent.systemagent.api.ITask#execute(java.util.Map, com.singularity.ee.agent.systemagent.api.TaskExecutionContext)
     */
    public TaskOutput execute(Map<String, String> taskArguments, TaskExecutionContext taskExecutionContext) throws TaskExecutionException {
        try {
            String host = taskArguments.get("host");
            String username = taskArguments.get("username");
            String password = taskArguments.get("password");

            String metricPrefix = taskArguments.get("metricPrefix");
            if (metricPrefix == null || metricPrefix.length() <= 0) {
                metricPrefix = VMWareMonitor.metricPrefix;
            }

            List<String> vmNames = getNamedArgument(taskArguments, "vmnames");
            List<String> hostNames = getNamedArgument(taskArguments, "hostnames");

            connectAndFetchStats(host, username, password, vmNames, hostNames, metricPrefix);
            logger.info("Finished execution");
            return new TaskOutput("Finished execution");
        } catch (Exception e) {
            logger.error("Failed tp execute the VMWare monitoring task", e);
            throw new TaskExecutionException("Failed tp execute the VMWare monitoring task" + e);
        }

    }

    private List<String> getNamedArgument(Map<String, String> taskArguments, String name) {
        String namedArguments = taskArguments.get(name);
        String[] splittingNames = namedArguments.split(",");
        for (int i = 0; i < splittingNames.length; i++) {
            splittingNames[i] = splittingNames[i].replaceAll("\\s", "");
        }
        return Arrays.asList(splittingNames);
    }

    /**
     * Returns the metric to the AppDynamics Controller.
     *
     * @param metricName  Name of the Metric
     * @param metricValue Value of the Metric
     */
    public void printMetric(String metricName, Object metricValue) {

        if (metricValue == null) {
            logger.info("Ignoring metric [" + metricName + "] as it has null value");
            return;
        }

        MetricWriter metricWriter = getMetricWriter(metricName,
                MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION,
                MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT,
                MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_INDIVIDUAL
        );

        if (logger.isDebugEnabled()) {
            logger.debug("Metric key:value before ceiling = " + metricName + ":" + String.valueOf(metricValue));
        }
        metricWriter.printMetric(toWholeNumberString(metricValue));
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