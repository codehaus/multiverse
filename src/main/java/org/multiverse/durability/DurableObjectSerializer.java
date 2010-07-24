package org.multiverse.durability;

/**
 * Responsible for materializing and dematerializing DurableObjects and State.
 *
 * @author Peter Veentjer.
 */
public interface DurableObjectSerializer<O extends DurableObject, S extends DurableState> {

    byte[] serialize(S state);

    S deserializeState(O owner, byte[] content, DurableObjectLoader loader);

    O deserializeObject(String id, byte[] content);
}
