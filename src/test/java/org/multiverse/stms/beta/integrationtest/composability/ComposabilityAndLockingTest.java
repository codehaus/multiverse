package org.multiverse.stms.beta.integrationtest.composability;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.StmUtils;
import org.multiverse.api.Transaction;
import org.multiverse.api.closures.AtomicVoidClosure;
import org.multiverse.api.references.IntRef;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.multiverse.api.StmUtils.newIntRef;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

public class ComposabilityAndLockingTest {

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
    }

    @Test
    public void whenEnsuredInOuter_thenCanSafelyBeEnsuredInInner() {
        final int initialValue = 10;
        final IntRef ref = newIntRef(initialValue);

        StmUtils.execute(new AtomicVoidClosure() {
            @Override
            public void execute(Transaction tx) throws Exception {
                ref.ensure();

                StmUtils.execute(new AtomicVoidClosure() {
                    @Override
                    public void execute(Transaction tx) throws Exception {
                        ref.ensure();
                        assertTrue(ref.isEnsuredBySelf());
                    }
                });
            }
        });

        assertTrue(ref.atomicIsFree());
        assertEquals(initialValue, ref.atomicGet());
    }

    @Test
    public void whenEnsuredInOuter_thenCanSafelyBePrivatizedInInner() {
        final int initialValue = 10;
        final IntRef ref = newIntRef(initialValue);

        StmUtils.execute(new AtomicVoidClosure() {
            @Override
            public void execute(Transaction tx) throws Exception {
                ref.ensure();

                StmUtils.execute(new AtomicVoidClosure() {
                    @Override
                    public void execute(Transaction tx) throws Exception {
                        ref.privatize();
                        assertTrue(ref.isPrivatizedBySelf());
                    }
                });
            }
        });

        assertTrue(ref.atomicIsFree());
        assertEquals(initialValue, ref.atomicGet());
    }

    @Test
    public void whenPrivatizedInOuter_thenCanSafelyBeEnsuredInInner() {
        final int initialValue = 10;
        final IntRef ref = newIntRef(initialValue);

        StmUtils.execute(new AtomicVoidClosure() {
            @Override
            public void execute(Transaction tx) throws Exception {
                ref.privatize();

                StmUtils.execute(new AtomicVoidClosure() {
                    @Override
                    public void execute(Transaction tx) throws Exception {
                        ref.ensure();
                        assertTrue(ref.isPrivatizedBySelf());
                    }
                });
            }
        });

        assertTrue(ref.atomicIsFree());
        assertEquals(initialValue, ref.atomicGet());
    }

    @Test
    public void whenPrivatizedInOuter_thenCanSafelyBePrivatizedInInner() {
        final int initialValue = 10;
        final IntRef ref = newIntRef(initialValue);

        StmUtils.execute(new AtomicVoidClosure() {
            @Override
            public void execute(Transaction tx) throws Exception {
                ref.privatize();

                StmUtils.execute(new AtomicVoidClosure() {
                    @Override
                    public void execute(Transaction tx) throws Exception {
                        ref.privatize();
                        assertTrue(ref.isPrivatizedBySelf());
                    }
                });
            }
        });

        assertTrue(ref.atomicIsFree());
        assertEquals(initialValue, ref.atomicGet());
    }
}
