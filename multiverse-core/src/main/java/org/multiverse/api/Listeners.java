package org.multiverse.api;

import org.multiverse.api.latches.Latch;

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


    /**
     * Opens all listeners. Stops as soon as it finds a null, and can safely be called with a null listenersArray.
     *
     * @param arrayOfListeners the array of Listeners.
     */
    public static void openAll(Listeners[] arrayOfListeners) {
        if (arrayOfListeners == null) {
            return;
        }

        for (int k = 0; k < arrayOfListeners.length; k++) {
            Listeners listeners = arrayOfListeners[k];
            if (listeners == null) {
                return;
            }
            listeners.openAll();
        }
    }
}