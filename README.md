# AppDynamics VMWare - Monitoring Extension

This extension works only with the standalone machine agent.

##Use Case

VMware vSphere ([www.vmware.com](http://www.vmware.com/products/datacenter-virtualization/vsphere/overview.html)) is a cloud computing virtualization operating system. The VMWare extension gets statistics from the VSphere server and displays them in the AppDynamics Metric Browser.

Metrics include:

####VM Metrics
* Ballooned Memory
* Compressed Memory
* Overhead Memory Consumed
* Distributed CPU Entitlement
* Distributed Memory Entitlement
* Guest and Host Memory Usage
* Overall CPU Demand and Usage
* Private and Shared Memory
* Static CPU Entitlement
* Static Memory Entitlement
* Swapped Memory
* Uptime
* Memory MB
* Num CPU

####Host Metrics
* Distributed CPU Fairness
* Distributed Memory Fairness
* Overall CPU Usage
* Overall Memory Usage
* Up Time
* Memory Size
* CPU Cores

##Installation

1. Run 'mvn clean install' from the vmware-vsphere-monitoring-extension directory
2. Deploy the file VMWareMonitor.zip found in the 'dist' directory into \<machineagent install dir\>/monitors/
3. Unzip the deployed file
4. Open \<machineagent install dir\>/monitors/VMWareMonitor/config.yml and update the host (Host of VSphere), username and password (VSphere credentials). Note: The host can be specified with or without a specific port. For instance, if no port is specified, port 80 will be used (i.e. argument name="host" is-required="true" default-value="hostname:" ). On the other hand, if there is specific port then it needs to be appended to the host in the monitor.xml (i.e.argument name="host" is-required="true" default-value="hostname:port")
5. Also in hostConfig, the host and vms arguments needs to be configured. There are two ways to specify the value for this argument. If * is specified as the value then all the VMs/Hosts associated with the host will be fetched. If a comma separated list of values is provided, then only those VMs/Hosts wil be fetched. (see config.yml for examples)
6. Restart the machineagent
7. In the AppDynamics Metric Browser, look for: Application Infrastructure Performance  | \<Tier\> | Custom Metrics | VMWare | Status


##Directory Structure

| File/Folder | Description |
| --- | --- |
| src/main/resources/config | Contains the monitor.xml and config.yml |
| src/main/java | Contains source code to the VMWare Monitoring Extension |
| target | Only obtained when using maven. Run 'maven clean install' to get the distributable .zip file |
| pom.xml | Maven build script to package the project (only required if changing java code) |
| Main Java File | src/main/java/com/appdynamics/monitors/VMWare/VMWareMonitor.java

##Example config.yml

```
# By default the port is 80/443 ( http/https ) for the host. If there is a specific port that is being used then append it to the host
# Case 1, default port  : default-value="hostname"
# Case 2, specific port : default-value="hostname:1234"
host: ""

#Escape any special characters in username with "\"
username: ""

#Provide password or encryptedPassword and encryptionKey. See the documentation to find about password encryption.
password: ""

encryptedPassword:
encryptionKey:

#Provide information about hosts and vms to monitor.
# "host" will take host name you want to monitor or "*" to monitor all hosts
# "vms" will take vm names in the host specified or "*" to monitor all vms in that host
# "*" will fetch all the available hosts/vms.
hostConfig:
    - host: "host1"
      vms: ["vm1","vm2"]
    - host: "host2"
      vms: ["*"]

#Replaces characters in metric name with the specified characters.
# "replace" takes any regular expression
# "replaceWith" takes the string to replace the matched characters
metricCharacterReplacer:
    - replace: ","
      replaceWith: " "

metricPrefix: "Custom Metrics|vmware|Status|"

```

##Metrics

###VM Metrics

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

###Host Metrics
| Metric | Description |
| --- | --- |
| Distributed CPU Fairness | The fairness of distributed CPU resource allocation on the host |
| Distributed Memory Fairness | The fairness of distributed memory resource allocation on the host |
| Overall CPU Usage | Aggregated CPU usage across all cores on the host in MHz. This is only available if the host is connected |
| Overall Memory Usage | Physical memory usage on the host in MB. This is only available if the host is connected |
| Up Time | The system uptime of the host in seconds.  |
| Memory Size | Memory size of the host machine  |
| CPU Cores | CPU core sof this host machine  |


###Password Encryption
To set encryptedPassword in config.yaml, follow the steps below:

1. Download the util jar to encrypt the AWS Credentials from [here](https://github.com/Appdynamics/maven-repo/blob/master/releases/com/appdynamics/appd-exts-commons/1.1.2/appd-exts-commons-1.1.2.jar).
2. Run command:

   	~~~   
   	java -cp appd-exts-commons-1.1.2.jar com.appdynamics.extensions.crypto.Encryptor EncryptionKey PasswordToEncrypt
   	
   	For example: 
   	java -cp "appd-exts-commons-1.1.2.jar" com.appdynamics.extensions.crypto.Encryptor test mypassword
   	~~~
   	
3. Set encryptedPassword and encryptionKey in the config.yml.


##Contributing

Always feel free to fork and contribute any changes directly via GitHub.

##Community

Find out more in the [AppSphere](https://www.appdynamics.com/community/exchange/extension/vmware-vsphere-monitoring-extension/) community.

##Support

For any questions or feature request, please contact [AppDynamics Center of Excellence](mailto:help@appdynamics.com).
