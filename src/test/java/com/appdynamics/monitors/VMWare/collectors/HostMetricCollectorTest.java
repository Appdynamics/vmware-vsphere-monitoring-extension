package com.appdynamics.monitors.VMWare.collectors;

import com.appdynamics.extensions.executorservice.MonitorExecutorService;
import com.appdynamics.extensions.metrics.Metric;
import com.appdynamics.monitors.VMWare.metrics.HostMetrics;
import com.appdynamics.monitors.VMWare.metrics.VMWareMetrics;
import com.vmware.vim25.HostCpuInfo;
import com.vmware.vim25.HostHardwareInfo;
import com.vmware.vim25.HostListSummary;
import com.vmware.vim25.HostListSummaryQuickStats;
import com.vmware.vim25.ManagedEntityStatus;
import com.vmware.vim25.mo.HostSystem;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Phaser;

@RunWith(PowerMockRunner.class)
@PrepareForTest({HostMetricCollector.class})
public class HostMetricCollectorTest {

    @Mock
    private HostSystem hostEntity;

    @Mock
    private Phaser metricCollectorsPhaser;

    @Mock
    private MonitorExecutorService executorService;

    @Mock
    private VMWareMetrics vmWareMetrics;

    @Mock
    private HostListSummary hostListSummary;

    @Mock
    private HostListSummaryQuickStats hostListSummaryQuickStats;

    @Mock
    private HostHardwareInfo hostHardwareInfo;

    @Mock
    private HostCpuInfo hostCpuInfo;

    @Mock
    private HostMetrics hostMetrics;


    @Test
    public void shouldNotCollectHostMetricsWhenStatusIsRed() {
        List<Map<String, Object>> hostConfigs = new ArrayList<>();
        Map<String, Object> hostConfig = new HashMap<>();
        hostConfig.put("host", "host1");
        hostConfigs.add(hostConfig);

        List<Metric> collectedMetrics = new ArrayList<>();

        String metricPrefix = "Custom Metrics|vmware|Status|TestVMWare";

        Mockito.when(hostEntity.getName()).thenReturn("host1");

        Mockito.when(hostEntity.getOverallStatus()).thenReturn(ManagedEntityStatus.red);


        HostMetricCollector hostMetricCollector = new HostMetricCollector(metricPrefix, hostEntity, hostConfigs, metricCollectorsPhaser, executorService, collectedMetrics, vmWareMetrics);
        hostMetricCollector.run();

        Assert.assertEquals("Collected metrics should be 1", 1, collectedMetrics.size());
        Metric metric = collectedMetrics.get(0);
        String metricName = metric.getMetricPath();
        String metricValue = metric.getMetricValue();
        Assert.assertEquals("Metric name should be the host status", "Custom Metrics|vmware|Status|TestVMWare|HostSystem|host1|Status", metricName);
        Assert.assertEquals("Metric value should be of the status red", "3", metricValue);
    }

    @Test
    public void shouldCollectHostMetricsWhenStatusIsNotRed() {
        List<Map<String, Object>> hostConfigs = new ArrayList<>();
        Map<String, Object> hostConfig = new HashMap<>();
        hostConfig.put("host", "host1");
        hostConfigs.add(hostConfig);

        List<Metric> collectedMetrics = new ArrayList<>();

        String metricPrefix = "Custom Metrics|vmware|Status|TestVMWare";

        Mockito.when(hostEntity.getName()).thenReturn("host1");

        Mockito.when(hostEntity.getOverallStatus()).thenReturn(ManagedEntityStatus.green);

        Mockito.when(hostEntity.getSummary()).thenReturn(hostListSummary);

        Mockito.when(hostListSummary.getQuickStats()).thenReturn(hostListSummaryQuickStats);

        Mockito.when(hostListSummaryQuickStats.getOverallCpuUsage()).thenReturn(3);
        Mockito.when(hostListSummaryQuickStats.getOverallMemoryUsage()).thenReturn(20);

        Mockito.when(hostEntity.getHardware()).thenReturn(hostHardwareInfo);

        Mockito.when(hostHardwareInfo.getCpuInfo()).thenReturn(hostCpuInfo);

        Mockito.when(hostCpuInfo.getHz()).thenReturn(2l);

        Mockito.when(hostCpuInfo.getNumCpuCores()).thenReturn((short) 4);

        Mockito.when(vmWareMetrics.getHostMetrics()).thenReturn(hostMetrics);

        com.appdynamics.monitors.VMWare.metrics.Metric[] metrics = setupMetrics();

        Mockito.when(hostMetrics.getMetrics()).thenReturn(metrics);

        HostMetricCollector hostMetricCollector = new HostMetricCollector(metricPrefix, hostEntity, hostConfigs, metricCollectorsPhaser, executorService, collectedMetrics, vmWareMetrics);
        hostMetricCollector.run();

        Assert.assertTrue("Collected metrics should be > 1", collectedMetrics.size() > 1);

        for (Metric metric : collectedMetrics) {

            String metricName = metric.getMetricName();
            if ("status".equals(metricName)) {
                Assert.assertEquals("Host status should be green", "1", metric.getMetricValue());
            } else if ("Overall CPU Usage".equals(metricName)) {
                Assert.assertEquals("3", metric.getMetricValue());
            } else if ("Overall Memory Usage".equals(metricName)) {
                Assert.assertEquals("20", metric.getMetricValue());
            } else if ("CPU Cores".equals(metricName)) {
                Assert.assertEquals("4", metric.getMetricValue());
            }
        }
    }

    private com.appdynamics.monitors.VMWare.metrics.Metric[] setupMetrics() {
        com.appdynamics.monitors.VMWare.metrics.Metric metric1 = new com.appdynamics.monitors.VMWare.metrics.Metric();
        metric1.setName("Overall CPU Usage");

        com.appdynamics.monitors.VMWare.metrics.Metric metric2 = new com.appdynamics.monitors.VMWare.metrics.Metric();
        metric2.setName("Overall Memory Usage");

        com.appdynamics.monitors.VMWare.metrics.Metric metric3 = new com.appdynamics.monitors.VMWare.metrics.Metric();
        metric3.setName("CPU Cores");

        return new com.appdynamics.monitors.VMWare.metrics.Metric[]{metric1, metric2, metric3};

    }

}
