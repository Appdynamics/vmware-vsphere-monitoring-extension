package com.appdynamics.monitors.vmware;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.Map;

import org.apache.log4j.Logger;

import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.MetricWriter;
import com.singularity.ee.agent.systemagent.api.TaskExecutionContext;
import com.singularity.ee.agent.systemagent.api.TaskOutput;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import com.vmware.vim25.VirtualMachineQuickStats;
import com.vmware.vim25.mo.*;

public class VMWareMonitor extends AManagedMonitor
{
	/**
	 * The metric can be found in Application Infrastructure Performance|{@literal <}Node{@literal >}|Custom Metrics|VMWare|Status
	 */
	public static String metricPrefix = "Custom Metrics|VMWare|Status|";

	private ServiceInstance serviceInstance;
	protected volatile String host;
	protected volatile String machineId;
	protected volatile String username;
	protected volatile String password;

	protected final Logger logger = Logger.getLogger(this.getClass().getName());

	/**
	 * Fetches statistics from VSphere and uploads them to the controller
	 * @throws Exception
	 */
	public void populate() throws Exception
	{
		Folder root = serviceInstance.getRootFolder();
		ManagedEntity managedEntity = new InventoryNavigator(root).searchManagedEntity("VirtualMachine", machineId);

		if (managedEntity == null || !(managedEntity instanceof VirtualMachine))
		{
			throw new Exception("Could not find VM");
		}

		VirtualMachineQuickStats vmStats = ((VirtualMachine)managedEntity).getSummary().getQuickStats();

		printMetric("Ballooned Memory", vmStats.balloonedMemory,
			MetricWriter.METRIC_AGGREGATION_TYPE_AVERAGE, 
			MetricWriter.METRIC_TIME_ROLLUP_TYPE_AVERAGE, 
			MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE
		);
		printMetric("Compressed Memory", vmStats.compressedMemory,
			MetricWriter.METRIC_AGGREGATION_TYPE_AVERAGE, 
			MetricWriter.METRIC_TIME_ROLLUP_TYPE_AVERAGE, 
			MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE
		);
		printMetric("Overhead Memory Consumed", vmStats.consumedOverheadMemory,
			MetricWriter.METRIC_AGGREGATION_TYPE_AVERAGE, 
			MetricWriter.METRIC_TIME_ROLLUP_TYPE_AVERAGE, 
			MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE
		);
		printMetric("Distributed CPU Entitlement", vmStats.distributedCpuEntitlement,
			MetricWriter.METRIC_AGGREGATION_TYPE_AVERAGE, 
			MetricWriter.METRIC_TIME_ROLLUP_TYPE_AVERAGE, 
			MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE
		);
		printMetric("Distributed Memory Entitlement", vmStats.distributedMemoryEntitlement,
			MetricWriter.METRIC_AGGREGATION_TYPE_AVERAGE, 
			MetricWriter.METRIC_TIME_ROLLUP_TYPE_AVERAGE, 
			MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE
		);
		printMetric("Guest Memory Usage", vmStats.guestMemoryUsage,
			MetricWriter.METRIC_AGGREGATION_TYPE_AVERAGE, 
			MetricWriter.METRIC_TIME_ROLLUP_TYPE_AVERAGE, 
			MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE
		);
		printMetric("Host Memory Usage", vmStats.hostMemoryUsage,
			MetricWriter.METRIC_AGGREGATION_TYPE_AVERAGE, 
			MetricWriter.METRIC_TIME_ROLLUP_TYPE_AVERAGE, 
			MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE
		);
		printMetric("Overall CPU Usage", vmStats.overallCpuUsage,
			MetricWriter.METRIC_AGGREGATION_TYPE_AVERAGE, 
			MetricWriter.METRIC_TIME_ROLLUP_TYPE_AVERAGE, 
			MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE
		);
		printMetric("Overall CPU Demand", vmStats.overallCpuDemand,
			MetricWriter.METRIC_AGGREGATION_TYPE_AVERAGE, 
			MetricWriter.METRIC_TIME_ROLLUP_TYPE_AVERAGE, 
			MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE
		);
		printMetric("Private Memory", vmStats.privateMemory,
			MetricWriter.METRIC_AGGREGATION_TYPE_AVERAGE, 
			MetricWriter.METRIC_TIME_ROLLUP_TYPE_AVERAGE, 
			MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE
		);
		printMetric("Shared Memory", vmStats.sharedMemory,
			MetricWriter.METRIC_AGGREGATION_TYPE_AVERAGE, 
			MetricWriter.METRIC_TIME_ROLLUP_TYPE_AVERAGE, 
			MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE
		);
		printMetric("Static CPU Entitlement", vmStats.staticCpuEntitlement,
			MetricWriter.METRIC_AGGREGATION_TYPE_AVERAGE, 
			MetricWriter.METRIC_TIME_ROLLUP_TYPE_AVERAGE, 
			MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE
		);
		printMetric("Static Memory Entitlement", vmStats.staticMemoryEntitlement,
			MetricWriter.METRIC_AGGREGATION_TYPE_AVERAGE, 
			MetricWriter.METRIC_TIME_ROLLUP_TYPE_AVERAGE, 
			MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE
		);
		printMetric("Swapped Memory", vmStats.swappedMemory,
			MetricWriter.METRIC_AGGREGATION_TYPE_AVERAGE, 
			MetricWriter.METRIC_TIME_ROLLUP_TYPE_AVERAGE, 
			MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE
		);
		printMetric("Up Time", vmStats.uptimeSeconds, 
			MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION,
			MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT, 
			MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_INDIVIDUAL
		);
	}

	/**
	 * Creates an active connection to VSphere Manager
	 * @throws MalformedURLException
	 * @throws RemoteException 
	 */
	public void connect() throws RemoteException, MalformedURLException
	{
		String url = "https://" + host + "/sdk";

		if (serviceInstance == null)
		{
			serviceInstance = new ServiceInstance(new URL(url), username, password, true);
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
		serviceInstance.getServerConnection().logout();

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
			machineId = InetAddress.getLocalHost().getHostName();
			username = arg0.get("username");
			password = arg0.get("password");

			connect();
			populate();
			close();
		}
		catch (Exception e)
		{
			return new TaskOutput("Failed with error: " + e);
		}

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