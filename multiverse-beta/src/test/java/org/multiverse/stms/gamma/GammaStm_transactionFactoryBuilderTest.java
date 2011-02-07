package org.multiverse.stms.gamma;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.api.*;
import org.multiverse.api.exceptions.IllegalTransactionFactoryException;
import org.multiverse.api.lifecycle.TransactionListener;
import org.multiverse.stms.gamma.transactions.GammaTransaction;
import org.multiverse.stms.gamma.transactions.GammaTransactionConfiguration;
import org.multiverse.stms.gamma.transactions.GammaTransactionFactory;
import org.multiverse.stms.gamma.transactions.GammaTransactionFactoryBuilder;
import org.multiverse.stms.gamma.transactions.fat.FatVariableLengthGammaTransaction;

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
        assertTrue(config.isSpeculative());
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
        TransactionListener listener = mock(TransactionListener.class);
        GammaTransactionFactoryBuilder newBuilder = oldBuilder.addPermanentListener(listener);

        assertEquals(asList(listener), newBuilder.getConfiguration().getPermanentListeners());
        assertTrue(oldBuilder.getConfiguration().getPermanentListeners().isEmpty());
    }

    @Test
    public void whenPermanentListenerAdded_thenNoCheckForDuplicates() {
        GammaTransactionFactoryBuilder oldBuilder = stm.newTransactionFactoryBuilder();
        TransactionListener listener = mock(TransactionListener.class);
        GammaTransactionFactoryBuilder newBuilder = oldBuilder.addPermanentListener(listener)
                .addPermanentListener(listener);

        assertEquals(asList(listener, listener), newBuilder.getConfiguration().getPermanentListeners());
    }

    @Test
    public void whenNoPermanentListenersAdded_thenEmptyList() {
        GammaTransactionFactoryBuilder builder = stm.newTransactionFactoryBuilder();
        assertTrue(builder.getConfiguration().getPermanentListeners().isEmpty());
    }

    @Test
    public void whenMultipleListenersAdded_thenTheyAreAddedInOrder() {
        TransactionListener listener1 = mock(TransactionListener.class);
        TransactionListener listener2 = mock(TransactionListener.class);
        GammaTransactionFactoryBuilder builder = stm.newTransactionFactoryBuilder()
                .addPermanentListener(listener1)
                .addPermanentListener(listener2);

        List<TransactionListener> listeners = builder.getConfiguration().getPermanentListeners();
        assertEquals(asList(listener1, listener2), listeners);
    }

    @Test
    public void whenGetPermanentListenersCalled_immutableListReturned() {
        GammaTransactionFactoryBuilder builder = stm.newTransactionFactoryBuilder()
                .addPermanentListener(mock(TransactionListener.class))
                .addPermanentListener(mock(TransactionListener.class));

        List<TransactionListener> listeners = builder.getConfiguration().getPermanentListeners();

        try {
            listeners.clear();
            fail();
        } catch (UnsupportedOperationException e) {
        }
    }

    @Test
    public void whenReadLockModeOverridesWriteLockMode() {
        whenReadLockModeOverridesWriteLockMode(LockMode.None, LockMode.None);
        whenReadLockModeOverridesWriteLockMode(LockMode.None, LockMode.Read);
        whenReadLockModeOverridesWriteLockMode(LockMode.None, LockMode.Write);
        whenReadLockModeOverridesWriteLockMode(LockMode.None, LockMode.Exclusive);

        whenReadLockModeOverridesWriteLockMode(LockMode.Read, LockMode.None);
        whenReadLockModeOverridesWriteLockMode(LockMode.Read, LockMode.Read);
        whenReadLockModeOverridesWriteLockMode(LockMode.Read, LockMode.Write);
        whenReadLockModeOverridesWriteLockMode(LockMode.Read, LockMode.Exclusive);

        whenReadLockModeOverridesWriteLockMode(LockMode.Write, LockMode.None);
        whenReadLockModeOverridesWriteLockMode(LockMode.Write, LockMode.Read);
        whenReadLockModeOverridesWriteLockMode(LockMode.Write, LockMode.Write);
        whenReadLockModeOverridesWriteLockMode(LockMode.Write, LockMode.Exclusive);

        whenReadLockModeOverridesWriteLockMode(LockMode.Exclusive, LockMode.None);
        whenReadLockModeOverridesWriteLockMode(LockMode.Exclusive, LockMode.Read);
        whenReadLockModeOverridesWriteLockMode(LockMode.Exclusive, LockMode.Write);
        whenReadLockModeOverridesWriteLockMode(LockMode.Exclusive, LockMode.Exclusive);
    }

    public void whenReadLockModeOverridesWriteLockMode(LockMode readLockMode, LockMode writeLockMode) {
        GammaTransactionFactory txFactory = stm.newTransactionFactoryBuilder()
                .setWriteLockMode(writeLockMode)
                .setReadLockMode(readLockMode)
                .newTransactionFactory();

        assertEquals(readLockMode, txFactory.getConfiguration().readLockMode);
        assertEquals(readLockMode.asInt(), txFactory.getConfiguration().readLockModeAsInt);

        if (readLockMode.asInt() > writeLockMode.asInt()) {
            assertEquals(readLockMode, txFactory.getConfiguration().getWriteLockMode());
            assertEquals(readLockMode.asInt(), txFactory.getConfiguration().writeLockModeAsInt);
        } else {
            assertEquals(writeLockMode, txFactory.getConfiguration().getWriteLockMode());
            assertEquals(writeLockMode.asInt(), txFactory.getConfiguration().writeLockModeAsInt);
        }
    }

    @Test
    public void whenWriteLockModeOverridesReadLockMode() {
        whenWriteLockModeOverridesReadLockMode(LockMode.None, LockMode.None, true);
        whenWriteLockModeOverridesReadLockMode(LockMode.None, LockMode.Read, true);
        whenWriteLockModeOverridesReadLockMode(LockMode.None, LockMode.Write, true);
        whenWriteLockModeOverridesReadLockMode(LockMode.None, LockMode.Exclusive, true);

        whenWriteLockModeOverridesReadLockMode(LockMode.Read, LockMode.None, false);
        whenWriteLockModeOverridesReadLockMode(LockMode.Read, LockMode.Read, true);
        whenWriteLockModeOverridesReadLockMode(LockMode.Read, LockMode.Write, true);
        whenWriteLockModeOverridesReadLockMode(LockMode.Read, LockMode.Exclusive, true);

        whenWriteLockModeOverridesReadLockMode(LockMode.Write, LockMode.None, false);
        whenWriteLockModeOverridesReadLockMode(LockMode.Write, LockMode.Read, false);
        whenWriteLockModeOverridesReadLockMode(LockMode.Write, LockMode.Write, true);
        whenWriteLockModeOverridesReadLockMode(LockMode.Write, LockMode.Exclusive, true);

        whenWriteLockModeOverridesReadLockMode(LockMode.Exclusive, LockMode.None, false);
        whenWriteLockModeOverridesReadLockMode(LockMode.Exclusive, LockMode.Read, false);
        whenWriteLockModeOverridesReadLockMode(LockMode.Exclusive, LockMode.Write, false);
        whenWriteLockModeOverridesReadLockMode(LockMode.Exclusive, LockMode.Exclusive, true);
    }

    public void whenWriteLockModeOverridesReadLockMode(LockMode readLock, LockMode writeLock, boolean success) {
        if (success) {
            GammaTransactionFactory txFactory = stm.newTransactionFactoryBuilder()
                    .setReadLockMode(readLock)
                    .setWriteLockMode(writeLock)
                    .newTransactionFactory();

            assertEquals(readLock, txFactory.getConfiguration().getReadLockMode());
            assertEquals(writeLock, txFactory.getConfiguration().getWriteLockMode());
        } else {
            try {
                stm.newTransactionFactoryBuilder()
                        .setReadLockMode(readLock)
                        .setWriteLockMode(writeLock)
                        .newTransactionFactory();
                fail();
            } catch (IllegalTransactionFactoryException expected) {
            }
        }
    }

    @Test
    public void whenReadtrackingDisabled() {
        GammaTransactionFactory txFactory = stm.newTransactionFactoryBuilder()
                .setReadTrackingEnabled(false)
                .setBlockingAllowed(false)
                .newTransactionFactory();

        assertFalse(txFactory.getConfiguration().isReadTrackingEnabled());
    }

    @Test
    public void whenSpeculativeConfigEnabled() {
        GammaTransactionFactory txFactory = stm.newTransactionFactoryBuilder()
                .setDirtyCheckEnabled(false)
                .setSpeculative(true)
                .newTransactionFactory();

        GammaTransactionConfiguration configuration = txFactory.getConfiguration();
        assertFalse(configuration.getSpeculativeConfiguration().fat);
        assertTrue(configuration.isSpeculative());
    }

    @Test
    public void whenWriteSkewNotAllowed() {
        GammaTransactionFactory txFactory = stm.newTransactionFactoryBuilder()
                .setSpeculative(true)
                .setIsolationLevel(IsolationLevel.Serializable)
                .newTransactionFactory();


        GammaTransactionConfiguration configuration = txFactory.getConfiguration();
        assertTrue(configuration.getSpeculativeConfiguration().fat);
        assertTrue(configuration.isSpeculative());
    }

    @Test
    @Ignore
    public void whenWriteSkewNotAllowedThenFatTransaction() {
        GammaTransactionFactory txFactory = stm.newTransactionFactoryBuilder()
                .setSpeculative(true)
                .setIsolationLevel(IsolationLevel.Serializable)
                .newTransactionFactory();

        GammaTransaction tx = txFactory.newTransaction();
        assertTrue(tx instanceof FatVariableLengthGammaTransaction);
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
