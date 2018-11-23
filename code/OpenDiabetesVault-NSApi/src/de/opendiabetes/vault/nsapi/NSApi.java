package de.opendiabetes.vault.nsapi;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

public class NSApi {
    private Client client;
    private String target;
    private String token;

    public NSApi(String host, String port, String token) {
        this.client = ClientBuilder.newClient();
        this.target = "https://" + host + ":" + port + "/api/v1";
        this.token = token;
    }

    public String getStatus() {
        return new GetBuilder(client, target, "status").token(token).get();
    }

    public GetBuilder getEntries() {
        return new GetBuilder(client, target, "entries").token(token);
    }
}
