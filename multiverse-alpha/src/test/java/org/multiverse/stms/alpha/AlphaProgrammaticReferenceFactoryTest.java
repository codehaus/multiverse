package org.multiverse.stms.alpha;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Transaction;
import org.multiverse.api.TransactionFactory;
import org.multiverse.api.programmatic.ProgrammaticReference;
import org.multiverse.api.programmatic.ProgrammaticReferenceFactory;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;

import static org.junit.Assert.assertNotNull;
import static org.multiverse.TestUtils.assertInstanceOf;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;

/**
 * @author Peter Veentjer
 */
public class AlphaProgrammaticReferenceFactoryTest {
    private AlphaStm stm;
    private ProgrammaticReferenceFactory refFactory;
    private TransactionFactory<AlphaTransaction> txFactory;

    @Before
    public void setUp() {
        stm = (AlphaStm) getGlobalStmInstance();
        txFactory = stm.getTransactionFactoryBuilder()
                .setSpeculativeConfigurationEnabled(false)
                .setReadonly(false)
                .build();
        refFactory = stm.getProgrammaticReferenceFactoryBuilder().build();
    }

    @Test
    public void makeSureFactoryNotNull() {
        assertNotNull(refFactory);
    }

    @Test
    public void create() {
        ProgrammaticReference ref = refFactory.create(null);
        assertInstanceOf(ref, AlphaProgrammaticReference.class);
    }

    @Test
    public void createWithTransaction() {
        Transaction tx = txFactory.start();
        ProgrammaticReference ref = refFactory.create(tx, null);
        tx.commit();

        assertInstanceOf(ref, AlphaProgrammaticReference.class);
    }
}
