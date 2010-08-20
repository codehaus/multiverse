package org.multiverse.durability;

import org.multiverse.api.exceptions.TodoException;

public class DummyEntity implements DurableObject {
    private String id;
    private final byte[] bytes;

    public DummyEntity(String id, byte[] bytes) {
        this.id = id;
        this.bytes = bytes;
    }

    @Override
    public String ___getStorageId() {
        return id;
    }

    @Override
    public void ___setStorageId(String id) {
        this.id = id;
    }

    public byte[] toBytes() {
        return bytes;
    }

    @Override
    public void ___markAsDurable() {
        throw new TodoException();
    }

    @Override
    public boolean ___isDurable() {
        throw new TodoException();
    }
}
