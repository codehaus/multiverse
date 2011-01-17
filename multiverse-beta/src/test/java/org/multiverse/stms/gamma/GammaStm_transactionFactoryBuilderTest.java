package org.multiverse.stms.gamma;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.api.*;
import org.multiverse.api.lifecycle.TransactionLifecycleListener;
import org.multiverse.stms.gamma.transactions.*;

import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

public class GammaStm_transactionFactoryBuilderTest {
    private GammaStm stm;

    @Before
    public void setUp() {
        stm = new GammaStm();
    }

    @Test
    public void whenDefaultTransactionFactory() {
        GammaTransactionConfiguration config = new GammaTransactionConfiguration(stm);
        config.init();

        assertEquals(IsolationLevel.Snapshot, config.isolationLevel);
        assertFalse(config.isInterruptible());
        assertFalse(config.isReadonly());
        assertEquals(LockMode.None, config.readLockMode);
        assertEquals(LockMode.None, config.writeLockMode);
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
        stm.newTransactionFactoryBuilder().addPermanentListener(null);
    }

    @Test
    public void whenPermanentListenerAdded() {
        GammaTransactionFactoryBuilder oldBuilder = stm.newTransactionFactoryBuilder();
        TransactionLifecycleListener listener = mock(TransactionLifecycleListener.class);
        GammaTransactionFactoryBuilder newBuilder = oldBuilder.addPermanentListener(listener);

        assertEquals(asList(listener), newBuilder.getTransactionConfiguration().getPermanentListeners());
        assertTrue(oldBuilder.getTransactionConfiguration().getPermanentListeners().isEmpty());
    }

    @Test
    public void whenPermanentListenerAdded_thenNoCheckForDuplicates() {
        GammaTransactionFactoryBuilder oldBuilder = stm.newTransactionFactoryBuilder();
        TransactionLifecycleListener listener = mock(TransactionLifecycleListener.class);
        GammaTransactionFactoryBuilder newBuilder = oldBuilder.addPermanentListener(listener)
                .addPermanentListener(listener);

        assertEquals(asList(listener, listener), newBuilder.getTransactionConfiguration().getPermanentListeners());
    }

    @Test
    public void whenNoPermanentListenersAdded_thenEmptyList() {
        GammaTransactionFactoryBuilder builder = stm.newTransactionFactoryBuilder();
        assertTrue(builder.getTransactionConfiguration().getPermanentListeners().isEmpty());
    }

    @Test
    public void whenMultipleListenersAdded_thenTheyAreAddedInOrder() {
        TransactionLifecycleListener listener1 = mock(TransactionLifecycleListener.class);
        TransactionLifecycleListener listener2 = mock(TransactionLifecycleListener.class);
        GammaTransactionFactoryBuilder builder = stm.newTransactionFactoryBuilder()
                .addPermanentListener(listener1)
                .addPermanentListener(listener2);

        List<TransactionLifecycleListener> listeners = builder.getTransactionConfiguration().getPermanentListeners();
        assertEquals(asList(listener1, listener2), listeners);
    }

    @Test
    public void whenGetPermanentListenersCalled_immutableListReturned() {
        GammaTransactionFactoryBuilder builder = stm.newTransactionFactoryBuilder()
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
        GammaTransactionFactory txFactory = stm.newTransactionFactoryBuilder()
                .setReadTrackingEnabled(false)
                .setBlockingAllowed(false)
                .build();

        assertFalse(txFactory.getTransactionConfiguration().isReadTrackingEnabled());
    }

    @Test
    public void whenSpeculativeConfigEnabled() {
        GammaTransactionFactory txFactory = stm.newTransactionFactoryBuilder()
                .setSpeculativeConfigurationEnabled(true)
                .build();

        GammaTransactionConfiguration configuration = txFactory.getTransactionConfiguration();
        assertFalse(configuration.getSpeculativeConfiguration().isFat);
        assertTrue(configuration.isSpeculativeConfigEnabled());
    }

    @Test
    public void whenWriteSkewNotAllowed() {
        GammaTransactionFactory txFactory = stm.newTransactionFactoryBuilder()
                .setSpeculativeConfigurationEnabled(true)
                .setIsolationLevel(IsolationLevel.Serializable)
                .build();


        GammaTransactionConfiguration configuration = txFactory.getTransactionConfiguration();
        assertTrue(configuration.getSpeculativeConfiguration().isFat);
        assertTrue(configuration.isSpeculativeConfigEnabled());
    }

    @Test
    @Ignore
    public void whenWriteSkewNotAllowedThenFatTransaction() {
        GammaTransactionFactory txFactory = stm.newTransactionFactoryBuilder()
                .setSpeculativeConfigurationEnabled(true)
                .setIsolationLevel(IsolationLevel.Serializable)
                .build();

        GammaTransaction tx = txFactory.newTransaction();
        assertTrue(tx instanceof MapGammaTransaction);
    }

    @Test
    @Ignore
    public void whenWriteSkewAllowedThenFatTransaction() {
        /*
        GammaTransactionFactory txFactory = stm.createTransactionFactoryBuilder()
                .setSpeculativeConfigurationEnabled(true)
                .setIsolationLevel(IsolationLevel.Snapshot)
                .build();

        GammaTransaction tx = txFactory.newTransaction();
        assertTrue(tx instanceof AbstractLeanGammaTransaction);*/
    }
}
