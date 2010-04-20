package org.multiverse.stms.alpha.programmatic;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Stm;
import org.multiverse.api.Transaction;
import org.multiverse.api.TransactionFactory;
import org.multiverse.api.programmatic.ProgrammaticLong;
import org.multiverse.api.programmatic.ProgrammaticReferenceFactory;

import static junit.framework.Assert.assertEquals;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class AlphaProgrammaticLong_atomicBehaviorTest {
    private Stm stm;
    private ProgrammaticReferenceFactory refFactory;
    private TransactionFactory txFactory;
    private ProgrammaticLong[] refs;
    private int refCount = 10000;

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
        stm = getGlobalStmInstance();
        refFactory = stm.getProgrammaticReferenceFactoryBuilder()
                .build();
        txFactory = stm.getTransactionFactoryBuilder()
                .setReadonly(false)
                .setSpeculativeConfigurationEnabled(false)
                .build();

        refs = new ProgrammaticLong[refCount];
        for (int k = 0; k < refCount; k++) {
            refs[k] = refFactory.atomicCreateLong(0);
        }
    }

    @After
    public void tearDown() {
        clearThreadLocalTransaction();
    }

    @Test
    public void testAllCommit() {
        Transaction tx = txFactory.start();

        for (ProgrammaticLong ref : refs) {
            ref.inc(tx, 1);
        }

        tx.commit();

        for (ProgrammaticLong ref : refs) {
            assertEquals(1, ref.get());
        }
    }

    @Test
    public void testNoneCommit() {
        Transaction tx = txFactory.start();

        for (ProgrammaticLong ref : refs) {
            ref.inc(tx, 1);
        }

        tx.abort();

        for (ProgrammaticLong ref : refs) {
            assertEquals(0, ref.get());
        }
    }
}
