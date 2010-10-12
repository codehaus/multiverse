package org.multiverse.stms.beta.integrationtest.composability;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.StmUtils;
import org.multiverse.api.Transaction;
import org.multiverse.api.closures.AtomicVoidClosure;
import org.multiverse.api.references.IntRef;

import static org.junit.Assert.*;
import static org.multiverse.api.StmUtils.newIntRef;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

public class ComposabilityTest {

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

    @Test
    public void whenComposingTransaction_thenInnerAndOuterTransactionAreTheSame() {
        StmUtils.execute(new AtomicVoidClosure() {
            @Override
            public void execute(final Transaction outerTx) throws Exception {
                StmUtils.execute(new AtomicVoidClosure() {
                    @Override
                    public void execute(Transaction innerTx) throws Exception {
                        assertSame(innerTx, outerTx);
                    }
                });
            }
        });
    }

    @Test
    public void whenSurroundingTransactionFails_thenChangesInInnerTransactionWillRollback() {
        int initialValue = 10;
        final IntRef ref = newIntRef(initialValue);

        try {
            StmUtils.execute(new AtomicVoidClosure() {
                @Override
                public void execute(Transaction tx) throws Exception {
                    StmUtils.execute(new AtomicVoidClosure() {
                        @Override
                        public void execute(Transaction tx) throws Exception {
                            ref.increment();
                        }
                    });

                    throw new MyException();
                }
            });
            fail();
        } catch (MyException expected) {
        }

        assertEquals(initialValue, ref.atomicGet());
    }

    @Test
    public void whenInnerTransactionFails_thenOuterTransactionWillRollback() {
        int initialValue = 10;
        final IntRef ref = newIntRef(initialValue);

        try {
            StmUtils.execute(new AtomicVoidClosure() {
                @Override
                public void execute(Transaction tx) throws Exception {
                    ref.increment();

                    StmUtils.execute(new AtomicVoidClosure() {
                        @Override
                        public void execute(Transaction tx) throws Exception {
                            throw new MyException();
                        }
                    });
                }
            });
            fail();
        } catch (MyException expected) {
        }

        assertEquals(initialValue, ref.atomicGet());
    }

    @Test
    public void whenOuterTransactionMakesChange_thenItWillBeVisibleInInnerTransaction() {
        final int initialValue = 10;
        final IntRef ref = newIntRef(initialValue);

        StmUtils.execute(new AtomicVoidClosure() {
            @Override
            public void execute(Transaction tx) throws Exception {
                ref.increment();

                StmUtils.execute(new AtomicVoidClosure() {
                    @Override
                    public void execute(Transaction tx) throws Exception {
                        assertEquals(initialValue + 1, ref.get());
                    }
                });
            }
        });

        assertEquals(initialValue + 1, ref.atomicGet());
    }

    @Test
    public void whenInnerTransactionMakesChange_thenItWillBeVisibleInOuterTransaction() {
        final int initialValue = 10;
        final IntRef ref = newIntRef(initialValue);

        StmUtils.execute(new AtomicVoidClosure() {
            @Override
            public void execute(Transaction tx) throws Exception {

                StmUtils.execute(new AtomicVoidClosure() {
                    @Override
                    public void execute(Transaction tx) throws Exception {
                        ref.increment();
                    }
                });

                assertEquals(initialValue + 1, ref.get());
            }
        });

        assertEquals(initialValue + 1, ref.atomicGet());
    }

    class MyException extends RuntimeException {
    }
}