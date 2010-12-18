package org.multiverse;

import org.multiverse.api.Transaction;
import org.multiverse.api.TransactionStatus;
import org.multiverse.api.blocking.RetryLatch;
import org.multiverse.api.functions.Function;
import org.multiverse.api.lifecycle.TransactionLifecycleListener;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.Listeners;
import org.multiverse.stms.beta.transactionalobjects.*;
import org.multiverse.stms.beta.transactions.BetaTransaction;
import org.multiverse.stms.beta.transactions.FatMonoBetaTransaction;
import org.multiverse.utils.Bugshaker;
import org.multiverse.utils.ThreadLocalRandom;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.junit.Assert.*;
import static org.multiverse.stms.beta.BetaStmTestUtils.newLongRef;
import static org.multiverse.stms.beta.orec.OrecTestUtils.assertHasNoCommitLock;

/**
 * @author Peter Veentjer
 */
public class TestUtils implements MultiverseConstants {

    public static void clearCurrentThreadInterruptedStatus() {
        Thread.interrupted();
    }

    public static void assertEqualsDouble(String msg, double expected, double found) {
        assertEquals(msg, Double.doubleToLongBits(expected), Double.doubleToLongBits(found));
    }

    public static void assertEqualsDouble(double expected, double found) {
        assertEqualsDouble(null, expected, found);
    }

    public static int processorCount() {
        return Runtime.getRuntime().availableProcessors();
    }

    public static void assertAttached(BetaTransaction tx, BetaTranlocal tranlocal) {
        BetaTranlocal result = tx.get(tranlocal.owner);
        assertSame(tranlocal, result);
    }

    public static void assertNotAttached(BetaTransaction tx, BetaTransactionalObject object) {
        BetaTranlocal result = tx.get(object);
        assertNull(result);
    }

    public static void assertHasNoCommutingFunctions(BetaLongRefTranlocal tranlocal) {
        assertHasCommutingFunctions(tranlocal);
    }

    public static void assertHasUpdates(BetaTransaction tx) {
        assertTrue((Boolean) getField(tx, "hasUpdates"));
    }

    public static void assertHasNoUpdates(BetaTransaction tx) {
        assertFalse((Boolean) getField(tx, "hasUpdates"));
    }

    public static void assertHasCommutingFunctions(BetaLongRefTranlocal tranlocal, Function... expected) {
        CallableNode current = tranlocal.headCallable;
        List<Function> functions = new LinkedList<Function>();
        while (current != null) {
            functions.add(current.function);
            current = current.next;
        }

        assertEquals(asList(expected), functions);
    }

    public static void assertHasNoNormalListeners(BetaTransaction tx) {
        assertHasNormalListeners(tx);
    }

    public static void assertHasNormalListeners(BetaTransaction tx, TransactionLifecycleListener... listeners) {
        List<TransactionLifecycleListener> l =
                (List<TransactionLifecycleListener>) getField(tx, "normalListeners");
        if (l == null) {
            l = new LinkedList<TransactionLifecycleListener>();
        }
        assertEquals(Arrays.asList(listeners), l);
    }

    public static void assertEra(RetryLatch latch, long era) {
        assertEquals(era, latch.getEra());
    }

    public static void assertOpen(RetryLatch latch) {
        assertTrue(latch.isOpen());
    }

    public static void assertClosed(RetryLatch latch) {
        assertFalse(latch.isOpen());
    }

    public static void assertAllNull(BetaTranlocal[] array) {
        assertNotNull(array);

        for (BetaTranlocal tranlocal : array) {
            assertNull(tranlocal);
        }
    }

    public static void assertEqualByteArray(byte[] array1, byte[] array2) {
        if (array1 == array2) {
            return;
        }

        if (array1 == null) {
            fail();
        }

        int length = array1.length;
        assertEquals(length, array2.length);
        for (int k = 0; k < array1.length; k++) {
            assertEquals(array1[k], array2[k]);
        }
    }

    public static void assertHasListeners(BetaTransactionalObject ref, RetryLatch... listeners) {
        Set<RetryLatch> expected = new HashSet<RetryLatch>(Arrays.asList(listeners));

        Set<RetryLatch> found = new HashSet<RetryLatch>();
        Listeners l = (Listeners) getField(ref, "___listeners");
        while (l != null) {
            found.add(l.listener);
            l = l.next;
        }
        assertEquals(expected, found);
    }

    public static void assertHasNoListeners(BetaTransactionalObject ref) {
        assertHasListeners(ref);
    }

