package de.opendiabetes.vault.nsapi;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

public class NSApi {
    WebTarget target;

    public NSApi(String host) {
        Client client = ClientBuilder.newClient();

        target = client.target(host + "/api/v1");
    }

    public String getStatus() {
        String response = target.path("status")
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .get(String.class);
        return response;
    }

}
