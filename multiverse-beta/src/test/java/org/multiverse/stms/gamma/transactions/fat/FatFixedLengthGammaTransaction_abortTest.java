package org.multiverse.stms.gamma.transactions.fat;

import org.multiverse.stms.gamma.transactionalobjects.GammaRefTranlocal;

import static org.junit.Assert.assertNull;

public class FatFixedLengthGammaTransaction_abortTest extends FatGammaTransaction_abortTest<FatFixedLengthGammaTransaction> {

    @Override
    protected void assertCleaned(FatFixedLengthGammaTransaction tx) {
        GammaRefTranlocal node = tx.head;
        while (node != null) {
            assertNull(node.owner);
            node = node.next;
        }
    }

    @Override
    protected FatFixedLengthGammaTransaction newTransaction() {
        return new FatFixedLengthGammaTransaction(stm);
    }
}
