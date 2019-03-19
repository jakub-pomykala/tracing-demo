package com.ibm.journeys.pipeline;

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
public class Transform {

    private Client client;
    private WebTarget target;
	String url;
	
    @PostConstruct
    private void initClient() {
        client = ClientBuilder.newBuilder()
                .connectTimeout(2, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .build();
        // Use URL in environment
        url = System.getenv("NEXT_STEP_URL");
        System.out.println ("Using URL from environment: " + url);
        target = client.target(url);
    }

    public void transform(String instrument, String reqId) {
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
            System.out.println("Calling pipeline node: " + url);
            r = target.request().header("x-request-id", reqId).post(Entity.json(requestBody));
            return r;

        } catch (Exception e) {
            throw new IllegalStateException("Could not invoke pipeline node, reason: " + e.getMessage(), e);
        }
    }

    private void validateResponse(Response response) {
        if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL)
            throw new IllegalStateException("Could not perform request, status: " + response.getStatus());
    }

    @PreDestroy
    private void closeClient() {
        client.close();
    }
}
