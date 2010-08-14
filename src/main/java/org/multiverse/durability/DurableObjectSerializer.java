package org.multiverse.durability;

import org.multiverse.stms.beta.transactions.BetaTransaction;

/**
 * Responsible for materializing and dematerializing DurableObjects and State.
 *
 * @author Peter Veentjer.
 */
public interface DurableObjectSerializer<O extends DurableObject, S extends DurableState> {

    byte[] serialize(S state);

    /**
     * Populates the state
     *
     * @param state
     * @param content
     * @param loader
     */
    void populate(S state, byte[] content, DurableObjectLoader loader);

    S deserializeState(O owner, byte[] content, DurableObjectLoader loader);

    O deserializeObject(String id, byte[] content, BetaTransaction transaction);
}
