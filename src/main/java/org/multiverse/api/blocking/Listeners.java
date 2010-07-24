package org.multiverse.api.blocking;

/**
 * This is an 'immutable' class. It can be either used for
 *
 */
public class Listeners {

    public Listeners next;
    public Latch listener;
    public long listenerEra;

    public void reset(){
        next = null;
        listener = null;
        listenerEra = Long.MIN_VALUE;
    }
}
