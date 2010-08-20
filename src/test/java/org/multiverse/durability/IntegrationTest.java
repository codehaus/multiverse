package org.multiverse.durability;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.api.exceptions.TodoException;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import java.util.UUID;

@Ignore
public class IntegrationTest {

    private SimpleStorage storage;

    @Before
    public void setUp() {
        storage = new SimpleStorage(new BetaStm());
        storage.register(DummyEntity.class, new DummyEntitySerializer());
        storage.clear();
    }

    @Test
    public void test() {
        DurableObject entity = new DummyEntity(UUID.randomUUID().toString(), new byte[]{});

        UnitOfWrite unitOfWrite = storage.startUnitOfWrite();
        unitOfWrite.addRoot(entity);
        unitOfWrite.commit();
    }

    class DummyEntitySerializer implements DurableObjectSerializer {

        @Override
        public DurableObject deserializeObject(String id, byte[] content, BetaTransaction transaction) {
            throw new TodoException();
        }

        @Override
        public void populate(DurableState state, byte[] content, DurableObjectLoader loader) {
            throw new TodoException();
        }

        @Override
        public byte[] serialize(DurableState state) {
            return new byte[0];  //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public DurableState deserializeState(DurableObject owner, byte[] content, DurableObjectLoader loader) {
            throw new TodoException();
        }

    }

    //@Test
    //public void storeAndLoad() {
    //    DummyEntity write = new DummyEntity("foo",new byte[]{1,2,3});
    //    //storage.persist(write);
    //
    //    DummyEntity read = (DummyEntity) storage.loadEntity(write.___getStorageId());
    //    assertEquals(write.___getStorageId(),read.___getStorageId());
    //    assertEqualByteArray(write.toBytes(), read.toBytes());
    //}
}
