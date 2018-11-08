/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package restclient;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType; 
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

/**
 *
 * @author anna Root resource (exposed at "myresource" path)
 */
@Path("https://TUDarmstadtUAM@uam-bp11.ns.10be.de:22577")
public class RESTClient {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        System.out.println(getIt());
        // TODO code application logic here
    }

    /**
     * Method handling HTTP GET requests. The returned object will be sent to
     * the client as "text/plain" media type.
     *
     * @return String that will be returned as a text/plain response.
     */
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public static String getIt() {
        return "Got it!";
    }
}
