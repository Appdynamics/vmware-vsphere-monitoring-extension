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
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.PatternLayout;
import org.slf4j.Logger;

import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class VMWareMonitor extends ABaseMonitor {

    private static final String DEFAULT_METRIC_PREFIX = Constants.DEFAULT_METRIC_PREFIX;

    private static final Logger logger = ExtensionsLoggerFactory.getLogger(VMWareMonitor.class);

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

    public static void main(String[] args) throws TaskExecutionException {
        ConsoleAppender ca = new ConsoleAppender();
        ca.setWriter(new OutputStreamWriter(System.out));
        ca.setLayout(new PatternLayout("[%t] %d{DATE} %5p %c{1} - %m%n"));
        ca.setThreshold(Level.DEBUG);

        org.apache.log4j.Logger.getRootLogger().addAppender(ca);

        final VMWareMonitor monitor = new VMWareMonitor();

        final Map<String, String> taskArgs = new HashMap<String, String>();
        taskArgs.put("config-file", "src/main/resources/conf/config.yml");
        taskArgs.put("metric-file", "src/main/resources/conf/metrics.xml");

        //monitor.execute(taskArgs, null);

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(new Runnable() {
            public void run() {
                try {
                    monitor.execute(taskArgs, null);
                } catch (Exception e) {
                    logger.error("Error while running the task", e);
                }
            }
        }, 2, 30, TimeUnit.SECONDS);
    }
}