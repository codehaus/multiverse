package org.multiverse.stms.beta;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.ExponentialBackoffPolicy;
import org.multiverse.api.PessimisticLockLevel;
import org.multiverse.api.PropagationLevel;
import org.multiverse.api.TraceLevel;
import org.multiverse.stms.beta.transactions.AbstractFatBetaTransaction;
import org.multiverse.stms.beta.transactions.AbstractLeanBetaTransaction;
import org.multiverse.stms.beta.transactions.BetaTransaction;
import org.multiverse.stms.beta.transactions.BetaTransactionConfiguration;

import static org.junit.Assert.*;

public class BetaStm_transactionFactoryBuilderTest {
    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
    }

    @Test
    public void whenDefaultTransactionFactory() {
        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm);
        config.validate();

        assertFalse(config.isInterruptible());
        assertFalse(config.isReadonly());
        assertFalse(config.lockReads);
        assertFalse(config.lockWrites);
        assertEquals(PessimisticLockLevel.None, config.getPessimisticLockLevel());
        assertTrue(config.dirtyCheck);
        assertSame(stm, config.getStm());
        assertSame(stm.getGlobalConflictCounter(), config.getGlobalConflictCounter());
        assertTrue(config.trackReads);
        assertTrue(config.blockingAllowed);
        assertEquals(1000, config.maxRetries);
        assertTrue(config.isSpeculativeConfigEnabled());
        assertTrue(config.isAnonymous);
        assertSame(ExponentialBackoffPolicy.INSTANCE_100_MS_MAX, config.getBackoffPolicy());
        assertEquals(Long.MAX_VALUE, config.getTimeoutNs());
        assertSame(TraceLevel.None, config.getTraceLevel());
        assertTrue(config.writeSkewAllowed);
        assertEquals(PropagationLevel.Requires, config.getPropagationLevel());
    }

    @Test
    public void whenReadtrackingDisabled() {
        BetaTransactionFactory txFactory = stm.getTransactionFactoryBuilder()
                .setReadTrackingEnabled(false)
                .setBlockingAllowed(false)
                .build();

        assertFalse(txFactory.getTransactionConfiguration().isReadTrackingEnabled());
    }

    @Test
    public void whenSpeculativeConfigEnabled() {
        BetaTransactionFactory txFactory = stm.getTransactionFactoryBuilder()
                .setSpeculativeConfigEnabled(true)
                .build();


        BetaTransactionConfiguration configuration = txFactory.getTransactionConfiguration();
        assertFalse(configuration.getSpeculativeConfig().isFat());
        assertTrue(configuration.isSpeculativeConfigEnabled());
    }

    @Test
    public void whenWriteSkewNotAllowed() {
        BetaTransactionFactory txFactory = stm.getTransactionFactoryBuilder()
                .setSpeculativeConfigEnabled(true)
                .setWriteSkewAllowed(false)
                .build();


        BetaTransactionConfiguration configuration = txFactory.getTransactionConfiguration();
        assertTrue(configuration.getSpeculativeConfig().isFat());
        assertTrue(configuration.isSpeculativeConfigEnabled());
    }
    
    @Test
    public void whenWriteSkewNotAllowedThenFatTransaction(){
        BetaTransactionFactory txFactory = stm.getTransactionFactoryBuilder()
                .setSpeculativeConfigEnabled(true)
                .setWriteSkewAllowed(false)
                .build();

        BetaTransaction tx = txFactory.start();
        assertTrue(tx instanceof AbstractFatBetaTransaction);
    }

    @Test
    public void whenWriteSkewAllowedThenFatTransaction(){
        BetaTransactionFactory txFactory = stm.getTransactionFactoryBuilder()
                .setSpeculativeConfigEnabled(true)
                .setWriteSkewAllowed(true)
                .build();

        BetaTransaction tx = txFactory.start();
        assertTrue(tx instanceof AbstractLeanBetaTransaction);
    }
}
