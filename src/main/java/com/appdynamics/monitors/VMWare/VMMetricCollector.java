package com.appdynamics.monitors.VMWare;

import com.appdynamics.extensions.util.MetricWriteHelper;
import com.vmware.vim25.VirtualHardware;
import com.vmware.vim25.VirtualMachineQuickStats;
import com.vmware.vim25.mo.VirtualMachine;
import org.apache.log4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

/**
 * @author Satish Muddam
 */
public class VMMetricCollector extends BaseMetricCollector {

    private static final Logger logger = Logger.getLogger(VMMetricCollector.class);

    private final Map<Pattern, String> replacers;
    private final ExecutorService executorService;
    private CountDownLatch hostCountDown;


    public VMMetricCollector(MetricWriteHelper metricWriter, String metricPrefix, Map<Pattern, String> replacers, Integer vmThreads, CountDownLatch hostCountDown) {
        super(metricWriter, metricPrefix);
        this.replacers = replacers;
        executorService = Executors.newFixedThreadPool(vmThreads);
        this.hostCountDown = hostCountDown;
        createShutDownHandler();
    }

    private void createShutDownHandler() {
        ExecutorService poolShutdownService = Executors.newFixedThreadPool(1);
        poolShutdownService.submit(new Runnable() {
            public void run() {
                try {
                    hostCountDown.await();
                    if (executorService != null && !executorService.isShutdown()) {
                        executorService.shutdown();
                    }
                } catch (InterruptedException e) {
                    logger.error("Could not wait to shutdown the scheduler ", e);
                }
            }
        });

    }

    public void addVMs(List<VirtualMachine> virtualMachines, String baseMetricPath) {

        for (VirtualMachine virtualMachine : virtualMachines) {
            executorService.submit(new VMMetricTask(virtualMachine, baseMetricPath));
        }
    }

    private class VMMetricTask implements Runnable {

        private VirtualMachine virtualMachine;
        private String baseMetricPath;

        VMMetricTask(VirtualMachine virtualMachine, String baseMetricPath) {
            this.virtualMachine = virtualMachine;
            this.baseMetricPath = baseMetricPath;
        }

        public void run() {

            VirtualMachineQuickStats vmStats = virtualMachine.getSummary().getQuickStats();

            String virtualMachineName = virtualMachine.getName();

            virtualMachineName = applyReplacers(virtualMachineName, replacers);

            String baseMetricName = baseMetricPath + "|" + "VirtualMachine" + "|" + virtualMachineName;
            printMetric(baseMetricName + "|Ballooned Memory", vmStats.getBalloonedMemory());
            printMetric(baseMetricName + "|Compressed Memory", vmStats.getCompressedMemory());
            printMetric(baseMetricName + "|Overhead Memory Consumed", vmStats.getConsumedOverheadMemory());
            printMetric(baseMetricName + "|Distributed CPU Entitlement", vmStats.getDistributedCpuEntitlement());
            printMetric(baseMetricName + "|Distributed Memory Entitlement", vmStats.getDistributedMemoryEntitlement());
            printMetric(baseMetricName + "|Guest Memory Usage", vmStats.getGuestMemoryUsage());
            printMetric(baseMetricName + "|Host Memory Usage", vmStats.getHostMemoryUsage());
            printMetric(baseMetricName + "|Overall CPU Usage", vmStats.getOverallCpuUsage());
            printMetric(baseMetricName + "|Overall CPU Demand", vmStats.getOverallCpuDemand());
            printMetric(baseMetricName + "|Private Memory", vmStats.getPrivateMemory());
            printMetric(baseMetricName + "|Shared Memory", vmStats.getSharedMemory());
            printMetric(baseMetricName + "|Static CPU Entitlement", vmStats.getStaticCpuEntitlement());
            printMetric(baseMetricName + "|Static Memory Entitlement", vmStats.getStaticMemoryEntitlement());
            printMetric(baseMetricName + "|Swapped Memory", vmStats.getSwappedMemory());
            printMetric(baseMetricName + "|Up Time", vmStats.getUptimeSeconds());

            VirtualHardware hardware = virtualMachine.getConfig().getHardware();
            printMetric(baseMetricName + "|Memory MB", hardware.getMemoryMB());
            printMetric(baseMetricName + "|Num CPU", hardware.getNumCPU());

        }
    }
}

