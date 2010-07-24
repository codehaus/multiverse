package org.multiverse.durability;

import org.junit.Before;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertEquals;

public class IntegrationTest {

    private SimpleStorage storage;

    @Before
    public void setUp() {
        storage = new SimpleStorage();
        storage.register(DummyEntity.class, new DummyEntitySerializer());
        storage.clear();
    }

    @Test
    public void test(){
        DurableObject entity = new DummyEntity(UUID.randomUUID().toString(),new byte[]{});

        UnitOfWork unitOfWork = storage.startUnitOfWork();
        unitOfWork.addRoot(entity);
        unitOfWork.commit();
    }

    class DummyEntitySerializer implements DurableObjectSerializer {

        @Override
        public byte[] serialize(DurableState state) {
            return new byte[0];  //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public DurableState deserializeState(DurableObject owner, byte[] content,DurableObjectLoader loader) {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public DurableObject deserializeObject(String id, byte[] content) {
            return new DummyEntity(id, content);
        }
    }

    //@Test
    //public void storeAndLoad() {
    //    DummyEntity write = new DummyEntity("foo",new byte[]{1,2,3});
    //    //storage.persist(write);
    //
    //    DummyEntity read = (DummyEntity) storage.loadEntity(write.getStorageId());
    //    assertEquals(write.getStorageId(),read.getStorageId());
    //    assertEqualByteArray(write.toBytes(), read.toBytes());
    //}
}
