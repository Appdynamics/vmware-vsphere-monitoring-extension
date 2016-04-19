package com.appdynamics.monitors.VMWare;

import com.appdynamics.extensions.util.MetricWriteHelper;
import com.google.common.collect.Lists;
import com.vmware.vim25.HostHardwareInfo;
import com.vmware.vim25.HostListSummaryQuickStats;
import com.vmware.vim25.mo.HostSystem;
import com.vmware.vim25.mo.ManagedEntity;
import com.vmware.vim25.mo.VirtualMachine;
import org.apache.log4j.Logger;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

/**
 * @author Satish Muddam
 */
public class HostMetricCollector extends BaseMetricCollector {

    private static final Logger logger = Logger.getLogger(HostMetricCollector.class);

    private final List<ManagedEntity> hostEntities;
    private final Map<Pattern, String> replacers;
    private final List<Map<String, Object>> hostConfig;
    private final VMMetricCollector vmMetricCollector;
    private final ExecutorService executorService;
    private final CountDownLatch hostCountDown;

    public HostMetricCollector(MetricWriteHelper metricWriter, String metricPrefix, List<ManagedEntity> hostEntities, List<Map<String, Object>> hostConfig, Map<Pattern, String> replacers, Integer hostThreads, Integer vmThreads) {
        super(metricWriter, metricPrefix);
        this.hostEntities = hostEntities;
        this.replacers = replacers;
        this.hostConfig = hostConfig;
        hostCountDown = new CountDownLatch(hostEntities.size());
        vmMetricCollector = new VMMetricCollector(metricWriter, metricPrefix, replacers, vmThreads, hostCountDown);
        executorService = Executors.newFixedThreadPool(hostThreads);
    }

    public void execute() {

        try {
            for (final ManagedEntity managedEntity : hostEntities) {
                executorService.submit(new Runnable() {

                    public void run() {

                        HostSystem hostSystem = (HostSystem) managedEntity;

                        HostListSummaryQuickStats hostStats = hostSystem.getSummary().getQuickStats();

                        long totalHz = hostSystem.getHardware().getCpuInfo().getHz();
                        short numCpuCores = hostSystem.getHardware().getCpuInfo().getNumCpuCores();

                        String hostName = managedEntity.getName();

                        String replacedHostName = applyReplacers(hostName, replacers);

                        String baseMetricName = "HostSystem" + "|" + replacedHostName;

                        printMetric(baseMetricName + "|Distributed CPU Fairness", hostStats.getDistributedCpuFairness());

                        printMetric(baseMetricName + "|Distributed Memory Fairness", hostStats.getDistributedMemoryFairness());

                        Integer overallCpuUsageinMHz = hostStats.getOverallCpuUsage();
                        printMetric(baseMetricName + "|Overall CPU Usage", overallCpuUsageinMHz);


                        double totalCapacityMHz = totalHz * numCpuCores * 0.000001;
                        double cpuUsagePercent = (overallCpuUsageinMHz * 100) / totalCapacityMHz;

                        printMetric(baseMetricName + "|Overall CPU Usage %", Math.round(cpuUsagePercent));

                        printMetric(baseMetricName + "|Overall Memory Usage", hostStats.getOverallMemoryUsage());
                        printMetric(baseMetricName + "|Up Time", hostStats.getUptime());

                        HostHardwareInfo hardwareInfo = hostSystem.getHardware();
                        printMetric(baseMetricName + "|Memory Size", hardwareInfo.getMemorySize());
                        printMetric(baseMetricName + "|CPU Cores", hardwareInfo.getCpuInfo().getNumCpuCores());

                        List<VirtualMachine> vms = getVMs(hostSystem);

                        vmMetricCollector.addVMs(vms, baseMetricName);

                        hostCountDown.countDown();
                    }

                });
            }
        } finally {
            if (executorService != null && !executorService.isShutdown()) {
                executorService.shutdown();
            }
        }
    }

    private List<VirtualMachine> getVMs(HostSystem hostSystem) {
        List<VirtualMachine> allVMs = new ArrayList<VirtualMachine>();

        try {
            VirtualMachine[] vms = hostSystem.getVms();

            if (vms != null && vms.length > 0) {

                String hostName = hostSystem.getName();

                if (logger.isDebugEnabled()) {
                    logger.debug("Found " + vms.length + " vms for host [ " + hostName + " ]");
                    StringBuilder sb = new StringBuilder();
                    for (VirtualMachine vm : vms) {
                        if (sb.length() > 0) {
                            sb.append(",");
                        }
                        sb.append(vm.getName());
                    }
                    logger.debug("VM machines [ " + sb.toString() + " ]");
                }

                List<String> vmConfigForHost = getVMConfigForHost(hostName, hostConfig);

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
            }
        } catch (RemoteException e) {
            logger.error("Unable to get the VMs", e);
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