    public static Object getField(Object o, String fieldname) {
        if (o == null || fieldname == null) {
            throw new NullPointerException();
        }

        try {
            Field field = findField(o.getClass(), fieldname);
            if (field == null) {
                fail(format("field '%s' is not found on class '%s' or on one of its super classes", fieldname, o.getClass().getName()));
            }
            field.setAccessible(true);
            return field.get(o);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static Field findField(Class clazz, String fieldname) {
        try {
            return clazz.getDeclaredField(fieldname);
        } catch (NoSuchFieldException e) {
            if (clazz.equals(Object.class)) {
                return null;
            }

            return findField(clazz.getSuperclass(), fieldname);
        }
    }

    public static void assertNotEquals(long l1, long l2) {
        assertFalse(format("both values are %s, but should not be equal", l2), l1 == l2);
    }

    public static void assertIsNew(Transaction... transactions) {
        for (Transaction tx : transactions) {
            assertEquals(TransactionStatus.Undefined, tx.getStatus());
        }
    }

    public static void assertIsPrepared(Transaction... transactions) {
        for (Transaction tx : transactions) {
            assertEquals(TransactionStatus.Prepared, tx.getStatus());
        }
    }

    public static void assertIsAborted(Transaction... transactions) {
        for (Transaction tx : transactions) {
            assertEquals(TransactionStatus.Aborted, tx.getStatus());
        }
    }

    public static void assertIsCommitted(Transaction... transactions) {
        for (Transaction tx : transactions) {
            assertEquals(TransactionStatus.Committed, tx.getStatus());
        }
    }

    public static void assertIsActive(Transaction... transactions) {
        for (Transaction tx : transactions) {
            assertEquals(TransactionStatus.Active, tx.getStatus());
        }
    }

    public static BetaLongRef createReadBiasedLongRef(BetaStm stm) {
        return createReadBiasedLongRef(stm, 0);
    }

    public static int randomInt(int max) {
        if (max <= 0) {
            return 0;
        }

        return ThreadLocalRandom.current().nextInt(max);
    }

    public static void sleepRandomMs(int maxMs) {
        Bugshaker.sleepUs((long) randomInt((int) TimeUnit.MILLISECONDS.toMicros(maxMs)));
    }

    public static void sleepMs(long ms) {
        long us = TimeUnit.MILLISECONDS.toMicros(ms);
        Bugshaker.sleepUs(us);
    }

    //todo: should be moved to betastmtestutils.

    public static BetaLongRef createReadBiasedLongRef(BetaStm stm, long value) {
        BetaLongRef ref = newLongRef(stm, value);

        for (int k = 0; k < ref.___getOrec().___getReadBiasedThreshold(); k++) {
            BetaTransaction tx = new FatMonoBetaTransaction(stm);
            tx.openForRead(ref, LOCKMODE_NONE);
            tx.commit();
            assertHasNoCommitLock(ref.___getOrec());
        }

        assertTrue(ref.___getOrec().___isReadBiased());

        return ref;
    }

    public static boolean randomBoolean() {
        return randomInt(10) % 2 == 0;
    }

    public static boolean randomOneOf(int chance) {
        return randomInt(Integer.MAX_VALUE) % chance == 0;
    }

    public static long getStressTestDurationMs(long defaultDuration) {
        String value = System.getProperty("org.multiverse.integrationtest.durationMs", "" + defaultDuration);
        return Long.parseLong(value);
    }


    public static void assertIsInterrupted(Thread t) {
        assertTrue(t.isInterrupted());
    }

    public static void assertAlive(Thread... threads) {
        for (Thread thread : threads) {
            assertTrue(thread.getState().toString(), thread.isAlive());
        }
    }

    public static void assertNotAlive(Thread... threads) {
        for (Thread thread : threads) {
            assertFalse(thread.isAlive());
        }
    }

    public static void startAll(TestThread... threads) {
        for (Thread thread : threads) {
            thread.start();
        }
    }

    public static void sleepRandomUs(int maxUs) {
        Bugshaker.sleepUs((long) randomInt(maxUs));
    }

    public static void assertInstanceof(Class expected, Object o) {
        assertTrue(o.getClass().getName(), expected.isAssignableFrom(o.getClass()));
    }

    /**
     * Joins all threads. If this can't be done within 5 minutes, an assertion failure is thrown.
     *
     * @param threads the threads to join.
     * @return the total duration of all threads (so the sum of the time each thread has been running.
     * @see #joinAll(long, TestThread...) for more specifics.
     */
    public static long joinAll(TestThread... threads) {
        return joinAll(5 * 60, threads);
    }

    /**
     * Joins all threads. If one of the thread throws a throwable, the join will fail as well.
     *
     * @param timeoutSec the timeout in seconds. If the join doesn't complete within that time, the
     *                   join fails.
     * @param threads    the threads to join.
     * @return the total duration of all threads (so the sum of the time each thread has been running.
     */
    @SuppressWarnings({"ThrowableResultOfMethodCallIgnored"})
    public static long joinAll(long timeoutSec, TestThread... threads) {
        if (timeoutSec < 0) {
            throw new IllegalArgumentException();
        }

        List<TestThread> uncompleted = new LinkedList<TestThread>(Arrays.asList(threads));

        long maxTimeMs = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(timeoutSec);

        long durationMs = 0;

        while (!uncompleted.isEmpty()) {
            for (Iterator<TestThread> it = uncompleted.iterator(); it.hasNext();) {
                TestThread thread = it.next();
                try {
                    if (System.currentTimeMillis() > maxTimeMs) {
                        fail(String.format(
                                "Failed to join all threads in %s seconds, remaining threads %s",
                                timeoutSec, uncompleted));
                    }
                    thread.join(100);

                    if (!thread.isAlive()) {
                        it.remove();
                        durationMs += thread.getDurationMs();

                        if (thread.getThrowable() == null) {
                            System.out.printf("Multiverse > %s completed successfully\n", thread.getName());
                        } else {
                            System.out.printf("Multiverse > %s encountered the following error\n", thread.getName());
                            thread.getThrowable().printStackTrace();
                            fail(String.format("Multiverse > %s completed with failure", thread.getName()));
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(String.format("Joining %s was interrupted", thread), e);
                }
            }
        }

        return durationMs;
    }

}