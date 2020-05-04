/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.monitors.VMWare;

import com.appdynamics.extensions.MetricWriteHelper;
import com.appdynamics.extensions.TasksExecutionServiceProvider;
import com.appdynamics.extensions.conf.MonitorContext;
import com.appdynamics.extensions.conf.MonitorContextConfiguration;
import com.appdynamics.extensions.executorservice.MonitorExecutorService;
import com.appdynamics.monitors.VMWare.collectors.HostMetricCollector;
import com.appdynamics.monitors.VMWare.metrics.HostMetrics;
import com.appdynamics.monitors.VMWare.metrics.VMMetrics;
import com.appdynamics.monitors.VMWare.metrics.VMWareMetrics;
import com.appdynamics.monitors.VMWare.util.Constants;
import com.google.common.collect.Lists;
import com.vmware.vim25.mo.Folder;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ManagedEntity;
import com.vmware.vim25.mo.ServerConnection;
import com.vmware.vim25.mo.ServiceInstance;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Satish Muddam
 */

@RunWith(PowerMockRunner.class)
@PrepareForTest({VMWareMonitorTask.class})
public class VMWareMonitorTaskTest {

    @Mock
    private TasksExecutionServiceProvider tasksExecutionServiceProvider;

    @Mock
    private MonitorContextConfiguration configuration;

    @Mock
    private MonitorContext monitorContext;

    @Mock
    private MonitorExecutorService executorService;

    @Mock
    private ServiceInstance serviceInstance;

    @Mock
    private Folder rootFolder;

    @Mock
    private VMWareMetrics vmWareMetrics;

    @Mock
    private HostMetrics hostMetrics;

    @Mock
    private VMMetrics vmMetrics;

    @Mock
    private InventoryNavigator inventoryNavigator;

    @Mock
    private ManagedEntity managedEntity;

    @Mock
    private HostMetricCollector hostMetricCollector;

    @Mock
    private ServerConnection serverConnection;

    @Mock
    private MetricWriteHelper metricWriteHelper;


    @Test
    public void shouldCloseConnectionAfterMetricCollection() throws Exception {
        Map<String, Object> vmWareServers = setupVMWareServer();
        when(configuration.getMetricsXml()).thenReturn(vmWareMetrics);
        when(vmWareMetrics.getHostMetrics()).thenReturn(hostMetrics);
        when(vmWareMetrics.getVmMetrics()).thenReturn(vmMetrics);

        when(configuration.getContext()).thenReturn(monitorContext);
        when(monitorContext.getExecutorService()).thenReturn(executorService);

        when(configuration.getMetricPrefix()).thenReturn("Custom Metrics|vmware|Status|");

        PowerMockito.whenNew(ServiceInstance.class).withAnyArguments().thenReturn(serviceInstance);
        when(serviceInstance.getRootFolder()).thenReturn(rootFolder);

        PowerMockito.whenNew(InventoryNavigator.class).withAnyArguments().thenReturn(inventoryNavigator);
        when(inventoryNavigator.searchManagedEntity(eq(Constants.HOSTSYSTEM), anyString())).thenReturn(managedEntity);

        PowerMockito.whenNew(HostMetricCollector.class).withAnyArguments().thenReturn(hostMetricCollector);

        when(rootFolder.getServerConnection()).thenReturn(serverConnection);

        when(tasksExecutionServiceProvider.getMetricWriteHelper()).thenReturn(metricWriteHelper);
        doNothing().when(metricWriteHelper).transformAndPrintMetrics(anyList());

        VMWareMonitorTask vmWareMonitorTask = new VMWareMonitorTask(tasksExecutionServiceProvider, configuration, vmWareServers);
        vmWareMonitorTask.run();

        verify(serverConnection, times(1)).logout();
    }

    private Map<String, Object> setupVMWareServer() {
        Map<String, Object> vmWareServers = new HashMap<>();
        vmWareServers.put("displayName", "TestVMWare");
        vmWareServers.put("host", "vsphere:443");
        vmWareServers.put("username", "admin");
        vmWareServers.put("password", "admin");

        List<Map<String, Object>> hostConfigs = new ArrayList<>();
        Map<String, Object> hostConfig = new HashMap<>();
        hostConfig.put("host", "host1");
        hostConfig.put("vms", Lists.newArrayList("vm1", "vm2"));
        hostConfigs.add(hostConfig);

        vmWareServers.put("hostConfig", hostConfigs);
        return vmWareServers;
    }


}
