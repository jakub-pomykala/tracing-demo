package com.ibm.journeys.pipeline;

import javax.ejb.Stateless;

import java.util.Random;
import java.util.concurrent.locks.LockSupport;
import javax.inject.Inject;

@Stateless
public class ProcessData {

    @Inject
    NextNode nn;

    public void Process(String instrument, String reqId) {
        LockSupport.parkNanos(80_000_000L);
        String work_time = System.getenv("WORK_TIME");
        System.out.println("Max work time (secs) = " + work_time);
        long millis = (long) (Math.random() * (Integer.parseInt(work_time) * 1000));
        System.out.println("Work time = " + millis + " ms");
        try  {
            Thread.sleep(millis);
        }
        catch (Exception e) {}

        System.out.println("Processing " + instrument + " after sleeping for " + millis + " ms");

        nn.nextStep(instrument, reqId);
    }

}
