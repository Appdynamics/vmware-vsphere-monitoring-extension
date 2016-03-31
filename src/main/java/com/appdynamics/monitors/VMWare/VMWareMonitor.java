package com.appdynamics.monitors.VMWare;

import static com.appdynamics.extensions.yml.YmlReader.readFromFile;

import com.appdynamics.TaskInputArgs;
import com.appdynamics.extensions.PathResolver;
import com.appdynamics.extensions.crypto.CryptoUtil;
import com.appdynamics.monitors.VMWare.config.Configuration;
import com.appdynamics.monitors.VMWare.config.HostConfig;
import com.appdynamics.monitors.VMWare.config.MetricCharacterReplacer;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.MetricWriter;
import com.singularity.ee.agent.systemagent.api.TaskExecutionContext;
import com.singularity.ee.agent.systemagent.api.TaskOutput;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import com.vmware.vim25.HostHardwareInfo;
import com.vmware.vim25.HostListSummaryQuickStats;
import com.vmware.vim25.InvalidProperty;
import com.vmware.vim25.RuntimeFault;
import com.vmware.vim25.VirtualHardware;
import com.vmware.vim25.VirtualMachineQuickStats;
import com.vmware.vim25.mo.Folder;
import com.vmware.vim25.mo.HostSystem;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ManagedEntity;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.VirtualMachine;
import org.apache.log4j.Logger;

