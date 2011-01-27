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

        assertEquals(asList(listener), newBuilder.getTransactionConfiguration().getPermanentListeners());
        assertTrue(oldBuilder.getTransactionConfiguration().getPermanentListeners().isEmpty());
    }

    @Test
    public void whenPermanentListenerAdded_thenNoCheckForDuplicates() {
        GammaTransactionFactoryBuilder oldBuilder = stm.newTransactionFactoryBuilder();
        TransactionListener listener = mock(TransactionListener.class);
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
        TransactionListener listener1 = mock(TransactionListener.class);
        TransactionListener listener2 = mock(TransactionListener.class);
        GammaTransactionFactoryBuilder builder = stm.newTransactionFactoryBuilder()
                .addPermanentListener(listener1)
                .addPermanentListener(listener2);

        List<TransactionListener> listeners = builder.getTransactionConfiguration().getPermanentListeners();
        assertEquals(asList(listener1, listener2), listeners);
    }

    @Test
    public void whenGetPermanentListenersCalled_immutableListReturned() {
        GammaTransactionFactoryBuilder builder = stm.newTransactionFactoryBuilder()
                .addPermanentListener(mock(TransactionListener.class))
                .addPermanentListener(mock(TransactionListener.class));

        List<TransactionListener> listeners = builder.getTransactionConfiguration().getPermanentListeners();

        try {
            listeners.clear();
            fail();
        } catch (UnsupportedOperationException e) {
        }
    }

    @Test
    public void whenReadWriteLockLevel() {
        whenReadLockWriteLockLevel(LockMode.None, LockMode.None, true);
        whenReadLockWriteLockLevel(LockMode.None, LockMode.Read, true);
        whenReadLockWriteLockLevel(LockMode.None, LockMode.Write, true);
        whenReadLockWriteLockLevel(LockMode.None, LockMode.Exclusive, true);

        whenReadLockWriteLockLevel(LockMode.Read, LockMode.None, false);
        whenReadLockWriteLockLevel(LockMode.Read, LockMode.Read, true);
        whenReadLockWriteLockLevel(LockMode.Read, LockMode.Write, true);
        whenReadLockWriteLockLevel(LockMode.Read, LockMode.Exclusive, true);

        whenReadLockWriteLockLevel(LockMode.Write, LockMode.None, false);
        whenReadLockWriteLockLevel(LockMode.Write, LockMode.Read, false);
        whenReadLockWriteLockLevel(LockMode.Write, LockMode.Write, true);
        whenReadLockWriteLockLevel(LockMode.Write, LockMode.Exclusive, true);

        whenReadLockWriteLockLevel(LockMode.Exclusive, LockMode.None, false);
        whenReadLockWriteLockLevel(LockMode.Exclusive, LockMode.Read, false);
        whenReadLockWriteLockLevel(LockMode.Exclusive, LockMode.Write, false);
        whenReadLockWriteLockLevel(LockMode.Exclusive, LockMode.Exclusive, true);
    }

    public void whenReadLockWriteLockLevel(LockMode readLock, LockMode writeLock, boolean success) {
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
                .setSpeculativeConfigurationEnabled(true)
                .newTransactionFactory();

        GammaTransactionConfiguration configuration = txFactory.getConfiguration();
        assertFalse(configuration.getSpeculativeConfiguration().isFat);
        assertTrue(configuration.isSpeculative());
    }

    @Test
    public void whenWriteSkewNotAllowed() {
        GammaTransactionFactory txFactory = stm.newTransactionFactoryBuilder()
                .setSpeculativeConfigurationEnabled(true)
                .setIsolationLevel(IsolationLevel.Serializable)
                .newTransactionFactory();


        GammaTransactionConfiguration configuration = txFactory.getConfiguration();
        assertTrue(configuration.getSpeculativeConfiguration().isFat);
        assertTrue(configuration.isSpeculative());
    }

    @Test
    @Ignore
    public void whenWriteSkewNotAllowedThenFatTransaction() {
        GammaTransactionFactory txFactory = stm.newTransactionFactoryBuilder()
                .setSpeculativeConfigurationEnabled(true)
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
