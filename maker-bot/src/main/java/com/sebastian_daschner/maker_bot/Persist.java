package com.sebastian_daschner.maker_bot;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.util.concurrent.TimeUnit;

@ApplicationScoped
public class Persist {

    private Client client;
    private WebTarget target;

    @PostConstruct
    private void initClient() {
        client = ClientBuilder.newBuilder()
                .connectTimeout(2, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .build();
        // Use URL in environment
        String url = System.getenv("PERSIST_URL");
        System.out.println ("Using persistence URL from environment: " + url);
        target = client.target(url);
    }

    public void persistInstrument(String instrument, String reqId) {
        JsonObject requestBody = createRequestBody();
        Response response = sendRequest(requestBody, reqId);
        validateResponse(response);
    }

    private JsonObject createRequestBody() {
        return Json.createObjectBuilder()
                .add("instrument", "Instrument_type_goes_here")
                .build();
    }

    private Response sendRequest(JsonObject requestBody, String reqId) {
        try {
            Response r = null;
            System.out.println("Saving instrument to database");
            r = target.request().header("x-request-id", reqId).post(Entity.json(requestBody));
            return r;

        } catch (Exception e) {
            throw new IllegalStateException("Could not save instrument, reason: " + e.getMessage(), e);
        }
    }

    private void validateResponse(Response response) {
        if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL)
            throw new IllegalStateException("Could not save instrument, status: " + response.getStatus());
    }

    @PreDestroy
    private void closeClient() {
        client.close();
    }
}
