package org.multiverse.stms.beta;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.AtomicBlock;
import org.multiverse.api.PropagationLevel;

import static org.junit.Assert.assertTrue;

public class BetaStm_atomicBlockTest {

    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
    }

    @Test
    public void whenCreateAtomicBlock() {
        AtomicBlock block = stm.getDefaultAtomicBlock();
        assertTrue(block instanceof LeanBetaAtomicBlock);
    }

    @Test
    public void testDefault() {
        AtomicBlock block = stm.newTransactionFactoryBuilder()
                .buildAtomicBlock();
        assertTrue(block instanceof LeanBetaAtomicBlock);
    }

    @Test
    public void whenMandatory() {
        AtomicBlock block = stm.newTransactionFactoryBuilder()
                .setPropagationLevel(PropagationLevel.Mandatory)
                .buildAtomicBlock();
        assertTrue(block instanceof FatBetaAtomicBlock);
    }

    @Test
    public void whenRequires() {
        AtomicBlock block = stm.newTransactionFactoryBuilder()
                .setPropagationLevel(PropagationLevel.Requires)
                .buildAtomicBlock();
        assertTrue(block instanceof LeanBetaAtomicBlock);
    }

    @Test
    public void whenRequiresNew() {
        AtomicBlock block = stm.newTransactionFactoryBuilder()
                .setPropagationLevel(PropagationLevel.RequiresNew)
                .buildAtomicBlock();
        assertTrue(block instanceof FatBetaAtomicBlock);
    }

    @Test
    public void whenNever() {
        AtomicBlock block = stm.newTransactionFactoryBuilder()
                .setPropagationLevel(PropagationLevel.Never)
                .buildAtomicBlock();
        assertTrue(block instanceof FatBetaAtomicBlock);
    }

    @Test
    public void whenSupports() {
        AtomicBlock block = stm.newTransactionFactoryBuilder()
                .setPropagationLevel(PropagationLevel.Supports)
                .buildAtomicBlock();
        assertTrue(block instanceof FatBetaAtomicBlock);
    }
}
