/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.monitors.VMWare.metrics;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "VMWareMetrics")
@XmlAccessorType(XmlAccessType.FIELD)
public class VMWareMetrics {

    @XmlElement(name = "HostMetrics")
    private HostMetrics hostMetrics;

    @XmlElement(name = "VMMetrics")
    private VMMetrics vmMetrics;

    public HostMetrics getHostMetrics() {
        return hostMetrics;
    }

    public void setHostMetrics(HostMetrics hostMetrics) {
        this.hostMetrics = hostMetrics;
    }

    public VMMetrics getVmMetrics() {
        return vmMetrics;
    }

    public void setVmMetrics(VMMetrics vmMetrics) {
        this.vmMetrics = vmMetrics;
    }
}
