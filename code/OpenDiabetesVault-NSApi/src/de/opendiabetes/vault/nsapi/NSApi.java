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
        return client.target(target).path("status")
                .queryParam("token", token)
                .request(MediaType.APPLICATION_JSON)
                .get(String.class);
    }

    public GetBuilder getEntries() {
        return new GetBuilder(client, target, "entries").token(token);
    }

    public GetBuilder getSlice(String storage, String field, String type, String prefix, String regex) {
        return new GetBuilder(client, target, "slice/" + storage + "/" + field + "/" + type + "/" + prefix + "/" + regex).token(token);
    }

    public GetBuilder getEcho(String storage, String spec) {
        return new GetBuilder(client, target, "echo/" + storage + "/" + spec).token(token);
    }

    public GetBuilder getTimesEcho(String prefix, String regex) {
        return new GetBuilder(client, target, "times/echo/" + prefix + "/" + regex).token(token);
    }

    public GetBuilder getTimes(String prefix, String regex) {
        return new GetBuilder(client, target, "times/" + prefix + "/" + regex).token(token);
    }

    public String postEntries(String entries) {
        return client.target(target).path("entries")
                .queryParam("token", token)
                .request(MediaType.APPLICATION_JSON)
                .header("content-type", MediaType.APPLICATION_JSON)
                .post(Entity.json(entries), String.class);
    }

    public GetBuilder getTreatments() {
        return new GetBuilder(client, target, "treatments").token(token);
    }

    public String postTreatments(String treatments) {
        return client.target(target).path("treatments")
                .queryParam("token", token)
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(treatments), String.class);
    }

    public void close() {
        this.client.close();
    }
}
