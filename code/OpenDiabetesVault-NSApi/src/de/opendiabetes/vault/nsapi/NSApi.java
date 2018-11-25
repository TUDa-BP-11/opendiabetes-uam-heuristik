package de.opendiabetes.vault.nsapi;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;

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

    public GetBuilder getSlice(String storage, String field, String type, String prefix, String regex) {
        return new GetBuilder(client, target, "slice/" + storage + "/" + field + "/" + type + "/" + prefix + "/" + regex).token(token);
    }

    public String postEntries(String entries) {
        return client.target(target).path("entries").queryParam("token", token).request(MediaType.APPLICATION_JSON).post(Entity.json(entries), String.class);
    }

}
