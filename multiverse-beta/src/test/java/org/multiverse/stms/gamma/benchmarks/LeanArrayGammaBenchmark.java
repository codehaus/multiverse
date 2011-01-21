package org.multiverse.stms.gamma.benchmarks;

import org.benchy.BenchyUtils;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.gamma.GammaConstants;
import org.multiverse.stms.gamma.GammaStm;
import org.multiverse.stms.gamma.transactionalobjects.AbstractGammaObject;
import org.multiverse.stms.gamma.transactionalobjects.AbstractGammaRef;
import org.multiverse.stms.gamma.transactionalobjects.GammaRef;
import org.multiverse.stms.gamma.transactionalobjects.GammaRefTranlocal;
import org.multiverse.stms.gamma.transactions.lean.LeanArrayGammaTransaction;

public class LeanArrayGammaBenchmark implements GammaConstants {

    private GammaStm stm;

    @Before
    public void setUp() {
        stm = new GammaStm();
    }

    public static void main(String[] args) {
        LeanArrayGammaBenchmark benchmark = new LeanArrayGammaBenchmark();
        benchmark.setUp();
        benchmark.testInlined();
    }

    @Test
    public void testRead1() {
        final long txCount = 1000 * 1000 * 1000;
        final GammaRef<String> ref1 = new GammaRef<String>(stm);
        final LeanArrayGammaTransaction tx = new LeanArrayGammaTransaction(stm);
        final long startMs = System.currentTimeMillis();

        for (long k = 0; k < txCount; k++) {
            ref1.openForRead(tx);
            tx.commit();
            tx.hardReset();
        }

        long durationMs = System.currentTimeMillis() - startMs;

        String s = BenchyUtils.operationsPerSecondPerThreadAsString(txCount, durationMs, 1);

        System.out.printf("Performance is %s transactions/second/thread\n", s);


        System.out.println(ref1.toDebugString());
    }

    @Test
    public void testInlined() {
        final long txCount = 5L * 1000 * 1000 * 1000;
        final GammaRef<String> ref1 = new GammaRef<String>(stm);
        final LeanArrayGammaTransaction tx = new LeanArrayGammaTransaction(stm);
        final long startMs = System.currentTimeMillis();

        for (long k = 0; k < txCount; k++) {
            if (tx.status != GammaConstants.TX_ACTIVE) {
                throw tx.abortOpenForReadOnBadStatus(ref1);
            }

            final boolean hasReads = tx.hasReads;
            final GammaRefTranlocal[] array = tx.tranlocals;
            int index;
            GammaRefTranlocal tranlocal = null;
            final int txSize = tx.size;
            for (index = 0; index < txSize; index++) {
                tranlocal = array[index];
                final AbstractGammaRef owner = tranlocal.owner;

                if (owner == ref1) {
                    //if (index > 3) {
                    //    GammaRefTranlocal tmp = array[0];
                    //    array[index] = tmp;
                    //    array[0] = tranlocal;
                    //}
                    return;
                }
            }


            //we have not found it, but there also is no spot available.
            if (index == array.length - 1) {
                throw tx.abortOnTooSmallSize(array.length + 1);
            }

            //load it
            tx.size = txSize + 1;
            //tranlocal.mode = TRANLOCAL_READ;
            //tranlocal.isDirty = false;
            tranlocal = array[index];
            tranlocal.owner = ref1;
            //tranlocal.setLockMode(LOCKMODE_NONE);
            //tranlocal.hasDepartObligation = false;
            while (true) {
                //JMM: nothing can jump behind the following statement
                Object readRef = ref1.ref_value;
                final long readVersion = ref1.version;

                //wait for the exclusive lock to come available.
                int spinCount = 64;
                for (; ;) {
                    if ((ref1.orec & AbstractGammaObject.BITMASK_EXCLUSIVELOCK) == 0) {
                        break;
                    }
                    spinCount--;
                    if (spinCount < 0) {
                        throw tx.abortOnReadWriteConflict();
                    }
                }

                //check if the version and value we read are still the same, if they are not, we have read illegal memory,
                //so we are going to try again.
                if (readVersion == ref1.version) {
                    //at this point we are sure that the read was unlocked.
                    tranlocal.version = readVersion;
                    tranlocal.ref_value = readRef;
                    //tranlocal.ref_oldValue = readRef;
                    break;
                }
            }

            //lets put it in the front it isn't the first one that is opened.
            //if (index > 3) {
            //    GammaRefTranlocal tmp = array[0];
            //    array[index] = tmp;
            //    array[0] = tranlocal;
            //}

            //check if the transaction still is read consistent.
            if (hasReads) {
                for (int l = 0; l < array.length; l++) {
                    final GammaRefTranlocal t = array[l];
                    //if we are at the end, we are done.
                    final AbstractGammaRef owner = t.owner;

                    if (owner == null) {
                        break;
                    }

                    if (t != tranlocal && ((owner.orec & AbstractGammaObject.BITMASK_EXCLUSIVELOCK) != 0 || owner.version != t.version)) {
                        throw tx.abortOnReadWriteConflict();
                    }
                }
            } else {
                tx.hasReads = true;
            }

            //we are done, the load was correct and the transaction still is read consistent.
            tx.commit();
            tx.hardReset();
        }

        long durationMs = System.currentTimeMillis() - startMs;

        String s = BenchyUtils.operationsPerSecondPerThreadAsString(txCount, durationMs, 1);

        System.out.printf("Performance is %s transactions/second/thread\n", s);


        System.out.println(ref1.toDebugString());
    }


