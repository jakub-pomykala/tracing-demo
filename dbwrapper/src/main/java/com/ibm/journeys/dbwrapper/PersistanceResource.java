package com.ibm.journeys.dbwrapper;

import javax.json.JsonObject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import java.util.Enumeration;


@Path("persist")
public class PersistanceResource {

    @POST
    public void instrument(JsonObject jsonObject, @Context HttpServletRequest request) {

        // Write instrument to a database
		logHeaders(request);
        String instrument = jsonObject.getString("instrument", null);

        long millis = (long) (Math.random() * 1000);

        // Simulate potentially wrong running call to database.
        // TODO: why not also simulate failures here?
        System.out.println("Persisting " + instrument + " to data store.");
        try  {
            Thread.sleep(millis);
        }
        catch (Exception e) {}

    }

    private void logHeaders(HttpServletRequest request) {
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String header = headerNames.nextElement();
            System.out.println(header + ": " + request.getHeader(header));
        }
        System.out.println();
    }
}
