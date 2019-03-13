package de.opendiabetes.vault.parser;

/**
 * Representation of the Nightscout server status
 */
public class Status {
    private String status;
    private String name;
    private String version;
    private String serverTime;
    private boolean apiEnabled;

    public String getStatus() {
        return status;
    }

    /**
     * @return true if the status equals "ok"
     */
    public boolean isStatusOk() {
        return "ok".equals(status);
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public String getServerTime() {
        return serverTime;
    }

    public boolean isApiEnabled() {
        return apiEnabled;
    }
}
