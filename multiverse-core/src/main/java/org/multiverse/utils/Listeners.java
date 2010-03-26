package org.multiverse.utils;

import org.multiverse.api.Latch;

/**
 * A immutable single-linked list for storing listener-latches.
 * <p/>
 * Structure is thread-safe and designed to be used in a CAS-loop.
 *
 * @author Peter Veentjer.
 */
public final class Listeners {
    private final Latch listener;
    private final Listeners next;

    public Listeners(Latch listener, Listeners next) {
        assert listener != null;
        this.listener = listener;
        this.next = next;
    }

    /**
     * Returns the listener Latch stored in this ListenerNode.
     *
     * @return the listener Latch.
     */
    public Latch getListener() {
        return listener;
    }

    /**
     * Gets the next ListenerNode or null if this ListerNode is the end of the line.
     *
     * @return the next ListenerNode
     */
    public Listeners getNext() {
        return next;
    }

    /**
     * Opens all listeners. Method is not recursive but iterative.
     */
    public void openAll() {
        Listeners node = this;
        do {
            node.listener.open();
            node = node.next;
        } while (node != null);
    }


}