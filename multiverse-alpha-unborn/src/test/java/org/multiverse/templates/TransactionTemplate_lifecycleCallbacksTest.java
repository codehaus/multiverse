package org.multiverse.templates;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.api.Transaction;
import org.multiverse.api.TransactionFactory;
import org.multiverse.stms.AbstractTransactionImpl;
import org.multiverse.stms.alpha.manualinstrumentation.IntRef;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
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

    @Ignore
    @Test
    public void testAbort() throws Exception {
        Transaction tx = new AbstractTransactionImpl();

        TransactionFactory txFactory = mock(TransactionFactory.class);
        when(txFactory.create()).thenReturn(tx);

        TransactionTemplate t = spy(new TransactionalTemplateImpl(txFactory));
        Exception expected = new Exception();
        when(t.execute(tx)).thenThrow(expected);

        try {
            t.executeChecked();
            fail();
        } catch (Exception found) {
            found.printStackTrace();
            assertSame(expected, found);
        }

        verify(t, times(1)).onInit();
        verify(t, times(1)).onPostStart(tx);
        verify(t, times(0)).onPreCommit(tx);
        verify(t, times(0)).onPostCommit();
        verify(t, times(1)).onPreAbort(tx);
        verify(t, times(1)).onPostAbort();
    }

    @Test
    @Ignore
    public void testCommit() {
        Transaction tx = new AbstractTransactionImpl();

        TransactionFactory txFactory = mock(TransactionFactory.class);
        when(txFactory.create()).thenReturn(tx);

        TransactionTemplate t = spy(new TransactionalTemplateImpl(txFactory));

        t.execute();

        //todo: needs to be activated again
        //verify(t, times(1)).onInit();
        verify(t, times(1)).onPostStart(tx);
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

    @Test
     public void testSpeculative(){
         final IntRef ref = new IntRef();

         final AtomicInteger committedCount = new AtomicInteger();
         final AtomicInteger abortedCount = new AtomicInteger();

         TransactionTemplate template = new TransactionTemplate() {
             @Override
             public Object execute(Transaction tx) throws Exception {
                 ref.inc();
                 return null;
             }

             @Override
             protected void onPostCommit() {
                 committedCount.incrementAndGet();
             }

             @Override
             public void onPostAbort(){
                 abortedCount.incrementAndGet();
             }
         };

         //first time execute. 2 aborts (from readonly to monoupdate, from mono update to array update).
         template.execute();
         assertEquals(1, ref.get());
         assertEquals(1, committedCount.get());
         assertEquals(1, abortedCount.get());

         committedCount.set(0);
         abortedCount.set(0);

         //execute again: the transactiontemplate has learned, so no unwanted aborts anymore.
         template.execute();
         assertEquals(2, ref.get());
         assertEquals(1, committedCount.get());
         assertEquals(0, abortedCount.get());
     }

     @Test
     public void test() {
         final IntRef ref1 = new IntRef();
         final IntRef ref2 = new IntRef();

         final AtomicInteger committedCount = new AtomicInteger();
         final AtomicInteger abortedCount = new AtomicInteger();

         TransactionTemplate template = new TransactionTemplate() {
             @Override
             public Object execute(Transaction tx) throws Exception {
                 ref1.inc();
                 ref2.inc();
                 return null;
             }

             @Override
             protected void onPostCommit() {
                 committedCount.incrementAndGet();
             }

             @Override
             public void onPostAbort(){
                 abortedCount.incrementAndGet();
             }
         };

         //first time execute. 2 aborts (from readonly to monoupdate, from mono update to array update).
         template.execute();
         assertEquals(1, ref1.get());
         assertEquals(1, ref2.get());
         assertEquals(1, committedCount.get());
         assertEquals(2, abortedCount.get());

         committedCount.set(0);
         abortedCount.set(0);

         //execute again: the transactiontemplate has learned, so no unwanted aborts anymore.
         template.execute();
         assertEquals(2, ref1.get());
         assertEquals(2, ref2.get());
         assertEquals(1, committedCount.get());
         assertEquals(0, abortedCount.get());
     }
    
}
