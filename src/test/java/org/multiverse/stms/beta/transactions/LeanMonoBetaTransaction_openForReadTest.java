package org.multiverse.stms.beta.transactions;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.exceptions.SpeculativeConfigurationError;
import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactionalobjects.LongRef;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.multiverse.TestUtils.assertAborted;
import static org.multiverse.TestUtils.createReadBiasedLongRef;
import static org.multiverse.stms.beta.orec.OrecTestUtils.*;

public class LeanMonoBetaTransaction_openForReadTest {
    private BetaObjectPool pool;
    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
        pool = new BetaObjectPool();
    }

    @Test
    public void whenUntrackedRead_thenSpeculativeConfigError() {
        LongRef ref = createReadBiasedLongRef(stm, 100);

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .setBlockingAllowed(false)
                .setReadTrackingEnabled(false);

        LeanMonoBetaTransaction tx = new LeanMonoBetaTransaction(config);

        try {
            tx.openForRead(ref, false, pool);
            fail();
        } catch (SpeculativeConfigurationError expected) {
        }

        assertEquals(2, config.getSpeculativeConfig().getMinimalLength());
        assertAborted(tx);
        assertSurplus(1, ref);
        assertReadBiased(ref);
        assertUnlocked(ref);
    }
}
