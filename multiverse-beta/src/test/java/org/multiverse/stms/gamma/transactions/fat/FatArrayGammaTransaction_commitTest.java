package org.multiverse.stms.gamma.transactions.fat;

import org.multiverse.stms.gamma.transactionalobjects.GammaRefTranlocal;
import org.multiverse.stms.gamma.transactions.GammaTransactionConfiguration;

import static org.junit.Assert.assertNull;

public class FatArrayGammaTransaction_commitTest extends FatGammaTransaction_commitTest<FatLinkedGammaTransaction> {

    @Override
    protected void assertCleaned(FatLinkedGammaTransaction tx) {
        GammaRefTranlocal node = tx.head;
        while (node != null) {
            assertNull(node.owner);
            node = node.next;
        }
    }

    @Override
    protected FatLinkedGammaTransaction newTransaction(GammaTransactionConfiguration config) {
        return new FatLinkedGammaTransaction(config);
    }

    @Override
    protected FatLinkedGammaTransaction newTransaction() {
        return new FatLinkedGammaTransaction(stm);
    }
}
