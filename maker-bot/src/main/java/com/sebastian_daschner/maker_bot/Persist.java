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

    private Client client, pipeClient;
    private WebTarget target_persist;
    private WebTarget target_pipeline;

    String pipeline_url;
    String persist_url;

    @PostConstruct
    private void initClient() {
        client = ClientBuilder.newBuilder()
                .connectTimeout(2, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .build();
        pipeClient = ClientBuilder.newBuilder()
                .connectTimeout(2, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .build();
        
        // Use URL in environment
        pipeline_url = System.getenv("PIPELINE_URL");
        persist_url = System.getenv("PERSIST_URL");
        System.out.println ("Using persistence and pipeline URLs from environment: " + persist_url + " : " + pipeline_url);
        target_persist = client.target(persist_url);
        target_pipeline = pipeClient.target(pipeline_url);
    }

    public void persistInstrument(String instrument, String reqId) {
        JsonObject requestBody = createRequestBody();
        sendRequest(requestBody, reqId);
    }

    private JsonObject createRequestBody() {
        return Json.createObjectBuilder()
                .add("instrument", "Instrument_type_goes_here")
                .build();
    }

    private void sendRequest(JsonObject requestBody, String reqId) {

        try {
            System.out.println("Kicking off processing pipeline");
            target_pipeline.request().header("x-request-id", reqId).post(Entity.json(requestBody));
        } catch (Exception e) {
            throw new IllegalStateException("Could not instantiate pipeline, reason: " + e.getMessage(), e);
        }
        

        try {
            System.out.println("Saving instrument to database");
            target_persist.request().header("x-request-id", reqId).post(Entity.json(requestBody));
        } catch (Exception e) {
            throw new IllegalStateException("Could not save instrument, reason: " + e.getMessage(), e);
        }
        
    }

    @PreDestroy
    private void closeClient() {
        client.close();
        pipeClient.close();
    }
}
