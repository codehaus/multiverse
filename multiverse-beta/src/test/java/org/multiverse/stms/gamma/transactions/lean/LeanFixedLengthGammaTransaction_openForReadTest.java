package org.multiverse.stms.gamma.transactions.lean;

import org.multiverse.stms.gamma.transactionalobjects.GammaLongRef;
import org.multiverse.stms.gamma.transactionalobjects.GammaRefTranlocal;
import org.multiverse.stms.gamma.transactions.GammaTransactionConfiguration;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class LeanFixedLengthGammaTransaction_openForReadTest extends LeanGammaTransaction_openForReadTest<LeanFixedLengthGammaTransaction> {

    @Override
    public LeanFixedLengthGammaTransaction newTransaction() {
        return new LeanFixedLengthGammaTransaction(stm);
    }

    @Override
    public int getMaximumLength() {
        return new GammaTransactionConfiguration(stm).maxFixedLengthTransactionSize;
    }

}
