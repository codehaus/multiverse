package org.multiverse.stms.gamma.transactions;

import org.multiverse.stms.gamma.transactionalobjects.GammaRefTranlocal;

import static org.junit.Assert.assertNull;

public class ArrayGammaTransaction_abortTest extends GammaTransaction_abortTest<ArrayGammaTransaction> {

    @Override
    protected void assertCleaned(ArrayGammaTransaction tx) {
        GammaRefTranlocal node = tx.head;
        while (node != null) {
            assertNull(node.owner);
            node = node.next;
        }
    }

    @Override
    protected ArrayGammaTransaction newTransaction() {
        return new ArrayGammaTransaction(stm);
    }
}
