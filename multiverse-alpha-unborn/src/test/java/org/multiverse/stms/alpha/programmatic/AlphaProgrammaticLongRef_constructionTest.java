package org.multiverse.stms.alpha.programmatic;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.PreparedTransactionException;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class AlphaProgrammaticLongRef_constructionTest {
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
    public void constructorWithValue() {
        long version = stm.getVersion();
        AlphaProgrammaticLongRef ref = new AlphaProgrammaticLongRef(10);

        assertEquals(version, stm.getVersion());
        assertNull(ref.___getLockOwner());
        assertEquals(10, ref.get());
    }

    // ===================== value =======================

    @Test
    public void whenAborted_thenDeadTransactionException() {
        AlphaTransaction tx = stm.getTransactionFactoryBuilder()
                .build()
                .start();

        tx.abort();

        long version = stm.getVersion();
        try {
            new AlphaProgrammaticLongRef(tx, 10);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsAborted(tx);
        assertEquals(version, stm.getVersion());
    }

    @Test
    public void whenCommitted_thenDeadTransactionException() {
        AlphaTransaction tx = stm.getTransactionFactoryBuilder()
                .build()
                .start();

        tx.commit();

        long version = stm.getVersion();
        try {
            new AlphaProgrammaticLongRef(tx, 10);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsCommitted(tx);
        assertEquals(version, stm.getVersion());
    }

    @Test
    public void whenPrepared_thenPreparedTransactionException() {
        AlphaTransaction tx = stm.getTransactionFactoryBuilder()
                .build()
                .start();

        tx.prepare();

        long version = stm.getVersion();
        try {
            new AlphaProgrammaticLongRef(tx, 10);
            fail();
        } catch (PreparedTransactionException expected) {
        }

        assertIsPrepared(tx);
        assertEquals(version, stm.getVersion());
    }

    @Test
    public void whenNullTransaction_thenNullPointerException() {
        long version = stm.getVersion();

        try {
            new AlphaProgrammaticLongRef((AlphaTransaction) null, 10);
            fail();
        } catch (NullPointerException expected) {
        }

        assertEquals(version, stm.getVersion());
    }

    @Test
    public void whenTransactionUsed() {
        AlphaTransaction tx = stm.getTransactionFactoryBuilder()
                .setSpeculativeConfigurationEnabled(false)
                .setReadonly(false)
                .build()
                .start();

        String value = "foo";
        AlphaProgrammaticRef ref = new AlphaProgrammaticRef(tx, value);
        assertSame(value, ref.get(tx));
        assertNull(ref.___load());

        long version = stm.getVersion();
        tx.commit();

        assertSame(value, ref.atomicGet());
        assertIsCommitted(tx);
        assertEquals(version, stm.getVersion());
    }

    @Test
    public void whenTransactionAborts() {
        AlphaTransaction tx = stm.getTransactionFactoryBuilder()
                .setSpeculativeConfigurationEnabled(false)
                .setReadonly(false)
                .build()
                .start();

        String value = "foo";
        long version = stm.getVersion();
        AlphaProgrammaticRef ref = new AlphaProgrammaticRef(tx, value);
        tx.abort();

        assertNull(ref.___load());
        assertEquals(version, stm.getVersion());
    }
}
