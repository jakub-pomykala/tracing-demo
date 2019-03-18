package com.sebastian_daschner.maker_bot;

import javax.ejb.Stateless;

import java.util.Random;
import java.util.concurrent.locks.LockSupport;
import javax.inject.Inject;

@Stateless
public class MakerBot {

    @Inject
    Persist persist;

    public void print(String instrument, String reqId) {
        LockSupport.parkNanos(80_000_000L);

        long millis = (long) (Math.random() * 1000);

        try  {
            Thread.sleep(millis);
        }
        catch (Exception e) {}

        System.out.println("Printing a " + instrument + " after sleeping for " + millis + " ms");
        // persist.persistInstrument(instrument, reqId);
    }

}
