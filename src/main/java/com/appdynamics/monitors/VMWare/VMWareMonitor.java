package com.appdynamics.monitors.vmware;

import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.vmware.vim25.mo.*;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.MetricWriter;
import com.singularity.ee.agent.systemagent.api.TaskExecutionContext;
import com.singularity.ee.agent.systemagent.api.TaskOutput;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import com.vmware.vim25.VirtualMachineQuickStats;

public class VMWareMonitor extends AManagedMonitor
{
	/**
	 * The metric can be found in Application Infrastructure Performance|{@literal <}Node{@literal >}|Custom Metrics|vmware|Status
	 */
	public static String metricPrefix = "Custom Metrics|vmware|Status|VM Name|";

	private ServiceInstance serviceInstance;
	protected volatile String host;
	protected volatile List<String> vmnames;
	protected volatile String username;
	protected volatile String password;
    private ServerConnection serverConnection = null;

	protected final Logger logger = Logger.getLogger(this.getClass().getName());

    public VMWareMonitor() {
        logger.setLevel(Level.INFO);
    }

    /**
	 * Fetches statistics from VSphere and uploads them to the controller
	 * @throws Exception
	 */
	public void populate() throws Exception
	{
        Folder root = serviceInstance.getRootFolder();
        List<ManagedEntity> managedEntities = new ArrayList<ManagedEntity>();
        // Get all VMs
        if (vmnames.size() == 1 && vmnames.get(0).equals("*")) {
            managedEntities = Arrays.asList(new InventoryNavigator(root).searchManagedEntities("VirtualMachine"));
        }
        // Get specific VMs
        else {
            for (String vmname : vmnames) {
                managedEntities.add(new InventoryNavigator(root).searchManagedEntity("VirtualMachine", vmname));
            }
        }
        for (ManagedEntity managedEntity: managedEntities) {
            if (managedEntity == null || !(managedEntity instanceof VirtualMachine))
            {
                throw new Exception("Could not find VM");
            }
            VirtualMachineQuickStats vmStats = ((VirtualMachine)managedEntity).getSummary().getQuickStats();

            printMetric(managedEntity.getName() + "|Ballooned Memory", vmStats.balloonedMemory,
                    MetricWriter.METRIC_AGGREGATION_TYPE_AVERAGE,
                    MetricWriter.METRIC_TIME_ROLLUP_TYPE_AVERAGE,
                    MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE
            );
            printMetric(managedEntity.getName() + "|Compressed Memory", vmStats.compressedMemory,
                    MetricWriter.METRIC_AGGREGATION_TYPE_AVERAGE,
                    MetricWriter.METRIC_TIME_ROLLUP_TYPE_AVERAGE,
                    MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE
            );
            printMetric(managedEntity.getName() + "|Overhead Memory Consumed", vmStats.consumedOverheadMemory,
                    MetricWriter.METRIC_AGGREGATION_TYPE_AVERAGE,
                    MetricWriter.METRIC_TIME_ROLLUP_TYPE_AVERAGE,
                    MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE
            );
            printMetric(managedEntity.getName() + "|Distributed CPU Entitlement", vmStats.distributedCpuEntitlement,
                    MetricWriter.METRIC_AGGREGATION_TYPE_AVERAGE,
                    MetricWriter.METRIC_TIME_ROLLUP_TYPE_AVERAGE,
                    MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE
            );
            printMetric(managedEntity.getName() + "|Distributed Memory Entitlement", vmStats.distributedMemoryEntitlement,
                    MetricWriter.METRIC_AGGREGATION_TYPE_AVERAGE,
                    MetricWriter.METRIC_TIME_ROLLUP_TYPE_AVERAGE,
                    MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE
            );
            printMetric(managedEntity.getName() + "|Guest Memory Usage", vmStats.guestMemoryUsage,
                    MetricWriter.METRIC_AGGREGATION_TYPE_AVERAGE,
                    MetricWriter.METRIC_TIME_ROLLUP_TYPE_AVERAGE,
                    MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE
            );
            printMetric(managedEntity.getName() + "|Host Memory Usage", vmStats.hostMemoryUsage,
                    MetricWriter.METRIC_AGGREGATION_TYPE_AVERAGE,
                    MetricWriter.METRIC_TIME_ROLLUP_TYPE_AVERAGE,
                    MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE
            );
            printMetric(managedEntity.getName() + "|Overall CPU Usage", vmStats.overallCpuUsage,
                    MetricWriter.METRIC_AGGREGATION_TYPE_AVERAGE,
                    MetricWriter.METRIC_TIME_ROLLUP_TYPE_AVERAGE,
                    MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE
            );
            printMetric(managedEntity.getName() + "|Overall CPU Demand", vmStats.overallCpuDemand,
                    MetricWriter.METRIC_AGGREGATION_TYPE_AVERAGE,
                    MetricWriter.METRIC_TIME_ROLLUP_TYPE_AVERAGE,
                    MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE
            );
            printMetric(managedEntity.getName() + "|Private Memory", vmStats.privateMemory,
                    MetricWriter.METRIC_AGGREGATION_TYPE_AVERAGE,
                    MetricWriter.METRIC_TIME_ROLLUP_TYPE_AVERAGE,
                    MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE
            );
            printMetric(managedEntity.getName() + "|Shared Memory", vmStats.sharedMemory,
                    MetricWriter.METRIC_AGGREGATION_TYPE_AVERAGE,
                    MetricWriter.METRIC_TIME_ROLLUP_TYPE_AVERAGE,
                    MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE
            );
            printMetric(managedEntity.getName() + "|Static CPU Entitlement", vmStats.staticCpuEntitlement,
                    MetricWriter.METRIC_AGGREGATION_TYPE_AVERAGE,
                    MetricWriter.METRIC_TIME_ROLLUP_TYPE_AVERAGE,
                    MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE
            );
            printMetric(managedEntity.getName() + "|Static Memory Entitlement", vmStats.staticMemoryEntitlement,
                    MetricWriter.METRIC_AGGREGATION_TYPE_AVERAGE,
                    MetricWriter.METRIC_TIME_ROLLUP_TYPE_AVERAGE,
                    MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE
            );
            printMetric(managedEntity.getName() + "|Swapped Memory", vmStats.swappedMemory,
                    MetricWriter.METRIC_AGGREGATION_TYPE_AVERAGE,
                    MetricWriter.METRIC_TIME_ROLLUP_TYPE_AVERAGE,
                    MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE
            );
            printMetric(managedEntity.getName() + "|Up Time", vmStats.uptimeSeconds,
                    MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION,
                    MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT,
                    MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_INDIVIDUAL
            );
        }
        logger.info("Printed metrics successfully...");
	}

