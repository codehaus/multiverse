package org.multiverse.stms.beta;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.ExponentialBackoffPolicy;
import org.multiverse.api.PessimisticLockLevel;
import org.multiverse.api.PropagationLevel;
import org.multiverse.api.TraceLevel;
import org.multiverse.api.lifecycle.TransactionLifecycleListener;
import org.multiverse.stms.beta.transactions.AbstractFatBetaTransaction;
import org.multiverse.stms.beta.transactions.AbstractLeanBetaTransaction;
import org.multiverse.stms.beta.transactions.BetaTransaction;
import org.multiverse.stms.beta.transactions.BetaTransactionConfiguration;

import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

public class BetaStm_transactionFactoryBuilderTest {
    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
    }

    @Test
    public void whenDefaultTransactionFactory() {
        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm);
        config.init();

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
        assertSame(ExponentialBackoffPolicy.MAX_100_MS, config.getBackoffPolicy());
        assertEquals(Long.MAX_VALUE, config.getTimeoutNs());
        assertSame(TraceLevel.None, config.getTraceLevel());
        assertTrue(config.writeSkewAllowed);
        assertEquals(PropagationLevel.Requires, config.getPropagationLevel());
        assertTrue(config.getPermanentListeners().isEmpty());
    }

    @Test(expected = NullPointerException.class)
    public void whenNullPermanentListener_thenNullPointerException() {
        stm.createTransactionFactoryBuilder().addPermanentListener(null);
    }

    @Test
    public void whenPermanentListenerAdded() {
        BetaTransactionFactoryBuilder oldBuilder = stm.createTransactionFactoryBuilder();
        TransactionLifecycleListener listener = mock(TransactionLifecycleListener.class);
        BetaTransactionFactoryBuilder newBuilder = oldBuilder.addPermanentListener(listener);

        assertEquals(asList(listener), newBuilder.getTransactionConfiguration().getPermanentListeners());
        assertTrue(oldBuilder.getTransactionConfiguration().getPermanentListeners().isEmpty());
    }

    @Test
    public void whenPermanentListenerAdded_thenNoCheckForDuplicates() {
        BetaTransactionFactoryBuilder oldBuilder = stm.createTransactionFactoryBuilder();
        TransactionLifecycleListener listener = mock(TransactionLifecycleListener.class);
        BetaTransactionFactoryBuilder newBuilder = oldBuilder.addPermanentListener(listener)
                .addPermanentListener(listener);

        assertEquals(asList(listener, listener), newBuilder.getTransactionConfiguration().getPermanentListeners());
    }

    @Test
    public void whenNoPermanentListenersAdded_thenEmptyList() {
        BetaTransactionFactoryBuilder builder = stm.createTransactionFactoryBuilder();
        assertTrue(builder.getTransactionConfiguration().getPermanentListeners().isEmpty());
    }

    @Test
    public void whenMultipleListenersAdded_thenTheyAreAddedInOrder() {
        TransactionLifecycleListener listener1 = mock(TransactionLifecycleListener.class);
        TransactionLifecycleListener listener2 = mock(TransactionLifecycleListener.class);
        BetaTransactionFactoryBuilder builder = stm.createTransactionFactoryBuilder()
                .addPermanentListener(listener1)
                .addPermanentListener(listener2);

        List<TransactionLifecycleListener> listeners = builder.getTransactionConfiguration().getPermanentListeners();
        assertEquals(asList(listener1, listener2), listeners);
    }

    @Test
    public void whenGetPermanentListenersCalled_immutableListReturned() {
        BetaTransactionFactoryBuilder builder = stm.createTransactionFactoryBuilder()
                .addPermanentListener(mock(TransactionLifecycleListener.class))
                .addPermanentListener(mock(TransactionLifecycleListener.class));

        List<TransactionLifecycleListener> listeners = builder.getTransactionConfiguration().getPermanentListeners();

        try {
            listeners.clear();
            fail();
        } catch (UnsupportedOperationException e) {
        }
    }


    @Test
    public void whenReadtrackingDisabled() {
        BetaTransactionFactory txFactory = stm.createTransactionFactoryBuilder()
                .setReadTrackingEnabled(false)
                .setBlockingAllowed(false)
                .build();

        assertFalse(txFactory.getTransactionConfiguration().isReadTrackingEnabled());
    }

    @Test
    public void whenSpeculativeConfigEnabled() {
        BetaTransactionFactory txFactory = stm.createTransactionFactoryBuilder()
                .setSpeculativeConfigurationEnabled(true)
                .build();

        BetaTransactionConfiguration configuration = txFactory.getTransactionConfiguration();
        assertFalse(configuration.getSpeculativeConfig().isFat());
        assertTrue(configuration.isSpeculativeConfigEnabled());
    }

    @Test
    public void whenWriteSkewNotAllowed() {
        BetaTransactionFactory txFactory = stm.createTransactionFactoryBuilder()
                .setSpeculativeConfigurationEnabled(true)
                .setWriteSkewAllowed(false)
                .build();


        BetaTransactionConfiguration configuration = txFactory.getTransactionConfiguration();
        assertTrue(configuration.getSpeculativeConfig().isFat());
        assertTrue(configuration.isSpeculativeConfigEnabled());
    }

    @Test
    public void whenWriteSkewNotAllowedThenFatTransaction() {
        BetaTransactionFactory txFactory = stm.createTransactionFactoryBuilder()
                .setSpeculativeConfigurationEnabled(true)
                .setWriteSkewAllowed(false)
                .build();

        BetaTransaction tx = txFactory.start();
        assertTrue(tx instanceof AbstractFatBetaTransaction);
    }

    @Test
    public void whenWriteSkewAllowedThenFatTransaction() {
        BetaTransactionFactory txFactory = stm.createTransactionFactoryBuilder()
                .setSpeculativeConfigurationEnabled(true)
                .setWriteSkewAllowed(true)
                .build();

        BetaTransaction tx = txFactory.start();
        assertTrue(tx instanceof AbstractLeanBetaTransaction);
    }
}
