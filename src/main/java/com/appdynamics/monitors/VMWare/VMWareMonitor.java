package com.appdynamics.monitors.VMWare;

import com.appdynamics.TaskInputArgs;
import com.appdynamics.extensions.PathResolver;
import com.appdynamics.extensions.conf.MonitorConfiguration;
import com.appdynamics.extensions.crypto.CryptoUtil;
import com.appdynamics.extensions.util.MetricWriteHelper;
import com.appdynamics.extensions.util.MetricWriteHelperFactory;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.TaskExecutionContext;
import com.singularity.ee.agent.systemagent.api.TaskOutput;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import com.vmware.vim25.InvalidProperty;
import com.vmware.vim25.RuntimeFault;
import com.vmware.vim25.mo.Folder;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ManagedEntity;
import com.vmware.vim25.mo.ServiceInstance;
import org.apache.log4j.Logger;

import java.io.File;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class VMWareMonitor extends AManagedMonitor {
    /**
     * The metric can be found in Application Infrastructure Performance|{@literal <}Node{@literal >}|Custom Metrics|vmware|Status
     */
    private static final String METRIC_PREFIX = "Custom Metrics|vmware|Status|";

    private static final Logger logger = Logger.getLogger(VMWareMonitor.class);

    private static final String CONFIG_ARG = "config-file";
    private static final String FILE_NAME = "monitors/VMWareMonitor/config.yml";

    private boolean initialized;
    private MonitorConfiguration configuration;
    private Folder rootFolder;

    public VMWareMonitor() {
        printVersion();
    }

    private void printVersion() {
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
        printVersion();
        logger.info("Starting the VMWare Monitoring task.");

        try {
            String configFilename = getConfigFilename(taskArguments.get(CONFIG_ARG));
            if (!initialized) {
                initialize(configFilename);
            }
            configuration.executeTask();

            logger.info("Finished execution");
            return new TaskOutput("Finished execution");
        } catch (Exception e) {
            logger.error("Failed to execute the VMWare monitoring task", e);
            throw new TaskExecutionException("Failed to execute the VMWare monitoring task" + e);
        }
    }

    private void initialize(String configFile) {
        if (!initialized) {

            MetricWriteHelper metricWriteHelper = MetricWriteHelperFactory.create(this);
            MonitorConfiguration conf = new MonitorConfiguration(METRIC_PREFIX, new TaskRunnable(), metricWriteHelper);

            conf.setConfigYml(configFile, new MonitorConfiguration.FileWatchListener() {
                public void onFileChange(File file) {
                    connect();
                }
            });

            conf.checkIfInitialized(MonitorConfiguration.ConfItem.CONFIG_YML, MonitorConfiguration.ConfItem.METRIC_PREFIX, MonitorConfiguration.ConfItem.METRIC_WRITE_HELPER);
            this.configuration = conf;

            connect();

            initialized = true;
        }
    }

    private void connect() {

        if (configuration != null && configuration.getConfigYml() != null) {
            Map<String, ?> config = configuration.getConfigYml();
            String host = (String) config.get("host");
            String username = (String) config.get("username");
            String password = getPassword(config);

            String url = "https://" + host + "/sdk";
            try {
                ServiceInstance serviceInstance = new ServiceInstance(new URL(url), username, password, true);
                rootFolder = serviceInstance.getRootFolder();
            } catch (Exception e) {
                logger.error("Unable to connect to the host [" + host + "]", e);
                throw new RuntimeException("Unable to connect to the host [" + host + "]", e);
            }
            logger.info("Connection to: " + url + " Successful");
        }
    }

    private class TaskRunnable implements Runnable {

        public void run() {

            Map<String, ?> config = configuration.getConfigYml();

            List<Map<String, Object>> hostConfigs = (List<Map<String, Object>>) config.get("hostConfig");
            if (hostConfigs != null && !hostConfigs.isEmpty()) {

                List<Map<String, String>> metricCharacterReplacers = (List<Map<String, String>>) config.get("metricCharacterReplacer");

                Map<Pattern, String> replacers = new HashMap<Pattern, String>();

                if (metricCharacterReplacers != null) {
                    for (Map<String, String> metricCharacterReplacer : metricCharacterReplacers) {
                        String replace = metricCharacterReplacer.get("replace");
                        String replaceWith = metricCharacterReplacer.get("replaceWith");

                        Pattern pattern = Pattern.compile(replace);

                        replacers.put(pattern, replaceWith);
                    }
                }

                Integer hostThreads = (Integer) config.get("hostThreads");

                if (hostThreads == null) {
                    hostThreads = 1;
                }

                Integer vmThreads = (Integer) config.get("vmThreads");

                if (vmThreads == null) {
                    vmThreads = 1;
                }

                populateMetrics(rootFolder, hostConfigs, replacers, hostThreads, vmThreads);
            } else {
                logger.info("hostConfig not specified in configuration. Exiting the process");
            }
        }
    }

    private void populateMetrics(Folder rootFolder, List<Map<String, Object>> hostConfigs, Map<Pattern, String> replacers, Integer hostThreads, Integer vmThreads) {

        List<ManagedEntity> hostEntities = getHostMachines(rootFolder, hostConfigs);

        if (logger.isDebugEnabled()) {
            logger.debug("Found " + hostEntities.size() + " hosts");
            StringBuilder sb = new StringBuilder();
            for (ManagedEntity managedEntity : hostEntities) {
                if (sb.length() > 0) {
                    sb.append(",");
                }
                sb.append(managedEntity.getName());
            }
            logger.debug("Host machines [ " + sb.toString() + " ]");
        }

        HostMetricCollector hostMetricCollector = new HostMetricCollector(configuration.getMetricWriter(), configuration.getMetricPrefix(), hostEntities, hostConfigs, replacers, hostThreads, vmThreads);
        hostMetricCollector.execute();
    }

    private List<ManagedEntity> getHostMachines(Folder rootFolder, List<Map<String, Object>> hostConfigs) {
        List<ManagedEntity> hostEntities = new ArrayList<ManagedEntity>();

        for (Map<String, Object> hostConfig : hostConfigs) {
            String hostName = (String) hostConfig.get("host");
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

    private String getPassword(Map<String, ?> config) {
        String password = null;

        if (!Strings.isNullOrEmpty((String) config.get("password"))) {
            password = (String) config.get("password");

        } else {
            try {
                Map<String, String> args = Maps.newHashMap();
                args.put(TaskInputArgs.PASSWORD_ENCRYPTED, (String) config.get("encryptedPassword"));
                args.put(TaskInputArgs.ENCRYPTION_KEY, (String) config.get("encryptionKey"));
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
}