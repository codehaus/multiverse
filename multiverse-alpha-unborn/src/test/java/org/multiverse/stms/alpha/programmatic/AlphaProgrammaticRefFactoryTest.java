package org.multiverse.stms.alpha.programmatic;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Transaction;
import org.multiverse.api.TransactionFactory;
import org.multiverse.api.programmatic.ProgrammaticRef;
import org.multiverse.api.programmatic.ProgrammaticRefFactory;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;

import static org.junit.Assert.assertNotNull;
import static org.multiverse.TestUtils.assertInstanceOf;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class AlphaProgrammaticRefFactoryTest {
    private AlphaStm stm;
    private ProgrammaticRefFactory refFactory;
    private TransactionFactory<AlphaTransaction> txFactory;

    @Before
    public void setUp() {
        stm = (AlphaStm) getGlobalStmInstance();
        txFactory = stm.getTransactionFactoryBuilder()
                .setSpeculativeConfigurationEnabled(false)
                .setReadonly(false)
                .build();
        refFactory = stm.getProgrammaticRefFactoryBuilder()
                .build();
        clearThreadLocalTransaction();
    }

    @Test
    public void tearDown() {
        clearThreadLocalTransaction();
    }

    @Test
    public void makeSureFactoryNotNull() {
        assertNotNull(refFactory);
    }

    @Test
    public void create() {
        ProgrammaticRef ref = refFactory.createRef(null);
        assertInstanceOf(ref, AlphaProgrammaticRef.class);
    }

    @Test
    public void createWithTransaction() {
        Transaction tx = txFactory.start();
        ProgrammaticRef ref = refFactory.createRef(tx, null);
        tx.commit();

        assertInstanceOf(ref, AlphaProgrammaticRef.class);
    }
}
