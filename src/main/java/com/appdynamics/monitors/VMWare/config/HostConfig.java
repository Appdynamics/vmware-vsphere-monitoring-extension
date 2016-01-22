package com.appdynamics.monitors.VMWare.config;


import java.util.List;

public class HostConfig {

    private String host;
    private List<String> vms;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public List<String> getVms() {
        return vms;
    }

    public void setVms(List<String> vms) {
        this.vms = vms;
    }
}
