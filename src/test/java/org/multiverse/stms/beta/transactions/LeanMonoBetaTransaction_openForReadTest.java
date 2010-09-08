package org.multiverse.stms.beta.transactions;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.exceptions.SpeculativeConfigurationError;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRef;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.multiverse.TestUtils.assertIsAborted;
import static org.multiverse.TestUtils.createReadBiasedLongRef;
import static org.multiverse.stms.beta.orec.OrecTestUtils.*;

public class LeanMonoBetaTransaction_openForReadTest {
    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
    }

    @Test
    public void whenUntrackedRead_thenSpeculativeConfigError() {
        BetaLongRef ref = createReadBiasedLongRef(stm, 100);

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .setBlockingAllowed(false)
                .setReadTrackingEnabled(false)
                .init();

        LeanMonoBetaTransaction tx = new LeanMonoBetaTransaction(config);

        try {
            tx.openForRead(ref, false);
            fail();
        } catch (SpeculativeConfigurationError expected) {
        }

        assertEquals(2, config.getSpeculativeConfiguration().minimalLength);
        assertIsAborted(tx);
        assertSurplus(1, ref);
        assertReadBiased(ref);
        assertUnlocked(ref);
    }
}
