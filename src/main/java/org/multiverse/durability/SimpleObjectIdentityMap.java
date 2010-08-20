package org.multiverse.durability;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class SimpleObjectIdentityMap implements ObjectIdentityMap {

    private final ConcurrentMap<String, DurableObject> entities
            = new ConcurrentHashMap<String, DurableObject>();

    @Override
    public DurableObject get(String id) {
        if (id == null) {
            throw new NullPointerException();
        }

        return entities.get(id);
    }

    @Override
    public void clear() {
        entities.clear();
    }

    @Override
    public DurableObject putIfAbsent(DurableObject object) {
        if (object == null) {
            throw new NullPointerException();
        }

        String id = object.___getStorageId();
        return entities.putIfAbsent(id, object);
    }
}
