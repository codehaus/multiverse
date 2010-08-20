package org.multiverse;

import org.multiverse.api.TransactionStatus;
import org.multiverse.api.blocking.Latch;
import org.multiverse.api.blocking.Listeners;
import org.multiverse.api.lifecycle.TransactionLifecycleListener;
import org.multiverse.functions.LongFunction;
import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaStmUtils;
import org.multiverse.stms.beta.transactionalobjects.BetaTransactionalObject;
import org.multiverse.stms.beta.transactionalobjects.LongRef;
import org.multiverse.stms.beta.transactionalobjects.LongRefTranlocal;
import org.multiverse.stms.beta.transactionalobjects.Tranlocal;
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
import static org.multiverse.stms.beta.orec.OrecTestUtils.assertUnlocked;

/**
 * @author Peter Veentjer
 */
public class TestUtils {

    public static int processorCount() {
        return Runtime.getRuntime().availableProcessors();
    }

    public static void assertAttached(BetaTransaction tx, Tranlocal tranlocal) {
        Tranlocal result = tx.get(tranlocal.getOwner());
        assertSame(tranlocal, result);
    }

    public static void assertNotAttached(BetaTransaction tx, BetaTransactionalObject object) {
        Tranlocal result = tx.get(object);
        assertNull(result);
    }

    public static void assertHasNoCommutingFunctions(LongRefTranlocal tranlocal) {
        assertHasCommutingFunctions(tranlocal);
    }

    public static void assertHasCommutingFunctions(LongRefTranlocal tranlocal, LongFunction... expected) {
        LongRefTranlocal.CallableNode current = tranlocal.headCallable;
        List<LongFunction> functions = new LinkedList<LongFunction>();
        while (current != null) {
            functions.add(current.callable);
            current = current.next;
        }

        assertEquals(asList(expected), functions);
    }

    public static void assertHasNoPermanentListeners(BetaTransaction tx) {
        assertHasPermanentListeners(tx);
    }

    public static void assertHasPermanentListeners(BetaTransaction tx, TransactionLifecycleListener... listeners) {
        List<TransactionLifecycleListener> l = (List<TransactionLifecycleListener>) getField(tx, "permanentListeners");
        if (l == null) {
            l = new LinkedList();
        }
        assertEquals(Arrays.asList(listeners), l);
    }

    public static void assertHasNoNormalListeners(BetaTransaction tx) {
        assertHasNormalListeners(tx);
    }

    public static void assertHasNormalListeners(BetaTransaction tx, TransactionLifecycleListener... listeners) {
        List<TransactionLifecycleListener> l = (List<TransactionLifecycleListener>) getField(tx, "normalListeners");
        if (l == null) {
            l = new LinkedList();
        }
        assertEquals(Arrays.asList(listeners), l);
    }

    public static void assertEra(Latch latch, long era) {
        assertEquals(era, latch.getEra());
    }

    public static void assertOpen(Latch latch) {
        assertTrue(latch.isOpen());
    }

    public static void assertClosed(Latch latch) {
        assertFalse(latch.isOpen());
    }

    public static void assertAllNull(Tranlocal[] array) {
        assertNotNull(array);

        for (Tranlocal tranlocal : array) {
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

    public static void assertHasListeners(BetaTransactionalObject ref, Latch... listeners) {
        Set<Latch> expected = new HashSet(Arrays.asList(listeners));

        Set<Latch> found = new HashSet<Latch>();
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

    public static void assertNew(BetaTransaction tx) {
        assertEquals(TransactionStatus.Unstarted, tx.getStatus());
    }

    public static void assertPrepared(BetaTransaction tx) {
        assertEquals(TransactionStatus.Prepared, tx.getStatus());
    }

    public static void assertAborted(BetaTransaction tx) {
        assertEquals(TransactionStatus.Aborted, tx.getStatus());
    }

    public static void assertCommitted(BetaTransaction tx) {
        assertEquals(TransactionStatus.Committed, tx.getStatus());
    }

    public static void assertActive(BetaTransaction tx) {
        assertEquals(TransactionStatus.Active, tx.getStatus());
    }

    public static LongRef createReadBiasedLongRef(BetaStm stm) {
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

    public static LongRef createReadBiasedLongRef(BetaStm stm, long value) {
        LongRef ref = BetaStmUtils.createLongRef(stm, value);

        for (int k = 0; k < ref.___getOrec().___getReadBiasedThreshold(); k++) {
            BetaTransaction tx = new FatMonoBetaTransaction(stm);
            tx.openForRead(ref, false, new BetaObjectPool());
            tx.commit(new BetaObjectPool());
            assertUnlocked(ref.___getOrec());
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
            assertTrue(thread.isAlive());
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


    /**
     * Joins all threads. If this can't be done within 5 minutes, an assertion failure is thrown.
     *
     * @param threads
     */
    public static void joinAll(TestThread... threads) {
        joinAll(5 * 60 * 1000, threads);
    }

    @SuppressWarnings({"ThrowableResultOfMethodCallIgnored"})
    public static void joinAll(long maxJoinTimeMillis, TestThread... threads) {
        if (maxJoinTimeMillis < 0) {
            throw new IllegalArgumentException();
        }

        List<TestThread> uncompleted = new LinkedList(Arrays.asList(threads));

        long maxTimeMillis = System.currentTimeMillis() + maxJoinTimeMillis;

        while (!uncompleted.isEmpty()) {
            for (Iterator<TestThread> it = uncompleted.iterator(); it.hasNext();) {
                TestThread thread = it.next();
                try {
                    if (System.currentTimeMillis() > maxTimeMillis) {
                        fail(String.format(
                                "Failed to join all threads in %s ms, remaining threads %s",
                                maxJoinTimeMillis, uncompleted));
                    }
                    thread.join(100);
                    if (!thread.isAlive()) {
                        it.remove();

                        if (thread.getThrowable() == null) {
                            System.out.printf("%s completed successfully\n", thread.getName());
                        } else {
                            System.out.printf("%s encountered the following error\n", thread.getName());
                            thread.getThrowable().printStackTrace();
                            fail(String.format("%s completed with failure", thread.getName()));
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(String.format("Joining %s was interrupted", thread), e);
                }
            }
        }
    }

}
