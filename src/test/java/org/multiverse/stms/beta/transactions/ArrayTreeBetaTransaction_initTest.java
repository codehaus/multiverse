package org.multiverse.stms.beta.transactions;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.lifecycle.TransactionLifecycleListener;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.ObjectPool;
import org.multiverse.stms.beta.refs.LongRef;
import org.multiverse.stms.beta.refs.Tranlocal;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.multiverse.TestUtils.*;
import static org.multiverse.stms.beta.StmUtils.createLongRef;
import static org.multiverse.stms.beta.orec.OrecTestUtils.assertUnlocked;

public class ArrayTreeBetaTransaction_initTest {
     private BetaStm stm;
    private ObjectPool pool;

    @Before
    public void setUp() {
        stm = new BetaStm();
        pool = new ObjectPool();
    }

    @Test
    public void whenUnstarted() {

    }

     @Test
    public void whenNullConfig_thenNullPointerException() {
        ArrayTreeBetaTransaction tx = new ArrayTreeBetaTransaction(stm);
        tx.prepare(pool);

        try{
            tx.init(null);
            fail();
        }catch(NullPointerException expected){
        }

        assertPrepared(tx);
    }
    
    @Test
    public void whenPermanentListenersAvailable_thenTheyAreRemoved() {
        ArrayTreeBetaTransaction tx = new ArrayTreeBetaTransaction(stm);
        TransactionLifecycleListener listener = mock(TransactionLifecycleListener.class);
        tx.registerPermanent(listener);

        BetaTransactionConfig config = new BetaTransactionConfig(stm);
        tx.init(config);

        assertSame(config, tx.getConfig());
        assertInitialized(tx);
    }

    @Test
    public void whenNormalListenersAvailable_thenTheyAreRemoved() {
        ArrayTreeBetaTransaction tx = new ArrayTreeBetaTransaction(stm);
        TransactionLifecycleListener listener = mock(TransactionLifecycleListener.class);
        tx.register(listener);

        BetaTransactionConfig config = new BetaTransactionConfig(stm);
        tx.init(config);

        assertSame(config, tx.getConfig());
        assertInitialized(tx);
    }

    @Test
    public void whenPrepared_thenAborted() {
        LongRef ref = createLongRef(stm);

        ArrayTreeBetaTransaction tx = new ArrayTreeBetaTransaction(stm);
        tx.openForWrite(ref,true,pool);
        tx.prepare(pool);

        BetaTransactionConfig config = new BetaTransactionConfig(stm);
        tx.init(config);

        assertSame(config, tx.getConfig());
        assertInitialized(tx);

        assertUnlocked(ref);
    }

    @Test
    public void whenAborted() {
        LongRef ref = createLongRef(stm);

        ArrayTreeBetaTransaction tx = new ArrayTreeBetaTransaction(stm);
        tx.openForWrite(ref, false, pool).value++;
        tx.abort(pool);

        BetaTransactionConfig config = new BetaTransactionConfig(stm);
        tx.init(config);

        assertSame(config, tx.getConfig());
        assertInitialized(tx);
    }

    @Test
    public void whenCommitted() {
        LongRef ref = createLongRef(stm);
        ArrayTreeBetaTransaction tx = new ArrayTreeBetaTransaction(stm);
        tx.openForWrite(ref, false, pool).value++;
        tx.commit(pool);

        BetaTransactionConfig config = new BetaTransactionConfig(stm);
        tx.init(config);

        assertSame(config, tx.getConfig());
        assertInitialized(tx);
    }

    private void assertInitialized(ArrayTreeBetaTransaction tx) {
        assertActive(tx);
        assertNull(getField(tx, "normalListeners"));
        assertNull(getField(tx, "permanentListeners"));
        assertAllNull((Tranlocal[]) getField(tx,"array"));
        assertEquals(false, getField(tx, "needsRealClose"));
        assertEquals(1, tx.getAttempt());
    }
}
