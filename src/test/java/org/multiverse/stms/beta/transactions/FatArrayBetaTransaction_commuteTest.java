package org.multiverse.stms.beta.transactions;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.BetaStm;

public class FatArrayBetaTransaction_commuteTest {

    private BetaStm stm;
    private BetaObjectPool pool;

    @Before
    public void setUp() {
        stm = new BetaStm();
        pool = new BetaObjectPool();
    }

    @Test
    @Ignore
    public void whenAlreadyOpenedForRead() {

    }

    @Test
    @Ignore
    public void whenAlreadyOpenedForWrite() {

    }

    @Test
    @Ignore
    public void whenNotOpenedBefore() {

    }

    @Test
    @Ignore
    public void whenNotOpenedBeforeAndLocked() {

    }

    @Test
    @Ignore
    public void whenAlreadyOpenedForCommute() {

    }

    @Test
    @Ignore
    public void whenLocked() {

    }

    @Test
    @Ignore
    public void whenPrepared() {
    }

    @Test
    @Ignore
    public void whenAlreadyCommitted_thenDeadTransactionException() {
    }

    @Test
    @Ignore
    public void whenAlreadyAborted_thenDeadTransactionException() {
    }
}
