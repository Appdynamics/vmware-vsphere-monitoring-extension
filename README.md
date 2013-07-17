# AppDynamics VMWare Monitoring Extension


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


##Included Files and Folders

| Files/Folder | Description |
| --- | --- |
| conf | Contains the monitor.xml |
| lib | Contains third-party project references |
| src | Contains source code to the VMWare Monitoring Extension |
| dist | Only obtained when using ant. Run 'ant build' to get binaries. Run 'ant package' to get the distributable .zip file |
| build.xml | Ant build script to package the project (only required if changing java code) |
| Main Java File | src/main/java/com/appdynamics/monitors/vmware/VMWareMonitor.java


##Installation

1. Run 'ant package' from the vmware-vsphere-monitoring-extension directory
2. Deploy the file VMWareMonitor.zip found in the 'dist' directory into \<machineagent install dir\>/monitors/
3. Unzip the deployed file
4. Open \<machineagent install dir\>/monitors/VMWareMonitor/monitor.xml and update the host (Host of VSphere), username and password (VSphere credentials).
5. Restart the machineagent
6. In the AppDynamics Metric Browser, look for: Application Infrastructure Performance  | \<Tier\> | Custom Metrics | VMWare | Status


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
