/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.monitors.VMWare;

import com.appdynamics.extensions.AMonitorTaskRunnable;
import com.appdynamics.extensions.MetricWriteHelper;
import com.appdynamics.extensions.TasksExecutionServiceProvider;
import com.appdynamics.extensions.conf.MonitorContextConfiguration;
import com.appdynamics.extensions.logging.ExtensionsLoggerFactory;
import com.appdynamics.extensions.util.CryptoUtils;
import com.appdynamics.monitors.VMWare.collectors.HostMetricCollector;
import com.appdynamics.monitors.VMWare.metrics.VMWareMetrics;
import com.appdynamics.monitors.VMWare.util.Constants;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.vmware.vim25.InvalidProperty;
import com.vmware.vim25.RuntimeFault;
import com.vmware.vim25.mo.Folder;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ManagedEntity;
import com.vmware.vim25.mo.ServiceInstance;
import org.slf4j.Logger;

import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Phaser;

public class VMWareMonitorTask implements AMonitorTaskRunnable {

    private static final Logger logger = ExtensionsLoggerFactory.getLogger(VMWareMonitorTask.class);

    private MonitorContextConfiguration contextConfiguration;
    private Map<String, ?> vmWareServer;
    private Folder rootFolder;
    private VMWareMetrics vmWareMetrics;
    private MetricWriteHelper metricWriteHelper;
    private String displayName;

    public VMWareMonitorTask(TasksExecutionServiceProvider tasksExecutionServiceProvider, MonitorContextConfiguration contextConfiguration, Map<String, ?> vmWareServer) {
        this.contextConfiguration = contextConfiguration;
        this.vmWareServer = vmWareServer;
        this.vmWareMetrics = (VMWareMetrics) contextConfiguration.getMetricsXml();
        this.metricWriteHelper = tasksExecutionServiceProvider.getMetricWriteHelper();
        displayName = (String) vmWareServer.get(Constants.DISPLAY_NAME);
    }

    public void onTaskComplete() {
        logger.info("All tasks for server {} finished", vmWareServer.get(com.appdynamics.extensions.Constants.HOST));
    }

    public void run() {

        List<com.appdynamics.extensions.metrics.Metric> collectedMetrics = new ArrayList<>();
        String host = (String) vmWareServer.get(com.appdynamics.extensions.Constants.HOST);

        StringBuilder heartbeatMetricPath = new StringBuilder(contextConfiguration.getMetricPrefix());

        if (!Strings.isNullOrEmpty(displayName)) {
            heartbeatMetricPath.append("|").append(displayName);
        }

        heartbeatMetricPath.append("|HeartBeat");

        try {
            connect();
            com.appdynamics.extensions.metrics.Metric heartBeatMetric = new com.appdynamics.extensions.metrics.Metric(Constants.HEARTBEAT, String.valueOf(1), heartbeatMetricPath.toString());
            collectedMetrics.add(heartBeatMetric);
        } catch (Exception e) {
            com.appdynamics.extensions.metrics.Metric heartBeatMetric = new com.appdynamics.extensions.metrics.Metric(Constants.HEARTBEAT, String.valueOf(0), heartbeatMetricPath.toString());
            collectedMetrics.add(heartBeatMetric);
            metricWriteHelper.transformAndPrintMetrics(collectedMetrics);
            throw e;
        }

        //If rootFolder is null, exit
        if (rootFolder == null) {
            logger.error("Could not establish connection to host [{}], not collecting metrics", host);
            return;
        }

        List<Map<String, Object>> hostConfig = (List<Map<String, Object>>) vmWareServer.get(Constants.HOSTCONFIG);
        if (hostConfig != null && !hostConfig.isEmpty()) {

            Phaser metricCollectorsPhaser = new Phaser();
            metricCollectorsPhaser.register();

            List<ManagedEntity> hostEntities = getHostMachines(rootFolder, hostConfig);
            if (logger.isDebugEnabled()) {
                logger.debug("Found " + hostEntities.size() + " hosts");
                StringBuilder sb = new StringBuilder();
                for (ManagedEntity managedEntity : hostEntities) {
                    if (sb.length() > 0) {
                        sb.append(",");
                    }
                    sb.append(managedEntity.getName());
                }
                logger.debug("Host machines [{}]", sb.toString());
            }

            for (final ManagedEntity hostEntity : hostEntities) {

                String metricPrefixWithDisplayName = contextConfiguration.getMetricPrefix();
                if (!Strings.isNullOrEmpty(displayName)) {
                    metricPrefixWithDisplayName += "|" + displayName;
                }


                HostMetricCollector hostMetricCollector = new HostMetricCollector(metricPrefixWithDisplayName, hostEntity, hostConfig, metricCollectorsPhaser, contextConfiguration.getContext().getExecutorService(), collectedMetrics, vmWareMetrics);
                contextConfiguration.getContext().getExecutorService().execute("HostMetricCollector-" + hostEntity.getName(), hostMetricCollector);
            }

            metricCollectorsPhaser.arriveAndAwaitAdvance();

            //Close connection
            try {
                logger.info("Closing the connection to server [{}]", host);
                rootFolder.getServerConnection().logout();
            } catch (Exception e) {
                logger.error("Unable to close the connection", e);
            }

            if (collectedMetrics.size() > 0) {
                logger.debug("Printing {} metrics", collectedMetrics.size());
                metricWriteHelper.transformAndPrintMetrics(collectedMetrics);
            }

        } else {
            logger.info("hostConfig not specified in configuration. Exiting the process");
        }
    }

