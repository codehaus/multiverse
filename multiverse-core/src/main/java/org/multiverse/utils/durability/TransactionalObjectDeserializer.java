package org.multiverse.utils.durability;

public interface TransactionalObjectDeserializer {

    void start(Object atomicObject);

    void end(Object atomicObject);

    void persist(String field, boolean value);

    void persist(String field, byte value);

    void persist(String field, char value);

    void persist(String field, double value);

    void persist(String field, float value);

    void persist(String field, int value);

    void persist(String field, long value);

    void persist(String field, short value);

    void persist(Object value);
}
