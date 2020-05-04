/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.monitors.VMWare;

import com.appdynamics.extensions.ABaseMonitor;
import com.appdynamics.extensions.TasksExecutionServiceProvider;
import com.appdynamics.extensions.logging.ExtensionsLoggerFactory;
import com.appdynamics.extensions.util.AssertUtils;
import com.appdynamics.monitors.VMWare.metrics.VMWareMetrics;
import com.appdynamics.monitors.VMWare.util.Constants;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;

public class VMWareMonitor extends ABaseMonitor {

    private static final String DEFAULT_METRIC_PREFIX = Constants.DEFAULT_METRIC_PREFIX;

    private static final Logger logger = ExtensionsLoggerFactory.getLogger(VMWareMonitor.class);

    public VMWareMonitor() {
        String msg = "Using Monitor Version [" + getImplementationVersion() + "]";
        logger.info(msg);
        System.out.println(msg);
    }

    protected String getDefaultMetricPrefix() {
        return DEFAULT_METRIC_PREFIX;
    }

    public String getMonitorName() {
        return Constants.MONITOR_NAME;
    }

    protected void doRun(TasksExecutionServiceProvider tasksExecutionServiceProvider) {

        List<Map<String, ?>> vmWareServers = (List<Map<String, ?>>)
                this.getContextConfiguration().getConfigYml().get(Constants.SERVERS);

        for (Map<String, ?> vmWareServer : vmWareServers) {

            VMWareMonitorTask task = new VMWareMonitorTask(tasksExecutionServiceProvider, this.getContextConfiguration(), vmWareServer);

            String displayName = (String) vmWareServer.get(Constants.DISPLAY_NAME);

            if (vmWareServers.size() > 1) {
                AssertUtils.assertNotNull(displayName,
                        "The displayName can not be null when multiple servers are configured");
            }

            String taskName = displayName == null ? "DEFAULT" : displayName;
            tasksExecutionServiceProvider.submit(taskName, task);
        }
    }

    @Override
    protected List<Map<String, ?>> getServers() {
        List<Map<String, ?>> servers = (List<Map<String, ?>>) this.getContextConfiguration().getConfigYml().get(Constants.SERVERS);
        AssertUtils.assertNotNull(servers, "The 'servers' section in config.yml is not initialised");
        return servers;
    }

    @Override
    protected void initializeMoreStuff(Map<String, String> args) {
        getContextConfiguration().setMetricXml(args.get(Constants.METRIC_FILE), VMWareMetrics.class);
    }
}