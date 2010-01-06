package org.multiverse;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.*;
import org.multiverse.api.Transaction;
import org.multiverse.api.TransactionStatus;
import org.multiverse.utils.instrumentation.InstrumentationProblemMonitor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class TestUtils {

    public static void resetInstrumentationProblemMonitor() {
        try {
            Field field = InstrumentationProblemMonitor.class.getDeclaredField(
                    "problemFound");
            field.setAccessible(true);
            field.set(InstrumentationProblemMonitor.INSTANCE, false);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static void assertIsInterrupted(Thread t) {
        assertTrue(t.isInterrupted());
    }

    public static void assertAreAlive(Thread... threads) {
        for (Thread thread : threads) {
            assertTrue(thread.isAlive());
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

    public static void assertNoInstrumentationProblems() {
        assertFalse(InstrumentationProblemMonitor.INSTANCE.isProblemFound());
    }

    public static void assertIsActive(Transaction t) {
        assertNotNull("No transaction found", t);
        assertEquals(TransactionStatus.active, t.getStatus());
    }

    public static void assertIsCommitted(Transaction t) {
        assertNotNull("No transaction found", t);
        assertEquals(TransactionStatus.committed, t.getStatus());
    }

    public static void assertIsAborted(Transaction t) {
        assertNotNull("No transaction found", t);
        assertEquals(TransactionStatus.aborted, t.getStatus());
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

    public static int randomInt(int max) {
        return ThreadLocalRandom.current().nextInt(max);
    }

    public static void sleepRandomMs(int maxMs) {
        if (maxMs <= 0) {
            return;
        }

        sleepMs((long) randomInt(maxMs));
        Thread.yield();
    }

    public static void sleepMs(long ms) {
        if (ms == 0) {
            return;
        }

        try {
            Thread.sleep(ms);
        } catch (InterruptedException ex) {
            Thread.interrupted();
            throw new RuntimeException(ex);
        }
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
