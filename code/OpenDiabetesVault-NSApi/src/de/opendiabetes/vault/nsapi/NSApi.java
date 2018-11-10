package de.opendiabetes.vault.nsapi;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

public class NSApi {
    WebTarget target;

    public NSApi(String host) {
        Client client = ClientBuilder.newClient();

        target = client.target(host + "/api/v1");
    }

    private Invocation.Builder buildPath(String path) {
        Invocation.Builder b = target.path(path).request(MediaType.APPLICATION_JSON);
        return b;
    }

    public String getStatus() {
        return buildPath("status").get(String.class);
    }

    public GetEntriesBuilder getEntries() {
        return new GetEntriesBuilder(this);
    }
    
    public String get(String path) {
        System.out.println(path);
        return buildPath(path).get(String.class);
    }
    
    public String getProfile() {
        return buildPath("profile").get(String.class);
    }
    
    public String getTreatments() {
        return buildPath("treatments").get(String.class);
    }
}
