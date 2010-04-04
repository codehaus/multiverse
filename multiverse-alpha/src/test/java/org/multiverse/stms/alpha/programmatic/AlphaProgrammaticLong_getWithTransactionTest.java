package org.multiverse.stms.alpha.programmatic;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Transaction;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.PreparedTransactionException;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class AlphaProgrammaticLong_getWithTransactionTest {

    private AlphaStm stm;

    @Before
    public void setUp() {
        stm = (AlphaStm) getGlobalStmInstance();
        clearThreadLocalTransaction();
    }

    @After
    public void tearDown() {
        clearThreadLocalTransaction();
    }

    @Test
    public void whenTransactionNull_thenNullPointerException() {
        AlphaProgrammaticLong ref = new AlphaProgrammaticLong(stm, 1);

        long version = stm.getVersion();
        try {
            ref.get(null);
            fail();
        } catch (NullPointerException expected) {
        }

        assertEquals(version, stm.getVersion());
        assertEquals(1, ref.atomicGet());
    }

    @Test
    public void whenSuccess() {
        AlphaProgrammaticLong ref = new AlphaProgrammaticLong(stm, 1);

        Transaction tx = stm.getTransactionFactoryBuilder()
                .setSpeculativeConfigurationEnabled(false)
                .build()
                .start();

        long version = stm.getVersion();
        long result = ref.get(tx);

        assertEquals(version, stm.getVersion());
        assertEquals(1, ref.atomicGet());
        assertEquals(1, result);
    }

    @Test
    public void whenAborted_thenDeadTransactionException() {
        AlphaProgrammaticLong ref = new AlphaProgrammaticLong(stm, 10);

        AlphaTransaction tx = stm.getTransactionFactoryBuilder()
                .build()
                .start();

        tx.abort();

        long version = stm.getVersion();
        try {
            ref.get(tx);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsAborted(tx);
        assertEquals(version, stm.getVersion());
        assertEquals(10, ref.get());
    }

    @Test
    public void whenCommitted_thenDeadTransactionException() {
        AlphaProgrammaticLong ref = new AlphaProgrammaticLong(stm, 10);

        AlphaTransaction tx = stm.getTransactionFactoryBuilder()
                .build()
                .start();
        tx.commit();

        long version = stm.getVersion();
        try {
            ref.get(tx);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsCommitted(tx);
        assertEquals(version, stm.getVersion());
        assertEquals(10, ref.get());
    }

    @Test
    public void whenPrepared_thenPreparedTransactionException() {
        AlphaProgrammaticLong ref = new AlphaProgrammaticLong(stm, 10);

        AlphaTransaction tx = stm.getTransactionFactoryBuilder()
                .build()
                .start();
        tx.prepare();

        long version = stm.getVersion();
        try {
            ref.get(tx);
            fail();
        } catch (PreparedTransactionException expected) {
        }

        assertIsPrepared(tx);
        assertEquals(version, stm.getVersion());
        assertEquals(10, ref.get());
    }
}
