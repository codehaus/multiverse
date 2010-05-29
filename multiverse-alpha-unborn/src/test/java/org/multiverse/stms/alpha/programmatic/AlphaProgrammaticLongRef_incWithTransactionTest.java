package org.multiverse.stms.alpha.programmatic;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
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
public class AlphaProgrammaticLongRef_incWithTransactionTest {

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
        AlphaProgrammaticLongRef ref = new AlphaProgrammaticLongRef(stm, 10);

        long version = stm.getVersion();
        try {
            ref.inc(null, 20);
            fail();
        } catch (NullPointerException expected) {
        }
        assertEquals(version, stm.getVersion());
        assertEquals(10, ref.atomicGet());
    }

    @Test
    public void whenTransactionAborts_thenValueNotCommitted() {
        AlphaProgrammaticLongRef ref = new AlphaProgrammaticLongRef(stm, 10);

        AlphaTransaction tx = stm.getTransactionFactoryBuilder()
                .setSpeculativeConfigurationEnabled(false)
                .setReadonly(false)
                .build()
                .start();

        long version = stm.getVersion();
        ref.inc(tx, 1);
        tx.abort();

        assertIsAborted(tx);
        assertEquals(version, stm.getVersion());
        assertEquals(10, ref.get());
    }

    @Test
    public void whenSuccess() {
        AlphaProgrammaticLongRef ref = new AlphaProgrammaticLongRef(stm, 10);

        AlphaTransaction tx = stm.getTransactionFactoryBuilder()
                .setSpeculativeConfigurationEnabled(false)
                .setReadonly(false)
                .build()
                .start();

        long version = stm.getVersion();
        ref.inc(tx, 1);
        tx.commit();

        assertIsCommitted(tx);
        assertEquals(version + 1, stm.getVersion());
        assertEquals(11, ref.get());
    }

    @Test
    public void whenAborted_thenDeadTransactionException() {
        AlphaProgrammaticLongRef ref = new AlphaProgrammaticLongRef(stm, 10);

        AlphaTransaction tx = stm.getTransactionFactoryBuilder()
                .build()
                .start();

        tx.abort();

        long version = stm.getVersion();
        try {
            ref.inc(tx, 1);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsAborted(tx);
        assertEquals(version, stm.getVersion());
        assertEquals(10, ref.get());
    }

    @Test
    public void whenCommitted_thenDeadTransactionException() {
        AlphaProgrammaticLongRef ref = new AlphaProgrammaticLongRef(stm, 10);

        AlphaTransaction tx = stm.getTransactionFactoryBuilder()
                .build()
                .start();
        tx.commit();

        long version = stm.getVersion();
        try {
            ref.inc(tx, 1);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsCommitted(tx);
        assertEquals(version, stm.getVersion());
        assertEquals(10, ref.get());
    }

    @Test
    public void whenPrepared_thenPreparedTransactionException() {
        AlphaProgrammaticLongRef ref = new AlphaProgrammaticLongRef(stm, 10);

        AlphaTransaction tx = stm.getTransactionFactoryBuilder()
                .build()
                .start();
        tx.prepare();

        long version = stm.getVersion();
        try {
            ref.inc(tx, 1);
            fail();
        } catch (PreparedTransactionException expected) {
        }

        assertIsPrepared(tx);
        assertEquals(version, stm.getVersion());
        assertEquals(10, ref.get());
    }

}
