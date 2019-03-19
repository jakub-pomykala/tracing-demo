package com.ibm.journeys.pipeline;

import javax.json.JsonObject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import java.util.Enumeration;
import javax.inject.Inject;

@Path("process")
public class PipelineResource {

	@Inject
    Transform tx;
    String reqId;
	
    @POST
    public void doProcess(JsonObject jsonObject, @Context HttpServletRequest request) {

        // Do something with the payload and call next in chain
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

		tx.transform(instrument, reqId);

    }

    private void logHeaders(HttpServletRequest request) {
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String header = headerNames.nextElement();
            System.out.println(header + ": " + request.getHeader(header));
			if(header.equals("x-request-id")) {
				reqId = request.getHeader(header);
			}
        }
        System.out.println();
    }
}