import java.io.File;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VMWareMonitor extends AManagedMonitor {
    /**
     * The metric can be found in Application Infrastructure Performance|{@literal <}Node{@literal >}|Custom Metrics|vmware|Status
     */
    public static String metricPrefix = "Custom Metrics|vmware|Status|";
    private static final String ONE = "1";
    private static final Logger logger = Logger.getLogger(VMWareMonitor.class);

    private static final String CONFIG_ARG = "config-file";
    private static final String FILE_NAME = "monitors/VMWareMonitor/config.yml";

    public VMWareMonitor() {
        String msg = "Using Monitor Version [" + getImplementationVersion() + "]";
        logger.info(msg);
        System.out.println(msg);
    }

    private static String getImplementationVersion() {
        return VMWareMonitor.class.getPackage().getImplementationTitle();
    }

    /**
     * Main execution of the Monitor
     *
     * @see com.singularity.ee.agent.systemagent.api.ITask#execute(java.util.Map, com.singularity.ee.agent.systemagent.api.TaskExecutionContext)
     */
    public TaskOutput execute(Map<String, String> taskArguments, TaskExecutionContext taskExecutionContext) throws TaskExecutionException {
        try {

            logger.info("Starting the VMWare Monitoring task.");
            String configFilename = getConfigFilename(taskArguments.get(CONFIG_ARG));
            Configuration config = readFromFile(configFilename, Configuration.class);
            String host = config.getHost();
            String username = config.getUsername();
            String password = getPassword(config);

            String metricPrefix = config.getMetricPrefix();
            if (Strings.isNullOrEmpty(metricPrefix)) {
                metricPrefix = VMWareMonitor.metricPrefix;
            }

            List<HostConfig> hostConfigs = config.getHostConfig();
            if (hostConfigs != null && !hostConfigs.isEmpty()) {

                List<MetricCharacterReplacer> metricCharacterReplacers = config.getMetricCharacterReplacer();

                Map<Pattern, String> replacers = new HashMap<Pattern, String>();

                if (metricCharacterReplacers != null) {
                    for (MetricCharacterReplacer metricCharacterReplacer : metricCharacterReplacers) {
                        String replace = metricCharacterReplacer.getReplace();
                        String replaceWith = metricCharacterReplacer.getReplaceWith();

                        Pattern pattern = Pattern.compile(replace);

                        replacers.put(pattern, replaceWith);
                    }
                }

                connectAndFetchStats(host, username, password, hostConfigs, replacers, metricPrefix);
            }
            logger.info("Finished execution");
            return new TaskOutput("Finished execution");
        } catch (Exception e) {
            logger.error("Failed to execute the VMWare monitoring task", e);
            throw new TaskExecutionException("Failed to execute the VMWare monitoring task" + e);
        }
    }

    /**
     * Creates an active connection to VSphere Manager
     *
     * @param host         host
     * @param username     username
     * @param password     password
     * @param hostConfigs  host config
     * @param replacers
     * @param metricPrefix metric prefix  @throws TaskExecutionException
     */
    private void connectAndFetchStats(String host, String username, String password, List<HostConfig> hostConfigs, Map<Pattern, String> replacers, String metricPrefix) throws TaskExecutionException {
        String url = "https://" + host + "/sdk";
        try {
            ServiceInstance serviceInstance = new ServiceInstance(new URL(url), username, password, true);
            Folder rootFolder = serviceInstance.getRootFolder();
            logger.info("Connection to: " + url + " Successful");

            populateMetrics(rootFolder, hostConfigs, replacers, metricPrefix);

            close(serviceInstance);
        } catch (Exception e) {
            logger.error("Unable to connect to the host [" + host + "]", e);
            throw new TaskExecutionException("Unable to connect to the host [" + host + "]", e);
        }
    }

    private void populateMetrics(Folder rootFolder, List<HostConfig> hostConfigs, Map<Pattern, String> replacers, String metricPrefix) {

        List<ManagedEntity> hostEntities = getHostMachines(rootFolder, hostConfigs);

        for (ManagedEntity hostEntity : hostEntities) {
            Map<String, Number> metrics = populateHostAndVMMetrics(hostEntity, hostConfigs, replacers);
            printStats(metrics, metricPrefix);
        }
    }

    private List<ManagedEntity> getHostMachines(Folder rootFolder, List<HostConfig> hostConfigs) {
        List<ManagedEntity> hostEntities = new ArrayList<ManagedEntity>();

        for (HostConfig hostConfig : hostConfigs) {
            String hostName = hostConfig.getHost();
            try {
                if ("*".equals(hostName)) {
                    hostEntities = Arrays.asList(new InventoryNavigator(rootFolder).searchManagedEntities("HostSystem"));
                } else {
                    ManagedEntity hostSystem = new InventoryNavigator(rootFolder).searchManagedEntity("HostSystem", hostName);
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

    /**
     * Fetches virtual machine statistics from VSphere
     *
     * @param virtualMachine virtual machine
     * @param baseMetricPath base metric path
     * @param hostMetrics    host metrics
     * @param replacers
     */
    private void populateVMMetrics(VirtualMachine virtualMachine, String baseMetricPath, Map<String, Number> hostMetrics, Map<Pattern, String> replacers) {

        VirtualMachineQuickStats vmStats = virtualMachine.getSummary().getQuickStats();

        String virtualMachineName = virtualMachine.getName();

        virtualMachineName = applyReplacers(virtualMachineName, replacers);

        String baseMetricName = baseMetricPath + "|" + "VirtualMachine" + "|" + virtualMachineName;
        hostMetrics.put(baseMetricName + "|Ballooned Memory", vmStats.getBalloonedMemory());
        hostMetrics.put(baseMetricName + "|Compressed Memory", vmStats.getCompressedMemory());
        hostMetrics.put(baseMetricName + "|Overhead Memory Consumed", vmStats.getConsumedOverheadMemory());
        hostMetrics.put(baseMetricName + "|Distributed CPU Entitlement", vmStats.getDistributedCpuEntitlement());
        hostMetrics.put(baseMetricName + "|Distributed Memory Entitlement", vmStats.getDistributedMemoryEntitlement());
        hostMetrics.put(baseMetricName + "|Guest Memory Usage", vmStats.getGuestMemoryUsage());
        hostMetrics.put(baseMetricName + "|Host Memory Usage", vmStats.getHostMemoryUsage());
        hostMetrics.put(baseMetricName + "|Overall CPU Usage", vmStats.getOverallCpuUsage());
        hostMetrics.put(baseMetricName + "|Overall CPU Demand", vmStats.getOverallCpuDemand());
        hostMetrics.put(baseMetricName + "|Private Memory", vmStats.getPrivateMemory());
        hostMetrics.put(baseMetricName + "|Shared Memory", vmStats.getSharedMemory());
        hostMetrics.put(baseMetricName + "|Static CPU Entitlement", vmStats.getStaticCpuEntitlement());
        hostMetrics.put(baseMetricName + "|Static Memory Entitlement", vmStats.getStaticMemoryEntitlement());
        hostMetrics.put(baseMetricName + "|Swapped Memory", vmStats.getSwappedMemory());
        hostMetrics.put(baseMetricName + "|Up Time", vmStats.getUptimeSeconds());

        VirtualHardware hardware = virtualMachine.getConfig().getHardware();
        hostMetrics.put(baseMetricName + "|Memory MB", hardware.getMemoryMB());
        hostMetrics.put(baseMetricName + "|Num CPU", hardware.getNumCPU());
    }

    /**
     * Fetches host statistics from VSphere
     *
     * @param hostEntity host entity
     * @param replacers
     */
    private Map<String, Number> populateHostAndVMMetrics(ManagedEntity hostEntity, List<HostConfig> hostConfigs, Map<Pattern, String> replacers) {
        Map<String, Number> hostMetrics = new HashMap<String, Number>();

        HostSystem hostSystem = (HostSystem) hostEntity;

        HostListSummaryQuickStats hostStats = hostSystem.getSummary().getQuickStats();

        String hostName = hostEntity.getName();

        String replacedHostName = applyReplacers(hostName, replacers);

        String baseMetricName = "HostSystem" + "|" + replacedHostName;
        hostMetrics.put(baseMetricName + "|Distributed CPU Fairness", hostStats.getDistributedCpuFairness());

        hostMetrics.put(baseMetricName + "|Distributed Memory Fairness", hostStats.getDistributedMemoryFairness());

        hostMetrics.put(baseMetricName + "|Overall CPU Usage", hostStats.getOverallCpuUsage());
        hostMetrics.put(baseMetricName + "|Overall Memory Usage", hostStats.getOverallMemoryUsage());
        hostMetrics.put(baseMetricName + "|Up Time", hostStats.getUptime());

        HostHardwareInfo hardwareInfo = hostSystem.getHardware();
        hostMetrics.put(baseMetricName + "|Memory Size", hardwareInfo.getMemorySize());
        hostMetrics.put(baseMetricName + "|CPU Cores", hardwareInfo.getCpuInfo().getNumCpuCores());

        List<String> vmConfigForHost = getVMConfigForHost(hostName, hostConfigs);

        try {
            VirtualMachine[] vms = hostSystem.getVms();


            if (vms != null && vms.length > 0) {

                for (String vmNameFromConfig : vmConfigForHost) {
                    boolean foundVM = false;
                    boolean allVms = false;

                    if ("*".equals(vmNameFromConfig)) {
                        allVms = true;
                    }

                    for (VirtualMachine virtualMachine : vms) {
                        String vmName = virtualMachine.getName();

                        if (vmName.equalsIgnoreCase(vmNameFromConfig) || "*".equals(vmNameFromConfig)) {
                            populateVMMetrics(virtualMachine, baseMetricName, hostMetrics, replacers);
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

        return hostMetrics;
    }

    private List<String> getVMConfigForHost(String hostName, List<HostConfig> hostConfigs) {
        for (HostConfig hostConfig : hostConfigs) {
            if (hostName.equalsIgnoreCase(hostConfig.getHost()) || "*".equals(hostConfig.getHost())) {
                return hostConfig.getVms();
            }
        }
        return Lists.newArrayList();
    }

    private void printStats(Map<String, Number> stats, String metricPrefix) {
        if (stats == null) {
            return;
        }

        for (Map.Entry<String, Number> stat : stats.entrySet()) {

            String metricName = stat.getKey();

            printMetric(metricPrefix + metricName, stat.getValue());
        }
    }

    private String applyReplacers(String name, Map<Pattern, String> replacers) {

        if (name == null || name.length() == 0 || replacers == null) {
            return name;
        }

        for (Map.Entry<Pattern, String> replacerEntry : replacers.entrySet()) {

            Pattern pattern = replacerEntry.getKey();

            Matcher matcher = pattern.matcher(name);
            name = matcher.replaceAll(replacerEntry.getValue());
        }

        return name;
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


    private String getPassword(Configuration config) {
        String password = null;

        if (!Strings.isNullOrEmpty(config.getPassword())) {
            password = config.getPassword();

        } else {
            try {
                Map<String, String> args = Maps.newHashMap();
                args.put(TaskInputArgs.PASSWORD_ENCRYPTED, config.getEncryptedPassword());
                args.put(TaskInputArgs.ENCRYPTION_KEY, config.getEncryptionKey());
                password = CryptoUtil.getPassword(args);

            } catch (IllegalArgumentException e) {
                String msg = "Encryption Key not specified. Please set the value in config.yaml.";
                logger.error(msg);
                throw new IllegalArgumentException(msg);
            }
        }

        return password;
    }

    private String getConfigFilename(String filename) {
        if (filename == null) {
            return "";
        }

        if ("".equals(filename)) {
            filename = FILE_NAME;
        }
        // for absolute paths
        if (new File(filename).exists()) {
            return filename;
        }
        // for relative paths
        File jarPath = PathResolver.resolveDirectory(AManagedMonitor.class);
        String configFileName = "";
        if (!Strings.isNullOrEmpty(filename)) {
            configFileName = jarPath + File.separator + filename;
        }
        return configFileName;
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