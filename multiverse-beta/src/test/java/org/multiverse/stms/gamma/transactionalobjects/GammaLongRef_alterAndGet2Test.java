package org.multiverse.stms.gamma.transactionalobjects;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.api.LockMode;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.PreparedTransactionException;
import org.multiverse.api.exceptions.ReadWriteConflict;
import org.multiverse.api.functions.Functions;
import org.multiverse.api.functions.LongFunction;
import org.multiverse.stms.gamma.GammaStm;
import org.multiverse.stms.gamma.transactions.GammaTransaction;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.*;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransaction;
import static org.multiverse.stms.gamma.GammaTestUtils.*;


public class GammaLongRef_alterAndGet2Test {
    private GammaStm stm;

      @Before
      public void setUp() {
          stm = new GammaStm();
          clearThreadLocalTransaction();
      }

      @Test
      public void whenNullTransaction_thenNullPointerException() {
          int initialValue = 10;
          GammaLongRef ref = new GammaLongRef(stm, initialValue);
          long initialVersion = ref.getVersion();

          LongFunction function = mock(LongFunction.class);

          try {
              ref.alterAndGet(null, function);
              fail();
          } catch (NullPointerException expected) {
          }

          verifyZeroInteractions(function);
          assertVersionAndValue(ref, initialVersion, initialValue);
      }

      @Test
      public void whenNullFunction_thenNullPointerException() {
          int initialValue = 10;
          GammaLongRef ref = new GammaLongRef(stm, initialValue);
          long initialVersion = ref.getVersion();

          GammaTransaction tx = stm.startDefaultTransaction();

          try {
              ref.alterAndGet(tx, null);
              fail();
          } catch (NullPointerException expected) {
          }

          assertRefHasNoLocks(ref);
          assertIsAborted(tx);
          assertVersionAndValue(ref, initialVersion, initialValue);
      }

      @Test
      public void whenCommittedTransaction_thenDeadTransactionException() {
          int initialValue = 10;
          GammaLongRef ref = new GammaLongRef(stm, initialValue);
          long initialVersion = ref.getVersion();

          GammaTransaction tx = stm.startDefaultTransaction();
          tx.commit();

          LongFunction function = mock(LongFunction.class);

          try {
              ref.alterAndGet(tx, function);
              fail();
          } catch (DeadTransactionException expected) {
          }

          assertIsCommitted(tx);
          assertRefHasNoLocks(ref);
          assertVersionAndValue(ref, initialVersion, initialValue);
      }

      @Test
      public void whenPreparedTransaction_thenPreparedTransactionException() {
          int initialValue = 10;
          GammaLongRef ref = new GammaLongRef(stm, initialValue);
          long initialVersion = ref.getVersion();

          GammaTransaction tx = stm.startDefaultTransaction();
          tx.prepare();

          LongFunction function = mock(LongFunction.class);

          try {
              ref.alterAndGet(tx, function);
              fail();
          } catch (PreparedTransactionException expected) {
          }

          assertRefHasNoLocks(ref);
          assertIsAborted(tx);
          assertVersionAndValue(ref, initialVersion, initialValue);
      }

      @Test
      public void whenAbortedTransaction() {
          int initialValue = 10;
          GammaLongRef ref = new GammaLongRef(stm, initialValue);
          long initialVersion = ref.getVersion();

          GammaTransaction tx = stm.startDefaultTransaction();
          tx.abort();

          LongFunction function = mock(LongFunction.class);

          try {
              ref.alterAndGet(tx, function);
              fail();
          } catch (DeadTransactionException expected) {
          }

          assertRefHasNoLocks(ref);
          assertIsAborted(tx);
          assertVersionAndValue(ref, initialVersion, initialValue);
      }

      @Test
      public void whenFunctionCausesException() {
          long initialValue = 10;
          GammaLongRef ref = new GammaLongRef(stm, initialValue);
          long initialVersion = ref.getVersion();

          LongFunction function = mock(LongFunction.class);
          RuntimeException ex = new RuntimeException();
          when(function.call(anyLong())).thenThrow(ex);

          GammaTransaction tx = stm.startDefaultTransaction();

          try {
              ref.alterAndGet(tx, function);
              fail();
          } catch (RuntimeException found) {
              assertSame(ex, found);
          }

          assertRefHasNoLocks(ref);
          assertIsAborted(tx);
          assertNull(getThreadLocalTransaction());
          assertVersionAndValue(ref, initialVersion, initialValue);
      }

      @Test
      public void whenPrivatizedByOther() {
          int initialValue = 10;
          GammaLongRef ref = new GammaLongRef(stm, initialValue);
          long version = ref.getVersion();

          GammaTransaction otherTx = stm.startDefaultTransaction();
          ref.getLock().acquire(otherTx, LockMode.Commit);

          GammaTransaction tx = stm.startDefaultTransaction();
          LongFunction function = mock(LongFunction.class);

          try {
              ref.alterAndGet(tx, function);
              fail();
          } catch (ReadWriteConflict expected) {
          }

          assertSurplus(1, ref);
          assertIsAborted(tx);
          assertRefHasCommitLock(ref, otherTx);
          assertVersionAndValue(ref, version, initialValue);
      }


      @Test
      public void whenEnsuredByOther_thenOperationSucceedsButCommitFails() {
          GammaLongRef ref = new GammaLongRef(stm, 10);
          long version = ref.getVersion();

          GammaTransaction otherTx = stm.startDefaultTransaction();
          ref.getLock().acquire(otherTx, LockMode.Write);

          GammaTransaction tx = stm.startDefaultTransaction();
          LongFunction function = Functions.newIncLongFunction(1);
          ref.alterAndGet(tx, function);

          try {
              tx.commit();
              fail();
          } catch (ReadWriteConflict expected) {
          }

          assertRefHasWriteLock(ref, otherTx);
          assertSurplus(1, ref);
          assertIsActive(otherTx);
          assertIsAborted(tx);
          assertVersionAndValue(ref, version, 10);
      }

      @Test
      @Ignore
      public void whenListenersAvailable_thenTheyAreNotified() {
          /*
          long initialValue = 10;
          GammaLongRef ref = new GammaLongRef(stm, initialValue);
          long initialVersion = ref.getVersion();

          LongRefAwaitThread thread = new LongRefAwaitThread(ref, initialValue + 1);
          thread.start();

          sleepMs(500);

          GammaTransaction tx = stm.startDefaultTransaction();
          ref.alterAndGet(tx, newIncLongFunction());
          tx.commit();

          joinAll(thread);

          assertVersionAndValue(ref, initialVersion + 1, initialValue + 1);*/
      }

      @Test
      public void whenSuccess() {
          LongFunction function = new LongFunction() {
              @Override
              public long call(long current) {
                  return current + 1;
              }
          };

          GammaLongRef ref = new GammaLongRef(stm, 100);
          GammaTransaction tx = stm.startDefaultTransaction();
          long result = ref.alterAndGet(tx, function);
          tx.commit();

          assertEquals(101, ref.atomicGet());
          assertEquals(101, result);
      }
    
}
