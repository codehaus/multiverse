package org.multiverse.stms.beta.transactions;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.ObjectPool;

import static org.junit.Assert.fail;
import static org.multiverse.TestUtils.assertAborted;
import static org.multiverse.TestUtils.assertCommitted;

/**
 * @author Peter Veentjer
 */
public class ArrayTreeBetaTransaction_abortTest {

    private BetaStm stm;
    private ObjectPool pool;

    @Before
    public void setUp() {
        stm = new BetaStm();
        pool = new ObjectPool();
    }

    @Test
    public void whenUnused(){
        ArrayTreeBetaTransaction tx = new ArrayTreeBetaTransaction(stm);
        tx.abort(pool);

        assertAborted(tx);
    }

    @Test
    @Ignore
    public void whenNormalReads(){

    }

    @Test
    @Ignore
    public void whenPessimisticLockedReads(){

    }

    @Test
    @Ignore
    public void whenReadBiasedReads(){

    }

    //todo: writes.

    @Test
    @Ignore
    public void whenPrepared(){

    }

    @Test
    public void whenAborted_thenIgnored() {
        ArrayTreeBetaTransaction tx = new ArrayTreeBetaTransaction(stm);
        tx.abort(pool);

        tx.abort(pool);
        assertAborted(tx);
    }

    @Test
    public void whenCommitted_thenDeadTransactionException() {
        ArrayTreeBetaTransaction tx = new ArrayTreeBetaTransaction(stm);
        tx.commit(pool);

        try {
            tx.abort(pool);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertCommitted(tx);
    }
}
