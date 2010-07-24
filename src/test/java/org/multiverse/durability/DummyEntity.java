package org.multiverse.durability;

public class DummyEntity implements DurableObject {
    private String id;
    private final byte[] bytes;

    public DummyEntity(String id, byte[] bytes) {
        this.id = id;
        this.bytes = bytes;
    }

    @Override
    public String getStorageId() {
        return id;
    }

    @Override
    public void setStorageId(String id) {
        this.id = id;
    }

    public byte[] toBytes() {
        return bytes;
    }

}
