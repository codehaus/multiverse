package org.multiverse.stms.gamma.transactions.fat;

import org.multiverse.stms.gamma.transactionalobjects.GammaRefTranlocal;

import static org.junit.Assert.assertNull;

public class FatArrayGammaTransaction_abortTest extends FatGammaTransaction_abortTest<FatLinkedGammaTransaction> {

    @Override
    protected void assertCleaned(FatLinkedGammaTransaction tx) {
        GammaRefTranlocal node = tx.head;
        while (node != null) {
            assertNull(node.owner);
            node = node.next;
        }
    }

    @Override
    protected FatLinkedGammaTransaction newTransaction() {
        return new FatLinkedGammaTransaction(stm);
    }
}
