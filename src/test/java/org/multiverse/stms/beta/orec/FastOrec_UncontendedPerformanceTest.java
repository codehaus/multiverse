package org.multiverse.stms.beta.orec;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaStmConstants;
import org.multiverse.stms.beta.conflictcounters.GlobalConflictCounter;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRef;

import java.util.concurrent.TimeUnit;

import static org.multiverse.stms.beta.benchmarks.BenchmarkUtils.transactionsPerSecondAsString;

/**
 * A Performance test to help to figure out if overhead can be removed from the FastOrec.
 *
 * todo: idea; work with ccas? Could be cheaper under contention.. but more expensive is there is no contention.
 *
 * @author Peter Veentjer.
 */
public class FastOrec_UncontendedPerformanceTest implements BetaStmConstants {
    private BetaLongRef ref;
    private GlobalConflictCounter globalConflictCounter;
    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
        ref = new BetaLongRef(stm);
        globalConflictCounter = new GlobalConflictCounter();
    }

    @Test
    public void update() {
        FastOrec orec = new FastOrec();
        int cycles = 1000 * 1000 * 1000;
        long startNs = System.nanoTime();

        for (long k = 0; k < cycles; k++) {
            int arriveStatus = orec.___arrive(0);
            if (arriveStatus == ARRIVE_NORMAL) {
                orec.___tryLockAfterNormalArrive(0);
            } else {
                orec.___tryLockAndArrive(0);
            }
            orec.___departAfterUpdateAndUnlock(globalConflictCounter, ref);
        }

        long durationNs = System.nanoTime() - startNs;
        String performance = transactionsPerSecondAsString(cycles, TimeUnit.NANOSECONDS.toMillis(durationNs));

        System.out.printf("Update cycles benchmark\n");
        System.out.printf("Duration    : %s ms\n", TimeUnit.NANOSECONDS.toMillis(durationNs));
        System.out.printf("Cycles      : %s\n", cycles);
        System.out.printf("Performance : %s update-cycles/second\n", performance);
        System.out.printf("Orec        : %s\n", orec.___toOrecString());
    }

    @Test
    public void pessimisticUpdate() {
        FastOrec orec = new FastOrec();
        int cycles = 1000 * 1000 * 1000;
        long startNs = System.nanoTime();

        for (long k = 0; k < cycles; k++) {
            int arriveStatus = orec.___tryLockAndArrive(0);
            if (arriveStatus != ARRIVE_NORMAL) {
                orec.___tryLockAndArrive(0);
            }
            orec.___departAfterUpdateAndUnlock(globalConflictCounter, ref);
        }

        long durationNs = System.nanoTime() - startNs;
        String performance = transactionsPerSecondAsString(cycles, TimeUnit.NANOSECONDS.toMillis(durationNs));

        System.out.printf("Pessimistic Update cycles benchmark\n");
        System.out.printf("Duration    : %s ms\n", TimeUnit.NANOSECONDS.toMillis(durationNs));
        System.out.printf("Cycles      : %s\n", cycles);
        System.out.printf("Performance : %s update-cycles/second\n", performance);
        System.out.printf("Orec        : %s\n", orec.___toOrecString());
    }

    /**
     * Because an orec can become read biased, we need to do a update from time to time to prevent this
     * from happening.
     */
    @Test
    public void normalRead() {
        FastOrec orec = new FastOrec();
        int cycles = 1000 * 1000 * 1000;
        long startNs = System.nanoTime();

        for (long k = 0; k < cycles; k++) {
            int arriveStatus = orec.___arrive(0);
            if (arriveStatus == ARRIVE_NORMAL) {
                orec.___departAfterReading();
            } else {
                orec.___tryLockAndArrive(0);
                orec.___departAfterUpdateAndUnlock(globalConflictCounter, ref);
            }
        }

        long durationNs = System.nanoTime() - startNs;
        String performance = transactionsPerSecondAsString(cycles, TimeUnit.NANOSECONDS.toMillis(durationNs));

        System.out.printf("Readheavy cycles benchmark\n");
        System.out.printf("Duration    : %s ms\n", TimeUnit.NANOSECONDS.toMillis(durationNs));
        System.out.printf("Cycles      : %s\n", cycles);
        System.out.printf("Performance : %s arrive-departs/second\n", performance);
        System.out.printf("Orec        : %s\n", orec.___toOrecString());
    }

    @Test
    public void readBiasedRead(){
        FastOrec orec = new FastOrec();
        long cycles = 10l* 1000 * 1000 * 1000;
        long startNs = System.nanoTime();

        for (long k = 0; k < cycles; k++) {
            int arriveStatus = orec.___arrive(0);
            if (arriveStatus == ARRIVE_NORMAL) {
                orec.___departAfterReading();
            }
        }

        long durationNs = System.nanoTime() - startNs;
        String performance = transactionsPerSecondAsString(cycles, TimeUnit.NANOSECONDS.toMillis(durationNs));

        System.out.printf("Readbiased cycles benchmark\n");
        System.out.printf("Duration    : %s ms\n", TimeUnit.NANOSECONDS.toMillis(durationNs));
        System.out.printf("Cycles      : %s\n", cycles);
        System.out.printf("Performance : %s arrive-departs/second\n", performance);
        System.out.printf("Orec        : %s\n", orec.___toOrecString());
    }
}
