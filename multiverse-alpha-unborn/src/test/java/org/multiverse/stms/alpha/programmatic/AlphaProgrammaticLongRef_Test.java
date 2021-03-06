package org.multiverse.stms.alpha.programmatic;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.annotations.TransactionalObject;
import org.multiverse.api.Transaction;
import org.multiverse.api.TransactionFactory;
import org.multiverse.api.programmatic.ProgrammaticLongRef;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;

import static org.junit.Assert.assertEquals;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class AlphaProgrammaticLongRef_Test {
    private AlphaStm stm;
    private TransactionFactory<AlphaTransaction> txFactory;

    @Before
    public void setUp() {
        stm = (AlphaStm) getGlobalStmInstance();
        txFactory = stm.getTransactionFactoryBuilder()
                .setSpeculativeConfigurationEnabled(false)
                .setReadonly(false)
                .build();
    }

    @Test
    public void testBasics() {
        AlphaProgrammaticLongRef ref = new AlphaProgrammaticLongRef(10);
        assertEquals(10, ref.get());
    }

    @Test
    public void normalInc() {
        AlphaProgrammaticLongRef ref = new AlphaProgrammaticLongRef(10);
        ref.inc(5);
        assertEquals(15, ref.get());
    }

    @Test
    public void commutingInc() {
        AlphaProgrammaticLongRef ref = new AlphaProgrammaticLongRef(10);
        Transaction tx = txFactory.start();
        ref.commutingInc(tx, 1);
        tx.commit();

        assertEquals(11, ref.get());
    }

    @Test
    public void conflictingcommutingInc() {
        AlphaProgrammaticLongRef ref = new AlphaProgrammaticLongRef(10);
        Transaction tx1 = txFactory.start();
        ref.commutingInc(tx1, 1);


        Transaction tx2 = txFactory.start();
        ref.commutingInc(tx2, 1);
        tx2.commit();
        tx1.commit();

        assertEquals(12, ref.get());
    }

    @Test
    public void test() {
        Foo foo = new Foo();
        foo.inc();

        assertEquals(1, foo.get());
    }

    @TransactionalObject
    class Foo {
        final ProgrammaticLongRef size = getGlobalStmInstance()
                .getProgrammaticRefFactoryBuilder()
                .build()
                .createLongRef(0);

        public void inc() {
            size.inc(getThreadLocalTransaction(), 1);
        }

        public long get() {
            return size.get();
        }
    }
}
