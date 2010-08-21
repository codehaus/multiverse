package org.multiverse.stms.alpha;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.PropagationLevel;
import org.multiverse.api.Stm;
import org.multiverse.api.Transaction;
import org.multiverse.api.TransactionFactory;
import org.multiverse.api.exceptions.NoTransactionAllowedException;
import org.multiverse.api.exceptions.NoTransactionFoundException;
import org.multiverse.templates.TransactionTemplate;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.multiverse.TestUtils.assertIsActive;
import static org.multiverse.TestUtils.assertIsCommitted;
import static org.multiverse.TestUtils.assertIsNew;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.*;

public class TransactionTemplate_propagationTest {
    private Stm stm;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
        clearThreadLocalTransaction();
    }

    @Test
    public void whenNeverAndTransactionAvailable_thenNoTransactionAllowedException() {
        Transaction otherTx = stm.getTransactionFactoryBuilder().build().start();
        setThreadLocalTransaction(otherTx);

        TransactionFactory transactionFactory = stm.getTransactionFactoryBuilder()
                .setPropagationLevel(PropagationLevel.Never)
                .build();

        final Runnable task = mock(Runnable.class);
        try {
            new TransactionTemplate(transactionFactory) {
                @Override
                public Object execute(Transaction tx) throws Exception {
                    task.run();
                    return null;
                }
            }.execute();
            fail();
        } catch (NoTransactionAllowedException expected) {

        }


        verifyZeroInteractions(task);
        assertIsActive(otherTx);
        assertSame(otherTx, getThreadLocalTransaction());
    }

    @Test
    public void whenNeverAndNoTransactionAvailable() {
        TransactionFactory transactionFactory = stm.getTransactionFactoryBuilder()
                .setPropagationLevel(PropagationLevel.Never)
                .build();

        final Runnable task = mock(Runnable.class);
        new TransactionTemplate(transactionFactory) {
            @Override
            public Object execute(Transaction tx) throws Exception {
                task.run();
                return null;
            }
        }.execute();

        verify(task).run();
        verifyZeroInteractions(task);
        assertNull(getThreadLocalTransaction());
    }

    @Test
    public void whenMandatoryAndNoTransactionAvailable_thenNoTransactionFoundException() {

        TransactionFactory transactionFactory = stm.getTransactionFactoryBuilder()
                .setPropagationLevel(PropagationLevel.Mandatory)
                .build();

        final Runnable task = mock(Runnable.class);
        try {
            new TransactionTemplate(transactionFactory) {
                @Override
                public Object execute(Transaction tx) throws Exception {
                    task.run();
                    return null;
                }
            }.execute();
            fail();
        } catch (NoTransactionFoundException expected) {
        }

        verifyZeroInteractions(task);
        assertNull(getThreadLocalTransaction());

    }

    @Test
    public void whenMandatoryAndTransactionAvailable_thenExistingTransactionUsed() {
        final Transaction otherTx = stm.getTransactionFactoryBuilder().build().start();
        setThreadLocalTransaction(otherTx);

        TransactionFactory transactionFactory = stm.getTransactionFactoryBuilder()
                .setPropagationLevel(PropagationLevel.Mandatory)
                .build();

        final Runnable task = mock(Runnable.class);

        new TransactionTemplate(transactionFactory) {
            @Override
            public Object execute(Transaction tx) throws Exception {
                assertSame(otherTx, tx);
                task.run();
                return null;
            }
        }.execute();


        verify(task).run();
        assertSame(otherTx, getThreadLocalTransaction());
        assertIsActive(otherTx);
    }

    @Test
    public void whenRequiresAndNoTransactionAvailable_thenNewTransactionUsed() {
        TransactionFactory transactionFactory = stm.getTransactionFactoryBuilder()
                .setPropagationLevel(PropagationLevel.Requires)
                .build();

        final Runnable task = mock(Runnable.class);
        new TransactionTemplate(transactionFactory) {
            @Override
            public Object execute(Transaction tx) throws Exception {
                assertNotNull(tx);
                assertIsNew(tx);
                task.run();
                return null;
            }
        }.execute();


        verify(task).run();
        assertIsCommitted(getThreadLocalTransaction());
    }

    @Test
    public void whenRequiresAndTransactionAvailable_thenExistingTransactionUsed() {
            final Transaction otherTx = stm.getTransactionFactoryBuilder().build().start();
        setThreadLocalTransaction(otherTx);

        TransactionFactory transactionFactory = stm.getTransactionFactoryBuilder()
                .setPropagationLevel(PropagationLevel.Requires)
                .build();

        final Runnable task = mock(Runnable.class);
        new TransactionTemplate(transactionFactory) {
            @Override
            public Object execute(Transaction tx) throws Exception {
                assertSame(otherTx, tx);
                task.run();
                return null;
            }
        }.execute();


        verify(task).run();
        assertSame(otherTx, getThreadLocalTransaction());
        assertIsActive(otherTx);
    }

    @Test
    public void whenRequiresNewAndNoTransactionAvailable_thenNewTransactionCreated() {
         TransactionFactory transactionFactory = stm.getTransactionFactoryBuilder()
                .setPropagationLevel(PropagationLevel.RequiresNew)
                .build();

        final Runnable task = mock(Runnable.class);
        new TransactionTemplate(transactionFactory) {
            @Override
            public Object execute(Transaction tx) throws Exception {
                assertNotNull(tx);
                assertIsNew(tx);
                task.run();
                return null;
            }
        }.execute();


        verify(task).run();
        assertNull(getThreadLocalTransaction());
    }

    @Test
    public void whenRequiresNewAndTransactionAvailable_thenExistingTransactionSuspended() {
         final Transaction otherTx = stm.getTransactionFactoryBuilder().build().start();
        setThreadLocalTransaction(otherTx);

        TransactionFactory transactionFactory = stm.getTransactionFactoryBuilder()
                .setPropagationLevel(PropagationLevel.RequiresNew)
                .build();

        final Runnable task = mock(Runnable.class);

        new TransactionTemplate(transactionFactory) {
            @Override
            public Object execute(Transaction tx) throws Exception {
                assertNotSame(otherTx, tx);
                task.run();
                return null;
            }
        }.execute();


        verify(task).run();
        assertSame(otherTx, getThreadLocalTransaction());
        assertIsActive(otherTx);        
    }

    @Test
    public void whenSupportsAndTransactionAvailable() {
          final Transaction otherTx = stm.getTransactionFactoryBuilder().build().start();
        setThreadLocalTransaction(otherTx);

        TransactionFactory transactionFactory = stm.getTransactionFactoryBuilder()
                .setPropagationLevel(PropagationLevel.Supports)
                .build();

        final Runnable task = mock(Runnable.class);

        new TransactionTemplate(transactionFactory) {
            @Override
            public Object execute(Transaction tx) throws Exception {
                assertSame(otherTx, tx);
                task.run();
                return null;
            }
        }.execute();


        verify(task).run();
        assertSame(otherTx, getThreadLocalTransaction());
        assertIsActive(otherTx);
    }

    @Test
    public void whenSupportsAndNoTransactionAvailable() {
        TransactionFactory transactionFactory = stm.getTransactionFactoryBuilder()
                .setPropagationLevel(PropagationLevel.Supports)
                .build();

        final Runnable task = mock(Runnable.class);

        new TransactionTemplate(transactionFactory) {
            @Override
            public Object execute(Transaction tx) throws Exception {
                assertNull(tx);
                task.run();
                return null;
            }
        }.execute();


        verify(task).run();
        assertNull(getThreadLocalTransaction());
    }
}
