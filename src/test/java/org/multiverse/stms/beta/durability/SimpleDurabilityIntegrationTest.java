package org.multiverse.stms.beta.durability;

import org.junit.Before;
import org.junit.Ignore;
import org.multiverse.durability.SimpleStorage;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaStmConstants;

@Ignore
public class SimpleDurabilityIntegrationTest implements BetaStmConstants {
    private BetaStm stm;
    private SimpleStorage storage;

    @Before
    public void setUp() {
        stm = new BetaStm();
        storage = new SimpleStorage(stm);
        //    storage.register(BetaLongRef.class, new LongRefSerializer());
        storage.clear();
    }

    /*
    @Test
    public void whenSingleItemWritten() {
        BetaLongRef ref = newLongRef(stm, 100);

        UnitOfWrite unitOfWrite = storage.startUnitOfWrite();
        unitOfWrite.addChange(ref.___unsafeLoad());
        unitOfWrite.addRoot(ref);
        unitOfWrite.commit();

        storage.clearEntities();

        BetaLongRef loaded = (BetaLongRef) storage.loadDurableObject(ref.___getStorageId());
        assertEquals(100, loaded.atomicGet());
    }


    @Test
    public void whenUpdated() {
        BetaLongRef ref = newLongRef(stm, 100);

        UnitOfWrite write1 = storage.startUnitOfWrite();
        write1.addChange(ref.___unsafeLoad());
        write1.addRoot(ref);
        write1.commit();

        storage.clearEntities();

        BetaLongRef loaded = (BetaLongRef) storage.loadDurableObject(ref.___getStorageId());
        
        loaded.atomicIncrementAndGet(1);

        UnitOfWrite write2 = storage.startUnitOfWrite();
        write2.addChange(loaded.___unsafeLoad());
        write2.commit();

        BetaLongRef loaded2 = (BetaLongRef) storage.loadDurableObject(loaded.___getStorageId());
        assertEquals(101, loaded2.atomicGet());
    }

    @Test
    public void whenMultipleItemsWritten() {
        BetaLongRef ref1 = newLongRef(stm, 100);
        BetaLongRef ref2 = newLongRef(stm, 200);


        UnitOfWrite unitOfWrite = storage.startUnitOfWrite();
        unitOfWrite.addChange(ref1.___unsafeLoad());
        unitOfWrite.addChange(ref2.___unsafeLoad());
        unitOfWrite.addRoot(ref1);
        unitOfWrite.addRoot(ref2);
        unitOfWrite.commit();

        storage.clearEntities();

        BetaLongRef loaded1 = (BetaLongRef) storage.loadDurableObject(ref1.___getStorageId());
        assertEquals(100, loaded1.atomicGet());
        BetaLongRef loaded2 = (BetaLongRef) storage.loadDurableObject(ref2.___getStorageId());
        assertEquals(200, loaded2.atomicGet());
    }


    class LongRefSerializer implements DurableObjectSerializer<BetaLongRef, LongRefTranlocal> {

        @Override
        public byte[] serialize(LongRefTranlocal state) {
            Map map = new HashMap<String, String>();
            map.put("value", "" + state.value);
            return SerializeUtils.serialize(map);
        }

        @Override
        public void populate(LongRefTranlocal state, byte[] content, DurableObjectLoader loader) {
            Map<String, String> map = SerializeUtils.deserializeMap(content);
            state.value = Long.parseLong(map.get("value"));
        }

        @Override
        public LongRefTranlocal deserializeState(BetaLongRef owner, byte[] content, DurableObjectLoader loader) {
            throw new TodoException();
        }

        @Override
        public BetaLongRef deserializeObject(String id, byte[] content, BetaTransaction transaction) {
            BetaLongRef ref = new BetaLongRef(transaction);
            ref.___setStorageId(id);
            return ref;
        }
    }          */
}
