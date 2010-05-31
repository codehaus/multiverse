package org.multiverse.templates;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Stm;
import org.multiverse.api.TransactionFactory;
import org.multiverse.api.TransactionFactoryBuilder;
import org.multiverse.api.lifecycle.TransactionLifecycleListener;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class TransactionBoilerplate_constructionTest {

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
    }

    @Test(expected = NullPointerException.class)
    public void whenNullStm_thenNullPointerException() {
        new TransactionBoilerplate((Stm) null);
    }

    @Test
    public void whenStm() {
        Stm stm = mock(Stm.class);
        TransactionFactoryBuilder txFactoryBuilder = mock(TransactionFactoryBuilder.class);
        TransactionFactory txFactory = mock(TransactionFactory.class);

        when(stm.getTransactionFactoryBuilder()).thenReturn(txFactoryBuilder);
        when(txFactoryBuilder.build()).thenReturn(txFactory);

        TransactionBoilerplate boilerplate = new TransactionBoilerplate(stm);

        assertSame(txFactory, boilerplate.getTransactionFactory());
        assertTrue(boilerplate.isThreadLocalAware());
        assertNull(boilerplate.getTransactionLifecycleListener());
    }

    @Test(expected = NullPointerException.class)
    public void whenNullTransactionFactory_thenNullPointerException() {
        new TransactionBoilerplate((TransactionFactory) null);
    }

    @Test
    public void whenTxFactoryUsed() {
        TransactionFactory txFactory = mock(TransactionFactory.class);

        TransactionBoilerplate boilerplate = new TransactionBoilerplate(txFactory);

        assertSame(txFactory, boilerplate.getTransactionFactory());
        assertNull(boilerplate.getTransactionLifecycleListener());
        assertTrue(boilerplate.isThreadLocalAware());
    }

    @Test(expected = NullPointerException.class)
    public void whenNullTransactionFactoryAndOtherArguments_thenNullPointerException() {
        new TransactionBoilerplate((TransactionFactory) null, mock(TransactionLifecycleListener.class), true);
    }

    @Test
    public void whenFullConstructor() {
        TransactionFactory txFactory = mock(TransactionFactory.class);
        TransactionLifecycleListener listener = mock(TransactionLifecycleListener.class);

        TransactionBoilerplate boilerplate = new TransactionBoilerplate(txFactory,listener, false);

        assertSame(txFactory, boilerplate.getTransactionFactory());
        assertSame(listener, boilerplate.getTransactionLifecycleListener());
        assertFalse(boilerplate.isThreadLocalAware());
    }
}
