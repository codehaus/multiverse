package org.multiverse.durability;

public interface DurableObjectLoader {

    DurableObject load(String id);
}
