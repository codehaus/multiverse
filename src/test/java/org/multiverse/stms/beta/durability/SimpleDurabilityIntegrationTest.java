package org.multiverse.stms.beta.durability;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.exceptions.TodoException;
import org.multiverse.durability.DurableObjectLoader;
import org.multiverse.durability.DurableObjectSerializer;
import org.multiverse.durability.SimpleStorage;
import org.multiverse.durability.UnitOfWrite;
import org.multiverse.durability.account.SerializeUtils;
import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.refs.LongRef;
import org.multiverse.stms.beta.refs.LongRefTranlocal;
import org.multiverse.stms.beta.transactions.BetaTransaction;
import org.multiverse.stms.beta.transactions.FatMonoBetaTransaction;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.multiverse.stms.beta.BetaStmUtils.createLongRef;

public class SimpleDurabilityIntegrationTest {
    private BetaStm stm;
    private BetaObjectPool pool;
    private SimpleStorage storage;

    @Before
    public void setUp() {
        stm = new BetaStm();
        pool = new BetaObjectPool();
        storage = new SimpleStorage(stm);
        storage.register(LongRef.class, new LongRefSerializer());
        storage.clear();
    }

    @Test
    public void whenSingleItemWritten() {
        LongRef ref = createLongRef(stm, 100);

        UnitOfWrite unitOfWrite = storage.startUnitOfWrite();
        unitOfWrite.addChange(ref.active);
        unitOfWrite.addRoot(ref);
        unitOfWrite.commit();

        storage.clearEntities();

        LongRef loaded = (LongRef) storage.loadDurableObject(ref.getStorageId());
        assertEquals(100, loaded.unsafeLoad().value);
    }


    @Test
    public void whenUpdated() {
        LongRef ref = createLongRef(stm, 100);

        UnitOfWrite write1 = storage.startUnitOfWrite();
        write1.addChange(ref.active);
        write1.addRoot(ref);
        write1.commit();

        storage.clearEntities();

        LongRef loaded = (LongRef) storage.loadDurableObject(ref.getStorageId());

        BetaTransaction tx = new FatMonoBetaTransaction(stm);
        tx.openForWrite(loaded, false, pool).value++;
        tx.commit();

        UnitOfWrite write2 = storage.startUnitOfWrite();
        write2.addChange(loaded.active);
        write2.commit();

        LongRef loaded2 = (LongRef) storage.loadDurableObject(loaded.getStorageId());
        assertEquals(101, loaded2.unsafeLoad().value);
    }

    @Test
    public void whenMultipleItemsWritten() {
        LongRef ref1 = createLongRef(stm, 100);
        LongRef ref2 = createLongRef(stm, 200);


        UnitOfWrite unitOfWrite = storage.startUnitOfWrite();
        unitOfWrite.addChange(ref1.active);
        unitOfWrite.addChange(ref2.active);
        unitOfWrite.addRoot(ref1);
        unitOfWrite.addRoot(ref2);
        unitOfWrite.commit();

        storage.clearEntities();

        LongRef loaded1 = (LongRef) storage.loadDurableObject(ref1.getStorageId());
        assertEquals(100, loaded1.unsafeLoad().value);
        LongRef loaded2 = (LongRef) storage.loadDurableObject(ref2.getStorageId());
        assertEquals(200, loaded2.unsafeLoad().value);
    }


    class LongRefSerializer implements DurableObjectSerializer<LongRef, LongRefTranlocal> {

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
        public LongRefTranlocal deserializeState(LongRef owner, byte[] content, DurableObjectLoader loader) {
            throw new TodoException();
        }

        @Override
        public LongRef deserializeObject(String id, byte[] content, BetaTransaction transaction) {
            LongRef ref = new LongRef(transaction);
            ref.setStorageId(id);
            return ref;
        }
    }
}
