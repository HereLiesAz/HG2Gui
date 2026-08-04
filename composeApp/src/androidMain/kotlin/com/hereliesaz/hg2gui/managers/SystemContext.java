package com.hereliesaz.hg2gui.managers;

public class SystemContext {
    private static SystemContext instance;

    public enum OSType {
        UBUNTU, MACOS, WINDOWS
    }

    private OSType os = OSType.UBUNTU;
    private String user = "root";
    private String hostname = "hitchhiker-guide";

    private SystemContext() {}

    public static synchronized SystemContext getInstance() {
        if (instance == null) {
            instance = new SystemContext();
        }
        return instance;
    }

    public OSType getOs() {
        return os;
    }

    public void setOs(OSType os) {
        this.os = os;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }
}
