package org.multiverse.stms.gamma.transactionalobjects;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.PreparedTransactionException;
import org.multiverse.api.exceptions.TransactionRequiredException;
import org.multiverse.api.references.LongRef;
import org.multiverse.stms.gamma.GammaConstants;
import org.multiverse.stms.gamma.GammaStm;
import org.multiverse.stms.gamma.GammaStmConfiguration;
import org.multiverse.stms.gamma.transactions.GammaTransaction;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.assertIsAborted;
import static org.multiverse.TestUtils.assertIsCommitted;
import static org.multiverse.api.ThreadLocalTransaction.*;
import static org.multiverse.stms.gamma.GammaTestUtils.*;

public class GammaLongRef_getAndIncrement1Test implements GammaConstants{
    
    private GammaStm stm;

       @Before
       public void setUp() {
           GammaStmConfiguration config = new GammaStmConfiguration();
           config.maxRetries = 10;
           stm = new GammaStm(config);
           clearThreadLocalTransaction();
       }

       @Test
       public void whenPreparedTransactionAvailable_thenPreparedTransactionException() {
           GammaLongRef ref = new GammaLongRef(stm, 10);
           long version = ref.getVersion();

           GammaTransaction tx = stm.startDefaultTransaction();
           tx.prepare();
           setThreadLocalTransaction(tx);

           try {
               ref.getAndIncrement(30);
               fail();
           } catch (PreparedTransactionException expected) {

           }

           assertIsAborted(tx);
           assertVersionAndValue(ref, version, 10);
       }

       @Test
       public void whenActiveTransactionAvailable() {
           LongRef ref = new GammaLongRef(stm, 10);

           GammaTransaction tx = stm.startDefaultTransaction();
           setThreadLocalTransaction(tx);
           long value = ref.getAndIncrement(20);
           tx.commit();

           assertEquals(10, value);
           assertIsCommitted(tx);
           assertEquals(30, ref.atomicGet());
           assertSame(tx, getThreadLocalTransaction());
       }

       @Test
       public void whenNoChange() {
           GammaLongRef ref = new GammaLongRef(stm, 10);
           long version = ref.getVersion();

           GammaTransaction tx = stm.startDefaultTransaction();
           setThreadLocalTransaction(tx);
           long value = ref.getAndIncrement(0);
           tx.commit();

           assertEquals(10, value);
           assertIsCommitted(tx);
           assertEquals(10, ref.atomicGet());
           assertSame(tx, getThreadLocalTransaction());
           assertVersionAndValue(ref, version, 10);
       }

       @Test
       @Ignore
       public void whenListenersAvailable() {
           /*
           long initialValue = 10;
           GammaLongRef ref = new GammaLongRef(stm, initialValue);
           long initialVersion = ref.getVersion();

           long amount = 2;
           LongRefAwaitThread thread = new LongRefAwaitThread(ref,initialValue+amount);
           thread.start();

           sleepMs(500);

           GammaTransaction tx = stm.startDefaultTransaction();
           setThreadLocalTransaction(tx);
           long result = ref.getAndIncrement(amount);
           tx.commit();

           joinAll(thread);

           assertEquals(initialValue, result);
           assertRefHasNoLocks(ref);
           assertVersionAndValue(ref, initialVersion + 1, initialValue+amount);*/
       }

       @Test
       public void whenLocked_thenReadWriteConflict() {
           /*
           long initialValue = 10;
           GammaLongRef ref = new GammaLongRef(stm, initialValue);
           long version = ref.getVersion();

           GammaTransaction otherTx = stm.startDefaultTransaction();
           otherTx.openForRead(ref, LOCKMODE_COMMIT);

           GammaTransaction tx = stm.startDefaultTransaction();
           setThreadLocalTransaction(tx);
           try {
               ref.getAndIncrement(1);
               fail();
           } catch (ReadWriteConflict expected) {
           }

           assertIsAborted(tx);
           assertVersionAndValue(ref, version, initialValue);
           assertSurplus(1, ref);
           assertRefHasCommitLock(ref, otherTx);*/
       }

       @Test
       public void whenNoTransactionAvailable_thenNoTransactionFoundException() {
           long initialValue = 10;
           GammaLongRef ref = new GammaLongRef(stm, initialValue);
           long initialVersion = ref.getVersion();

           try {
               ref.getAndIncrement(1);
               fail();
           } catch (TransactionRequiredException expected) {

           }

           assertSurplus(0, ref);
           assertRefHasNoLocks(ref);
           assertVersionAndValue(ref, initialVersion, initialValue);
           assertNull(getThreadLocalTransaction());
       }

       @Test
       public void whenCommittedTransactionAvailable_thenDeadTransactionException() {
           long initialValue = 10;
           GammaLongRef ref = new GammaLongRef(stm, initialValue);
           long initialVersion = ref.getVersion();

           GammaTransaction tx = stm.startDefaultTransaction();
           setThreadLocalTransaction(tx);
           tx.commit();

           try {
               ref.getAndIncrement(2);
               fail();
           } catch (DeadTransactionException expected) {

           }

           assertSurplus(0, ref);
           assertRefHasNoLocks(ref);
           assertVersionAndValue(ref, initialVersion, initialValue);
           assertSame(tx, getThreadLocalTransaction());
           assertIsCommitted(tx);
       }

       @Test
       public void whenAbortedTransactionAvailable_thenDeadTransactionException() {
           long initialValue = 10;
           GammaLongRef ref = new GammaLongRef(stm, initialValue);
           long initialVersion = ref.getVersion();

           GammaTransaction tx = stm.startDefaultTransaction();
           setThreadLocalTransaction(tx);
           tx.abort();

           try {
               ref.getAndIncrement(1);
               fail();
           } catch (DeadTransactionException expected) {
           }

           assertSurplus(0, ref);
           assertRefHasNoLocks(ref);
           assertVersionAndValue(ref, initialVersion, initialValue);
           assertSame(tx, getThreadLocalTransaction());
           assertIsAborted(tx);
       }
    
}
