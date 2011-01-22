package org.multiverse.stms.gamma.integration.isolation;

import org.junit.Test;
import org.multiverse.api.AtomicBlock;
import org.multiverse.stms.gamma.LeanGammaAtomicBlock;
import org.multiverse.stms.gamma.transactions.fat.FatVariableLengthGammaTransactionFactory;

public class ReadConsistency_FatVariableLengthGammaTransaction_StressTest extends ReadConsistency_AbstractTest {

    private int refCount;

    @Test
    public void testWith2Refs() {
        refCount = 2;
        test(refCount);
    }

    @Test
    public void testWith4Refs() {
        refCount = 4;
        test(refCount);
    }

    @Test
    public void testWith8Refs() {
        refCount = 8;
        test(refCount);
    }

    @Test
    public void testWith16Refs() {
        refCount = 16;
        test(refCount);
    }

    @Test
    public void testWith32Refs() {
        refCount = 32;
        test(refCount);
    }

    @Test
    public void testWith128Refs() {
        refCount = 128;
        test(refCount);
    }

    @Test
    public void testWith512Refs() {
        refCount = 512;
        test(refCount);
    }

    @Test
    public void testWith2048Refs() {
        refCount = 2048;
        test(refCount);
    }

    @Override
    protected AtomicBlock createReadBlock() {
        return new LeanGammaAtomicBlock(new FatVariableLengthGammaTransactionFactory(stm));
    }

    @Override
    protected AtomicBlock createWriteBlock() {
        return new LeanGammaAtomicBlock(new FatVariableLengthGammaTransactionFactory(stm));
    }
}
