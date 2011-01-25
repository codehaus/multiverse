package org.multiverse.stms.gamma.transactions;

import org.junit.Test;
import org.multiverse.stms.gamma.GammaStm;
import org.multiverse.stms.gamma.GammaStmConfiguration;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Veentjer
 */
public class GammaTransactionConfigurationTest {

    @Test
    public void testIsRichMansConflictScanRequired() {
        GammaStmConfiguration stmConfig = new GammaStmConfiguration();
        stmConfig.maximumPoorMansConflictScanLength = 0;
        stmConfig.speculativeConfigEnabled = true;
        GammaStm stm = new GammaStm(stmConfig);
        GammaTransactionConfiguration txConfig = new GammaTransactionConfiguration(stm, stmConfig);
        txConfig.init();

        assertTrue(txConfig.speculativeConfiguration.get().isRichMansConflictScanRequired);
    }

    @Test
    public void testIsRichMansConflictScanRequiredIfZeroMaximumPoorMans() {
        GammaStmConfiguration stmConfig = new GammaStmConfiguration();
        stmConfig.maximumPoorMansConflictScanLength = 10;
        stmConfig.speculativeConfigEnabled = true;
        GammaStm stm = new GammaStm(stmConfig);
        GammaTransactionConfiguration txConfig = new GammaTransactionConfiguration(stm, stmConfig);
        txConfig.init();

        assertFalse(txConfig.speculativeConfiguration.get().isRichMansConflictScanRequired);
    }
}
