package org.multiverse.api.blocking;

/**
 * @author Peter Veentjer
 */
public interface LatchFactory {

    /**
     * Creates a new Latch.
     *
     * @return
     */
    Latch create();
}
