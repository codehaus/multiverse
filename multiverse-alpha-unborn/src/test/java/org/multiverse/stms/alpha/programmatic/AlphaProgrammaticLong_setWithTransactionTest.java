package org.multiverse.stms.alpha.programmatic;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.PreparedTransactionException;
import org.multiverse.api.exceptions.UncommittedReadConflict;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class AlphaProgrammaticLong_setWithTransactionTest {
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
            ref.set(null, 20);
            fail();
        } catch (NullPointerException expected) {
        }

        assertEquals(version, stm.getVersion());
        assertEquals(1, ref.atomicGet());
    }

    @Test
    public void whenSuccess() {
        AlphaProgrammaticLong ref = new AlphaProgrammaticLong(stm, 10);

        AlphaTransaction tx = stm.getTransactionFactoryBuilder()
                .setSpeculativeConfigurationEnabled(false)
                .setReadonly(false)
                .build()
                .start();

        long version = stm.getVersion();
        ref.set(tx, 20);

        assertEquals(10, ref.atomicGet());
        tx.commit();

        assertEquals(20, ref.atomicGet());
        assertEquals(version + 1, stm.getVersion());
    }

    @Test
    public void whenNoChange() {
        AlphaProgrammaticLong ref = new AlphaProgrammaticLong(stm, 10);
        AlphaProgrammaticLongTranlocal committed = (AlphaProgrammaticLongTranlocal) ref.___load();

        AlphaTransaction tx = stm.getTransactionFactoryBuilder()
                .setSpeculativeConfigurationEnabled(false)
                .setReadonly(false)
                .build()
                .start();

        long version = stm.getVersion();
        ref.set(tx, 10);

        assertEquals(10, ref.atomicGet());
        tx.commit();

        assertEquals(10, ref.atomicGet());
        assertEquals(version, stm.getVersion());
        assertSame(committed, ref.___load());
        assertNull(ref.___getLockOwner());
    }

    @Test
    public void whenNotCommittedBefore() {
        AlphaProgrammaticLong ref = AlphaProgrammaticLong.createUncommitted(stm);

        AlphaTransaction tx = stm.getTransactionFactoryBuilder()
                .setSpeculativeConfigurationEnabled(false)
                .setReadonly(false)
                .build()
                .start();

        long version = stm.getVersion();
        try {
            ref.set(tx, 10);
            fail();
        } catch (UncommittedReadConflict expected) {
        }

        assertEquals(version, stm.getVersion());
        assertNull(ref.___load());
        assertNull(ref.___getLockOwner());
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
            ref.set(tx, 1);
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
            ref.set(tx, 1);
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
            ref.set(tx, 1);
            fail();
        } catch (PreparedTransactionException expected) {
        }

        assertIsPrepared(tx);
        assertEquals(version, stm.getVersion());
        assertEquals(10, ref.get());
    }

}
