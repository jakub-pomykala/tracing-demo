package com.sebastian_daschner.instrument_craft_shop.control;

import com.sebastian_daschner.instrument_craft_shop.entity.InstrumentType;

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
public class MakerBot {

    private Client client;
    private WebTarget target;

    @PostConstruct
    private void initClient() {
        client = ClientBuilder.newBuilder()
                .connectTimeout(2, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
        
        target = client.target("http://maker-bot:9080/maker-bot/resources/jobs");
    }

    public void printInstrument(InstrumentType type, String reqId, String parentspan, int instrument_count) {
        JsonObject requestBody = createRequestBody(type);
        Response response = sendRequest(requestBody, reqId, parentspan, instrument_count);
        validateResponse(response);
    }

    private JsonObject createRequestBody(InstrumentType type) {
        return Json.createObjectBuilder()
                .add("instrument", type.name().toLowerCase())
                .build();
    }

    private Response sendRequest(JsonObject requestBody, String reqId, String parentspan, int instrument_count) {
            Response r = null;
            while (instrument_count>0) {
                System.out.println(instrument_count + " instruments left to print.");
                 r = target.request().post(Entity.json(requestBody));
                 validateResponse(r);
                instrument_count--;
            }
            return r;
    }

    private void validateResponse(Response response) {
        if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL)
            throw new IllegalStateException("Could not print instrument, status: " + response.getStatus());
    }

    @PreDestroy
    private void closeClient() {
        client.close();
    }
}
