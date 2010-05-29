package org.multiverse.stms.alpha.programmatic;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Transaction;
import org.multiverse.api.TransactionFactory;
import org.multiverse.stms.alpha.AlphaStm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class AlphaProgrammaticLongRef_commutingIncWithTransactionTest {
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
        AlphaProgrammaticLongRef ref = new AlphaProgrammaticLongRef(stm, 1);

        long version = stm.getVersion();
        try {
            ref.commutingInc(null, 20);
            fail();
        } catch (NullPointerException expected) {
        }

        assertEquals(version, stm.getVersion());
        assertEquals(1, ref.atomicGet());
    }

    @Test
    public void commuting() {
        TransactionFactory txFactory = stm.getTransactionFactoryBuilder()
                .setSpeculativeConfigurationEnabled(false)
                .setReadonly(false)
                .build();

        AlphaProgrammaticLongRef ref = new AlphaProgrammaticLongRef(stm, 10);

        Transaction tx1 = txFactory.start();
        Transaction tx2 = txFactory.start();

        ref.commutingInc(tx1, 2);
        ref.commutingInc(tx2, 3);

        long version = stm.getVersion();
        tx1.commit();
        tx2.commit();

        assertEquals(15, ref.atomicGet());
        assertEquals(version + 2, stm.getVersion());
    }

    @Test
    public void multipleCommutingIncs() {
        TransactionFactory txFactory = stm.getTransactionFactoryBuilder()
                .setSpeculativeConfigurationEnabled(false)
                .setReadonly(false)
                .build();

        AlphaProgrammaticLongRef ref = new AlphaProgrammaticLongRef(stm, 10);

        Transaction tx1 = txFactory.start();
        Transaction tx2 = txFactory.start();

        ref.commutingInc(tx1, 1);
        ref.commutingInc(tx2, 1);
        ref.commutingInc(tx1, 1);
        tx1.commit();

        ref.commutingInc(tx2, 1);

        long version = stm.getVersion();
        tx2.commit();

        assertEquals(14, ref.atomicGet());
        assertEquals(version + 1, stm.getVersion());
    }
}