    private void connect() {

        if (vmWareServer != null) {
            String host = (String) vmWareServer.get(com.appdynamics.extensions.Constants.HOST);
            String username = (String) vmWareServer.get(com.appdynamics.extensions.Constants.USER);
            String password = getPassword(vmWareServer);

            String url = "https://" + host + "/sdk";
            try {
                ServiceInstance serviceInstance = new ServiceInstance(new URL(url), username, password, true);
                rootFolder = serviceInstance.getRootFolder();
            } catch (Exception e) {
                logger.error("Unable to connect to the host [{}]", host, e);
                throw new RuntimeException("Unable to connect to the host [" + host + "]", e);
            }
            logger.info("Connection to: " + url + " Successful");
        } else {
            logger.error("Server configuration is null, exiting without collecting metrics.");
            throw new RuntimeException("Server configuration is null, exiting without collecting metrics.");
        }
    }

    private String getPassword(Map<String, ?> config) {
        String password = null;

        if (!Strings.isNullOrEmpty((String) config.get(com.appdynamics.extensions.Constants.PASSWORD))) {
            password = (String) config.get(com.appdynamics.extensions.Constants.PASSWORD);
        } else {
            try {
                Map<String, String> args = Maps.newHashMap();
                args.put(com.appdynamics.extensions.Constants.ENCRYPTED_PASSWORD, (String) config.get(com.appdynamics.extensions.Constants.ENCRYPTED_PASSWORD));
                args.put(com.appdynamics.extensions.Constants.ENCRYPTION_KEY, (String) config.get(com.appdynamics.extensions.Constants.ENCRYPTION_KEY));
                password = CryptoUtils.getPassword(args);

            } catch (IllegalArgumentException e) {
                String msg = "Encryption Key not specified. Please set the value in config.yaml.";
                logger.error(msg);
                throw new IllegalArgumentException(msg);
            }
        }
        return password;
    }

    private List<ManagedEntity> getHostMachines(Folder rootFolder, List<Map<String, Object>> hostConfigs) {
        List<ManagedEntity> hostEntities = new ArrayList<ManagedEntity>();

        for (Map<String, Object> hostConfig : hostConfigs) {
            String hostName = (String) hostConfig.get(com.appdynamics.extensions.Constants.HOST);
            try {
                if ("*".equals(hostName)) {
                    hostEntities = Arrays.asList(new InventoryNavigator(rootFolder).searchManagedEntities(Constants.HOSTSYSTEM));
                } else {
                    ManagedEntity hostSystem = new InventoryNavigator(rootFolder).searchManagedEntity(Constants.HOSTSYSTEM, hostName);
                    if (hostSystem != null) {
                        hostEntities.add(hostSystem);
                    } else {
                        logger.error("Could not find Host with name " + hostName);
                    }
                }
            } catch (InvalidProperty invalidProperty) {
                logger.error("Unable to get the host details", invalidProperty);
            } catch (RuntimeFault runtimeFault) {
                logger.error("Unable to get the host details", runtimeFault);
            } catch (RemoteException e) {
                logger.error("Unable to get the host details", e);
            }
        }
        return hostEntities;
    }
}
