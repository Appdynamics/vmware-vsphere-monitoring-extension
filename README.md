# AppDynamics VMWare Monitoring Extension

- [Use Case](vmware-readme.md#use-case)
- [Installation](vmware-readme.md#installation)
    - [Example XML](vmware-readme.md#example-xml)
    - [Rebuilding the Project](vmware-readme.md#rebuilding-the-project)
- [Metrics](vmware-readme.md#metrics)
- [Files and Folders](vmware-readme.md#files-and-folders)
- [Contributing](vmware-readme.md#contributing)

##Use Case

VMware vSphere ([www.vmware.com](http://www.vmware.com/products/datacenter-virtualization/vsphere/overview.html))
is a cloud computing virtualization operating system.
The VMWare extension gets statistics from the VSphere server and displays them in the AppDynamics Metric Browser.

Metrics include:

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

##Installation

1. In \<machine-agent-home\>/monitors create a new subdirectory for the extension.
2. Copy the contents in the 'dist' folder to the folder made in step 1.
3. Open monitor.xml and update the host (Host of VSphere), username and password (VSphere credentials).
4. Restart the Machine A1gent.
5. Metrics will be uploaded to: Application Infrastructure Performance|\<Node\>|Custom Metrics|VMWare|Status.

### Example XML

    <monitor>
	    <name>VMWareMonitor</name>
	    <type>managed</type>
	    <description>VMWare Monitor</description>
	    <monitor-configuration></monitor-configuration>
	    <monitor-run-task>
		    <execution-style>periodic</execution-style>
		    <execution-frequency-in-seconds>60</execution-frequency-in-seconds>
		    <name>VMWare Monitor Run Task</name>
		    <display-name>VMWare Monitor Task</display-name>
		    <description>VMWare Monitor Task</description>
		    <type>java</type>
		    <execution-timeout-in-secs>60</execution-timeout-in-secs>
		    <task-arguments>
			    <argument name="host" is-required="true" default-value="vcenter.url.com" />
			    <argument name="username" is-required="true" default-value="user1" />
			    <argument name="password" is-required="true" default-value="pass1" />
		    </task-arguments>
		    <java-task>
			<classpath>vmware.jar;vijava5120121125.jar;dom4j-1.6.1.jar</classpath>
			    <impl-class>com.appdynamics.monitors.vmware.VMWareMonitor</impl-class>
		    </java-task>
	    </monitor-run-task>
    </monitor>

###Rebuilding the Project

1. Go to root directory (where all the files are located) through command line
2. Type "ant" (without the quotes)
3. 'dist' will be updated with the monitor.xml, vmware.jar, and other libraries

##Files and Folders

| Files/Folder | Description |
| --- | --- |
| bin | Contains class files |
| conf | Contains the monitor.xml |
| lib | Contains third-party project references |
| src | Contains source code to VMWare Custom Monitor |
| dist | Contains the final distribution package (monitor.xml, vmware.jar and other libraries) |
| build.xml | Ant build script to package the project (only required if changing java code) |
| Main Java File | src/com/appdynamics/monitors/vmware/VMWareMonitor.java

##Metrics

Metric | Description |
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


##Contributing

Always feel free to fork and contribute any changes directly via GitHub.


##Support

For any support questions, please contact ace@appdynamics.com.
