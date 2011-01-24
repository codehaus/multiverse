package org.multiverse.stms.gamma.transactionalobjects;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.exceptions.SpeculativeConfigurationError;
import org.multiverse.stms.gamma.GammaStm;
import org.multiverse.stms.gamma.transactions.fat.FatFixedLengthGammaTransaction;
import org.multiverse.stms.gamma.transactions.fat.FatMonoGammaTransaction;
import org.multiverse.stms.gamma.transactions.lean.LeanFixedLengthGammaTransaction;
import org.multiverse.stms.gamma.transactions.lean.LeanMonoGammaTransaction;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.assertIsAborted;
import static org.multiverse.TestUtils.assertIsActive;
import static org.multiverse.stms.gamma.GammaTestUtils.*;

public class GammaBooleanRef_constructionTest {

    private GammaStm stm;

    @Before
    public void setUp() {
        stm = new GammaStm();
    }

    @Test
    public void whenInitialValueUsed() {
        GammaBooleanRef ref = new GammaBooleanRef(stm, true);

        assertVersionAndValue(ref, GammaObject.VERSION_UNCOMMITTED + 1, true);
        assertRefHasNoLocks(ref);
        assertReadonlyCount(ref, 0);
        assertSurplus(ref, 0);
    }

    @Test
    public void whenDefaultValueUsed() {
        GammaBooleanRef ref = new GammaBooleanRef(stm);

        assertVersionAndValue(ref, GammaObject.VERSION_UNCOMMITTED + 1, false);
        assertRefHasNoLocks(ref);
        assertReadonlyCount(ref, 0);
        assertSurplus(ref, 0);
    }

    @Test
    public void withTransaction_whenFatMonoGammaTransactionUsed() {
        FatMonoGammaTransaction tx = new FatMonoGammaTransaction(stm);
        GammaBooleanRef ref = new GammaBooleanRef(tx, true);

        assertIsActive(tx);
        assertRefHasExclusiveLock(ref, tx);
        assertTrue(tx.hasWrites);
        assertFalse(tx.config.speculativeConfiguration.get().areConstructedObjectsRequired);
    }

    @Test
    public void withTransaction_whenFatFixedLengthGammaTransactionUsed() {
        FatFixedLengthGammaTransaction tx = new FatFixedLengthGammaTransaction(stm);
        GammaBooleanRef ref = new GammaBooleanRef(tx, true);

        assertIsActive(tx);
        assertRefHasExclusiveLock(ref, tx);
        assertTrue(tx.hasWrites);
        assertFalse(tx.config.speculativeConfiguration.get().areConstructedObjectsRequired);
    }

    @Test
    public void withTransaction_whenFatVariableLengthGammaTransactionUsed() {
        FatFixedLengthGammaTransaction tx = new FatFixedLengthGammaTransaction(stm);
        GammaBooleanRef ref = new GammaBooleanRef(tx, true);

        assertIsActive(tx);
        assertRefHasExclusiveLock(ref, tx);
        assertTrue(tx.hasWrites);
        assertFalse(tx.config.speculativeConfiguration.get().areConstructedObjectsRequired);
    }

    @Test
    public void withTransaction_whenLeanFixedLengthGammaTransactionUsed() {
        LeanFixedLengthGammaTransaction tx = new LeanFixedLengthGammaTransaction(stm);

        try {
            new GammaBooleanRef(tx, true);
            fail();
        } catch (SpeculativeConfigurationError expected) {
        }

        assertIsAborted(tx);
        assertTrue(tx.config.speculativeConfiguration.get().areConstructedObjectsRequired);
    }

    @Test
    public void withTransaction_whenLeanMonoGammaTransactionUsed() {
        LeanMonoGammaTransaction tx = new LeanMonoGammaTransaction(stm);

        try {
            new GammaBooleanRef(tx, true);
            fail();
        } catch (SpeculativeConfigurationError expected) {
        }

        assertIsAborted(tx);
        assertTrue(tx.config.speculativeConfiguration.get().areConstructedObjectsRequired);
    }
}
