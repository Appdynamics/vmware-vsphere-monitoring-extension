package com.appdynamics.monitors.VMWare.config;

import java.util.List;

public class Configuration {

    private String host;
    private String username;
    private String password;

    private String encryptedPassword;
    private String encryptionKey;
    private List<HostConfig> hostConfig;
    private List<MetricCharacterReplacer> metricCharacterReplacer;
    private String metricPrefix;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEncryptedPassword() {
        return encryptedPassword;
    }

    public void setEncryptedPassword(String encryptedPassword) {
        this.encryptedPassword = encryptedPassword;
    }

    public String getEncryptionKey() {
        return encryptionKey;
    }

    public void setEncryptionKey(String encryptionKey) {
        this.encryptionKey = encryptionKey;
    }

    public List<HostConfig> getHostConfig() {
        return hostConfig;
    }

    public void setHostConfig(List<HostConfig> hostConfig) {
        this.hostConfig = hostConfig;
    }

    public List<MetricCharacterReplacer> getMetricCharacterReplacer() {
        return metricCharacterReplacer;
    }

    public void setMetricCharacterReplacer(List<MetricCharacterReplacer> metricCharacterReplacer) {
        this.metricCharacterReplacer = metricCharacterReplacer;
    }

    public String getMetricPrefix() {
        return metricPrefix;
    }

    public void setMetricPrefix(String metricPrefix) {
        this.metricPrefix = metricPrefix;
    }
}
