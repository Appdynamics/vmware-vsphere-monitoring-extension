# AppDynamics VMWare - Monitoring Extension

This extension works only with the standalone machine agent.

## Use Case

VMware vSphere ([www.vmware.com](http://www.vmware.com/products/datacenter-virtualization/vsphere/overview.html)) is a cloud computing virtualization operating system. The VMWare extension gets statistics from the VSphere server and displays them in the AppDynamics Metric Browser.

## Prerequisites
1. Before the extension is installed, the prerequisites mentioned [here](https://community.appdynamics.com/t5/Knowledge-Base/Extensions-Prerequisites-Guide/ta-p/35213) need to be met. Please do not proceed with the extension installation if the specified prerequisites are not met. 
2. The Extension also needs a Vsphere server to be installed.
3. The extension needs to be able to connect to VMWare Vsphere server in order to collect and send metrics. To do this, you will have to either establish a remote connection in between the extension and the product, or have an agent on the same machine running the product in order for the extension to collect and send the metrics.


## Installation

1. Run 'mvn clean install' from the vmware-vsphere-monitoring-extension directory
2. Deploy the file VMWareMonitor.zip found in the 'target' directory into \<machineagent install dir\>/monitors/
3. Unzip the deployed file
4. Open \<machineagent install dir\>/monitors/VMWareMonitor/config.yml and update the host (Host of VSphere), username and password (VSphere credentials). Note: The host can be specified with or without a specific port. For instance, if no port is specified, port 80 will be used. On the other hand, if there is specific port then it needs to be appended to the host in the config.yml
5. Also in hostConfig, the host and vms arguments needs to be configured. There are two ways to specify the value for this argument. If * is specified as the value then all the VMs/Hosts associated with the host will be fetched. If a comma separated list of values is provided, then only those VMs/Hosts wil be fetched. (see config.yml for examples)
6. In metrics.xml you can comment unwanted metrics to reduce the number of metrics reported to controller
7. Restart the machineagent

Please place the extension in the "monitors" directory of your Machine Agent installation directory. Do not place the extension in the "extensions" directory of your Machine Agent installation directory.

## Configuartion

### config.yml

Please copy all the contents of the config.yml file and go [here](https://jsonformatter.org/yaml-validator). On reaching the website, paste the contents and press the “Validate YAML” button.

If you get a valid output, that means your formatting is correct and you may move on to the next step.

#### Metric prefix
Please follow section 2.1 of the [Document](https://community.appdynamics.com/t5/Knowledge-Base/How-do-I-troubleshoot-missing-custom-metrics-or-extensions/ta-p/28695) to set up metric prefix.
```
# Use this only if SIM is enabled. Please make sure to have a trailing |
#metricPrefix: "Custom Metrics|vmware|Status|"

#This will create it in specific Tier aka Component. Replace <COMPONENT_ID>. Please make sure to have a trailing |.
metricPrefix: "Server|Component:<COMPONENT_ID>|Custom Metrics|vmware|Status|"
```

#### Servers configuration
```
# By default the port is 80/443 ( http/https ) for the host. If there is a specific port that is being used then append it to the host
# Case 1, default port  : default-value="hostname"
# Case 2, specific port : default-value="hostname:1234"
servers:
  # displayName is optional if you are configuring only 1 server. If you are configuring multiple servers, configuring displayName is mandatory.
  # When configured displayName is added to the metric path
  - displayName: ""
    host: ""

    username: ""

    #Provide password or encryptedPassword and encryptionKey. See the documentation to find about password encryption.
    password:

    encryptedPassword: ""
    encryptionKey: ""

      #Provide information about hosts and vms to monitor.
      # "host" will take host name you want to monitor or "*" to monitor all hosts
      # "vms" will take vm names in the host specified or "*" to monitor all vms in that host
      # "*" will fetch all the available hosts/vms.
    hostConfig:
      - host: "host1"
        vms: ["vm1","vm2"]
      - host: "host2"
        vms: ["*"]
```
- displayName: Display name for your server. It will be included in metric path. If single server is configured then it is not mandatory. For multiple server configuration, it is mandatory
- username: Username for your vsphere server
- password: Password for your vsphere server
- encryptedPassword: To configure encrypted password (refer Credentials Encryption section for more details)
- hostConfig: Configurations for hosts and vms to monitor 

#### Metric path replacements
Please refer to this [document](https://community.appdynamics.com/t5/Knowledge-Base/Metric-Path-CharSequence-Replacements-in-Extensions/ta-p/35412) to get details on Metric path replacements
```
#Replaces characters in metric name with the specified characters. By default extension takes care of replacing "|",":",",".
#Specify any other char you want to replace here.
# "replace" takes any regular expression
# "replaceWith" takes the string to replace the matched characters
#metricPathReplacements:
#    - replace: ","
#      replaceWith: " "
```
#### Number of threads
```
#Configure this based on the number of hosts and vms you want to monitor. You will get "Queue Capacity reached!! Rejecting runnable tasks.. " error if the numberOfThreads is far less than the
# hosts and vms from which the extension has to collect metrics. You will have to increase numberOfThreads in this case.
numberOfThreads: 15
```
#### Task Schedule
Please refer this [document](https://community.appdynamics.com/t5/Knowledge-Base/Task-Schedule-for-Extensions/ta-p/35414) to get  details on Task schedule feature.
```
taskSchedule:
  numberOfThreads: 1
  taskDelaySeconds: 60
```

## Metrics

### VM Metrics

| Metric | Description |
| --- | --- |
| Ballooned Memory | The size of the balloon driver in the VM, in MB. The host will inflate the balloon driver to reclaim physical memory from the VM. This is a sign that there is memory pressure on the host. |
| Compressed Memory | The amount of compressed memory currently consumed by VM, in Kb. |
| Overhead Memory Consumed | The amount of consumed overhead memory, in MB, for this VM. |
| Distributed CPU Entitlement | This is the amount of CPU resource, in MHz, that this VM is entitled to, as calculated by DRS. Valid only for a VM managed by DRS.  |
| Distributed Memory Entitlement | This is the amount of memory, in MB, that this VM is entitled to, as calculated by DRS. Valid only for a VM managed by DRS. |
| Guest Memory Usage | Guest memory utilization statistics, in MB. This is also known as active guest memory. The number can be between 0 and the configured memory size of the virtual machine. Valid while the virtual machine is running. |
| Host Memory Usage | Host memory utilization statistics, in MB. This is also known as consumed host memory. This is between 0 and the configured resource limit. Valid while the virtual machine is running. This includes the overhead memory of the VM. |
| Overall CPU Usage | Basic CPU Usage statistics, in MHz. Valid while the virtual machine is running.  |
| Overall CPU Demand | Basic CPU Demand statistics, in MHz. Valid while the virtual machine is running.  |
| Private Memory | The portion of memory, in MB, that is granted to this VM from non-shared host memory. |
| Shared Memory | The portion of memory, in MB, that is granted to this VM from host memory that is shared between VMs.  |
| Static CPU Entitlement | The static CPU resource entitlement for a virtual machine. This value is calculated based on this virtual machine's resource reservations, shares and limit, and doesn't take into account current usage. This is the worst case CPU allocation for this virtual machine, that is, the amount of CPU resource this virtual machine would receive if all virtual machines running in the cluster went to maximum consumption. Units are MHz. |
| Static Memory Entitlement | The static memory resource entitlement for a virtual machine. This value is calculated based on this virtual machine's resource reservations, shares and limit, and doesn't take into account current usage. This is the worst case memory allocation for this virtual machine, that is, the amount of memory this virtual machine would receive if all virtual machines running in the cluster went to maximum consumption. Units are MB. |
| Swapped Memory | The portion of memory, in MB, that is granted to this VM from the host's swap space. This is a sign that there is memory pressure on the host.  |
| Uptime | The system uptime of the VM in seconds. |
| Memory MB | Memory in MB |
| Num CPU | Number of CPU Cores |
| Status | Shows the current status colour code of the VM. 0=gray, 1=green, 2=yellow, 3=red |

### Host Metrics
| Metric | Description |
| --- | --- |
| Distributed CPU Fairness | The fairness of distributed CPU resource allocation on the host |
| Distributed Memory Fairness | The fairness of distributed memory resource allocation on the host |
| Overall CPU Usage | Aggregated CPU usage across all cores on the host in MHz. This is only available if the host is connected |
| Overall Memory Usage | Physical memory usage on the host in MB. This is only available if the host is connected |
| Up Time | The system uptime of the host in seconds.  |
| Memory Size | Memory size of the host machine  |
| CPU Cores | CPU cores of this host machine  |
| Status | Shows the current status colour code of the Host. 0=gray, 1=green, 2=yellow, 3=red |


This monitor can potentially register hundred of new metrics, depending on how 
many hosta and vms you are configuring. By default, the Machine Agent will only report 450 
metrics to the controller, so you may need to increase that limit when 
installing this monitor. To increase the metric limit, you must add a parameter 
when starting the Machine Agent, like this:

    java -Dappdynamics.agent.maxMetrics=1000 -jar machineagent.jar

## Credentials Encryption
Please visit [this page](https://community.appdynamics.com/t5/Knowledge-Base/How-do-I-use-Password-Encryption-with-Extensions/ta-p/29397) to get detailed instructions on password encryption. The steps in this document will guide you through the whole process.

## Workbench

Workbench is an inbuilt feature provided with each extension in order to assist you to fine tune the extension setup before you actually deploy it on the controller. Please review the following document on [How to use the Extensions WorkBench](https://community.appdynamics.com/t5/Knowledge-Base/How-do-I-use-the-Extensions-WorkBench/ta-p/30130).


* If the server is not accessible from outside/browser, you can use the following end points to see the list of registered metrics and errors.
~~~
    #Get the stats
    curl http://localhost:9090/api/stats
    #Get the registered metrics
    curl http://localhost:9090/api/metric-paths
~~~
* You can make the changes to config.yml and validate it from the browser or the API
* Once the configuration is complete, you can kill the workbench and start the Machine Agent

## Troubleshooting
Please follow the steps listed in this [troubleshooting-document](https://community.appdynamics.com/t5/Knowledge-Base/How-do-I-troubleshoot-missing-custom-metrics-or-extensions/ta-p/28695) in order to troubleshoot your issue. These are a set of common issues that customers might have faced during the installation of the extension.

## Contributing

Always feel free to fork and contribute any changes directly here on [GitHub](https://github.com/Appdynamics/vmware-vsphere-monitoring-extension).

## Version
|          Name            |  Version   |
|--------------------------|------------|
|Extension Version         |3.0.2       |
|Last Update               |10/02/2021  |
|Changes list              |[ChangeLog](https://github.com/Appdynamics/vmware-vsphere-monitoring-extension/blob/master/CHANGES.md)|

**Note**: While extensions are maintained and supported by customers under the open-source licensing model, they interact with agents and Controllers that are subject to [AppDynamics’ maintenance and support policy](https://docs.appdynamics.com/latest/en/product-and-release-announcements/maintenance-support-for-software-versions). Some extensions have been tested with AppDynamics 4.5.13+ artifacts, but you are strongly recommended against using versions that are no longer supported.
