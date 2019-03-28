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

import jdk.nashorn.internal.runtime.JSONFunctions;

import java.util.concurrent.TimeUnit;

@ApplicationScoped
public class NextNode {

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
        System.out.println ("Using pipeline URL from environment: " + url);
        if (url==null || !url.contains("http")) {
            System.out.println("Final processing step.");
            target = null;
        } else {
            target = client.target(url);
        }
    }

    public void nextStep(JsonObject requestBody) {
        Response response = sendRequest(requestBody);
        validateResponse(response);
    }

    private JsonObject createRequestBody() {
        return Json.createObjectBuilder()
                .add("instrument", "Instrument_type_goes_here")
                .build();
    }

    private Response sendRequest(JsonObject requestBody) {
        if (target != null) {
            Response r = null;
            System.out.println("Calling pipeline node: " + url);
            r = target.request().post(Entity.json(requestBody));
            return r;
        }

        return Response.ok("All done").build();
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
