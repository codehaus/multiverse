package org.multiverse.stms.beta.transactions;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.beta.BetaStm;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class BetaTransactionPool_transactionsTest {

    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
    }

    @Test
    public void testFatTransactions() {
        BetaTransactionPool pool = new BetaTransactionPool();

        FatMonoBetaTransaction monoTx1 = new FatMonoBetaTransaction(stm);
        FatMonoBetaTransaction monoTx2 = new FatMonoBetaTransaction(stm);
        FatArrayBetaTransaction arrayTx1 = new FatArrayBetaTransaction(stm);
        FatArrayBetaTransaction arrayTx2 = new FatArrayBetaTransaction(stm);
        FatArrayTreeBetaTransaction arrayTreeTx1 = new FatArrayTreeBetaTransaction(stm);
        FatArrayTreeBetaTransaction arrayTreeTx2 = new FatArrayTreeBetaTransaction(stm);

        pool.putBetaTransaction(monoTx1);
        pool.putBetaTransaction(monoTx2);
        pool.putBetaTransaction(arrayTx1);
        pool.putBetaTransaction(arrayTx2);
        pool.putBetaTransaction(arrayTreeTx1);
        pool.putBetaTransaction(arrayTreeTx2);

        assertSame(arrayTreeTx2, pool.takeFatArrayTreeBetaTransaction());
        assertSame(monoTx2, pool.takeFatMonoBetaTransaction());
        assertSame(monoTx1, pool.takeFatMonoBetaTransaction());
        assertNull(pool.takeFatMonoBetaTransaction());

        assertSame(arrayTx2, pool.takeFatArrayBetaTransaction());
        assertSame(arrayTx1, pool.takeFatArrayBetaTransaction());
        assertNull(pool.takeFatArrayBetaTransaction());

        assertSame(arrayTreeTx1, pool.takeFatArrayTreeBetaTransaction());
        assertNull(pool.takeFatArrayBetaTransaction());
    }

    @Test
    public void testLeanTransactions() {
        BetaTransactionPool pool = new BetaTransactionPool();

        LeanMonoBetaTransaction monoTx1 = new LeanMonoBetaTransaction(stm);
        LeanMonoBetaTransaction monoTx2 = new LeanMonoBetaTransaction(stm);
        LeanArrayBetaTransaction arrayTx1 = new LeanArrayBetaTransaction(stm);
        LeanArrayBetaTransaction arrayTx2 = new LeanArrayBetaTransaction(stm);
        LeanArrayTreeBetaTransaction arrayTreeTx1 = new LeanArrayTreeBetaTransaction(stm);
        LeanArrayTreeBetaTransaction arrayTreeTx2 = new LeanArrayTreeBetaTransaction(stm);

        pool.putBetaTransaction(monoTx1);
        pool.putBetaTransaction(monoTx2);
        pool.putBetaTransaction(arrayTx1);
        pool.putBetaTransaction(arrayTx2);
        pool.putBetaTransaction(arrayTreeTx1);
        pool.putBetaTransaction(arrayTreeTx2);

        assertSame(arrayTreeTx2, pool.takeLeanArrayTreeBetaTransaction());
        assertSame(monoTx2, pool.takeLeanMonoBetaTransaction());
        assertSame(monoTx1, pool.takeLeanMonoBetaTransaction());
        assertNull(pool.takeLeanMonoBetaTransaction());

        assertSame(arrayTx2, pool.takeLeanArrayBetaTransaction());
        assertSame(arrayTx1, pool.takeLeanArrayBetaTransaction());
        assertNull(pool.takeLeanArrayBetaTransaction());

        assertSame(arrayTreeTx1, pool.takeLeanArrayTreeBetaTransaction());
        assertNull(pool.takeLeanArrayBetaTransaction());
    }
}
