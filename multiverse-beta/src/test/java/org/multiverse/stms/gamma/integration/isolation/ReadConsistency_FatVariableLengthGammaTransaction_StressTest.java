package org.multiverse.stms.gamma.integration.isolation;

import org.junit.Test;
import org.multiverse.api.AtomicBlock;
import org.multiverse.stms.gamma.LeanGammaAtomicBlock;
import org.multiverse.stms.gamma.transactions.GammaTransactionConfiguration;
import org.multiverse.stms.gamma.transactions.fat.FatVariableLengthGammaTransactionFactory;

public class ReadConsistency_FatVariableLengthGammaTransaction_StressTest extends ReadConsistency_AbstractTest {

    private int refCount;

    @Test
    public void testWith2Refs() {
        refCount = 2;
        run(refCount);
    }

    @Test
    public void testWith4Refs() {
        refCount = 4;
        run(refCount);
    }

    @Test
    public void testWith8Refs() {
        refCount = 8;
        run(refCount);
    }

    @Test
    public void testWith16Refs() {
        refCount = 16;
        run(refCount);
    }

    @Test
    public void testWith32Refs() {
        refCount = 32;
        run(refCount);
    }

    @Test
    public void testWith128Refs() {
        refCount = 128;
        run(refCount);
    }

    @Test
    public void testWith512Refs() {
        refCount = 512;
        run(refCount);
    }

    @Test
    public void testWith2048Refs() {
        refCount = 2048;
        run(refCount);
    }

    @Override
    protected AtomicBlock createReadBlock() {
        GammaTransactionConfiguration config = new GammaTransactionConfiguration(stm)
                .setMaximumPoorMansConflictScanLength(refCount);
        return new LeanGammaAtomicBlock(new FatVariableLengthGammaTransactionFactory(config));
    }

    @Override
    protected AtomicBlock createWriteBlock() {
        GammaTransactionConfiguration config = new GammaTransactionConfiguration(stm)
                .setMaximumPoorMansConflictScanLength(refCount);
        return new LeanGammaAtomicBlock(new FatVariableLengthGammaTransactionFactory(config));
    }
}
