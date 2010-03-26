package org.multiverse;

import org.multiverse.api.Transaction;
import org.multiverse.api.TransactionStatus;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.*;

public class TestUtils {

    public static void clearCurrentThreadInterruptedStatus() {
        Thread.interrupted();
    }

    public static void assertInstanceOf(Object o, Class clazz) {
        assertTrue(
                String.format("o %s is not a subtype from clazz %s", o, clazz.getName()),
                o.getClass().isAssignableFrom(clazz));
    }

    public static boolean hasMethod(Class clazz, String method, Class... types) {
        try {
            clazz.getMethod(method, types);
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    public static boolean hasField(Class clazz, String fieldname) {
        try {
            clazz.getDeclaredField(fieldname);
            return true;
        } catch (NoSuchFieldException e) {
            return false;
        }
    }

    public static Object getField(Object o, String fieldname) {
        try {
            Field field = o.getClass().getDeclaredField(fieldname);
            field.setAccessible(true);
            return field.get(o);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
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

    @SuppressWarnings({"ThrowableInstanceNeverThrown"})
    public static void testIncomplete() {
        StackTraceElement caller = new Throwable().getStackTrace()[1];

        System.out.println("============================================================================");
        System.out.printf("Test '%s' incomplete in file '%s' at line %s\n",
                caller.getMethodName(), caller.getFileName(), caller.getLineNumber());
        System.out.println("============================================================================");
    }

    @SuppressWarnings({"ThrowableInstanceNeverThrown"})
    public static void testIncomplete(String reason) {
        StackTraceElement caller = new Throwable().getStackTrace()[1];

        System.out.println("============================================================================");
        System.out.printf("Test '%s' incomplete in file '%s' at line %s\n",
                caller.getMethodName(), caller.getFileName(), caller.getLineNumber());
        System.out.printf("Reason: %s\n", reason);
        System.out.println("============================================================================");
    }

    public static void assertIsActive(Transaction... transactions) {
        assertNotNull("No transaction found", transactions);
        for (Transaction tx : transactions) {
            assertEquals(TransactionStatus.active, tx.getStatus());
        }
    }

    public static void assertIsPrepared(Transaction... transactions) {
        assertNotNull("No transaction found", transactions);
        for (Transaction tx : transactions) {
            assertEquals(TransactionStatus.prepared, tx.getStatus());
        }
    }

    public static void assertIsCommitted(Transaction... transactions) {
        assertNotNull("No transaction found", transactions);
        for (Transaction tx : transactions) {
            assertEquals(TransactionStatus.committed, tx.getStatus());
        }
    }

    public static void assertIsAborted(Transaction... transactions) {
        assertNotNull("No transaction found", transactions);
        for (Transaction tx : transactions) {
            assertEquals(TransactionStatus.aborted, tx.getStatus());
        }
    }

    public static boolean equals(Object o1, Object o2) {
        if (o1 == null) {
            return o2 == null;
        }

        return o1.equals(o2);
    }

    public static boolean randomBoolean() {
        return randomInt(10) % 2 == 0;
    }

    public static boolean randomOneOf(int chance) {
        return randomInt(Integer.MAX_VALUE) % chance == 0;
    }

    /**
     * Returns random int exclusive max. 0 is allowed as max and returns 0 (so this is unlike the Random.nextInt).
     *
     * @param max
     * @return
     */
    public static int randomInt(int max) {
        if (max <= 0) {
            return 0;
        }
        return ThreadLocalRandom.current().nextInt(max);
    }

    public static void sleepRandomMs(int maxMs) {
        sleepMs((long) randomInt(maxMs));
    }

    public static void sleepSome() {
        sleepMs(100);
    }

    public static void sleepMs(long ms) {
        long us = TimeUnit.MILLISECONDS.toMicros(ms);
        sleepUs(us);
    }

    public static void sleepUs(long us) {
        if (us <= 0) {
            return;
        }

        long nanos = TimeUnit.MICROSECONDS.toNanos(us);
        LockSupport.parkNanos(nanos);
    }

    public static void startAll(TestThread... threads) {
        for (Thread thread : threads) {
            thread.start();
        }
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

    public static String readText(File errorOutputFile) {
        try {
            StringBuffer sb = new StringBuffer();
            BufferedReader reader = new BufferedReader(new FileReader(errorOutputFile));

            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
