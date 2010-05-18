package org.multiverse.stms.alpha.instrumentation.gettersetter;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.annotations.TransactionalObject;
import org.multiverse.stms.alpha.AlphaStm;

import java.util.concurrent.TimeUnit;

import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * A performance test that test 3 different getter/setter approaches:
 * 1) direct field access (so no getter setter)
 * 2) getter/setter that is inlined
 * 3) getter/setter that can't be inlined.
 * <p/>
 * The performance of 1 and 2 should be almost the same. The 3 should be a lot slower.
 * In my machine number 1 and 2 do 50M inc/second and no 3  20M inc/second. So the getter
 * setter inlining works.
 * <p/>
 * Performance optimizations are disabled when using the Javaagent. So don't expect a
 * big performance improvement here.
 *
 * @author Peter Veentjer
 */
public class SetterInlinePerformanceTest {
    private static final int incCount = 1000 * 1000 * 200;

    private AlphaStm stm;

    @Before
    public void setUp() {
        stm = (AlphaStm) getGlobalStmInstance();
        clearThreadLocalTransaction();
    }

    @After
    public void tearDown() {
        clearThreadLocalTransaction();
    }

    @Test
    public void inlinedGetterSetter() {
        long startNs = System.nanoTime();

        Foo foo = new Foo();
        foo.loopWithInlinedGetterSetter();

        long durationNs = System.nanoTime() - startNs;
        double incPerSecond = (1.0d * incCount * TimeUnit.SECONDS.toNanos(1)) / durationNs;
        System.out.printf("Performance %s inc/second\n", incPerSecond);
    }

    @Test
    public void fastLoop() {
        long startNs = System.nanoTime();

        Foo foo = new Foo();
        foo.loop();

        long durationNs = System.nanoTime() - startNs;
        double incPerSecond = (1.0d * incCount * TimeUnit.SECONDS.toNanos(1)) / durationNs;
        System.out.printf("Performance %s inc/second\n", incPerSecond);
    }

    @Test
    public void nonOptimizedLoop() {
        long startNs = System.nanoTime();

        Foo foo = new Foo();
        foo.nonOptimizedLoop();

        long durationNs = System.nanoTime() - startNs;
        double incPerSecond = (1.0d * incCount * TimeUnit.SECONDS.toNanos(1)) / durationNs;
        System.out.printf("Performance %s inc/second\n", incPerSecond);
    }

    @TransactionalObject
    public class Foo {
        int value;

        final int get() {
            return value;
        }

        final void set(int value) {
            this.value = value;
        }

        final int get(int destroyOptimization) {
            return value;
        }

        final void set(int value, int destroyOptimization) {
            this.value = value;
        }

        void loopWithInlinedGetterSetter() {
            for (int k = 0; k < incCount; k++) {
                set(get() + 1);

                if (k % (200 * 1000 * 1000) == 0) {
                    System.out.println(k);
                }
            }
        }

        void loop() {
            for (int k = 0; k < incCount; k++) {
                value++;

                if (k % (200 * 1000 * 1000) == 0) {
                    System.out.println(k);
                }
            }
        }

        void nonOptimizedLoop() {
            for (int k = 0; k < incCount; k++) {
                set(get(0) + 1, 0);

                if (k % (200 * 1000 * 1000) == 0) {
                    System.out.println(k);
                }
            }
        }
    }
}