    @Test
    public void testReread1() {
        final long txCount = 1000 * 1000 * 1000;
        final GammaRef<String> ref1 = new GammaRef<String>(stm);
        final LeanArrayGammaTransaction tx = new LeanArrayGammaTransaction(stm);
        final long startMs = System.currentTimeMillis();

        for (long k = 0; k < txCount; k++) {
            ref1.openForRead(tx);
            ref1.openForRead(tx);
            tx.commit();
            tx.hardReset();
        }

        long durationMs = System.currentTimeMillis() - startMs;

        String s = BenchyUtils.operationsPerSecondPerThreadAsString(txCount, durationMs, 1);

        System.out.printf("Performance is %s transactions/second/thread\n", s);


        System.out.println(ref1.toDebugString());
    }

    @Test
    public void testRead2() {
        final long txCount = 1000 * 1000 * 1000;
        final GammaRef<String> ref1 = new GammaRef<String>(stm);
        final GammaRef<String> ref2 = new GammaRef<String>(stm);
        final LeanArrayGammaTransaction tx = new LeanArrayGammaTransaction(stm);
        final long startMs = System.currentTimeMillis();

        for (long k = 0; k < txCount; k++) {
            ref1.openForRead(tx);
            ref2.openForRead(tx);

            tx.commit();
            tx.hardReset();
        }

        long durationMs = System.currentTimeMillis() - startMs;

        String s = BenchyUtils.operationsPerSecondPerThreadAsString(txCount, durationMs, 1);

        System.out.printf("Performance is %s transactions/second/thread\n", s);


        System.out.println(ref1.toDebugString());
    }

    @Test
    public void testReread2() {
        final long txCount = 1000 * 1000 * 1000;
        final GammaRef<String> ref1 = new GammaRef<String>(stm);
        final GammaRef<String> ref2 = new GammaRef<String>(stm);
        final LeanArrayGammaTransaction tx = new LeanArrayGammaTransaction(stm);
        final long startMs = System.currentTimeMillis();

        for (long k = 0; k < txCount; k++) {
            ref1.openForRead(tx);
            ref2.openForRead(tx);
            ref1.openForRead(tx);
            ref2.openForRead(tx);

            tx.commit();
            tx.hardReset();
        }

        long durationMs = System.currentTimeMillis() - startMs;

        String s = BenchyUtils.operationsPerSecondPerThreadAsString(txCount, durationMs, 1);

        System.out.printf("Performance is %s transactions/second/thread\n", s);


        System.out.println(ref1.toDebugString());
    }

    @Test
    public void testRead3() {
        final long txCount = 1000 * 1000 * 1000;
        final GammaRef<String> ref1 = new GammaRef<String>(stm);
        final GammaRef<String> ref2 = new GammaRef<String>(stm);
        final GammaRef<String> ref3 = new GammaRef<String>(stm);
        final LeanArrayGammaTransaction tx = new LeanArrayGammaTransaction(stm);
        final long startMs = System.currentTimeMillis();

        for (long k = 0; k < txCount; k++) {
            ref1.openForRead(tx);
            ref2.openForRead(tx);
            ref3.openForRead(tx);
            tx.commit();
            tx.hardReset();
        }

        long durationMs = System.currentTimeMillis() - startMs;

        String s = BenchyUtils.operationsPerSecondPerThreadAsString(txCount, durationMs, 1);

        System.out.printf("Performance is %s transactions/second/thread\n", s);


        System.out.println(ref1.toDebugString());
    }

    @Test
    public void testReread3() {
        final long txCount = 1000 * 1000 * 1000;
        final GammaRef<String> ref1 = new GammaRef<String>(stm);
        final GammaRef<String> ref2 = new GammaRef<String>(stm);
        final GammaRef<String> ref3 = new GammaRef<String>(stm);
        final LeanArrayGammaTransaction tx = new LeanArrayGammaTransaction(stm);
        final long startMs = System.currentTimeMillis();

        for (long k = 0; k < txCount; k++) {
            ref1.openForRead(tx);
            ref2.openForRead(tx);
            ref3.openForRead(tx);
            ref1.openForRead(tx);
            ref2.openForRead(tx);
            ref3.openForRead(tx);
            tx.commit();
            tx.hardReset();
        }

        long durationMs = System.currentTimeMillis() - startMs;

        String s = BenchyUtils.operationsPerSecondPerThreadAsString(txCount, durationMs, 1);

        System.out.printf("Performance is %s transactions/second/thread\n", s);


        System.out.println(ref1.toDebugString());
    }
}
