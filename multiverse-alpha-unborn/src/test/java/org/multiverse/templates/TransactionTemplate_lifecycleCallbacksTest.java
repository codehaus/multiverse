package org.multiverse.templates;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Transaction;
import org.multiverse.api.TransactionFactory;
import org.multiverse.stms.AbstractTransactionImpl;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

public class TransactionTemplate_lifecycleCallbacksTest {

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
    }

    @After
    public void tearDown() {
        clearThreadLocalTransaction();
    }

    @Test
    public void testAbort() throws Exception {
        Transaction tx = new AbstractTransactionImpl();

        TransactionFactory txFactory = mock(TransactionFactory.class);
        when(txFactory.start()).thenReturn(tx);

        TransactionTemplate t = spy(new TransactionalTemplateImpl(txFactory));
        Exception expected = new Exception();
        when(t.execute(tx)).thenThrow(expected);

        try {
            t.executeChecked();
            fail();
        } catch (Exception found) {
            assertSame(expected, found);
        }

        verify(t, times(1)).onInit();
        verify(t, times(1)).onStart(tx);
        verify(t, times(0)).onPreCommit(tx);
        verify(t, times(0)).onPostCommit();
        verify(t, times(1)).onPreAbort(tx);
        verify(t, times(1)).onPostAbort();
    }

    @Test
    public void testCommit() {
        Transaction tx = new AbstractTransactionImpl();

        TransactionFactory txFactory = mock(TransactionFactory.class);
        when(txFactory.start()).thenReturn(tx);

        TransactionTemplate t = spy(new TransactionalTemplateImpl(txFactory));

        t.execute();

        verify(t, times(1)).onInit();
        verify(t, times(1)).onStart(tx);
        verify(t, times(1)).onPreCommit(tx);
        verify(t, times(1)).onPostCommit();
        verify(t, times(0)).onPreAbort(tx);
        verify(t, times(0)).onPostAbort();
    }

    public class TransactionalTemplateImpl extends TransactionTemplate {
        public TransactionalTemplateImpl(TransactionFactory txFactory) {
            super(txFactory);
        }

        @Override
        public Object execute(Transaction tx) throws Exception {
            return null;
        }
    }

}
