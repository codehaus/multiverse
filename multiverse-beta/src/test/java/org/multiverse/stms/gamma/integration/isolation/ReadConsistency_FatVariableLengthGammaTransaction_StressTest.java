package org.multiverse.stms.gamma.integration.isolation;

import org.junit.Test;
import org.multiverse.api.AtomicBlock;
import org.multiverse.stms.gamma.LeanGammaAtomicBlock;
import org.multiverse.stms.gamma.transactions.GammaTransactionConfiguration;
import org.multiverse.stms.gamma.transactions.fat.FatVariableLengthGammaTransactionFactory;

public class ReadConsistency_FatVariableLengthGammaTransaction_StressTest extends ReadConsistency_AbstractTest {

    private int refCount;
    private boolean poorMansReadConsistency;

    @Test
    public void poorMansConflictScan_testWith2Refs() {
        refCount = 2;
        poorMansReadConsistency = true;
        run(refCount);
    }

    @Test
    public void poorMansConflictScan_testWith4Refs() {
        refCount = 4;
        poorMansReadConsistency = true;
        run(refCount);
    }

    @Test
    public void poorMansConflictScan_testWith8Refs() {
        refCount = 8;
        poorMansReadConsistency = true;
        run(refCount);
    }

    @Test
    public void poorMansConflictScan_testWith16Refs() {
        refCount = 16;
        poorMansReadConsistency = true;
        run(refCount);
    }

    @Test
    public void poorMansConflictScan_testWith32Refs() {
        refCount = 32;
        poorMansReadConsistency = true;
        run(refCount);
    }

    @Test
    public void poorMansConflictScan_testWith128Refs() {
        refCount = 128;
        poorMansReadConsistency = true;
        run(refCount);
    }

    @Test
    public void poorMansConflictScan_testWith512Refs() {
        refCount = 512;
        poorMansReadConsistency = true;
        run(refCount);
    }

    @Test
    public void poorMansConflictScan_testWith2048Refs() {
        poorMansReadConsistency = true;
        refCount = 2048;
        run(refCount);
    }

    @Test
    public void richMansConflictScan_testWith2Refs() {
        refCount = 2;
        poorMansReadConsistency = false;
        run(refCount);
    }

    @Test
    public void richMansConflictScan_testWith4Refs() {
        refCount = 4;
        poorMansReadConsistency = false;
        run(refCount);
    }

    @Test
    public void richMansConflictScan_testWith8Refs() {
        refCount = 8;
        poorMansReadConsistency = false;
        run(refCount);
    }

    @Test
    public void richMansConflictScan_testWith16Refs() {
        refCount = 16;
        poorMansReadConsistency = false;
        run(refCount);
    }

    @Test
    public void richMansConflictScan_testWith32Refs() {
        refCount = 32;
        poorMansReadConsistency = false;
        run(refCount);
    }

    @Test
    public void richMansConflictScan_testWith128Refs() {
        refCount = 128;
        poorMansReadConsistency = false;
        run(refCount);
    }

    @Test
    public void richMansConflictScan_testWith512Refs() {
        refCount = 512;
        poorMansReadConsistency = false;
        run(refCount);
    }

    @Test
    public void richMansConflictScan_testWith2048Refs() {
        poorMansReadConsistency = false;
        refCount = 2048;
        run(refCount);
    }

    @Override
    protected AtomicBlock createReadBlock() {
        GammaTransactionConfiguration config = new GammaTransactionConfiguration(stm)
                .setMaximumPoorMansConflictScanLength(poorMansReadConsistency ? Integer.MAX_VALUE : 0);
        return new LeanGammaAtomicBlock(new FatVariableLengthGammaTransactionFactory(config));
    }

    @Override
    protected AtomicBlock createWriteBlock() {
        GammaTransactionConfiguration config = new GammaTransactionConfiguration(stm)
                .setMaximumPoorMansConflictScanLength(poorMansReadConsistency ? Integer.MAX_VALUE : 0);
        return new LeanGammaAtomicBlock(new FatVariableLengthGammaTransactionFactory(config));
    }
}
