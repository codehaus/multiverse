package org.multiverse.stms.gamma;

import org.multiverse.api.AtomicBlock;
import org.multiverse.stms.gamma.transactions.GammaTransactionFactory;

public interface GammaAtomicBlock extends AtomicBlock {

    @Override
    GammaTransactionFactory getTransactionFactory();
}