	/**
	 * Creates an active connection to VSphere Manager
	 * @throws MalformedURLException
	 * @throws RemoteException 
	 */
	public void connect() throws RemoteException, MalformedURLException
	{
		String url = "https://" + host + "/sdk";
            try {
                serviceInstance = new ServiceInstance(new URL(url), username, password, true);
                serverConnection = serviceInstance.getServerConnection();
            }
            catch (Exception e) {
                logger.error("Exception", e);
            }
		logger.info("Connection to: " + url + " Successful");
	}

	/**
	 * Closes the active connection to VSphere
	 */
	private void close()
	{
		if (serviceInstance == null)
		{
			return;
		}
		serverConnection.logout();
		logger.info("Connection closed");
	}

	/**
	 * Main execution of the Monitor
	 * @see com.singularity.ee.agent.systemagent.api.ITask#execute(java.util.Map, com.singularity.ee.agent.systemagent.api.TaskExecutionContext)
	 */
	public TaskOutput execute(Map<String, String> arg0, TaskExecutionContext arg1) throws TaskExecutionException
	{
        try
		{
			host = arg0.get("host");
            username = arg0.get("username");
            password = arg0.get("password");

            String[] splittingNames = arg0.get("vmnames").split(",");
            for (int i = 0; i < splittingNames.length; i++) {
                splittingNames[i] = splittingNames[i].replaceAll("\\s","");
            }
            vmnames = Arrays.asList(splittingNames);

            connect();
            populate();
			close();
		}
		catch (Exception e)
		{
			logger.error("Exception: ", e);
            return new TaskOutput("Failed with error: " + e);
		}
        logger.info("Finished execution");
		return new TaskOutput("Finished execution");
	}

	/**
	 * Returns the metric to the AppDynamics Controller.
	 * @param 	metricName		Name of the Metric
	 * @param 	metricValue		Value of the Metric
	 * @param 	aggregation		Average OR Observation OR Sum
	 * @param 	timeRollup		Average OR Current OR Sum
	 * @param 	cluster			Collective OR Individual
	 */
	public void printMetric(String metricName, Object metricValue, String aggregation, String timeRollup, String cluster)
	{
		MetricWriter metricWriter = getMetricWriter(getMetricPrefix() + metricName, 
			aggregation,
			timeRollup,
			cluster
		);

		metricWriter.printMetric(String.valueOf(metricValue));
	}

	/**
	 * @return Returns the metric path
	 */
	public String getMetricPrefix()
	{
		return VMWareMonitor.metricPrefix;
	}
}